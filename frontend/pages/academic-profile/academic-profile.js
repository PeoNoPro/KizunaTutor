/**
 * KizunaTutor - Hồ sơ cá nhân của Cán bộ học vụ.
 * GET/PUT /api/academic/profile: thông tin tài khoản (sửa họ tên, số điện thoại).
 */

import { API, ApiError } from '/js/api.js';
import { showToast } from '/js/main.js';
import { mountAcademicLayout } from '/components/academic-layout/academic-layout.js';
import { setUserMenuName, initialsOf } from '/components/user-menu/user-menu.js';

const STATUS_VIEW = {
  ACTIVE: { label: 'Đang hoạt động', cls: 'bg-success/10 text-success' },
  PENDING: { label: 'Chờ kích hoạt', cls: 'bg-tertiary-fixed text-tertiary' },
  SUSPENDED: { label: 'Tạm ngưng', cls: 'bg-secondary-fixed text-on-secondary-fixed' },
  LOCKED: { label: 'Đã khóa', cls: 'bg-error-container text-error' },
  BANNED: { label: 'Bị cấm', cls: 'bg-error-container text-error' }
};

const PHONE_PATTERN = /^(\+84|0)\d{9,10}$/;
const $ = (id) => document.getElementById(id);
let current = null;

function setVisible(el, visible) {
  el.classList.toggle('hidden', !visible);
}

function pad(n) {
  return String(n).padStart(2, '0');
}

function formatDate(value) {
  if (!value) return '—';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return '—';
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`;
}

function showFieldError(field, message) {
  const el = $(`${field}Error`);
  if (!el) return;
  el.textContent = message || '';
  setVisible(el, Boolean(message));
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

function render(p) {
  current = p;
  $('avatar').textContent = initialsOf(p.fullName);
  $('nameHeading').textContent = p.fullName;
  $('roleBadge').textContent = p.roleLabel || 'Cán bộ học vụ';
  const status = STATUS_VIEW[p.accountStatus] || { label: p.accountStatus || '', cls: 'bg-surface-container text-on-surface-variant' };
  $('statusBadge').textContent = status.label;
  $('statusBadge').className = `px-2.5 py-0.5 rounded-full text-caption font-semibold ${status.cls}`;
  setVisible($('statusBadge'), Boolean(status.label));
  $('joinedInput').value = formatDate(p.createdAt);

  $('fullNameInput').value = p.fullName || '';
  $('emailInput').value = p.email || '';
  $('phoneInput').value = p.phone || '';

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
    $('errorMessage').textContent = err instanceof ApiError ? err.message : 'Hệ thống đang bận, bạn vui lòng thử lại sau.';
    setVisible($('errorState'), true);
  } finally {
    setVisible($('loadingState'), false);
  }
}

function validate(fullName, phone) {
  let ok = true;
  showFieldError('fullName', '');
  showFieldError('phone', '');
  if (!fullName) {
    showFieldError('fullName', 'Vui lòng nhập họ và tên.');
    ok = false;
  }
  if (phone && !PHONE_PATTERN.test(phone)) {
    showFieldError('phone', 'Số điện thoại không hợp lệ (ví dụ 0912345678).');
    ok = false;
  }
  return ok;
}

async function save(event) {
  event.preventDefault();
  const fullName = $('fullNameInput').value.trim();
  const phone = $('phoneInput').value.trim();
  if (!validate(fullName, phone)) return;

  $('saveButton').disabled = true;
  $('saveText').textContent = 'Đang lưu…';
  try {
    const res = await API.academicOfficer.updateProfile({ fullName, phone });
    render(res.profile);
    syncStoredName(res.profile.fullName);
    showToast(res.message || 'Đã cập nhật hồ sơ cá nhân.', 'success');
  } catch (err) {
    if (err instanceof ApiError && err.status === 401) return;
    if (err instanceof ApiError && err.fields) {
      Object.entries(err.fields).forEach(([field, message]) => showFieldError(field, message));
    }
    showToast(err instanceof ApiError ? err.message : 'Không thể lưu thay đổi.', 'error');
  } finally {
    $('saveButton').disabled = false;
    $('saveText').textContent = 'Lưu thay đổi';
  }
}

mountAcademicLayout('profile', 'Hồ sơ cá nhân');

document.addEventListener('DOMContentLoaded', () => {
  $('retryButton').addEventListener('click', load);
  $('profileForm').addEventListener('submit', save);
  $('resetButton').addEventListener('click', () => {
    if (current) render(current);
    showFieldError('fullName', '');
    showFieldError('phone', '');
  });
  load();
});
