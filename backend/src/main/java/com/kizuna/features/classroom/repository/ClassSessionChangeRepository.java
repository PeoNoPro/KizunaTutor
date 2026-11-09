package com.kizuna.features.classroom.repository;

import com.kizuna.features.classroom.entity.ClassSessionChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassSessionChangeRepository extends JpaRepository<ClassSessionChange, UUID> {

    Optional<ClassSessionChange> findBySchedule_ScheduleIdAndOriginalDate(UUID scheduleId, LocalDate originalDate);

    /** Mọi thay đổi (dời / hủy) của các lớp — dùng để sinh lịch buổi học của khóa. */
    List<ClassSessionChange> findBySchedule_Space_SpaceIdIn(Collection<UUID> spaceIds);

    /** Bỏ các thay đổi chưa tới (theo ngày gốc) của một buổi hằng tuần — dùng khi gia sư đổi giờ hoặc xóa buổi đó. */
    long deleteBySchedule_ScheduleIdAndOriginalDateGreaterThanEqual(UUID scheduleId, LocalDate from);
}
