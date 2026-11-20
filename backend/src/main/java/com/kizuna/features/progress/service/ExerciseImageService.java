package com.kizuna.features.progress.service;

import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.progress.dto.ExerciseDtos.ImageContent;
import com.kizuna.features.progress.dto.ExerciseDtos.ImageInfo;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/** Ảnh minh họa cho câu hỏi bài làm trực tiếp (FE-07): lưu R2 khi bật, ngược lại lưu DB (giống các tệp khác của nhóm). */
public interface ExerciseImageService {

    /** Gia sư của lớp tải lên một ảnh; ảnh được gắn vào bài khi lưu bài. */
    ImageInfo upload(CurrentActor actor, UUID spaceId, MultipartFile file);

    /** Nội dung ảnh. Gia sư của lớp, hoặc học viên của lớp khi ảnh thuộc bài đã giao và đã mở. */
    ImageContent content(CurrentActor actor, UUID spaceId, UUID imageId);
}
