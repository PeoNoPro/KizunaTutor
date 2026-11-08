package com.kizuna.features.progress.service.impl;

import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.service.ClassroomService;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.classroom.util.MaterialFileValidator;
import com.kizuna.features.classroom.util.MaterialFileValidator.MaterialFileType;
import com.kizuna.features.progress.dto.FileAttachmentResponse;
import com.kizuna.features.progress.dto.SubmissionFileContent;
import com.kizuna.features.progress.entity.Assignment;
import com.kizuna.features.progress.entity.AssignmentFile;
import com.kizuna.features.progress.repository.AssignmentFileRepository;
import com.kizuna.features.progress.repository.AssignmentRepository;
import com.kizuna.features.progress.service.AssignmentAttachmentService;
import com.kizuna.features.progress.util.SubmissionFileSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssignmentAttachmentServiceImpl implements AssignmentAttachmentService {

    private final ClassroomService classroomService;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentFileRepository fileRepository;
    private final MaterialFileValidator validator;

    @Override
    @Transactional
    public List<FileAttachmentResponse> applyChanges(Assignment assignment, List<MultipartFile> files,
                                                     List<UUID> removeFileIds) {
        UUID assignmentId = assignment.getAssignmentId();
        List<MultipartFile> chosen = validator.nonEmpty(files);
        List<AssignmentFile> current = fileRepository.findByAssignmentIdOrderByCreatedAtAscFileNameAsc(assignmentId);
        Set<UUID> toRemove = new HashSet<>(removeFileIds == null ? List.of() : removeFileIds);
        List<AssignmentFile> removed = current.stream().filter(f -> toRemove.contains(f.getFileId())).toList();
        if (removed.size() != toRemove.size()) {
            throw ClassroomException.notFound("Không tìm thấy tệp đề bài cần gỡ trong bài tập này.");
        }
        int remaining = current.size() - removed.size() + chosen.size();
        if (remaining > MaterialFileValidator.MAX_FILES) {
            throw ClassroomException.field("file",
                    "Mỗi bài tập có tối đa " + MaterialFileValidator.MAX_FILES + " tệp đề bài.");
        }
        boolean noContent = assignment.getDescription() == null || assignment.getDescription().isBlank();
        if (noContent && remaining == 0) {
            throw ClassroomException.field("description", "Vui lòng nhập nội dung bài tập hoặc tải lên tệp đề bài.");
        }
        // Kiểm tra hết các tệp trước khi ghi: một tệp lỗi → báo lỗi kèm tên tệp, không lưu tệp nào
        List<SubmissionFileSupport.PreparedFile> prepared = SubmissionFileSupport.prepare(validator, chosen, "de-bai");
        if (!removed.isEmpty()) fileRepository.deleteAll(removed);
        for (SubmissionFileSupport.PreparedFile f : prepared) {
            fileRepository.insertFile(UUID.randomUUID(), assignmentId, f.fileName(), f.data().length, f.data());
        }
        return list(assignmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileAttachmentResponse> list(UUID assignmentId) {
        return fileRepository.findByAssignmentIdOrderByCreatedAtAscFileNameAsc(assignmentId).stream()
                .map(FileAttachmentResponse::of).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionFileContent getFile(CurrentActor actor, UUID spaceId, UUID assignmentId, UUID fileId) {
        classroomService.loadAccessibleSpace(actor, spaceId);
        assignmentRepository.findByAssignmentIdAndSpace_SpaceId(assignmentId, spaceId)
                .orElseThrow(() -> ClassroomException.notFound("Không tìm thấy bài tập trong lớp học này."));
        AssignmentFile file = fileRepository.findByFileIdAndAssignmentId(fileId, assignmentId)
                .orElseThrow(() -> ClassroomException.notFound("Không tìm thấy tệp đề bài."));
        byte[] data = fileRepository.findFileData(file.getFileId());
        if (data == null || data.length == 0) {
            throw ClassroomException.notFound("Không tìm thấy tệp đề bài.");
        }
        MaterialFileType type = SubmissionFileSupport.typeOf(file.getFileName());
        return new SubmissionFileContent(data, type == null ? "application/octet-stream" : type.contentType,
                file.getFileName());
    }
}
