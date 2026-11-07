package com.kizuna.features.classroom.service;

import com.kizuna.features.classroom.dto.ClassScheduleRequest;
import com.kizuna.features.classroom.dto.ClassScheduleResponse;
import com.kizuna.features.classroom.util.CurrentActor;

import java.util.List;
import java.util.UUID;

/** FE-06: lịch học hằng tuần của lớp. Gia sư thêm/sửa buổi học; không có thao tác xóa (theo nghiệp vụ). */
public interface ClassScheduleService {

    /** Gia sư hoặc học viên của lớp xem lịch học. */
    List<ClassScheduleResponse> list(CurrentActor actor, UUID spaceId);

    /** Gia sư thêm một buổi học hằng tuần cho lớp. */
    ClassScheduleResponse add(CurrentActor actor, UUID spaceId, ClassScheduleRequest request);

    /** Gia sư đổi thứ/giờ của một buổi học. */
    ClassScheduleResponse update(CurrentActor actor, UUID spaceId, UUID scheduleId, ClassScheduleRequest request);
}
