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
import com.kizuna.features.progress.service.ExerciseListenService;
import com.kizuna.features.storage.R2StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
 *   Câu có giới hạn số lần nghe: lúc làm bài học viên nghe qua {@link ExerciseListenService} (máy chủ đếm lần nghe).
 * - Kiểm tra cả phần đầu tệp (MP3, WAV, OGG, M4A, WEBM), không chỉ đuôi tên / MIME.
 * - Dọn file: file không gắn với bài nào (vừa tải chưa lưu, câu hỏi đã bỏ file, bài đã xóa) quá 1 ngày được xóa
 *   trong DB và trên R2 (sau khi commit) mỗi khi gia sư của lớp tải file mới; tải lên lỗi thì tệp vừa tải được xóa.
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
    /** Đếm số lần nghe ở máy chủ; null (test cũ) = không chặn link trực tiếp. */
    private final ExerciseListenService listen;

    @Autowired
    public ExerciseAudioServiceImpl(ClassroomService classroomService, ExerciseAudioRepository audios,
                                    OnlineExerciseRepository exercises, ObjectProvider<R2StorageService> storage,
                                    ObjectProvider<S3Client> s3,
                                    @Value("${app.r2.bucket:}") String bucket,
                                    @Value("${app.r2.presigned-url-ttl-seconds:900}") long urlTtlSeconds,
                                    ExerciseListenService listen) {
        this(classroomService, audios, exercises, storage, s3, bucket, urlTtlSeconds, Clock.system(VN), listen);
    }

    public ExerciseAudioServiceImpl(ClassroomService classroomService, ExerciseAudioRepository audios,
                                    OnlineExerciseRepository exercises, ObjectProvider<R2StorageService> storage,
                                    ObjectProvider<S3Client> s3, String bucket, long urlTtlSeconds, Clock clock) {
        this(classroomService, audios, exercises, storage, s3, bucket, urlTtlSeconds, clock, null);
    }

    public ExerciseAudioServiceImpl(ClassroomService classroomService, ExerciseAudioRepository audios,
                                    OnlineExerciseRepository exercises, ObjectProvider<R2StorageService> storage,
                                    ObjectProvider<S3Client> s3, String bucket, long urlTtlSeconds, Clock clock,
                                    ExerciseListenService listen) {
        this.listen = listen;
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
        if (detectAudio(file) == null) {
            throw ClassroomException.field("file", "Tệp không phải file âm thanh hợp lệ. Chỉ chấp nhận tệp MP3, WAV, OGG, M4A hoặc WEBM.");
        }
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
        // Ghi DB lỗi / transaction rollback: xóa tệp vừa tải để không bỏ lại tệp mồ côi trên R2
        deleteOnRollback(uploaded.objectKey());
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
            // Câu có giới hạn số lần nghe: đang làm bài (hoặc chưa nộp lượt nào) thì phải nghe qua API đếm lần nghe
            if (listen != null && listen.requiresCounting(actor.learner().getLearnerId(), e.getExerciseId(), audioId)) {
                throw ClassroomException.forbidden("File nghe này có giới hạn số lần nghe. Hãy nghe trong lúc làm bài.");
            }
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

    /**
     * Xóa các file không gắn với bài nào quá 1 ngày của lớp: xóa trong DB ngay, trên R2 chỉ sau khi transaction commit
     * (tải lên lỗi → rollback thì bản ghi và tệp vẫn còn nguyên). Lỗi xóa trên R2 không chặn việc tải lên.
     */
    void cleanUp(UUID spaceId) {
        List<ExerciseAudio> stale = audios.findUnlinkedBefore(spaceId, OffsetDateTime.now(clock).minusDays(1));
        if (stale.isEmpty()) return;
        audios.deleteAll(stale);
        List<String> keys = stale.stream().map(ExerciseAudio::getStorageKey).toList();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    keys.forEach(ExerciseAudioServiceImpl.this::deleteQuietly);
                }
            });
        } else {
            keys.forEach(this::deleteQuietly);
        }
    }

    /** Transaction rollback (ghi DB lỗi…) thì xóa tệp vừa tải lên R2. */
    private void deleteOnRollback(String key) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) deleteQuietly(key);
            }
        });
    }

    private void deleteQuietly(String key) {
        S3Client client = s3.getIfAvailable();
        if (client == null || bucket == null || bucket.isBlank() || key == null) return;
        try {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (SdkException e) {
            log.warn("[FE-07] Không xóa được file nghe trên R2 ({}): {}", key, e.getMessage());
        }
    }

    /**
     * Nhận dạng file âm thanh theo phần đầu tệp (không chỉ đuôi tên / MIME); null nếu không phải MP3, WAV, OGG, M4A, WEBM.
     */
    public static String detectAudio(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        byte[] d = new byte[12];
        int n;
        try (InputStream in = file.getInputStream()) {
            n = in.readNBytes(d, 0, d.length);
        } catch (IOException e) {
            return null;
        }
        return detectAudio(d, n);
    }

    public static String detectAudio(byte[] d, int n) {
        if (n >= 3 && d[0] == 'I' && d[1] == 'D' && d[2] == '3') return "audio/mpeg";               // MP3 có thẻ ID3
        if (n >= 2 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xE0) == 0xE0) return "audio/mpeg";          // MP3 / AAC frame
        if (n >= 12 && ascii(d, 0, 4).equals("RIFF") && ascii(d, 8, 4).equals("WAVE")) return "audio/wav";
        if (n >= 4 && ascii(d, 0, 4).equals("OggS")) return "audio/ogg";
        if (n >= 8 && ascii(d, 4, 4).equals("ftyp")) return "audio/mp4";                             // M4A
        if (n >= 4 && (d[0] & 0xFF) == 0x1A && (d[1] & 0xFF) == 0x45 && (d[2] & 0xFF) == 0xDF && (d[3] & 0xFF) == 0xA3) {
            return "audio/webm";
        }
        return null;
    }

    private static String ascii(byte[] d, int from, int len) {
        return new String(d, from, len, StandardCharsets.US_ASCII);
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
