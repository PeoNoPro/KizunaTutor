package com.kizuna.repository;

import com.kizuna.entity.TutorCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface TutorCredentialRepository extends JpaRepository<TutorCredential, UUID> {
}
