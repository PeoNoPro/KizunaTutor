/**
 * KizunaTutor - Hồ sơ cá nhân của học viên (FE-02, User Story 1), mở từ menu tài khoản trong dashboard học viên.
 * Đặc tả: specs/002-profile-management/. Dữ liệu từ GET/PUT /api/profile/learner/me.
 *
 * Trang chia thành các hàng (thông tin cá nhân, trình độ & mục tiêu, số điện thoại, mật khẩu); mỗi hàng lưu riêng.
 * PUT cần đủ trường nên mỗi lần lưu ghép giá trị mới của hàng với hồ sơ hiện tại.
 * Trang có thể được nạp lại bởi sidebarRouter (thay <main> rồi import lại module) nên khởi tạo ngay khi module chạy.
 */

import { API, ApiError } from '/js/api.js';
import { API_CONFIG } from '/js/config/api.config.js';
import { mountChangePasswordForm } from '/components/change-password/change-password.js';

const MAX_AVATAR_SIZE = 5 * 1024 * 1024;
const AVATAR_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
const PHONE_PATTERN = /^(0|\+84)[0-9]{9}$/;
const AVATAR_URL_PATTERN = /^(https?:\/\/\S+|\/api\/profile\/avatar\/[A-Za-z0-9._-]+)?$/;
const DEFAULT_ERROR = 'Hệ thống đang bận, bạn vui lòng thử lại sau.';
const API_ORIGIN = new URL(API_CONFIG.BASE_URL).origin;

const $ = (id) => document.getElementById(id);
let profile = null;
let avatarUrl = '';

function errorText(err) {
  return err instanceof ApiError ? err.message : DEFAULT_ERROR;
}

function showState(id) {
  ['profileLoading', 'profileError', 'profileContent'].forEach((s) => $(s).classList.toggle('hidden', s !== id));
}

// ----------------------------------------------------------------- Tiện ích hiển thị

/** Ảnh tải lên lưu dạng /api/profile/avatar/... nên cần gắn địa chỉ máy chủ backend. */
function avatarSrc(url) {
  if (!url) return '';
  return url.startsWith('/api/') ? `${API_ORIGIN}${url}` : url;
}

function renderAvatar(container, url, fullName) {
  container.replaceChildren();
  const initial = (fullName || '?').trim().charAt(0).toUpperCase() || '?';
  if (!url) {
    container.textContent = initial;
    return;
  }
  const img = document.createElement('img');
  img.src = avatarSrc(url);
  img.alt = 'Ảnh đại diện';
  img.className = 'w-full h-full object-cover';
  img.addEventListener('error', () => { container.textContent = initial; }, { once: true });
  container.appendChild(img);
}

function setFieldError(field, message) {
  const input = $(field);
  const hint = document.querySelector(`[data-error-for="${field}"]`);
  if (input) input.classList.toggle('border-[#BA1A1A]', Boolean(message));
  if (hint) {
    hint.textContent = message || '';
    hint.classList.toggle('hidden', !message);
  }
}

function setFormMessage(form, message, type = 'error') {
  const box = form.querySelector('[data-form-message]');
  const spacing = form.id === 'phoneForm' ? 'mt-3 ' : '';
  box.textContent = message || '';
  box.className = `${spacing}rounded-lg px-4 py-3 text-sm ${type === 'success'
    ? 'bg-[#059669]/10 border border-[#059669]/20 text-[#047857]'
    : 'bg-[#BA1A1A]/5 border border-[#BA1A1A]/20 text-[#BA1A1A]'}`;
  box.classList.toggle('hidden', !message);
}

function setSaving(form, saving, label) {
  const button = form.querySelector('[data-save]');
  button.disabled = saving;
  form.querySelector('[data-save-label]').textContent = saving ? 'Đang lưu...' : label;
}

function formatDateTime(value) {
  const date = value ? new Date(value) : null;
  return date && !Number.isNaN(date.getTime())
    ? date.toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' }) : '';
}

/** Đồng bộ tên và ảnh hiển thị ở sidebar/header dùng chung (đọc từ kizuna_user). */
function syncStoredUser(data) {
  try {
    const raw = localStorage.getItem('kizuna_user');
    if (!raw) return;
    const user = JSON.parse(raw) || {};
    user.fullName = data.fullName;
    user.avatarUrl = data.avatarUrl;
    localStorage.setItem('kizuna_user', JSON.stringify(user));
  } catch (e) { /* bỏ qua khi trình duyệt chặn localStorage */ }
  const sidebarName = $('sidebarLearnerName');
  if (sidebarName) sidebarName.textContent = data.fullName;
}

// ----------------------------------------------------------------- Đổ dữ liệu vào từng hàng

function fillPersonal() {
  avatarUrl = profile.avatarUrl || '';
  $('fullName').value = profile.fullName || '';
  $('email').value = profile.email || '';
  $('avatarUrl').value = avatarUrl;
  $('btnRemoveAvatar').classList.toggle('hidden', !avatarUrl);
  renderAvatar($('avatarPreview'), avatarUrl, profile.fullName);
  updatePersonalDirty();
}

function fillLearning() {
  $('currentLevel').value = profile.currentLevel || '';
  $('targetLevel').value = profile.targetLevel || '';
  $('learningGoals').value = profile.learningGoals || '';
  updateLearningDirty();
}

function fillPhone() {
  $('phoneText').textContent = profile.phone || 'Chưa cập nhật';
  $('phoneText').classList.toggle('text-[#68737D]', !profile.phone);
  $('btnEditPhone').textContent = profile.phone ? 'Đổi số' : 'Thêm số';
  setPhoneEditing(false);
}

function renderAll() {
  fillPersonal();
  fillLearning();
  fillPhone();
  const updated = formatDateTime(profile.updatedAt);
  $('profileUpdatedAt').textContent = updated ? `Cập nhật lần cuối: ${updated}` : '';
}

function updatePersonalDirty() {
  const dirty = $('fullName').value.trim() !== (profile.fullName || '') || avatarUrl !== (profile.avatarUrl || '');
  $('personalForm').querySelector('[data-save]').disabled = !dirty;
}

function updateLearningDirty() {
  const dirty = $('currentLevel').value !== (profile.currentLevel || '')
    || $('targetLevel').value !== (profile.targetLevel || '')
    || $('learningGoals').value.trim() !== (profile.learningGoals || '');
  $('learningForm').querySelector('[data-save]').disabled = !dirty;
}

function setPhoneEditing(editing) {
  $('phoneView').classList.toggle('hidden', editing);
  $('phoneEdit').classList.toggle('hidden', !editing);
  setFieldError('phone', '');
  if (editing) {
    $('phone').value = profile.phone || '';
    $('phone').focus();
  }
}

// ----------------------------------------------------------------- Lưu

async function save(form, overrides, label) {
  const payload = {
    fullName: profile.fullName,
    phone: profile.phone || '',
    avatarUrl: profile.avatarUrl || '',
    currentLevel: profile.currentLevel || null,
    targetLevel: profile.targetLevel || null,
    learningGoals: profile.learningGoals || '',
    ...overrides
  };
  setFormMessage(form, '');
  setSaving(form, true, label);
  try {
    profile = await API.profile.updateLearnerProfile(payload);
    syncStoredUser(profile);
    return true;
  } catch (err) {
    setFormMessage(form, errorText(err));
    return false;
  } finally {
    setSaving(form, false, label);
  }
}

async function handlePersonalSubmit(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const fullName = $('fullName').value.trim();
  setFieldError('fullName', '');
  setFieldError('avatarUrl', '');
  if (!fullName) return setFieldError('fullName', 'Vui lòng nhập họ và tên.');
  if (fullName.length > 100) return setFieldError('fullName', 'Họ và tên tối đa 100 ký tự.');
  if (!AVATAR_URL_PATTERN.test(avatarUrl)) {
    return setFieldError('avatarUrl', 'Đường dẫn ảnh đại diện phải bắt đầu bằng http:// hoặc https://.');
  }
  if (await save(form, { fullName, avatarUrl }, 'Lưu thay đổi')) {
    fillPersonal();
    $('avatarUrlRow').classList.add('hidden');
    setFormMessage(form, 'Cập nhật hồ sơ thành công.', 'success');
  }
}

async function handleLearningSubmit(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const learningGoals = $('learningGoals').value.trim();
  setFieldError('learningGoals', '');
  if (learningGoals.length > 2000) return setFieldError('learningGoals', 'Mục tiêu học tập tối đa 2000 ký tự.');
  const overrides = {
    currentLevel: $('currentLevel').value || null,
    targetLevel: $('targetLevel').value || null,
    learningGoals
  };
  if (await save(form, overrides, 'Lưu thay đổi')) {
    fillLearning();
    setFormMessage(form, 'Cập nhật hồ sơ thành công.', 'success');
  }
}

async function handlePhoneSubmit(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const phone = $('phone').value.trim();
  setFieldError('phone', '');
  if (!PHONE_PATTERN.test(phone)) return setFieldError('phone', 'Số điện thoại không đúng định dạng 10 chữ số.');
  if (phone === (profile.phone || '')) return setPhoneEditing(false);
  if (await save(form, { phone }, 'Lưu số mới')) {
    fillPhone();
    setFormMessage(form, 'Cập nhật số điện thoại thành công.', 'success');
  }
}

// ----------------------------------------------------------------- Ảnh đại diện

function setAvatar(url) {
  avatarUrl = url;
  $('avatarUrl').value = url;
  $('btnRemoveAvatar').classList.toggle('hidden', !url);
  renderAvatar($('avatarPreview'), url, $('fullName').value);
  updatePersonalDirty();
}

async function handleAvatarFile(event) {
  const file = event.target.files && event.target.files[0];
  if (!file) return;
  setFieldError('avatarUrl', '');
  if (!AVATAR_TYPES.includes(file.type) || file.size > MAX_AVATAR_SIZE) {
    setFieldError('avatarUrl', 'Ảnh đại diện chỉ nhận tệp JPG, PNG hoặc WEBP, dung lượng tối đa 5MB.');
    event.target.value = '';
    return;
  }
  const label = $('avatarUploadLabel');
  label.textContent = 'Đang tải ảnh...';
  try {
    const uploaded = await API.profile.uploadAvatar(file);
    setAvatar(uploaded.avatarUrl);
  } catch (err) {
    setFieldError('avatarUrl', errorText(err));
  } finally {
    label.textContent = 'Tải ảnh lên';
    event.target.value = '';
  }
}

// ----------------------------------------------------------------- Khởi tạo

async function loadProfile() {
  showState('profileLoading');
  try {
    profile = await API.profile.getLearnerProfile();
    renderAll();
    showState('profileContent');
  } catch (err) {
    $('profileErrorMessage').textContent = errorText(err);
    showState('profileError');
  }
}

function bindEvents() {
  $('btnRetryProfile').addEventListener('click', loadProfile);

  $('personalForm').addEventListener('submit', handlePersonalSubmit);
  $('fullName').addEventListener('input', updatePersonalDirty);
  $('avatarFile').addEventListener('change', handleAvatarFile);
  $('btnToggleAvatarUrl').addEventListener('click', () => {
    $('avatarUrlRow').classList.toggle('hidden');
    if (!$('avatarUrlRow').classList.contains('hidden')) $('avatarUrl').focus();
  });
  $('avatarUrl').addEventListener('change', () => setAvatar($('avatarUrl').value.trim()));
  $('btnRemoveAvatar').addEventListener('click', () => setAvatar(''));

  $('learningForm').addEventListener('submit', handleLearningSubmit);
  ['currentLevel', 'targetLevel'].forEach((id) => $(id).addEventListener('change', updateLearningDirty));
  $('learningGoals').addEventListener('input', updateLearningDirty);

  $('phoneForm').addEventListener('submit', handlePhoneSubmit);
  $('btnEditPhone').addEventListener('click', () => {
    setFormMessage($('phoneForm'), '');
    setPhoneEditing(true);
  });
  $('btnCancelPhone').addEventListener('click', () => setPhoneEditing(false));
}

if ($('profileContent')) {
  bindEvents();
  mountChangePasswordForm($('changePasswordSection'));
  loadProfile();
}
