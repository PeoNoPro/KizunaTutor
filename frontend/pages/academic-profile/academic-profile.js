/**
 * KizunaTutor - Hồ sơ cá nhân của Cán bộ học vụ.
 * GET/PUT /api/academic/profile: thông tin tài khoản (sửa họ tên, số điện thoại).
 * Bố cục các hàng đồng bộ với hồ sơ học viên (FE-02). Hàng Mật khẩu dùng khối chung components/change-password,
 * gửi tới API đổi mật khẩu của nhân sự nội bộ (PUT /api/admin/operations/profile/password, có CSRF),
 * vốn đã cho phép Cán bộ học vụ.
 */

import { API, ApiError, fetchAPI } from '/js/api.js';
import { mountAcademicLayout } from '/components/academic-layout/academic-layout.js';
import { setUserMenuName, initialsOf } from '/components/user-menu/user-menu.js';
import { mountChangePasswordForm } from '/components/change-password/change-password.js';

const STATUS_VIEW = {
  ACTIVE: { label: 'Đang hoạt động', cls: 'bg-[#059669]/10 text-[#047857]' },
  PENDING: { label: 'Chờ kích hoạt', cls: 'bg-[#D97706]/10 text-[#B45309]' },
  SUSPENDED: { label: 'Tạm ngưng', cls: 'bg-[#D97706]/10 text-[#B45309]' },
  LOCKED: { label: 'Đã khóa', cls: 'bg-[#BA1A1A]/10 text-[#BA1A1A]' },
  BANNED: { label: 'Bị cấm', cls: 'bg-[#BA1A1A]/10 text-[#BA1A1A]' }
};
const STAFF_PROFILE_PATH = '/admin/operations/profile';
const PHONE_PATTERN = /^(\+84|0)\d{9,10}$/;
const DEFAULT_ERROR = 'Hệ thống đang bận, bạn vui lòng thử lại sau.';
const $ = (id) => document.getElementById(id);
let current = null;

function setVisible(el, visible) {
  el.classList.toggle('hidden', !visible);
}

function pad(n) {
  return String(n).padStart(2, '0');
}

function formatDate(value) {
  if (!value) return '';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return '';
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`;
}

function showFieldError(field, message) {
  const el = $(`${field}Error`);
  if (!el) return;
  el.textContent = message || '';
  setVisible(el, Boolean(message));
  const input = $(`${field}Input`);
  if (input) input.classList.toggle('border-[#BA1A1A]', Boolean(message));
}

function setFormMessage(form, message, type = 'error') {
  const box = form.querySelector('[data-form-message]');
  box.textContent = message || '';
  box.className = `${form.id === 'phoneForm' ? 'mt-3 ' : ''}rounded-lg px-4 py-3 text-sm ${type === 'success'
    ? 'bg-[#059669]/10 border border-[#059669]/20 text-[#047857]'
    : 'bg-[#BA1A1A]/5 border border-[#BA1A1A]/20 text-[#BA1A1A]'}`;
  setVisible(box, Boolean(message));
}

/** Lưu tên mới vào phiên đăng nhập để sidebar các trang khác hiển thị đúng. */
function syncStoredName(fullName) {
  try {
    const raw = localStorage.getItem('kizuna_user');
    if (!raw) return;
    const user = JSON.parse(raw);
    user.fullName = fullName;
    localStorage.setItem('kizuna_user', JSON.stringify(user));
  } catch (e) {}
}

function updateDirty() {
  $('saveButton').disabled = !current || $('fullNameInput').value.trim() === (current.fullName || '');
}

function setPhoneEditing(editing) {
  setVisible($('phoneView'), !editing);
  setVisible($('phoneEdit'), editing);
  showFieldError('phone', '');
  if (editing) {
    $('phoneInput').value = current?.phone || '';
    $('phoneInput').focus();
  }
}

function render(p) {
  current = p;
  $('avatar').textContent = initialsOf(p.fullName);
  $('nameHeading').textContent = p.fullName;
  $('roleBadge').textContent = p.roleLabel || 'Cán bộ học vụ';
  const status = STATUS_VIEW[p.accountStatus];
  $('statusBadge').textContent = status ? status.label : '';
  $('statusBadge').className = `px-2.5 py-0.5 rounded-full text-xs font-semibold ${status ? status.cls : ''}`;
  setVisible($('statusBadge'), Boolean(status));
  const joined = formatDate(p.createdAt);
  $('joinedText').textContent = joined ? `Tham gia từ ${joined}` : '';

  $('fullNameInput').value = p.fullName || '';
  $('emailInput').value = p.email || '';
  $('phoneText').textContent = p.phone || 'Chưa cập nhật';
  $('phoneText').classList.toggle('text-[#68737D]', !p.phone);
  $('btnEditPhone').textContent = p.phone ? 'Đổi số' : 'Thêm số';
  setPhoneEditing(false);
  updateDirty();

  setUserMenuName('academic', p.fullName);
}

async function load() {
  setVisible($('loadingState'), true);
  setVisible($('errorState'), false);
  setVisible($('contentState'), false);
  try {
    render(await API.academicOfficer.getProfile());
    setVisible($('contentState'), true);
  } catch (err) {
    if (err instanceof ApiError && err.status === 401) return;
    $('errorMessage').textContent = err instanceof ApiError ? err.message : DEFAULT_ERROR;
    setVisible($('errorState'), true);
  } finally {
    setVisible($('loadingState'), false);
  }
}

/** PUT cần cả họ tên và số điện thoại nên ghép giá trị mới với hồ sơ hiện tại. */
async function saveProfile(form, button, labelEl, label, changes, successMessage) {
  setFormMessage(form, '');
  button.disabled = true;
  labelEl.textContent = 'Đang lưu…';
  try {
    const res = await API.academicOfficer.updateProfile({ fullName: current.fullName, phone: current.phone || '', ...changes });
    render(res.profile);
    syncStoredName(res.profile.fullName);
    setFormMessage(form, successMessage, 'success');
  } catch (err) {
    if (err instanceof ApiError && err.status === 401) return;
    if (err instanceof ApiError && err.fields) {
      Object.entries(err.fields).forEach(([field, message]) => showFieldError(field, message));
    }
    setFormMessage(form, err instanceof ApiError ? err.message : 'Không thể lưu thay đổi.');
    button.disabled = false;
  } finally {
    labelEl.textContent = label;
  }
}

function handleProfileSubmit(event) {
  event.preventDefault();
  const fullName = $('fullNameInput').value.trim();
  showFieldError('fullName', '');
  if (!fullName) return showFieldError('fullName', 'Vui lòng nhập họ và tên.');
  saveProfile($('profileForm'), $('saveButton'), $('saveText'), 'Lưu thay đổi', { fullName }, 'Đã cập nhật hồ sơ cá nhân.');
}

function handlePhoneSubmit(event) {
  event.preventDefault();
  const phone = $('phoneInput').value.trim();
  showFieldError('phone', '');
  if (phone && !PHONE_PATTERN.test(phone)) return showFieldError('phone', 'Số điện thoại không hợp lệ (ví dụ 0912345678).');
  if (phone === (current?.phone || '')) return setPhoneEditing(false);
  saveProfile($('phoneForm'), $('savePhoneButton'), $('savePhoneText'), 'Lưu số mới', { phone }, 'Đã cập nhật số điện thoại.');
}

mountAcademicLayout('profile', 'Hồ sơ cá nhân');

document.addEventListener('DOMContentLoaded', () => {
  $('retryButton').addEventListener('click', load);
  $('profileForm').addEventListener('submit', handleProfileSubmit);
  $('fullNameInput').addEventListener('input', updateDirty);
  $('phoneForm').addEventListener('submit', handlePhoneSubmit);
  $('btnEditPhone').addEventListener('click', () => { setFormMessage($('phoneForm'), ''); setPhoneEditing(true); });
  $('btnCancelPhone').addEventListener('click', () => setPhoneEditing(false));
  mountChangePasswordForm($('changePasswordSection'), {
    submit: ({ currentPassword, newPassword }) => fetchAPI(`${STAFF_PROFILE_PATH}/password`, {
      method: 'PUT', body: { currentPassword, newPassword }, csrfPath: `${STAFF_PROFILE_PATH}/csrf`
    })
  });
  load();
});
