package com.kizuna.features.progress.service;

import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.progress.dto.GradeRequest;
import com.kizuna.features.progress.dto.SubmissionFileContent;
import com.kizuna.features.progress.dto.SubmissionResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/** FE-07: nộp bài (UC-64), nộp lại (UC-96), chấm điểm (UC-67). */
public interface SubmissionService {

    SubmissionResponse submit(CurrentActor actor, UUID spaceId, UUID assignmentId, String content, MultipartFile file);

    SubmissionResponse grade(CurrentActor actor, UUID spaceId, UUID assignmentId, UUID submissionId, GradeRequest request);

    SubmissionFileContent getFile(CurrentActor actor, UUID spaceId, UUID assignmentId, UUID submissionId);
}
