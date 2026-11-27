package com.kizuna.features.admin.service;
import java.util.UUID;
/** Public service boundary consumed by booking; no cross-feature repository access. */
public interface BookingPolicySnapshotProvider {
    Snapshot lockCurrent();
    record Snapshot(UUID revisionId,int durationMinutes,int minAdvanceHours) { }
}
