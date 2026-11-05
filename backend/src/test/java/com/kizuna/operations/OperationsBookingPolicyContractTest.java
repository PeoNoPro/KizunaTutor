package com.kizuna.operations;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
class OperationsBookingPolicyContractTest extends OperationsIntegrationBase {
    @Test void boundariesVersionAndHistory() throws Exception {
        var current=data(mvc.perform(get("/api/admin/operations/booking-policy").with(admin())).andExpect(status().isOk()).andReturn());
        long version=current.get("version").asLong();
        String body="{\"expectedVersion\":"+version+",\"durationMinutes\":120,\"minAdvanceHours\":168,\"reason\":\"Kiểm biên hợp lệ\"}";
        mvc.perform(put("/api/admin/operations/booking-policy").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.data.holdMinutes").value(15));
        mvc.perform(put("/api/admin/operations/booking-policy").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
        mvc.perform(get("/api/admin/operations/booking-policy/history").with(admin())).andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").isNumber());
    }
    @Test void rejectsDecimalsStringsAndUnapprovedKeys() throws Exception {
        long version=data(mvc.perform(get("/api/admin/operations/booking-policy").with(admin())).andReturn()).get("version").asLong();
        for(String bad:new String[]{"14","121","15.5","\"45\"","null"}) {
            String body="{\"expectedVersion\":"+version+",\"durationMinutes\":"+bad+",\"minAdvanceHours\":12,\"reason\":\"Lý do\"}";
            mvc.perform(put("/api/admin/operations/booking-policy").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        String body="{\"expectedVersion\":"+version+",\"durationMinutes\":45,\"minAdvanceHours\":12,\"reason\":\"Lý do\",\"holdMinutes\":99}";
        mvc.perform(put("/api/admin/operations/booking-policy").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
    }
}
