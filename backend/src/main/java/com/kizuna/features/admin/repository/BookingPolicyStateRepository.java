package com.kizuna.features.admin.repository;
import com.kizuna.features.admin.entity.BookingPolicyState;
import org.springframework.data.jpa.repository.*;
import java.util.UUID;
public interface BookingPolicyStateRepository extends JpaRepository<BookingPolicyState,UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from BookingPolicyState s where s.stateId=:id")
    java.util.Optional<BookingPolicyState> lock(@org.springframework.data.repository.query.Param("id") java.util.UUID id);
}
