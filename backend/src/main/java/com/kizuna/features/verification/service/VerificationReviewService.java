package com.kizuna.features.verification.service;

import com.kizuna.features.verification.dto.request.RejectVerificationRequest;
import com.kizuna.features.verification.dto.request.RequestInfoRequest;
import com.kizuna.features.verification.dto.request.VerificationQueueQuery;
import com.kizuna.features.verification.dto.response.DecisionResultResponse;
import com.kizuna.features.verification.dto.response.PageResponse;
import com.kizuna.features.verification.dto.response.TutorVerificationResponse;
import com.kizuna.features.verification.dto.response.VerificationHistoryItemResponse;
import com.kizuna.features.verification.dto.response.VerificationQueueItemResponse;
import com.kizuna.features.verification.dto.response.VerificationSummaryResponse;

import java.util.List;
import java.util.UUID;

/**
 * Nghiệp vụ thẩm định phía cán bộ học vụ (UC-25, UC-26, UC-27, UC-28, UC-84).
 */
public interface VerificationReviewService {

    VerificationSummaryResponse getSummary();

    PageResponse<VerificationQueueItemResponse> getQueue(VerificationQueueQuery query);

    TutorVerificationResponse getDetail(UUID tutorId);

    DocumentFile loadDocumentFile(UUID tutorId, UUID documentId);

    DecisionResultResponse approve(UUID tutorId);

    DecisionResultResponse reject(UUID tutorId, RejectVerificationRequest request);

    DecisionResultResponse requestInfo(UUID tutorId, RequestInfoRequest request);

    List<VerificationHistoryItemResponse> getHistory(UUID tutorId);
}
