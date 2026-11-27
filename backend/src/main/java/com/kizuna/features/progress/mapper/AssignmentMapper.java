package com.kizuna.features.progress.mapper;

import com.kizuna.features.progress.dto.FileAttachmentResponse;
import com.kizuna.features.progress.dto.SubmissionResponse;
import com.kizuna.features.progress.entity.AssignmentSubmission;
import com.kizuna.features.progress.entity.SubmissionFile;
import com.kizuna.features.progress.util.AssignmentRules;

import java.time.OffsetDateTime;
import java.util.List;

public final class AssignmentMapper {

    private AssignmentMapper() {
    }

    public static SubmissionResponse toSubmissionResponse(AssignmentSubmission s, OffsetDateTime dueDate,
                                                          List<SubmissionFile> files) {
        return new SubmissionResponse(
                s.getSubmissionId(),
                s.getAttemptNumber() == null ? 1 : s.getAttemptNumber(),
                s.getSubmissionContent(),
                files == null ? List.of() : files.stream().map(FileAttachmentResponse::of).toList(),
                s.getSubmittedAt(),
                AssignmentRules.onTime(dueDate, s.getSubmittedAt()),
                s.getGrade(),
                s.getTutorFeedback(),
                s.isFinalized(),
                s.getGradedAt());
    }
}
