package com.kizuna.features.classroom.service.impl;

import com.kizuna.features.classroom.dto.LearningMaterialResponse;
import com.kizuna.features.classroom.dto.MaterialFileContent;
import com.kizuna.features.classroom.dto.MaterialFileResponse;
import com.kizuna.features.classroom.entity.LearningMaterial;
import com.kizuna.features.classroom.entity.LearningMaterialFile;
import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.repository.LearningMaterialFileRepository;
import com.kizuna.features.classroom.repository.LearningMaterialRepository;
import com.kizuna.features.classroom.service.ClassroomService;
import com.kizuna.features.classroom.service.LearningMaterialService;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.classroom.util.MaterialFileValidator;
import com.kizuna.features.classroom.util.MaterialFileValidator.MaterialFileType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Tài liệu học tập FE-06: một tài liệu gồm tiêu đề + mô tả + 1–10 tệp.
 * Tệp lưu trong bảng learning_material_files (migration 014), nội dung ở cột file_data (BYTEA).
 * Bản ghi và nội dung tệp được ghi trong cùng một giao dịch nên lỗi ở bất kỳ bước nào cũng không để lại dữ liệu dở dang.
 * Cột learning_materials.file_url/file_type (NOT NULL từ thiết kế cũ) lưu tên/loại của tệp đầu tiên.
 */
@Service
@RequiredArgsConstructor
public class LearningMaterialServiceImpl implements LearningMaterialService {

    static final int TITLE_MAX = 150;
    static final int DESCRIPTION_MAX = 1000;
    static final int FILE_NAME_MAX = 200;

    private final ClassroomService classroomService;
    private final LearningMaterialRepository materialRepository;
    private final LearningMaterialFileRepository fileRepository;
    private final MaterialFileValidator fileValidator;

    /** Tệp đã kiểm tra, sẵn sàng ghi. */
    private record Prepared(MaterialFileType type, String fileName, byte[] bytes) {
    }

    @Override
    @Transactional(readOnly = true)
    public List<LearningMaterialResponse> list(CurrentActor actor, UUID spaceId) {
        classroomService.loadAccessibleSpace(actor, spaceId);
        List<LearningMaterial> materials = materialRepository.findBySpace_SpaceIdOrderByCreatedAtDesc(spaceId);
        if (materials.isEmpty()) return List.of();
        Map<UUID, List<LearningMaterialFile>> filesByMaterial = fileRepository
                .findByMaterialIdInOrderByCreatedAtAscFileNameAsc(materials.stream().map(LearningMaterial::getMaterialId).toList())
                .stream().collect(Collectors.groupingBy(LearningMaterialFile::getMaterialId));
        return materials.stream()
                .map(m -> toResponse(m, filesByMaterial.getOrDefault(m.getMaterialId(), List.of())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MaterialFileContent getFile(CurrentActor actor, UUID spaceId, UUID materialId, UUID fileId) {
        classroomService.loadAccessibleSpace(actor, spaceId);
        LearningMaterial material = findInSpace(materialId, spaceId);
        LearningMaterialFile file = fileRepository.findByFileIdAndMaterialId(fileId, material.getMaterialId())
                .orElseThrow(() -> ClassroomException.notFound("Không tìm thấy tệp trong tài liệu này."));
        byte[] data = fileRepository.findFileData(file.getFileId());
        if (data == null || data.length == 0) {
            throw ClassroomException.notFound("Tệp của tài liệu này hiện không còn trên hệ thống.");
        }
        return new MaterialFileContent(data, contentTypeOf(typeName(file.getFileName())), file.getFileName());
    }

    @Override
    @Transactional
    public LearningMaterialResponse upload(CurrentActor actor, UUID spaceId, String title, String description,
                                           List<MultipartFile> files) {
        LearningSpace space = classroomService.loadManageableSpace(actor, spaceId);
        String cleanTitle = normalizeTitle(title);
        String cleanDescription = normalizeDescription(description);
        List<MultipartFile> chosen = fileValidator.nonEmpty(files);
        if (chosen.isEmpty()) {
            throw ClassroomException.field("file", "Vui lòng chọn ít nhất một tệp tài liệu.");
        }
        List<Prepared> prepared = prepare(chosen);

        LearningMaterial saved = materialRepository.save(LearningMaterial.builder()
                .space(space)
                .title(cleanTitle)
                .description(cleanDescription)
                .fileUrl(prepared.get(0).fileName())
                .fileType(prepared.get(0).type().name())
                .uploadedBy(actor.user())
                .build());
        insertFiles(saved.getMaterialId(), prepared);
        return toResponse(saved, filesOf(saved.getMaterialId()));
    }

    @Override
    @Transactional
    public LearningMaterialResponse update(CurrentActor actor, UUID spaceId, UUID materialId, String title,
                                           String description, List<MultipartFile> files, List<UUID> removeFileIds) {
        classroomService.loadManageableSpace(actor, spaceId);
        LearningMaterial material = findInSpace(materialId, spaceId);
        String cleanTitle = normalizeTitle(title);
        String cleanDescription = normalizeDescription(description);

        List<MultipartFile> chosen = fileValidator.nonEmpty(files);
        List<LearningMaterialFile> current = filesOf(materialId);
        Set<UUID> toRemove = new HashSet<>(removeFileIds == null ? List.of() : removeFileIds);
        List<LearningMaterialFile> removed = current.stream().filter(f -> toRemove.contains(f.getFileId())).toList();
        if (removed.size() != toRemove.size()) {
            throw ClassroomException.notFound("Không tìm thấy tệp cần gỡ trong tài liệu này.");
        }
        int remaining = current.size() - removed.size() + chosen.size();
        if (remaining == 0) {
            throw ClassroomException.field("file", "Tài liệu cần có ít nhất một tệp.");
        }
        if (remaining > MaterialFileValidator.MAX_FILES) {
            throw ClassroomException.field("file", "Mỗi tài liệu có tối đa " + MaterialFileValidator.MAX_FILES + " tệp.");
        }
        List<Prepared> prepared = prepare(chosen);

        fileRepository.deleteAll(removed);
        insertFiles(materialId, prepared);
        List<LearningMaterialFile> after = filesOf(materialId);
        material.setTitle(cleanTitle);
        material.setDescription(cleanDescription);
        if (!after.isEmpty()) {
            material.setFileUrl(after.get(0).getFileName());
            String type = typeName(after.get(0).getFileName());
            if (type != null) material.setFileType(type);
        }
        return toResponse(materialRepository.save(material), after);
    }

    @Override
    @Transactional
    public void delete(CurrentActor actor, UUID spaceId, UUID materialId) {
        classroomService.loadManageableSpace(actor, spaceId);
        materialRepository.delete(findInSpace(materialId, spaceId));
    }

    // ---------------------------------------------------------------------

    private LearningMaterial findInSpace(UUID materialId, UUID spaceId) {
        return materialRepository.findByMaterialIdAndSpace_SpaceId(materialId, spaceId)
                .orElseThrow(() -> ClassroomException.notFound("Không tìm thấy tài liệu trong lớp học này."));
    }

    private List<LearningMaterialFile> filesOf(UUID materialId) {
        return fileRepository.findByMaterialIdOrderByCreatedAtAscFileNameAsc(materialId);
    }

    /** Kiểm tra và đọc hết các tệp trước khi ghi: lỗi ở tệp nào cũng không lưu dở dang. */
    private List<Prepared> prepare(List<MultipartFile> files) {
        return files.stream().map(f -> {
            MaterialFileType type;
            try {
                type = fileValidator.validate(f);
            } catch (ClassroomException e) {
                String name = f.getOriginalFilename() == null ? "" : f.getOriginalFilename()
                        .substring(Math.max(f.getOriginalFilename().lastIndexOf('/'), f.getOriginalFilename().lastIndexOf('\\')) + 1).strip();
                if (name.isEmpty()) throw e;
                String message = "Tệp \"" + name + "\": " + e.getMessage();
                throw new ClassroomException(e.getStatus(), message, java.util.Map.of("file", message));
            }
            return new Prepared(type, cleanFileName(f.getOriginalFilename(), type), readBytes(f));
        }).toList();
    }

    private void insertFiles(UUID materialId, List<Prepared> prepared) {
        for (Prepared p : prepared) {
            fileRepository.insertFile(UUID.randomUUID(), materialId, p.fileName(), p.bytes().length, p.bytes());
        }
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw ClassroomException.field("file", "Không đọc được tệp. Vui lòng chọn lại tệp khác.");
        }
    }

    /** Bỏ đường dẫn, ký tự điều khiển và ký tự không hợp lệ; luôn kết thúc bằng phần mở rộng đã xác minh. */
    static String cleanFileName(String original, MaterialFileType type) {
        String name = original == null ? "" : original;
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        name = name.replaceAll("[\\p{Cntrl}<>:\"|?*]", "").strip();
        int dot = name.lastIndexOf('.');
        String base = (dot >= 0 ? name.substring(0, dot) : name).strip();
        if (base.isEmpty()) base = "tai-lieu";
        if (base.length() > FILE_NAME_MAX) base = base.substring(0, FILE_NAME_MAX).strip();
        return base + "." + type.extension;
    }

    /** Loại tệp (PDF, DOCX, ...) suy ra từ phần mở rộng của tên đã làm sạch; null nếu không xác định. */
    static String typeName(String fileName) {
        if (fileName == null) return null;
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(java.util.Locale.ROOT);
        for (MaterialFileType t : MaterialFileType.values()) {
            if (t.extension.equals(ext)) return t.name();
        }
        return "jpeg".equals(ext) ? MaterialFileType.JPG.name() : null;
    }

    static String contentTypeOf(String fileType) {
        try {
            return MaterialFileType.valueOf(fileType).contentType;
        } catch (IllegalArgumentException | NullPointerException e) {
            return "application/octet-stream";
        }
    }

    static String normalizeTitle(String raw) {
        String title = raw == null ? "" : raw.strip();
        if (title.isEmpty()) throw ClassroomException.field("title", "Vui lòng nhập tiêu đề tài liệu.");
        if (title.length() > TITLE_MAX) {
            throw ClassroomException.field("title", "Tiêu đề tài liệu tối đa " + TITLE_MAX + " ký tự.");
        }
        return title;
    }

    static String normalizeDescription(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String description = raw.strip();
        if (description.length() > DESCRIPTION_MAX) {
            throw ClassroomException.field("description", "Mô tả tối đa " + DESCRIPTION_MAX + " ký tự.");
        }
        return description;
    }

    private static LearningMaterialResponse toResponse(LearningMaterial m, List<LearningMaterialFile> files) {
        String uploader = m.getUploadedBy() != null ? m.getUploadedBy().getFullName() : null;
        List<MaterialFileResponse> fileList = files.stream()
                .map(f -> new MaterialFileResponse(f.getFileId(), f.getFileName(), typeName(f.getFileName()),
                        f.getFileSize() == null ? 0 : f.getFileSize()))
                .toList();
        return new LearningMaterialResponse(m.getMaterialId(), m.getTitle(), m.getDescription(), m.getFileType(),
                fileList, m.getCreatedAt(), uploader);
    }
}
