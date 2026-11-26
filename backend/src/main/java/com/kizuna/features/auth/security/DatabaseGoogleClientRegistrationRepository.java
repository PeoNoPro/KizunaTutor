package com.kizuna.features.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.stereotype.Component;

/**
 * Cung cấp ClientRegistration "google" đọc động từ PostgreSQL (system_configurations),
 * giúp pull code từ Git về là chạy ngay không cần lưu secret vào repo hay .env.
 */
@Primary
@Component
@RequiredArgsConstructor
public class DatabaseGoogleClientRegistrationRepository implements ClientRegistrationRepository {

    private final GoogleOAuthConfigService configService;

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        if (!"google".equalsIgnoreCase(registrationId)) {
            return null;
        }
        GoogleOAuthConfigService.GoogleOAuthConfig cfg = configService.currentConfig();
        return ClientRegistration.withRegistrationId("google")
                .clientId(cfg.clientId())
                .clientSecret(cfg.clientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(cfg.redirectUri())
                .scope("email", "profile", "openid")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .userNameAttributeName("sub")
                .clientName("Google")
                .build();
    }
}
