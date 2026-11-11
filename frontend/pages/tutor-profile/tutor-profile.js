/**
 * KizunaTutor - Tutor Profile Detail Page Logic (UC-31)
 * Hỗ trợ URL ngắn /gs/:tutorCode (ví dụ: /gs/10001), ?code=10001 hoặc ?id=<UUID>.
 * Lấy thông tin chi tiết từ Database Supabase qua REST API.
 */

import { API } from '../../js/api.js';
import { formatVND } from '../../js/main.js';
import { openModal, closeModal } from '../../components/modal/modal.js';
import { isSlotClashing } from '../../js/tutor-schedule-helpers.js';
import { containsDetailedAddressOrGps, escapeHtml } from '../../js/profile-utils.js';

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
        <div class="p-12 text-center bg-white rounded-2xl border border-[#E5DECB] space-y-4 max-w-lg mx-auto my-12 shadow-sm">
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
        <div class="p-5 rounded-2xl border border-[#E5DECB] bg-white space-y-3 hover:shadow-xs transition-shadow review-item ${isExtra ? 'extra-review hidden' : ''}" data-has-comment="${r.comment ? 'true' : 'false'}">
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
            <p class="text-xs md:text-sm text-[#2D3748] leading-relaxed italic bg-[#FCFBF7] p-3.5 rounded-xl border border-[#F0ECE1]">
              “${r.comment}”
            </p>
          ` : ''}
        </div>
      `;
    }

    const reviewsCardsHtml = reviewList.length > 0
      ? reviewList.map((r, i) => renderSingleReview(r, i)).join('')
      : `
        <div class="p-8 text-center bg-white rounded-2xl border border-[#E5DECB] space-y-2">
          <p class="text-xs text-slate-500">Chưa có đánh giá nào từ học viên. Hãy là người đầu tiên đặt lịch học thử!</p>
        </div>
      `;

    // Hàm render khối Môn dạy & học phí (theo môn học, thời lượng, học phí thực tế của gia sư)
    function renderSubjectsAndPricingHtml(tutor) {
      const rawRate = Number(tutor.hourlyRate);
      const hasRate = !isNaN(rawRate) && rawRate > 0;
      const rate60 = hasRate ? rawRate : 0;
      const rate90 = hasRate ? Math.round(rawRate * 1.5) : 0;

      const price60Html = hasRate
        ? `<span class="font-bold text-[#006194] text-sm sm:text-base">${formatVND(rate60)} <span class="text-xs font-normal text-slate-400">/buổi</span></span>`
        : `<span class="inline-flex items-center px-2.5 py-0.5 rounded-md bg-slate-100 text-slate-600 text-xs font-medium">Chưa cập nhật</span>`;

      const price90Html = hasRate
        ? `<span class="font-bold text-[#006194] text-sm sm:text-base">${formatVND(rate90)} <span class="text-xs font-normal text-slate-400">/buổi</span></span>`
        : `<span class="inline-flex items-center px-2.5 py-0.5 rounded-md bg-slate-100 text-slate-600 text-xs font-medium">Thỏa thuận</span>`;

      const highestLevel = (tutor.highestJlptLevel || '').toUpperCase();
      const hasN1orN2 = ['N1', 'N2'].includes(highestLevel);

      const specialties = tutor.specialties || [];
      const specialtiesStr = (specialties.join(' ') + ' ' + (tutor.headline || '') + ' ' + (tutor.bio || '')).toLowerCase();

      const hasKaiwa = specialtiesStr.includes('kaiwa') || specialtiesStr.includes('giao tiếp') || specialtiesStr.includes('khẩu ngữ') || specialtiesStr.includes('phát âm');
      const hasBusiness = specialtiesStr.includes('thương mại') || specialtiesStr.includes('doanh nghiệp') || specialtiesStr.includes('bjt') || specialtiesStr.includes('business') || specialtiesStr.includes('công sở');

      // Danh sách các khối môn học được tạo động theo dữ liệu của chính gia sư
      const subjectBlocks = [];

      // 1. Khối Luyện thi JLPT & Tiếng Nhật nền tảng
      let jlptSubLevel1 = 'Lớp N5, Lớp N4, Lớp N3, Ôn luyện cấp tốc';
      let jlptSubLevel2 = 'Lớp N2, Lớp N1, Luyện giải đề chuyên sâu Dokkai & Choukai';

      if (highestLevel === 'N3') {
        jlptSubLevel1 = 'Lớp N5, Lớp N4, Lớp N3, Ôn luyện sơ - trung cấp';
      } else if (highestLevel === 'N4' || highestLevel === 'N5') {
        jlptSubLevel1 = 'Tiếng Nhật sơ cấp, Bảng chữ cái Hiragana & Katakana, Lớp N5';
      }

      subjectBlocks.push(`
        <div class="border border-slate-200/90 rounded-2xl overflow-hidden shadow-2xs">
          <div class="flex items-center gap-2 px-4 py-2.5 bg-[#F0F5FA] border-b border-slate-200 text-[#006194] font-bold text-sm">
            <span class="material-symbols-outlined text-[18px]">school</span>
            <span>Luyện thi JLPT & Ngữ pháp</span>
          </div>
          <div class="divide-y divide-slate-100 bg-white">
            <div class="flex flex-col sm:flex-row sm:items-center justify-between p-3.5 px-4 gap-2 hover:bg-slate-50/50 transition-colors">
              <span class="text-xs sm:text-sm font-medium text-slate-800">${jlptSubLevel1}</span>
              <div class="flex items-center justify-between sm:justify-end gap-6 sm:gap-10 shrink-0 text-xs sm:text-sm">
                <span class="text-slate-400">60 phút/buổi</span>
                ${price60Html}
              </div>
            </div>
            ${hasN1orN2 ? `
            <div class="flex flex-col sm:flex-row sm:items-center justify-between p-3.5 px-4 gap-2 hover:bg-slate-50/50 transition-colors">
              <span class="text-xs sm:text-sm font-medium text-slate-800">${jlptSubLevel2}</span>
              <div class="flex items-center justify-between sm:justify-end gap-6 sm:gap-10 shrink-0 text-xs sm:text-sm">
                <span class="text-slate-400">90 phút/buổi</span>
                ${price90Html}
              </div>
            </div>
            ` : ''}
          </div>
        </div>
      `);

      // 2. Khối Tiếng Nhật giao tiếp (Kaiwa)
      if (hasKaiwa || !hasBusiness) {
        subjectBlocks.push(`
          <div class="border border-slate-200/90 rounded-2xl overflow-hidden shadow-2xs">
            <div class="flex items-center gap-2 px-4 py-2.5 bg-[#F0F5FA] border-b border-slate-200 text-[#006194] font-bold text-sm">
              <span class="material-symbols-outlined text-[18px]">forum</span>
              <span>Tiếng Nhật giao tiếp (Kaiwa)</span>
            </div>
            <div class="divide-y divide-slate-100 bg-white">
              <div class="flex flex-col sm:flex-row sm:items-center justify-between p-3.5 px-4 gap-2 hover:bg-slate-50/50 transition-colors">
                <span class="text-xs sm:text-sm font-medium text-slate-800">Giao tiếp phản xạ hàng ngày, Phát âm chuẩn bản xứ</span>
                <div class="flex items-center justify-between sm:justify-end gap-6 sm:gap-10 shrink-0 text-xs sm:text-sm">
                  <span class="text-slate-400">60 phút/buổi</span>
                  ${price60Html}
                </div>
              </div>
              <div class="flex flex-col sm:flex-row sm:items-center justify-between p-3.5 px-4 gap-2 hover:bg-slate-50/50 transition-colors">
                <span class="text-xs sm:text-sm font-medium text-slate-800">Hội thoại nâng cao, Phỏng vấn xin việc & Du học Nhật Bản</span>
                <div class="flex items-center justify-between sm:justify-end gap-6 sm:gap-10 shrink-0 text-xs sm:text-sm">
                  <span class="text-slate-400">90 phút/buổi</span>
                  ${price90Html}
                </div>
              </div>
            </div>
          </div>
        `);
      }

      // 3. Khối Tiếng Nhật thương mại (Business)
      if (hasBusiness) {
        subjectBlocks.push(`
          <div class="border border-slate-200/90 rounded-2xl overflow-hidden shadow-2xs">
            <div class="flex items-center gap-2 px-4 py-2.5 bg-[#F0F5FA] border-b border-slate-200 text-[#006194] font-bold text-sm">
              <span class="material-symbols-outlined text-[18px]">business_center</span>
              <span>Tiếng Nhật thương mại (Business)</span>
            </div>
            <div class="divide-y divide-slate-100 bg-white">
              <div class="flex flex-col sm:flex-row sm:items-center justify-between p-3.5 px-4 gap-2 hover:bg-slate-50/50 transition-colors">
                <span class="text-xs sm:text-sm font-medium text-slate-800">Tiếng Nhật công sở, Soạn thảo văn bản email, Ôn thi chứng chỉ BJT</span>
                <div class="flex items-center justify-between sm:justify-end gap-6 sm:gap-10 shrink-0 text-xs sm:text-sm">
                  <span class="text-slate-400">90 phút/buổi</span>
                  ${price90Html}
                </div>
              </div>
            </div>
          </div>
        `);
      }

      // Hình thức dạy (teaching format)
      const teachingFormatStr = (tutor.teachingFormat || '').toLowerCase();
      const hasOnline = teachingFormatStr.includes('online') || !teachingFormatStr;
      const hasOffline = teachingFormatStr.includes('offline') || teachingFormatStr.includes('trực tiếp') || (tutor.location && tutor.location.trim().length > 0);

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
        <div class="bg-white border border-[#E5DECB] rounded-2xl p-6 md:p-8 shadow-sm space-y-5">
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
          <div class="bg-white border border-[#E5DECB] rounded-2xl p-6 md:p-8 shadow-sm space-y-6">
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
                  <span class="text-[#E5DECB]">|</span>
                  <div class="flex items-center gap-1">
                    <span class="material-symbols-outlined text-[16px] text-primary">school</span>
                    <span>${tutor.experienceYears || 0} năm kinh nghiệm</span>
                  </div>
                  <span class="text-[#E5DECB]">|</span>
                  <div class="flex items-center gap-1">
                    <span class="material-symbols-outlined text-[16px] text-primary">location_on</span>
                    <span>${escapeHtml(sanitizeDisplayLocation(tutor.location))}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- Tags Chuyên môn & Trình độ -->
            <div class="pt-4 border-t border-[#ECE8DE] flex flex-wrap gap-2 items-center">
              <span class="text-xs font-bold text-[#0F2545] mr-2">Chuyên môn:</span>
              <span class="px-3 py-1 rounded-lg bg-sky-50 text-sky-800 text-xs font-bold border border-sky-200">Trình độ JLPT ${tutor.highestJlptLevel || 'N1'}</span>
              ${(tutor.specialties || []).map(s => `
                <span class="px-3 py-1 rounded-lg bg-slate-100 text-slate-700 text-xs font-medium border border-slate-200">${s}</span>
              `).join('')}
            </div>
          </div>

          <!-- Bio / Giới thiệu chi tiết -->
          <div class="bg-white border border-[#E5DECB] rounded-2xl p-6 md:p-8 shadow-sm space-y-4">
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
          <div id="weekly-booking-calendar-section" class="bg-white border border-[#E5DECB] rounded-2xl p-6 md:p-8 shadow-sm space-y-5 scroll-mt-28 transition-all duration-300">

            <div class="flex items-start justify-between flex-wrap gap-3 pb-3 border-b border-slate-100">
              <div class="space-y-1">
                <h3 class="text-lg font-bold text-[#0F2545] flex items-center gap-2">
                  <span class="material-symbols-outlined text-primary">calendar_month</span>
                  Lịch rảnh giảng dạy hằng tuần
                </h3>
                <p class="text-xs text-on-surface-variant leading-relaxed">
                  Bấm vào giờ bạn muốn để mở form kết nối với gia sư. Bạn không cần học thử trước.
                </p>
              </div>
              <div class="flex items-center gap-3 text-[11px] text-slate-500 pt-1">
                <span class="inline-flex items-center gap-1"><span class="w-2.5 h-2.5 rounded bg-slate-200"></span>Đã có học viên</span>
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
          <div class="bg-white border border-[#E5DECB] rounded-2xl p-6 md:p-8 shadow-sm space-y-6">
            <!-- Header khối đánh giá -->
            <div class="flex items-center justify-between flex-wrap gap-2 pb-1">
              <h3 class="text-lg font-bold text-[#0F2545] flex items-center gap-2">
                <span class="text-amber-500 text-xl font-black">★</span>
                <span>Đánh giá từ học viên (${totalReviews})</span>
              </h3>
            </div>

            <!-- Hộp tổng quan điểm đánh giá & Biểu đồ sao -->
            <div class="bg-[#FAF8F5] border border-[#ECE8DE] rounded-2xl p-6 grid grid-cols-1 md:grid-cols-12 gap-6 items-center">
              <!-- Cột trái: Điểm số trung bình -->
              <div class="md:col-span-4 flex flex-col items-center justify-center text-center space-y-2 border-b md:border-b-0 md:border-r border-[#ECE8DE] pb-4 md:pb-0 md:pr-4">
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
                <button type="button" id="toggle-more-reviews-btn" class="w-full py-3 px-4 rounded-xl bg-white border border-[#E5DECB] hover:bg-[#FAF8F5] text-xs font-semibold text-[#0F2545] transition-colors flex items-center justify-center gap-1.5 shadow-2xs">
                  <span id="toggle-more-text">Xem toàn bộ ${totalReviews} đánh giá</span>
                  <span class="material-symbols-outlined text-[16px]" id="toggle-more-icon">expand_more</span>
                </button>
              </div>
            ` : ''}
          </div>

        </div>

        <!-- Cột Phải: Thẻ Đặt lịch & Học phí cố định -->
        <div class="lg:col-span-1 space-y-6 lg:sticky lg:top-28">
          <div class="bg-white border-2 border-primary/20 rounded-2xl p-6 shadow-lg space-y-6">
            <div class="space-y-1 pb-4 border-b border-[#ECE8DE]">
              <span class="text-xs font-semibold text-slate-500">Học phí</span>
              <div class="flex items-baseline gap-1">
                ${Number(tutor.hourlyRate) > 0 ? `
                  <span class="text-3xl font-extrabold text-[#006194]">${formatVND(tutor.hourlyRate)}</span>
                  <span class="text-xs text-on-surface-variant font-medium">/ giờ (60 phút)</span>
                ` : `
                  <span class="text-2xl font-extrabold text-[#006194]">Thỏa thuận</span>
                  <span class="text-xs text-on-surface-variant font-medium">(Chưa cập nhật)</span>
                `}
              </div>
            </div>

            <!-- Hành động chính (CTA) -->
            <div class="space-y-2.5">
              <button type="button" id="open-connect-btn" class="w-full h-12 px-4 rounded-xl bg-[#006194] hover:bg-[#004b73] border border-[#004b73] text-white font-semibold text-[15px] transition-all active:scale-[0.98] flex items-center justify-center gap-2.5 cursor-pointer">
                <span class="material-symbols-outlined text-[20px]">handshake</span>
                Kết nối với gia sư
              </button>
              <button type="button" id="open-booking-btn" class="w-full h-12 px-4 rounded-xl bg-white hover:bg-slate-50 ring-1 ring-inset ring-slate-200 hover:ring-slate-300 text-[#0F2545] font-semibold text-[15px] transition-all active:scale-[0.98] flex items-center justify-center gap-2.5 cursor-pointer">
                <span class="material-symbols-outlined text-[20px]">calendar_add_on</span>
                Đặt lịch học thử 1-1
              </button>
            </div>

            <!-- Cam kết đảm bảo quyền lợi -->
            <div class="pt-4 border-t border-[#ECE8DE] space-y-3 text-xs text-on-surface-variant">
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
    const WEEKDAY_COLUMNS = [
      { dow: 2, shortName: 'Thứ 2', fullName: 'Thứ hai' },
      { dow: 3, shortName: 'Thứ 3', fullName: 'Thứ ba' },
      { dow: 4, shortName: 'Thứ 4', fullName: 'Thứ tư' },
      { dow: 5, shortName: 'Thứ 5', fullName: 'Thứ năm' },
      { dow: 6, shortName: 'Thứ 6', fullName: 'Thứ sáu' },
      { dow: 7, shortName: 'Thứ 7', fullName: 'Thứ bảy' },
      { dow: 1, shortName: 'Chủ nhật', fullName: 'Chủ nhật' }
    ];
    const DURATION_OPTIONS = [60, 90, 120];
    const MAX_SELECTED_SLOTS = 7;
    const TRIAL_DURATION_MINUTES = 30;
    const MIN_CONNECT_DURATION = DURATION_OPTIONS[0];
    const MAX_PREVIEW_ITEMS = 8;
    let isWeeklySlotsExpanded = false;

    const pad = (n) => String(n).padStart(2, '0');
    const toMinutes = (hhmm) => {
      const [h, m] = String(hhmm).split(':').map(Number);
      return h * 60 + m;
    };
    const toHHMM = (minutes) => `${pad(Math.floor(minutes / 60))}:${pad(minutes % 60)}`;
    const dayNameOf = (dow) => (WEEKDAY_COLUMNS.find(c => c.dow === dow) || {}).fullName || '';
    const slotLabel = (s) => `${dayNameOf(s.dow)} ${toHHMM(s.start)}–${toHHMM(s.start + s.duration)}`;
    const overlaps = (aStart, aEnd, bStart, bEnd) => aStart < bEnd && aEnd > bStart;
    const isLoggedIn = () => !!(localStorage.getItem('kizuna_token') || localStorage.getItem('token'));

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

    function friendlyError(err, fallback) {
      if (err && (err.status === 401)) return 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.';
      if (err && (err.name === 'TypeError' || err.name === 'TimeoutError' || String(err.message || '').toLowerCase().includes('fetch'))) {
        return 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra lại kết nối mạng hoặc thử lại sau.';
      }
      if (err && err.status === 403) return 'Chỉ tài khoản học viên mới có thể gửi yêu cầu này.';
      return (err && err.message) ? err.message : fallback;
    }

    // ---------------------------------------------------------------- Lịch rảnh hằng tuần (chỉ hiển thị)

    function lockedRangesOf(dow) {
      return (tutor.lockedSlots || [])
        .filter(l => Number(l.dayOfWeek) === dow && l.startTime && l.endTime)
        .map(l => ({ start: toMinutes(l.startTime), end: toMinutes(l.endTime) }));
    }

    /** Khung rảnh của một ngày sau khi trừ đi các ca đã có học viên. */
    function freeRangesOf(dow) {
      const locked = lockedRangesOf(dow);
      const result = [];
      (tutor.availabilities || [])
        .filter(a => Number(a.dayOfWeek) === dow && a.startTime && a.endTime)
        .forEach(a => {
          let pieces = [{ start: toMinutes(a.startTime), end: toMinutes(a.endTime) }];
          locked.forEach(l => {
            pieces = pieces.flatMap(p => {
              if (!overlaps(p.start, p.end, l.start, l.end)) return [p];
              const parts = [];
              if (l.start > p.start) parts.push({ start: p.start, end: l.start });
              if (l.end < p.end) parts.push({ start: l.end, end: p.end });
              return parts;
            });
          });
          result.push(...pieces.filter(p => p.end - p.start >= 30));
        });
      return result.sort((x, y) => x.start - y.start);
    }

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

      const expandWrapper = document.getElementById('weekly-expand-wrapper');
      const expandText = document.getElementById('toggle-weekly-slots-text');
      const expandIcon = document.getElementById('toggle-weekly-slots-icon');
      let anyColumnTooLong = false;

      gridEl.innerHTML = WEEKDAY_COLUMNS.map(col => {
        // Giờ bắt đầu còn trống (đủ cho ca ngắn nhất) + các ca đã có học viên (mờ)
        const items = [
          ...startTimesOf(col.dow, MIN_CONNECT_DURATION).map(start => ({ start, locked: false })),
          ...lockedRangesOf(col.dow).map(r => ({ ...r, locked: true }))
        ].sort((x, y) => x.start - y.start);
        const hasFree = items.some(i => !i.locked);
        if (items.length > MAX_PREVIEW_ITEMS) anyColumnTooLong = true;
        const shown = isWeeklySlotsExpanded ? items : items.slice(0, MAX_PREVIEW_ITEMS);

        const itemsHtml = shown.map(i => i.locked ? `
            <div class="w-full py-1 px-1 rounded-lg bg-slate-100 text-slate-400 text-center select-none cursor-not-allowed" title="Ca này đã có học viên">
              <div class="text-[11px] sm:text-xs font-semibold line-through">${toHHMM(i.start)}–${toHHMM(i.end)}</div>
              <div class="text-[10px] font-medium">Đã có học viên</div>
            </div>` : `
            <button type="button"
              class="weekly-slot-btn w-full py-1.5 px-1 rounded-lg text-xs sm:text-sm font-semibold text-[#0F2545] hover:text-[#006194] hover:bg-sky-50 underline decoration-slate-300 hover:decoration-[#006194] underline-offset-4 transition-all text-center cursor-pointer active:scale-95"
              data-dow="${col.dow}" data-start="${i.start}"
              title="Kết nối học ${col.fullName} lúc ${toHHMM(i.start)} hằng tuần">
              ${toHHMM(i.start)}
            </button>`).join('');

        return `
          <div class="flex flex-col items-center min-w-[76px] sm:min-w-[82px] text-center">
            <div class="h-1 w-full rounded-full ${hasFree ? 'bg-emerald-500' : 'bg-slate-200'} mb-2.5"></div>
            <div class="pb-2 w-full">
              <span class="block text-xs sm:text-sm font-bold ${hasFree ? 'text-slate-700' : 'text-slate-400'}">${col.shortName}</span>
            </div>
            <div class="w-full flex flex-col items-center gap-1.5 pt-1 min-h-[50px]">
              ${itemsHtml || '<span class="text-[11px] text-slate-300 pt-2">Không có ca</span>'}
            </div>
          </div>`;
      }).join('');

      if (expandWrapper) {
        expandWrapper.classList.toggle('hidden', !anyColumnTooLong);
        if (expandText) expandText.innerText = isWeeklySlotsExpanded ? 'Thu gọn lịch trình' : 'Xem toàn bộ lịch trình';
        if (expandIcon) expandIcon.innerText = isWeeklySlotsExpanded ? 'expand_less' : 'expand_more';
      }

      // Bấm vào một giờ → mở form kết nối với ca đó đã được chọn sẵn
      gridEl.querySelectorAll('.weekly-slot-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          const dow = Number(btn.dataset.dow);
          const start = Number(btn.dataset.start);
          requireLogin(() => openDirectModal({ dow, start }));
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

    // ---------------------------------------------------------------- Form kết nối trực tiếp

    let connectDuration = MIN_CONNECT_DURATION;
    let connectDay = null; // dow đang xem trong form
    const selectedSlots = []; // { dow, start (phút từ 00:00), duration }

    /** Các giờ bắt đầu (bước 30 phút) đủ chỗ cho một ca dài `duration` phút trong khung rảnh còn trống. */
    function startTimesOf(dow, duration = connectDuration) {
      const starts = [];
      freeRangesOf(dow).forEach(r => {
        for (let t = Math.ceil(r.start / 30) * 30; t + duration <= r.end && t + duration < 24 * 60; t += 30) {
          if (isSlotClashing(tutor, dow, null, t, duration)) continue;
          starts.push(t);
        }
      });
      return [...new Set(starts)].sort((a, b) => a - b);
    }

    const directErrorAlert = document.getElementById('direct-error-alert');
    const directErrorMsg = document.getElementById('direct-error-message');
    const directSubmitBtn = document.getElementById('btn-direct-submit');

    function showDirectError(msg) {
      if (!directErrorAlert || !directErrorMsg) return;
      directErrorMsg.textContent = msg;
      directErrorAlert.classList.toggle('hidden', !msg);
      if (msg) directErrorAlert.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }

    const directSubtitle = document.getElementById('direct-modal-subtitle');
    if (directSubtitle && tutor.fullName) {
      directSubtitle.textContent = `Chọn ca học cố định mỗi tuần cùng Sensei ${tutor.fullName}, không cần học thử trước`;
    }

    function renderConnectForm() {
      // Thời lượng
      const durEl = document.getElementById('dc-duration');
      if (durEl) {
        durEl.innerHTML = DURATION_OPTIONS.map(d => `
          <button type="button" data-duration="${d}"
            class="px-3 py-1 rounded-lg text-xs font-bold transition-all cursor-pointer ${d === connectDuration ? 'bg-white text-[#006194] shadow-2xs border border-slate-200' : 'text-slate-500 hover:text-slate-700'}">
            ${d} phút
          </button>`).join('');
        durEl.querySelectorAll('[data-duration]').forEach(btn => btn.addEventListener('click', () => {
          connectDuration = Number(btn.dataset.duration);
          renderConnectForm();
        }));
      }

      // Thứ trong tuần
      const daysEl = document.getElementById('dc-days');
      if (daysEl) {
        daysEl.innerHTML = WEEKDAY_COLUMNS.map(col => {
          const available = startTimesOf(col.dow).length > 0;
          const active = col.dow === connectDay;
          const count = selectedSlots.filter(s => s.dow === col.dow).length;
          const cls = active
            ? 'border-2 border-[#006194] bg-sky-50 text-[#006194] font-bold'
            : available ? 'border border-slate-200 hover:bg-slate-50 text-slate-800 font-semibold' : 'border border-transparent text-slate-300';
          return `
            <button type="button" data-day="${col.dow}" ${available || count ? '' : 'disabled'}
              class="relative py-2 rounded-xl text-[11px] sm:text-xs transition-all ${available || count ? 'cursor-pointer' : 'cursor-not-allowed'} ${cls}">
              ${col.shortName === 'Chủ nhật' ? 'CN' : col.shortName.replace('Thứ ', 'T')}
              ${count ? `<span class="absolute -top-1.5 -right-1 min-w-4 h-4 px-1 rounded-full bg-[#006194] text-white text-[10px] leading-4">${count}</span>` : ''}
            </button>`;
        }).join('');
        daysEl.querySelectorAll('[data-day]').forEach(btn => btn.addEventListener('click', () => {
          connectDay = Number(btn.dataset.day);
          renderConnectForm();
        }));
      }

      // Giờ bắt đầu của ngày đang chọn
      const timesEl = document.getElementById('dc-times');
      const timesLabel = document.getElementById('dc-times-label');
      if (timesLabel) timesLabel.textContent = connectDay ? `Giờ bắt đầu - ${dayNameOf(connectDay)}` : 'Giờ bắt đầu';
      if (timesEl) {
        const starts = connectDay ? startTimesOf(connectDay) : [];
        const daySelected = selectedSlots.filter(s => s.dow === connectDay);
        const allStarts = [...new Set([...starts, ...daySelected.map(s => s.start)])].sort((a, b) => a - b);
        if (!allStarts.length) {
          timesEl.innerHTML = `<div class="col-span-full py-4 text-center text-xs text-slate-500">${connectDay
            ? `Không còn khung ${connectDuration} phút trống vào ${dayNameOf(connectDay)}. Hãy chọn thứ khác hoặc thời lượng ngắn hơn.`
            : 'Gia sư chưa có khung giờ trống phù hợp.'}</div>`;
        } else {
          timesEl.innerHTML = allStarts.map(t => {
            const chosen = daySelected.find(s => s.start === t);
            const blocked = !chosen && daySelected.some(s => overlaps(t, t + connectDuration, s.start, s.start + s.duration));
            if (chosen) {
              return `<button type="button" data-start="${t}" class="py-2 px-1 rounded-xl border-2 border-[#006194] bg-[#006194] text-white text-xs font-bold cursor-pointer" title="Bỏ chọn">
                ${toHHMM(t)}–${toHHMM(t + chosen.duration)}</button>`;
            }
            if (blocked) {
              return `<div class="py-2 px-1 rounded-xl border border-slate-100 text-slate-300 text-xs font-medium text-center cursor-not-allowed" title="Trùng với ca bạn đã chọn">${toHHMM(t)}</div>`;
            }
            return `<button type="button" data-start="${t}" class="py-2 px-1 rounded-xl border border-slate-300 bg-white hover:border-[#006194] hover:bg-sky-50 text-slate-800 text-xs font-semibold cursor-pointer transition-all"
              title="${dayNameOf(connectDay)} ${toHHMM(t)}–${toHHMM(t + connectDuration)}">${toHHMM(t)}</button>`;
          }).join('');
          timesEl.querySelectorAll('[data-start]').forEach(btn => btn.addEventListener('click', () => toggleSlot(connectDay, Number(btn.dataset.start))));
        }
      }

      // Ca đã chọn
      const selLabel = document.getElementById('dc-selected-label');
      const selEl = document.getElementById('dc-selected');
      if (selLabel) selLabel.textContent = `Ca đã chọn (${selectedSlots.length}/${MAX_SELECTED_SLOTS})`;
      if (selEl) {
        selEl.innerHTML = selectedSlots.length
          ? selectedSlots.map((s, i) => `
              <span class="inline-flex items-center gap-1.5 pl-3 pr-1.5 py-1 rounded-full bg-sky-50 border border-sky-200 text-xs font-semibold text-[#006194]">
                ${slotLabel(s)}
                <button type="button" data-remove-index="${i}" class="w-5 h-5 rounded-full hover:bg-sky-100 flex items-center justify-center cursor-pointer" aria-label="Bỏ chọn ${slotLabel(s)}">
                  <span class="material-symbols-outlined text-[14px]">close</span>
                </button>
              </span>`).join('')
          : '<span class="text-xs text-slate-500">Chưa chọn ca nào. Hãy chọn thứ rồi bấm vào giờ bắt đầu.</span>';
        selEl.querySelectorAll('[data-remove-index]').forEach(btn => btn.addEventListener('click', () => {
          selectedSlots.splice(Number(btn.dataset.removeIndex), 1);
          renderConnectForm();
        }));
      }

      if (directSubmitBtn) directSubmitBtn.disabled = selectedSlots.length === 0;
      const submitText = document.getElementById('btn-direct-submit-text');
      if (submitText) {
        submitText.textContent = selectedSlots.length
          ? `Gửi yêu cầu kết nối (${selectedSlots.length} ca/tuần)`
          : 'Gửi yêu cầu kết nối';
      }
    }

    function toggleSlot(dow, start) {
      showDirectError('');
      const idx = selectedSlots.findIndex(s => s.dow === dow && s.start === start);
      if (idx >= 0) {
        selectedSlots.splice(idx, 1);
      } else if (selectedSlots.length >= MAX_SELECTED_SLOTS) {
        showDirectError(`Bạn chỉ có thể chọn tối đa ${MAX_SELECTED_SLOTS} ca mỗi tuần.`);
        return;
      } else {
        selectedSlots.push({ dow, start, duration: connectDuration });
        const order = (d) => WEEKDAY_COLUMNS.findIndex(c => c.dow === d);
        selectedSlots.sort((a, b) => order(a.dow) - order(b.dow) || a.start - b.start);
      }
      renderConnectForm();
    }

    /** preselect: { dow, start } khi học viên bấm một giờ trên bảng lịch rảnh. */
    function openDirectModal(preselect = null) {
      if (preselect) {
        connectDay = preselect.dow;
        const already = selectedSlots.some(s => s.dow === preselect.dow && s.start === preselect.start);
        const clash = selectedSlots.some(s => s.dow === preselect.dow
          && overlaps(preselect.start, preselect.start + connectDuration, s.start, s.start + s.duration));
        if (!already && !clash && selectedSlots.length < MAX_SELECTED_SLOTS) {
          // Giờ bấm trên bảng luôn đủ cho ca ngắn nhất; nếu thời lượng đang chọn không vừa thì lùi về ca ngắn nhất
          if (!startTimesOf(preselect.dow).includes(preselect.start)) connectDuration = MIN_CONNECT_DURATION;
          if (startTimesOf(preselect.dow).includes(preselect.start)) {
            selectedSlots.push({ dow: preselect.dow, start: preselect.start, duration: connectDuration });
            const order = (d) => WEEKDAY_COLUMNS.findIndex(c => c.dow === d);
            selectedSlots.sort((a, b) => order(a.dow) - order(b.dow) || a.start - b.start);
          }
        }
      } else if (!connectDay || !startTimesOf(connectDay).length) {
        const firstDay = WEEKDAY_COLUMNS.find(c => startTimesOf(c.dow).length > 0);
        connectDay = firstDay ? firstDay.dow : null;
      }
      showDirectError('');
      renderConnectForm();
      openModal('direct-connect-modal');
    }

    if (directSubmitBtn) {
      directSubmitBtn.addEventListener('click', async () => {
        showDirectError('');
        if (!selectedSlots.length) {
          showDirectError('Hãy chọn ít nhất một ca học hằng tuần.');
          return;
        }
        const level = document.getElementById('direct-level');
        const goals = document.getElementById('direct-goals');
        const notes = document.getElementById('direct-notes');
        const payload = {
          tutorId: tutor.tutorId,
          slots: selectedSlots.map(s => ({ dayOfWeek: s.dow, startTime: toHHMM(s.start), durationMinutes: s.duration })),
          currentLevel: level ? level.value : null,
          learningGoals: goals ? (goals.value.trim() || null) : null,
          initialMessage: notes ? (notes.value.trim() || null) : null
        };

        directSubmitBtn.disabled = true;
        const original = directSubmitBtn.innerHTML;
        directSubmitBtn.innerHTML = '<span class="material-symbols-outlined animate-spin text-[18px]">progress_activity</span><span>Đang gửi...</span>';
        try {
          await API.connectionRequests.create(payload);
          const body = document.getElementById('direct-modal-body');
          const footer = document.getElementById('direct-modal-footer');
          if (body) {
            body.innerHTML = `
              <div class="p-8 text-center space-y-4">
                <div class="w-16 h-16 rounded-full bg-emerald-50 text-emerald-600 flex items-center justify-center mx-auto shadow-xs border border-emerald-100">
                  <span class="material-symbols-outlined text-[34px]">check_circle</span>
                </div>
                <h3 class="text-lg font-bold text-[#0F2545]">Đã gửi yêu cầu kết nối</h3>
                <p class="text-xs text-on-surface-variant leading-relaxed">
                  Sensei <strong>${escapeHtml(tutor.fullName || '')}</strong> sẽ xem các ca bạn chọn và phản hồi sớm.
                  Khi gia sư đồng ý, hai bạn được kết nối và có thể bắt đầu lớp học chính thức.
                </p>
                <ul class="p-4 bg-[#FAF8F2] rounded-2xl border border-[#ECE8DE] text-xs text-left space-y-1.5">
                  ${selectedSlots.map(s => `<li class="font-semibold text-[#0F2545]">${slotLabel(s)}</li>`).join('')}
                </ul>
                <div class="grid grid-cols-2 gap-2.5 pt-2">
                  <a href="/pages/learner-connections/" class="py-2.5 px-3 rounded-xl bg-[#006194] hover:bg-[#004b73] text-white text-xs font-bold text-center transition-all shadow-xs flex items-center justify-center gap-1.5">
                    <span class="material-symbols-outlined text-[16px]">list_alt</span>
                    <span>Xem yêu cầu của tôi</span>
                  </a>
                  <button type="button" onclick="window.closeModal('direct-connect-modal'); location.reload();" class="py-2.5 px-3 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold transition-all">
                    Hoàn tất
                  </button>
                </div>
              </div>`;
          }
          if (footer) footer.style.display = 'none';
        } catch (err) {
          console.error('Lỗi khi gửi yêu cầu kết nối:', err);
          showDirectError(friendlyError(err, 'Không thể gửi yêu cầu kết nối lúc này. Vui lòng thử lại sau.'));
          directSubmitBtn.disabled = false;
          directSubmitBtn.innerHTML = original;
        }
      });
    }

    const openConnectBtn = document.getElementById('open-connect-btn');
    if (openConnectBtn) {
      openConnectBtn.addEventListener('click', () => requireLogin(() => openDirectModal()));
    }

    const toggleWkExpandBtn = document.getElementById('btn-toggle-weekly-slots');
    if (toggleWkExpandBtn) {
      toggleWkExpandBtn.addEventListener('click', () => {
        isWeeklySlotsExpanded = !isWeeklySlotsExpanded;
        renderWeeklyCalendar();
      });
    }

    renderWeeklyCalendar();

    // ---------------------------------------------------------------- Modal học thử (tự chọn ngày giờ)

    const modalAvatar = document.getElementById('booking-modal-avatar');
    const modalAvatarContainer = document.getElementById('booking-modal-avatar-container');
    const initialsAvatarHtml = `
      <div class="w-12 h-12 rounded-2xl ${tutorStyle.bg} ${tutorStyle.text} font-bold flex items-center justify-center border border-slate-200 shadow-xs text-sm">
        ${tutorInitials}
      </div>`;
    if (tutor.avatarUrl && modalAvatar) {
      modalAvatar.src = tutor.avatarUrl;
      modalAvatar.onerror = () => {
        if (modalAvatarContainer) modalAvatarContainer.innerHTML = initialsAvatarHtml;
      };
    } else if (modalAvatarContainer) {
      modalAvatarContainer.innerHTML = initialsAvatarHtml;
    }
    const modalSubtitle = document.getElementById('booking-modal-subtitle');
    if (modalSubtitle && tutor.fullName) {
      modalSubtitle.innerText = `Chọn thời gian bạn muốn học thử cùng Sensei ${tutor.fullName}`;
    }

    const continueBtn = document.getElementById('btn-booking-continue');
    const modalBody = document.getElementById('booking-modal-body');
    const modalFooter = document.getElementById('booking-modal-footer');
    const errorAlert = document.getElementById('booking-error-alert');
    const errorMsgEl = document.getElementById('booking-error-message');
    const trialDateInput = document.getElementById('trial-date');
    const trialTimeInput = document.getElementById('trial-time');
    const trialSummary = document.getElementById('trial-time-summary');
    const DAY_NAMES = ['Chủ nhật', 'Thứ hai', 'Thứ ba', 'Thứ tư', 'Thứ năm', 'Thứ sáu', 'Thứ bảy'];

    function showError(msg) {
      if (errorAlert && errorMsgEl) {
        errorMsgEl.innerText = msg;
        errorAlert.classList.remove('hidden');
        errorAlert.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
      } else {
        alert(msg);
      }
    }

    function clearError() {
      if (errorAlert) errorAlert.classList.add('hidden');
    }

    /** Ngày giờ học thử học viên chọn (giờ Việt Nam), hoặc null nếu chưa đủ. */
    function getTrialStart() {
      if (!trialDateInput || !trialTimeInput || !trialDateInput.value || !trialTimeInput.value) return null;
      const [y, mo, d] = trialDateInput.value.split('-').map(Number);
      const [h, mi] = trialTimeInput.value.split(':').map(Number);
      return new Date(y, mo - 1, d, h, mi, 0, 0);
    }

    function updateTrialSummary() {
      const start = getTrialStart();
      if (continueBtn) continueBtn.disabled = !start;
      if (!trialSummary) return;
      if (!start) {
        trialSummary.textContent = 'Bạn có thể chọn bất kỳ ngày giờ nào thuận tiện. Gia sư sẽ xem và đồng ý hoặc từ chối yêu cầu của bạn.';
        return;
      }
      const end = new Date(start.getTime() + TRIAL_DURATION_MINUTES * 60000);
      trialSummary.innerHTML = `Bạn muốn học thử vào <strong class="text-[#0F2545]">${DAY_NAMES[start.getDay()]}, ${pad(start.getDate())}/${pad(start.getMonth() + 1)}/${start.getFullYear()} · ${pad(start.getHours())}:${pad(start.getMinutes())}–${pad(end.getHours())}:${pad(end.getMinutes())}</strong>. Gia sư sẽ xác nhận lại với bạn.`;
    }

    if (trialDateInput) {
      const today = new Date();
      trialDateInput.min = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`;
      trialDateInput.addEventListener('change', () => { clearError(); updateTrialSummary(); });
    }
    if (trialTimeInput) {
      trialTimeInput.addEventListener('change', () => { clearError(); updateTrialSummary(); });
    }
    updateTrialSummary();

    function openTrialModal() {
      clearError();
      updateTrialSummary();
      openModal('connect-modal');
    }

    if (continueBtn && modalBody) {
      continueBtn.addEventListener('click', () => requireLogin(submitTrial, 'connect=true'));
    }

    async function submitTrial() {
      clearError();
      const start = getTrialStart();
      if (!start) {
        showError('Vui lòng chọn ngày và giờ bạn muốn học thử.');
        return;
      }
      if (start <= new Date()) {
        showError('Thời gian học thử phải ở tương lai. Vui lòng chọn thời gian khác.');
        return;
      }

      const scheduledStartTime = `${start.getFullYear()}-${pad(start.getMonth() + 1)}-${pad(start.getDate())}T${pad(start.getHours())}:${pad(start.getMinutes())}:00+07:00`;
      const levelInput = document.getElementById('booking-level');
      const goalsInput = document.getElementById('booking-goals');
      const notesInput = document.getElementById('booking-notes');
      const payload = {
        tutorId: tutor.tutorId,
        scheduledStartTime,
        durationMinutes: TRIAL_DURATION_MINUTES,
        learningMode: 'ONLINE',
        locationDetails: null,
        currentLevel: levelInput ? levelInput.value : 'N5',
        learningGoals: goalsInput ? (goalsInput.value.trim() || null) : null,
        initialMessage: notesInput ? (notesInput.value.trim() || 'Đăng ký học thử trực tuyến 1–1') : 'Đăng ký học thử trực tuyến 1–1'
      };

      continueBtn.disabled = true;
      const origBtnHtml = continueBtn.innerHTML;
      continueBtn.innerHTML = '<span class="material-symbols-outlined animate-spin text-[18px]">progress_activity</span><span>Đang xử lý...</span>';

      try {
        await API.trialBookings.create(payload);
        const end = new Date(start.getTime() + TRIAL_DURATION_MINUTES * 60000);
        const timeText = `${pad(start.getHours())}:${pad(start.getMinutes())}–${pad(end.getHours())}:${pad(end.getMinutes())}`;
        const dayFull = `${DAY_NAMES[start.getDay()]}, ${start.getDate()}/${start.getMonth() + 1}/${start.getFullYear()}`;

        modalBody.innerHTML = `
          <div class="p-8 text-center space-y-4">
            <div class="w-16 h-16 rounded-full bg-emerald-50 text-emerald-600 flex items-center justify-center mx-auto shadow-xs border border-emerald-100">
              <span class="material-symbols-outlined text-[34px]">check_circle</span>
            </div>
            <h3 class="text-lg font-bold text-[#0F2545]">Đã gửi yêu cầu học thử thành công!</h3>
            <p class="text-xs text-on-surface-variant leading-relaxed">
              Yêu cầu học thử <strong>${TRIAL_DURATION_MINUTES} phút</strong> đã được gửi tới <strong>Sensei ${escapeHtml(tutor.fullName || '')}</strong>.
            </p>
            <div class="p-4 bg-[#FAF8F2] rounded-2xl border border-[#ECE8DE] text-xs text-left space-y-2.5">
              <div class="flex items-center justify-between">
                <span class="text-slate-500 font-medium">Thời gian:</span>
                <span class="font-bold text-[#0F2545]">${timeText} • ${dayFull}</span>
              </div>
              <div class="flex items-center justify-between">
                <span class="text-slate-500 font-medium">Trạng thái:</span>
                <span class="inline-flex items-center gap-1 font-bold text-amber-700 bg-amber-50 px-2 py-0.5 rounded-full border border-amber-200">
                  <span class="w-1.5 h-1.5 rounded-full bg-amber-500 animate-pulse"></span>
                  Chờ gia sư xác nhận
                </span>
              </div>
              <div class="flex items-center justify-between">
                <span class="text-slate-500 font-medium">Hình thức:</span>
                <span class="font-semibold text-slate-800">Trực tuyến (Google Meet)</span>
              </div>
              <div class="flex items-center justify-between">
                <span class="text-slate-500 font-medium">Học phí:</span>
                <span class="font-bold text-emerald-600">0 VNĐ (Miễn phí học thử 1-1)</span>
              </div>
            </div>
            <div class="text-[11px] text-slate-500 italic">
              Sensei sẽ xem thời gian bạn chọn và đồng ý hoặc từ chối. Bạn có thể theo dõi trong danh sách lịch học của mình.
            </div>
            <div class="grid grid-cols-2 gap-2.5 pt-2">
              <a href="/pages/learner-connections/" class="py-2.5 px-3 rounded-xl bg-[#006194] hover:bg-[#004b73] text-white text-xs font-bold text-center transition-all shadow-xs flex items-center justify-center gap-1.5">
                <span class="material-symbols-outlined text-[16px]">calendar_month</span>
                <span>Xem lịch học của tôi</span>
              </a>
              <button type="button" onclick="window.closeModal('connect-modal'); location.reload();" class="py-2.5 px-3 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold transition-all">
                Hoàn tất
              </button>
            </div>
          </div>
        `;
        if (modalFooter) modalFooter.style.display = 'none';
      } catch (err) {
        console.error('Lỗi khi gửi yêu cầu học thử:', err);
        showError(friendlyError(err, 'Không thể gửi yêu cầu lúc này. Vui lòng thử lại sau.'));
        continueBtn.disabled = false;
        continueBtn.innerHTML = origBtnHtml;
      }
    }

    // Nút "Đặt lịch học thử 1-1" ở cột bên phải
    const openBtn = document.getElementById('open-booking-btn');
    if (openBtn) {
      openBtn.addEventListener('click', () => requireLogin(openTrialModal, 'connect=true'));
    }

    // Liên kết tới thẳng phần lịch rảnh
    const urlParams = new URLSearchParams(window.location.search);
    if (window.location.hash === '#weekly-booking-calendar-section' || urlParams.get('section') === 'schedule') {
      requestAnimationFrame(() => setTimeout(scrollToCalendar, 150));
    }

    // connect=true (từ nút "Đặt lịch học thử" ở trang tìm gia sư): mở sẵn modal học thử
    if (urlParams.get('connect') === 'true') {
      setTimeout(() => requireLogin(openTrialModal), 350);
    }

  } catch (err) {
    console.error('Lỗi khi tải chi tiết gia sư:', err);
    container.innerHTML = `
      <div class="p-12 text-center bg-white rounded-2xl border border-[#E5DECB] space-y-3">
        <h3 class="text-lg font-bold text-[#0F2545]">Không thể tải hồ sơ gia sư</h3>
        <p class="text-sm text-on-surface-variant">Vui lòng kiểm tra lại đường truyền hoặc quay lại danh sách tìm kiếm.</p>
        <a href="/pages/tutor-discovery/" class="inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl bg-primary text-white text-sm font-semibold">Quay lại danh sách</a>
      </div>
    `;
  }
});
