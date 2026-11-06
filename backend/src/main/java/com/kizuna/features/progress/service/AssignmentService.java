package com.kizuna.features.progress.service;

import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.progress.dto.AssignmentDetailResponse;
import com.kizuna.features.progress.dto.AssignmentRequest;
import com.kizuna.features.progress.dto.AssignmentSummaryResponse;

import java.util.List;
import java.util.UUID;

/** FE-07: bài tập (UC-63 View, UC-66 Create & Assign, UC-95 Update). */
public interface AssignmentService {

    List<AssignmentSummaryResponse> list(CurrentActor actor, UUID spaceId);

    AssignmentDetailResponse detail(CurrentActor actor, UUID spaceId, UUID assignmentId);

    AssignmentSummaryResponse create(CurrentActor actor, UUID spaceId, AssignmentRequest request);

    AssignmentSummaryResponse update(CurrentActor actor, UUID spaceId, UUID assignmentId, AssignmentRequest request);
}
