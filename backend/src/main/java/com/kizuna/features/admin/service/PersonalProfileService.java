package com.kizuna.features.admin.service;

import com.kizuna.core.exception.BusinessException;
import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

/** Nhân sự nội bộ tự sửa họ tên, số điện thoại và đổi mật khẩu của chính mình. Email và vai trò do Admin quản lý. */
@Service @RequiredArgsConstructor
public class PersonalProfileService {
    private static final BCryptPasswordEncoder ENCODER=new BCryptPasswordEncoder();
    private final EntityManager em;
    private final OperationsActorResolver actors;
    private final Clock operationsClock;

    public record ProfileUpdate(
        @NotBlank(message="Vui lòng nhập họ và tên.") @Size(max=100,message="Họ và tên tối đa 100 ký tự.") String fullName,
        @Pattern(regexp="^$|^[0-9+ ]{8,20}$",message="Số điện thoại gồm 8–20 chữ số.") String phone) { }

    public record PasswordChange(
        @NotBlank(message="Vui lòng nhập mật khẩu hiện tại.") String currentPassword,
        @NotBlank(message="Vui lòng nhập mật khẩu mới.") @Size(min=8,max=64,message="Mật khẩu mới dài 8–64 ký tự.")
        @Pattern(regexp="^(?=.*[A-Za-z])(?=.*[0-9]).+$",message="Mật khẩu mới cần có cả chữ và số.") String newPassword) { }

    @Transactional(timeout=5)
    public Map<String,Object> update(ProfileUpdate r) {
        var actor=actors.requireStaff();
        var phone=r.phone()==null||r.phone().isBlank()?null:r.phone().replace(" ","");
        try {
            em.createNativeQuery("UPDATE users SET full_name=:name,phone=:phone,updated_at=:now WHERE user_id=:id AND deleted_at IS NULL")
                .setParameter("name",r.fullName().trim()).setParameter("phone",phone).setParameter("now",OffsetDateTime.now(operationsClock)).setParameter("id",actor.userId()).executeUpdate();
            em.flush();
        } catch(DataIntegrityViolationException|jakarta.persistence.PersistenceException exception) {
            throw new BusinessException(409,"Số điện thoại này đã được tài khoản khác sử dụng.");
        }
        return actors.personalProfile();
    }

    @Transactional(timeout=5)
    public Map<String,Object> changePassword(PasswordChange r) {
        var actor=actors.requireStaff();
        var rows=em.createNativeQuery("SELECT password_hash FROM users WHERE user_id=:id AND deleted_at IS NULL FOR UPDATE").setParameter("id",actor.userId()).getResultList();
        var hash=rows.isEmpty()||rows.get(0)==null?"":rows.get(0).toString();
        if(!hash.startsWith("$2")||!ENCODER.matches(r.currentPassword(),hash))throw new BusinessException(400,"Mật khẩu hiện tại chưa đúng.");
        if(r.currentPassword().equals(r.newPassword()))throw new BusinessException(400,"Mật khẩu mới cần khác mật khẩu hiện tại.");
        em.createNativeQuery("UPDATE users SET password_hash=:hash,must_change_password=false,updated_at=:now WHERE user_id=:id")
            .setParameter("hash",ENCODER.encode(r.newPassword())).setParameter("now",OffsetDateTime.now(operationsClock)).setParameter("id",actor.userId()).executeUpdate();
        return Map.of("changed",true);
    }
}
