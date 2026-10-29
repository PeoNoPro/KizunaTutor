package com.kizuna.core.repository;

import com.kizuna.core.entity.User;
import com.kizuna.core.entity.UserAccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByDeletedAtIsNull();

    @Query("SELECT COUNT(u) FROM User u WHERE u.role.roleName IN :roleNames AND u.deletedAt IS NULL")
    long countByRoleNames(@Param("roleNames") Collection<String> roleNames);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role.roleName = :roleName AND u.accountStatus = :status AND u.deletedAt IS NULL")
    long countByRoleNameAndStatus(@Param("roleName") String roleName, @Param("status") UserAccountStatus status);

    @Query("SELECT COUNT(u) FROM User u WHERE u.accountStatus IN :statuses AND u.deletedAt IS NULL")
    long countByAccountStatusIn(@Param("statuses") Collection<UserAccountStatus> statuses);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = "DELETE FROM learning_spaces WHERE learner_id IN (SELECT learner_id FROM learners WHERE user_id NOT IN (SELECT user_id FROM users WHERE email IN ('admin@kizuna.vn', 'manager@kizuna.vn', 'cs@kizuna.vn', 'academic@kizuna.vn', 'tutor@kizuna.vn', 'learner@kizuna.vn'))); " +
                   "DELETE FROM users WHERE email NOT IN ('admin@kizuna.vn', 'manager@kizuna.vn', 'cs@kizuna.vn', 'academic@kizuna.vn', 'tutor@kizuna.vn', 'learner@kizuna.vn')", nativeQuery = true)
    void deleteNonCoreUsers();
}
