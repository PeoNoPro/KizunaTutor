/**
 * Từ điển ngôn ngữ giao diện Học Viên (Learner UI Microcopy)
 * Phong cách: Trẻ trung, hóm hỉnh, động viên, mang năng lượng Gen Z học tiếng Nhật.
 */
window.LearnerCopy = {
  greetings: {
    welcome: (name) => `Hôm nay lại thêm một ngày chăm chỉ cùng tiếng Nhật nhé! ☕`,
    streak: (streak) => streak && streak > 0 
      ? `Giữ lửa ${streak} ngày liên tục rồi đấy, Kizuna Sensei tự hào về bạn! 🔥`
      : `Khởi đầu ngày học đầu tiên cùng tiếng Nhật, Kizuna Sensei tự hào về bạn! 🌸`
  },
  homework: {
    empty: "Gia sư chưa giao bài mới, tranh thủ làm cốc trà sữa hoặc cày vài chữ Kanji đi nào! 🧋",
    deadlineUrgent: "Deadline đang thở sau gáy rồi nè, vào luyện vài nét nộp bài cho Sensei thôi! ⏰",
    noClassLocked: "Khu vực này dành riêng cho học viên có Sensei kèm cặp. Tìm một người thầy hợp gu ngay thôi! 🤝"
  },
  canvas: {
    ctaPractice: "Cầm bút luyện nét ngay ✍️",
    ctaMinigame: "Bắn chữ giải ngố nào 🎮",
    ctaGrade: "Chấm Điểm Nét AI 🎯",
    highScore: "Nét vẽ mượt như Shodo thực thụ, Sensei chấm điểm 10 không có nhưng! ✨",
    wrongOrder: "Ối, nét này đi hơi ngược đường ray rồi, thử lại đường quyền khác nhé! 🎯",
    guidance: "Hãy cầm bút múa vài đường quyền theo thứ tự nét chuẩn. Sau khi viết xong, bấm [Chấm Điểm Nét AI] để xem Sensei AI chấm điểm nhé! 🖌️"
  },
  dictionary: {
    searchPlaceholder: "🔍 Nhập chữ Hán, âm Hán Việt (Ái, Hòa, Tâm) hoặc nghĩa tiếng Việt...",
    emptyResult: "Không tìm thấy chữ Kanji nào phù hợp, bạn thử tìm từ khóa khác xem sao nha! 🧐",
    saveSuccess: "Đã cất chữ vào bộ thẻ thành công, tha hồ lôi ra cày cuốc! 🗂️"
  }
};
