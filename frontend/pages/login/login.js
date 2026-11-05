/**
 * KizunaTutor - Quản lý đăng nhập & Điều hướng phiên làm việc
 * Module Feature-Based độc lập tuân thủ nghiêm ngặt FRONTEND_RULES.md
 */

import { showToast } from '/js/main.js';

// =========================================================================
// 1. CẤU HÌNH & KHÓA LƯU TRỮ PHIÊN
// =========================================================================
const STORAGE_KEYS = {
  TOKEN: 'kizuna_token',
  USER: 'kizuna_user'
};

const DASHBOARD_ROUTES = {
  ROLE_ADMIN: '/admin/dashboard',
  ROLE_MANAGER: '/manager/dashboard',
  ROLE_CS_STAFF: '/cs/dashboard',
  ROLE_ACADEMIC_OFFICER: '/academic/dashboard',
  ROLE_TUTOR: '/tutor/dashboard',
  ROLE_LEARNER: '/pages/home/',
  ROLE_STUDENT: '/pages/home/'
};

const DEMO_ACCOUNTS = {
  admin: { email: 'admin@kizuna.vn', pass: 'admin123', role: 'ROLE_ADMIN', fullName: 'Quản Trị Viên (Admin)' },
  manager: { email: 'manager@kizuna.vn', pass: '123456', role: 'ROLE_MANAGER', fullName: 'Quản Lý Vận Hành' },
  cs: { email: 'cs@kizuna.vn', pass: '123456', role: 'ROLE_CS_STAFF', fullName: 'Chăm Sóc Khách Hàng (CS)' },
  academic: { email: 'academic@kizuna.vn', pass: '123456', role: 'ROLE_ACADEMIC_OFFICER', fullName: 'Cán Bộ Học Vụ' },
  tutor: { email: 'tutor@kizuna.vn', pass: '123456', role: 'ROLE_TUTOR', fullName: 'Giảng Viên Kizuna' },
  learner: { email: 'learner@kizuna.vn', pass: '123456', role: 'ROLE_LEARNER', fullName: 'Học Viên Kizuna' }
};

let firstTimeAuthData = {
  token: null,
  user: null
};

// =========================================================================
// 2. CÁC HÀM TIỆN ÍCH QUẢN LÝ PHIÊN
// =========================================================================
export function setAuthSession(token, user) {
  if (token) localStorage.setItem(STORAGE_KEYS.TOKEN, token);
  if (user) localStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(user));
}

export function getAuthToken() {
  return localStorage.getItem(STORAGE_KEYS.TOKEN);
}

export function getAuthUser() {
  const raw = localStorage.getItem(STORAGE_KEYS.USER);
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch (e) {
    return null;
  }
}

export function clearAuthSession() {
  [STORAGE_KEYS.TOKEN, STORAGE_KEYS.USER, 'access_token', 'refresh_token'].forEach(k => {
    try { localStorage.removeItem(k); } catch (e) {}
  });
  try { sessionStorage.clear(); } catch (e) {}
}

function showLoginAlert(message, type = 'danger') {
  const alertBox = document.getElementById('loginAlertBox');
  if (!alertBox) return;

  alertBox.classList.remove('hidden', 'bg-rose-50', 'text-rose-800', 'border-rose-200', 'bg-emerald-50', 'text-emerald-800', 'border-emerald-200');

  if (type === 'success') {
    alertBox.className = 'mb-4 p-3.5 rounded-xl text-xs sm:text-sm font-medium border bg-emerald-50 text-emerald-800 border-emerald-200 flex items-center gap-2';
    alertBox.innerHTML = `<span>✓</span> <span>${message}</span>`;
  } else {
    alertBox.className = 'mb-4 p-3.5 rounded-xl text-xs sm:text-sm font-medium border bg-rose-50 text-rose-800 border-rose-200 flex items-center gap-2';
    alertBox.innerHTML = `<span>⚠️</span> <span>${message}</span>`;
  }
}

function hideLoginAlert() {
  const alertBox = document.getElementById('loginAlertBox');
  if (alertBox) {
    alertBox.classList.add('hidden');
    alertBox.innerHTML = '';
  }
}

// =========================================================================
// 3. XỬ LÝ ĐĂNG NHẬP (HANDLE LOGIN)
// =========================================================================
async function handleLoginSubmit(event) {
  if (event) event.preventDefault();

  const emailInput = document.getElementById('loginEmail');
  const passwordInput = document.getElementById('loginPassword');
  const submitBtn = document.getElementById('btnLoginSubmit');

  if (!emailInput || !passwordInput) return;

  const email = emailInput.value.trim();
  const password = passwordInput.value;

  if (!email || !password) {
    showLoginAlert('Vui lòng nhập đầy đủ địa chỉ email và mật khẩu.', 'danger');
    return;
  }

  // Cập nhật trạng thái loading
  if (submitBtn) {
    submitBtn.disabled = true;
    submitBtn.innerHTML = `
      <span class="inline-block animate-spin mr-1">⏳</span>
      <span>Đang xác thực thông tin...</span>
    `;
  }
  hideLoginAlert();

  let response = null;
  let data = {};
  let networkFailed = false;

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 1200);

  try {
    response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      body: JSON.stringify({ email, password }),
      signal: controller.signal
    });
    clearTimeout(timeoutId);
    data = await response.json().catch(() => ({}));
  } catch (err) {
    clearTimeout(timeoutId);
    networkFailed = true;
  }

  // Nếu Backend chưa khởi động (502 hoặc ngắt kết nối mạng)
  if (networkFailed || (response && response.status === 502)) {
    const matchedDemo = Object.values(DEMO_ACCOUNTS).find(
      acc => acc.email.toLowerCase() === email.toLowerCase()
    );

    if (matchedDemo) {
      if (matchedDemo.pass === password) {
        const demoUser = {
          id: 9999,
          email: matchedDemo.email,
          fullName: matchedDemo.fullName,
          role: matchedDemo.role,
          roles: [matchedDemo.role],
          isDemo: true
        };
        setAuthSession('demo-jwt-token-' + Date.now(), demoUser);
        showLoginAlert('Đăng nhập thành công (Chế độ Thử nghiệm)! Hệ thống đang chuyển hướng...', 'success');
        const targetUrl = DASHBOARD_ROUTES[matchedDemo.role] || '/learner/dashboard';
        setTimeout(() => {
          window.location.href = targetUrl;
        }, 300);
        return;
      } else {
        showLoginAlert('Mật khẩu không chính xác cho tài khoản demo: ' + matchedDemo.email, 'danger');
        if (submitBtn) {
          submitBtn.disabled = false;
          submitBtn.innerHTML = `<span>Đăng nhập</span><span class="material-symbols-outlined text-[18px]">arrow_forward</span>`;
        }
        return;
      }
    }

    showLoginAlert('Không thể kết nối Backend (cổng 8085 chưa bật). Hãy click 1 trong 6 nút "Đăng nhập nhanh 1-Click" bên dưới để vào ngay giao diện!', 'danger');
    if (submitBtn) {
      submitBtn.disabled = false;
      submitBtn.innerHTML = `<span>Đăng nhập</span><span class="material-symbols-outlined text-[18px]">arrow_forward</span>`;
    }
    return;
  }

  if (!response.ok) {
    const errorMsg = data.message || data.error || 'Đăng nhập không thành công. Vui lòng kiểm tra lại email hoặc mật khẩu.';
    showLoginAlert(errorMsg, 'danger');
    if (submitBtn) {
      submitBtn.disabled = false;
      submitBtn.innerHTML = `<span>Đăng nhập</span><span class="material-symbols-outlined text-[18px]">arrow_forward</span>`;
    }
    return;
  }

  // 1. Lưu phiên đăng nhập từ máy chủ thật
  setAuthSession(data.token, data.user);

  // 2. Kiểm tra nếu có cờ bắt buộc đổi mật khẩu lần đầu
  if (data.mustChangePassword || (data.user && data.user.mustChangePassword)) {
    showFirstTimeChangePasswordModal(data.token, data.user, password);
    if (submitBtn) {
      submitBtn.disabled = false;
      submitBtn.innerHTML = `<span>Đăng nhập</span><span class="material-symbols-outlined text-[18px]">arrow_forward</span>`;
    }
    return;
  }

  showLoginAlert('Đăng nhập thành công! Hệ thống đang chuyển hướng...', 'success');

  // 3. Điều hướng theo vai trò người dùng
  const userRole = (data.user && data.user.role) ? data.user.role : 'ROLE_LEARNER';
  const isLearner = userRole === 'ROLE_LEARNER' || userRole === 'ROLE_STUDENT';
  const targetUrl = isLearner ? '/pages/home/' : (data.redirectUrl || DASHBOARD_ROUTES[userRole] || '/pages/home/');

  setTimeout(() => {
    window.location.href = targetUrl;
  }, 400);
}

// =========================================================================
// 4. KIỂM THỬ NHANH 6 VAI TRÒ (QUICK LOGIN HELPER)
// =========================================================================
function setupQuickLoginButtons() {
  document.querySelectorAll('.btn-quick-login').forEach(btn => {
    btn.addEventListener('click', () => {
      const roleKey = btn.dataset.role;
      const account = DEMO_ACCOUNTS[roleKey];
      if (account) {
        const emailInput = document.getElementById('loginEmail');
        const passwordInput = document.getElementById('loginPassword');
        if (emailInput && passwordInput) {
          emailInput.value = account.email;
          passwordInput.value = account.pass;
          handleLoginSubmit();
        }
      }
    });
  });
}

// =========================================================================
// 5. MODAL ĐỔI MẬT KHẨU LẦN ĐẦU (FIRST-TIME PASSWORD CHANGE)
// =========================================================================
function showFirstTimeChangePasswordModal(token, user, currentPassword) {
  firstTimeAuthData.token = token;
  firstTimeAuthData.user = user;

  const modal = document.getElementById('firstTimeChangePasswordModal');
  if (!modal) return;

  modal.classList.remove('hidden');
  modal.classList.add('flex');

  const currPassInput = document.getElementById('firstTimeCurrentPassword');
  if (currPassInput && currentPassword) {
    currPassInput.value = currentPassword;
  }
}

async function handleFirstTimeChangePassword(e) {
  e.preventDefault();
  const currPass = document.getElementById('firstTimeCurrentPassword')?.value;
  const newPass = document.getElementById('firstTimeNewPassword')?.value;
  const confirmPass = document.getElementById('firstTimeConfirmPassword')?.value;
  const alertEl = document.getElementById('firstTimeModalAlert');
  const submitBtn = document.getElementById('btnSubmitFirstTimePassword');

  if (!currPass || !newPass || !confirmPass) {
    if (alertEl) {
      alertEl.className = 'mb-4 p-3 rounded-xl text-xs font-semibold bg-rose-50 text-rose-800 border border-rose-200';
      alertEl.textContent = 'Vui lòng điền đầy đủ tất cả các trường mật khẩu.';
      alertEl.classList.remove('hidden');
    }
    return;
  }

  if (newPass !== confirmPass) {
    if (alertEl) {
      alertEl.className = 'mb-4 p-3 rounded-xl text-xs font-semibold bg-rose-50 text-rose-800 border border-rose-200';
      alertEl.textContent = 'Mật khẩu mới và mật khẩu xác nhận không trùng khớp.';
      alertEl.classList.remove('hidden');
    }
    return;
  }

  if (submitBtn) {
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span>Đang cập nhật mật khẩu...</span>';
  }

  try {
    const res = await fetch('/api/auth/change-password-first-time', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${firstTimeAuthData.token || getAuthToken()}`
      },
      body: JSON.stringify({
        currentPassword: currPass,
        newPassword: newPass,
        confirmPassword: confirmPass
      })
    });

    const data = await res.json().catch(() => ({}));

    if (!res.ok) {
      if (alertEl) {
        alertEl.className = 'mb-4 p-3 rounded-xl text-xs font-semibold bg-rose-50 text-rose-800 border border-rose-200';
        alertEl.textContent = data.message || 'Cập nhật mật khẩu thất bại. Vui lòng kiểm tra lại.';
        alertEl.classList.remove('hidden');
      }
      if (submitBtn) {
        submitBtn.disabled = false;
        submitBtn.innerHTML = '<span>Cập nhật mật khẩu &amp; Vào hệ thống</span>';
      }
      return;
    }

    if (alertEl) {
      alertEl.className = 'mb-4 p-3 rounded-xl text-xs font-semibold bg-emerald-50 text-emerald-800 border border-emerald-200';
      alertEl.textContent = 'Đổi mật khẩu thành công! Đang điều hướng...';
      alertEl.classList.remove('hidden');
    }

    const userRole = firstTimeAuthData.user?.role || 'ROLE_LEARNER';
    const targetUrl = DASHBOARD_ROUTES[userRole] || '/pages/home/';
    setTimeout(() => {
      window.location.href = targetUrl;
    }, 600);

  } catch (err) {
    if (alertEl) {
      alertEl.className = 'mb-4 p-3 rounded-xl text-xs font-semibold bg-rose-50 text-rose-800 border border-rose-200';
      alertEl.textContent = 'Lỗi kết nối máy chủ. Vui lòng thử lại sau.';
      alertEl.classList.remove('hidden');
    }
    if (submitBtn) {
      submitBtn.disabled = false;
      submitBtn.innerHTML = '<span>Cập nhật mật khẩu &amp; Vào hệ thống</span>';
    }
  }
}

// =========================================================================
// 6. KHỞI TẠO DOM & LẮNG NGHE SỰ KIỆN (DOM READY)
// =========================================================================
document.addEventListener('DOMContentLoaded', () => {
  // Mobile menu toggle
  const mobileMenuBtn = document.getElementById('mobileMenuBtn');
  const mobileMenu = document.getElementById('mobileMenu');
  if (mobileMenuBtn && mobileMenu) {
    mobileMenuBtn.addEventListener('click', () => {
      mobileMenu.classList.toggle('hidden');
    });
  }

  // Form submit
  document.getElementById('loginForm')?.addEventListener('submit', handleLoginSubmit);

  // Quick login buttons
  setupQuickLoginButtons();

  // First time password form
  document.getElementById('firstTimeChangePasswordForm')?.addEventListener('submit', handleFirstTimeChangePassword);

  // Xử lý query parameters trên URL
  const params = new URLSearchParams(window.location.search);
  const error = params.get('error');
  const registered = params.get('registered');

  if (registered === 'true') {
    showLoginAlert('Tài khoản đã tạo thành công! Hãy đăng nhập để bắt đầu.', 'success');
  } else if (error) {
    let msg = 'Đăng nhập không thành công. Vui lòng kiểm tra lại.';
    if (error === 'email_not_provided') msg = 'Dịch vụ xác thực không cung cấp thông tin email của bạn.';
    if (error === 'oauth2_failure') msg = 'Đăng nhập bằng tài khoản Google thất bại hoặc bạn đã hủy ủy quyền.';
    if (error === 'oauth2_process_error') msg = 'Lỗi xử lý tài khoản Google phía máy chủ.';
    showLoginAlert(msg, 'danger');
  }
});
