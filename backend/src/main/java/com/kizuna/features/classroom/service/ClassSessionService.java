package com.kizuna.features.classroom.service;

import com.kizuna.features.classroom.dto.ClassSessionMoveRequest;
import com.kizuna.features.classroom.dto.ClassSessionResponse;
import com.kizuna.features.classroom.util.CurrentActor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Buổi học cụ thể của khóa (FE-06): xem theo khoảng ngày; gia sư dời / hủy / thêm riêng buổi trong MỘT tuần
 * (các tuần khác giữ nguyên, lịch tự sinh cho đủ tổng số buổi của khóa).
 */
public interface ClassSessionService {

    /** Các buổi trong khoảng [from, to] (tối đa 62 ngày) của mọi lớp đang mở mà người dùng tham gia. */
    List<ClassSessionResponse> sessions(CurrentActor actor, LocalDate from, LocalDate to);

    /** Dời buổi của lịch hằng tuần {@code scheduleId} rơi vào ngày {@code originalDate} sang ngày/giờ khác trong cùng tuần. */
    void move(CurrentActor actor, UUID spaceId, UUID scheduleId, LocalDate originalDate, ClassSessionMoveRequest request);

    /** Hủy riêng buổi đó (không tính vào khóa, khóa tự bù thêm buổi ở cuối). */
    void cancel(CurrentActor actor, UUID spaceId, UUID scheduleId, LocalDate originalDate);

    /** Đưa buổi đã dời về lại lịch gốc. */
    void restore(CurrentActor actor, UUID spaceId, UUID scheduleId, LocalDate originalDate);

    /** Thêm một buổi riêng (request.newDate + giờ). */
    void addExtra(CurrentActor actor, UUID spaceId, ClassSessionMoveRequest request);

    /** Dời buổi thêm riêng sang ngày/giờ khác trong cùng tuần. */
    void moveExtra(CurrentActor actor, UUID spaceId, UUID extraId, ClassSessionMoveRequest request);

    /** Hủy buổi thêm riêng. */
    void cancelExtra(CurrentActor actor, UUID spaceId, UUID extraId);
}
