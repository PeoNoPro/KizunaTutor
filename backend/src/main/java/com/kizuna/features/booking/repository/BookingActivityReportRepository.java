package com.kizuna.features.booking.repository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.List;
@Repository @RequiredArgsConstructor
public class BookingActivityReportRepository {
    private final EntityManager em;
    public List<Object[]> counts(OffsetDateTime start,OffsetDateTime end) {
        return em.createNativeQuery("SELECT booking_status::text,count(*) FROM trial_bookings WHERE created_at>=:start AND created_at<:end GROUP BY booking_status ORDER BY booking_status")
            .setParameter("start",start).setParameter("end",end).getResultList();
    }
}
