package com.kizuna.features.classroom.service;

import com.kizuna.features.classroom.dto.LearningMaterialResponse;
import com.kizuna.features.classroom.dto.MaterialFileContent;
import com.kizuna.features.classroom.util.CurrentActor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/** FE-06: tài liệu học tập (UC-61 View, UC-65 Upload, UC-94 Update, Delete Learning Material). */
public interface LearningMaterialService {

    List<LearningMaterialResponse> list(CurrentActor actor, UUID spaceId);

    MaterialFileContent getFile(CurrentActor actor, UUID spaceId, UUID materialId);

    LearningMaterialResponse upload(CurrentActor actor, UUID spaceId, String title, String description, MultipartFile file);

    /** file == null hoặc rỗng: chỉ sửa tiêu đề/mô tả. */
    LearningMaterialResponse update(CurrentActor actor, UUID spaceId, UUID materialId,
                                    String title, String description, MultipartFile file);

    /** Xóa hẳn tài liệu (bản ghi và nội dung tệp). Chỉ gia sư của lớp, khi lớp không ở chế độ chỉ xem. */
    void delete(CurrentActor actor, UUID spaceId, UUID materialId);
}
