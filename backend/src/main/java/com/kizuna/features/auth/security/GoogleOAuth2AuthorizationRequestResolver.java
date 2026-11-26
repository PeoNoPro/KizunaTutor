package com.kizuna.features.auth.security;

import com.kizuna.core.entity.RoleEnum;
import com.kizuna.features.auth.controller.GoogleOAuthController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/**
 * Tùy biến khởi tạo OAuth2 Authorization Request cho Google:
 * - Chặn ngay khi Client ID đang trống hoặc là placeholder mặc định (để GoogleOAuthController chuyển về /login?error=oauth2_not_configured).
 * - Lưu vai trò đã chọn (chỉ nhận ROLE_LEARNER hoặc ROLE_TUTOR) vào HttpSession để dùng khi Google callback về.
 */
public class GoogleOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    public static final String SESSION_SELECTED_ROLE = "OAUTH2_SELECTED_ROLE";
    private static final String AUTHORIZATION_BASE_URI = "/oauth2/authorization";

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final DefaultOAuth2AuthorizationRequestResolver delegate;

    public GoogleOAuth2AuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, AUTHORIZATION_BASE_URI);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authorizationRequest = delegate.resolve(request);
        if (authorizationRequest == null) {
            return null;
        }
        return handleIfGoogleConfigured(request, authorizationRequest);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest authorizationRequest = delegate.resolve(request, clientRegistrationId);
        if (authorizationRequest == null) {
            return null;
        }
        return handleIfGoogleConfigured(request, authorizationRequest);
    }

    private OAuth2AuthorizationRequest handleIfGoogleConfigured(HttpServletRequest request,
                                                                OAuth2AuthorizationRequest authorizationRequest) {
        ClientRegistration google = clientRegistrationRepository.findByRegistrationId("google");
        String clientId = google != null ? google.getClientId() : null;
        if (clientId == null || clientId.isBlank()
                || GoogleOAuthController.PLACEHOLDER_CLIENT_ID.equals(clientId.trim())) {
            return null;
        }

        String requestedRole = request != null ? request.getParameter("role") : null;
        String safeRole = RoleEnum.ROLE_TUTOR.name().equals(requestedRole)
                ? RoleEnum.ROLE_TUTOR.name()
                : RoleEnum.ROLE_LEARNER.name();

        if (request != null) {
            HttpSession session = request.getSession(true);
            session.setAttribute(SESSION_SELECTED_ROLE, safeRole);
        }
        return authorizationRequest;
    }
}
