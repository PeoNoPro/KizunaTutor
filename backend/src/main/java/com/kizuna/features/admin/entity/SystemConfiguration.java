package com.kizuna.features.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "system_configurations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemConfiguration {

    @Id
    @Column(name = "config_key", length = 100)
    private String configKey;
    @Column(name = "config_value", nullable = false) private String configValue;
    @Column(name = "data_type", nullable = false) private String dataType;
    @Column(name = "description") private String description;
    @Column(name = "updated_at", nullable = false) private java.time.OffsetDateTime updatedAt;
}
