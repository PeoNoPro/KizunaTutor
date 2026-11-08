package com.kizuna.features.progress.service;

import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.progress.dto.FileAttachmentResponse;
import com.kizuna.features.progress.dto.SubmissionFileContent;
import com.kizuna.features.progress.entity.Assignment;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/** FE-07: các tệp đề bài của bài tập (UC-66, UC-95). Mỗi bài tập tối đa 5 tệp, mỗi tệp tối đa 25 MB. */
public interface AssignmentAttachmentService {

    /**
     * Áp dụng thay đổi tệp đề bài cho bài tập VỪA LƯU, chạy trong cùng giao dịch với việc lưu bài tập
     * (gọi từ AssignmentService.create/update, đã kiểm quyền ở đó): gỡ các tệp removeFileIds, thêm các tệp mới.
     * Kiểm tra TẤT CẢ tệp trước khi ghi; chỉ cần một tệp lỗi là ném lỗi (kèm tên tệp) → cả giao dịch hủy, không lưu gì.
     * Quy tắc trên kết quả cuối: tối đa 5 tệp; bài tập phải có nội dung hoặc ít nhất một tệp.
     * @return toàn bộ tệp đề bài sau khi lưu
     */
    List<FileAttachmentResponse> applyChanges(Assignment assignment, List<MultipartFile> files, List<UUID> removeFileIds);

    /** Danh sách tệp đề bài của một bài tập. */
    List<FileAttachmentResponse> list(UUID assignmentId);

    /** Gia sư hoặc học viên của lớp xem/tải một tệp đề bài. */
    SubmissionFileContent getFile(CurrentActor actor, UUID spaceId, UUID assignmentId, UUID fileId);
}
