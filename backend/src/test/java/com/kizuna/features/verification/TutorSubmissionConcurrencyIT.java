package com.kizuna.features.verification;

import com.kizuna.core.entity.Role;
import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.core.repository.RoleRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.verification.dto.request.SubmitVerificationRequest;
import com.kizuna.features.verification.entity.CredentialType;
import com.kizuna.features.verification.entity.TutorCredential;
import com.kizuna.features.verification.entity.TutorVerifAction;
import com.kizuna.features.verification.entity.TutorVerificationHistory;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
import com.kizuna.features.verification.repository.TutorCredentialRepository;
import com.kizuna.features.verification.repository.TutorVerificationHistoryRepository;
import com.kizuna.features.verification.repository.VerificationTutorRepository;
import com.kizuna.features.verification.service.TutorVerificationService;
import com.kizuna.features.verification.service.VerificationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * FR-008: hai lần nộp đồng thời chỉ ghi nhận một lần. Cần PostgreSQL kiểm thử riêng
 * (biến VERIFICATION_TEST_DB_URL); tự bỏ qua khi không có. KHÔNG trỏ biến này tới DB dùng chung.
 */
@SpringBootTest
@ActiveProfiles("verification-test")
@EnabledIfEnvironmentVariable(named = "VERIFICATION_TEST_DB_URL", matches = ".+")
class TutorSubmissionConcurrencyIT {

    @Autowired private TutorVerificationService service;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private VerificationTutorRepository tutorRepository;
    @Autowired private TutorCredentialRepository credentialRepository;
    @Autowired private TutorVerificationHistoryRepository historyRepository;
    @Autowired private org.springframework.core.env.Environment environment;

    private User user;

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        if (user != null) {
            userRepository.deleteById(user.getUserId());
        }
    }

    @Test
    void concurrentSubmitsRecordExactlyOneSubmission() throws Exception {
        Role tutorRole = roleRepository.findByRoleName("ROLE_TUTOR").orElseThrow();
        user = userRepository.save(User.builder()
                .role(tutorRole)
                .email("fe03-it-" + UUID.randomUUID() + "@kizuna.test")
                .passwordHash("x")
                .fullName("Gia sư kiểm thử")
                .build());
        Tutor tutor = tutorRepository.save(Tutor.builder()
                .user(user)
                .verificationStatus(TutorVerificationStatus.UNVERIFIED)
                .phone("0905123456")
                .dateOfBirth(LocalDate.of(1995, 1, 1))
                .address("Đà Nẵng")
                .bio("Giới thiệu dành cho kiểm thử tích hợp FE-03.")
                .highestJlptLevel("N2")
                .build());
        Path dir = Path.of(environment.getProperty("app.verification.storage-dir"));
        Files.createDirectories(dir);
        for (CredentialType type : List.of(CredentialType.IDENTITY_DOCUMENT, CredentialType.JLPT_CERTIFICATE)) {
            String key = UUID.randomUUID() + ".pdf";
            Files.writeString(dir.resolve(key), "%PDF-1.4 %%EOF");
            credentialRepository.save(TutorCredential.builder()
                    .tutor(tutor).credentialType(type).title(type.name()).storageKey(key).build());
        }

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user, null,
                List.of(new SimpleGrantedAuthority("ROLE_TUTOR")));
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> submit = () -> {
            SecurityContextHolder.getContext().setAuthentication(auth);
            start.await();
            try {
                service.submit(new SubmitVerificationRequest(true));
                return true;
            } catch (VerificationException e) {
                return false;
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<Boolean> first = pool.submit(submit);
        Future<Boolean> second = pool.submit(submit);
        start.countDown();
        int successes = (first.get() ? 1 : 0) + (second.get() ? 1 : 0);
        pool.shutdown();

        assertEquals(1, successes);
        List<TutorVerificationHistory> timeline = historyRepository.findTimeline(tutor.getTutorId());
        assertEquals(1, timeline.stream().filter(h -> h.getAction() == TutorVerifAction.SUBMIT).count());
    }
}
