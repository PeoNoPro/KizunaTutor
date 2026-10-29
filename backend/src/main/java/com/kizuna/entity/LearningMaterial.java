package com.kizuna.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "learning_materials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningMaterial extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "materialId")
    private UUID materialId;
}
