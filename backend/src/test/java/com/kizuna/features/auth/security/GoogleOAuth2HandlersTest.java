package com.kizuna.features.auth.security;

import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.auth.controller.GoogleOAuthController;
import com.kizuna.features.auth.dto.response.LoginResponse;
import com.kizuna.features.auth.dto.response.UserSummaryDto;
import com.kizuna.features.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("Google OAuth2 Resolver & Handlers Unit Tests")
class GoogleOAuth2HandlersTest {

    private AuthService authService;
    private GoogleOAuth2SuccessHandler successHandler;
    private GoogleOAuth2FailureHandler failureHandler;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        successHandler = new GoogleOAuth2SuccessHandler(authService);
        failureHandler = new GoogleOAuth2FailureHandler();
    }

    private ClientRegistrationRepository googleRepoWithClientId(String clientId) {
        ClientRegistration reg = ClientRegistration.withRegistrationId("google")
                .clientId(clientId)
                .clientSecret("real-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:3000/login/oauth2/code/google")
                .scope("openid", "email", "profile")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .userNameAttributeName("sub")
                .clientName("Google")
                .build();
        return new InMemoryClientRegistrationRepository(reg);
    }

    @Test
    @DisplayName("Placeholder Client ID -> Resolver trả về null để GoogleOAuthController xử lý redirect báo lỗi cấu hình")
    void resolver_whenClientIdIsPlaceholder_returnsNull() {
        GoogleOAuth2AuthorizationRequestResolver resolver = new GoogleOAuth2AuthorizationRequestResolver(
                googleRepoWithClientId(GoogleOAuthController.PLACEHOLDER_CLIENT_ID));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorization/google");
        request.setServletPath("/oauth2/authorization/google");

        OAuth2AuthorizationRequest authReq = resolver.resolve(request);

        assertThat(authReq).isNull();
    }

    @Test
    @DisplayName("Client ID thật + ?role=ROLE_TUTOR -> Lưu ROLE_TUTOR vào session và tạo OAuth2AuthorizationRequest")
    void resolver_whenValidClientIdAndTutorRole_storesTutorRoleInSession() {
        GoogleOAuth2AuthorizationRequestResolver resolver = new GoogleOAuth2AuthorizationRequestResolver(
                googleRepoWithClientId("123456789-real.apps.googleusercontent.com"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorization/google");
        request.setServletPath("/oauth2/authorization/google");
        request.setParameter("role", "ROLE_TUTOR");

        OAuth2AuthorizationRequest authReq = resolver.resolve(request);

        assertThat(authReq).isNotNull();
        assertThat(authReq.getRedirectUri()).isEqualTo("http://localhost:3000/login/oauth2/code/google");
        assertThat(request.getSession(false).getAttribute(GoogleOAuth2AuthorizationRequestResolver.SESSION_SELECTED_ROLE))
                .isEqualTo("ROLE_TUTOR");
    }

    @Test
    @DisplayName("Security Case: ?role=ROLE_ADMIN -> Chỉ lưu ROLE_LEARNER vào session")
    void resolver_whenAdminRoleRequested_storesLearnerRoleInSession() {
        GoogleOAuth2AuthorizationRequestResolver resolver = new GoogleOAuth2AuthorizationRequestResolver(
                googleRepoWithClientId("123456789-real.apps.googleusercontent.com"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorization/google");
        request.setServletPath("/oauth2/authorization/google");
        request.setParameter("role", "ROLE_ADMIN");

        resolver.resolve(request);

        assertThat(request.getSession(false).getAttribute(GoogleOAuth2AuthorizationRequestResolver.SESSION_SELECTED_ROLE))
                .isEqualTo("ROLE_LEARNER");
    }

    @Test
    @DisplayName("Thiếu email hoặc email_verified=false -> Redirect /login?error=email_not_provided")
    void successHandler_whenEmailMissingOrUnverified_redirectsEmailNotProvided() throws Exception {
        OAuth2User unverifiedUser = new DefaultOAuth2User(
                List.of(() -> "ROLE_USER"),
                Map.of("sub", "123", "email", "unverified@gmail.com", "email_verified", false),
                "sub"
        );
        OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(unverifiedUser, unverifiedUser.getAuthorities(), "google");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(request, response, auth);

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getHeader("Location")).isEqualTo("/login?error=email_not_provided");
        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("Google user hợp lệ -> Gọi authenticateWithGoogle, gắn cookie access_token và redirect relative /login?token=...")
    void successHandler_whenValidGoogleUser_setsCookieAndRedirectsRelativeLoginUrl() throws Exception {
        OAuth2User googleUser = new DefaultOAuth2User(
                List.of(() -> "ROLE_USER"),
                Map.of(
                        "sub", "123",
                        "email", "hocvien@gmail.com",
                        "email_verified", true,
                        "name", "Học Viên",
                        "picture", "https://lh3.googleusercontent.com/pic.png"
                ),
                "sub"
        );
        OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(googleUser, googleUser.getAuthorities(), "google");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(GoogleOAuth2AuthorizationRequestResolver.SESSION_SELECTED_ROLE, "ROLE_TUTOR");
        request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();

        LoginResponse loginResponse = LoginResponse.builder()
                .token("jwt-123")
                .user(UserSummaryDto.builder()
                        .email("hocvien@gmail.com")
                        .fullName("Học Viên")
                        .role("ROLE_TUTOR")
                        .build())
                .redirectUrl("/tutor/dashboard")
                .build();

        when(authService.authenticateWithGoogle(
                "hocvien@gmail.com",
                "Học Viên",
                "https://lh3.googleusercontent.com/pic.png",
                "ROLE_TUTOR"
        )).thenReturn(loginResponse);

        successHandler.onAuthenticationSuccess(request, response, auth);

        assertThat(response.getStatus()).isEqualTo(302);
        String location = response.getHeader("Location");
        assertThat(location).startsWith("/login?token=jwt-123");
        assertThat(location).contains("role=ROLE_TUTOR");
        assertThat(location).doesNotContain("127.0.0.1:8085");

        Cookie cookie = response.getCookie("access_token");
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getValue()).isEqualTo("jwt-123");
        assertThat(session.getAttribute(GoogleOAuth2AuthorizationRequestResolver.SESSION_SELECTED_ROLE)).isNull();
    }

    @Test
    @DisplayName("Tài khoản bị khóa -> Redirect /login?error=account_blocked")
    void successHandler_whenUserBlocked_redirectsAccountBlocked() throws Exception {
        OAuth2User googleUser = new DefaultOAuth2User(
                List.of(() -> "ROLE_USER"),
                Map.of("sub", "123", "email", "locked@gmail.com", "email_verified", true, "name", "Locked"),
                "sub"
        );
        OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(googleUser, googleUser.getAuthorities(), "google");

        when(authService.authenticateWithGoogle(anyString(), any(), any(), anyString()))
                .thenThrow(new BusinessException(403, "Tài khoản bị khóa"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        successHandler.onAuthenticationSuccess(request, response, auth);

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getHeader("Location")).isEqualTo("/login?error=account_blocked");
    }

    @Test
    @DisplayName("FailureHandler -> Xóa role session và redirect /login?error=oauth2_failure")
    void failureHandler_redirectsOAuth2Failure() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(GoogleOAuth2AuthorizationRequestResolver.SESSION_SELECTED_ROLE, "ROLE_TUTOR");
        request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();

        failureHandler.onAuthenticationFailure(request, response, new BadCredentialsException("denied"));

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getHeader("Location")).isEqualTo("/login?error=oauth2_failure");
        assertThat(session.getAttribute(GoogleOAuth2AuthorizationRequestResolver.SESSION_SELECTED_ROLE)).isNull();
    }
}
