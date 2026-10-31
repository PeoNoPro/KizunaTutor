package com.kizuna.features.tutor_search.repository;

import com.kizuna.core.entity.Tutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository truy vấn dữ liệu gia sư phục vụ tìm kiếm và khám phá.
 */
@Repository
public interface TutorSearchRepository extends JpaRepository<Tutor, UUID>, JpaSpecificationExecutor<Tutor> {
    
    /**
     * Lấy danh sách các gia sư tiêu biểu (Featured Tutors) có điểm đánh giá cao nhất.
     */
    List<Tutor> findTop6ByIsFeaturedTrueOrderByAvgRatingDesc();
}

