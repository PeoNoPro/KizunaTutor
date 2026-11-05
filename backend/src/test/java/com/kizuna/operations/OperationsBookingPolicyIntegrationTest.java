package com.kizuna.operations;
import com.kizuna.features.booking.service.BookingPolicyEvaluator;
import com.kizuna.features.admin.service.BookingPolicySnapshotProvider.Snapshot;
import com.kizuna.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.UUID;
/** Evaluator subset only. These tests do not claim a real booking workflow exists. */
class OperationsBookingPolicyIntegrationTest {
    @Test void exactAdvanceBoundaryAndDurationSnapshot() {
        var r=new Snapshot(UUID.randomUUID(),45,12);
        var now=OffsetDateTime.parse("2026-10-31T01:00:00Z"); var start=now.plusHours(12);
        var result=BookingPolicyEvaluator.evaluate(r,start,now);
        assertEquals(start.plusMinutes(45),result.computedEndAt()); assertEquals(r.revisionId(),result.revisionId());
        assertEquals(45,result.durationMinutes());
        assertThrows(BusinessException.class,()->BookingPolicyEvaluator.evaluate(r,start.minusSeconds(1),now));
    }
}
