/**
 * KizunaTutor - Authentication UI & Forgot/Reset Password Module (ES6)
 * Tách logic dùng chung cho trang Đăng nhập & Đăng ký (Zero-Reload & Anti-Silent Failure)
 */
import { apiClient } from '/js/api/apiClient.js';
import { KizunaToast } from '/js/apiClient.js';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/**
 * Hàm dùng chung cập nhật trạng thái disable/loading cho nút bấm
 */
function toggleButtonLoading(button, isLoading, defaultHtml, loadingLabel) {
  if (!button) return;
  button.disabled = isLoading;
  button.innerHTML = isLoading
    ? `<i class="fa-solid fa-spinner fa-spin"></i><span>${loadingLabel}</span>`
    : defaultHtml;
}

/**
 * 1. Khởi tạo tính năng Ẩn/Hiện mật khẩu cho mọi input có nút [data-toggle-password]
 */
export function initPasswordVisibilityToggles() {
  const toggleButtons = document.querySelectorAll('[data-toggle-password]');

  toggleButtons.forEach((button) => {
    if (button.dataset.bound === 'true') return;
    button.dataset.bound = 'true';

    button.addEventListener('click', () => {
      const targetSelector = button.getAttribute('data-password-target');
      const passwordInput = targetSelector ? document.querySelector(targetSelector) : null;
      const iconElement = button.querySelector('i');

      if (!passwordInput || !iconElement) {
        console.error('[AuthUI] Không tìm thấy ô mật khẩu hoặc icon mục tiêu:', targetSelector);
        KizunaToast.danger('Không thể thao tác với trường mật khẩu lúc này.');
        return;
      }

      const isHidden = passwordInput.type === 'password';
      passwordInput.type = isHidden ? 'text' : 'password';

      iconElement.classList.toggle('fa-eye', !isHidden);
      iconElement.classList.toggle('fa-eye-slash', isHidden);
      button.setAttribute('aria-pressed', String(isHidden));
      button.setAttribute('aria-label', isHidden ? 'Ẩn mật khẩu' : 'Hiện mật khẩu');
    });
  });
}

/**
 * 2. Khởi tạo Modal Quên mật khẩu 2 bước:
 *    - Bước 1: Nhập Gmail -> Gửi mật khẩu ngẫu nhiên qua API (/api/auth/forgot-password)
 *    - Bước 2: Chuyển giao diện giữ nguyên Gmail -> Nhập mật khẩu được cấp + Đổi & xác minh mật khẩu mới (/api/auth/reset-password)
 */
export function initForgotPasswordModal() {
  const modal = document.getElementById('forgotPasswordModal');
  const openBtn = document.getElementById('btnOpenForgotModal');
  const closeBtn = document.getElementById('btnCloseForgotModal');
  const cancelBtn = document.getElementById('btnCancelForgotModal');
  const backStep1Btn = document.getElementById('btnBackToForgotStep1');

  const step1View = document.getElementById('forgotStep1View');
  const step2View = document.getElementById('forgotStep2View');

  const forgotForm = document.getElementById('forgotPasswordForm');
  const forgotEmailInput = document.getElementById('forgotEmailInput');
  const sendBtn = document.getElementById('btnSendForgotOtp');

  const resetForm = document.getElementById('resetPasswordForm');
  const resetEmailInput = document.getElementById('resetEmailInput');
  const tempPasswordInput = document.getElementById('resetTempPassword');
  const newPasswordInput = document.getElementById('resetNewPassword');
  const confirmPasswordInput = document.getElementById('resetConfirmPassword');
  const confirmResetBtn = document.getElementById('btnConfirmResetPassword');

  if (!modal || !openBtn || !forgotForm || !forgotEmailInput || !sendBtn) {
    return;
  }
  if (forgotForm.dataset.bound === 'true') return;
  forgotForm.dataset.bound = 'true';

  const defaultSendHtml = sendBtn.innerHTML;
  const defaultConfirmResetHtml = confirmResetBtn ? confirmResetBtn.innerHTML : '';

  const showStep = (stepNumber, preservedEmail = '') => {
    if (!step1View || !step2View) return;

    if (stepNumber === 2) {
      step1View.classList.add('hidden');
      step2View.classList.remove('hidden');
      if (resetEmailInput && preservedEmail) {
        resetEmailInput.value = preservedEmail;
      }
      tempPasswordInput?.focus();
    } else {
      step2View.classList.add('hidden');
      step1View.classList.remove('hidden');
      if (preservedEmail) {
        forgotEmailInput.value = preservedEmail;
      }
      forgotEmailInput.focus();
    }
  };

  const openModal = (event) => {
    if (event) event.preventDefault();
    const loginEmail = document.getElementById('loginEmail')?.value?.trim();
    if (loginEmail && !forgotEmailInput.value) {
      forgotEmailInput.value = loginEmail;
    }
    showStep(1, forgotEmailInput.value.trim());
    modal.classList.remove('hidden');
    modal.classList.add('flex');
  };

  const closeModal = () => {
    modal.classList.add('hidden');
    modal.classList.remove('flex');
  };

  openBtn.addEventListener('click', openModal);
  closeBtn?.addEventListener('click', closeModal);
  cancelBtn?.addEventListener('click', closeModal);
  backStep1Btn?.addEventListener('click', () => {
    showStep(1, resetEmailInput?.value?.trim() || forgotEmailInput.value.trim());
  });

  modal.addEventListener('click', (event) => {
    if (event.target === modal) {
      closeModal();
    }
  });

  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape' && !modal.classList.contains('hidden')) {
      closeModal();
    }
  });

  // BƯỚC 1: Gửi mật khẩu ngẫu nhiên về Gmail -> Chuyển sang Bước 2 (giữ Gmail)
  forgotForm.addEventListener('submit', async (event) => {
    event.preventDefault();

    const email = forgotEmailInput.value.trim();
    if (!email || !EMAIL_REGEX.test(email)) {
      KizunaToast.danger('Vui lòng nhập địa chỉ email hợp lệ để nhận mật khẩu mới.');
      forgotEmailInput.focus();
      return;
    }

    toggleButtonLoading(sendBtn, true, defaultSendHtml, 'Đang cấp mật khẩu mới...');

    try {
      const response = await apiClient.post('/api/auth/forgot-password', { email });

      if (!response || response.success === false) {
        throw new Error(response?.message || 'Không thể cấp lại mật khẩu lúc này. Vui lòng thử lại.');
      }

      KizunaToast.success(
        response.message || `Mật khẩu mới đã được gửi tới ${email}. Vui lòng nhập mật khẩu trong email để đặt lại mật khẩu!`
      );

      // Chuyển giao diện sang Bước 2 và giữ nguyên tài khoản Gmail của người dùng
      resetForm?.reset();
      showStep(2, response.data?.email || email);
    } catch (error) {
      console.error('[ForgotPassword] Lỗi khi cấp lại mật khẩu mới:', error);
      KizunaToast.danger(error?.message || 'Hệ thống gặp sự cố khi gửi mật khẩu mới. Vui lòng thử lại sau.');
    } finally {
      toggleButtonLoading(sendBtn, false, defaultSendHtml);
    }
  });

  // BƯỚC 2: Xác minh mật khẩu được cấp & Đổi sang mật khẩu mới
  if (resetForm && confirmResetBtn) {
    resetForm.addEventListener('submit', async (event) => {
      event.preventDefault();

      const email = resetEmailInput?.value?.trim() || '';
      const temporaryPassword = tempPasswordInput?.value || '';
      const newPassword = newPasswordInput?.value || '';
      const confirmPassword = confirmPasswordInput?.value || '';

      if (!email || !EMAIL_REGEX.test(email)) {
        KizunaToast.danger('Thiếu thông tin Gmail tài khoản. Vui lòng quay lại Bước 1.');
        showStep(1);
        return;
      }

      if (!temporaryPassword.trim()) {
        KizunaToast.danger('Vui lòng nhập mật khẩu được cấp trong email.');
        tempPasswordInput?.focus();
        return;
      }

      if (newPassword.length < 6) {
        KizunaToast.danger('Mật khẩu mới phải có ít nhất 6 ký tự.');
        newPasswordInput?.focus();
        return;
      }

      if (newPassword !== confirmPassword) {
        KizunaToast.danger('Mật khẩu mới và mật khẩu xác minh không trùng khớp.');
        confirmPasswordInput?.focus();
        return;
      }

      toggleButtonLoading(confirmResetBtn, true, defaultConfirmResetHtml, 'Đang xác minh & đổi mật khẩu...');

      try {
        const response = await apiClient.post('/api/auth/reset-password', {
          email,
          temporaryPassword,
          newPassword,
          confirmPassword
        });

        if (!response || response.success === false) {
          throw new Error(response?.message || 'Không thể đổi mật khẩu lúc này. Vui lòng thử lại.');
        }

        KizunaToast.success(
          response.message || 'Đổi mật khẩu thành công! Bạn có thể đăng nhập bằng mật khẩu mới.'
        );

        // Điền sẵn Gmail vào form đăng nhập chính và đóng modal
        const loginEmailEl = document.getElementById('loginEmail');
        const loginPasswordEl = document.getElementById('loginPassword');
        if (loginEmailEl) loginEmailEl.value = email;
        if (loginPasswordEl) {
          loginPasswordEl.value = '';
          loginPasswordEl.focus();
        }

        forgotForm.reset();
        resetForm.reset();
        showStep(1);
        closeModal();
      } catch (error) {
        console.error('[ResetPassword] Lỗi khi xác minh và đổi mật khẩu:', error);
        KizunaToast.danger(error?.message || 'Xác minh mật khẩu thất bại. Vui lòng kiểm tra lại.');
      } finally {
        toggleButtonLoading(confirmResetBtn, false, defaultConfirmResetHtml);
      }
    });
  }
}
