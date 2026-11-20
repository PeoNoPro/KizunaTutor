package com.kizuna.features.progress.service;

import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.progress.dto.ExerciseDtos.AudioInfo;
import com.kizuna.features.progress.dto.ExerciseDtos.AudioUrl;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/** File nghe cho câu hỏi bài làm trực tiếp (FE-07), lưu trên Cloudflare R2. */
public interface ExerciseAudioService {

    /** Gia sư của lớp tải lên một file âm thanh; file được gắn vào bài khi lưu bài. */
    AudioInfo upload(CurrentActor actor, UUID spaceId, MultipartFile file);

    /** Link nghe tạm (có hạn). Gia sư của lớp, hoặc học viên của lớp khi bài đã giao và đã mở. */
    AudioUrl url(CurrentActor actor, UUID spaceId, UUID audioId);
}
