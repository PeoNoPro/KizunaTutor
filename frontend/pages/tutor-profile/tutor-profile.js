/**
 * KizunaTutor - Tutor Profile Detail Page Logic (UC-31)
 * Hỗ trợ URL ngắn /gs/:tutorCode (ví dụ: /gs/10001), ?code=10001 hoặc ?id=<UUID>.
 * Lấy thông tin chi tiết từ Database Supabase qua REST API.
 */

import { API } from '../../js/api.js';
import { formatVND } from '../../js/main.js';
import { containsDetailedAddressOrGps, escapeHtml } from '../../js/profile-utils.js';
import { isLoggedIn, friendlyError, buildBookingQuery } from '../../js/booking-form.js';
import {
  createBookingPickers, WEEKDAY_COLUMNS, TRIAL_DURATION_MINUTES, toHHMM, fmtDM, fmtDMY, todayIsoVN
} from '../../js/booking-pickers.js';

function sanitizeDisplayLocation(rawLocation) {
  if (!rawLocation) return 'Hà Nội, Việt Nam';
  const text = String(rawLocation).trim();
  if (text.includes(' - ')) {
    const [province, detail] = text.split(/\s+-\s+/, 2);
    if (detail && !containsDetailedAddressOrGps(detail)) {
      return `${province.trim()} - ${detail.trim()}`;
    }
    return province.trim() || 'Hà Nội, Việt Nam';
  }
  return containsDetailedAddressOrGps(text) ? 'Hà Nội, Việt Nam' : text;
}

/** Bảng học phí theo môn học / cấp độ từ bài đăng đã duyệt (rỗng nếu gia sư chỉ có một mức giá chung). */
function priceItemsOf(tutor) {
  return Array.isArray(tutor?.priceItems) ? tutor.priceItems : [];
}


function getInitials(name) {
  if (!name) return 'HV';
  const parts = name.trim().split(/\s+/);
  if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

function getAvatarStyle(name) {
  const styles = [
    { bg: 'bg-sky-100', text: 'text-sky-800' },
    { bg: 'bg-rose-100', text: 'text-rose-800' },
    { bg: 'bg-amber-100', text: 'text-amber-800' },
    { bg: 'bg-emerald-100', text: 'text-emerald-800' },
    { bg: 'bg-purple-100', text: 'text-purple-800' }
  ];
  let hash = 0;
  for (let i = 0; i < (name || '').length; i++) {
    hash = (hash + name.charCodeAt(i)) % styles.length;
  }
  return styles[hash];
}

document.addEventListener('DOMContentLoaded', async () => {
  // Trích xuất mã gia sư (tutor_code) từ pathname /gs/:code hoặc query param
  const pathname = window.location.pathname;
  const gsMatch = pathname.match(/\/gs\/(\d+)/);
  const urlParams = new URLSearchParams(window.location.search);
  const tutorCode = gsMatch ? gsMatch[1] : urlParams.get('code');
  const tutorId = urlParams.get('id');

  if (!tutorCode && !tutorId) {
    window.location.href = '/pages/tutor-discovery/';
    return;
  }

  const container = document.getElementById('profile-content');
  if (!container) return;

  try {
    let tutor = null;

    if (tutorCode) {
      tutor = await API.tutors.getByCode(tutorCode);
    } else if (tutorId) {
      tutor = await API.tutors.getDetail(tutorId);
    }

    if (!tutor) {
      container.innerHTML = `
        <div class="p-12 text-center bg-white rounded-2xl border border-[#E5E7EB] space-y-4 max-w-lg mx-auto my-12 shadow-sm">
          <div class="w-16 h-16 rounded-full bg-amber-50 text-amber-600 flex items-center justify-center mx-auto">
            <span class="material-symbols-outlined text-[32px]">person_off</span>
          </div>
          <h3 class="text-xl font-bold text-[#0F2545]">Không tìm thấy hồ sơ gia sư</h3>
          <p class="text-sm text-on-surface-variant">Mã gia sư yêu cầu không tồn tại hoặc hồ sơ chưa được công khai.</p>
          <a href="/pages/tutor-discovery/" class="inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl bg-primary hover:bg-primary-container text-white text-sm font-semibold transition-colors">
            Khám phá danh sách gia sư
          </a>
        </div>
      `;
      return;
    }

    // Cập nhật tiêu đề trang
    document.title = `${tutor.fullName} - Chi tiết gia sư tiếng Nhật | KizunaTutor`;

    const reviewList = tutor.reviews || [];
    const totalReviews = reviewList.length > 0 ? reviewList.length : (tutor.totalReviews || 0);
    const avgRating = tutor.avgRating != null ? Number(tutor.avgRating).toFixed(1) : '5.0';

    function renderSingleReview(r, index) {
      const style = getAvatarStyle(r.learnerName);
      const initials = getInitials(r.learnerName);
      const isExtra = index >= 3;
      return `
        <div class="p-5 rounded-2xl border border-[#E5E7EB] bg-white space-y-3 hover:shadow-xs transition-shadow review-item ${isExtra ? 'extra-review hidden' : ''}" data-has-comment="${r.comment ? 'true' : 'false'}">
          <!-- Top: Thông tin học viên & Đánh giá sao -->
          <div class="flex items-start justify-between gap-3">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-full flex items-center justify-center text-xs font-bold ${style.bg} ${style.text} flex-shrink-0">
                ${initials}
              </div>
              <div>
                <div class="flex items-center gap-2">
                  <h4 class="text-sm font-bold text-[#0F2545]">${r.learnerName}</h4>
                  <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 text-[10px] font-bold border border-emerald-200/60">
                    <span class="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
                    Đã học
                  </span>
                </div>
                <p class="text-xs text-slate-500 font-medium">${r.courseName || 'Học viên lớp Kaiwa 1-1'}</p>
              </div>
            </div>

            <div class="text-right flex-shrink-0 space-y-1">
              <div class="flex items-center justify-end text-amber-500 text-xs tracking-tighter">
                ★★★★★
              </div>
              <div class="text-[11px] text-slate-400 font-medium">
                ${r.createdAt || 'Gần đây'}
              </div>
            </div>
          </div>

          <!-- Lời nhận xét -->
          ${r.comment ? `
            <p class="text-xs md:text-sm text-[#2D3748] leading-relaxed italic bg-[#F3F4F6] p-3.5 rounded-xl border border-[#F3F4F6]">
              “${r.comment}”
            </p>
          ` : ''}
        </div>
      `;
    }

    const reviewsCardsHtml = reviewList.length > 0
      ? reviewList.map((r, i) => renderSingleReview(r, i)).join('')
      : `
        <div class="p-8 text-center bg-white rounded-2xl border border-[#E5E7EB] space-y-2">
          <p class="text-xs text-slate-500">Chưa có đánh giá nào từ học viên. Hãy là người đầu tiên đặt lịch học thử!</p>
        </div>
      `;

    // Hàm render khối Môn dạy & học phí (theo môn học, thời lượng, học phí thực tế của gia sư)
    function renderSubjectsAndPricingHtml(tutor) {
      // Chỉ dùng dữ liệu thật: bảng học phí theo môn / cấp độ trong bài đăng đã duyệt (tutor_posts.price_items).
      // Không tự sinh danh sách lớp hay học phí theo thời lượng.
      const safe = (text) => String(text == null ? '' : text)
        .replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' })[c]);
      // Giá theo buổi (bảng học phí có thời lượng); bài cũ chỉ có một mức giá chung thì hiện theo giờ
      const priceHtml = (rate, unit) => Number(rate) > 0
        ? `<span class="font-bold text-[#006194] text-sm sm:text-base whitespace-nowrap">${formatVND(Number(rate))} <span class="text-xs font-normal text-slate-400">/${unit}</span></span>`
        : `<span class="inline-flex items-center px-2.5 py-0.5 rounded-md bg-slate-100 text-slate-600 text-xs font-medium">Thỏa thuận</span>`;

      // Nhóm theo loại môn (mã level của bảng học phí: JLPT_N5..JLPT_N1, KAIWA, BUSINESS, EJU)
      const GROUPS = [
        { key: 'JLPT', title: 'Luyện thi JLPT', icon: 'school', match: (lv) => lv.startsWith('JLPT_') },
        { key: 'KAIWA', title: 'Tiếng Nhật giao tiếp (Kaiwa)', icon: 'forum', match: (lv) => lv === 'KAIWA' },
        { key: 'BUSINESS', title: 'Tiếng Nhật thương mại', icon: 'work', match: (lv) => lv === 'BUSINESS' },
        { key: 'EJU', title: 'Luyện thi EJU', icon: 'menu_book', match: (lv) => lv === 'EJU' }
      ];
      const items = priceItemsOf(tutor).filter(p => p && p.level);
      const groupBlock = (title, icon, rows) => `
          <div class="rounded-xl border border-slate-200 overflow-hidden">
            <div class="px-4 py-2.5 bg-sky-50/70 border-b border-slate-100 flex items-center gap-2 text-sm font-bold text-[#006194]">
              <span class="material-symbols-outlined text-[18px]">${icon}</span>${title}
            </div>
            <div class="divide-y divide-slate-100 bg-white">${rows}</div>
          </div>`;
      // minutes: thời lượng một buổi gia sư đặt cho môn (null với dữ liệu cũ)
      const row = (label, rate, minutes = null, unit = 'giờ') => `
              <div class="flex items-center justify-between p-3.5 px-4 gap-4 hover:bg-slate-50/50 transition-colors">
                <span class="text-xs sm:text-sm font-medium text-slate-800 min-w-0">${label}</span>
                <span class="flex items-center gap-3 sm:gap-5 shrink-0">
                  ${minutes ? `<span class="text-xs sm:text-sm text-slate-500 whitespace-nowrap">${minutes} phút/buổi</span>` : ''}
                  ${priceHtml(rate, unit)}
                </span>
              </div>`;

      const subjectBlocks = [];
      if (items.length) {
        GROUPS.forEach(g => {
          const groupItems = items.filter(p => g.match(String(p.level).toUpperCase()));
          if (groupItems.length) {
            subjectBlocks.push(groupBlock(g.title, g.icon,
              groupItems.map(p => p.pricePerSession != null
                ? row(safe(p.levelLabel || p.level), p.pricePerSession, p.sessionMinutes, 'buổi')
                : row(safe(p.levelLabel || p.level), p.hourlyRate)).join('')));
          }
        });
      } else {
        // Bài đăng chưa có bảng học phí theo môn: một mức học phí chung cho các môn trong phần chuyên môn
        const specialtiesText = (tutor.specialties || []).map(s => String(s).replace(/\s+/g, ' ').trim()).filter(Boolean).join(', ');
        subjectBlocks.push(groupBlock('Học phí chung', 'payments',
          row(specialtiesText ? safe(specialtiesText) : 'Các môn gia sư nhận dạy', tutor.hourlyRate)));
      }

      // Hình thức dạy (teaching format)
      // Lấy theo DB: tutor_posts.teaching_format của bài đăng đang hiển thị (ONLINE | OFFLINE | BOTH).
      // Không đoán từ địa chỉ; chưa khai báo hình thức dạy thì không hiện nhãn nào.
      const formatCode = String(tutor.teachingFormatCode || '').toUpperCase();
      const hasOnline = formatCode === 'ONLINE' || formatCode === 'BOTH';
      const hasOffline = formatCode === 'OFFLINE' || formatCode === 'BOTH';

      const badges = [];
      if (hasOffline) {
        badges.push(`
          <span class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-100 text-slate-700 text-xs font-medium">
            <span class="text-emerald-600 font-bold">✓</span> Tại nhà học sinh
          </span>
        `);
      }
      if (hasOnline) {
        badges.push(`
          <span class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-100 text-slate-700 text-xs font-medium">
            <span class="text-emerald-600 font-bold">✓</span> Online
          </span>
        `);
      }

      return `
        <!-- Môn dạy & học phí -->
        <div class="bg-white border border-[#E5E7EB] rounded-2xl p-6 md:p-8 shadow-sm space-y-5">
          <h3 class="text-lg font-bold text-[#0F2545] flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">menu_book</span>
            Môn dạy & học phí
          </h3>

          <!-- Danh sách các nhóm môn học -->
          <div class="space-y-4">
            ${subjectBlocks.join('')}
          </div>

          <!-- Hình thức giảng dạy (Badges with Checkmarks) -->
          <div class="flex items-center gap-2 pt-1 flex-wrap">
            ${badges.join('')}
          </div>
        </div>
      `;
    }

    const tutorStyle = getAvatarStyle(tutor.fullName);
    const tutorInitials = getInitials(tutor.fullName);
    const tutorAvatarImgHtml = tutor.avatarUrl
      ? `<img class="w-28 h-28 rounded-2xl object-cover border-2 border-emerald-500 shadow-md"
              src="${tutor.avatarUrl}"
              alt="${tutor.fullName}"
              onerror="this.style.display='none'; this.nextElementSibling.style.display='flex';">
         <div class="w-28 h-28 rounded-2xl ${tutorStyle.bg} ${tutorStyle.text} font-bold text-3xl hidden items-center justify-center border-2 border-emerald-500 shadow-md">
           ${tutorInitials}
         </div>`
      : `<div class="w-28 h-28 rounded-2xl ${tutorStyle.bg} ${tutorStyle.text} font-bold text-3xl flex items-center justify-center border-2 border-emerald-500 shadow-md">
           ${tutorInitials}
         </div>`;

    // Render nội dung hồ sơ
    container.innerHTML = `
      <!-- Breadcrumb -->
      <nav class="flex items-center gap-2 text-xs text-on-surface-variant mb-6">
        <a href="/pages/home/" class="hover:text-primary transition-colors">Trang chủ</a>
        <span class="material-symbols-outlined text-[14px]">chevron_right</span>
        <a href="/pages/tutor-discovery/" class="hover:text-primary transition-colors">Tìm gia sư</a>
        <span class="material-symbols-outlined text-[14px]">chevron_right</span>
        <span class="font-semibold text-on-surface truncate">${tutor.fullName}</span>
      </nav>

      <!-- Layout 2 Cột -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
        
        <!-- Cột Trái: Chi tiết hồ sơ & Giới thiệu -->
        <div class="lg:col-span-2 space-y-6">
          
          <!-- Profile Header Card -->
          <div class="bg-white border border-[#E5E7EB] rounded-2xl p-6 md:p-8 shadow-sm space-y-6">
            <div class="flex flex-col sm:flex-row items-start sm:items-center gap-6">
              <div class="relative shrink-0">
                ${tutorAvatarImgHtml}
                <span class="absolute -bottom-2 -right-2 px-2 py-0.5 rounded-full bg-emerald-500 text-white text-[10px] font-bold shadow-sm flex items-center gap-1">
                  <span class="w-1.5 h-1.5 rounded-full bg-white animate-pulse"></span> Trực tuyến
                </span>
              </div>

              <div class="flex-1 space-y-2">
                <h1 class="text-2xl md:text-3xl font-bold text-[#0F2545]">${tutor.fullName}</h1>

                <p class="text-sm font-medium text-primary">${tutor.headline || 'Gia sư tiếng Nhật'}</p>

                <div class="flex flex-wrap items-center gap-4 text-xs pt-1 text-on-surface-variant">
                  ${totalReviews > 0 ? `
                  <div class="flex items-center gap-1 text-amber-500 font-bold">
                    <span class="material-symbols-outlined text-[18px]" style="font-variation-settings: 'FILL' 1;">star</span>
                    <span class="text-sm">${avgRating}</span>
                    <span class="text-on-surface-variant font-normal">(${totalReviews} đánh giá)</span>
                  </div>
                  ` : `
                  <div class="flex items-center gap-1 text-slate-500 font-medium">
                    <span class="material-symbols-outlined text-[16px] text-slate-400">rate_review</span>
                    <span>Chưa có đánh giá</span>
                  </div>
                  `}
                  <span class="text-[#E5E7EB]">|</span>
                  <div class="flex items-center gap-1">
                    <span class="material-symbols-outlined text-[16px] text-primary">school</span>
                    <span>${tutor.experienceYears || 0} năm kinh nghiệm</span>
                  </div>
                  <span class="text-[#E5E7EB]">|</span>
                  <div class="flex items-center gap-1">
                    <span class="material-symbols-outlined text-[16px] text-primary">location_on</span>
                    <span>${escapeHtml(sanitizeDisplayLocation(tutor.location))}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- Tags Chuyên môn & Trình độ -->
            <div class="pt-4 border-t border-[#E5E7EB] flex flex-wrap gap-2 items-center">
              <span class="text-xs font-bold text-[#0F2545] mr-2">Chuyên môn:</span>
              <span class="px-3 py-1 rounded-lg bg-sky-50 text-sky-800 text-xs font-bold border border-sky-200">Trình độ JLPT ${tutor.highestJlptLevel || 'N1'}</span>
              ${(tutor.specialties || []).map(s => `
                <span class="px-3 py-1 rounded-lg bg-slate-100 text-slate-700 text-xs font-medium border border-slate-200">${s}</span>
              `).join('')}
            </div>
          </div>

          <!-- Bio / Giới thiệu chi tiết -->
          <div class="bg-white border border-[#E5E7EB] rounded-2xl p-6 md:p-8 shadow-sm space-y-4">
            <h3 class="text-lg font-bold text-[#0F2545] flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">person</span>
              Giới thiệu bản thân
            </h3>
            <div class="text-sm text-slate-700 leading-relaxed whitespace-pre-line">
              ${tutor.bio || 'Chào mừng các bạn đến với lớp học tiếng Nhật! Với kinh nghiệm giảng dạy và tình yêu dành cho ngôn ngữ cùng văn hóa xứ sở hoa anh đào, tôi luôn đồng hành sát sao cùng từng học viên để chinh phục mục tiêu ngôn ngữ vững chắc nhất.'}
            </div>
          </div>

          <!-- Môn dạy & học phí (theo mẫu) -->
          ${renderSubjectsAndPricingHtml(tutor)}

          <!-- Lịch rảnh hằng tuần (chỉ hiển thị). Muốn kết nối: bấm "Kết nối với gia sư" để mở form chọn ca. -->
          <div id="weekly-booking-calendar-section" class="bg-white border border-[#E5E7EB] rounded-2xl p-6 md:p-8 shadow-sm space-y-5 scroll-mt-28 transition-all duration-300">

            <div class="flex items-start justify-between flex-wrap gap-3 pb-3 border-b border-slate-100">
              <div class="space-y-1">
                <h3 class="text-lg font-bold text-[#0F2545] flex items-center gap-2">
                  <span class="material-symbols-outlined text-primary">calendar_month</span>
                  Lịch rảnh giảng dạy
                </h3>
                <p class="text-xs text-on-surface-variant leading-relaxed">
                  Giờ còn trống trong 2 tuần tới. Bấm vào một giờ để đặt học thử 30 phút. Muốn học lâu dài theo lịch cố định, bấm "Kết nối với gia sư".
                </p>
              </div>
              <div class="flex items-center gap-1.5">
                <button type="button" id="week-prev-btn" class="w-9 h-9 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 flex items-center justify-center text-slate-700 disabled:text-slate-300 disabled:hover:bg-white disabled:cursor-not-allowed cursor-pointer transition-all" aria-label="Tuần trước">
                  <span class="material-symbols-outlined text-[20px]">chevron_left</span>
                </button>
                <div class="min-w-[150px] text-center">
                  <div id="week-range-label" class="text-sm font-bold text-[#0F2545]">&nbsp;</div>
                </div>
                <button type="button" id="week-next-btn" class="w-9 h-9 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 flex items-center justify-center text-slate-700 disabled:text-slate-300 disabled:hover:bg-white disabled:cursor-not-allowed cursor-pointer transition-all" aria-label="Tuần sau">
                  <span class="material-symbols-outlined text-[20px]">chevron_right</span>
                </button>
              </div>
            </div>

            <!-- Lưới 7 cột Thứ hai -> Chủ nhật -->
            <div class="overflow-x-auto pb-2 -mx-2 px-2">
              <div id="weekly-calendar-grid" class="grid grid-cols-7 gap-1.5 sm:gap-2 min-w-[620px]">
                <!-- 7 cột được render động qua JS -->
              </div>
            </div>

            <!-- Nút Xem toàn bộ lịch trình / Thu gọn -->
            <div id="weekly-expand-wrapper" class="hidden pt-3 text-center border-t border-slate-100">
              <button type="button" id="btn-toggle-weekly-slots" class="inline-flex items-center gap-1.5 py-2.5 px-5 rounded-xl border border-slate-300 bg-white hover:bg-slate-50 text-xs sm:text-sm font-bold text-slate-700 transition-all shadow-2xs cursor-pointer">
                <span id="toggle-weekly-slots-text">Xem toàn bộ lịch trình</span>
                <span id="toggle-weekly-slots-icon" class="material-symbols-outlined text-[18px]">expand_more</span>
              </button>
            </div>

          </div>

          <!-- Đánh giá từ học viên (UC-31) -->
          <div class="bg-white border border-[#E5E7EB] rounded-2xl p-6 md:p-8 shadow-sm space-y-6">
            <!-- Header khối đánh giá -->
            <div class="flex items-center justify-between flex-wrap gap-2 pb-1">
              <h3 class="text-lg font-bold text-[#0F2545] flex items-center gap-2">
                <span class="text-amber-500 text-xl font-black">★</span>
                <span>Đánh giá từ học viên (${totalReviews})</span>
              </h3>
            </div>

            <!-- Hộp tổng quan điểm đánh giá & Biểu đồ sao -->
            <div class="bg-[#F9FAFB] border border-[#E5E7EB] rounded-2xl p-6 grid grid-cols-1 md:grid-cols-12 gap-6 items-center">
              <!-- Cột trái: Điểm số trung bình -->
              <div class="md:col-span-4 flex flex-col items-center justify-center text-center space-y-2 border-b md:border-b-0 md:border-r border-[#E5E7EB] pb-4 md:pb-0 md:pr-4">
                <div class="text-4xl md:text-5xl font-black text-[#0F2545] tracking-tight">
                  ${avgRating}
                </div>
                <div class="flex items-center gap-1 text-amber-500 text-lg">
                  ★★★★★
                </div>
                <p class="text-xs text-[#5C5F62] font-medium leading-relaxed">
                  100% học viên hài lòng trên ${totalReviews} lượt đánh giá
                </p>
                <div class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200/60 text-[11px] font-semibold">
                  <span class="material-symbols-outlined text-[13px] text-emerald-600">shield_with_heart</span>
                  <span>Gia sư tiêu biểu - 0 phản hồi tiêu cực</span>
                </div>
              </div>

              <!-- Cột phải: Phân bổ số sao (5 sao -> 1 sao) -->
              <div class="md:col-span-8 space-y-2 pl-0 md:pl-2">
                <div class="flex items-center gap-3 text-xs">
                  <span class="w-8 font-bold text-slate-700 flex items-center gap-0.5">5 <span class="text-amber-500">★</span></span>
                  <div class="flex-1 h-2 rounded-full bg-slate-200 overflow-hidden">
                    <div class="h-full bg-amber-500 rounded-full" style="width: 100%;"></div>
                  </div>
                  <span class="w-6 text-right font-semibold text-slate-600">${totalReviews}</span>
                </div>
                <div class="flex items-center gap-3 text-xs opacity-60">
                  <span class="w-8 font-semibold text-slate-600 flex items-center gap-0.5">4 <span class="text-slate-400">★</span></span>
                  <div class="flex-1 h-2 rounded-full bg-slate-200 overflow-hidden">
                    <div class="h-full bg-amber-500 rounded-full" style="width: 0%;"></div>
                  </div>
                  <span class="w-6 text-right font-medium text-slate-400">0</span>
                </div>
                <div class="flex items-center gap-3 text-xs opacity-60">
                  <span class="w-8 font-semibold text-slate-600 flex items-center gap-0.5">3 <span class="text-slate-400">★</span></span>
                  <div class="flex-1 h-2 rounded-full bg-slate-200 overflow-hidden">
                    <div class="h-full bg-amber-500 rounded-full" style="width: 0%;"></div>
                  </div>
                  <span class="w-6 text-right font-medium text-slate-400">0</span>
                </div>
                <div class="flex items-center gap-3 text-xs opacity-60">
                  <span class="w-8 font-semibold text-slate-600 flex items-center gap-0.5">2 <span class="text-slate-400">★</span></span>
                  <div class="flex-1 h-2 rounded-full bg-slate-200 overflow-hidden">
                    <div class="h-full bg-amber-500 rounded-full" style="width: 0%;"></div>
                  </div>
                  <span class="w-6 text-right font-medium text-slate-400">0</span>
                </div>
                <div class="flex items-center gap-3 text-xs opacity-60">
                  <span class="w-8 font-semibold text-slate-600 flex items-center gap-0.5">1 <span class="text-slate-400">★</span></span>
                  <div class="flex-1 h-2 rounded-full bg-slate-200 overflow-hidden">
                    <div class="h-full bg-amber-500 rounded-full" style="width: 0%;"></div>
                  </div>
                  <span class="w-6 text-right font-medium text-slate-400">0</span>
                </div>
              </div>
            </div>

            <!-- Danh sách các nhận xét -->
            <div class="space-y-3.5" id="reviews-list-container">
              ${reviewsCardsHtml}
            </div>

            <!-- Nút Xem toàn bộ đánh giá (Tự động ẩn nếu <= 3, chỉ hiện khi > 3) -->
            ${totalReviews > 3 ? `
              <div class="pt-1 text-center">
                <button type="button" id="toggle-more-reviews-btn" class="w-full py-3 px-4 rounded-xl bg-white border border-[#E5E7EB] hover:bg-[#F9FAFB] text-xs font-semibold text-[#0F2545] transition-colors flex items-center justify-center gap-1.5 shadow-2xs">
                  <span id="toggle-more-text">Xem toàn bộ ${totalReviews} đánh giá</span>
                  <span class="material-symbols-outlined text-[16px]" id="toggle-more-icon">expand_more</span>
                </button>
              </div>
            ` : ''}
          </div>

        </div>

        <!-- Cột Phải: Thẻ Đặt lịch & Kết nối -->
        <div class="lg:col-span-1 space-y-6 lg:sticky lg:top-28">
          <div class="bg-white border-2 border-primary/20 rounded-2xl p-6 shadow-lg space-y-6">
            <!-- Hành động chính (CTA) -->
            <div class="space-y-2.5">
              <button type="button" id="open-booking-btn" class="w-full h-12 px-4 rounded-xl bg-[#006194] hover:bg-[#004b73] border border-[#004b73] text-white font-semibold text-[15px] transition-all active:scale-[0.98] flex items-center justify-center cursor-pointer">
                Đặt lịch học thử 1-1
              </button>
              <button type="button" id="open-connect-btn" class="w-full h-12 px-4 rounded-xl bg-white hover:bg-slate-50 ring-1 ring-inset ring-slate-200 hover:ring-slate-300 text-[#0F2545] font-semibold text-[15px] transition-all active:scale-[0.98] flex items-center justify-center cursor-pointer">
                Kết nối với gia sư
              </button>
            </div>

            <!-- Cam kết đảm bảo quyền lợi -->
            <div class="pt-4 border-t border-[#E5E7EB] space-y-3 text-xs text-on-surface-variant">
              <div class="flex items-center gap-2.5">
                <span class="material-symbols-outlined text-emerald-600 text-[18px]">verified_user</span>
                <span>Hồ sơ, bằng cấp JLPT đã được kiểm tra nghiêm ngặt</span>
              </div>
            </div>
          </div>

          <div class="text-center">
            <a href="/pages/tutor-discovery/" class="inline-flex items-center gap-1.5 text-xs text-on-surface-variant hover:text-primary font-semibold transition-colors">
              <span class="material-symbols-outlined text-[16px]">arrow_back</span>
              Quay lại danh sách tìm kiếm gia sư
            </a>
          </div>
        </div>

      </div>
    `;


    // Xử lý nút Xem thêm đánh giá (chỉ khi có trên 3 đánh giá)
    const toggleBtn = document.getElementById('toggle-more-reviews-btn');
    if (toggleBtn) {
      let isExpanded = false;
      toggleBtn.addEventListener('click', () => {
        isExpanded = !isExpanded;
        const extraCards = document.querySelectorAll('.extra-review');
        extraCards.forEach(card => card.classList.toggle('hidden', !isExpanded));

        const textEl = document.getElementById('toggle-more-text');
        const iconEl = document.getElementById('toggle-more-icon');
        if (textEl) {
          textEl.textContent = isExpanded ? 'Thu gọn bớt đánh giá' : `Xem toàn bộ ${totalReviews} đánh giá`;
        }
        if (iconEl) {
          iconEl.textContent = isExpanded ? 'expand_less' : 'expand_more';
        }
      });
    }


    // ======================================================================
    // Hai cách bắt đầu học với gia sư:
    //  1) Kết nối trực tiếp: chọn các ca hằng tuần trong lịch rảnh → gia sư đồng ý/từ chối.
    //  2) Học thử 30 phút: học viên tự chọn ngày giờ bất kỳ → gia sư đồng ý/từ chối.
    // ======================================================================
    const MAX_PREVIEW_ITEMS = 8;
    let isWeeklySlotsExpanded = false;

    /** Chạy onReady khi đã đăng nhập; nếu chưa thì mở hộp đăng nhập (hoặc chuyển trang đăng nhập). */
    function requireLogin(onReady, redirectFlag = '') {
      if (isLoggedIn()) {
        onReady();
        return;
      }
      const tutorDisplay = tutor.fullName ? `Sensei ${tutor.fullName}` : 'gia sư';
      if (typeof window.openLoginModal === 'function') {
        window.openLoginModal({ tutorName: tutorDisplay, onSuccess: onReady });
        return;
      }
      let returnUrl = window.location.pathname + window.location.search;
      if (redirectFlag && !returnUrl.includes(redirectFlag)) {
        returnUrl += (returnUrl.includes('?') ? '&' : '?') + redirectFlag;
      }
      window.location.href = `/login?redirect=${encodeURIComponent(returnUrl)}`;
    }

    // ---------------------------------------------------------------- Popup chọn giờ (dùng chung với trang xác nhận)
    // Popup chỉ chọn giờ; "Tiếp tục" sang /pages/booking-confirm/ để nhập liên hệ, lời nhắn rồi mới gửi.
    const pickers = createBookingPickers(tutor, {
      onContinue: (sel) => requireLogin(
        () => { window.location.href = `/pages/booking-confirm/?${buildBookingQuery(sel)}`; },
        sel.type === 'trial' ? 'connect=true' : 'connect=direct'),
      onWeekLoading: () => {
        const gridEl = document.getElementById('weekly-calendar-grid');
        if (gridEl && !pickers.weekData) {
          gridEl.innerHTML = '<div class="col-span-7 py-8 text-center text-xs text-slate-500">Đang tải lịch rảnh...</div>';
        }
      },
      onWeekLoaded: () => renderWeeklyCalendar(),
      onWeekError: (err, retry) => {
        const gridEl = document.getElementById('weekly-calendar-grid');
        if (!gridEl) return;
        gridEl.innerHTML = `
          <div class="col-span-7 py-8 text-center space-y-2">
            <div class="text-sm font-bold text-[#0F2545]">Không tải được lịch rảnh</div>
            <div class="text-xs text-slate-500">${escapeHtml(friendlyError(err, 'Vui lòng thử lại sau.'))}</div>
            <button type="button" id="week-retry-btn" class="mt-1 py-1.5 px-4 rounded-lg border border-slate-300 text-xs font-bold text-slate-700 hover:bg-slate-50 cursor-pointer">Thử lại</button>
          </div>`;
        document.getElementById('week-retry-btn')?.addEventListener('click', retry);
      }
    });

    function renderWeeklyCalendar() {
      const gridEl = document.getElementById('weekly-calendar-grid');
      if (!gridEl) return;

      if (!(tutor.availabilities || []).length) {
        gridEl.innerHTML = `
          <div class="col-span-7 py-8 text-center space-y-1">
            <div class="text-sm font-bold text-[#0F2545]">Gia sư chưa cập nhật lịch rảnh</div>
            <div class="text-xs text-slate-500">Bạn vẫn có thể đặt lịch học thử vào thời gian bạn muốn.</div>
          </div>`;
        return;
      }
      if (!pickers.weekData) return;
      pickers.renderWeekHeader();

      const expandWrapper = document.getElementById('weekly-expand-wrapper');
      const expandText = document.getElementById('toggle-weekly-slots-text');
      const expandIcon = document.getElementById('toggle-weekly-slots-icon');
      const today = todayIsoVN();
      let anyColumnTooLong = false;

      gridEl.innerHTML = WEEKDAY_COLUMNS.map(col => {
        const day = pickers.dayOf(col.dow);
        const date = day ? day.date : null;
        const isPast = !!date && date < today;
        const isToday = date === today;
        // Chỉ hiện giờ còn trống đủ cho một buổi học thử; giờ đã có lịch bị ẩn hẳn
        const starts = pickers.gridStartTimesOf(col.dow, TRIAL_DURATION_MINUTES);
        const hasFree = starts.length > 0;
        if (starts.length > MAX_PREVIEW_ITEMS) anyColumnTooLong = true;
        const shown = isWeeklySlotsExpanded ? starts : starts.slice(0, MAX_PREVIEW_ITEMS);

        const itemsHtml = shown.map(start => `
            <button type="button"
              class="weekly-slot-btn w-full py-1.5 px-1 rounded-lg text-xs sm:text-sm font-semibold text-[#0F2545] hover:text-[#006194] hover:bg-sky-50 underline decoration-slate-300 hover:decoration-[#006194] underline-offset-4 transition-all text-center cursor-pointer active:scale-95"
              data-date="${date || ''}" data-start="${start}"
              title="Đặt học thử ${TRIAL_DURATION_MINUTES} phút: ${col.fullName} ${date ? fmtDMY(date) : ''} lúc ${toHHMM(start)}">
              ${toHHMM(start)}
            </button>`).join('');

        const emptyText = isPast ? 'Đã qua' : 'Không có ca';
        return `
          <div class="flex flex-col items-center min-w-[76px] sm:min-w-[82px] text-center ${isPast ? 'opacity-60' : ''}">
            <div class="h-1 w-full rounded-full ${hasFree ? 'bg-emerald-500' : 'bg-slate-200'} mb-2.5"></div>
            <div class="pb-2 w-full">
              <span class="block text-xs sm:text-sm font-bold ${hasFree ? 'text-slate-700' : 'text-slate-400'}">${col.shortName}</span>
              <span class="block text-[11px] font-semibold ${isToday ? 'text-[#006194]' : 'text-slate-400'}">${date ? fmtDM(date) : ''}${isToday ? ' · Hôm nay' : ''}</span>
            </div>
            <div class="w-full flex flex-col items-center gap-1.5 pt-1 min-h-[50px]">
              ${itemsHtml || `<span class="text-[11px] text-slate-300 pt-2">${emptyText}</span>`}
            </div>
          </div>`;
      }).join('');

      if (expandWrapper) {
        expandWrapper.classList.toggle('hidden', !anyColumnTooLong);
        if (expandText) expandText.innerText = isWeeklySlotsExpanded ? 'Thu gọn lịch trình' : 'Xem toàn bộ lịch trình';
        if (expandIcon) expandIcon.innerText = isWeeklySlotsExpanded ? 'expand_less' : 'expand_more';
      }

      // Bấm vào một giờ trống → mở form học thử với ngày giờ đó đã được chọn sẵn
      gridEl.querySelectorAll('.weekly-slot-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          const date = btn.dataset.date;
          const start = Number(btn.dataset.start);
          if (!date) return;
          requireLogin(() => pickers.openTrialModal({ date, start }), 'connect=true');
        });
      });
    }

    function scrollToCalendar() {
      const scheduleSection = document.getElementById('weekly-booking-calendar-section');
      if (!scheduleSection) return;
      scheduleSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
      scheduleSection.classList.add('ring-2', 'ring-[#006194]/30');
      setTimeout(() => scheduleSection.classList.remove('ring-2', 'ring-[#006194]/30'), 1600);
    }

    const openConnectBtn = document.getElementById('open-connect-btn');
    if (openConnectBtn) {
      openConnectBtn.addEventListener('click', () => requireLogin(() => pickers.openDirectModal()));
    }

    const toggleWkExpandBtn = document.getElementById('btn-toggle-weekly-slots');
    if (toggleWkExpandBtn) {
      toggleWkExpandBtn.addEventListener('click', () => {
        isWeeklySlotsExpanded = !isWeeklySlotsExpanded;
        renderWeeklyCalendar();
      });
    }

    [['week-prev-btn', -1], ['week-next-btn', 1]].forEach(([id, delta]) => {
      const el = document.getElementById(id);
      if (el) el.addEventListener('click', () => pickers.shiftWeek(delta));
    });

    if ((tutor.availabilities || []).length) {
      pickers.showWeek();
    } else {
      renderWeeklyCalendar();
    }

    // Nút "Đặt lịch học thử 1-1" ở cột bên phải
    const openBtn = document.getElementById('open-booking-btn');
    if (openBtn) {
      openBtn.addEventListener('click', () => requireLogin(() => pickers.openTrialModal(), 'connect=true'));
    }

    // Liên kết tới thẳng phần lịch rảnh
    const urlParams = new URLSearchParams(window.location.search);
    if (window.location.hash === '#weekly-booking-calendar-section' || urlParams.get('section') === 'schedule') {
      requestAnimationFrame(() => setTimeout(scrollToCalendar, 150));
    }

    // connect=true (từ nút "Đặt lịch học thử" ở trang tìm gia sư): mở sẵn modal học thử
    if (urlParams.get('connect') === 'true') {
      setTimeout(() => requireLogin(() => pickers.openTrialModal()), 350);
    }
    // connect=direct (nút "Đổi giờ" ở trang xác nhận kết nối): mở lại popup chọn ca
    if (urlParams.get('connect') === 'direct') {
      setTimeout(() => requireLogin(() => pickers.openDirectModal()), 350);
    }

  } catch (err) {
    console.error('Lỗi khi tải chi tiết gia sư:', err);
    container.innerHTML = `
      <div class="p-12 text-center bg-white rounded-2xl border border-[#E5E7EB] space-y-3">
        <h3 class="text-lg font-bold text-[#0F2545]">Không thể tải hồ sơ gia sư</h3>
        <p class="text-sm text-on-surface-variant">Vui lòng kiểm tra lại đường truyền hoặc quay lại danh sách tìm kiếm.</p>
        <a href="/pages/tutor-discovery/" class="inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl bg-primary text-white text-sm font-semibold">Quay lại danh sách</a>
      </div>
    `;
  }
});
