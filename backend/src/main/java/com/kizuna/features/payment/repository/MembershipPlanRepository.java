package com.kizuna.features.payment.repository;

import com.kizuna.features.payment.entity.MembershipPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Gói hội viên (UC-99, UC-100, UC-104).
 * Cột features_json là JSONB nhưng entity MembershipPlan (không thuộc phạm vi sửa của FE-09) ánh xạ kiểu chuỗi thường,
 * nên mọi thao tác GHI đi qua câu lệnh có CAST(... AS jsonb); không cập nhật gói qua dirty-checking của entity.
 */
@Repository
public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, UUID> {

    List<MembershipPlan> findByIsActiveTrueOrderByDurationMonthsAscPriceAsc();

    List<MembershipPlan> findAllByOrderByDurationMonthsAscPriceAsc();

    boolean existsByPlanNameIgnoreCase(String planName);

    boolean existsByPlanNameIgnoreCaseAndPlanIdNot(String planName, UUID planId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "INSERT INTO membership_plans (plan_id, plan_name, duration_months, price, features_json, is_active) "
            + "VALUES (:planId, :planName, :durationMonths, :price, CAST(:featuresJson AS jsonb), TRUE)", nativeQuery = true)
    int insertPlan(@Param("planId") UUID planId,
                   @Param("planName") String planName,
                   @Param("durationMonths") int durationMonths,
                   @Param("price") BigDecimal price,
                   @Param("featuresJson") String featuresJson);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE membership_plans SET plan_name = :planName, price = :price, "
            + "features_json = CAST(:featuresJson AS jsonb) WHERE plan_id = :planId", nativeQuery = true)
    int updatePlan(@Param("planId") UUID planId,
                   @Param("planName") String planName,
                   @Param("price") BigDecimal price,
                   @Param("featuresJson") String featuresJson);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE membership_plans SET is_active = :active WHERE plan_id = :planId", nativeQuery = true)
    int updateActive(@Param("planId") UUID planId, @Param("active") boolean active);
}
