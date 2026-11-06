package com.kizuna.operations;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.*;
import java.util.*;
class OperationsIncidentConcurrencyTest extends OperationsIntegrationBase {
    @Test void competingClaimsHaveOneWinnerAndNoDuplicateSequence() throws Exception {
        var i=data(mvc.perform(post("/api/admin/operations/incidents").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"subject\":\"Cạnh tranh\",\"description\":\"Kiểm thử\",\"severity\":\"NORMAL\",\"components\":[\"DATABASE\"]}")).andReturn());
        var executor=Executors.newFixedThreadPool(2); var gate=new CountDownLatch(1);
        Callable<Integer> claim=()->{gate.await();return mvc.perform(post("/api/admin/operations/incidents/"+i.get("incidentId").asText()+"/claim").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":0}")).andReturn().getResponse().getStatus();};
        try {
            var a=executor.submit(claim); var b=executor.submit(claim); gate.countDown();
            var codes=new ArrayList<>(List.of(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS))); Collections.sort(codes);
            assertEquals(List.of(200,409),codes);
            assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM technical_incident_events",Integer.class));
        } finally {executor.shutdownNow();}
    }
    @Test void eventFailureRollsBackMutation() throws Exception {
        String constraint="fe11_test_event_failure";
        jdbc.execute("ALTER TABLE technical_incident_events ADD CONSTRAINT "+constraint+" CHECK(action <> 'CREATE')");
        try {
            mvc.perform(post("/api/admin/operations/incidents").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"subject\":\"Rollback\",\"description\":\"Kiểm thử\",\"severity\":\"NORMAL\",\"components\":[\"DATABASE\"]}")).andExpect(status().isServiceUnavailable());
            assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM technical_incidents",Integer.class));
        } finally { jdbc.execute("ALTER TABLE technical_incident_events DROP CONSTRAINT "+constraint); }
    }
    @Test void sameTimestampEventsRemainOrderedBySequence() throws Exception {
        var i=data(mvc.perform(post("/api/admin/operations/incidents").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"subject\":\"Thứ tự\",\"description\":\"Kiểm thử\",\"severity\":\"NORMAL\",\"components\":[\"DATABASE\"]}")).andReturn());
        String path="/api/admin/operations/incidents/"+i.get("incidentId").asText();
        mvc.perform(post(path+"/claim").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":0}")).andExpect(status().isOk());
        jdbc.update("UPDATE technical_incident_events SET occurred_at='2026-01-01T00:00:00Z'");
        var history=data(mvc.perform(get(path+"/events").with(admin())).andReturn());
        assertEquals(1,history.get("items").get(0).get("sequenceNo").asInt());assertEquals(2,history.get("items").get(1).get("sequenceNo").asInt());
    }
}
