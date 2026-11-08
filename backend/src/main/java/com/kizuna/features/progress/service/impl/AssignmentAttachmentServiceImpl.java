package com.kizuna.features.progress.service.impl;

import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.service.ClassroomService;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.classroom.util.MaterialFileValidator;
import com.kizuna.features.classroom.util.MaterialFileValidator.MaterialFileType;
import com.kizuna.features.progress.dto.AssignmentAttachmentResponse;
import com.kizuna.features.progress.dto.SubmissionFileContent;
import com.kizuna.features.progress.entity.Assignment;
import com.kizuna.features.progress.repository.AssignmentRepository;
import com.kizuna.features.progress.repository.AssignmentSubmissionRepository;
import com.kizuna.features.progress.service.AssignmentAttachmentService;
import com.kizuna.features.progress.util.SubmissionFileSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * Tệp đề bài của bài tập: cùng định dạng và giới hạn với tài liệu học tập FE-06 (PDF, PNG, JPG, MP3; 10 MB).
 * Chỉ gia sư của lớp đang hoạt động được thay/gỡ, và chỉ khi bài tập chưa có bài được chấm (giống sửa bài tập).
 */
@Service
@RequiredArgsConstructor
public class AssignmentAttachmentServiceImpl implements AssignmentAttachmentService {

    private final ClassroomService classroomService;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final MaterialFileValidator fileValidator;

    @Override
    @Transactional
    public AssignmentAttachmentResponse upload(CurrentActor actor, UUID spaceId, UUID assignmentId,
                                               MultipartFile file) {
        Assignment assignment = loadEditable(actor, spaceId, assignmentId);
        MaterialFileType type = fileValidator.validate(file);
        byte[] bytes = readBytes(file);
        assignment.setAttachmentUrl(SubmissionFileSupport.cleanFileName(file.getOriginalFilename(), type, "de-bai"));
        assignmentRepository.save(assignment);
        assignmentRepository.updateFileData(assignmentId, bytes);
        return new AssignmentAttachmentResponse(assignment.getAttachmentUrl(), type.name());
    }

    @Override
    @Transactional
    public AssignmentAttachmentResponse remove(CurrentActor actor, UUID spaceId, UUID assignmentId) {
        Assignment assignment = loadEditable(actor, spaceId, assignmentId);
        if (assignment.getAttachmentUrl() == null) {
            throw ClassroomException.notFound("Bài tập này chưa có tệp đính kèm.");
        }
        if (assignment.getDescription() == null || assignment.getDescription().isBlank()) {
            throw ClassroomException.field("description",
                    "Bài tập cần có nội dung hoặc tệp đề bài. Vui lòng nhập nội dung trước khi gỡ tệp.");
        }
        assignment.setAttachmentUrl(null);
        assignmentRepository.save(assignment);
        assignmentRepository.updateFileData(assignmentId, null);
        return new AssignmentAttachmentResponse(null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionFileContent getFile(CurrentActor actor, UUID spaceId, UUID assignmentId) {
        classroomService.loadAccessibleSpace(actor, spaceId);
        Assignment assignment = findInSpace(assignmentId, spaceId);
        byte[] data = assignment.getAttachmentUrl() == null ? null : assignmentRepository.findFileData(assignmentId);
        if (data == null || data.length == 0) {
            throw ClassroomException.notFound("Bài tập này không có tệp đính kèm.");
        }
        MaterialFileType type = SubmissionFileSupport.typeOf(assignment.getAttachmentUrl());
        return new SubmissionFileContent(data, type == null ? "application/octet-stream" : type.contentType,
                assignment.getAttachmentUrl());
    }

    // ---------------------------------------------------------------------

    private Assignment loadEditable(CurrentActor actor, UUID spaceId, UUID assignmentId) {
        if (!actor.isTutor()) {
            throw ClassroomException.forbidden("Chỉ gia sư của lớp mới được đính kèm tệp cho bài tập.");
        }
        classroomService.loadManageableSpace(actor, spaceId);
        Assignment assignment = findInSpace(assignmentId, spaceId);
        if (submissionRepository.existsByAssignment_AssignmentIdAndFinalizedTrue(assignmentId)) {
            throw ClassroomException.conflict("Bài tập đã có bài được chấm nên không thể chỉnh sửa.");
        }
        return assignment;
    }

    private Assignment findInSpace(UUID assignmentId, UUID spaceId) {
        return assignmentRepository.findByAssignmentIdAndSpace_SpaceId(assignmentId, spaceId)
                .orElseThrow(() -> ClassroomException.notFound("Không tìm thấy bài tập trong lớp học này."));
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw ClassroomException.field("file", "Không đọc được tệp. Vui lòng chọn lại tệp khác.");
        }
    }
}
