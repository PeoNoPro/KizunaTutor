package com.kizuna.features.notification.entity;



import com.kizuna.core.entity.*;
import com.kizuna.core.entity.BaseEntity;
import com.kizuna.core.entity.User;
import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Tutor;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "user_notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserNotification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "userNotificationId")
    private UUID userNotificationId;
}
