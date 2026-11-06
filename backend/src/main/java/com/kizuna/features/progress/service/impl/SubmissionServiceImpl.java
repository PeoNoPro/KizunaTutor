package com.kizuna.features.progress.service.impl;

import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.service.ClassroomService;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.classroom.util.MaterialFileValidator;
import com.kizuna.features.classroom.util.MaterialFileValidator.MaterialFileType;
import com.kizuna.features.progress.dto.GradeRequest;
import com.kizuna.features.progress.dto.SubmissionFileContent;
import com.kizuna.features.progress.dto.SubmissionResponse;
import com.kizuna.features.progress.entity.Assignment;
import com.kizuna.features.progress.entity.AssignmentSubmission;
import com.kizuna.features.progress.entity.AssignmentSubmissionStatus;
import com.kizuna.features.progress.mapper.AssignmentMapper;
import com.kizuna.features.progress.repository.AssignmentRepository;
import com.kizuna.features.progress.repository.AssignmentSubmissionRepository;
import com.kizuna.features.progress.service.SubmissionService;
import com.kizuna.features.progress.util.AssignmentRules;
import com.kizuna.features.progress.util.SubmissionFileSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Nộp bài / nộp lại / chấm điểm theo quy tắc trong {@link AssignmentRules}.
 * Bản ghi và nội dung tệp được ghi trong cùng một giao dịch.
 */
@Service
public class SubmissionServiceImpl implements SubmissionService {

    static final int CONTENT_MAX = 5000;
    static final int FEEDBACK_MAX = 5000;

    private final ClassroomService classroomService;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final MaterialFileValidator fileValidator;
    private final Clock clock;

    @Autowired
    public SubmissionServiceImpl(ClassroomService classroomService, AssignmentRepository assignmentRepository,
                                 AssignmentSubmissionRepository submissionRepository,
                                 MaterialFileValidator fileValidator) {
        this(classroomService, assignmentRepository, submissionRepository, fileValidator, Clock.systemDefaultZone());
    }

    public SubmissionServiceImpl(ClassroomService classroomService, AssignmentRepository assignmentRepository,
                                 AssignmentSubmissionRepository submissionRepository,
                                 MaterialFileValidator fileValidator, Clock clock) {
        this.classroomService = classroomService;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.fileValidator = fileValidator;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SubmissionResponse submit(CurrentActor actor, UUID spaceId, UUID assignmentId, String content,
                                     MultipartFile file) {
        LearningSpace space = classroomService.loadAccessibleSpace(actor, spaceId);
        if (!actor.isLearner()
                || !Objects.equals(actor.learner().getLearnerId(), space.getLearner().getLearnerId())) {
            throw ClassroomException.forbidden("Chỉ học viên của lớp mới được nộp bài tập.");
        }
        Assignment assignment = findInSpace(assignmentId, spaceId);
        OffsetDateTime now = OffsetDateTime.now(clock);

        if (classroomService.isReadOnly(space)) {
            throw ClassroomException.conflict("Lớp học đang ở chế độ chỉ xem nên không thể nộp bài.");
        }
        List<AssignmentSubmission> previous = submissionRepository
                .findByAssignment_AssignmentIdAndLearner_LearnerIdOrderByAttemptNumberAsc(
                        assignmentId, actor.learner().getLearnerId());
        if (previous.stream().anyMatch(AssignmentSubmission::isFinalized)) {
            throw ClassroomException.conflict("Bài tập đã được chấm điểm nên không thể nộp thêm.");
        }
        if (AssignmentRules.dueOver(assignment.getDueDate(), now) && !previous.isEmpty()) {
            throw ClassroomException.conflict("Đã quá hạn nộp bài nên không thể nộp lại hoặc chỉnh sửa bài làm.");
        }

        String cleanContent = normalizeContent(content);
        boolean hasFile = file != null && !file.isEmpty();
        if (cleanContent == null && !hasFile) {
            throw ClassroomException.field("content", "Vui lòng nhập nội dung trả lời hoặc chọn tệp bài làm.");
        }
        MaterialFileType type = null;
        byte[] bytes = null;
        if (hasFile) {
            type = fileValidator.validate(file);
            bytes = readBytes(file);
        }

        int nextAttempt = previous.stream().mapToInt(s -> s.getAttemptNumber() == null ? 1 : s.getAttemptNumber())
                .max().orElse(0) + 1;
        AssignmentSubmission saved = submissionRepository.save(AssignmentSubmission.builder()
                .assignment(assignment)
                .learner(actor.learner())
                .attemptNumber((short) nextAttempt)
                .submissionContent(cleanContent)
                .attachmentUrl(hasFile ? SubmissionFileSupport.cleanFileName(file.getOriginalFilename(), type) : null)
                .status(AssignmentSubmissionStatus.SUBMITTED)
                .finalized(false)
                .submittedAt(now)
                .build());
        if (bytes != null) {
            submissionRepository.updateFileData(saved.getSubmissionId(), bytes);
        }
        return AssignmentMapper.toSubmissionResponse(saved, assignment.getDueDate());
    }

    @Override
    @Transactional
    public SubmissionResponse grade(CurrentActor actor, UUID spaceId, UUID assignmentId, UUID submissionId,
                                    GradeRequest request) {
        if (!actor.isTutor()) {
            throw ClassroomException.forbidden("Chỉ gia sư của lớp mới được chấm điểm bài tập.");
        }
        LearningSpace space = classroomService.loadManageableSpace(actor, spaceId);
        Assignment assignment = findInSpace(assignmentId, spaceId);
        AssignmentSubmission submission = findSubmission(submissionId, assignmentId);
        OffsetDateTime now = OffsetDateTime.now(clock);

        List<AssignmentSubmission> all = submissionRepository
                .findByAssignment_AssignmentIdAndLearner_LearnerIdOrderByAttemptNumberAsc(
                        assignmentId, space.getLearner().getLearnerId());
        if (all.stream().anyMatch(AssignmentSubmission::isFinalized)) {
            throw ClassroomException.conflict("Bài tập này đã được chấm điểm. Mỗi bài chỉ được chấm một lần.");
        }
        if (assignment.getDueDate() != null && !AssignmentRules.dueOver(assignment.getDueDate(), now)) {
            throw ClassroomException.conflict("Chỉ được chấm điểm sau khi hết hạn nộp bài.");
        }
        AssignmentSubmission latest = all.isEmpty() ? null : all.get(all.size() - 1);
        if (latest == null || !Objects.equals(latest.getSubmissionId(), submission.getSubmissionId())) {
            throw ClassroomException.conflict("Chỉ chấm được lần nộp mới nhất của học viên.");
        }

        BigDecimal grade = normalizeGrade(request == null ? null : request.grade(), assignment.getTotalPoints());
        String feedback = normalizeFeedback(request == null ? null : request.feedback());

        submission.setGrade(grade);
        submission.setTutorFeedback(feedback);
        submission.setFinalized(true);
        submission.setStatus(AssignmentSubmissionStatus.GRADED);
        submission.setGradedAt(now);
        AssignmentSubmission saved = submissionRepository.save(submission);
        return AssignmentMapper.toSubmissionResponse(saved, assignment.getDueDate());
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionFileContent getFile(CurrentActor actor, UUID spaceId, UUID assignmentId, UUID submissionId) {
        classroomService.loadAccessibleSpace(actor, spaceId);
        findInSpace(assignmentId, spaceId);
        AssignmentSubmission submission = findSubmission(submissionId, assignmentId);
        byte[] data = submission.getAttachmentUrl() == null ? null
                : submissionRepository.findFileData(submission.getSubmissionId());
        if (data == null || data.length == 0) {
            throw ClassroomException.notFound("Lần nộp này không có tệp bài làm.");
        }
        MaterialFileType type = SubmissionFileSupport.typeOf(submission.getAttachmentUrl());
        return new SubmissionFileContent(data, type == null ? "application/octet-stream" : type.contentType,
                submission.getAttachmentUrl());
    }

    // ---------------------------------------------------------------------

    private Assignment findInSpace(UUID assignmentId, UUID spaceId) {
        return assignmentRepository.findByAssignmentIdAndSpace_SpaceId(assignmentId, spaceId)
                .orElseThrow(() -> ClassroomException.notFound("Không tìm thấy bài tập trong lớp học này."));
    }

    private AssignmentSubmission findSubmission(UUID submissionId, UUID assignmentId) {
        return submissionRepository.findBySubmissionIdAndAssignment_AssignmentId(submissionId, assignmentId)
                .orElseThrow(() -> ClassroomException.notFound("Không tìm thấy bài nộp của bài tập này."));
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw ClassroomException.field("file", "Không đọc được tệp. Vui lòng chọn lại tệp khác.");
        }
    }

    static String normalizeContent(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String c = raw.strip();
        if (c.length() > CONTENT_MAX) {
            throw ClassroomException.field("content", "Nội dung trả lời tối đa " + CONTENT_MAX + " ký tự.");
        }
        return c;
    }

    static String normalizeFeedback(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String f = raw.strip();
        if (f.length() > FEEDBACK_MAX) {
            throw ClassroomException.field("feedback", "Nhận xét tối đa " + FEEDBACK_MAX + " ký tự.");
        }
        return f;
    }

    static BigDecimal normalizeGrade(BigDecimal raw, BigDecimal totalPoints) {
        if (raw == null) throw ClassroomException.field("grade", "Vui lòng nhập điểm.");
        BigDecimal g = raw.setScale(2, RoundingMode.HALF_UP);
        if (g.signum() < 0 || g.compareTo(totalPoints) > 0) {
            throw ClassroomException.field("grade",
                    "Điểm phải từ 0 đến " + totalPoints.stripTrailingZeros().toPlainString() + ".");
        }
        return g;
    }
}
