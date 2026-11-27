package com.kizuna.features.verification;

import com.kizuna.core.entity.User;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.verification.dto.request.UpdateOfficerProfileRequest;
import com.kizuna.features.verification.entity.TutorVerifAction;
import com.kizuna.features.verification.repository.TutorVerificationHistoryRepository;
import com.kizuna.features.verification.service.AcademicOfficerService;
import com.kizuna.features.verification.service.CurrentUserProvider;
import com.kizuna.features.verification.service.VerificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Trang "Lịch sử xét duyệt" và "Hồ sơ cá nhân" của cán bộ học vụ. */
class AcademicOfficerServiceTest {

    TutorVerificationHistoryRepository historyRepository = mock(TutorVerificationHistoryRepository.class);
    UserRepository userRepository = mock(UserRepository.class);
    CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    AcademicOfficerService service = new AcademicOfficerService(historyRepository, userRepository, currentUserProvider);

    User officer = User.builder().userId(UUID.randomUUID()).email("hv@kizuna.test").fullName("Cán bộ A").build();

    @BeforeEach
    void setUp() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(officer);
        when(userRepository.findById(officer.getUserId())).thenReturn(Optional.of(officer));
        when(historyRepository.searchDecisions(any(), anyString(), any())).thenReturn(Page.empty());
    }

    @SuppressWarnings("unchecked")
    private Collection<TutorVerifAction> searchedActions(String action) {
        service.getReviewHistory(action, null, null, null);
        ArgumentCaptor<Collection<TutorVerifAction>> actions = ArgumentCaptor.forClass(Collection.class);
        verify(historyRepository, atLeastOnce()).searchDecisions(actions.capture(), anyString(), any(Pageable.class));
        return actions.getValue();
    }

    @Test
    void historyDefaultsToAllThreeDecisions() {
        assertEquals(3, searchedActions("ALL").size());
        assertTrue(searchedActions(null).containsAll(
                java.util.List.of(TutorVerifAction.APPROVE, TutorVerifAction.REJECT, TutorVerifAction.REQUEST_INFO)));
    }

    @Test
    void historyFiltersBySingleDecision() {
        assertEquals(java.util.Set.of(TutorVerifAction.REJECT), java.util.Set.copyOf(searchedActions("reject")));
    }

    @Test
    void historyRejectsUnknownActionAndTooLongKeyword() {
        assertThrows(VerificationException.class, () -> service.getReviewHistory("SUBMIT", null, 0, 10));
        assertThrows(VerificationException.class, () -> service.getReviewHistory("ALL", "x".repeat(101), 0, 10));
    }

    @Test
    void historyCapsPageSize() {
        service.getReviewHistory("ALL", "  ", 0, 500);
        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        ArgumentCaptor<String> pattern = ArgumentCaptor.forClass(String.class);
        verify(historyRepository).searchDecisions(any(), pattern.capture(), page.capture());
        assertEquals(50, page.getValue().getPageSize());
        assertEquals("", pattern.getValue());
    }

    @Test
    void updateProfileValidatesNameAndPhone() {
        UpdateOfficerProfileRequest request = new UpdateOfficerProfileRequest();
        request.setFullName(" ");
        request.setPhone("12345");

        VerificationException e = assertThrows(VerificationException.class, () -> service.updateProfile(request));
        assertTrue(e.getFields().containsKey("fullName"));
        assertTrue(e.getFields().containsKey("phone"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateProfileSavesTrimmedValues() {
        UpdateOfficerProfileRequest request = new UpdateOfficerProfileRequest();
        request.setFullName("  Nguyễn Văn B  ");
        request.setPhone("0912345678");

        Map<String, Object> result = service.updateProfile(request);

        assertEquals("Nguyễn Văn B", officer.getFullName());
        assertEquals("0912345678", officer.getPhone());
        assertEquals(AcademicOfficerService.MSG_PROFILE_UPDATED, result.get("message"));
        verify(userRepository).save(officer);
    }
}
