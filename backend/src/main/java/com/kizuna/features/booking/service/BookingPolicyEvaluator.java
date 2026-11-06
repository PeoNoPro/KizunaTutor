package com.kizuna.features.booking.service;
import com.kizuna.core.exception.BusinessException;

import com.kizuna.features.admin.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.UUID;

/** Public integration boundary. Caller creates booking + snapshot in the SAME transaction. */
@Service @RequiredArgsConstructor
public class BookingPolicyEvaluator {
    private final BookingPolicySnapshotProvider policies;
    private final Clock operationsClock;

    @Transactional(propagation=Propagation.MANDATORY)
    public Evaluation evaluate(OffsetDateTime startAt) {
        var r=policies.lockCurrent();
        return evaluate(r,startAt,OffsetDateTime.now(operationsClock));
    }
    public static Evaluation evaluate(BookingPolicySnapshotProvider.Snapshot r,OffsetDateTime startAt,OffsetDateTime serverNow) {
        if(startAt==null || startAt.isBefore(serverNow.plusHours(r.minAdvanceHours()))) throw BusinessException.invalid();
        return new Evaluation(r.revisionId(),r.durationMinutes(),r.minAdvanceHours(),startAt.plusMinutes(r.durationMinutes()));
    }
    public record Evaluation(UUID revisionId,int durationMinutes,int minAdvanceHours,OffsetDateTime computedEndAt) { }
}
