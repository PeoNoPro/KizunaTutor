package com.kizuna.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity lưu trữ thông tin các Vai trò (Role) trong hệ thống KizunaTutor.
 * Bao gồm 6 Actor chính:
 * 1. ROLE_ADMIN: Quản trị viên
 * 2. ROLE_MANAGER: Quản lý vận hành
 * 3. ROLE_CS_STAFF: Nhân viên CSKH
 * 4. ROLE_ACADEMIC_OFFICER: Cán bộ học vụ
 * 5. ROLE_TUTOR: Gia sư Kizuna
 * 6. ROLE_LEARNER: Học viên
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id", nullable = false)
    private Short roleId;

    @Column(name = "role_name", length = 50, nullable = false, unique = true)
    private String roleName;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
