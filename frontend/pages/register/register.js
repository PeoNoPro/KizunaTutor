/**
 * KizunaTutor - Quản lý đăng ký tài khoản & Xác thực mã OTP 6 số
 * Module Feature-Based độc lập tuân thủ nghiêm ngặt FRONTEND_RULES.md
 */

import { showToast } from '/js/main.js';
import { setAuthSession } from '/pages/login/login.js';
import { initPasswordVisibilityToggles } from '/js/auth-features.js';

// =========================================================================
// 1. TRẠNG THÁI QUẢN LÝ LUỒNG OTP
// =========================================================================
const otpState = {
  email: '',
  fullName: '',
  role: 'ROLE_LEARNER',
  targetJlpt: 'N3',
  password: '',
  countdownInterval: null,
  totalSecondsLeft: 300, // 5 phút (300 giây)
  resendInterval: null,
  resendSecondsLeft: 60  // 60 giây Cooldown
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

// =========================================================================
// 2. KHỞI TẠO FORM ĐĂNG KÝ (BƯỚC 1: GỬI OTP)
// =========================================================================
function initRegistrationForm() {
  const regForm = document.getElementById('registrationForm');
  if (!regForm) return;

  regForm.addEventListener('submit', async function (e) {
    e.preventDefault();

    const fullNameInput = document.getElementById('regFullName');
    const emailInput = document.getElementById('regEmail');
    const passwordInput = document.getElementById('regPassword');
    const roleInput = document.querySelector('input[name="userRole"]:checked');
    const submitBtn = document.getElementById('btnSubmitRegistration');

    const fullName = fullNameInput ? fullNameInput.value.trim() : '';
    const email = emailInput ? emailInput.value.trim().toLowerCase() : '';
    const password = passwordInput ? passwordInput.value : '';
    const role = roleInput ? roleInput.value : 'ROLE_LEARNER';

    if (!fullName || !email || !password) {
      showRegisterAlert('Vui lòng điền đầy đủ tất cả các trường thông tin.', 'danger');
      return;
    }

    if (password.length < 6) {
      showRegisterAlert('Mật khẩu phải có tối thiểu 6 ký tự bảo mật.', 'danger');
      return;
    }

    // Lưu state tạm
    otpState.fullName = fullName;
    otpState.email = email;
    otpState.password = password;
    otpState.role = role;

    // Hiển thị trạng thái đang xử lý
    if (submitBtn) {
      submitBtn.disabled = true;
      submitBtn.innerHTML = `
        <span class="inline-block animate-spin mr-1">⏳</span>
        <span>Đang gửi mã xác thực...</span>
      `;
    }
    hideRegisterAlert();

    try {
      const response = await fetch('/api/auth/register/send-otp', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        },
        body: JSON.stringify({
          fullName: fullName,
          email: email,
          password: password,
          role: role,
          targetJlpt: otpState.targetJlpt
        })
      });

      const data = await response.json().catch(() => ({}));

      if (!response.ok) {
        const errorMsg = data.message || data.error || 'Không thể gửi mã xác thực. Vui lòng kiểm tra lại thông tin.';
        showRegisterAlert(errorMsg, 'danger');
        return;
      }

      showToast(data.message || 'Mã xác thực OTP đã được gửi đến Gmail của bạn!', 'success');
      openOtpModal(email);

    } catch (err) {
      console.debug('Lỗi kết nối khi gửi OTP:', err);
      showRegisterAlert('Không thể kết nối đến máy chủ. Vui lòng kiểm tra lại kết nối mạng.', 'danger');
    } finally {
      if (submitBtn) {
        submitBtn.disabled = false;
        submitBtn.innerHTML = `
          <span>Nhận mã xác thực OTP</span>
          <span class="material-symbols-outlined text-[18px]">send</span>
        `;
      }
    }
  });
}

function showRegisterAlert(message, type = 'danger') {
  const alertBox = document.getElementById('registerAlertBox');
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

function hideRegisterAlert() {
  const alertBox = document.getElementById('registerAlertBox');
  if (alertBox) {
    alertBox.classList.add('hidden');
    alertBox.innerHTML = '';
  }
}

// =========================================================================
// 3. KHỞI TẠO 6 Ô NHẬP MÃ OTP ĐỘC LẬP
// =========================================================================
function initOtpInputs() {
  const inputs = document.querySelectorAll('.otp-digit-input');
  if (!inputs || inputs.length === 0) return;

  inputs.forEach((input, index) => {
    // 1. Nhập số -> tự động nhảy sang ô tiếp theo
    input.addEventListener('input', function (e) {
      const val = this.value.replace(/[^0-9]/g, '');
      this.value = val ? val[0] : '';

      if (this.value && index < inputs.length - 1) {
        inputs[index + 1].focus();
      }
      checkAutoSubmitOtp();
    });

    // 2. Phím Backspace -> lùi về ô trước
    input.addEventListener('keydown', function (e) {
      if (e.key === 'Backspace' && !this.value && index > 0) {
        inputs[index - 1].focus();
      }
    });

    // 3. Dán chuỗi 6 số từ Clipboard
    input.addEventListener('paste', function (e) {
      e.preventDefault();
      const pasteData = (e.clipboardData || window.clipboardData).getData('text');
      const numbers = pasteData.replace(/[^0-9]/g, '').slice(0, inputs.length);

      for (let i = 0; i < numbers.length; i++) {
        inputs[i].value = numbers[i];
      }

      if (numbers.length < inputs.length) {
        inputs[numbers.length].focus();
      } else {
        inputs[inputs.length - 1].focus();
        checkAutoSubmitOtp();
      }
    });
  });
}

function getEnteredOtp() {
  const inputs = document.querySelectorAll('.otp-digit-input');
  let otp = '';
  inputs.forEach(input => {
    otp += (input.value || '').trim();
  });
  return otp;
}

function clearOtpInputs() {
  const inputs = document.querySelectorAll('.otp-digit-input');
  inputs.forEach(input => {
    input.value = '';
  });
  if (inputs.length > 0) inputs[0].focus();
}

function checkAutoSubmitOtp() {
  const otp = getEnteredOtp();
  if (otp.length === 6) {
    handleVerifyOtp();
  }
}

// =========================================================================
// 4. QUẢN LÝ MODAL OTP & ĐỒNG HỒ ĐẾM NGƯỢC
// =========================================================================
function openOtpModal(email) {
  const modal = document.getElementById('otpModal');
  const targetEmailEl = document.getElementById('otpTargetEmailDisplay');

  if (!modal) return;

  if (targetEmailEl) {
    targetEmailEl.textContent = email;
  }

  hideOtpAlert();
  clearOtpInputs();

  modal.classList.remove('hidden');
  modal.classList.add('flex');

  startCountdownTimer();
  startResendCooldownTimer();

  const firstInput = document.querySelector('.otp-digit-input');
  if (firstInput) {
    setTimeout(() => firstInput.focus(), 150);
  }
}

function closeOtpModal() {
  const modal = document.getElementById('otpModal');
  if (!modal) return;

  modal.classList.add('hidden');
  modal.classList.remove('flex');

  if (otpState.countdownInterval) {
    clearInterval(otpState.countdownInterval);
    otpState.countdownInterval = null;
  }
  if (otpState.resendInterval) {
    clearInterval(otpState.resendInterval);
    otpState.resendInterval = null;
  }
}

function startCountdownTimer() {
  if (otpState.countdownInterval) clearInterval(otpState.countdownInterval);

  otpState.totalSecondsLeft = 300; // 5 phút
  updateCountdownDisplay(otpState.totalSecondsLeft);

  otpState.countdownInterval = setInterval(() => {
    otpState.totalSecondsLeft--;

    if (otpState.totalSecondsLeft <= 0) {
      clearInterval(otpState.countdownInterval);
      otpState.countdownInterval = null;
      updateCountdownDisplay(0);
      showOtpAlert('Mã xác thực đã hết hạn. Vui lòng nhấn nút "Gửi lại mã OTP".', 'danger');
      disableOtpInputs(true);
      return;
    }

    updateCountdownDisplay(otpState.totalSecondsLeft);
  }, 1000);
}

function updateCountdownDisplay(seconds) {
  const timerEl = document.getElementById('otpCountdownTimer');
  if (!timerEl) return;

  const minutes = Math.floor(seconds / 60);
  const secs = seconds % 60;
  timerEl.textContent = `${String(minutes).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;

  if (seconds <= 60) {
    timerEl.className = 'font-mono font-bold text-sm text-rose-600 animate-pulse';
  } else {
    timerEl.className = 'font-mono font-bold text-sm text-[#0F2545]';
  }
}

function startResendCooldownTimer() {
  const resendBtn = document.getElementById('btnResendOtp');
  if (!resendBtn) return;

  if (otpState.resendInterval) clearInterval(otpState.resendInterval);

  otpState.resendSecondsLeft = 60;
  resendBtn.disabled = true;
  resendBtn.innerHTML = `<span>Gửi lại mã (${otpState.resendSecondsLeft}s)</span>`;

  otpState.resendInterval = setInterval(() => {
    otpState.resendSecondsLeft--;

    if (otpState.resendSecondsLeft <= 0) {
      clearInterval(otpState.resendInterval);
      otpState.resendInterval = null;
      resendBtn.disabled = false;
      resendBtn.innerHTML = `
        <span class="material-symbols-outlined text-[15px]">refresh</span>
        <span>Gửi lại mã OTP</span>
      `;
      return;
    }

    resendBtn.innerHTML = `<span>Gửi lại mã (${otpState.resendSecondsLeft}s)</span>`;
  }, 1000);
}

function disableOtpInputs(disabled) {
  document.querySelectorAll('.otp-digit-input').forEach(input => {
    input.disabled = disabled;
  });
  const btn = document.getElementById('btnConfirmOtp');
  if (btn) btn.disabled = disabled;
}

function showOtpAlert(message, type = 'danger') {
  const alertBox = document.getElementById('otpModalAlertBox');
  if (!alertBox) return;

  alertBox.classList.remove('hidden', 'bg-rose-50', 'text-rose-800', 'border-rose-200', 'bg-emerald-50', 'text-emerald-800', 'border-emerald-200');

  if (type === 'success') {
    alertBox.className = 'mb-4 p-3 rounded-xl text-xs font-semibold bg-emerald-50 text-emerald-800 border border-emerald-200 flex items-center gap-2';
    alertBox.innerHTML = `<span>✓</span> <span>${message}</span>`;
  } else {
    alertBox.className = 'mb-4 p-3 rounded-xl text-xs font-semibold bg-rose-50 text-rose-800 border border-rose-200 flex items-center gap-2';
    alertBox.innerHTML = `<span>⚠️</span> <span>${message}</span>`;
  }
}

function hideOtpAlert() {
  const alertBox = document.getElementById('otpModalAlertBox');
  if (alertBox) {
    alertBox.classList.add('hidden');
    alertBox.innerHTML = '';
  }
}

// =========================================================================
// 5. XÁC THỰC MÃ OTP (BƯỚC 2: VERIFY OTP)
// =========================================================================
async function handleVerifyOtp() {
  const otpCode = getEnteredOtp();
  const confirmBtn = document.getElementById('btnConfirmOtp');

  if (otpCode.length < 6) {
    showOtpAlert('Vui lòng nhập đủ 6 chữ số mã xác thực.', 'danger');
    return;
  }

  if (confirmBtn) {
    confirmBtn.disabled = true;
    confirmBtn.innerHTML = `
      <span class="inline-block animate-spin mr-1">⏳</span>
      <span>Đang xác thực bảo mật...</span>
    `;
  }
  hideOtpAlert();

  try {
    const response = await fetch('/api/auth/register/verify-otp', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      body: JSON.stringify({
        email: otpState.email,
        otp: otpCode
      })
    });

    const data = await response.json().catch(() => ({}));

    if (!response.ok) {
      const errorMsg = data.message || data.error || 'Mã xác thực không chính xác hoặc đã hết hiệu lực.';
      showOtpAlert(errorMsg, 'danger');
      clearOtpInputs();
      return;
    }

    showOtpAlert('Xác thực tài khoản thành công! Đang đăng nhập...', 'success');

    // Lưu phiên đăng nhập
    setAuthSession(data.token, data.user);

    // Chuyển hướng
    const userRole = (data.user && data.user.role) ? data.user.role : otpState.role;
    const targetUrl = data.redirectUrl || DASHBOARD_ROUTES[userRole] || '/pages/home/';

    setTimeout(() => {
      window.location.href = targetUrl;
    }, 500);

  } catch (err) {
    console.debug('Lỗi kết nối khi xác thực OTP:', err);
    showOtpAlert('Không thể kết nối đến máy chủ. Vui lòng kiểm tra lại kết nối mạng.', 'danger');
  } finally {
    if (confirmBtn) {
      confirmBtn.disabled = false;
      confirmBtn.innerHTML = `
        <span>Xác nhận &amp; Đăng nhập</span>
        <span class="material-symbols-outlined text-[18px]">arrow_forward</span>
      `;
    }
  }
}

// =========================================================================
// 6. GỬI LẠI MÃ OTP (RESEND OTP)
// =========================================================================
async function handleResendOtp() {
  const resendBtn = document.getElementById('btnResendOtp');
  if (!resendBtn || resendBtn.disabled) return;

  hideOtpAlert();
  clearOtpInputs();
  disableOtpInputs(false);

  try {
    const response = await fetch('/api/auth/register/resend-otp', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      body: JSON.stringify({ email: otpState.email })
    });

    const data = await response.json().catch(() => ({}));

    if (!response.ok) {
      const errorMsg = data.message || data.error || 'Không thể gửi lại mã xác thực. Vui lòng thử lại sau.';
      showOtpAlert(errorMsg, 'danger');
      return;
    }

    showToast('Mã xác thực mới đã được gửi vào Gmail của bạn.', 'success');
    showOtpAlert('Mã xác thực mới đã được gửi lại vào Gmail của bạn.', 'success');

    startCountdownTimer();
    startResendCooldownTimer();

  } catch (err) {
    console.debug('Lỗi khi gửi lại OTP:', err);
    showOtpAlert('Lỗi kết nối đến máy chủ. Vui lòng thử lại sau.', 'danger');
  }
}

// =========================================================================
// 7. KHỞI TẠO DOM & LẮNG NGHE SỰ KIỆN (DOM READY)
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

  // Khởi tạo form và các ô số OTP
  initRegistrationForm();
  initPasswordVisibilityToggles();
  initOtpInputs();

  // Nút xác nhận OTP
  document.getElementById('btnConfirmOtp')?.addEventListener('click', handleVerifyOtp);

  // Nút gửi lại OTP
  document.getElementById('btnResendOtp')?.addEventListener('click', handleResendOtp);

  // Nút đổi email / đóng modal
  document.getElementById('btnChangeEmail')?.addEventListener('click', closeOtpModal);
  document.getElementById('btnCloseOtpModal')?.addEventListener('click', closeOtpModal);
});
