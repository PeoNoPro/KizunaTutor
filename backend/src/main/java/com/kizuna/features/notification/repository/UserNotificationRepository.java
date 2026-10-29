package com.kizuna.features.notification.repository;


import com.kizuna.core.entity.*;
import com.kizuna.features.notification.entity.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, UUID> {
}
