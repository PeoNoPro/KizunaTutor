/**
 * KizunaTutor - Hồ sơ cá nhân & giảng dạy của Gia sư (FE-02, User Story 2).
 * Đặc tả: specs/002-profile-management/. Dữ liệu từ GET/PUT /api/profile/tutor/me.
 */

import { API, ApiError } from '/js/api.js';
import { API_CONFIG } from '/js/config/api.config.js';
import { mountTutorLayout, setTutorIdentity } from '/components/tutor-layout/tutor-layout.js';
import { mountChangePasswordForm } from '/components/change-password/change-password.js';
import {
  DEFAULT_PROFILE_ERROR,
  INCOMPLETE_TUTOR_BANNER,
  VERIFICATION_LABEL_MAP,
  formatCurrencyVnd,
  formatDateTime,
  isTutorProfileIncomplete,
  renderAvatarNode,
  syncLocalUser,
  validateAvatarFile,
  validateTutorForm
} from '/js/profile-utils.js';

const API_ORIGIN = new URL(API_CONFIG.BASE_URL).origin;
const $ = (id) => document.getElementById(id);

const state = {
  profile: null,
  locations: [],
  avatarUrl: '',
  mode: 'preview'
};

function errorText(err) {
  return err instanceof ApiError ? err.message : DEFAULT_PROFILE_ERROR;
}

function showState(id) {
  ['tutorLoading', 'tutorError', 'tutorContent'].forEach((s) => {
    $(s)?.classList.toggle('hidden', s !== id);
  });
}

function setMode(mode, preserveToast = false) {
  state.mode = mode === 'edit' ? 'edit' : 'preview';
  const isEdit = state.mode === 'edit';
  if (!preserveToast) {
    $('tutorSuccessToast')?.classList.add('hidden');
  }
  $('tutorPreviewSection')?.classList.toggle('hidden', isEdit);
  $('tutorEditForm')?.classList.toggle('hidden', !isEdit);

  const btnPreview = $('btnModePreview');
  const btnEdit = $('btnModeEdit');
  if (btnPreview && btnEdit) {
    btnPreview.setAttribute('aria-selected', String(!isEdit));
    btnEdit.setAttribute('aria-selected', String(isEdit));
    btnPreview.className = `px-3.5 py-2 rounded-lg text-label-md font-medium transition-colors ${!isEdit ? 'bg-white text-on-surface shadow-sm' : 'text-on-surface-variant hover:text-on-surface'}`;
    btnEdit.className = `px-3.5 py-2 rounded-lg text-label-md font-medium transition-colors ${isEdit ? 'bg-white text-on-surface shadow-sm' : 'text-on-surface-variant hover:text-on-surface'}`;
  }
}

function clearFormFeedback() {
  const box = $('tutorFormMessage');
  if (box) {
    box.textContent = '';
    box.classList.add('hidden');
  }
  document.querySelectorAll('#tutorEditForm [data-error-for]').forEach((hint) => {
    hint.textContent = '';
    hint.classList.add('hidden');
  });
  document.querySelectorAll('#tutorEditForm .has-error').forEach((el) => {
    el.classList.remove('has-error');
  });
}

function setFieldError(field, message) {
  const input = $(field);
  const hint = document.querySelector(`#tutorEditForm [data-error-for="${field}"]`);
  if (input) input.classList.toggle('has-error', Boolean(message));
  if (hint) {
    hint.textContent = message || '';
    hint.classList.toggle('hidden', !message);
  }
}

function showFormMessage(message, type = 'error') {
  const box = $('tutorFormMessage');
  if (!box) return;
  box.textContent = message || '';
  box.className = `rounded-lg px-4 py-3 text-sm ${type === 'success'
    ? 'bg-[#059669]/10 border border-[#059669]/20 text-[#047857]'
    : 'bg-[#BA1A1A]/5 border border-[#BA1A1A]/20 text-[#BA1A1A]'}`;
  box.classList.toggle('hidden', !message);
}

function populateProvinceOptions(selectedProvince) {
  const select = $('province');
  if (!select) return;
  const list = [...state.locations];
  if (selectedProvince && !list.includes(selectedProvince)) {
    list.unshift(selectedProvince);
  }
  select.replaceChildren();
  const defaultOpt = document.createElement('option');
  defaultOpt.value = '';
  defaultOpt.textContent = 'Chọn Tỉnh/Thành phố';
  select.appendChild(defaultOpt);

  list.forEach((loc) => {
    const opt = document.createElement('option');
    opt.value = loc;
    opt.textContent = loc;
    select.appendChild(opt);
  });
  select.value = selectedProvince || '';
}

function verificationBadgeClass(status) {
  if (status === 'APPROVED') return 'tv-badge tv-status-approved';
  if (status === 'PENDING') return 'tv-badge tv-status-pending';
  if (status === 'NEEDS_INFO') return 'tv-badge tv-status-needs-info';
  if (status === 'REJECTED') return 'tv-badge tv-status-rejected';
  return 'tv-badge tv-status-unverified';
}

function renderPreview() {
  const p = state.profile;
  if (!p) return;

  renderAvatarNode($('previewAvatar'), p.avatarUrl, p.fullName, API_ORIGIN);
  $('previewFullName').textContent = p.fullName || 'Gia sư KizunaTutor';
  $('previewHeadline').textContent = p.headline || 'Chưa cập nhật tiêu đề giảng dạy';
  $('previewEmail').textContent = p.email || '';

  const badge = $('previewVerificationBadge');
  badge.className = verificationBadgeClass(p.verificationStatus);
  badge.textContent = VERIFICATION_LABEL_MAP[p.verificationStatus] || 'Chưa xác minh';

  const publicLink = $('previewPublicLink');
  if (p.tutorCode) {
    publicLink.href = `/gs/${encodeURIComponent(p.tutorCode)}`;
    publicLink.classList.remove('hidden');
  } else {
    publicLink.classList.add('hidden');
  }

  $('previewHourlyRate').textContent = formatCurrencyVnd(p.hourlyRate);
  $('previewJlpt').textContent = p.highestJlptLevel ? `JLPT ${p.highestJlptLevel}` : 'Chưa cập nhật';
  $('previewExperience').textContent = (p.experienceYears !== null && p.experienceYears !== undefined)
    ? `${p.experienceYears} năm kinh nghiệm`
    : 'Chưa cập nhật';
  $('previewAddress').textContent = p.fullAddress || p.province || 'Chưa cập nhật khu vực';
  $('previewPhone').textContent = p.phone || 'Chưa cập nhật';
  $('previewUpdatedAt').textContent = formatDateTime(p.updatedAt) || 'Chưa ghi nhận';
  $('previewBio').textContent = p.bio || 'Chưa có phần giới thiệu bản thân.';

  const incomplete = isTutorProfileIncomplete(p);
  $('tutorIncompleteText').textContent = INCOMPLETE_TUTOR_BANNER;
  $('tutorIncompleteBanner').classList.toggle('hidden', !incomplete);
}

function fillEditForm() {
  const p = state.profile;
  if (!p) return;

  clearFormFeedback();
  state.avatarUrl = p.avatarUrl || '';
  $('fullName').value = p.fullName || '';
  $('email').value = p.email || '';
  $('phone').value = p.phone || '';
  $('avatarUrl').value = state.avatarUrl;
  $('avatarUrlBox').classList.add('hidden');
  $('btnRemoveAvatar').classList.toggle('hidden', !state.avatarUrl);
  renderAvatarNode($('editAvatarPreview'), state.avatarUrl, p.fullName, API_ORIGIN);

  $('headline').value = p.headline || '';
  $('highestJlptLevel').value = p.highestJlptLevel || '';
  $('experienceYears').value = p.experienceYears ?? '';
  $('hourlyRate').value = p.hourlyRate ?? '';
  populateProvinceOptions(p.province || '');
  $('addressDetail').value = p.addressDetail || '';
  $('bio').value = p.bio || '';

  const updated = formatDateTime(p.updatedAt);
  $('profileUpdatedAt').textContent = updated ? `Cập nhật lần cuối: ${updated}` : '';
}

function syncHeaderIdentity(p) {
  syncLocalUser({ fullName: p.fullName, avatarUrl: p.avatarUrl });
  setTutorIdentity({
    fullName: p.fullName,
    jlptLevel: p.highestJlptLevel || '',
    statusLabel: VERIFICATION_LABEL_MAP[p.verificationStatus] || ''
  });
}

function setAvatar(url) {
  state.avatarUrl = url;
  $('avatarUrl').value = url;
  $('btnRemoveAvatar').classList.toggle('hidden', !url);
  renderAvatarNode($('editAvatarPreview'), url, $('fullName').value || state.profile?.fullName, API_ORIGIN);
}

async function handleAvatarUpload(event) {
  const file = event.target.files?.[0];
  if (!file) return;
  setFieldError('avatarUrl', '');
  const validationError = validateAvatarFile(file);
  if (validationError) {
    event.target.value = '';
    setFieldError('avatarUrl', validationError);
    return;
  }
  try {
    const res = await API.profile.uploadAvatar(file);
    setAvatar(res.avatarUrl || '');
    showFormMessage('Tải ảnh đại diện lên thành công. Hãy nhấn Lưu thay đổi để áp dụng vào hồ sơ.', 'success');
  } catch (err) {
    setFieldError('avatarUrl', errorText(err));
  } finally {
    event.target.value = '';
  }
}

async function handleSubmit(event) {
  event.preventDefault();
  clearFormFeedback();

  const values = {
    fullName: $('fullName').value.trim(),
    phone: $('phone').value.trim(),
    avatarUrl: state.avatarUrl.trim(),
    headline: $('headline').value.trim(),
    bio: $('bio').value.trim(),
    experienceYears: $('experienceYears').value,
    hourlyRate: $('hourlyRate').value,
    highestJlptLevel: $('highestJlptLevel').value,
    province: $('province').value,
    addressDetail: $('addressDetail').value.trim()
  };

  const check = validateTutorForm(values);
  if (!check.valid) {
    Object.entries(check.errors).forEach(([field, msg]) => setFieldError(field, msg));
    showFormMessage('Vui lòng kiểm tra lại các trường đang báo lỗi.', 'error');
    return;
  }

  const submitBtn = $('btnSaveTutorProfile');
  submitBtn.disabled = true;
  $('saveTutorLabel').textContent = 'Đang lưu...';

  try {
    const payload = {
      ...values,
      experienceYears: Number(values.experienceYears),
      hourlyRate: Number(values.hourlyRate)
    };
    const updated = await API.profile.updateTutorProfile(payload);
    state.profile = updated;
    syncHeaderIdentity(updated);
    renderPreview();
    fillEditForm();
    const toast = $('tutorSuccessToast');
    if (toast) {
      toast.textContent = 'Cập nhật hồ sơ giảng dạy thành công.';
      toast.classList.remove('hidden');
    }
    setMode('preview', true);
    showFormMessage('Cập nhật hồ sơ giảng dạy thành công.', 'success');
  } catch (err) {
    if (err instanceof ApiError && Array.isArray(err.fieldErrors) && err.fieldErrors.length) {
      err.fieldErrors.forEach((f) => setFieldError(f.field, f.message));
    }
    showFormMessage(errorText(err), 'error');
  } finally {
    submitBtn.disabled = false;
    $('saveTutorLabel').textContent = 'Lưu thay đổi';
  }
}

async function loadData() {
  showState('tutorLoading');
  $('tutorModeControls')?.classList.add('hidden');
  try {
    const [profile, locations] = await Promise.all([
      API.profile.getTutorProfile(),
      API.tutors.getLocations().catch(() => [])
    ]);
    state.profile = profile;
    state.locations = Array.isArray(locations) ? locations : [];
    syncHeaderIdentity(profile);
    renderPreview();
    fillEditForm();
    $('tutorModeControls')?.classList.remove('hidden');
    showState('tutorContent');
  } catch (err) {
    $('tutorErrorMessage').textContent = errorText(err);
    showState('tutorError');
  }
}

function bindEvents() {
  $('btnRetryLoad')?.addEventListener('click', loadData);
  $('btnModePreview')?.addEventListener('click', () => setMode('preview'));
  $('btnModeEdit')?.addEventListener('click', () => {
    fillEditForm();
    setMode('edit');
  });
  $('btnStartEdit')?.addEventListener('click', () => {
    fillEditForm();
    setMode('edit');
  });
  $('btnBannerEdit')?.addEventListener('click', () => {
    fillEditForm();
    setMode('edit');
  });
  $('btnCancelEdit')?.addEventListener('click', () => setMode('preview'));
  $('btnResetEdit')?.addEventListener('click', () => {
    fillEditForm();
    setMode('preview');
  });
  $('avatarFileInput')?.addEventListener('change', handleAvatarUpload);
  $('btnToggleAvatarUrl')?.addEventListener('click', () => {
    $('avatarUrlBox')?.classList.toggle('hidden');
  });
  $('avatarUrl')?.addEventListener('input', () => {
    setAvatar($('avatarUrl').value.trim());
  });
  $('btnRemoveAvatar')?.addEventListener('click', () => {
    setAvatar('');
  });
  $('tutorEditForm')?.addEventListener('submit', handleSubmit);
}

mountTutorLayout('profile');
mountChangePasswordForm(document.getElementById('changePasswordSection'));
bindEvents();
loadData();
