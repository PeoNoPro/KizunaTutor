package com.kizuna.features.admin.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.*;
import java.time.OffsetDateTime;
@Entity @Table(name="technical_incidents") @Getter @Setter @NoArgsConstructor
public class TechnicalIncident extends com.kizuna.core.entity.BaseEntity {
    @Id @Column(name="incident_id") private UUID incidentId;
     @Column(name="subject") private String subject;
     @Column(name="description") private String description;
     @Column(name="severity") private String severity;
     @Column(name="status") private String status;
     @Column(name="reporter_id") private UUID reporterId;
     @Column(name="assignee_id") private UUID assigneeId;
     @Column(name="recurrence_of") private UUID recurrenceOf;
    @Version @Column(name="version") private Long version;
    @org.hibernate.annotations.BatchSize(size=50)
    @ElementCollection(fetch=FetchType.LAZY)
    @CollectionTable(name="incident_components",joinColumns=@JoinColumn(name="incident_id"))
    @Column(name="component_code") private Set<String> components = new LinkedHashSet<>();
}
