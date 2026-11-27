package com.kizuna.features.admin.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.*;

/** Chỉ đọc giao dịch gói hội viên và hồ sơ gia sư; không lấy mã/payload cổng thanh toán. */
@Repository @RequiredArgsConstructor
public class BusinessReportRepository {
    private static final String PERIOD=" WHERE p.booking_id IS NULL AND p.transaction_time>=:start AND p.transaction_time<:end";
    // FE-11 không có nghiệp vụ hoàn tiền nên không đưa giao dịch đã hoàn vào danh sách.
    private static final String LISTED=PERIOD+" AND p.status::text<>'REFUNDED'";
    private final EntityManager em;

    @SuppressWarnings("unchecked")
    public List<Object[]> revenueByStatus(OffsetDateTime start,OffsetDateTime end) {
        return em.createNativeQuery("SELECT p.status::text,count(*),coalesce(sum(p.amount),0) FROM payment_transactions p"+PERIOD+" GROUP BY 1").setParameter("start",start).setParameter("end",end).getResultList();
    }
    @SuppressWarnings("unchecked")
    public List<Object[]> revenueByPlan(OffsetDateTime start,OffsetDateTime end) {
        return em.createNativeQuery("SELECT m.plan_name,count(*),sum(p.amount) FROM payment_transactions p LEFT JOIN membership_plans m ON m.plan_id=p.plan_id"+PERIOD+" AND p.status::text='SUCCESS' GROUP BY 1 ORDER BY 3 DESC,1").setParameter("start",start).setParameter("end",end).getResultList();
    }
    @SuppressWarnings("unchecked")
    public List<Object[]> revenueByDay(OffsetDateTime start,OffsetDateTime end) {
        return em.createNativeQuery("SELECT to_char(p.transaction_time AT TIME ZONE 'Asia/Ho_Chi_Minh','YYYY-MM-DD'),sum(p.amount) FROM payment_transactions p"+PERIOD+" AND p.status::text='SUCCESS' GROUP BY 1").setParameter("start",start).setParameter("end",end).getResultList();
    }
    public long transactionCount(OffsetDateTime start,OffsetDateTime end) {
        return ((Number)em.createNativeQuery("SELECT count(*) FROM payment_transactions p"+LISTED).setParameter("start",start).setParameter("end",end).getSingleResult()).longValue();
    }
    @SuppressWarnings("unchecked")
    public List<Object[]> transactions(OffsetDateTime start,OffsetDateTime end) {
        return em.createNativeQuery("SELECT p.transaction_id,u.full_name,u.email,m.plan_name,p.amount,p.payment_gateway,p.status::text,p.transaction_time FROM payment_transactions p JOIN users u ON u.user_id=p.user_id LEFT JOIN membership_plans m ON m.plan_id=p.plan_id"+LISTED+" ORDER BY p.transaction_time DESC,p.transaction_id").setParameter("start",start).setParameter("end",end).getResultList();
    }
    public long tutorCount() {
        return ((Number)em.createNativeQuery("SELECT count(*) FROM tutors t JOIN users u ON u.user_id=t.user_id WHERE u.deleted_at IS NULL").getSingleResult()).longValue();
    }
    @SuppressWarnings("unchecked")
    public List<Object[]> tutors() {
        return em.createNativeQuery("""
            SELECT u.full_name,u.email,t.highest_jlpt_level,t.verification_status::text,
                (SELECT count(*) FROM tutor_credentials c WHERE c.tutor_id=t.tutor_id AND c.verified_status::text='VERIFIED'),
                (SELECT count(*) FROM tutor_credentials c WHERE c.tutor_id=t.tutor_id AND c.verified_status::text='PENDING'),
                t.submitted_at,t.verified_at
            FROM tutors t JOIN users u ON u.user_id=t.user_id WHERE u.deleted_at IS NULL
            ORDER BY t.submitted_at DESC NULLS LAST,u.full_name,t.tutor_id
            """).getResultList();
    }
}
