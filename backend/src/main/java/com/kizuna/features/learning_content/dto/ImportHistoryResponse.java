package com.kizuna.features.learning_content.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Một lần nhập thật trong lịch sử (lưu ở system_audit_logs). */
public record ImportHistoryResponse(UUID id, OffsetDateTime createdAt, String actorName, String actorEmail,
                                    String type, String typeLabel, String fileName, int totalRows, int created,
                                    int skippedExisting, int duplicatesInFile, int invalidRows) {
}
