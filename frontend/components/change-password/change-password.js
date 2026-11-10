/**
 * KizunaTutor - Hàng "Mật khẩu" trong khung hồ sơ cá nhân, dùng chung cho học viên và gia sư
 * (FE-02, contracts/profile-api.md mục 2A). Thu gọn mặc định; bấm "Đổi mật khẩu" để mở form.
 *
 * Trang dùng (bên trong khung các hàng hồ sơ):
 *   <div id="changePasswordSection"></div>
 *   import { mountChangePasswordForm } from '/components/change-password/change-password.js';
 *   mountChangePasswordForm(document.getElementById('changePasswordSection'));
 */

import { API } from '/js/api.js';

// Mặc định: học viên và gia sư (PUT /api/profile/password).
// Nhân sự nội bộ truyền API riêng: mountChangePasswordForm(el, { submit: (data) => ... }).
// Không dùng thẻ <p> vì trang quản trị có quy tắc `.admin-portal p` ghi đè màu chữ.
const defaultSubmit = (data) => API.profile.changePassword(data);

const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*[0-9]).+$/;
const FIELDS = ['currentPassword', 'newPassword', 'confirmPassword'];
const INPUT_CLASS = 'w-full rounded-lg border border-[#D9DEE5] bg-white pl-3 pr-10 py-2.5 text-sm text-[#0F2545] focus:outline-none focus:border-[#006194] focus:ring-2 focus:ring-[#006194]/15';
const OUTLINE_BUTTON = 'px-3 py-1.5 rounded-lg border border-[#006194] text-[#006194] text-sm font-medium hover:bg-[#006194]/5';

function fieldHtml(name, label, autocomplete, hint = '') {
  return `
    <div>
      <label for="cp_${name}" class="block text-sm text-[#0F2545] mb-1.5">${label}</label>
      <div class="relative">
        <input id="cp_${name}" name="${name}" type="password" autocomplete="${autocomplete}" maxlength="64" class="${INPUT_CLASS}">
        <button type="button" data-toggle-password="cp_${name}" class="absolute inset-y-0 right-0 px-3 text-[#68737D] hover:text-[#0F2545]" aria-label="Hiện hoặc ẩn mật khẩu">
          <span class="material-symbols-outlined text-[18px]">visibility</span>
        </button>
      </div>
      ${hint ? `<div class="mt-1 text-xs text-[#68737D]">${hint}</div>` : ''}
      <div data-cp-error="${name}" class="hidden mt-1 text-xs text-[#BA1A1A]"></div>
    </div>`;
}

function validate(data) {
  const errors = {};
  if (!data.currentPassword) errors.currentPassword = 'Vui lòng nhập mật khẩu hiện tại.';
  if (!data.newPassword) errors.newPassword = 'Vui lòng nhập mật khẩu mới.';
  else if (data.newPassword.length < 8 || data.newPassword.length > 64) errors.newPassword = 'Mật khẩu mới dài 8–64 ký tự.';
  else if (!PASSWORD_PATTERN.test(data.newPassword)) errors.newPassword = 'Mật khẩu mới cần có cả chữ và số.';
  else if (data.newPassword === data.currentPassword) errors.newPassword = 'Mật khẩu mới cần khác mật khẩu hiện tại.';
  if (!data.confirmPassword) errors.confirmPassword = 'Vui lòng nhập lại mật khẩu mới.';
  else if (data.confirmPassword !== data.newPassword) errors.confirmPassword = 'Mật khẩu xác nhận không khớp.';
  return errors;
}

export function mountChangePasswordForm(container, { submit: submitPassword = defaultSubmit } = {}) {
  if (!container) return;
  container.className = 'grid grid-cols-1 md:grid-cols-[240px,1fr] gap-4 md:gap-8 p-6';
  container.innerHTML = `
    <div>
      <div class="flex items-center gap-2">
        <span class="material-symbols-outlined text-[20px] text-[#006194]">key</span>
        <h2 class="text-[15px] font-bold text-[#0F2545]">Mật khẩu</h2>
      </div>
      <div class="text-xs text-[#68737D] mt-1 leading-relaxed">Dùng cùng email để đăng nhập. Nên đổi định kỳ để bảo vệ tài khoản.</div>
    </div>
    <div>
      <div data-cp-summary class="flex flex-wrap items-center justify-between gap-3">
        <span class="text-sm text-[#68737D] tracking-[0.2em]">••••••••</span>
        <button type="button" data-cp-open class="${OUTLINE_BUTTON}">Đổi mật khẩu</button>
      </div>
      <div data-cp-message class="hidden mt-3 rounded-lg px-4 py-3 text-sm"></div>
      <form data-cp-form novalidate class="hidden mt-4 space-y-4">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          ${fieldHtml('currentPassword', 'Mật khẩu hiện tại', 'current-password')}
          ${fieldHtml('newPassword', 'Mật khẩu mới', 'new-password', '8–64 ký tự, có cả chữ và số.')}
          ${fieldHtml('confirmPassword', 'Nhập lại mật khẩu mới', 'new-password')}
        </div>
        <div class="flex justify-end gap-2">
          <button type="button" data-cp-cancel class="px-4 py-2 rounded-lg border border-[#D9DEE5] text-sm font-medium text-[#3F4850] hover:bg-[#F8FAFC]">Hủy</button>
          <button type="submit" data-cp-submit class="inline-flex items-center gap-1.5 px-4 py-2 rounded-lg bg-[#006194] text-white text-sm font-semibold hover:bg-[#004b73] disabled:opacity-50 disabled:cursor-not-allowed">
            <span class="material-symbols-outlined text-[18px]">save</span><span data-cp-submit-label>Lưu mật khẩu</span>
          </button>
        </div>
      </form>
    </div>`;

  const form = container.querySelector('[data-cp-form]');
  const summary = container.querySelector('[data-cp-summary]');
  const message = container.querySelector('[data-cp-message]');
  const submit = container.querySelector('[data-cp-submit]');
  const submitLabel = container.querySelector('[data-cp-submit-label]');

  const setFieldError = (name, text) => {
    const hint = container.querySelector(`[data-cp-error="${name}"]`);
    container.querySelector(`#cp_${name}`).classList.toggle('border-[#BA1A1A]', Boolean(text));
    hint.textContent = text || '';
    hint.classList.toggle('hidden', !text);
  };
  const setMessage = (text, type) => {
    message.textContent = text || '';
    message.className = `mt-3 rounded-lg px-4 py-3 text-sm ${type === 'success'
      ? 'bg-[#059669]/10 border border-[#059669]/20 text-[#047857]'
      : 'bg-[#BA1A1A]/5 border border-[#BA1A1A]/20 text-[#BA1A1A]'}`;
    message.classList.toggle('hidden', !text);
  };
  const setOpen = (open) => {
    form.classList.toggle('hidden', !open);
    summary.classList.toggle('hidden', open);
    if (open) {
      setMessage('');
      container.querySelector('#cp_currentPassword').focus();
    } else {
      form.reset();
      FIELDS.forEach((f) => setFieldError(f, ''));
    }
  };

  container.querySelector('[data-cp-open]').addEventListener('click', () => setOpen(true));
  container.querySelector('[data-cp-cancel]').addEventListener('click', () => setOpen(false));
  container.querySelectorAll('[data-toggle-password]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const input = container.querySelector(`#${btn.dataset.togglePassword}`);
      const show = input.type === 'password';
      input.type = show ? 'text' : 'password';
      btn.querySelector('span').textContent = show ? 'visibility_off' : 'visibility';
    });
  });

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    FIELDS.forEach((f) => setFieldError(f, ''));
    setMessage('');
    const data = Object.fromEntries(FIELDS.map((f) => [f, container.querySelector(`#cp_${f}`).value]));
    const errors = validate(data);
    if (Object.keys(errors).length) {
      Object.entries(errors).forEach(([f, text]) => setFieldError(f, text));
      return;
    }
    submit.disabled = true;
    submitLabel.textContent = 'Đang lưu...';
    try {
      await submitPassword(data);
      setOpen(false);
      setMessage('Đổi mật khẩu thành công. Hãy dùng mật khẩu mới cho lần đăng nhập tiếp theo.', 'success');
    } catch (err) {
      // ApiError (authRequest) và lỗi của fetchAPI đều đã mang thông điệp tiếng Việt.
      setMessage(err && err.message ? err.message : 'Hệ thống đang bận, bạn vui lòng thử lại sau.', 'error');
    } finally {
      submit.disabled = false;
      submitLabel.textContent = 'Lưu mật khẩu';
    }
  });
}
