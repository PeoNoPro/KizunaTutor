/*
 * Nạp sẵn giao diện khung theo vai trò đã biết trong phiên (FE-10, FE-12).
 * Dùng bằng <script src="/js/frame-preload.js"></script> đặt trong <head> (script thường, chạy đồng bộ),
 * để CSS của khung tải cùng lúc với trang thay vì chờ JS chạy xong mới tải → không còn cảnh trang hiện trơn rồi khung "nhảy" vào.
 * Trong lúc chờ dựng khung, nội dung được ẩn (tối đa 1,5 giây) để không bị giật bố cục.
 * Vai trò lưu ở sessionStorage 'kizuna-frame-role' (learner, tutor, academic, admin, manager, cs).
 */
(function () {
  var role = null;
  try {
    role = sessionStorage.getItem('kizuna-frame-role');
    if (!role) {
      var cached = JSON.parse(sessionStorage.getItem('kizuna-notification-account') || sessionStorage.getItem('kizuna-support-account') || 'null');
      role = cached && cached.viewRole;
    }
  } catch (error) { role = null; }
  var write = function (html) { document.write(html); };
  var css = function (href) { if (!document.querySelector('link[href="' + href + '"]')) write('<link rel="stylesheet" href="' + href + '">'); };
  // Biểu tượng tải chặn (không dùng media=print) để không hiện chữ "notifications", "support_agent"... trước khi có font.
  if (!document.querySelector('link[href*="Material+Symbols+Outlined"][rel=stylesheet]:not([media=print])')) {
    write('<link id="framePreloadIcons" rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200">');
  }
  if (!role) return;
  if (role === 'learner') { css('/styles/learner-frame.css'); css('/styles/learner-frame-utilities.css'); }
  else if (role === 'tutor') css('/styles/tutor-theme.css');
  else if (role === 'academic') {
    write('<script src="https://cdn.tailwindcss.com"><\/script><script src="/components/tutor-layout/tailwind-theme.js"><\/script>');
    css('/styles/tutor-theme.css');
  } else {
    ['/styles/admin.css', '/styles/operations.css', '/styles/admin-portal.css'].forEach(css);
    if (role === 'cs') css('/styles/cs-workspace.css');
    css('/styles/staff-unified.css');
  }
  document.documentElement.classList.add('frame-pending');
  write('<style id="framePendingStyle">html.frame-pending main{visibility:hidden;animation:frame-reveal 0s 1.5s forwards}@keyframes frame-reveal{to{visibility:visible}}</style>');
})();

/** Gọi sau khi đã dựng xong khung (hoặc khi lỗi) để hiện nội dung. */
window.kizunaFrameReady = function (role) {
  document.documentElement.classList.remove('frame-pending');
  if (role) { try { sessionStorage.setItem('kizuna-frame-role', role); } catch (error) { /* Không lưu được thì lần sau tải như cũ. */ } }
};
