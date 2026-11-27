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

    /**
     * Giao bài tập kèm tệp đề bài trong MỘT lần (tối đa 5 tệp). Kiểm tra tất cả (tiêu đề, nội dung hoặc tệp, hạn nộp,
     * điểm, từng tệp); lỗi bất kỳ → không lưu gì.
     */
    AssignmentSummaryResponse create(CurrentActor actor, UUID spaceId, AssignmentRequest request,
                                     java.util.List<org.springframework.web.multipart.MultipartFile> files);

    default AssignmentSummaryResponse create(CurrentActor actor, UUID spaceId, AssignmentRequest request) {
        return create(actor, spaceId, request, null);
    }

    /** Sửa bài tập + thêm/gỡ/thay tệp đề bài trong MỘT lần; lỗi bất kỳ → không lưu gì. */
    AssignmentSummaryResponse update(CurrentActor actor, UUID spaceId, UUID assignmentId, AssignmentRequest request,
                                     java.util.List<org.springframework.web.multipart.MultipartFile> files,
                                     java.util.List<UUID> removeFileIds);

    default AssignmentSummaryResponse update(CurrentActor actor, UUID spaceId, UUID assignmentId, AssignmentRequest request) {
        return update(actor, spaceId, assignmentId, request, null, null);
    }
}
