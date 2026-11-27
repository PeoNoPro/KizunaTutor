package com.kizuna.features.learning_support.entity;



import com.kizuna.core.entity.*;
import com.kizuna.core.entity.BaseEntity;
import com.kizuna.core.entity.User;
import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Tutor;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity ghi nhận chi tiết các từ Kanji bị gõ sai nhiều nhất trong phiên chơi minigame,
 * phục vụ phân tích điểm yếu học tập và nhắc nhở ôn luyện.
 */
@Entity
@Table(name = "minigame_mistake_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MinigameMistakeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "log_id", updatable = false, nullable = false)
    private UUID logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private MinigameSession session;

    @Column(name = "kanji_id")
    private UUID kanjiId;

    @Column(name = "kanji_character", length = 10, nullable = false)
    private String kanjiCharacter;

    @Column(name = "reading", length = 50)
    private String reading;

    @Column(name = "mistake_count", nullable = false)
    @Builder.Default
    private Integer mistakeCount = 1;

    @Column(name = "typed_input", length = 100)
    private String typedInput;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
