package com.kizuna.features.classroom.service;

import com.kizuna.features.classroom.dto.ClassScheduleRequest;
import com.kizuna.features.classroom.dto.ClassScheduleResponse;
import com.kizuna.features.classroom.dto.TimetableEntryResponse;
import com.kizuna.features.classroom.util.CurrentActor;

import java.util.List;
import java.util.UUID;

/** FE-06: lịch học hằng tuần của lớp. Gia sư thêm/sửa/xóa buổi học; lớp luôn còn ít nhất 1 buổi (không xóa được buổi cuối). */
public interface ClassScheduleService {

    /** Thời khóa biểu: lịch hằng tuần của mọi lớp đang mở (ACTIVE/PAUSED) của người đang đăng nhập. */
    List<TimetableEntryResponse> timetable(CurrentActor actor);

    /** Gia sư hoặc học viên của lớp xem lịch học. */
    List<ClassScheduleResponse> list(CurrentActor actor, UUID spaceId);

    /** Gia sư thêm một buổi học hằng tuần cho lớp. */
    ClassScheduleResponse add(CurrentActor actor, UUID spaceId, ClassScheduleRequest request);

    /** Gia sư đổi thứ/giờ của một buổi học. */
    ClassScheduleResponse update(CurrentActor actor, UUID spaceId, UUID scheduleId, ClassScheduleRequest request);

    /** Gia sư xóa một buổi học khi lớp còn từ 2 buổi trở lên (lớp không được mất hết lịch học). */
    void delete(CurrentActor actor, UUID spaceId, UUID scheduleId);
}
