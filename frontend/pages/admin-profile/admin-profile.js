/**
 * KizunaTutor - Hồ sơ cá nhân của nhân sự nội bộ (Quản trị viên, Quản lý vận hành, CSKH) trong khung quản trị.
 * API giữ nguyên: GET/PUT /api/admin/operations/profile (họ tên, số điện thoại), PUT .../profile/password.
 * Bố cục các hàng đồng bộ với hồ sơ học viên (FE-02); hàng Mật khẩu dùng khối chung components/change-password.
 */

import { api, byId, createOperationsPage } from '../../js/pages/operations.js';
import { fetchAPI } from '../../js/api.js';
import { mountChangePasswordForm } from '/components/change-password/change-password.js';

const PATH = '/admin/operations/profile';
const PHONE_PATTERN = /^[0-9+ ]{8,20}$/;
// Mọi nhân viên nội bộ (không chỉ Admin) lấy mã CSRF ở endpoint riêng của trang hồ sơ.
const save = (path, body) => fetchAPI(path, { method: 'PUT', body, csrfPath: PATH + '/csrf' });
const state = { profile: null };

function initialsOf(name) {
  return String(name || '').trim().split(/\s+/).filter(Boolean).slice(-2).map((w) => w[0]).join('').toLocaleUpperCase('vi-VN') || '—';
}

function renderAvatar(data) {
  const avatar = byId('profileAvatar');
  avatar.replaceChildren();
  avatar.textContent = initialsOf(data.fullName);
  if (!data.avatarUrl) return;
  try {
    const url = new URL(data.avatarUrl, location.origin);
    if (url.protocol === 'https:' || (url.origin === location.origin && url.pathname.startsWith('/assets/'))) {
      const img = document.createElement('img');
      img.src = url.href;
      img.alt = '';
      img.referrerPolicy = 'no-referrer';
      img.className = 'w-full h-full object-cover';
      img.addEventListener('load', () => avatar.replaceChildren(img), { once: true });
    }
  } catch { /* Dùng chữ viết tắt khi địa chỉ ảnh không hợp lệ. */ }
}

function setFieldError(field, message) {
  byId(field).classList.toggle('border-[#BA1A1A]', Boolean(message));
  const hint = document.querySelector(`[data-error-for="${field}"]`);
  hint.textContent = message || '';
  hint.classList.toggle('hidden', !message);
}

function setFormMessage(form, message, type = 'error') {
  const box = form.querySelector('[data-form-message]');
  box.textContent = message || '';
  box.className = `${form.id === 'phoneForm' ? 'mt-3 ' : ''}rounded-lg px-4 py-3 text-sm ${type === 'success'
    ? 'bg-[#059669]/10 border border-[#059669]/20 text-[#047857]'
    : 'bg-[#BA1A1A]/5 border border-[#BA1A1A]/20 text-[#BA1A1A]'}`;
  box.classList.toggle('hidden', !message);
}

function updatePersonalDirty() {
  const dirty = Boolean(state.profile) && byId('profileFullName').value.trim() !== (state.profile.fullName || '');
  byId('personalForm').querySelector('[data-save]').disabled = !dirty;
}

function setPhoneEditing(editing) {
  byId('phoneView').classList.toggle('hidden', editing);
  byId('phoneEdit').classList.toggle('hidden', !editing);
  setFieldError('profilePhone', '');
  if (editing) {
    byId('profilePhone').value = state.profile?.phone || '';
    byId('profilePhone').focus();
  }
}

function render(data) {
  state.profile = data;
  renderAvatar(data);
  byId('profileName').textContent = data.fullName;
  byId('profileDetails').textContent = data.email || '';
  byId('profileRole').textContent = data.roleLabel || '';
  byId('profileStatus').textContent = data.statusLabel || '';
  byId('profileFullName').value = data.fullName || '';
  byId('profileEmail').value = data.email || '';
  byId('phoneText').textContent = data.phone || 'Chưa cập nhật';
  byId('phoneText').classList.toggle('text-[#68737D]', !data.phone);
  byId('btnEditPhone').textContent = data.phone ? 'Đổi số' : 'Thêm số';
  setPhoneEditing(false);
  // createOperationsPage bật lại mọi nút submit sau khi tải xong, nên tính trạng thái nút sau lượt đó.
  setTimeout(updatePersonalDirty, 0);
  // Header của khung quản trị cũng hiện tên mới.
  const name = document.querySelector('.admin-identity strong');
  if (name) name.textContent = data.fullName;
}

async function load() {
  render(await api(PATH));
}

/** PUT cần cả họ tên và số điện thoại nên ghép giá trị mới với hồ sơ hiện tại. */
async function saveProfile(form, changes, label, successMessage) {
  const button = form.querySelector('[data-save]');
  const labelEl = form.querySelector('[data-save-label]');
  setFormMessage(form, '');
  button.disabled = true;
  labelEl.textContent = 'Đang lưu...';
  try {
    await save(PATH, { fullName: state.profile.fullName, phone: state.profile.phone || '', ...changes });
    render(await api(PATH));
    setFormMessage(form, successMessage, 'success');
  } catch (error) {
    if (error.status === 401) { location.assign('/login'); return; }
    setFormMessage(form, error.message || 'Chưa lưu được. Vui lòng thử lại.');
    button.disabled = false;
  } finally {
    labelEl.textContent = label;
  }
}

byId('personalForm').addEventListener('submit', (event) => {
  event.preventDefault();
  const fullName = byId('profileFullName').value.trim();
  setFieldError('profileFullName', '');
  if (!fullName) return setFieldError('profileFullName', 'Vui lòng nhập họ và tên.');
  saveProfile(event.currentTarget, { fullName }, 'Lưu thay đổi', 'Đã lưu thông tin cá nhân.');
});
byId('profileFullName').addEventListener('input', updatePersonalDirty);

byId('phoneForm').addEventListener('submit', (event) => {
  event.preventDefault();
  const phone = byId('profilePhone').value.trim();
  setFieldError('profilePhone', '');
  if (phone && !PHONE_PATTERN.test(phone)) return setFieldError('profilePhone', 'Số điện thoại gồm 8–20 chữ số.');
  if (phone === (state.profile?.phone || '')) return setPhoneEditing(false);
  saveProfile(event.currentTarget, { phone }, 'Lưu số mới', 'Đã cập nhật số điện thoại.');
});
byId('btnEditPhone').addEventListener('click', () => { setFormMessage(byId('phoneForm'), ''); setPhoneEditing(true); });
byId('btnCancelPhone').addEventListener('click', () => setPhoneEditing(false));

mountChangePasswordForm(byId('changePasswordSection'), {
  submit: ({ currentPassword, newPassword }) => save(PATH + '/password', { currentPassword, newPassword })
});

createOperationsPage(load).start();
