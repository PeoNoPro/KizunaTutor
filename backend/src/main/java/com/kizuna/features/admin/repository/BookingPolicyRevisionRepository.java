package com.kizuna.features.admin.repository;
import com.kizuna.features.admin.entity.BookingPolicyRevision;
import org.springframework.data.jpa.repository.*;
import java.util.UUID;
public interface BookingPolicyRevisionRepository extends JpaRepository<BookingPolicyRevision,UUID> {
    java.util.Optional<BookingPolicyRevision> findByRevisionNumber(Long number);
}
