/**
 * KizunaTutor - Tiện ích dùng chung cho phân hệ Hồ sơ người dùng (FE-02).
 * Tập trung chuyển ngữ tiếng Việt, kiểm tra dữ liệu đầu vào và chuẩn hóa GB-15.
 */

export const MAX_AVATAR_SIZE = 5 * 1024 * 1024;
export const AVATAR_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
export const PHONE_PATTERN = /^(0|\+84)[0-9]{9}$/;
export const AVATAR_URL_PATTERN = /^(https?:\/\/\S+|\/api\/profile\/avatar\/[A-Za-z0-9._-]+)?$/;
export const DEFAULT_PROFILE_ERROR = 'Hệ thống đang bận, bạn vui lòng thử lại sau.';
export const INCOMPLETE_TUTOR_BANNER =
  'Hồ sơ giảng dạy của bạn hiện chưa đầy đủ thông tin học phí và tiêu đề. Vui lòng cập nhật để học viên có thể tìm thấy bạn.';

const GPS_PATTERN = /[-+]?\d{1,3}\.\d{4,}/;
const STREET_PATTERN = /(?:^|[\s,])(?:\d+[A-Za-z]?(?:\/\d+)*\s+\S+|(?:số\s*nhà|ngõ|ngách|hẻm|kiệt|căn\s*hộ|tòa\s*nhà|đường)\s+\S+|số\s+\d+|(?<!thành\s)phố\s+\S+)/iu;

export const ROLE_BADGE_MAP = {
  ROLE_LEARNER: 'Học viên',
  ROLE_TUTOR: 'Gia sư'
};

export const USER_STATUS_META = {
  ACTIVE: {
    label: 'Đang hoạt động',
    badgeClass: 'bg-emerald-50 text-emerald-700 border border-emerald-200',
    icon: 'check_circle'
  },
  SUSPENDED: {
    label: 'Tạm khóa',
    badgeClass: 'bg-amber-50 text-amber-800 border border-amber-200',
    icon: 'pause_circle'
  },
  BANNED: {
    label: 'Đã cấm',
    badgeClass: 'bg-rose-50 text-rose-700 border border-rose-200',
    icon: 'block'
  },
  PENDING: {
    label: 'Chờ kích hoạt',
    badgeClass: 'bg-slate-100 text-slate-700 border border-slate-200',
    icon: 'hourglass_empty'
  },
  LOCKED: {
    label: 'Tạm khóa đăng nhập',
    badgeClass: 'bg-amber-50 text-amber-800 border border-amber-200',
    icon: 'lock'
  }
};

export const VERIFICATION_LABEL_MAP = {
  APPROVED: 'Đã xác minh',
  PENDING: 'Chờ thẩm định',
  NEEDS_INFO: 'Cần bổ sung',
  REJECTED: 'Chưa đạt',
  UNVERIFIED: 'Chưa gửi hồ sơ'
};

export function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

export function avatarSrc(url, apiOrigin = '') {
  if (!url) return '';
  if (url.startsWith('/api/') && apiOrigin) {
    return `${apiOrigin}${url}`;
  }
  return url;
}

export function renderAvatarNode(container, url, fullName, apiOrigin = '') {
  if (!container) return;
  container.replaceChildren();
  const initial = (fullName || '?').trim().charAt(0).toUpperCase() || '?';
  if (!url) {
    container.textContent = initial;
    return;
  }
  const img = document.createElement('img');
  img.src = avatarSrc(url, apiOrigin);
  img.alt = 'Ảnh đại diện';
  img.className = 'w-full h-full object-cover';
  img.addEventListener('error', () => { container.textContent = initial; }, { once: true });
  container.appendChild(img);
}

export function formatDateTime(value) {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  return date.toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' });
}

export function formatCurrencyVnd(amount) {
  if (amount === null || amount === undefined || amount === '') return 'Chưa cập nhật';
  const num = Number(amount);
  if (Number.isNaN(num)) return 'Chưa cập nhật';
  return `${num.toLocaleString('vi-VN')} đ/giờ`;
}

export function statusLabel(status) {
  return USER_STATUS_META[status]?.label || 'Không xác định';
}

export function statusBadgeHtml(status) {
  const meta = USER_STATUS_META[status] || {
    label: 'Không xác định',
    badgeClass: 'bg-slate-100 text-slate-700 border border-slate-200',
    icon: 'help'
  };
  return `<span class="inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold ${meta.badgeClass}">
    <span class="material-symbols-outlined text-[14px]">${meta.icon}</span>
    ${escapeHtml(meta.label)}
  </span>`;
}

export function roleLabel(role, roleBadge) {
  if (roleBadge) return roleBadge;
  return ROLE_BADGE_MAP[role] || 'Người dùng';
}

export function containsDetailedAddressOrGps(value) {
  const clean = String(value ?? '').trim();
  if (!clean) return false;
  return GPS_PATTERN.test(clean) || STREET_PATTERN.test(clean);
}

export function isTutorProfileIncomplete(profile) {
  if (!profile) return true;
  const hasHeadline = Boolean(profile.headline && String(profile.headline).trim());
  const hasRate = profile.hourlyRate !== null && profile.hourlyRate !== undefined && Number(profile.hourlyRate) > 0;
  return !hasHeadline || !hasRate;
}

export function validateAvatarFile(file) {
  if (!file) return 'Vui lòng chọn tệp ảnh hợp lệ.';
  if (!AVATAR_TYPES.includes(file.type)) {
    return 'Chỉ chấp nhận tệp ảnh định dạng JPG, PNG hoặc WEBP.';
  }
  if (file.size > MAX_AVATAR_SIZE) {
    return 'Dung lượng ảnh không được vượt quá 5MB.';
  }
  return '';
}

export function validateTutorForm(values) {
  const errors = {};
  const fullName = String(values.fullName ?? '').trim();
  const phone = String(values.phone ?? '').trim();
  const avatarUrl = String(values.avatarUrl ?? '').trim();
  const headline = String(values.headline ?? '').trim();
  const bio = String(values.bio ?? '').trim();
  const experienceYears = Number(values.experienceYears);
  const hourlyRate = Number(values.hourlyRate);
  const highestJlptLevel = String(values.highestJlptLevel ?? '').trim();
  const province = String(values.province ?? '').trim();
  const addressDetail = String(values.addressDetail ?? '').trim();

  if (!fullName) errors.fullName = 'Vui lòng nhập họ và tên.';
  else if (fullName.length > 100) errors.fullName = 'Họ và tên tối đa 100 ký tự.';

  if (phone && !PHONE_PATTERN.test(phone)) {
    errors.phone = 'Số điện thoại không đúng định dạng 10 chữ số.';
  }

  if (avatarUrl && !AVATAR_URL_PATTERN.test(avatarUrl)) {
    errors.avatarUrl = 'Đường dẫn ảnh đại diện phải bắt đầu bằng http:// hoặc https://.';
  }

  if (!headline) errors.headline = 'Vui lòng nhập tiêu đề giảng dạy.';
  else if (headline.length > 200) errors.headline = 'Tiêu đề giảng dạy tối đa 200 ký tự.';

  if (bio.length > 3000) errors.bio = 'Giới thiệu bản thân tối đa 3000 ký tự.';

  if (values.experienceYears === '' || values.experienceYears === null || values.experienceYears === undefined
      || Number.isNaN(experienceYears) || !Number.isInteger(experienceYears)
      || experienceYears < 0 || experienceYears > 50) {
    errors.experienceYears = 'Số năm kinh nghiệm phải từ 0 đến 50 năm.';
  }

  if (values.hourlyRate === '' || values.hourlyRate === null || values.hourlyRate === undefined
      || Number.isNaN(hourlyRate) || hourlyRate < 0 || hourlyRate > 50000000) {
    errors.hourlyRate = 'Học phí phải từ 0 đến 50.000.000 đồng.';
  }

  if (!['N5', 'N4', 'N3', 'N2', 'N1'].includes(highestJlptLevel)) {
    errors.highestJlptLevel = 'Vui lòng chọn trình độ JLPT từ N5 đến N1.';
  }

  if (!province) errors.province = 'Vui lòng chọn Tỉnh/Thành phố.';

  if (addressDetail.length > 150) {
    errors.addressDetail = 'Quận/Huyện hoặc khu vực tối đa 150 ký tự.';
  } else if (containsDetailedAddressOrGps(addressDetail)) {
    errors.addressDetail = 'Chỉ ghi Quận/Huyện hoặc khu vực chung, không ghi số nhà, tên đường hay tọa độ.';
  }

  return { valid: Object.keys(errors).length === 0, errors };
}

export function validateStatusUpdate({ status, reason }) {
  if (!['ACTIVE', 'SUSPENDED', 'BANNED'].includes(status)) {
    return { valid: false, error: 'Vui lòng chọn trạng thái tài khoản hợp lệ.' };
  }
  const cleanReason = String(reason ?? '').trim();
  if (!cleanReason || cleanReason.length < 5 || cleanReason.length > 500) {
    return { valid: false, error: 'Lý do thay đổi trạng thái phải từ 5 đến 500 ký tự.' };
  }
  return { valid: true, error: '' };
}

export function syncLocalUser({ fullName, avatarUrl }) {
  try {
    const raw = localStorage.getItem('kizuna_user');
    if (!raw) return;
    const user = JSON.parse(raw) || {};
    if (fullName !== undefined) user.fullName = fullName;
    if (avatarUrl !== undefined) user.avatarUrl = avatarUrl;
    localStorage.setItem('kizuna_user', JSON.stringify(user));
  } catch (e) { /* bỏ qua khi trình duyệt chặn localStorage */ }
}
