package com.kizuna.core.repository;

import com.kizuna.core.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Short> {
    Optional<Role> findByRoleName(String roleName);
}
