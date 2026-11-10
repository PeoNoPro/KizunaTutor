/**
 * KizunaTutor - Hồ sơ cá nhân của học viên trong dashboard học viên (FE-02, User Story 1).
 * Đặc tả: specs/002-profile-management/. Dữ liệu lấy từ GET/PUT /api/profile/learner/me.
 *
 * Trang có thể được nạp lại bởi sidebarRouter (thay <main> rồi import lại module),
 * nên khởi tạo ngay khi module chạy, không chờ DOMContentLoaded.
 */

import { API, ApiError } from '/js/api.js';
import { API_CONFIG } from '/js/config/api.config.js';

const MAX_AVATAR_SIZE = 5 * 1024 * 1024;
const AVATAR_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
const PHONE_PATTERN = /^((0|\+84)[0-9]{9})?$/;
const AVATAR_URL_PATTERN = /^(https?:\/\/\S+|\/api\/profile\/avatar\/[A-Za-z0-9._-]+)?$/;
const LEVEL_LABELS = { BEGINNER: 'Mới bắt đầu', N5: 'N5', N4: 'N4', N3: 'N3', N2: 'N2', N1: 'N1' };
const DEFAULT_ERROR = 'Hệ thống đang bận, bạn vui lòng thử lại sau.';
const API_ORIGIN = new URL(API_CONFIG.BASE_URL).origin;

const $ = (id) => document.getElementById(id);
let profile = null;
let toastTimer = null;

function errorText(err) {
  return err instanceof ApiError ? err.message : DEFAULT_ERROR;
}

function showState(id) {
  ['profileLoading', 'profileError', 'profileContent'].forEach((s) => $(s).classList.toggle('hidden', s !== id));
  $('btnEditProfile').disabled = id !== 'profileContent';
}

function showToast(message, type = 'success') {
  const toast = $('profileToast');
  if (!toast) return;
  toast.textContent = message;
  toast.className = `fixed bottom-6 right-6 z-[60] max-w-sm px-4 py-3 rounded-xl shadow-lg text-sm font-semibold ${
    type === 'success' ? 'bg-[#059669] text-white' : 'bg-[#BA1A1A] text-white'}`;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.add('hidden'), 3500);
}

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

function formatDate(value) {
  if (!value) return '';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '' : date.toLocaleDateString('vi-VN');
}

function formatDateTime(value) {
  if (!value) return '';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '' : date.toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' });
}

function renderProfile(data) {
  renderAvatar($('profileAvatar'), data.avatarUrl, data.fullName);
  $('profileFullName').textContent = data.fullName || '';
  $('profileEmail').textContent = data.email || '';
  $('profilePhone').textContent = data.phone || 'Chưa cập nhật';
  $('profileCreatedAt').textContent = formatDate(data.createdAt) || 'Chưa có thông tin';
  $('profileCurrentLevel').textContent = LEVEL_LABELS[data.currentLevel] || 'Chưa chọn';
  $('profileTargetLevel').textContent = LEVEL_LABELS[data.targetLevel] || 'Chưa chọn';
  $('profileLearningGoals').textContent = data.learningGoals
    || 'Bạn chưa ghi mục tiêu học tập. Hãy chia sẻ để gia sư hiểu rõ nhu cầu của bạn.';
  const updated = formatDateTime(data.updatedAt);
  $('profileUpdatedAt').textContent = updated ? `Cập nhật lần cuối: ${updated}` : '';
}

/** Đồng bộ tên và ảnh hiển thị ở header/sidebar dùng chung (đọc từ kizuna_user). */
function syncStoredUser(data) {
  try {
    const raw = localStorage.getItem('kizuna_user');
    if (!raw) return;
    const user = JSON.parse(raw) || {};
    user.fullName = data.fullName;
    user.avatarUrl = data.avatarUrl;
    localStorage.setItem('kizuna_user', JSON.stringify(user));
  } catch (e) { /* bỏ qua khi trình duyệt chặn localStorage */ }
}

async function loadProfile() {
  showState('profileLoading');
  try {
    profile = await API.profile.getLearnerProfile();
    renderProfile(profile);
    showState('profileContent');
  } catch (err) {
    $('profileErrorMessage').textContent = errorText(err);
    showState('profileError');
  }
}

// ----------------------------------------------------------------- Form chỉnh sửa

function setFieldError(field, message) {
  const input = $(field);
  const hint = document.querySelector(`[data-error-for="${field}"]`);
  if (input) input.classList.toggle('border-[#BA1A1A]', Boolean(message));
  if (hint) {
    hint.textContent = message || '';
    hint.classList.toggle('hidden', !message);
  }
}

function setFormError(message) {
  $('formError').textContent = message || '';
  $('formError').classList.toggle('hidden', !message);
}

function clearErrors() {
  ['fullName', 'phone', 'avatarUrl', 'learningGoals'].forEach((f) => setFieldError(f, ''));
  setFormError('');
}

function readForm() {
  return {
    fullName: $('fullName').value.trim(),
    phone: $('phone').value.trim(),
    avatarUrl: $('avatarUrl').value.trim(),
    currentLevel: $('currentLevel').value || null,
    targetLevel: $('targetLevel').value || null,
    learningGoals: $('learningGoals').value.trim()
  };
}

function validate(data) {
  const errors = {};
  if (!data.fullName) errors.fullName = 'Vui lòng nhập họ và tên.';
  else if (data.fullName.length > 100) errors.fullName = 'Họ và tên tối đa 100 ký tự.';
  if (!PHONE_PATTERN.test(data.phone)) errors.phone = 'Số điện thoại không đúng định dạng 10 chữ số.';
  if (!AVATAR_URL_PATTERN.test(data.avatarUrl)) errors.avatarUrl = 'Đường dẫn ảnh đại diện phải bắt đầu bằng http:// hoặc https://.';
  if (data.learningGoals.length > 2000) errors.learningGoals = 'Mục tiêu học tập tối đa 2000 ký tự.';
  return errors;
}

function openModal() {
  if (!profile) return;
  clearErrors();
  $('fullName').value = profile.fullName || '';
  $('email').value = profile.email || '';
  $('phone').value = profile.phone || '';
  $('avatarUrl').value = profile.avatarUrl || '';
  $('currentLevel').value = profile.currentLevel || '';
  $('targetLevel').value = profile.targetLevel || '';
  $('learningGoals').value = profile.learningGoals || '';
  $('avatarFile').value = '';
  renderAvatar($('editAvatarPreview'), profile.avatarUrl, profile.fullName);
  $('profileEditModal').classList.remove('hidden');
  $('fullName').focus();
}

function closeModal() {
  $('profileEditModal').classList.add('hidden');
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
    $('avatarUrl').value = uploaded.avatarUrl;
    renderAvatar($('editAvatarPreview'), uploaded.avatarUrl, $('fullName').value);
  } catch (err) {
    setFieldError('avatarUrl', errorText(err));
  } finally {
    label.textContent = 'Tải ảnh lên';
    event.target.value = '';
  }
}

async function handleSubmit(event) {
  event.preventDefault();
  clearErrors();
  const data = readForm();
  const errors = validate(data);
  if (Object.keys(errors).length) {
    Object.entries(errors).forEach(([field, message]) => setFieldError(field, message));
    return;
  }
  const button = $('btnSaveProfile');
  button.disabled = true;
  button.textContent = 'Đang lưu...';
  try {
    profile = await API.profile.updateLearnerProfile(data);
    renderProfile(profile);
    syncStoredUser(profile);
    closeModal();
    showToast('Cập nhật hồ sơ thành công.');
  } catch (err) {
    setFormError(errorText(err));
  } finally {
    button.disabled = false;
    button.textContent = 'Lưu thay đổi';
  }
}

function bindEvents() {
  $('btnEditProfile').addEventListener('click', openModal);
  $('btnRetryProfile').addEventListener('click', loadProfile);
  $('profileEditForm').addEventListener('submit', handleSubmit);
  $('avatarFile').addEventListener('change', handleAvatarFile);
  $('avatarUrl').addEventListener('change', () => renderAvatar($('editAvatarPreview'), $('avatarUrl').value.trim(), $('fullName').value));
  document.querySelectorAll('#profileEditModal [data-close-modal]').forEach((btn) => btn.addEventListener('click', closeModal));
  $('profileEditModal').addEventListener('click', (e) => { if (e.target === e.currentTarget) closeModal(); });
  $('profileEditModal').addEventListener('keydown', (e) => { if (e.key === 'Escape') closeModal(); });
}

if ($('profileContent')) {
  bindEvents();
  loadProfile();
}
