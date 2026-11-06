package com.kizuna.operations;
import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.admin.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessResourceFailureException;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class OperationsHealthTest {
    @Test void probesRealSourceAndDoesNotRefreshTimestampOnRead() {
        var jdbc=mock(JdbcTemplate.class); var actors=mock(OperationsActorResolver.class);
        var clock=mock(Clock.class); when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        Instant now=Instant.parse("2026-10-31T00:00:00Z"); when(clock.instant()).thenReturn(now);
        when(jdbc.queryForObject("SELECT 1",Integer.class)).thenReturn(1);
        try(var collector=new CollectorCloseable(new OperationsHealthCollector(jdbc,actors,clock))) {
            collector.value.collect(); var first=collector.value.currentSnapshot();
            when(clock.instant()).thenReturn(now.plusSeconds(61));
            var later=collector.value.currentSnapshot();
            var components=(List<Map<String,Object>>)later.get("components");
            assertEquals(now,components.get(0).get("observedAt")); assertEquals(true,components.get(0).get("stale"));
            assertEquals("Chưa xác định",components.get(2).get("statusLabel")); assertNull(components.get(2).get("observedAt"));
            when(jdbc.queryForObject("SELECT 1",Integer.class)).thenThrow(new DataAccessResourceFailureException("test only"));
            collector.value.collect();
            var failed=(List<Map<String,Object>>)collector.value.currentSnapshot().get("components"); assertEquals("Gián đoạn",failed.get(1).get("statusLabel"));
        }
    }
    @Test void timeoutIsBoundedAndAuthorizationFailureIsNotBypassed() {
        var jdbc=mock(JdbcTemplate.class); var actors=mock(OperationsActorResolver.class);
        when(jdbc.queryForObject("SELECT 1",Integer.class)).thenAnswer(invocation->{Thread.sleep(3000);return 1;});
        var collector=new OperationsHealthCollector(jdbc,actors,Clock.systemUTC());
        try {
            assertTimeout(Duration.ofMillis(2800),collector::collect);
            when(actors.requireAdmin()).thenThrow(new BusinessException(503,"Chưa xác minh được quyền."));
            assertThrows(BusinessException.class,collector::read);
        } finally {collector.close();}
    }
    record CollectorCloseable(OperationsHealthCollector value) implements AutoCloseable { public void close(){value.close();} }
    @Test void exhaustedIdentityPoolFailsClosedWithinBudget() throws Exception {
        var properties=new org.springframework.boot.autoconfigure.jdbc.DataSourceProperties();
        // Enforce the same explicit test-DB guard before constructing the production pool.
        OperationsTestEnvironment.dataSource();
        properties.setUrl(System.getenv("OPERATIONS_TEST_DB_URL")); properties.setUsername(System.getenv("OPERATIONS_TEST_DB_USERNAME")); properties.setPassword(System.getenv("OPERATIONS_TEST_DB_PASSWORD"));
        var config=new com.kizuna.features.admin.config.OperationsConfig();
        try(var pool=config.operationsProbePool(properties)) {
            pool.dataSource().setMaximumPoolSize(1);
            try(var occupied=pool.dataSource().getConnection()) {
                var repository=new com.kizuna.features.admin.repository.OperationsIdentityRepository(config.operationsIdentityJdbc(pool));
                var actors=new OperationsActorResolver(repository);
                org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("ROLE_ADMIN@test.invalid",null,java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))));
                assertTimeout(Duration.ofSeconds(3),()->{
                    var failure=assertThrows(BusinessException.class,actors::requireAdmin); assertEquals(503,failure.status());
                });
            } finally {org.springframework.security.core.context.SecurityContextHolder.clearContext();}
        }
    }
}
