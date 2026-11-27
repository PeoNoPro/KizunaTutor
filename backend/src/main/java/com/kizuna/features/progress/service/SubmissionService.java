package com.kizuna.features.progress.service;

import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.progress.dto.GradeRequest;
import com.kizuna.features.progress.dto.SubmissionFileContent;
import com.kizuna.features.progress.dto.SubmissionResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * FE-07: nộp bài (UC-64), sửa bài đã nộp (UC-96), chấm điểm (UC-67).
 * Mỗi học viên một bài nộp cho mỗi bài tập: nội dung + tối đa 10 tệp (mỗi tệp tối đa 25 MB).
 */
public interface SubmissionService {

    /** Học viên nộp bài lần đầu (còn hạn, hoặc quá hạn nhưng chưa nộp lần nào → nộp muộn). */
    SubmissionResponse submit(CurrentActor actor, UUID spaceId, UUID assignmentId, String content,
                              List<MultipartFile> files);

    /**
     * Học viên sửa bài đã nộp khi còn hạn và chưa được chấm: thay nội dung, gỡ các tệp trong removeFileIds,
     * thêm các tệp mới. Sau khi sửa vẫn phải còn nội dung hoặc ít nhất một tệp.
     */
    SubmissionResponse update(CurrentActor actor, UUID spaceId, UUID assignmentId, UUID submissionId, String content,
                              List<MultipartFile> files, List<UUID> removeFileIds);

    SubmissionResponse grade(CurrentActor actor, UUID spaceId, UUID assignmentId, UUID submissionId, GradeRequest request);

    SubmissionFileContent getFile(CurrentActor actor, UUID spaceId, UUID assignmentId, UUID submissionId, UUID fileId);
}
