package com.kizuna.repository;

import com.kizuna.entity.SystemConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface SystemConfigurationRepository extends JpaRepository<SystemConfiguration, String> {
}
