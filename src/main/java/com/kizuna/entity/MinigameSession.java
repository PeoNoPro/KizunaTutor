package com.kizuna.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity lưu lại toàn bộ phiên chơi Minigame Bắn chữ Kanji (Kanji Rain Typing)
 * Bao gồm các chỉ số thống kê về thời gian chơi, từ gõ đúng, độ chính xác, điểm số và trạng thái thắng/thua.
 */
@Entity
@Table(name = "minigame_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MinigameSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "session_id", updatable = false, nullable = false)
    private UUID sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private KanjiFolder folder;

    @Column(name = "folder_name", length = 100)
    private String folderName;

    /**
     * Thời gian thực tế đã chơi (giây)
     */
    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;

    /**
     * Thời gian mục tiêu đã chọn (60s, 180s, 300s)
     */
    @Column(name = "target_duration_seconds")
    private Integer targetDurationSeconds;

    /**
     * Tổng số từ Hiragana đã gõ chính xác
     */
    @Column(name = "words_typed_count", nullable = false)
    private Integer wordsTypedCount;

    /**
     * Tổng số lần gõ sai (buzz)
     */
    @Column(name = "total_mistakes", nullable = false)
    @Builder.Default
    private Integer totalMistakes = 0;

    /**
     * Độ chính xác tính theo tỷ lệ phần trăm (0.0% -> 100.0%)
     */
    @Column(name = "accuracy", nullable = false)
    private Double accuracy;

    /**
     * Điểm số tích lũy của ván đấu
     */
    @Column(name = "score", nullable = false)
    @Builder.Default
    private Integer score = 0;

    /**
     * Trạng thái hoàn thành: true = Thắng (hết giờ an toàn), false = Thua (chạm vạch Deadline)
     */
    @Column(name = "is_won", nullable = false)
    private Boolean isWon;

    /**
     * ID chữ Kanji gây ra kết thúc trận (Game Over do chạm vạch Deadline)
     */
    @Column(name = "failed_kanji_id")
    private UUID failedKanjiId;

    /**
     * Mặt chữ Kanji gây thua cuộc
     */
    @Column(name = "failed_kanji_character", length = 10)
    private String failedKanjiCharacter;

    /**
     * Danh sách chi tiết các lỗi sai trong phiên
     */
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MinigameMistakeLog> mistakeLogs = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "played_at", nullable = false, updatable = false)
    private OffsetDateTime playedAt;
}
