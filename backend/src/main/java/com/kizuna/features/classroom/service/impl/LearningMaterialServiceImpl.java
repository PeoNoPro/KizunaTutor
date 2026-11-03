package com.kizuna.features.classroom.service.impl;

import com.kizuna.features.classroom.dto.LearningMaterialResponse;
import com.kizuna.features.classroom.dto.MaterialFileLinkResponse;
import com.kizuna.features.classroom.entity.LearningMaterial;
import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.repository.LearningMaterialRepository;
import com.kizuna.features.classroom.service.ClassroomService;
import com.kizuna.features.classroom.service.LearningMaterialService;
import com.kizuna.features.classroom.service.MaterialStorageService;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.classroom.util.MaterialFileValidator;
import com.kizuna.features.classroom.util.MaterialFileValidator.MaterialFileType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Tệp được tải lên kho trước, sau đó mới lưu bản ghi. Nếu lưu bản ghi thất bại thì xóa tệp vừa tải
 * để không có tệp mồ côi. Khi thay tệp, tệp cũ chỉ bị xóa sau khi bản ghi mới được lưu thành công.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LearningMaterialServiceImpl implements LearningMaterialService {

    static final int TITLE_MAX = 150;
    static final int DESCRIPTION_MAX = 1000;

    private final ClassroomService classroomService;
    private final LearningMaterialRepository materialRepository;
    private final MaterialStorageService storage;
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
    public MaterialFileLinkResponse getFileLink(CurrentActor actor, UUID spaceId, UUID materialId) {
        classroomService.loadAccessibleSpace(actor, spaceId);
        LearningMaterial material = findInSpace(materialId, spaceId);
        return new MaterialFileLinkResponse(storage.createSignedUrl(material.getFileUrl()), storage.signedUrlTtlSeconds());
    }

    @Override
    public LearningMaterialResponse upload(CurrentActor actor, UUID spaceId, String title, String description,
                                           MultipartFile file) {
        LearningSpace space = classroomService.loadManageableSpace(actor, spaceId);
        String cleanTitle = normalizeTitle(title);
        String cleanDescription = normalizeDescription(description);
        MaterialFileType type = fileValidator.validate(file);
        if (!storage.isConfigured()) throw ClassroomException.storageUnavailable();

        String path = storeFile(spaceId, type, file);
        try {
            LearningMaterial saved = materialRepository.save(LearningMaterial.builder()
                    .space(space)
                    .title(cleanTitle)
                    .description(cleanDescription)
                    .fileUrl(path)
                    .fileType(type.name())
                    .uploadedBy(actor.user())
                    .build());
            return toResponse(saved);
        } catch (RuntimeException e) {
            storage.delete(path);
            throw e;
        }
    }

    @Override
    public LearningMaterialResponse update(CurrentActor actor, UUID spaceId, UUID materialId, String title,
                                           String description, MultipartFile file) {
        classroomService.loadManageableSpace(actor, spaceId);
        LearningMaterial material = findInSpace(materialId, spaceId);
        String cleanTitle = normalizeTitle(title);
        String cleanDescription = normalizeDescription(description);

        boolean replaceFile = file != null && !file.isEmpty();
        String oldPath = material.getFileUrl();
        String oldType = material.getFileType();
        String newPath = null;
        if (replaceFile) {
            MaterialFileType type = fileValidator.validate(file);
            if (!storage.isConfigured()) throw ClassroomException.storageUnavailable();
            newPath = storeFile(spaceId, type, file);
            material.setFileUrl(newPath);
            material.setFileType(type.name());
        }
        material.setTitle(cleanTitle);
        material.setDescription(cleanDescription);

        LearningMaterial saved;
        try {
            saved = materialRepository.save(material);
        } catch (RuntimeException e) {
            if (newPath != null) {
                storage.delete(newPath);
                material.setFileUrl(oldPath);
                material.setFileType(oldType);
            }
            throw e;
        }
        if (newPath != null) {
            storage.delete(oldPath);
        }
        return toResponse(saved);
    }

    private LearningMaterial findInSpace(UUID materialId, UUID spaceId) {
        return materialRepository.findByMaterialIdAndSpace_SpaceId(materialId, spaceId)
                .orElseThrow(() -> ClassroomException.notFound("Không tìm thấy tài liệu trong lớp học này."));
    }

    private String storeFile(UUID spaceId, MaterialFileType type, MultipartFile file) {
        String path = "spaces/" + spaceId + "/" + UUID.randomUUID() + "." + type.extension;
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw ClassroomException.field("file", "Không đọc được tệp. Vui lòng chọn lại tệp khác.");
        }
        storage.upload(path, bytes, type.contentType);
        return path;
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
