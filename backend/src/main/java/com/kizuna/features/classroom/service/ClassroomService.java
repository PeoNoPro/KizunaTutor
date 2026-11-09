package com.kizuna.features.classroom.service;

import com.kizuna.features.classroom.dto.ClassroomCreateRequest;
import com.kizuna.features.classroom.dto.ClassroomSummaryResponse;
import com.kizuna.features.classroom.dto.EligibleLearnerResponse;
import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.util.CurrentActor;

import java.util.List;
import java.util.UUID;

/** FE-06: quản lý lớp học (UC-93 Access Classroom, UC-58 View Course). */
public interface ClassroomService {

    List<ClassroomSummaryResponse> listMine(CurrentActor actor);

    List<EligibleLearnerResponse> listEligibleLearners(CurrentActor actor);

    ClassroomSummaryResponse create(CurrentActor actor, ClassroomCreateRequest request);

    ClassroomSummaryResponse getDetail(CurrentActor actor, UUID spaceId);

    /** Gia sư của lớp đổi tên lớp (lớp đang hoạt động, gói hội viên còn hiệu lực). */
    ClassroomSummaryResponse rename(CurrentActor actor, UUID spaceId, String title);

    /** Gia sư sửa khóa học: tổng số buổi (không nhỏ hơn số buổi đã học), ngày bắt đầu (khi chưa có buổi nào diễn ra). */
    ClassroomSummaryResponse updateCourse(CurrentActor actor, UUID spaceId, com.kizuna.features.classroom.dto.ClassroomCourseRequest request);

    /** Lớp mà người dùng là gia sư hoặc học viên; 404 nếu không tồn tại, 403 nếu không thuộc lớp. */
    LearningSpace loadAccessibleSpace(CurrentActor actor, UUID spaceId);

    /** Lớp mà người dùng là gia sư và lớp không ở chế độ chỉ xem; dùng cho tải lên/cập nhật tài liệu. */
    LearningSpace loadManageableSpace(CurrentActor actor, UUID spaceId);

    boolean isReadOnly(LearningSpace space);
}
