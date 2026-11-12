/**
 * KizunaTutor - Tutor Discovery Pure Helpers
 * Chứa các hàm dựng view model và HTML thuần cho danh sách gia sư
 * và khung xem nhanh (Preview Panel), dùng 100% dữ liệu thật từ API.
 */

export function escapeHtml(value) {
  if (value === null || value === undefined) return '';
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

export function getTutorProfileUrl(tutor) {
  if (tutor && tutor.tutorCode) {
    return `/gs/${encodeURIComponent(String(tutor.tutorCode))}`;
  }
  const id = tutor ? (tutor.id || tutor.tutorId || '') : '';
  return `/pages/tutor-profile/?id=${encodeURIComponent(String(id))}`;
}

export function getShortDisplayName(fullName) {
  const clean = String(fullName || '').trim();
  if (!clean) return 'gia sư';
  const parts = clean.split(/\s+/);
  if (parts.length <= 2) return clean;
  return parts.slice(-2).join(' ');
}

export function getTutorInitials(fullName) {
  const clean = String(fullName || '').trim();
  if (!clean) return 'GS';
  const parts = clean.split(/\s+/).filter(Boolean);
  if (parts.length === 1) {
    return parts[0].slice(0, 2).toUpperCase();
  }
  return `${parts[0].charAt(0)}${parts[parts.length - 1].charAt(0)}`.toUpperCase();
}

/** fromLowest = true khi gia sư có nhiều mức học phí: hiện "Từ <giá thấp nhất>". */
export function formatHourlyRateInfo(amount, fromLowest = false) {
  const numeric = Number(amount);
  if (!Number.isFinite(numeric) || numeric <= 0) {
    return {
      primary: 'Liên hệ',
      unit: 'Thỏa thuận học phí'
    };
  }
  const formatted = new Intl.NumberFormat('vi-VN').format(Math.round(numeric));
  return {
    primary: `${fromLowest ? 'Từ ' : ''}${formatted}đ`,
    unit: '60 phút/giờ'
  };
}

export function normalizeTutorRecord(item) {
  const safeItem = item || {};
  const verifiedRaw = safeItem.verified !== undefined ? safeItem.verified : safeItem.isVerified;
  const featuredRaw = safeItem.featured !== undefined ? safeItem.featured : safeItem.isFeatured;
  const specialtiesRaw = Array.isArray(safeItem.specialties)
    ? safeItem.specialties.map(s => String(s || '').trim()).filter(Boolean)
    : [];

  return {
    id: safeItem.tutorId || safeItem.id || '',
    tutorCode: safeItem.tutorCode || null,
    fullName: String(safeItem.fullName || '').trim() || 'Gia sư tiếng Nhật',
    avatarUrl: safeItem.avatarUrl ? String(safeItem.avatarUrl).trim() : null,
    verified: Boolean(verifiedRaw),
    isFeatured: Boolean(featuredRaw),
    headline: String(safeItem.postTitle || safeItem.headline || '').trim() || 'Gia sư tiếng Nhật',
    experienceYears: Math.max(0, Number(safeItem.experienceYears) || 0),
    jlptLevel: String(safeItem.highestJlptLevel || 'N1').trim(),
    specialties: specialtiesRaw,
    teachingFormat: String(safeItem.teachingFormat || '').trim() || 'Online 1-1',
    location: String(safeItem.location || '').trim() || 'Việt Nam',
    hourlyRate: Math.max(0, Number(safeItem.hourlyRate) || 0),
    // Bảng học phí theo môn học / cấp độ (bài đăng đã duyệt); nhiều mức thì hiện "Từ <giá thấp nhất>"
    priceItems: Array.isArray(safeItem.priceItems) ? safeItem.priceItems : [],
    rating: safeItem.avgRating != null ? Number(safeItem.avgRating) : 0,
    reviewsCount: Math.max(0, Number(safeItem.totalReviews) || 0),
    studentCount: Math.max(0, Number(safeItem.studentCount) || 0),
    lessonCount: Math.max(0, Number(safeItem.lessonCount) || 0),
    bio: String(safeItem.bio || '').trim(),
    availabilities: Array.isArray(safeItem.availabilities) ? safeItem.availabilities : []
  };
}

export function buildMetricTriplet(tutor) {
  const safeTutor = tutor || {};
  const reviewsCount = Math.max(0, Number(safeTutor.reviewsCount) || 0);
  const rating = Number(safeTutor.rating) || 0;
  const studentCount = Math.max(0, Number(safeTutor.studentCount) || 0);
  const lessonCount = Math.max(0, Number(safeTutor.lessonCount) || 0);

  return [
    reviewsCount > 0 && rating > 0
      ? {
          value: `${rating.toFixed(1)} ★`,
          label: `${reviewsCount} đánh giá`
        }
      : {
          value: 'Mới',
          label: 'Chưa có đánh giá'
        },
    {
      value: String(studentCount),
      label: 'Học viên'
    },
    {
      value: String(lessonCount),
      label: 'Buổi học'
    }
  ];
}

export function summarizeAvailabilities(availabilities, limit = 4) {
  if (!Array.isArray(availabilities) || availabilities.length === 0) {
    return [];
  }
  return availabilities
    .filter(slot => slot && (slot.dayName || slot.dayOfWeek) && slot.startTime && slot.endTime)
    .slice(0, limit)
    .map(slot => ({
      dayName: String(slot.dayName || `Thứ ${slot.dayOfWeek}`).trim(),
      timeRange: `${String(slot.startTime).slice(0, 5)} - ${String(slot.endTime).slice(0, 5)}`
    }));
}

export function parseTimeMinutes(timeStr) {
  if (!timeStr || typeof timeStr !== 'string' || !timeStr.includes(':')) return null;
  const parts = timeStr.trim().split(':');
  const hours = parseInt(parts[0], 10);
  const mins = parseInt(parts[1], 10);
  if (!Number.isFinite(hours) || !Number.isFinite(mins)) return null;
  return hours * 60 + mins;
}

export function matchTutorTimeFilter(tutor, selectedShifts, selectedDays) {
  const shifts = selectedShifts instanceof Set ? selectedShifts : new Set(selectedShifts || []);
  const days = selectedDays instanceof Set ? selectedDays : new Set((selectedDays || []).map(Number));

  if (shifts.size === 0 && days.size === 0) return true;
  if (!tutor || !Array.isArray(tutor.availabilities) || tutor.availabilities.length === 0) return false;

  const shiftWindows = {
    morning: [0, 12 * 60],
    afternoon: [12 * 60, 18 * 60],
    evening: [18 * 60, 24 * 60]
  };

  return tutor.availabilities.some(av => {
    if (!av) return false;
    const dow = Number(av.dayOfWeek);
    if (days.size > 0 && !days.has(dow)) {
      return false;
    }

    if (shifts.size === 0) {
      return true;
    }

    const startMins = parseTimeMinutes(av.startTime);
    const parsedEndMins = parseTimeMinutes(av.endTime);
    if (startMins === null) return false;
    const endMins = parsedEndMins !== null && parsedEndMins > startMins ? parsedEndMins : startMins + 60;

    return Array.from(shifts).some(shiftKey => {
      const range = shiftWindows[shiftKey];
      if (!range) return false;
      const [winStart, winEnd] = range;
      return startMins < winEnd && endMins > winStart;
    });
  });
}

function renderAvatarBlock(tutor, sizeClasses) {
  const safeName = escapeHtml(tutor.fullName);
  if (tutor.avatarUrl) {
    return `
      <img
        src="${escapeHtml(tutor.avatarUrl)}"
        alt="Ảnh đại diện gia sư ${safeName}"
        class="${sizeClasses} rounded-[10px] object-cover border border-[#E5DECB] bg-[#F7F4EC]"
        loading="lazy"
      />
    `;
  }
  return `
    <div class="${sizeClasses} rounded-[10px] bg-gradient-to-br from-[#F7F4EC] to-[#EFE7D6] border border-[#E5DECB] text-[#0F2545] font-bold text-2xl flex items-center justify-center select-none">
      ${escapeHtml(getTutorInitials(tutor.fullName))}
    </div>
  `;
}

export function buildTutorCardHtml(tutor, options = {}) {
  const isSelected = Boolean(options.isSelected);
  const profileUrl = getTutorProfileUrl(tutor);
  const priceInfo = formatHourlyRateInfo(tutor.hourlyRate, (tutor.priceItems || []).length > 1);
  const metrics = buildMetricTriplet(tutor);
  const specialtiesText = tutor.specialties && tutor.specialties.length > 0
    ? tutor.specialties.join(', ')
    : tutor.headline;
  const rawBio = tutor.bio || 'Gia sư chưa cập nhật mô tả chi tiết cho hồ sơ giảng dạy.';
  const isLongBio = rawBio.length > 135;
  const slotCount = Array.isArray(tutor.availabilities) ? tutor.availabilities.length : 0;

  let activityNote = 'Sẵn sàng tiếp nhận yêu cầu học thử 1-1';
  let activityIcon = 'handshake';
  if (slotCount > 0) {
    activityNote = `Đang mở ${slotCount} khung giờ trống trong tuần`;
    activityIcon = 'event_available';
  } else if (tutor.reviewsCount > 0) {
    activityNote = `Đã nhận ${tutor.reviewsCount} đánh giá thực tế từ học viên`;
    activityIcon = 'verified_user';
  }

  const cardHighlightClass = isSelected
    ? 'tutor-card-selected border-[#0F2545] shadow-[0_10px_28px_rgba(15,37,69,0.10)] ring-1 ring-[#0F2545]/15'
    : 'border-[#E5DECB] hover:border-[#0F2545] hover:shadow-[0_10px_28px_rgba(15,37,69,0.08)]';

  return `
    <article
      class="tutor-card group relative bg-white border ${cardHighlightClass} rounded-[12px] p-5 transition-all duration-200 cursor-pointer"
      data-tutor-card
      data-tutor-id="${escapeHtml(tutor.id)}"
      aria-selected="${isSelected ? 'true' : 'false'}"
      tabindex="0"
    >
      <div class="flex flex-col md:flex-row gap-5">
        <!-- Cột trái: Ảnh chân dung gia sư -->
        <div class="shrink-0 flex md:block items-center justify-between gap-4">
          <a href="${profileUrl}" class="block shrink-0" aria-label="Xem hồ sơ ${escapeHtml(tutor.fullName)}">
            ${renderAvatarBlock(tutor, 'w-24 h-24 sm:w-32 sm:h-32 md:w-36 md:h-36')}
          </a>
          <div class="md:hidden text-right">
            <div class="text-[20px] font-bold text-[#0F2545] leading-tight">${escapeHtml(priceInfo.primary)}</div>
            <div class="text-[12px] text-[#475569]">${escapeHtml(priceInfo.unit)}</div>
          </div>
        </div>

        <!-- Cột giữa: Thông tin chuyên môn & giới thiệu -->
        <div class="flex-1 min-w-0 flex flex-col justify-between gap-2.5">
          <div class="space-y-2">
            <!-- Tên + Huy hiệu xác thực + JLPT -->
            <div class="flex flex-wrap items-center gap-2">
              <h3 class="text-[19px] font-bold text-[#0F2545] leading-snug group-hover:text-[#006194] transition-colors">
                <a href="${profileUrl}">${escapeHtml(tutor.fullName)}</a>
              </h3>
              ${tutor.verified ? `
                <span class="inline-flex items-center text-[#006194]" title="Hồ sơ đã xác thực">
                  <span class="material-symbols-outlined filled text-[18px]">verified</span>
                </span>
              ` : ''}
              <span class="px-2 py-0.5 rounded-[5px] bg-[#FAF8F2] border border-[#E5DECB] text-[#0F2545] text-[11px] font-bold">
                JLPT ${escapeHtml(tutor.jlptLevel)}
              </span>
            </div>

            <!-- Nhóm thẻ vai trò thực tế -->
            <div class="flex flex-wrap items-center gap-1.5">
              ${tutor.verified ? `
                <span class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-[6px] bg-[#EBF4FF] text-[#004B73] text-[11px] font-semibold border border-[#CCE5FF]">
                  <span class="material-symbols-outlined text-[13px]">shield</span>
                  <span>Đã xác thực</span>
                </span>
              ` : ''}
              ${tutor.isFeatured ? `
                <span class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-[6px] bg-[#FFE5EC] text-[#9F1239] text-[11px] font-semibold border border-[#FBCFE8]">
                  <span class="material-symbols-outlined filled text-[13px]">auto_awesome</span>
                  <span>Gia sư tiêu biểu</span>
                </span>
              ` : ''}
              <span class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-[6px] bg-[#F7F4EC] text-[#334155] text-[11px] font-medium border border-[#E5DECB]">
                ${escapeHtml(tutor.teachingFormat)}
              </span>
            </div>

            <!-- Dòng metadata thực tế từ DB -->
            <div class="space-y-1 text-[13px] text-[#334155]">
              <div class="flex items-start gap-1.5">
                <span class="material-symbols-outlined text-[16px] text-[#006194] shrink-0 mt-0.5">translate</span>
                <span class="line-clamp-1"><span class="font-semibold text-[#0F2545]">Chuyên môn:</span> ${escapeHtml(specialtiesText)}</span>
              </div>
              <div class="flex items-center gap-1.5">
                <span class="material-symbols-outlined text-[16px] text-[#006194] shrink-0">location_on</span>
                <span class="truncate">${escapeHtml(tutor.location)}</span>
              </div>
            </div>

            <!-- Tiêu đề bài đăng + Mô tả ngắn có thể mở rộng -->
            <div class="pt-0.5 space-y-1">
              <p class="text-[13.5px] font-semibold text-[#0F2545] leading-snug line-clamp-1">
                ${escapeHtml(tutor.headline)}
              </p>
              <p class="tutor-bio-text text-[13px] text-[#475569] leading-relaxed line-clamp-2" data-bio-text>
                ${escapeHtml(rawBio)}
              </p>
              ${isLongBio ? `
                <button
                  type="button"
                  data-action="toggle-bio"
                  aria-expanded="false"
                  class="text-[12.5px] font-semibold text-[#0F2545] underline underline-offset-2 hover:text-[#006194] cursor-pointer"
                >
                  Xem thêm
                </button>
              ` : ''}
            </div>
          </div>

          <!-- Tín hiệu lịch trống / uy tín thực tế -->
          <div class="flex items-center gap-1.5 pt-1 text-[12px] text-[#334155] font-medium">
            <span class="material-symbols-outlined text-[16px] text-[#006194]">${activityIcon}</span>
            <span>${escapeHtml(activityNote)}</span>
          </div>
        </div>

        <!-- Cột phải: Học phí, 3 chỉ số thật và nút hành động -->
        <div class="md:w-56 shrink-0 flex flex-col justify-between gap-3 md:border-l md:border-[#EFECE4] md:pl-5 pt-3 md:pt-0 border-t md:border-t-0 border-[#EFECE4]">
          <!-- Học phí -->
          <div class="hidden md:block">
            <div class="text-[22px] font-bold text-[#0F2545] leading-none tracking-tight">${escapeHtml(priceInfo.primary)}</div>
            <div class="text-[12px] text-[#475569] mt-1">${escapeHtml(priceInfo.unit)}</div>
          </div>

          <!-- 3 chỉ số thực tế từ DB -->
          <div class="grid grid-cols-3 gap-1 py-2.5 border-y border-[#F1EDE4] text-center">
            ${metrics.map(m => `
              <div class="px-0.5">
                <div class="text-[14px] font-bold text-[#0F2545] leading-tight truncate">${escapeHtml(m.value)}</div>
                <div class="text-[11px] text-[#475569] leading-tight mt-0.5 truncate">${escapeHtml(m.label)}</div>
              </div>
            `).join('')}
          </div>

          <!-- Hai nút CTA xếp chồng -->
          <div class="flex flex-col gap-2">
            <button
              type="button"
              data-action="connect"
              data-tutor-id="${escapeHtml(tutor.id)}"
              class="w-full py-2.5 px-3 rounded-[8px] bg-[#006194] hover:bg-[#004b73] text-white font-bold text-[13px] transition-all shadow-sm hover:shadow-md active:scale-[0.98] flex items-center justify-center cursor-pointer"
            >
              <span>Đặt lịch học thử</span>
            </button>
            <a
              href="${profileUrl}"
              class="w-full py-2 px-3 rounded-[8px] bg-white hover:bg-[#F7F4EC] text-[#0F2545] border border-[#CBD5E1] hover:border-[#0F2545] font-semibold text-[13px] transition-all flex items-center justify-center gap-1.5"
            >
              <span>Kết nối với gia sư</span>
            </a>
          </div>
        </div>
      </div>
    </article>
  `;
}

export function computePreviewOffsetPx({
  cardTopRelativeToAside = 0,
  asideTopInViewport = 96,
  asideHeight = 0,
  panelHeight = 0,
  stickyTopPx = 96
} = {}) {
  const maxOffset = Math.max(0, Math.round(asideHeight - panelHeight));
  if (maxOffset === 0) return 0;
  const rawTop = Math.max(0, Math.round(cardTopRelativeToAside));
  const viewportFloor = Math.max(0, Math.round(stickyTopPx - asideTopInViewport));
  const desiredTop = Math.max(rawTop, viewportFloor);
  return Math.min(maxOffset, desiredTop);
}

export function buildPreviewPanelHtml(tutor) {
  if (!tutor) {
    return '';
  }

  const profileUrl = getTutorProfileUrl(tutor);
  const scheduleUrl = `${profileUrl}#weekly-booking-calendar-section`;
  const safeName = escapeHtml(tutor.fullName);

  return `
    <div class="bg-white border border-[#E5DECB] rounded-[12px] p-3.5 shadow-[0_8px_24px_rgba(15,37,69,0.08)] space-y-3">
      <!-- Chỉ hiển thị ảnh đại diện sạch của gia sư từ DB -->
      <div class="w-full aspect-[16/10] rounded-[8px] overflow-hidden border border-[#EFECE4] bg-[#F7F4EC]">
        ${tutor.avatarUrl
          ? `<img src="${escapeHtml(tutor.avatarUrl)}" alt="Ảnh gia sư ${safeName}" class="w-full h-full object-cover" />`
          : `<div class="w-full h-full bg-gradient-to-br from-[#F7F4EC] to-[#EFE7D6] text-[#0F2545] font-bold text-3xl flex items-center justify-center select-none">${escapeHtml(getTutorInitials(tutor.fullName))}</div>`}
      </div>

      <!-- Chỉ 2 nút: Xem lịch & Xem hồ sơ gia sư -->
      <div class="grid grid-cols-2 gap-2.5">
        <a
          href="${scheduleUrl}"
          class="py-2.5 px-3 rounded-[8px] border border-[#E5DECB] bg-white hover:bg-[#F7F4EC] hover:border-[#0F2545] text-[#0F2545] text-[13px] font-semibold text-center transition-colors"
        >
          Xem lịch
        </a>
        <a
          href="${profileUrl}"
          class="py-2.5 px-3 rounded-[8px] border border-[#E5DECB] bg-white hover:bg-[#F7F4EC] hover:border-[#0F2545] text-[#0F2545] text-[13px] font-semibold text-center transition-colors"
        >
          Xem hồ sơ gia sư
        </a>
      </div>
    </div>
  `;
}
