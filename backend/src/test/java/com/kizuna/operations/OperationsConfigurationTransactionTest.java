package com.kizuna.operations;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.*;
class OperationsConfigurationTransactionTest extends OperationsIntegrationBase {
    String body(long version,String reason) {return "{\"expectedVersion\":"+version+",\"durationMinutes\":60,\"minAdvanceHours\":24,\"reason\":\""+reason+"\"}";}
    @Test void auditFailureRollsBackRevisionStateAndLegacyValues() throws Exception {
        var before=data(mvc.perform(get("/api/admin/operations/booking-policy").with(admin())).andReturn());
        int revisions=jdbc.queryForObject("SELECT count(*) FROM booking_policy_revisions",Integer.class);
        jdbc.execute("ALTER TABLE configuration_audit_events ADD CONSTRAINT fe11_test_config_failure CHECK(reason <> 'fail-test')");
        try {
            mvc.perform(put("/api/admin/operations/booking-policy").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(body(before.get("version").asLong(),"fail-test"))).andExpect(status().isServiceUnavailable());
            var after=data(mvc.perform(get("/api/admin/operations/booking-policy").with(admin())).andReturn());
            assertEquals(before,after); assertEquals(revisions,jdbc.queryForObject("SELECT count(*) FROM booking_policy_revisions",Integer.class));
        } finally {jdbc.execute("ALTER TABLE configuration_audit_events DROP CONSTRAINT fe11_test_config_failure");}
    }
    @Test void competingConfigurationSavesHaveOneWinner() throws Exception {
        long version=data(mvc.perform(get("/api/admin/operations/booking-policy").with(admin())).andReturn()).get("version").asLong();
        var executor=Executors.newFixedThreadPool(2); var gate=new CountDownLatch(1);
        Callable<Integer> save=()->{gate.await();return mvc.perform(put("/api/admin/operations/booking-policy").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(body(version,"Cạnh tranh cấu hình"))).andReturn().getResponse().getStatus();};
        try {
            var a=executor.submit(save);var b=executor.submit(save);gate.countDown();
            var codes=new ArrayList<>(List.of(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS)));Collections.sort(codes);assertEquals(List.of(200,409),codes);
        } finally {executor.shutdownNow();}
    }
    @Test void missingOrUnversionedLegacyConfigDisablesEditing() throws Exception {
        String original=jdbc.queryForObject("SELECT config_value FROM system_configurations WHERE config_key='TRIAL_DEFAULT_DURATION_MINUTES'",String.class);
        try {
            jdbc.update("UPDATE system_configurations SET config_value='30' WHERE config_key='TRIAL_DEFAULT_DURATION_MINUTES'");
            if("30".equals(original)) jdbc.update("UPDATE system_configurations SET config_value='31' WHERE config_key='TRIAL_DEFAULT_DURATION_MINUTES'");
            mvc.perform(get("/api/admin/operations/booking-policy").with(admin())).andExpect(status().isOk()).andExpect(jsonPath("$.data.editable").value(false));
        } finally {jdbc.update("UPDATE system_configurations SET config_value=? WHERE config_key='TRIAL_DEFAULT_DURATION_MINUTES'",original);}
    }
}
