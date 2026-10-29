/**
 * Từ điển ngôn ngữ giao diện Gia Sư (Tutor UI Microcopy)
 * Phong cách: Dí dỏm, đồng hành, thân thiện nhưng tôn trọng Sensei.
 */
window.TutorCopy = {
  greetings: {
    welcome: (name) => `Chào Sensei${name ? ' ' + name : ''}! Hôm nay lớp học của mình có gì vui không nào? 🌸`,
    subtitle: "Kho chữ Kanji đã sẵn sàng, nhặt vài chữ \"khoai khoai\" thử tài học viên và cùng chiêm ngưỡng nét bút Shodo nhé! 🖌️"
  },
  status: {
    pending: "Hồ sơ xịn sò của bạn đang được team Học vụ \"soi\" kỹ. Đợi tụi mình một chút nhé! 🍵",
    rejected: "Team Học vụ có chút nhắn nhủ nho nhỏ, Sensei liếc qua rồi bổ sung giúp tụi mình nha! 📑",
    approvedUnpaid: "Chỉ còn một bước kích hoạt gói là Sensei có thể mở lớp đón học viên về đội rồi! 🚀",
    activePaid: "Tài khoản Sensei đã sẵn sàng mở lớp, cùng học viên rèn nét chữ tài hoa! 🌟"
  },
  submissions: {
    empty: "Bàn chấm bài hôm nay đang vắng vẻ, học viên vẫn đang miệt mài múa bút! 🍃",
    hasNew: "Ting ting! Học viên vừa nộp bài, Sensei vào chấm xem nét chữ tiến bộ thế nào nhé! 📝"
  },
  kanjiLibrary: {
    subtitle: "Nhặt vài chữ \"khoai khoai\" để thử tài học viên tuần này nào Sensei! 💡",
    basketEmpty: "Chưa có chữ nào trong giỏ. Hãy bấm nút [+ Gom Chữ] ở bên dưới để thêm vào giỏ giao bài nhé! 🧺",
    ctaAssign: "Giao bài cho các trò thôi 🚀",
    assignSuccess: "Giao bài tập thành công! Chuẩn bị đón nhận bài nộp xịn sò từ học trò nhé! 🎉"
  },
  students: {
    empty: "Bàn học đang vắng bóng học viên. Khi học viên gia nhập hoặc được phân công, danh sách sẽ hiện ngay tại đây! 🌿"
  }
};
