package com.kizuna.features.classroom.service;

import com.kizuna.features.classroom.dto.LearningMaterialResponse;
import com.kizuna.features.classroom.dto.MaterialFileContent;
import com.kizuna.features.classroom.util.CurrentActor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * FE-06: tài liệu học tập (UC-61 View, UC-65 Upload, UC-94 Update, Delete Learning Material).
 * Một tài liệu = tiêu đề (bắt buộc) + mô tả + 1–10 tệp (PDF, DOC, DOCX, PNG, JPG, MP3; mỗi tệp ≤ 25 MB).
 */
public interface LearningMaterialService {

    List<LearningMaterialResponse> list(CurrentActor actor, UUID spaceId);

    /** Nội dung một tệp của tài liệu. */
    MaterialFileContent getFile(CurrentActor actor, UUID spaceId, UUID materialId, UUID fileId);

    /** Tạo một tài liệu gồm 1–10 tệp. */
    LearningMaterialResponse upload(CurrentActor actor, UUID spaceId, String title, String description,
                                    List<MultipartFile> files);

    /**
     * Sửa tiêu đề/mô tả; gỡ các tệp trong removeFileIds; thêm các tệp mới (thay tệp = gỡ tệp cũ + thêm tệp mới).
     * Sau khi sửa tài liệu phải còn 1–10 tệp.
     */
    LearningMaterialResponse update(CurrentActor actor, UUID spaceId, UUID materialId, String title, String description,
                                    List<MultipartFile> files, List<UUID> removeFileIds);

    /** Xóa hẳn tài liệu (bản ghi và mọi tệp). Chỉ gia sư của lớp, khi lớp không ở chế độ chỉ xem. */
    void delete(CurrentActor actor, UUID spaceId, UUID materialId);
}
