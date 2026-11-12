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
import com.kizuna.features.progress.entity.SubmissionFile;
import com.kizuna.features.progress.mapper.AssignmentMapper;
import com.kizuna.features.progress.repository.AssignmentRepository;
import com.kizuna.features.progress.repository.AssignmentSubmissionRepository;
import com.kizuna.features.progress.repository.SubmissionFileRepository;
import com.kizuna.features.progress.service.SubmissionService;
import com.kizuna.features.progress.util.AssignmentRules;
import com.kizuna.features.progress.util.SubmissionFileSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Nộp bài / sửa bài đã nộp / chấm điểm theo quy tắc trong {@link AssignmentRules}.
 * Mỗi học viên một bài nộp cho mỗi bài tập; bản ghi và nội dung tệp được ghi trong cùng một giao dịch.
 */
@Service
public class SubmissionServiceImpl implements SubmissionService {

    static final int CONTENT_MAX = 5000;
    static final int FEEDBACK_MAX = 5000;

    private final ClassroomService classroomService;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final SubmissionFileRepository fileRepository;
    private final MaterialFileValidator fileValidator;
    private final Clock clock;

    @Autowired
    public SubmissionServiceImpl(ClassroomService classroomService, AssignmentRepository assignmentRepository,
                                 AssignmentSubmissionRepository submissionRepository,
                                 SubmissionFileRepository fileRepository, MaterialFileValidator fileValidator) {
        this(classroomService, assignmentRepository, submissionRepository, fileRepository, fileValidator,
                Clock.systemDefaultZone());
    }

    public SubmissionServiceImpl(ClassroomService classroomService, AssignmentRepository assignmentRepository,
                                 AssignmentSubmissionRepository submissionRepository,
                                 SubmissionFileRepository fileRepository, MaterialFileValidator fileValidator,
                                 Clock clock) {
        this.classroomService = classroomService;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.fileRepository = fileRepository;
        this.fileValidator = fileValidator;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SubmissionResponse submit(CurrentActor actor, UUID spaceId, UUID assignmentId, String content,
                                     List<MultipartFile> files) {
        loadLearnerSpace(actor, spaceId);
        Assignment assignment = findInSpace(assignmentId, spaceId);
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (!AssignmentRules.isOpen(assignment.getOpensAt(), now)) {
            throw ClassroomException.notFound("Không tìm thấy bài tập trong lớp học này.");
        }

        List<AssignmentSubmission> previous = submissionsOf(assignmentId, actor);
        if (previous.stream().anyMatch(AssignmentSubmission::isFinalized)) {
            throw ClassroomException.conflict("Bài tập đã được chấm điểm nên không thể nộp thêm.");
        }
        if (!previous.isEmpty()) {
            throw ClassroomException.conflict("Bạn đã nộp bài tập này. Hãy chỉnh sửa bài đã nộp thay vì nộp lại.");
        }

        String cleanContent = normalizeContent(content);
        List<MultipartFile> chosen = fileValidator.nonEmpty(files);
        if (cleanContent == null && chosen.isEmpty()) {
            throw ClassroomException.field("content", "Vui lòng nhập nội dung trả lời hoặc chọn tệp bài làm.");
        }
        List<SubmissionFileSupport.PreparedFile> prepared = SubmissionFileSupport.prepare(fileValidator, chosen, "bai-lam");

        AssignmentSubmission saved = submissionRepository.save(AssignmentSubmission.builder()
                .assignment(assignment)
                .learner(actor.learner())
                .attemptNumber((short) 1)
                .submissionContent(cleanContent)
                .status(AssignmentSubmissionStatus.SUBMITTED)
                .finalized(false)
                .submittedAt(now)
                .build());
        insertFiles(saved.getSubmissionId(), prepared);
        return toResponse(saved, assignment);
    }

    @Override
    @Transactional
    public SubmissionResponse update(CurrentActor actor, UUID spaceId, UUID assignmentId, UUID submissionId,
                                     String content, List<MultipartFile> files, List<UUID> removeFileIds) {
        loadLearnerSpace(actor, spaceId);
        Assignment assignment = findInSpace(assignmentId, spaceId);
        AssignmentSubmission submission = findSubmission(submissionId, assignmentId);
        OffsetDateTime now = OffsetDateTime.now(clock);

        List<AssignmentSubmission> all = submissionsOf(assignmentId, actor);
        AssignmentSubmission latest = all.isEmpty() ? null : all.get(all.size() - 1);
        if (latest == null || !Objects.equals(latest.getSubmissionId(), submission.getSubmissionId())) {
            throw ClassroomException.forbidden("Bạn chỉ được chỉnh sửa bài nộp của chính mình.");
        }
        if (all.stream().anyMatch(AssignmentSubmission::isFinalized)) {
            throw ClassroomException.conflict("Bài tập đã được chấm điểm nên không thể chỉnh sửa bài nộp.");
        }
        if (AssignmentRules.dueOver(assignment.getDueDate(), now)) {
            throw ClassroomException.conflict("Đã quá hạn nộp bài nên không thể chỉnh sửa bài nộp.");
        }

        String cleanContent = normalizeContent(content);
        List<MultipartFile> chosen = fileValidator.nonEmpty(files);
        List<SubmissionFile> current = fileRepository.findBySubmissionIdOrderByCreatedAtAscFileNameAsc(submissionId);
        Set<UUID> toRemove = new HashSet<>(removeFileIds == null ? List.of() : removeFileIds);
        List<SubmissionFile> removed = current.stream().filter(f -> toRemove.contains(f.getFileId())).toList();
        if (removed.size() != toRemove.size()) {
            throw ClassroomException.notFound("Không tìm thấy tệp cần gỡ trong bài nộp này.");
        }
        int remaining = current.size() - removed.size() + chosen.size();
        if (remaining > MaterialFileValidator.MAX_FILES) {
            throw ClassroomException.field("file",
                    "Mỗi bài nộp có tối đa " + MaterialFileValidator.MAX_FILES + " tệp.");
        }
        if (cleanContent == null && remaining == 0) {
            throw ClassroomException.field("content",
                    "Bài nộp cần có nội dung trả lời hoặc ít nhất một tệp bài làm.");
        }
        List<SubmissionFileSupport.PreparedFile> prepared = SubmissionFileSupport.prepare(fileValidator, chosen, "bai-lam");

        fileRepository.deleteAll(removed);
        insertFiles(submissionId, prepared);
        submission.setSubmissionContent(cleanContent);
        submission.setSubmittedAt(now);
        AssignmentSubmission saved = submissionRepository.save(submission);
        return toResponse(saved, assignment);
    }

    @Override
    @Transactional
    public SubmissionResponse grade(CurrentActor actor, UUID spaceId, UUID assignmentId, UUID submissionId,
                                    GradeRequest request) {
        if (!actor.isTutor()) {
            throw ClassroomException.forbidden("Chỉ gia sư của lớp mới được chấm điểm bài tập.");
        }
        // Khóa học đã hoàn thành vẫn chấm được bài còn chờ
        LearningSpace space = classroomService.loadGradableSpace(actor, spaceId);
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
            throw ClassroomException.conflict("Chỉ chấm được bài nộp hiện tại của học viên.");
        }

        BigDecimal grade = normalizeGrade(request == null ? null : request.grade(), assignment.getTotalPoints());
        String feedback = normalizeFeedback(request == null ? null : request.feedback());

        submission.setGrade(grade);
        submission.setTutorFeedback(feedback);
        submission.setFinalized(true);
        submission.setStatus(AssignmentSubmissionStatus.GRADED);
        submission.setGradedAt(now);
        AssignmentSubmission saved = submissionRepository.save(submission);
        return toResponse(saved, assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionFileContent getFile(CurrentActor actor, UUID spaceId, UUID assignmentId, UUID submissionId,
                                         UUID fileId) {
        classroomService.loadAccessibleSpace(actor, spaceId);
        findInSpace(assignmentId, spaceId);
        findSubmission(submissionId, assignmentId);
        SubmissionFile file = fileRepository.findByFileIdAndSubmissionId(fileId, submissionId)
                .orElseThrow(() -> ClassroomException.notFound("Không tìm thấy tệp bài làm."));
        byte[] data = fileRepository.findFileData(file.getFileId());
        if (data == null || data.length == 0) {
            throw ClassroomException.notFound("Không tìm thấy tệp bài làm.");
        }
        MaterialFileType type = SubmissionFileSupport.typeOf(file.getFileName());
        return new SubmissionFileContent(data, type == null ? "application/octet-stream" : type.contentType,
                file.getFileName());
    }

    // ---------------------------------------------------------------------

    /** Học viên của lớp, lớp không ở chế độ chỉ xem. */
    private LearningSpace loadLearnerSpace(CurrentActor actor, UUID spaceId) {
        LearningSpace space = classroomService.loadAccessibleSpace(actor, spaceId);
        if (!actor.isLearner()
                || !Objects.equals(actor.learner().getLearnerId(), space.getLearner().getLearnerId())) {
            throw ClassroomException.forbidden("Chỉ học viên của lớp mới được nộp bài tập.");
        }
        if (classroomService.isReadOnly(space)) {
            throw ClassroomException.conflict("Lớp học đang ở chế độ chỉ xem nên không thể nộp hoặc sửa bài.");
        }
        return space;
    }

    private List<AssignmentSubmission> submissionsOf(UUID assignmentId, CurrentActor actor) {
        return submissionRepository.findByAssignment_AssignmentIdAndLearner_LearnerIdOrderByAttemptNumberAsc(
                assignmentId, actor.learner().getLearnerId());
    }

    private void insertFiles(UUID submissionId, List<SubmissionFileSupport.PreparedFile> prepared) {
        for (SubmissionFileSupport.PreparedFile f : prepared) {
            fileRepository.insertFile(UUID.randomUUID(), submissionId, f.fileName(), f.data().length, f.data());
        }
    }

    private SubmissionResponse toResponse(AssignmentSubmission s, Assignment assignment) {
        return AssignmentMapper.toSubmissionResponse(s, assignment.getDueDate(),
                fileRepository.findBySubmissionIdOrderByCreatedAtAscFileNameAsc(s.getSubmissionId()));
    }

    private Assignment findInSpace(UUID assignmentId, UUID spaceId) {
        return assignmentRepository.findByAssignmentIdAndSpace_SpaceId(assignmentId, spaceId)
                .orElseThrow(() -> ClassroomException.notFound("Không tìm thấy bài tập trong lớp học này."));
    }

    private AssignmentSubmission findSubmission(UUID submissionId, UUID assignmentId) {
        return submissionRepository.findBySubmissionIdAndAssignment_AssignmentId(submissionId, assignmentId)
                .orElseThrow(() -> ClassroomException.notFound("Không tìm thấy bài nộp của bài tập này."));
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
