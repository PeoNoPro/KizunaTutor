package com.kizuna.features.progress.dto;

import com.kizuna.features.progress.entity.AssignmentFile;
import com.kizuna.features.progress.entity.SubmissionFile;
import com.kizuna.features.progress.util.SubmissionFileSupport;

import java.util.UUID;

/** Một tệp đính kèm (đề bài hoặc bài làm). Không chứa dữ liệu tệp. fileType: PDF, DOC, DOCX, PNG, JPG, MP3. */
public record FileAttachmentResponse(UUID fileId, String fileName, String fileType, long fileSize) {

    public static FileAttachmentResponse of(AssignmentFile f) {
        return new FileAttachmentResponse(f.getFileId(), f.getFileName(), typeName(f.getFileName()),
                f.getFileSize() == null ? 0 : f.getFileSize());
    }

    public static FileAttachmentResponse of(SubmissionFile f) {
        return new FileAttachmentResponse(f.getFileId(), f.getFileName(), typeName(f.getFileName()),
                f.getFileSize() == null ? 0 : f.getFileSize());
    }

    private static String typeName(String fileName) {
        var type = SubmissionFileSupport.typeOf(fileName);
        return type == null ? null : type.name();
    }
}
