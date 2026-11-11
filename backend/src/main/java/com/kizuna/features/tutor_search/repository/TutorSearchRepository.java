package com.kizuna.features.tutor_search.repository;

import com.kizuna.core.entity.Tutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository truy vấn dữ liệu gia sư phục vụ tìm kiếm và khám phá.
 * Các truy vấn nạp kèm tài khoản (user) trong cùng query để không phải query riêng cho từng gia sư.
 */
@Repository
public interface TutorSearchRepository extends JpaRepository<Tutor, UUID>, JpaSpecificationExecutor<Tutor> {

    @Override
    @EntityGraph(attributePaths = {"user"})
    Page<Tutor> findAll(Specification<Tutor> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"user"})
    Optional<Tutor> findById(UUID id);

    /**
     * Lấy danh sách các gia sư tiêu biểu (Featured Tutors) có điểm đánh giá cao nhất.
     */
    @EntityGraph(attributePaths = {"user"})
    List<Tutor> findTop6ByIsFeaturedTrueOrderByAvgRatingDesc();

    /**
     * Tra cứu hồ sơ gia sư theo mã định danh tự tăng (tutor_code).
     */
    @EntityGraph(attributePaths = {"user"})
    Optional<Tutor> findByTutorCode(Long tutorCode);
}
