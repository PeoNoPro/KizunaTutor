package com.kizuna.features.progress.service.impl;

import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.service.ClassroomService;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.progress.dto.AssignmentDetailResponse;
import com.kizuna.features.progress.dto.AssignmentRequest;
import com.kizuna.features.progress.dto.AssignmentSummaryResponse;
import com.kizuna.features.progress.entity.Assignment;
import com.kizuna.features.progress.entity.AssignmentSubmission;
import com.kizuna.features.progress.mapper.AssignmentMapper;
import com.kizuna.features.progress.repository.AssignmentRepository;
import com.kizuna.features.progress.repository.AssignmentSubmissionRepository;
import com.kizuna.features.progress.service.AssignmentService;
import com.kizuna.features.progress.util.AssignmentRules;
import com.kizuna.features.progress.util.SubmissionFileSupport;
import com.kizuna.features.classroom.util.MaterialFileValidator.MaterialFileType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class AssignmentServiceImpl implements AssignmentService {

    static final int TITLE_MAX = 150;
    static final int DESCRIPTION_MAX = 5000;
    static final BigDecimal POINTS_MIN = new BigDecimal("1");
    static final BigDecimal POINTS_MAX = new BigDecimal("999.99");
    static final BigDecimal POINTS_DEFAULT = new BigDecimal("100.00");

    private final ClassroomService classroomService;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final Clock clock;

    @Autowired
    public AssignmentServiceImpl(ClassroomService classroomService, AssignmentRepository assignmentRepository,
                                 AssignmentSubmissionRepository submissionRepository) {
        this(classroomService, assignmentRepository, submissionRepository, Clock.systemDefaultZone());
    }

    public AssignmentServiceImpl(ClassroomService classroomService, AssignmentRepository assignmentRepository,
                                 AssignmentSubmissionRepository submissionRepository, Clock clock) {
        this.classroomService = classroomService;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentSummaryResponse> list(CurrentActor actor, UUID spaceId) {
        LearningSpace space = classroomService.loadAccessibleSpace(actor, spaceId);
        OffsetDateTime now = OffsetDateTime.now(clock);
        boolean readOnly = classroomService.isReadOnly(space);
        return assignmentRepository.findBySpace_SpaceIdOrderByCreatedAtDesc(spaceId).stream()
                .map(a -> toSummary(actor, a, submissionsOf(a, space), now, readOnly))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentDetailResponse detail(CurrentActor actor, UUID spaceId, UUID assignmentId) {
        LearningSpace space = classroomService.loadAccessibleSpace(actor, spaceId);
        Assignment assignment = findInSpace(assignmentId, spaceId);
        List<AssignmentSubmission> submissions = submissionsOf(assignment, space);
        AssignmentSummaryResponse summary = toSummary(actor, assignment, submissions, OffsetDateTime.now(clock),
                classroomService.isReadOnly(space));
        return new AssignmentDetailResponse(summary, submissions.stream()
                .map(s -> AssignmentMapper.toSubmissionResponse(s, assignment.getDueDate()))
                .toList());
    }

    @Override
    @Transactional
    public AssignmentSummaryResponse create(CurrentActor actor, UUID spaceId, AssignmentRequest request) {
        requireTutor(actor);
        LearningSpace space = classroomService.loadManageableSpace(actor, spaceId);
        OffsetDateTime now = OffsetDateTime.now(clock);
        AssignmentRequest req = request == null ? new AssignmentRequest(null, null, null, null) : request;

        String title = normalizeTitle(req.title());
        String description = normalizeDescription(req.description());
        if (req.dueDate() != null) requireFuture(req.dueDate(), now);
        BigDecimal points = normalizePoints(req.totalPoints());

        Assignment saved = assignmentRepository.save(Assignment.builder()
                .space(space)
                .title(title)
                .description(description)
                .dueDate(req.dueDate())
                .totalPoints(points)
                .build());
        return toSummary(actor, saved, List.of(), now, false);
    }

    @Override
    @Transactional
    public AssignmentSummaryResponse update(CurrentActor actor, UUID spaceId, UUID assignmentId,
                                            AssignmentRequest request) {
        requireTutor(actor);
        LearningSpace space = classroomService.loadManageableSpace(actor, spaceId);
        Assignment assignment = findInSpace(assignmentId, spaceId);
        if (submissionRepository.existsByAssignment_AssignmentIdAndFinalizedTrue(assignmentId)) {
            throw ClassroomException.conflict("Bài tập đã có bài được chấm nên không thể chỉnh sửa.");
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        AssignmentRequest req = request == null ? new AssignmentRequest(null, null, null, null) : request;

        String title = normalizeTitle(req.title());
        String description = normalizeDescription(req.description());
        BigDecimal points = normalizePoints(req.totalPoints());
        boolean dueChanged = !sameInstant(req.dueDate(), assignment.getDueDate());
        if (dueChanged && req.dueDate() != null) requireFuture(req.dueDate(), now);

        assignment.setTitle(title);
        assignment.setDescription(description);
        assignment.setTotalPoints(points);
        assignment.setDueDate(req.dueDate());
        Assignment saved = assignmentRepository.save(assignment);
        return toSummary(actor, saved, submissionsOf(saved, space), now, false);
    }

    // ---------------------------------------------------------------------

    private Assignment findInSpace(UUID assignmentId, UUID spaceId) {
        return assignmentRepository.findByAssignmentIdAndSpace_SpaceId(assignmentId, spaceId)
                .orElseThrow(() -> ClassroomException.notFound("Không tìm thấy bài tập trong lớp học này."));
    }

    private List<AssignmentSubmission> submissionsOf(Assignment assignment, LearningSpace space) {
        return submissionRepository.findByAssignment_AssignmentIdAndLearner_LearnerIdOrderByAttemptNumberAsc(
                assignment.getAssignmentId(), space.getLearner().getLearnerId());
    }

    private AssignmentSummaryResponse toSummary(CurrentActor actor, Assignment a, List<AssignmentSubmission> subs,
                                                OffsetDateTime now, boolean readOnly) {
        AssignmentSubmission latest = subs.isEmpty() ? null : subs.get(subs.size() - 1);
        boolean graded = subs.stream().anyMatch(AssignmentSubmission::isFinalized);
        return AssignmentSummaryResponse.builder()
                .assignmentId(a.getAssignmentId())
                .title(a.getTitle())
                .description(a.getDescription())
                .dueDate(a.getDueDate())
                .totalPoints(a.getTotalPoints())
                .attachmentName(a.getAttachmentUrl())
                .attachmentType(attachmentType(a.getAttachmentUrl()))
                .createdAt(a.getCreatedAt())
                .dueOver(AssignmentRules.dueOver(a.getDueDate(), now))
                .submissionState(AssignmentRules.state(a.getDueDate(), now, latest))
                .attemptCount(subs.size())
                .grade(subs.stream().filter(AssignmentSubmission::isFinalized).map(AssignmentSubmission::getGrade)
                        .filter(Objects::nonNull).findFirst().orElse(null))
                .canSubmit(actor.isLearner()
                        && AssignmentRules.canSubmit(a.getDueDate(), now, subs.size(), graded, readOnly))
                .canGrade(actor.isTutor()
                        && AssignmentRules.canGrade(a.getDueDate(), now, latest != null, graded, readOnly))
                .canEdit(actor.isTutor() && !readOnly && !graded)
                .build();
    }

    private static String attachmentType(String fileName) {
        MaterialFileType type = SubmissionFileSupport.typeOf(fileName);
        return type == null ? null : type.name();
    }

    private static void requireTutor(CurrentActor actor) {
        if (!actor.isTutor()) {
            throw ClassroomException.forbidden("Chỉ gia sư của lớp mới được giao và chỉnh sửa bài tập.");
        }
    }

    private static boolean sameInstant(OffsetDateTime a, OffsetDateTime b) {
        if (a == null || b == null) return a == b;
        return a.isEqual(b);
    }

    private static void requireFuture(OffsetDateTime dueDate, OffsetDateTime now) {
        if (!dueDate.isAfter(now)) {
            throw ClassroomException.field("dueDate", "Hạn nộp phải là thời điểm trong tương lai.");
        }
    }

    static String normalizeTitle(String raw) {
        String title = raw == null ? "" : raw.strip();
        if (title.isEmpty()) throw ClassroomException.field("title", "Vui lòng nhập tiêu đề bài tập.");
        if (title.length() > TITLE_MAX) {
            throw ClassroomException.field("title", "Tiêu đề bài tập tối đa " + TITLE_MAX + " ký tự.");
        }
        return title;
    }

    static String normalizeDescription(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String d = raw.strip();
        if (d.length() > DESCRIPTION_MAX) {
            throw ClassroomException.field("description", "Mô tả bài tập tối đa " + DESCRIPTION_MAX + " ký tự.");
        }
        return d;
    }

    static BigDecimal normalizePoints(BigDecimal raw) {
        if (raw == null) return POINTS_DEFAULT;
        BigDecimal p = raw.setScale(2, RoundingMode.HALF_UP);
        if (p.compareTo(POINTS_MIN) < 0 || p.compareTo(POINTS_MAX) > 0) {
            throw ClassroomException.field("totalPoints", "Điểm tối đa phải từ 1 đến 999,99.");
        }
        return p;
    }
}
