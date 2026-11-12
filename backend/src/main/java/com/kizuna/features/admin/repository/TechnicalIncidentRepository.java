package com.kizuna.features.admin.repository;
import com.kizuna.features.admin.entity.TechnicalIncident;
import org.springframework.data.jpa.repository.*;
import java.util.UUID;
public interface TechnicalIncidentRepository extends JpaRepository<TechnicalIncident,UUID>, JpaSpecificationExecutor<TechnicalIncident> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select i from TechnicalIncident i where i.incidentId=:id")
    java.util.Optional<TechnicalIncident> lock(@org.springframework.data.repository.query.Param("id") java.util.UUID id);
    /** Yêu cầu hỗ trợ này có đang/đã được chuyển lên cho Quản lý này không. */
    @org.springframework.data.jpa.repository.Query(value="SELECT count(*) FROM support_tickets WHERE ticket_id=:ticket AND escalated_to=:manager",nativeQuery=true)
    long countEscalatedTo(@org.springframework.data.repository.query.Param("ticket") java.util.UUID ticket,@org.springframework.data.repository.query.Param("manager") java.util.UUID manager);
}
