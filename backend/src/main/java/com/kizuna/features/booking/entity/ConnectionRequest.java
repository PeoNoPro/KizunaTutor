package com.kizuna.features.booking.entity;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Tutor;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity Yêu Cầu Kết Nối (ConnectionRequest)
 * Ánh xạ bảng connection_requests trong CSDL.
 */
@Entity
@Table(name = "connection_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "request_id", updatable = false, nullable = false)
    private UUID requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learner_id", nullable = false)
    private Learner learner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Tutor tutor;

    @Column(name = "initial_message", columnDefinition = "TEXT")
    private String initialMessage;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", nullable = false)
    private ConnectionRequestStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 20)
    @Builder.Default
    private ConnectionRequestType requestType = ConnectionRequestType.TRIAL;

    /** Lời nhắn / lý do từ chối của gia sư khi phản hồi yêu cầu kết nối trực tiếp. */
    @Column(name = "response_message", columnDefinition = "TEXT")
    private String responseMessage;

    /** Các buổi học cụ thể theo ngày (chỉ có ở yêu cầu DIRECT tạo từ khi có bảng connection_request_sessions). */
    @OneToMany(mappedBy = "connectionRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sessionDate ASC, startTime ASC")
    @BatchSize(size = 50)
    @Builder.Default
    private List<ConnectionRequestSession> sessions = new ArrayList<>();

    /** Các ca hằng tuần học viên chọn (chỉ có ở yêu cầu DIRECT). */
    @OneToMany(mappedBy = "connectionRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayOfWeek ASC, startTime ASC")
    @BatchSize(size = 50) // danh sách yêu cầu: nạp ca của nhiều yêu cầu trong 1 query thay vì mỗi yêu cầu 1 query
    @Builder.Default
    private List<ConnectionRequestSlot> slots = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "responded_at")
    private OffsetDateTime respondedAt;
}

