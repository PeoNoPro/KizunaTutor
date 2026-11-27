package com.kizuna.features.booking.repository;

import com.kizuna.features.booking.entity.TrialBookingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TrialBookingHistoryRepository extends JpaRepository<TrialBookingHistory, UUID> {

    List<TrialBookingHistory> findByTrialBookingBookingIdOrderByCreatedAtDesc(UUID bookingId);
}

