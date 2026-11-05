package com.kizuna.operations;

import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

class ManagementReportProfileTest extends OperationsIntegrationBase {
    private final List<UUID> connections=new ArrayList<>(),bookings=new ArrayList<>();
    private final LocalDate day=LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusDays(2);
    @BeforeEach void reportSchema(){
        jdbc.execute("CREATE TABLE IF NOT EXISTS connection_requests(request_id uuid PRIMARY KEY,status varchar(30),created_at timestamptz NOT NULL DEFAULT now())");
        jdbc.execute("ALTER TABLE trial_bookings ADD COLUMN IF NOT EXISTS created_at timestamptz NOT NULL DEFAULT now()");
        jdbc.execute("ALTER TABLE trial_bookings ADD COLUMN IF NOT EXISTS booking_status varchar(30)");
    }
    @AfterEach void cleanup(){for(var id:connections)jdbc.update("DELETE FROM connection_requests WHERE request_id=?",id);for(var id:bookings)jdbc.update("DELETE FROM trial_bookings WHERE booking_id=?",id);}
    private void connection(String status,LocalDate date){var id=UUID.randomUUID();connections.add(id);jdbc.update("INSERT INTO connection_requests(request_id,status,created_at) VALUES (?,?,?)",id,status,date.atTime(12,0).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime());}
    @Test void managerReadsRealCohortCountsAndCurrentStatuses() throws Exception {
        connection("ACCEPTED",day);connection("DECLINED",day);connection("ACCEPTED",day.minusDays(1));
        var booking=UUID.randomUUID();bookings.add(booking);jdbc.update("INSERT INTO trial_bookings(booking_id,booking_status,created_at) VALUES (?,'COMPLETED',?)",booking,day.atTime(12,0).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime());
        var data=data(mvc.perform(get("/api/admin/operations/management-report").param("startDate",day.toString()).param("endDate",day.toString()).with(user("ROLE_MANAGER@test.invalid")))
            .andExpect(status().isOk()).andReturn());
        assertEquals(2,data.get("connectionCount").asLong());assertEquals(1,data.get("bookingCount").asLong());assertEquals(50.0,data.get("acceptanceRatePercent").asDouble());assertEquals(7,data.get("totalAccounts").asLong());
        assertFalse(data.toString().contains("@test.invalid"));assertFalse(data.toString().contains("ROLE_"));
    }
    @Test void emptyCohortHasZeroCountsAndNoInventedRate() throws Exception {
        mvc.perform(get("/api/admin/operations/management-report").param("startDate",day.minusYears(1).toString()).param("endDate",day.minusYears(1).toString()).with(admin()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.connectionCount").value(0)).andExpect(jsonPath("$.data.bookingCount").value(0)).andExpect(jsonPath("$.data.acceptanceRatePercent").isEmpty());
    }
    @Test void reportPermissionDoesNotOpenAdministrativeApis() throws Exception {
        mvc.perform(get("/api/admin/operations/management-report")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/operations/management-report").with(user("ROLE_LEARNER@test.invalid"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/operations/staff").with(user("ROLE_MANAGER@test.invalid"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/operations/booking-policy").with(user("ROLE_MANAGER@test.invalid"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/operations/management-report").param("startDate",day.toString()).with(admin())).andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/operations/management-report").param("startDate",day.minusYears(2).toString()).param("endDate",day.toString()).with(admin())).andExpect(status().isBadRequest());
    }
    @Test void profileBelongsOnlyToCurrentInternalAccount() throws Exception {
        mvc.perform(get("/api/admin/operations/profile").with(admin())).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.fullName").value("Người kiểm thử 0")).andExpect(jsonPath("$.data.email").value("ROLE_ADMIN@test.invalid"))
            .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
        mvc.perform(get("/api/admin/operations/profile").with(user("ROLE_MANAGER@test.invalid"))).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.fullName").value("Người kiểm thử 3")).andExpect(jsonPath("$.data.roleLabel").value("Quản lý vận hành"));
        mvc.perform(get("/api/admin/operations/me").with(user("ROLE_MANAGER@test.invalid"))).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.canAdmin").value(false)).andExpect(jsonPath("$.data.canReport").value(true));
        mvc.perform(get("/api/admin/operations/profile").param("userId",ADMIN.toString()).with(user("ROLE_MANAGER@test.invalid"))).andExpect(status().isOk()).andExpect(jsonPath("$.data.fullName").value("Người kiểm thử 3"));
        mvc.perform(get("/api/admin/operations/profile").with(user("ROLE_TUTOR@test.invalid"))).andExpect(status().isForbidden());
    }
}
