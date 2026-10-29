package com.kizuna.features.booking.repository;


import com.kizuna.core.entity.*;
import com.kizuna.features.booking.entity.TrialBookingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface TrialBookingHistoryRepository extends JpaRepository<TrialBookingHistory, UUID> {
}
