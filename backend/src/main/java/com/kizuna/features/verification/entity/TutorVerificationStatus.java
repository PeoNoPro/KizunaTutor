package com.kizuna.features.verification.entity;



import com.kizuna.core.entity.*;
import com.kizuna.core.entity.BaseEntity;
import com.kizuna.core.entity.User;
import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Tutor;
public enum TutorVerificationStatus {
    UNVERIFIED, PENDING, APPROVED, REJECTED, ADDITIONAL_INFO_REQUIRED
}
