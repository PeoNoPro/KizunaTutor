/**
 * KizunaTutor - Tutor Schedule & Clash Detection Helpers (tutor-schedule-helpers.js)
 * Xử lý kiểm tra trùng lịch học lớp học (classSchedules) và buổi học thử đã đặt (bookedSlots).
 */

/**
 * Kiểm tra xem một khung giờ [startMinutes, startMinutes + durationMinutes] có bị trùng
 * với lịch học lớp cố định (classSchedules) hoặc buổi học thử đã đặt (bookedSlots) hay không.
 *
 * @param {Object} tutor Đối tượng gia sư chứa classSchedules và bookedSlots
 * @param {number} dayOfWeek Thứ trong tuần (1 = Chủ nhật, 2 = Thứ hai ... 7 = Thứ bảy)
 * @param {string} dateStr Ngày cần kiểm tra dạng "dd/MM/yyyy" (ví dụ: "13/11/2026")
 * @param {number} startMinutes Số phút tính từ 00:00 (ví dụ: 18:00 = 1080)
 * @param {number} durationMinutes Thời lượng buổi học thử (ví dụ: 30)
 * @returns {boolean} true nếu trùng lịch dạy hoặc lịch học thử đã đặt
 */
export function isSlotClashing(tutor, dayOfWeek, dateStr, startMinutes, durationMinutes) {
  if (!tutor) return false;
  const endMinutes = startMinutes + durationMinutes;

  // 1. Kiểm tra trùng lịch học chính thức hàng tuần của lớp (classSchedules)
  const dayClassSchedules = (tutor.classSchedules || []).filter(cs => Number(cs.dayOfWeek) === Number(dayOfWeek));
  for (const cs of dayClassSchedules) {
    if (!cs.startTime || !cs.endTime) continue;
    const [csStartH, csStartM] = cs.startTime.split(':').map(Number);
    const [csEndH, csEndM] = cs.endTime.split(':').map(Number);
    const csStart = csStartH * 60 + csStartM;
    const csEnd = csEndH * 60 + csEndM;

    // Khoảng thời gian giao nhau: startMinutes < csEnd && endMinutes > csStart
    if (startMinutes < csEnd && endMinutes > csStart) {
      return true;
    }
  }

  // 2. Kiểm tra trùng với các buổi học thử đã được đặt trước trên ngày cụ thể (bookedSlots)
  if (dateStr) {
    const dayBookedSlots = (tutor.bookedSlots || []).filter(bs => bs.date === dateStr);
    for (const bs of dayBookedSlots) {
      if (!bs.startTime || !bs.endTime) continue;
      const [bStartH, bStartM] = bs.startTime.split(':').map(Number);
      const [bEndH, bEndM] = bs.endTime.split(':').map(Number);
      const bStart = bStartH * 60 + bStartM;
      const bEnd = bEndH * 60 + bEndM;

      if (startMinutes < bEnd && endMinutes > bStart) {
        return true;
      }
    }
  }

  return false;
}

