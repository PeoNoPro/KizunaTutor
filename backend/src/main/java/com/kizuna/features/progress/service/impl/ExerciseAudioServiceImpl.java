package com.kizuna.features.progress.service.impl;

import com.kizuna.core.exception.BadRequestException;
import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.service.ClassroomService;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.progress.dto.ExerciseDtos.AudioInfo;
import com.kizuna.features.progress.dto.ExerciseDtos.AudioUrl;
import com.kizuna.features.progress.entity.ExerciseAudio;
import com.kizuna.features.progress.entity.OnlineExercise;
import com.kizuna.features.progress.repository.ExerciseAudioRepository;
import com.kizuna.features.progress.repository.OnlineExerciseRepository;
import com.kizuna.features.progress.service.ExerciseAudioService;
import com.kizuna.features.storage.R2StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * File nghe cho câu hỏi bài làm trực tiếp (FE-07), lưu trên Cloudflare R2 qua {@link R2StorageService} dùng chung của nhóm.
 * - Gia sư của lớp (lớp đang hoạt động, gói còn hiệu lực) tải lên; định dạng và dung lượng theo R2StorageService
 *   (MP3, WAV, OGG, M4A, WEBM; tối đa 25 MB). DB chỉ lưu storage_key.
 * - Nghe: trả link tạm có hạn. Gia sư của lớp; học viên của lớp chỉ khi file thuộc bài đã giao và đã tới giờ mở.
 * - Dọn file: file không gắn với bài nào (vừa tải chưa lưu, câu hỏi đã bỏ file, bài đã xóa) quá 1 ngày được xóa
 *   cả trên R2 và trong DB mỗi khi gia sư của lớp tải file mới.
 * - Máy chưa cấu hình R2 (R2_ENABLED khác true): tải lên / nghe báo "chưa cấu hình", các phần khác vẫn chạy.
 */
@Slf4j
@Service
public class ExerciseAudioServiceImpl implements ExerciseAudioService {

    public static final String NOT_CONFIGURED_MESSAGE =
            "Kho lưu file nghe (Cloudflare R2) chưa được cấu hình trên máy chủ. Vui lòng liên hệ quản trị viên.";
    static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ClassroomService classroomService;
    private final ExerciseAudioRepository audios;
    private final OnlineExerciseRepository exercises;
    private final ObjectProvider<R2StorageService> storage;
    private final ObjectProvider<S3Client> s3;
    private final String bucket;
    private final long urlTtlSeconds;
    private final Clock clock;

    @Autowired
    public ExerciseAudioServiceImpl(ClassroomService classroomService, ExerciseAudioRepository audios,
                                    OnlineExerciseRepository exercises, ObjectProvider<R2StorageService> storage,
                                    ObjectProvider<S3Client> s3,
                                    @Value("${app.r2.bucket:}") String bucket,
                                    @Value("${app.r2.presigned-url-ttl-seconds:900}") long urlTtlSeconds) {
        this(classroomService, audios, exercises, storage, s3, bucket, urlTtlSeconds, Clock.system(VN));
    }

    public ExerciseAudioServiceImpl(ClassroomService classroomService, ExerciseAudioRepository audios,
                                    OnlineExerciseRepository exercises, ObjectProvider<R2StorageService> storage,
                                    ObjectProvider<S3Client> s3, String bucket, long urlTtlSeconds, Clock clock) {
        this.classroomService = classroomService;
        this.audios = audios;
        this.exercises = exercises;
        this.storage = storage;
        this.s3 = s3;
        this.bucket = bucket;
        this.urlTtlSeconds = urlTtlSeconds;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AudioInfo upload(CurrentActor actor, UUID spaceId, MultipartFile file) {
        if (!actor.isTutor()) throw ClassroomException.forbidden("Chỉ gia sư của lớp mới được tải file nghe.");
        LearningSpace space = classroomService.loadManageableSpace(actor, spaceId);
        R2StorageService r2 = requireStorage();
        cleanUp(space.getSpaceId());
        R2StorageService.UploadedAudio uploaded;
        try {
            uploaded = r2.uploadAudio(file);
        } catch (BadRequestException e) {
            throw ClassroomException.field("file", e.getMessage());
        } catch (SdkException e) {
            log.warn("[FE-07] Không tải được file nghe lên R2: {}", e.getMessage());
            throw unavailable();
        }
        String name = cleanName(file.getOriginalFilename());
        ExerciseAudio saved = audios.save(ExerciseAudio.builder()
                .audioId(UUID.randomUUID())
                .spaceId(space.getSpaceId())
                .fileName(name)
                .fileSize(file.getSize())
                .contentType(uploaded.contentType())
                .storageKey(uploaded.objectKey())
                .build());
        return new AudioInfo(saved.getAudioId(), saved.getFileName(), saved.getFileSize(), null);
    }

    @Override
    @Transactional(readOnly = true)
    public AudioUrl url(CurrentActor actor, UUID spaceId, UUID audioId) {
        LearningSpace space = classroomService.loadAccessibleSpace(actor, spaceId);
        ExerciseAudio audio = audios.findByAudioIdAndSpaceId(audioId, spaceId).orElseThrow(ExerciseAudioServiceImpl::notFound);
        boolean tutor = actor.isTutor() && Objects.equals(actor.tutor().getTutorId(), space.getTutor().getTutorId());
        if (!tutor) {
            boolean learner = actor.isLearner()
                    && Objects.equals(actor.learner().getLearnerId(), space.getLearner().getLearnerId());
            if (!learner) throw ClassroomException.forbidden("Bạn không có quyền nghe file này.");
            OnlineExercise e = audio.getExerciseId() == null ? null
                    : exercises.findByExerciseIdAndSpace_SpaceId(audio.getExerciseId(), spaceId).orElse(null);
            if (e == null || !OnlineExerciseServiceImpl.visibleToLearner(e, OffsetDateTime.now(clock))) throw notFound();
        }
        try {
            return new AudioUrl(requireStorage().presignedGetUrl(audio.getStorageKey()), urlTtlSeconds);
        } catch (BadRequestException e) {
            throw notFound();
        } catch (SdkException e) {
            log.warn("[FE-07] Không tạo được link nghe R2: {}", e.getMessage());
            throw unavailable();
        }
    }

    /** Xóa (trên R2 và trong DB) các file không gắn với bài nào quá 1 ngày của lớp. Lỗi xóa trên R2 không chặn việc tải lên. */
    void cleanUp(UUID spaceId) {
        List<ExerciseAudio> stale = audios.findUnlinkedBefore(spaceId, OffsetDateTime.now(clock).minusDays(1));
        if (stale.isEmpty()) return;
        S3Client client = s3.getIfAvailable();
        if (client == null || bucket == null || bucket.isBlank()) return;
        for (ExerciseAudio a : stale) {
            try {
                client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(a.getStorageKey()).build());
                audios.delete(a);
            } catch (SdkException e) {
                log.warn("[FE-07] Không xóa được file nghe cũ trên R2 ({}): {}", a.getStorageKey(), e.getMessage());
            }
        }
    }

    private R2StorageService requireStorage() {
        R2StorageService r2 = storage.getIfAvailable();
        if (r2 == null) throw new ClassroomException(HttpStatus.SERVICE_UNAVAILABLE, NOT_CONFIGURED_MESSAGE);
        return r2;
    }

    /** Tên hiển thị: bỏ đường dẫn và ký tự không hợp lệ, tối đa 200 ký tự. */
    static String cleanName(String original) {
        String name = original == null ? "" : original;
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        name = name.replaceAll("[\\p{Cntrl}<>:\"|?*]", "").strip();
        if (name.isEmpty()) name = "file-nghe";
        return name.length() > 200 ? name.substring(name.length() - 200) : name;
    }

    private static ClassroomException unavailable() {
        return new ClassroomException(HttpStatus.SERVICE_UNAVAILABLE,
                "Kho lưu file nghe đang không phản hồi. Vui lòng thử lại sau.");
    }

    private static ClassroomException notFound() {
        return ClassroomException.notFound("Không tìm thấy file nghe.");
    }
}
