package com.kizuna.features.profile.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.core.entity.UserAccountStatus;
import com.kizuna.features.profile.dto.response.UserProfileDetailDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Kho lưu trữ nhật ký thay đổi trạng thái người dùng trên bảng system_audit_logs,
 * giữ tính cô lập hoàn toàn trong phân hệ hồ sơ (Constitution III.2).
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ProfileAuditLogRepository {

    public static final String ACTION_USER_STATUS_CHANGE = "USER_STATUS_CHANGE";
    public static final String TARGET_ENTITY_USER = "USER";

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public void recordStatusChange(UUID actorUserId,
                                   UUID targetUserId,
                                   UserAccountStatus previousStatus,
                                   UserAccountStatus newStatus,
                                   String reason,
                                   String ipAddress) {
        Map<String, Object> detailsMap = new LinkedHashMap<>();
        detailsMap.put("previousStatus", previousStatus == null ? null : previousStatus.name());
        detailsMap.put("newStatus", newStatus == null ? null : newStatus.name());
        detailsMap.put("reason", reason);
        String detailsJson;
        try {
            detailsJson = objectMapper.writeValueAsString(detailsMap);
        } catch (Exception ex) {
            detailsJson = "{}";
        }

        jdbcTemplate.update(
                """
                INSERT INTO system_audit_logs
                    (log_id, actor_user_id, action_type, target_entity, target_id, ip_address, details, created_at)
                VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CURRENT_TIMESTAMP)
                """,
                UUID.randomUUID(),
                actorUserId,
                ACTION_USER_STATUS_CHANGE,
                TARGET_ENTITY_USER,
                targetUserId,
                truncateIp(ipAddress),
                detailsJson
        );
    }

    public List<UserProfileDetailDto.StatusHistoryItem> findUserStatusHistory(UUID targetUserId) {
        try {
            return jdbcTemplate.query(
                    """
                    SELECT l.action_type,
                           l.details::text AS details_json,
                           l.created_at,
                           COALESCE(a.full_name, 'Quản lý hệ thống') AS actor_name
                    FROM system_audit_logs l
                    LEFT JOIN users a ON a.user_id = l.actor_user_id
                    WHERE l.target_entity = ?
                      AND l.target_id = ?
                      AND l.action_type = ?
                    ORDER BY l.created_at DESC
                    LIMIT 20
                    """,
                    (rs, rowNum) -> {
                        Map<String, Object> parsed = parseJson(rs.getString("details_json"));
                        Timestamp ts = rs.getTimestamp("created_at");
                        OffsetDateTime createdAt = ts == null ? null : ts.toInstant().atOffset(ZoneOffset.UTC);
                        return new UserProfileDetailDto.StatusHistoryItem(
                                rs.getString("action_type"),
                                stringValue(parsed.get("previousStatus")),
                                stringValue(parsed.get("newStatus")),
                                stringValue(parsed.get("reason")),
                                rs.getString("actor_name"),
                                createdAt
                        );
                    },
                    TARGET_ENTITY_USER,
                    targetUserId,
                    ACTION_USER_STATUS_CHANGE
            );
        } catch (Exception ex) {
            log.warn("Không thể đọc lịch sử trạng thái tài khoản từ nhật ký kiểm toán: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    private Map<String, Object> parseJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(raw, MAP_TYPE);
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String truncateIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return null;
        }
        String clean = ipAddress.trim();
        return clean.length() > 45 ? clean.substring(0, 45) : clean;
    }
}
