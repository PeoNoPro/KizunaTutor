package com.kizuna.repository;

import com.kizuna.entity.TrialBookingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface TrialBookingHistoryRepository extends JpaRepository<TrialBookingHistory, UUID> {
}
