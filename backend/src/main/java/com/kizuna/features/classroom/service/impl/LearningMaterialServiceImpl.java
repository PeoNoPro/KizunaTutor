package com.kizuna.features.classroom.service.impl;

import com.kizuna.features.classroom.dto.LearningMaterialResponse;
import com.kizuna.features.classroom.dto.MaterialFileContent;
import com.kizuna.features.classroom.entity.LearningMaterial;
import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.exception.ClassroomException;
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
import java.util.List;
import java.util.UUID;

/**
 * Tài liệu học tập FE-06. Nội dung tệp lưu trực tiếp trong cột learning_materials.file_data (BYTEA);
 * cột file_url lưu tên tệp gốc đã làm sạch. Bản ghi và nội dung tệp được ghi trong cùng một giao dịch
 * nên lỗi ở bất kỳ bước nào cũng không để lại dữ liệu dở dang.
 */
@Service
@RequiredArgsConstructor
public class LearningMaterialServiceImpl implements LearningMaterialService {

    static final int TITLE_MAX = 150;
    static final int DESCRIPTION_MAX = 1000;
    static final int FILE_NAME_MAX = 200;

    private final ClassroomService classroomService;
    private final LearningMaterialRepository materialRepository;
    private final MaterialFileValidator fileValidator;

    @Override
    @Transactional(readOnly = true)
    public List<LearningMaterialResponse> list(CurrentActor actor, UUID spaceId) {
        classroomService.loadAccessibleSpace(actor, spaceId);
        return materialRepository.findBySpace_SpaceIdOrderByCreatedAtDesc(spaceId).stream()
                .map(LearningMaterialServiceImpl::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MaterialFileContent getFile(CurrentActor actor, UUID spaceId, UUID materialId) {
        classroomService.loadAccessibleSpace(actor, spaceId);
        LearningMaterial material = findInSpace(materialId, spaceId);
        byte[] data = materialRepository.findFileData(material.getMaterialId());
        if (data == null || data.length == 0) {
            throw ClassroomException.notFound("Tệp của tài liệu này hiện không còn trên hệ thống.");
        }
        return new MaterialFileContent(data, contentTypeOf(material.getFileType()), material.getFileUrl());
    }

    @Override
    @Transactional
    public LearningMaterialResponse upload(CurrentActor actor, UUID spaceId, String title, String description,
                                           MultipartFile file) {
        LearningSpace space = classroomService.loadManageableSpace(actor, spaceId);
        String cleanTitle = normalizeTitle(title);
        String cleanDescription = normalizeDescription(description);
        MaterialFileType type = fileValidator.validate(file);
        byte[] bytes = readBytes(file);

        LearningMaterial saved = materialRepository.save(LearningMaterial.builder()
                .space(space)
                .title(cleanTitle)
                .description(cleanDescription)
                .fileUrl(cleanFileName(file.getOriginalFilename(), type))
                .fileType(type.name())
                .uploadedBy(actor.user())
                .build());
        materialRepository.updateFileData(saved.getMaterialId(), bytes);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public LearningMaterialResponse update(CurrentActor actor, UUID spaceId, UUID materialId, String title,
                                           String description, MultipartFile file) {
        classroomService.loadManageableSpace(actor, spaceId);
        LearningMaterial material = findInSpace(materialId, spaceId);
        String cleanTitle = normalizeTitle(title);
        String cleanDescription = normalizeDescription(description);

        byte[] newBytes = null;
        MaterialFileType newType = null;
        if (file != null && !file.isEmpty()) {
            newType = fileValidator.validate(file);
            newBytes = readBytes(file);
        }

        material.setTitle(cleanTitle);
        material.setDescription(cleanDescription);
        if (newBytes != null) {
            material.setFileUrl(cleanFileName(file.getOriginalFilename(), newType));
            material.setFileType(newType.name());
        }
        LearningMaterial saved = materialRepository.save(material);
        if (newBytes != null) {
            materialRepository.updateFileData(saved.getMaterialId(), newBytes);
        }
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(CurrentActor actor, UUID spaceId, UUID materialId) {
        classroomService.loadManageableSpace(actor, spaceId);
        materialRepository.delete(findInSpace(materialId, spaceId));
    }

    private LearningMaterial findInSpace(UUID materialId, UUID spaceId) {
        return materialRepository.findByMaterialIdAndSpace_SpaceId(materialId, spaceId)
                .orElseThrow(() -> ClassroomException.notFound("Không tìm thấy tài liệu trong lớp học này."));
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

    private static LearningMaterialResponse toResponse(LearningMaterial m) {
        String uploader = m.getUploadedBy() != null ? m.getUploadedBy().getFullName() : null;
        return new LearningMaterialResponse(m.getMaterialId(), m.getTitle(), m.getDescription(), m.getFileType(),
                m.getCreatedAt(), uploader);
    }
}
