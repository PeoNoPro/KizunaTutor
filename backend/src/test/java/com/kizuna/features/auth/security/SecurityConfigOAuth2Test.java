package com.kizuna.features.auth.security;

import com.kizuna.core.repository.UserRepository;
import com.kizuna.core.security.JwtAuthenticationFilter;
import com.kizuna.core.security.JwtTokenProvider;
import com.kizuna.core.security.JwtTokenVerifier;
import com.kizuna.core.security.RestAuthErrorHandlers;
import com.kizuna.core.security.SecurityConfig;
import com.kizuna.features.admin.service.OperationsActorResolver;
import com.kizuna.features.auth.controller.GoogleOAuthController;
import com.kizuna.features.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GoogleOAuthController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthErrorHandlers.class,
        GoogleOAuth2SuccessHandler.class,
        GoogleOAuth2FailureHandler.class,
        SecurityConfigOAuth2Test.OAuthTestConfig.class
})
@TestPropertySource(properties = {
        "spring.security.oauth2.client.registration.google.client-id=123456789-realtest.apps.googleusercontent.com",
        "app.auth.google-redirect-uri=http://localhost:3000/login/oauth2/code/google"
})
@DisplayName("SecurityConfig Google OAuth2 Integration Tests")
class SecurityConfigOAuth2Test {

    @TestConfiguration
    static class OAuthTestConfig {
        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            ClientRegistration google = ClientRegistration.withRegistrationId("google")
                    .clientId("123456789-realtest.apps.googleusercontent.com")
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
            return new InMemoryClientRegistrationRepository(google);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtTokenVerifier jwtTokenVerifier;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private OperationsActorResolver operationsActorResolver;

    @Test
    @DisplayName("Bật oauth2Login() vẫn giữ 401 JSON cho route /api/** chưa đăng nhập (không redirect 302)")
    void apiEndpoint_unauthenticated_stillReturns401JsonNot302() throws Exception {
        mockMvc.perform(get("/api/tutor/verification/status"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("GET /oauth2/authorization/google?role=ROLE_TUTOR đi qua Spring oauth2Login() có state và lưu role vào session")
    void oauth2AuthorizationGoogle_withTutorRole_redirectsWithStateAndStoresRoleInSession() throws Exception {
        MvcResult result = mockMvc.perform(get("/oauth2/authorization/google").param("role", "ROLE_TUTOR"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String location = result.getResponse().getRedirectedUrl();
        assertThat(location).startsWith("https://accounts.google.com/o/oauth2/v2/auth");
        assertThat(location).contains("state=");
        assertThat(location).contains("redirect_uri=http://localhost:3000/login/oauth2/code/google");

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        assertThat(session.getAttribute(GoogleOAuth2AuthorizationRequestResolver.SESSION_SELECTED_ROLE))
                .isEqualTo("ROLE_TUTOR");
    }
}
