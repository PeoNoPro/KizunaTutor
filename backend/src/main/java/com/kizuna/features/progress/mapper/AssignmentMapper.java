package com.kizuna.features.progress.mapper;

import com.kizuna.features.classroom.util.MaterialFileValidator.MaterialFileType;
import com.kizuna.features.progress.dto.SubmissionResponse;
import com.kizuna.features.progress.entity.AssignmentSubmission;
import com.kizuna.features.progress.util.AssignmentRules;
import com.kizuna.features.progress.util.SubmissionFileSupport;

import java.time.OffsetDateTime;

public final class AssignmentMapper {

    private AssignmentMapper() {
    }

    public static SubmissionResponse toSubmissionResponse(AssignmentSubmission s, OffsetDateTime dueDate) {
        MaterialFileType type = SubmissionFileSupport.typeOf(s.getAttachmentUrl());
        return new SubmissionResponse(
                s.getSubmissionId(),
                s.getAttemptNumber() == null ? 1 : s.getAttemptNumber(),
                s.getSubmissionContent(),
                s.getAttachmentUrl(),
                type == null ? null : type.name(),
                s.getSubmittedAt(),
                AssignmentRules.onTime(dueDate, s.getSubmittedAt()),
                s.getGrade(),
                s.getTutorFeedback(),
                s.isFinalized(),
                s.getGradedAt());
    }
}
