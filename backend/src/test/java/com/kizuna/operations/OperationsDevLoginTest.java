package com.kizuna.operations;

import com.kizuna.core.security.JwtAuthenticationAdapter;
import com.kizuna.core.security.JwtTokenVerifier;
import com.kizuna.features.admin.config.OperationsDevLoginSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OperationsDevLoginTest {
    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    private MockEnvironment local() {
        var environment = new MockEnvironment().withProperty("server.address", "127.0.0.1")
            .withProperty("spring.datasource.url", "jdbc:postgresql://127.0.0.1:5433/kizuna_admin_dev");
        environment.setActiveProfiles("admin-dev");
        return environment;
    }

    @Test void defaultRuntimeHasNoDevelopmentIdentity() {
        assertNull(OperationsDevLoginSettings.validatedEmail(new MockEnvironment(), ""));
    }

    @Test void developmentIdentityRequiresOnlyExplicitDevelopmentProfile() {
        assertThrows(IllegalStateException.class, () -> OperationsDevLoginSettings.validatedEmail(new MockEnvironment(), "test@test.invalid"));
        var environment = local();
        environment.setActiveProfiles("admin-dev", "production");
        assertThrows(IllegalStateException.class, () -> OperationsDevLoginSettings.validatedEmail(environment, "test@test.invalid"));
    }

    @Test void developmentIdentityRejectsRemoteDatabaseAndPublicBinding() {
        var environment = local().withProperty("server.address", "0.0.0.0");
        assertThrows(IllegalStateException.class, () -> OperationsDevLoginSettings.validatedEmail(environment, "test@test.invalid"));
        environment.withProperty("server.address", "127.0.0.1").withProperty("spring.datasource.url", "jdbc:postgresql://remote.invalid/kizuna_admin_dev");
        assertThrows(IllegalStateException.class, () -> OperationsDevLoginSettings.validatedEmail(environment, "test@test.invalid"));
        environment.withProperty("spring.datasource.url", "jdbc:postgresql://127.0.0.1/kizuna_db");
        assertThrows(IllegalStateException.class, () -> OperationsDevLoginSettings.validatedEmail(environment, "test@test.invalid"));
        environment.withProperty("spring.datasource.url", "jdbc:postgresql://127.0.0.1/kizuna_admin_dev?socketFactory=other");
        assertThrows(IllegalStateException.class, () -> OperationsDevLoginSettings.validatedEmail(environment, "test@test.invalid"));
    }

    @Test void isolatedDevelopmentSettingsAreAccepted() {
        assertEquals("test@test.invalid", OperationsDevLoginSettings.validatedEmail(local(), " test@test.invalid "));
    }

    @Test void onlyLocalTokenlessOperationsRequestsReceiveDevelopmentIdentity() throws Exception {
        var verifier = mock(JwtTokenVerifier.class);
        var filter = new JwtAuthenticationAdapter(verifier, "test@test.invalid");
        var request = new MockHttpServletRequest("GET", "/api/admin/operations/health");
        request.setRemoteAddr("127.0.0.1");
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        assertEquals("test@test.invalid", SecurityContextHolder.getContext().getAuthentication().getName());
        SecurityContextHolder.clearContext();
        request = new MockHttpServletRequest("GET", "/api/admin/operations/health");
        request.setRemoteAddr("192.0.2.1");
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test void developmentIdentityDoesNotReplaceInvalidTokensOrOtherFeatures() throws Exception {
        var filter = new JwtAuthenticationAdapter(mock(JwtTokenVerifier.class), "test@test.invalid");
        var request = new MockHttpServletRequest("GET", "/api/admin/operations/health");
        request.addHeader("Authorization", "Bearer invalid-test-token");
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        request = new MockHttpServletRequest("GET", "/api/policies");
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
