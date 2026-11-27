package com.kizuna.features.auth;

import com.kizuna.core.entity.Role;
import com.kizuna.core.entity.User;
import com.kizuna.core.entity.UserAccountStatus;
import com.kizuna.core.repository.RoleRepository;
import com.kizuna.core.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@SpringBootTest
public class ProvisionTestUsersTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void provisionAccounts() {
        Role learnerRole = roleRepository.findByRoleName("ROLE_LEARNER")
                .orElseThrow(() -> new IllegalStateException("ROLE_LEARNER not found"));

        // 1. Account for UTCID07: mustChangePassword = true
        Optional<User> firstTimeOpt = userRepository.findByEmail("firsttime@kizuna.vn");
        User firstTimeUser = firstTimeOpt.orElseGet(() -> User.builder()
                .email("firsttime@kizuna.vn")
                .fullName("Người Dùng Lần Đầu")
                .role(learnerRole)
                .build());
        firstTimeUser.setPasswordHash(passwordEncoder.encode("123456"));
        firstTimeUser.setAccountStatus(UserAccountStatus.ACTIVE);
        firstTimeUser.setMustChangePassword(true);
        userRepository.save(firstTimeUser);
        System.out.println(">>> PROVISIONED: firsttime@kizuna.vn (mustChange=true, pass=123456, status=ACTIVE)");

        // 2. Account for UTCID10: status = SUSPENDED
        Optional<User> bannedOpt = userRepository.findByEmail("banned@kizuna.vn");
        User bannedUser = bannedOpt.orElseGet(() -> User.builder()
                .email("banned@kizuna.vn")
                .fullName("Tài Khoản Bị Khóa")
                .role(learnerRole)
                .build());
        bannedUser.setPasswordHash(passwordEncoder.encode("123456"));
        bannedUser.setAccountStatus(UserAccountStatus.SUSPENDED);
        bannedUser.setMustChangePassword(false);
        userRepository.save(bannedUser);
        System.out.println(">>> PROVISIONED: banned@kizuna.vn (mustChange=false, pass=123456, status=SUSPENDED)");
    }
}
