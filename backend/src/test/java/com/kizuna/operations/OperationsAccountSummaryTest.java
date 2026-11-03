package com.kizuna.operations;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

class OperationsAccountSummaryTest extends OperationsIntegrationBase {
    @Test void currentAdministratorUsesDatabaseIdentityAndRequiresAdmin() throws Exception {
        mvc.perform(get("/api/admin/operations/me").with(admin())).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.displayName").value("Người kiểm thử 0"))
            .andExpect(jsonPath("$.data.roleLabel").value("Quản trị viên hệ thống"))
            .andExpect(jsonPath("$.data.email").doesNotExist());
        mvc.perform(get("/api/admin/operations/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/operations/me").with(user("ROLE_LEARNER@test.invalid"))).andExpect(status().isForbidden());
    }
    @Test void countsUndeletedAccountsAndActiveStateWithoutExposingIdentity() throws Exception {
        UUID inactive = UUID.randomUUID(), deleted = UUID.randomUUID();
        try {
            jdbc.update("INSERT INTO users(user_id,role_id,email,full_name,account_status) VALUES (?,2,?,'Kiểm thử không hoạt động','INACTIVE')", inactive, inactive + "@test.invalid");
            jdbc.update("INSERT INTO users(user_id,role_id,email,full_name,deleted_at) VALUES (?,3,?,'Kiểm thử đã xóa',now())", deleted, deleted + "@test.invalid");
            mvc.perform(get("/api/admin/operations/dashboard/accounts").with(admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalAccounts").value(8))
                .andExpect(jsonPath("$.data.activeAccounts").value(7))
                .andExpect(jsonPath("$.data.tutorAccounts").value(1))
                .andExpect(jsonPath("$.data.learnerAccounts").value(2))
                .andExpect(jsonPath("$.data.activeLearnerAccounts").value(1))
                .andExpect(jsonPath("$.data.staffAccounts").value(5))
                .andExpect(jsonPath("$.data.otherAccounts").value(0))
                .andExpect(jsonPath("$.data.generatedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.email").doesNotExist());
        } finally { jdbc.update("DELETE FROM users WHERE user_id IN (?,?)", inactive, deleted); }
    }

    @Test void summaryRequiresCurrentAdministrator() throws Exception {
        mvc.perform(get("/api/admin/operations/dashboard/accounts")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/operations/dashboard/accounts").with(user("ROLE_LEARNER@test.invalid")))
            .andExpect(status().isForbidden());
    }
}
