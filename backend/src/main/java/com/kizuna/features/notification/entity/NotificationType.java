package com.kizuna.features.notification.entity;



import com.kizuna.core.entity.*;
import com.kizuna.core.entity.BaseEntity;
import com.kizuna.core.entity.User;
import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Tutor;
public enum NotificationType {
    SYSTEM, TRIAL, ASSIGNMENT, PAYMENT, TICKET,
    /** Kết nối gia sư trực tiếp (migration 023_notification_type_connection.sql). */
    CONNECTION
}
