package com.kizuna.features.progress.service.impl;

import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.service.ClassroomService;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.progress.dto.ExerciseDtos.ImageContent;
import com.kizuna.features.progress.dto.ExerciseDtos.ImageInfo;
import com.kizuna.features.progress.entity.ExerciseImage;
import com.kizuna.features.progress.entity.OnlineExercise;
import com.kizuna.features.progress.repository.ExerciseImageRepository;
import com.kizuna.features.progress.repository.OnlineExerciseRepository;
import com.kizuna.features.progress.service.ExerciseImageService;
import com.kizuna.features.storage.R2FileStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Ảnh minh họa cho câu hỏi bài làm trực tiếp (FE-07).
 * - Lưu như các tệp khác của nhóm qua {@link R2FileStore}: bật R2 thì nội dung lên R2 (khóa "exercise-images/&lt;id&gt;"),
 *   cột image_data để NULL; tắt R2 thì lưu trong cột image_data.
 * - Chỉ nhận PNG, JPG, WEBP, GIF tối đa 5 MB; kiểm tra cả phần đầu tệp, không chỉ đuôi tên.
 * - Xem ảnh: gia sư của lớp; học viên của lớp chỉ khi ảnh thuộc bài đã giao và đã tới giờ mở.
 * - Ảnh không gắn với bài nào quá 1 ngày được xóa (cả trên R2) mỗi khi gia sư của lớp tải ảnh mới.
 */
@Service
public class ExerciseImageServiceImpl implements ExerciseImageService {

    public static final long MAX_BYTES = 5L * 1024 * 1024;
    static final String RULES = "Chỉ nhận ảnh PNG, JPG, WEBP hoặc GIF, tối đa 5 MB.";

    private final ClassroomService classroomService;
    private final ExerciseImageRepository images;
    private final OnlineExerciseRepository exercises;
    private final R2FileStore r2;
    private final Clock clock;

    @Autowired
    public ExerciseImageServiceImpl(ClassroomService classroomService, ExerciseImageRepository images,
                                    OnlineExerciseRepository exercises, R2FileStore r2) {
        this(classroomService, images, exercises, r2, Clock.system(OnlineExerciseServiceImpl.VN));
    }

    public ExerciseImageServiceImpl(ClassroomService classroomService, ExerciseImageRepository images,
                                    OnlineExerciseRepository exercises, R2FileStore r2, Clock clock) {
        this.classroomService = classroomService;
        this.images = images;
        this.exercises = exercises;
        this.r2 = r2;
        this.clock = clock;
    }

    static String r2Key(UUID imageId) {
        return "exercise-images/" + imageId;
    }

    @Override
    @Transactional
    public ImageInfo upload(CurrentActor actor, UUID spaceId, MultipartFile file) {
        if (!actor.isTutor()) throw ClassroomException.forbidden("Chỉ gia sư của lớp mới được tải ảnh lên.");
        LearningSpace space = classroomService.loadManageableSpace(actor, spaceId);
        if (file == null || file.isEmpty()) throw ClassroomException.field("file", "Vui lòng chọn ảnh. " + RULES);
        if (file.getSize() > MAX_BYTES) throw ClassroomException.field("file", "Ảnh vượt quá 5 MB. " + RULES);
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw ClassroomException.field("file", "Không đọc được ảnh. Vui lòng thử lại.");
        }
        String contentType = detectType(data);
        if (contentType == null) throw ClassroomException.field("file", "Tệp không phải ảnh hợp lệ. " + RULES);
        cleanUp(space.getSpaceId());
        UUID imageId = UUID.randomUUID();
        String name = ExerciseAudioServiceImpl.cleanName(file.getOriginalFilename());
        if (name.equals("file-nghe")) name = "anh";
        byte[] dbData = r2.put(r2Key(imageId), data, contentType);
        images.insertImage(imageId, space.getSpaceId(), name, data.length, contentType, dbData);
        return new ImageInfo(imageId, name, data.length);
    }

    @Override
    @Transactional(readOnly = true)
    public ImageContent content(CurrentActor actor, UUID spaceId, UUID imageId) {
        LearningSpace space = classroomService.loadAccessibleSpace(actor, spaceId);
        ExerciseImage image = images.findByImageIdAndSpaceId(imageId, spaceId).orElseThrow(ExerciseImageServiceImpl::notFound);
        boolean tutor = actor.isTutor() && Objects.equals(actor.tutor().getTutorId(), space.getTutor().getTutorId());
        if (!tutor) {
            boolean learner = actor.isLearner()
                    && Objects.equals(actor.learner().getLearnerId(), space.getLearner().getLearnerId());
            if (!learner) throw ClassroomException.forbidden("Bạn không có quyền xem ảnh này.");
            OnlineExercise e = image.getExerciseId() == null ? null
                    : exercises.findByExerciseIdAndSpace_SpaceId(image.getExerciseId(), spaceId).orElse(null);
            if (e == null || !OnlineExerciseServiceImpl.visibleToLearner(e, OffsetDateTime.now(clock))) throw notFound();
        }
        byte[] data = r2.read(images.findImageData(imageId), r2Key(imageId));
        if (data == null || data.length == 0) throw notFound();
        return new ImageContent(data, image.getContentType(), image.getFileName());
    }

    /** Xóa ảnh không gắn với bài nào quá 1 ngày của lớp (trong DB, và trên R2 sau khi commit). */
    void cleanUp(UUID spaceId) {
        List<ExerciseImage> stale = images.findUnlinkedBefore(spaceId, OffsetDateTime.now(clock).minusDays(1));
        if (stale.isEmpty()) return;
        images.deleteAll(stale);
        r2.deleteAfterCommit(stale.stream().map(i -> r2Key(i.getImageId())).toList());
    }

    /** Nhận dạng ảnh theo phần đầu tệp; null nếu không phải PNG / JPG / GIF / WEBP. */
    static String detectType(byte[] d) {
        if (d == null || d.length < 12) return null;
        if ((d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G') return "image/png";
        if ((d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) return "image/jpeg";
        if (d[0] == 'G' && d[1] == 'I' && d[2] == 'F' && d[3] == '8') return "image/gif";
        if (d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F'
                && new String(d, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).toUpperCase(Locale.ROOT).equals("WEBP")) {
            return "image/webp";
        }
        return null;
    }

    private static ClassroomException notFound() {
        return ClassroomException.notFound("Không tìm thấy ảnh.");
    }
}
