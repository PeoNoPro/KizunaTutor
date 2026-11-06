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

    @Test void statisticsCountRolesStatusesAndNewAccountsByVietnamDay() throws Exception {
        UUID suspended = UUID.randomUUID(), late = UUID.randomUUID(), deleted = UUID.randomUUID();
        try {
            jdbc.update("INSERT INTO users(user_id,role_id,email,full_name,account_status,created_at) VALUES (?,2,?,'Kiểm thử tạm khóa','SUSPENDED','2026-03-10T10:00:00+07:00')", suspended, suspended + "@test.invalid");
            // 17:30 UTC ngày 10 đã là ngày 11 theo giờ Việt Nam.
            jdbc.update("INSERT INTO users(user_id,role_id,email,full_name,created_at) VALUES (?,2,?,'Kiểm thử qua ngày','2026-03-10T17:30:00Z')", late, late + "@test.invalid");
            jdbc.update("INSERT INTO users(user_id,role_id,email,full_name,created_at,deleted_at) VALUES (?,2,?,'Kiểm thử đã xóa','2026-03-10T10:00:00+07:00',now())", deleted, deleted + "@test.invalid");
            String path = "/api/admin/operations/dashboard/account-statistics";
            mvc.perform(get(path).param("startDate", "2026-03-10").param("endDate", "2026-03-11").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.newAccounts.length()").value(2))
                .andExpect(jsonPath("$.data.newAccounts[0].date").value("2026-03-10"))
                .andExpect(jsonPath("$.data.newAccounts[0].count").value(1))
                .andExpect(jsonPath("$.data.newAccounts[1].count").value(1))
                .andExpect(jsonPath("$.data.statuses[?(@.label=='Tạm khóa')].count").value(1))
                .andExpect(jsonPath("$.data.roles[?(@.label=='Quản trị viên hệ thống')].count").isNotEmpty())
                .andExpect(jsonPath("$.data.email").doesNotExist());
            mvc.perform(get(path).param("startDate", "2026-03-11").param("endDate", "2026-03-10").with(admin())).andExpect(status().isBadRequest());
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
            mvc.perform(get(path).with(user("ROLE_MANAGER@test.invalid"))).andExpect(status().isForbidden());
        } finally { jdbc.update("DELETE FROM users WHERE user_id IN (?,?,?)", suspended, late, deleted); }
    }

    @Test void summaryRequiresCurrentAdministrator() throws Exception {
        mvc.perform(get("/api/admin/operations/dashboard/accounts")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/operations/dashboard/accounts").with(user("ROLE_LEARNER@test.invalid")))
            .andExpect(status().isForbidden());
    }
}
