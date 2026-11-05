package com.kizuna.features.payment;

import com.kizuna.core.entity.Role;
import com.kizuna.core.entity.User;
import com.kizuna.core.entity.UserAccountStatus;
import com.kizuna.core.exception.ConflictException;
import com.kizuna.core.exception.ServiceUnavailableException;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.core.security.JwtAuthenticationFilter;
import com.kizuna.core.security.JwtTokenProvider;
import com.kizuna.core.security.RestAuthErrorHandlers;
import com.kizuna.core.security.SecurityConfig;
import com.kizuna.features.payment.controller.AdminPaymentTransactionController;
import com.kizuna.features.payment.controller.ManagerMembershipPlanController;
import com.kizuna.features.payment.controller.ManagerTutorMembershipController;
import com.kizuna.features.payment.controller.TutorMembershipController;
import com.kizuna.features.payment.controller.VnpayIpnController;
import com.kizuna.features.payment.dto.response.MembershipViews;
import com.kizuna.features.payment.service.CurrentUserAccessor;
import com.kizuna.features.payment.service.MembershipPlanAdminService;
import com.kizuna.features.payment.service.PaymentIssueService;
import com.kizuna.features.payment.service.PaymentMessages;
import com.kizuna.features.payment.service.TutorMembershipMonitorService;
import com.kizuna.features.payment.service.TutorMembershipService;
import com.kizuna.features.payment.service.VnpayIpnService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phân quyền (FR-023, SC-007), IPN công khai (FR-004) và khung phản hồi BACKEND_RULES 6.2/6.3. Không chạm DB.
 */
@WebMvcTest(controllers = {TutorMembershipController.class, VnpayIpnController.class, ManagerMembershipPlanController.class,
        ManagerTutorMembershipController.class, AdminPaymentTransactionController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthErrorHandlers.class, CurrentUserAccessor.class})
class PaymentControllersTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private TutorMembershipService tutorService;
    @MockBean private VnpayIpnService ipnService;
    @MockBean private MembershipPlanAdminService planService;
    @MockBean private TutorMembershipMonitorService monitorService;
    @MockBean private PaymentIssueService issueService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private UserRepository userRepository;

    private static RequestPostProcessor as(String role) {
        User user = User.builder().userId(UUID.randomUUID()).email("u@kizuna.test").fullName("Người dùng")
                .role(Role.builder().roleName(role).build()).accountStatus(UserAccountStatus.ACTIVE).build();
        return authentication(new UsernamePasswordAuthenticationToken(user, null, List.of(new SimpleGrantedAuthority(role))));
    }

    @Test
    void anonymousIsUnauthorizedOnProtectedRoutes() throws Exception {
        mockMvc.perform(get("/api/tutor/memberships/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/manager/membership-plans")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/payment-transactions")).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongRolesAreForbidden() throws Exception {
        mockMvc.perform(get("/api/tutor/memberships/me").with(as("ROLE_LEARNER"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/manager/membership-plans").with(as("ROLE_TUTOR"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/manager/tutor-memberships").with(as("ROLE_ADMIN"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/payment-transactions").with(as("ROLE_MANAGER"))).andExpect(status().isForbidden());
    }

    @Test
    void ipnIsPublicAndReturnsVnpayFormat() throws Exception {
        when(ipnService.handle(anyMap())).thenReturn(new VnpayIpnService.IpnReply("97", "Invalid signature"));
        mockMvc.perform(get("/api/payments/vnpay/ipn").param("vnp_TxnRef", "abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("97"))
                .andExpect(jsonPath("$.Message").value("Invalid signature"))
                .andExpect(jsonPath("$.success").doesNotExist());
    }

    @Test
    void tutorPlansUseSuccessEnvelope() throws Exception {
        when(tutorService.listPlans()).thenReturn(List.of(
                new MembershipViews.PlanView(UUID.randomUUID(), "Gói 1 tháng", 1, 199_000L, List.of("Mở khóa phòng học"))));
        mockMvc.perform(get("/api/tutor/memberships/plans").with(as("ROLE_TUTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].price").value(199000))
                .andExpect(jsonPath("$.data[0].benefits[0]").value("Mở khóa phòng học"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void createPaymentReturns201AndConflictUsesFriendlyMessage() throws Exception {
        UUID planId = UUID.randomUUID();
        when(tutorService.createPayment(any(), eq(planId.toString()), any())).thenReturn(new MembershipViews.PaymentCreatedView(
                UUID.randomUUID(), 199_000L, "Gói 1 tháng", "https://pay.test?x", OffsetDateTime.now()));
        mockMvc.perform(post("/api/tutor/memberships/payments").with(as("ROLE_TUTOR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"planId\":\"" + planId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.paymentUrl").value("https://pay.test?x"));

        when(tutorService.createPayment(any(), eq("blocked"), any()))
                .thenThrow(new ConflictException(PaymentMessages.MSG34_NOT_APPROVED));
        mockMvc.perform(post("/api/tutor/memberships/payments").with(as("ROLE_TUTOR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"planId\":\"blocked\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(PaymentMessages.MSG34_NOT_APPROVED));
    }

    @Test
    void missingPlanIdIsValidationError() throws Exception {
        mockMvc.perform(post("/api/tutor/memberships/payments").with(as("ROLE_TUTOR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Vui lòng chọn gói hội viên."));
    }

    @Test
    void gatewayUnavailableIs503() throws Exception {
        when(issueService.checkWithGateway(eq("abc"), any(), any()))
                .thenThrow(new ServiceUnavailableException(PaymentMessages.MSG37_GATEWAY_UNAVAILABLE));
        mockMvc.perform(post("/api/admin/payment-transactions/abc/gateway-check").with(as("ROLE_ADMIN")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(PaymentMessages.MSG37_GATEWAY_UNAVAILABLE));
    }

    @Test
    void managerCanListPlans() throws Exception {
        when(planService.listPlans()).thenReturn(List.of());
        mockMvc.perform(get("/api/manager/membership-plans").with(as("ROLE_MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
