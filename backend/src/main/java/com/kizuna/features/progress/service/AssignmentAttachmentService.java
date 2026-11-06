package com.kizuna.features.progress.service;

import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.progress.dto.AssignmentAttachmentResponse;
import com.kizuna.features.progress.dto.SubmissionFileContent;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/** FE-07: tệp đề bài đính kèm khi giao/sửa bài tập (UC-66, UC-95). Mỗi bài tập tối đa một tệp. */
public interface AssignmentAttachmentService {

    /** Gia sư của lớp tải lên hoặc thay tệp đề bài. */
    AssignmentAttachmentResponse upload(CurrentActor actor, UUID spaceId, UUID assignmentId, MultipartFile file);

    /** Gia sư của lớp gỡ tệp đề bài. */
    AssignmentAttachmentResponse remove(CurrentActor actor, UUID spaceId, UUID assignmentId);

    /** Gia sư hoặc học viên của lớp xem/tải tệp đề bài. */
    SubmissionFileContent getFile(CurrentActor actor, UUID spaceId, UUID assignmentId);
}
