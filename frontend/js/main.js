/**
 * KizunaTutor - Main Application Bootstrap & Utilities
 * Dùng chung cho toàn bộ các trang trong hệ thống.
 * Cung cấp cơ chế nạp Header / Footer từ components/ để đồng bộ toàn dự án.
 */
import {fetchAPI} from './api.js';
import { initLearningToolsGuard } from './learning-tools-access.js';

// 1. Định dạng tiền tệ VND
export function formatVND(amount) {
  if (!amount) return '0 đ';
  return new Intl.NumberFormat('vi-VN').format(amount) + ' đ';
}

// 2. Debounce hỗ trợ ô nhập tìm kiếm
export function debounce(func, wait = 300) {
  let timeout;
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout);
      func(...args);
    };
    clearTimeout(timeout);
    timeout = setTimeout(later, wait);
  };
}

// 3. Hiển thị thông báo Toast lịch sự (Sentence case theo AGENTS.md)
export function showToast(message, type = 'info') {
  let toastContainer = document.getElementById('toast-container');
  if (!toastContainer) {
    toastContainer = document.createElement('div');
    toastContainer.id = 'toast-container';
    toastContainer.className = 'fixed bottom-5 right-5 z-[150] flex flex-col gap-2 pointer-events-none';
    document.body.appendChild(toastContainer);
  }

  const toast = document.createElement('div');
  const bgClass = type === 'success' ? 'bg-emerald-700 text-white' : (type === 'error' ? 'bg-rose-700 text-white' : 'bg-[#0F2545] text-white');
  
  toast.className = `${bgClass} px-4 py-3 rounded-xl shadow-lg text-xs md:text-sm font-medium flex items-center gap-2 transition-all duration-300 transform translate-y-2 opacity-0 pointer-events-auto max-w-md`;
  toast.innerHTML = `
    <span class="material-symbols-outlined text-[18px]">${type === 'success' ? 'check_circle' : (type === 'error' ? 'error' : 'info')}</span>
    <span data-toast-message></span>
  `;
  toast.querySelector('[data-toast-message]').textContent = message;

  toastContainer.appendChild(toast);

  // Animation In
  setTimeout(() => {
    toast.classList.remove('translate-y-2', 'opacity-0');
  }, 10);

  // Auto dismiss sau 3.5s
  setTimeout(() => {
    toast.classList.add('translate-y-2', 'opacity-0');
    setTimeout(() => toast.remove(), 300);
  }, 3500);
}

async function initSupportNavigation() {
  // Đã bỏ các mục "Yêu cầu hỗ trợ" và "Chính sách" khỏi thanh điều hướng header theo yêu cầu
}

// 4. Khởi tạo trạng thái thanh điều hướng (Active Navigation)
export function initNavigation() {
  const currentPath = window.location.pathname;
  const navLinks = document.querySelectorAll('header nav a');

  navLinks.forEach(link => {
    const href = link.getAttribute('href');
    if (!href) return;

    // Kiểm tra active cho trang Tìm gia sư (hỗ trợ cả /tutors, /pages/tutor-discovery/)
    const isTutorDiscovery = (currentPath.startsWith('/tutors') || currentPath.includes('tutor-discovery')) &&
                             (href.includes('tutor-discovery') || href === '/tutors');

    // Kiểm tra active cho trang Yêu cầu đã gửi
    const isRequests = currentPath.includes('connection-requests') && href.includes('connection-requests');

    // Kiểm tra active cho Trang chủ
    const isHome = (currentPath === '/' || currentPath === '/home' || currentPath.includes('/pages/home/')) &&
                   (href === '/pages/home/' || href === '/' || href === '/home');

    if (isTutorDiscovery || isRequests || (isHome && !href.includes('#'))) {
      link.className = 'inline-flex items-center gap-2 px-3.5 py-2 rounded-xl text-sm font-semibold transition-colors bg-[#E0EFF7] text-[#006194]';
      link.setAttribute('aria-current', 'page');
    } else {
      link.className = 'inline-flex items-center gap-2 px-3.5 py-2 rounded-xl text-sm font-medium text-[#3F4850] hover:text-[#006194] hover:bg-[#FAF8F2] transition-colors';
      link.removeAttribute('aria-current');
    }
  });

  // Toggle Mobile Menu
  const mobileMenuBtn = document.getElementById('mobileMenuBtn');
  const mobileMenu = document.getElementById('mobileMenu');
  if (mobileMenuBtn && mobileMenu && !mobileMenuBtn.hasAttribute('data-has-listener')) {
    mobileMenuBtn.setAttribute('data-has-listener', 'true');
    mobileMenuBtn.addEventListener('click', () => {
      const isHidden = mobileMenu.classList.toggle('hidden');
      const icon = mobileMenuBtn.querySelector('.material-symbols-outlined');
      if (icon) {
        icon.textContent = isHidden ? 'menu' : 'close';
      }
    });
  }
}

// Trang hồ sơ cá nhân của từng vai trò (FE-02 và các cổng nhân sự).
const PROFILE_URL_BY_ROLE = {
  ROLE_LEARNER: '/pages/learner-profile/',
  ROLE_TUTOR: '/tutor/profile',
  ROLE_ACADEMIC_OFFICER: '/academic/profile',
  ROLE_ADMIN: '/pages/admin-profile/',
  ROLE_MANAGER: '/pages/admin-profile/',
  ROLE_CS_STAFF: '/pages/admin-profile/'
};

// 5. Cập nhật giao diện người dùng đã đăng nhập (Đồng bộ tức thì 0ms từ đối tượng User)
export function applyUserUI(user) {
  if (!user) return;
  const guestActions = document.getElementById('headerGuestActions');
  const userActions = document.getElementById('headerUserActions');
  const mobileGuestActions = document.getElementById('mobileGuestActions');
  const mobileUserActions = document.getElementById('mobileUserActions');

  if (guestActions) guestActions.classList.add('hidden');
  if (userActions) userActions.classList.remove('hidden');
  if (mobileGuestActions) mobileGuestActions.classList.add('hidden');
  if (mobileUserActions) mobileUserActions.classList.remove('hidden');

  const isTutor = user.role === 'ROLE_TUTOR';
  const dashboardUrl = isTutor ? '/tutor/dashboard' : '/learner/dashboard';

  const headerDashboardBtn = document.getElementById('headerDashboardBtn');
  if (headerDashboardBtn) headerDashboardBtn.href = dashboardUrl;

  const mobileDashboardBtn = document.getElementById('mobileDashboardBtn');
  if (mobileDashboardBtn) mobileDashboardBtn.href = dashboardUrl;

  // Gia sư vào không gian giảng dạy; học viên (và vai trò khác) giữ nhãn "Không gian học tập".
  if (isTutor) {
    document.querySelectorAll('[data-workspace-label]').forEach((el) => { el.textContent = 'Không gian giảng dạy'; });
  }

  // FE-02: "Hồ sơ cá nhân" trong menu tài khoản mở đúng trang hồ sơ theo vai trò.
  const profileUrl = PROFILE_URL_BY_ROLE[user.role] || '/pages/learner-profile/';
  ['dropdownProfileLink', 'mobileProfileBtn'].forEach((id) => {
    const link = document.getElementById(id);
    if (link) link.href = profileUrl;
  });

  const dropdownSettingsLink = document.getElementById('dropdownSettingsLink');
  if (dropdownSettingsLink) dropdownSettingsLink.href = dashboardUrl;

  const mobileSettingsBtn = document.getElementById('mobileSettingsBtn');
  if (mobileSettingsBtn) mobileSettingsBtn.href = dashboardUrl;

  // Xử lý nút Chuông thông báo
  const headerNotificationBtn = document.getElementById('headerNotificationBtn');
  const notificationBadge = document.getElementById('notificationBadge');
  if (headerNotificationBtn && !headerNotificationBtn.hasAttribute('data-has-click')) {
    headerNotificationBtn.setAttribute('data-has-click', 'true');
    // FE-12: chuông mở khung xem nhanh thông báo (notification-badge.js); chấm đỏ chỉ hiện khi có thông báo chưa đọc.
  }
  if (notificationBadge) notificationBadge.classList.add('hidden');
  if (headerNotificationBtn) import('/js/pages/notification-badge.js').then(module => module.attachNotificationBadges()).catch(() => {});

  // Hiển thị Avatar & Tên & Phụ đề
  let displayName = user.fullName || user.email || 'Người dùng';
  if (isTutor && !displayName.toLowerCase().startsWith('sensei')) {
    displayName = 'Sensei ' + displayName;
  }
  let subtitle = user.subtitle || (isTutor ? 'Gia sư • JLPT N1' : 'Học viên');
  if (subtitle) {
    subtitle = subtitle.replace(/\s*•\s*Mục tiêu.*$/i, '').trim();
  }
  const initials = user.initialLetters || (displayName ? displayName.slice(0, 2).toUpperCase() : 'KZ');

  // Desktop Profile Pill
  const headerUserName = document.getElementById('headerUserName');
  const headerUserSubtitle = document.getElementById('headerUserSubtitle');
  const headerUserAvatar = document.getElementById('headerUserAvatar');
  const headerUserInitials = document.getElementById('headerUserInitials');

  if (headerUserName) headerUserName.textContent = displayName;
  if (headerUserSubtitle) headerUserSubtitle.textContent = subtitle;

  if (user.avatarUrl && headerUserAvatar) {
    headerUserAvatar.src = user.avatarUrl;
    headerUserAvatar.classList.remove('hidden');
    if (headerUserInitials) headerUserInitials.classList.add('hidden');
  } else {
    if (headerUserAvatar) headerUserAvatar.classList.add('hidden');
    if (headerUserInitials) {
      headerUserInitials.classList.remove('hidden');
      headerUserInitials.textContent = initials;
    }
  }

  // Dropdown info
  const dropdownFullName = document.getElementById('dropdownFullName');
  const dropdownEmail = document.getElementById('dropdownEmail');
  if (dropdownFullName) dropdownFullName.textContent = displayName;
  if (dropdownEmail) dropdownEmail.textContent = user.email || '';

  // Mobile Profile Info
  const mobileUserName = document.getElementById('mobileUserName');
  const mobileUserSubtitle = document.getElementById('mobileUserSubtitle');
  const mobileUserAvatar = document.getElementById('mobileUserAvatar');
  const mobileUserInitials = document.getElementById('mobileUserInitials');

  if (mobileUserName) mobileUserName.textContent = displayName;
  if (mobileUserSubtitle) mobileUserSubtitle.textContent = subtitle;

  if (user.avatarUrl && mobileUserAvatar) {
    mobileUserAvatar.src = user.avatarUrl;
    mobileUserAvatar.classList.remove('hidden');
    if (mobileUserInitials) mobileUserInitials.classList.add('hidden');
  } else {
    if (mobileUserAvatar) mobileUserAvatar.classList.add('hidden');
    if (mobileUserInitials) {
      mobileUserInitials.classList.remove('hidden');
      mobileUserInitials.textContent = initials;
    }
  }

  // Xử lý đóng/mở Dropdown menu khi nhấn vào Profile Pill
  const userProfilePill = document.getElementById('userProfilePill');
  const userDropdownMenu = document.getElementById('userDropdownMenu');

  if (userProfilePill && userDropdownMenu && !userProfilePill.hasAttribute('data-has-dropdown')) {
    userProfilePill.setAttribute('data-has-dropdown', 'true');
    userProfilePill.addEventListener('click', (e) => {
      e.stopPropagation();
      userDropdownMenu.classList.toggle('hidden');
    });

    document.addEventListener('click', (e) => {
      if (!userDropdownMenu.contains(e.target) && !userProfilePill.contains(e.target)) {
        userDropdownMenu.classList.add('hidden');
      }
    });
  }

  // Gắn sự kiện đăng xuất
  const handleLogout = async () => {
    try {
      await fetch('/api/auth/logout', { method: 'POST' });
    } catch (e) {
      // Bỏ qua lỗi mạng khi logout
    }
    ['kizuna_token', 'kizuna_user', 'access_token', 'refresh_token'].forEach(k => {
      try { localStorage.removeItem(k); } catch (e) {}
    });
    try { sessionStorage.clear(); } catch (e) {}
    window.location.href = '/pages/home/';
  };

  const headerLogoutBtn = document.getElementById('headerLogoutBtn');
  if (headerLogoutBtn && !headerLogoutBtn.hasAttribute('data-has-logout')) {
    headerLogoutBtn.setAttribute('data-has-logout', 'true');
    headerLogoutBtn.addEventListener('click', handleLogout);
  }

  const mobileLogoutBtn = document.getElementById('mobileLogoutBtn');
  if (mobileLogoutBtn && !mobileLogoutBtn.hasAttribute('data-has-logout')) {
    mobileLogoutBtn.setAttribute('data-has-logout', 'true');
    mobileLogoutBtn.addEventListener('click', handleLogout);
  }
}

// 6. Đồng bộ trạng thái đăng nhập (Optimistic Render 0ms + Xác thực ngầm với Server)
export async function syncAuthState() {
  const token = localStorage.getItem('kizuna_token');

  const guestActions = document.getElementById('headerGuestActions');
  const userActions = document.getElementById('headerUserActions');
  const mobileGuestActions = document.getElementById('mobileGuestActions');
  const mobileUserActions = document.getElementById('mobileUserActions');

  const renderGuest = () => {
    if (guestActions) guestActions.classList.remove('hidden');
    if (userActions) userActions.classList.add('hidden');
    if (mobileGuestActions) mobileGuestActions.classList.remove('hidden');
    if (mobileUserActions) mobileUserActions.classList.add('hidden');
  };

  if (!token) {
    renderGuest();
    return;
  }

  // Bước 1: Render ngay lập tức 0ms từ cache localStorage (loại bỏ hoàn toàn độ trễ 1s)
  const cachedUserStr = localStorage.getItem('kizuna_user');
  let cachedUser = null;
  if (cachedUserStr) {
    try { cachedUser = JSON.parse(cachedUserStr); } catch (e) { cachedUser = null; }
  }

  if (cachedUser) {
    applyUserUI(cachedUser);
  } else {
    // Có token nhưng chưa có dữ liệu user -> tạm ẩn nút khách để tránh chớp
    if (guestActions) guestActions.classList.add('hidden');
    if (mobileGuestActions) mobileGuestActions.classList.add('hidden');
  }

  // Bước 2: Xác thực ngầm dưới nền với Backend thật qua API GET /api/auth/me
  try {
    const res = await fetch('/api/auth/me', {
      headers: {
        'Authorization': 'Bearer ' + token
      }
    });

    if (res.ok) {
      const user = await res.json();
      localStorage.setItem('kizuna_user', JSON.stringify(user));
      applyUserUI(user);
    } else {
      // Token hết hạn hoặc không hợp lệ -> Xóa phiên và về Guest
      console.warn('Phiên đăng nhập không hợp lệ hoặc đã hết hạn.');
      localStorage.removeItem('kizuna_token');
      localStorage.removeItem('kizuna_user');
      renderGuest();
    }
  } catch (err) {
    console.error('Không thể kết nối API xác thực người dùng:', err);
    if (!cachedUser) {
      renderGuest();
    }
  }
}

// 6. Quản lý Modal Đăng nhập nhanh (Popup nền mờ khi người dùng chưa có tài khoản)
let authModalCallback = null;

export function openLoginModal(options = {}) {
  const modal = document.getElementById('authModal');
  if (!modal) {
    const redirectUrl = options.redirectUrl || window.location.pathname + window.location.search;
    window.location.href = `/login?redirect=${encodeURIComponent(redirectUrl)}`;
    return;
  }

  authModalCallback = typeof options.onSuccess === 'function' ? options.onSuccess : null;

  const tutorNameSpan = document.getElementById('authModalTutorName');
  if (tutorNameSpan) {
    tutorNameSpan.textContent = options.tutorName || 'gia sư';
  }

  const alertBox = document.getElementById('authModalAlert');
  if (alertBox) {
    alertBox.classList.add('hidden');
    alertBox.textContent = '';
  }

  modal.classList.remove('hidden');
  document.body.classList.add('overflow-hidden');

  const emailInput = document.getElementById('authModalEmail');
  if (emailInput) {
    setTimeout(() => emailInput.focus(), 100);
  }
}

export function closeLoginModal() {
  const modal = document.getElementById('authModal');
  if (!modal) return;
  modal.classList.add('hidden');
  document.body.classList.remove('overflow-hidden');
  authModalCallback = null;
}

export function initAuthModalEvents() {
  const modal = document.getElementById('authModal');
  const closeBtn = document.getElementById('closeAuthModalBtn');
  const form = document.getElementById('authModalForm');
  const togglePassBtn = document.getElementById('toggleAuthModalPass');
  const passInput = document.getElementById('authModalPassword');
  const alertBox = document.getElementById('authModalAlert');
  const submitBtn = document.getElementById('authModalSubmitBtn');

  if (closeBtn && !closeBtn.hasAttribute('data-has-event')) {
    closeBtn.setAttribute('data-has-event', 'true');
    closeBtn.addEventListener('click', closeLoginModal);
  }

  // Click ra ngoài vùng mờ để đóng modal
  if (modal && !modal.hasAttribute('data-has-event')) {
    modal.setAttribute('data-has-event', 'true');
    modal.addEventListener('click', (e) => {
      if (e.target === modal) {
        closeLoginModal();
      }
    });
  }

  // Toggle ẩn/hiện mật khẩu
  if (togglePassBtn && passInput && !togglePassBtn.hasAttribute('data-has-event')) {
    togglePassBtn.setAttribute('data-has-event', 'true');
    togglePassBtn.addEventListener('click', () => {
      const isPass = passInput.type === 'password';
      passInput.type = isPass ? 'text' : 'password';
      togglePassBtn.innerHTML = `<span class="material-symbols-outlined text-[16px]">${isPass ? 'visibility_off' : 'visibility'}</span>`;
    });
  }

  // Xử lý gửi form đăng nhập nhanh trong modal
  if (form && !form.hasAttribute('data-has-event')) {
    form.setAttribute('data-has-event', 'true');
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      const email = document.getElementById('authModalEmail')?.value.trim();
      const password = passInput?.value;

      if (!email || !password) {
        if (alertBox) {
          alertBox.textContent = 'Vui lòng điền đầy đủ email và mật khẩu.';
          alertBox.classList.remove('hidden');
        }
        return;
      }

      if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.innerHTML = `<span>Đang đăng nhập...</span>`;
      }

      try {
        const res = await fetch('/api/auth/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email, password })
        });

        const data = await res.json();

        if (!res.ok) {
          if (alertBox) {
            alertBox.textContent = data.message || 'Tài khoản hoặc mật khẩu không chính xác.';
            alertBox.classList.remove('hidden');
          }
          if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.innerHTML = `<span>Đăng nhập</span><span class="material-symbols-outlined text-[16px]">arrow_forward</span>`;
          }
          return;
        }

        // Lưu phiên đăng nhập
        localStorage.setItem('kizuna_token', data.token);
        if (data.user) {
          localStorage.setItem('kizuna_user', JSON.stringify(data.user));
        }

        // Cập nhật ngay trạng thái Header
        await syncAuthState();

        // Đóng modal đăng nhập
        const cb = authModalCallback;
        closeLoginModal();
        showToast('Đăng nhập thành công!', 'success');

        // Tự động mở tiếp hộp thoại kết nối gia sư nếu có
        if (typeof cb === 'function') {
          setTimeout(() => {
            cb();
          }, 300);
        }
      } catch (err) {
        console.error('Lỗi khi đăng nhập modal:', err);
        if (alertBox) {
          alertBox.textContent = 'Không thể kết nối đến máy chủ. Vui lòng thử lại sau.';
          alertBox.classList.remove('hidden');
        }
      } finally {
        if (submitBtn) {
          submitBtn.disabled = false;
          submitBtn.innerHTML = `<span>Đăng nhập</span><span class="material-symbols-outlined text-[16px]">arrow_forward</span>`;
        }
      }
    });
  }
}

// 7. Nạp Header / Footer dùng chung từ thư mục components/ (Component Loader)
export async function loadSharedComponents() {
  // Nạp Header từ components/header/header.html
  const headerPlaceholder = document.getElementById('header-placeholder') || 
                            document.getElementById('header-container') || 
                            document.querySelector('[data-component="header"]');
  if (headerPlaceholder) {
    try {
      const res = await fetch('/components/header/header.html');
      if (res.ok) {
        let html = await res.text();
        const token = localStorage.getItem('kizuna_token');
        if (token) {
          html = html.replace('id="headerGuestActions" class="', 'id="headerGuestActions" class="hidden ')
                     .replace('id="headerUserActions" class="hidden ', 'id="headerUserActions" class="')
                     .replace('id="mobileGuestActions" class="', 'id="mobileGuestActions" class="hidden ')
                     .replace('id="mobileUserActions" class="hidden ', 'id="mobileUserActions" class="');
        }
        headerPlaceholder.outerHTML = html;
        initNavigation();
        initAuthModalEvents();
        syncAuthState();
      } else {
        console.error('Không tìm thấy file header.html:', res.status);
      }
    } catch (err) {
      console.error('Lỗi khi nạp Header component:', err);
    }
  } else {
    initNavigation();
    initAuthModalEvents();
    syncAuthState();
  }

  // Nạp Footer từ components/footer/footer.html
  const footerPlaceholder = document.getElementById('footer-placeholder') || 
                            document.getElementById('footer-container') || 
                            document.querySelector('[data-component="footer"]');
  if (footerPlaceholder) {
    try {
      const res = await fetch('/components/footer/footer.html');
      if (res.ok) {
        const html = await res.text();
        footerPlaceholder.outerHTML = html;
      } else {
        console.error('Không tìm thấy file footer.html:', res.status);
      }
    } catch (err) {
      console.error('Lỗi khi nạp Footer component:', err);
    }
  }
}

// Khởi chạy tự động khi DOM sẵn sàng
document.addEventListener('DOMContentLoaded', () => {
  loadSharedComponents();
  // Trang công khai (trang chủ, tìm gia sư, chính sách…): khách hoặc học viên chưa vào lớp
  // bấm vào công cụ học bị khóa thì hiện hộp thoại hướng dẫn thay vì vào trang.
  initLearningToolsGuard();
});

// Gắn toàn cục
if (typeof window !== 'undefined') {
  window.formatVND = formatVND;
  window.debounce = debounce;
  window.showToast = showToast;
  window.loadSharedComponents = loadSharedComponents;
  window.initNavigation = initNavigation;
  window.syncAuthState = syncAuthState;
  window.applyUserUI = applyUserUI;
  window.openLoginModal = openLoginModal;
  window.closeLoginModal = closeLoginModal;
}
