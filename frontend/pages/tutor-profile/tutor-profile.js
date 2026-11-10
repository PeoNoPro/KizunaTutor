/**
 * KizunaTutor - Tutor Profile Detail Page Logic (UC-31)
 * Hỗ trợ URL ngắn /gs/:tutorCode (ví dụ: /gs/10001), ?code=10001 hoặc ?id=<UUID>.
 * Lấy thông tin chi tiết từ Database Supabase qua REST API.
 */

import { API } from '../../js/api.js';
import { formatVND } from '../../js/main.js';
import { openModal, closeModal } from '../../components/modal/modal.js';


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
                <div class="flex items-center gap-2.5 flex-wrap">
                  <h1 class="text-2xl md:text-3xl font-bold text-[#0F2545]">${tutor.fullName}</h1>
                  ${tutor.tutorCode ? `
                    <span class="inline-flex items-center px-2.5 py-0.5 rounded-md bg-sky-50 text-primary text-xs font-bold border border-sky-200">
                      Mã: #${tutor.tutorCode}
                    </span>
                  ` : ''}
                  <span class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-emerald-50 text-emerald-700 text-xs font-semibold border border-emerald-200">
                    <span class="material-symbols-outlined text-[15px] text-emerald-600">verified</span> Đã xác thực hồ sơ
                  </span>
                </div>

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
                    <span>${tutor.location || 'Hà Nội, Việt Nam'}</span>
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

          <!-- Lịch giảng dạy & Đặt lịch học thử trực tuyến 1-1 (Preply-style Unified Weekly Calendar) -->
          <div id="weekly-booking-calendar-section" class="bg-white border border-[#E5DECB] rounded-2xl p-6 md:p-8 shadow-sm space-y-5 scroll-mt-28 transition-all duration-300">
            
            <h3 class="text-lg font-bold text-[#0F2545] flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">calendar_month</span>
              Lịch rảnh giảng dạy trong tuần
            </h3>

            <!-- Thanh điều hướng tuần (Tuần trước/sau & Khoảng ngày & Tab 25m/50m) -->
            <div class="flex items-center justify-between flex-wrap gap-4 pb-2 border-b border-slate-100">
              <div class="flex items-center gap-2.5 sm:gap-3">
                <div class="flex items-center gap-1">
                  <button type="button" id="btn-weekly-prev" class="w-8 h-8 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 flex items-center justify-center text-slate-600 transition-colors shadow-2xs cursor-pointer disabled:opacity-30 disabled:cursor-not-allowed" aria-label="Tuần trước">
                    <span class="material-symbols-outlined text-[18px]">chevron_left</span>
                  </button>
                  <button type="button" id="btn-weekly-next" class="w-8 h-8 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 flex items-center justify-center text-slate-600 transition-colors shadow-2xs cursor-pointer" aria-label="Tuần sau">
                    <span class="material-symbols-outlined text-[18px]">chevron_right</span>
                  </button>
                </div>
                <span id="weekly-calendar-range-label" class="text-xs sm:text-sm md:text-base font-bold text-[#0F2545]">
                  <!-- Render động qua JS -->
                </span>
              </div>

              <!-- Thời lượng buổi học thử: 30 phút -->
              <div class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-100 border border-slate-200 text-xs font-bold text-[#0F2545]">
                <span class="material-symbols-outlined text-[16px] text-primary">schedule</span>
                <span>Học thử 30 phút</span>
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
            <div class="space-y-3">
              <button type="button" id="open-booking-btn" class="w-full py-3.5 px-4 rounded-xl bg-[#006194] hover:bg-[#004b73] text-white font-bold text-sm shadow-md hover:shadow-lg transition-all active:scale-[0.98] flex items-center justify-center gap-2">
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


    // === LOGIC CHO MODAL VÀ LỊCH ĐẶT HỌC THỬ TRỰC TUYẾN 1-1 ===
    let selectedDurationMinutes = 30;
    let selectedSlot = null;
    let weekOffset = 0; // 0 = tuần này
    let selectedDayOffset = 0; // 0 = Thứ 2, 1 = Thứ 3, ..., 6 = Chủ nhật

    // Bắt đầu nghiêm ngặt từ Thứ hai đến Chủ nhật
    const WEEKDAY_COLUMNS = [
      { dow: 2, shortName: 'Thứ 2', minName: 'T2', fullName: 'Thứ hai' },
      { dow: 3, shortName: 'Thứ 3', minName: 'T3', fullName: 'Thứ ba' },
      { dow: 4, shortName: 'Thứ 4', minName: 'T4', fullName: 'Thứ tư' },
      { dow: 5, shortName: 'Thứ 5', minName: 'T5', fullName: 'Thứ năm' },
      { dow: 6, shortName: 'Thứ 6', minName: 'T6', fullName: 'Thứ sáu' },
      { dow: 7, shortName: 'Thứ 7', minName: 'T7', fullName: 'Thứ bảy' },
      { dow: 1, shortName: 'Chủ nhật', minName: 'CN', fullName: 'Chủ nhật' }
    ];

    // Tính Thứ hai của tuần hiện tại (bắt đầu từ Thứ hai -> Chủ nhật)
    function getMondayOfCurrentWeek() {
      const d = new Date();
      const jsDay = d.getDay(); // 0 = Chủ nhật, 1 = Thứ 2, ..., 6 = Thứ 7
      const distToMonday = jsDay === 0 ? -6 : 1 - jsDay;
      return new Date(d.getFullYear(), d.getMonth(), d.getDate() + distToMonday);
    }

    const currentMonday = getMondayOfCurrentWeek();

    // Tự động tìm ngày đầu tiên có lịch rảnh khả dụng trong tuần để chọn mặc định
    function getInitialDayOffset() {
      const today = new Date();
      const startOfToday = new Date(today.getFullYear(), today.getMonth(), today.getDate()).getTime();
      const nowMinutes = today.getHours() * 60 + today.getMinutes();

      for (let offset = 0; offset < 7; offset++) {
        const d = new Date(currentMonday.getFullYear(), currentMonday.getMonth(), currentMonday.getDate() + offset);
        const startOfD = new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
        if (startOfD < startOfToday) continue; // Bỏ qua ngày quá khứ

        const isToday = startOfD === startOfToday;
        const dow = WEEKDAY_COLUMNS[offset].dow;
        const dayAvails = (tutor.availabilities || []).filter(a => Number(a.dayOfWeek) === dow);

        for (const avail of dayAvails) {
          if (!avail.startTime || !avail.endTime) continue;
          const [startH, startM] = avail.startTime.split(':').map(Number);
          const [endH, endM] = avail.endTime.split(':').map(Number);
          const startTotal = startH * 60 + startM;
          const endTotal = endH * 60 + endM;

          for (let time = startTotal; time + selectedDurationMinutes <= endTotal; time += 30) {
            if (isToday && time <= nowMinutes + 15) continue;
            return offset;
          }
        }
      }
      const todayJsDay = today.getDay();
      return todayJsDay === 0 ? 6 : todayJsDay - 1;
    }

    selectedDayOffset = getInitialDayOffset();

    function getSelectedDate() {
      const baseMonday = new Date(currentMonday.getFullYear(), currentMonday.getMonth(), currentMonday.getDate() + (weekOffset * 7));
      const d = new Date(baseMonday.getFullYear(), baseMonday.getMonth(), baseMonday.getDate() + selectedDayOffset);
      return d;
    }

    // Avatar gia sư trong modal (ưu tiên lấy từ database)
    const modalAvatar = document.getElementById('booking-modal-avatar');
    const modalAvatarContainer = document.getElementById('booking-modal-avatar-container');
    if (tutor.avatarUrl) {
      if (modalAvatar) {
        modalAvatar.src = tutor.avatarUrl;
        modalAvatar.onerror = () => {
          if (modalAvatarContainer) {
            modalAvatarContainer.innerHTML = `
              <div class="w-12 h-12 rounded-2xl ${tutorStyle.bg} ${tutorStyle.text} font-bold flex items-center justify-center border border-slate-200 shadow-xs text-sm">
                ${tutorInitials}
              </div>
            `;
          }
        };
      }
    } else {
      if (modalAvatarContainer) {
        modalAvatarContainer.innerHTML = `
          <div class="w-12 h-12 rounded-2xl ${tutorStyle.bg} ${tutorStyle.text} font-bold flex items-center justify-center border border-slate-200 shadow-xs text-sm">
            ${tutorInitials}
          </div>
        `;
      }
    }
    const modalSubtitle = document.getElementById('booking-modal-subtitle');
    if (modalSubtitle && tutor.fullName) {
      modalSubtitle.innerText = `Trao đổi về lộ trình và học thử 1-1 cùng Sensei ${tutor.fullName}`;
    }

    const continueBtn = document.getElementById('btn-booking-continue');
    const modalBody = document.getElementById('booking-modal-body');
    const modalFooter = document.getElementById('booking-modal-footer');
    const errorAlert = document.getElementById('booking-error-alert');
    const errorMsgEl = document.getElementById('booking-error-message');

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
      if (errorAlert) {
        errorAlert.classList.add('hidden');
      }
    }

    function updateContinueButtonState() {
      const formFields = document.getElementById('booking-form-fields');
      const footerEl = document.getElementById('booking-modal-footer');

      if (!selectedSlot) {
        if (formFields) formFields.classList.add('hidden');
        if (footerEl) footerEl.classList.add('hidden');
        if (continueBtn) {
          continueBtn.disabled = true;
          continueBtn.className = 'w-full py-3.5 px-4 rounded-2xl text-sm font-bold transition-all flex items-center justify-center gap-2 bg-slate-200 text-slate-400 cursor-not-allowed shadow-none';
        }
      } else {
        if (formFields) formFields.classList.remove('hidden');
        if (footerEl) footerEl.classList.remove('hidden');
        if (continueBtn) {
          continueBtn.disabled = false;
          continueBtn.className = 'w-full py-3.5 px-4 rounded-2xl text-sm font-bold transition-all flex items-center justify-center gap-2 bg-[#006194] text-white shadow-md hover:bg-[#004b73] active:scale-[0.99] cursor-pointer';
        }
      }
    }

    // Modal: 7 ngày trong tuần
    const weekLabelEl = document.getElementById('current-week-label');
    const daysContainer = document.getElementById('days-picker-container');
    const prevWeekBtn = document.getElementById('btn-prev-week');
    const nextWeekBtn = document.getElementById('btn-next-week');

    function renderDays() {
      if (!daysContainer) return;
      daysContainer.innerHTML = '';

      if (prevWeekBtn) {
        prevWeekBtn.disabled = weekOffset <= 0;
      }

      const baseMonday = new Date(currentMonday.getFullYear(), currentMonday.getMonth(), currentMonday.getDate() + (weekOffset * 7));
      const sundayDate = new Date(baseMonday.getFullYear(), baseMonday.getMonth(), baseMonday.getDate() + 6);

      if (weekLabelEl) {
        if (baseMonday.getMonth() === sundayDate.getMonth()) {
          weekLabelEl.innerText = `${baseMonday.getDate()} – ${sundayDate.getDate()} Tháng ${baseMonday.getMonth() + 1}, ${baseMonday.getFullYear()}`;
        } else {
          weekLabelEl.innerText = `${baseMonday.getDate()} Thg ${baseMonday.getMonth() + 1} – ${sundayDate.getDate()} Thg ${sundayDate.getMonth() + 1}, ${baseMonday.getFullYear()}`;
        }
      }

      const today = new Date();
      const startOfToday = new Date(today.getFullYear(), today.getMonth(), today.getDate()).getTime();
      const nowMinutes = today.getHours() * 60 + today.getMinutes();

      for (let i = 0; i < 7; i++) {
        const d = new Date(baseMonday.getFullYear(), baseMonday.getMonth(), baseMonday.getDate() + i);
        const startOfD = new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
        const isPastDay = startOfD < startOfToday;
        const isToday = startOfD === startOfToday;
        const col = WEEKDAY_COLUMNS[i];
        const isSelected = (i === selectedDayOffset);

        // Tính xem ngày này có khung giờ rảnh nào khả dụng không
        let hasAvailableSlots = false;
        if (!isPastDay) {
          const dayAvails = (tutor.availabilities || []).filter(a => Number(a.dayOfWeek) === col.dow);
          for (const avail of dayAvails) {
            if (!avail.startTime || !avail.endTime) continue;
            const [startH, startM] = avail.startTime.split(':').map(Number);
            const [endH, endM] = avail.endTime.split(':').map(Number);
            const startTotal = startH * 60 + startM;
            const endTotal = endH * 60 + endM;

            for (let time = startTotal; time + selectedDurationMinutes <= endTotal; time += 30) {
              if (isToday && time <= nowMinutes + 15) {
                continue;
              }
              hasAvailableSlots = true;
              break;
            }
            if (hasAvailableSlots) break;
          }
        }

        const btn = document.createElement('button');
        btn.type = 'button';
        
        let btnClasses = 'day-picker-btn flex flex-col items-center py-2 px-1 rounded-2xl border transition-all cursor-pointer ';
        let minNameClass = 'text-[11px] leading-tight ';
        let dateClass = 'text-sm mt-1 leading-tight ';
        let dotClass = 'w-1.5 h-1.5 rounded-full mt-1 ';

        if (isSelected) {
          if (hasAvailableSlots) {
            btnClasses += 'border-2 border-[#006194] bg-sky-50 text-[#006194] font-bold shadow-2xs';
            minNameClass += 'font-bold text-[#006194]';
            dateClass += 'font-extrabold text-[#006194]';
            dotClass += 'bg-emerald-500';
          } else {
            btnClasses += 'border-2 border-slate-300 bg-slate-100 text-slate-500 font-bold shadow-2xs';
            minNameClass += 'font-semibold text-slate-400';
            dateClass += 'font-bold text-slate-600';
            dotClass += 'bg-transparent';
          }
        } else {
          if (isPastDay) {
            btnClasses += 'border-transparent hover:bg-slate-50 text-slate-300 opacity-50';
            minNameClass += 'text-slate-300 font-normal';
            dateClass += 'text-slate-300 font-normal';
            dotClass += 'bg-transparent';
          } else if (hasAvailableSlots) {
            btnClasses += 'border-transparent hover:bg-slate-50 text-slate-800 font-medium';
            minNameClass += 'text-slate-600 font-semibold';
            dateClass += 'text-slate-900 font-bold';
            dotClass += 'bg-emerald-500';
          } else {
            btnClasses += 'border-transparent hover:bg-slate-50 text-slate-400 font-normal';
            minNameClass += 'text-slate-400 font-normal';
            dateClass += 'text-slate-400 font-normal';
            dotClass += 'bg-transparent';
          }
        }

        btn.className = btnClasses;
        btn.innerHTML = `
          <span class="${minNameClass}">${col.minName}</span>
          <span class="${dateClass}">${d.getDate()}</span>
          <span class="${dotClass}"></span>
        `;
        btn.addEventListener('click', () => {
          selectedDayOffset = i;
          selectedSlot = null;
          renderDays();
          renderAvailableSlots(getSelectedDate(), selectedDurationMinutes);
        });
        daysContainer.appendChild(btn);
      }
    }

    if (prevWeekBtn) {
      prevWeekBtn.addEventListener('click', () => {
        if (weekOffset > 0) {
          weekOffset--;
          selectedSlot = null;
          renderDays();
          renderAvailableSlots(getSelectedDate(), selectedDurationMinutes);
        }
      });
    }
    if (nextWeekBtn) {
      nextWeekBtn.addEventListener('click', () => {
        weekOffset++;
        selectedSlot = null;
        renderDays();
        renderAvailableSlots(getSelectedDate(), selectedDurationMinutes);
      });
    }

    // Modal: Render động các khung giờ theo lịch rảnh thực tế của gia sư
    function renderAvailableSlots(targetDate, durationMinutes, preselectSlot = null) {
      selectedSlot = preselectSlot;
      updateContinueButtonState();

      const morningGrid = document.getElementById('morning-slots-grid');
      const afternoonGrid = document.getElementById('afternoon-slots-grid');
      const eveningGrid = document.getElementById('evening-slots-grid');
      const emptyStateEl = document.getElementById('booking-empty-state');
      const emptyTitleEl = document.getElementById('booking-empty-title');

      const jsDay = targetDate.getDay();
      const backendDow = jsDay === 0 ? 1 : jsDay + 1;

      const dayAvails = (tutor.availabilities || []).filter(a => Number(a.dayOfWeek) === backendDow);

      const today = new Date();
      const startOfToday = new Date(today.getFullYear(), today.getMonth(), today.getDate()).getTime();
      const startOfTarget = new Date(targetDate.getFullYear(), targetDate.getMonth(), targetDate.getDate()).getTime();
      const isPastDay = startOfTarget < startOfToday;
      const isToday = startOfTarget === startOfToday;
      const nowMinutes = today.getHours() * 60 + today.getMinutes();

      const slotsSet = new Set();

      if (!isPastDay) {
        dayAvails.forEach(avail => {
          if (!avail.startTime || !avail.endTime) return;
          const [startH, startM] = avail.startTime.split(':').map(Number);
          const [endH, endM] = avail.endTime.split(':').map(Number);
          const startTotal = startH * 60 + startM;
          const endTotal = endH * 60 + endM;

          for (let time = startTotal; time + durationMinutes <= endTotal; time += 30) {
            if (isToday && time <= nowMinutes + 15) {
              continue;
            }
            const h = Math.floor(time / 60);
            const m = time % 60;
            const slotStr = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
            slotsSet.add(slotStr);
          }
        });
      }

      const allSlots = Array.from(slotsSet).sort();

      const morningSection = document.getElementById('morning-slots-section');
      const afternoonSection = document.getElementById('afternoon-slots-section');
      const eveningSection = document.getElementById('evening-slots-section');

      if (allSlots.length === 0) {
        if (emptyStateEl) {
          emptyStateEl.classList.remove('hidden');
          if (emptyTitleEl) {
            emptyTitleEl.innerText = `Không có khung giờ ${durationMinutes} phút khả dụng`;
          }
        }
        if (morningSection) morningSection.classList.add('hidden');
        if (afternoonSection) afternoonSection.classList.add('hidden');
        if (eveningSection) eveningSection.classList.add('hidden');
        return;
      }

      if (emptyStateEl) {
        emptyStateEl.classList.add('hidden');
      }

      // Phân nhóm theo 3 buổi (Sáng, Chiều, Tối)
      const morningSlots = allSlots.filter(s => parseInt(s.split(':')[0], 10) < 12);
      const afternoonSlots = allSlots.filter(s => {
        const h = parseInt(s.split(':')[0], 10);
        return h >= 12 && h < 18;
      });
      const eveningSlots = allSlots.filter(s => parseInt(s.split(':')[0], 10) >= 18);

      function renderSlotSection(sectionEl, gridEl, slots) {
        if (!sectionEl || !gridEl) return;
        if (!slots || slots.length === 0) {
          sectionEl.classList.add('hidden');
          gridEl.innerHTML = '';
          return;
        }
        sectionEl.classList.remove('hidden');
        gridEl.innerHTML = '';
        slots.forEach(slot => {
          const isSlotActive = (selectedSlot === slot);
          const btn = document.createElement('button');
          btn.type = 'button';
          btn.className = isSlotActive
            ? 'slot-btn py-2.5 px-2 text-center text-xs font-bold rounded-xl border-2 border-[#006194] bg-[#006194] text-white shadow-xs cursor-pointer'
            : 'slot-btn py-2.5 px-2 text-center text-xs font-semibold rounded-xl border border-slate-300 hover:border-slate-400 hover:bg-slate-50 transition-all text-slate-800 bg-white cursor-pointer shadow-2xs';
          btn.innerText = slot;
          btn.addEventListener('click', () => {
            document.querySelectorAll('.slot-btn').forEach(b => {
              b.className = 'slot-btn py-2.5 px-2 text-center text-xs font-semibold rounded-xl border border-slate-300 hover:border-slate-400 hover:bg-slate-50 transition-all text-slate-800 bg-white cursor-pointer shadow-2xs';
            });
            btn.className = 'slot-btn py-2.5 px-2 text-center text-xs font-bold rounded-xl border-2 border-[#006194] bg-[#006194] text-white shadow-xs cursor-pointer';
            selectedSlot = slot;
            updateContinueButtonState();
          });
          gridEl.appendChild(btn);
        });
      }

      renderSlotSection(morningSection, morningGrid, morningSlots);
      renderSlotSection(afternoonSection, afternoonGrid, afternoonSlots);
      renderSlotSection(eveningSection, eveningGrid, eveningSlots);
    }

    // Modal: Thời lượng cố định 30 phút theo quy định học thử
    renderDays();
    renderAvailableSlots(getSelectedDate(), selectedDurationMinutes);

    // Modal: Nút gửi yêu cầu học thử
    if (continueBtn && modalBody) {
      continueBtn.addEventListener('click', async () => {
        clearError();

        // 1. Kiểm tra xác thực đăng nhập
        const token = localStorage.getItem('kizuna_token') || localStorage.getItem('token');
        if (!token) {
          const tutorDisplay = tutor.fullName ? `Sensei ${tutor.fullName}` : 'gia sư';
          if (typeof window.openLoginModal === 'function') {
            window.openLoginModal({
              tutorName: tutorDisplay,
              onSuccess: () => {
                openModal('connect-modal');
              }
            });
            return;
          }
          const currentPath = window.location.pathname + window.location.search;
          window.location.href = `/login?redirect=${encodeURIComponent(currentPath)}`;
          return;
        }

        // 2. Tính toán ngày giờ bắt đầu
        if (!selectedSlot) {
          showError('Vui lòng chọn một khung giờ học khả dụng');
          return;
        }
        const targetDate = getSelectedDate();
        const [hours, minutes] = selectedSlot.split(':').map(Number);
        targetDate.setHours(hours, minutes, 0, 0);

        if (targetDate <= new Date()) {
          showError('Thời gian bắt đầu học phải ở tương lai. Vui lòng chọn khung giờ khác.');
          return;
        }

        // 3. Chuẩn bị payload đăng ký học thử trực tuyến 1-1
        const pad = (n) => String(n).padStart(2, '0');
        const scheduledStartTime = `${targetDate.getFullYear()}-${pad(targetDate.getMonth() + 1)}-${pad(targetDate.getDate())}T${pad(hours)}:${pad(minutes)}:00+07:00`;

        const levelInput = document.getElementById('booking-level');
        const goalsInput = document.getElementById('booking-goals');
        const notesInput = document.getElementById('booking-notes');

        const payload = {
          tutorId: tutor.tutorId,
          scheduledStartTime: scheduledStartTime,
          durationMinutes: selectedDurationMinutes,
          learningMode: 'ONLINE',
          locationDetails: null,
          currentLevel: levelInput ? levelInput.value : 'N5',
          learningGoals: goalsInput ? (goalsInput.value.trim() || null) : null,
          initialMessage: notesInput ? (notesInput.value.trim() || 'Đăng ký học thử trực tuyến 1–1') : 'Đăng ký học thử trực tuyến 1–1'
        };

        // 4. Gọi API gửi yêu cầu
        continueBtn.disabled = true;
        const origBtnHtml = continueBtn.innerHTML;
        continueBtn.innerHTML = '<span class="material-symbols-outlined animate-spin text-[18px]">progress_activity</span><span>Đang xử lý...</span>';

        try {
          await API.trialBookings.create(payload);

          // Render màn hình thành công bên trong modal
          const DAY_NAMES = ['Chủ nhật', 'Thứ hai', 'Thứ ba', 'Thứ tư', 'Thứ năm', 'Thứ sáu', 'Thứ bảy'];
          const dayFull = `${DAY_NAMES[targetDate.getDay()]}, ${targetDate.getDate()}/${targetDate.getMonth() + 1}/${targetDate.getFullYear()}`;

          modalBody.innerHTML = `
            <div class="p-8 text-center space-y-4">
              <div class="w-16 h-16 rounded-full bg-emerald-50 text-emerald-600 flex items-center justify-center mx-auto shadow-xs border border-emerald-100">
                <span class="material-symbols-outlined text-[34px]">check_circle</span>
              </div>
              <h3 class="text-lg font-bold text-[#0F2545]">Đã gửi yêu cầu học thử thành công!</h3>
              <p class="text-xs text-on-surface-variant leading-relaxed">
                Yêu cầu học thử <strong>${selectedDurationMinutes} phút</strong> đã được gửi tới <strong>Sensei ${tutor.fullName}</strong>.
              </p>
              <div class="p-4 bg-[#FAF8F2] rounded-2xl border border-[#ECE8DE] text-xs text-left space-y-2.5">
                <div class="flex items-center justify-between">
                  <span class="text-slate-500 font-medium">Khung giờ:</span>
                  <span class="font-bold text-[#0F2545]">${selectedSlot} • ${dayFull}</span>
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
                Sensei sẽ kiểm tra lịch và phản hồi yêu cầu. Bạn có thể theo dõi tiến độ trong danh sách lịch học của mình.
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
          if (modalFooter) {
            modalFooter.style.display = 'none';
          }

        } catch (err) {
          console.error('Lỗi khi gửi yêu cầu học thử:', err);
          if (err.status === 409) {
            showError('Khung giờ này vừa có người đăng ký. Vui lòng chọn khung giờ khác.');
          } else if (err.status === 401 || err.status === 403) {
            showError('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
          } else if (err.name === 'TypeError' || err.message?.toLowerCase().includes('fetch') || err.name === 'TimeoutError') {
            showError('Không thể kết nối đến máy chủ. Vui lòng kiểm tra lại kết nối mạng hoặc thử lại sau.');
          } else {
            showError(err.message && !err.message.includes('fetch') ? err.message : 'Không thể gửi yêu cầu lúc này. Vui lòng thử lại sau.');
          }
          continueBtn.disabled = false;
          continueBtn.innerHTML = origBtnHtml;
        }
      });
    }

    // === LOGIC CHO BẢNG LỊCH TUẦN TRÊN TRANG (PREPLY STYLE TABLE) ===
    let weeklyDurationMinutes = 30;
    let weeklyOffset = 0;
    let isWeeklySlotsExpanded = false;

    function renderWeeklyCalendar() {
      const gridEl = document.getElementById('weekly-calendar-grid');
      const rangeLabelEl = document.getElementById('weekly-calendar-range-label');
      const prevBtn = document.getElementById('btn-weekly-prev');
      const expandWrapper = document.getElementById('weekly-expand-wrapper');
      const expandText = document.getElementById('toggle-weekly-slots-text');
      const expandIcon = document.getElementById('toggle-weekly-slots-icon');

      if (!gridEl) return;

      if (prevBtn) {
        prevBtn.disabled = weeklyOffset <= 0;
      }

      const baseMonday = new Date(currentMonday.getFullYear(), currentMonday.getMonth(), currentMonday.getDate() + (weeklyOffset * 7));
      const sundayDate = new Date(baseMonday.getFullYear(), baseMonday.getMonth(), baseMonday.getDate() + 6);

      if (rangeLabelEl) {
        if (baseMonday.getMonth() === sundayDate.getMonth()) {
          rangeLabelEl.innerText = `${baseMonday.getDate()} – ${sundayDate.getDate()} Tháng ${baseMonday.getMonth() + 1}, ${baseMonday.getFullYear()}`;
        } else {
          rangeLabelEl.innerText = `${baseMonday.getDate()} Thg ${baseMonday.getMonth() + 1} – ${sundayDate.getDate()} Thg ${sundayDate.getMonth() + 1}, ${baseMonday.getFullYear()}`;
        }
      }

      const today = new Date();
      const startOfToday = new Date(today.getFullYear(), today.getMonth(), today.getDate()).getTime();
      const nowMinutes = today.getHours() * 60 + today.getMinutes();

      let hasAnyColumnExceedLimit = false;
      const MAX_PREVIEW_SLOTS = 8;

      const columnsData = WEEKDAY_COLUMNS.map((col, colIndex) => {
        const colDate = new Date(baseMonday.getFullYear(), baseMonday.getMonth(), baseMonday.getDate() + colIndex);
        const startOfCol = new Date(colDate.getFullYear(), colDate.getMonth(), colDate.getDate()).getTime();
        const isPastDay = startOfCol < startOfToday;
        const isToday = startOfCol === startOfToday;

        const pad = (n) => String(n).padStart(2, '0');
        const dateStr = `${pad(colDate.getDate())}/${pad(colDate.getMonth() + 1)}/${colDate.getFullYear()}`;

        const dayAvails = (tutor.availabilities || []).filter(a => Number(a.dayOfWeek) === col.dow);
        const slotsSet = new Set();

        if (!isPastDay) {
          dayAvails.forEach(avail => {
            if (!avail.startTime || !avail.endTime) return;
            const [startH, startM] = avail.startTime.split(':').map(Number);
            const [endH, endM] = avail.endTime.split(':').map(Number);
            const startTotal = startH * 60 + startM;
            const endTotal = endH * 60 + endM;

            for (let time = startTotal; time + weeklyDurationMinutes <= endTotal; time += 30) {
              if (isToday && time <= nowMinutes + 15) {
                continue;
              }
              const h = Math.floor(time / 60);
              const m = time % 60;
              slotsSet.add(`${pad(h)}:${pad(m)}`);
            }
          });
        }

        const sortedSlots = Array.from(slotsSet).sort();
        if (sortedSlots.length > MAX_PREVIEW_SLOTS) {
          hasAnyColumnExceedLimit = true;
        }

        const displayedSlots = isWeeklySlotsExpanded ? sortedSlots : sortedSlots.slice(0, MAX_PREVIEW_SLOTS);

        return {
          col,
          colDate,
          dateStr,
          isToday,
          isPastDay,
          allSlotsCount: sortedSlots.length,
          displayedSlots
        };
      });

      if (expandWrapper) {
        if (hasAnyColumnExceedLimit) {
          expandWrapper.classList.remove('hidden');
          if (expandText) {
            expandText.innerText = isWeeklySlotsExpanded ? 'Thu gọn lịch trình' : 'Xem toàn bộ lịch trình';
          }
          if (expandIcon) {
            expandIcon.innerText = isWeeklySlotsExpanded ? 'expand_less' : 'expand_more';
          }
        } else {
          expandWrapper.classList.add('hidden');
        }
      }

      gridEl.innerHTML = columnsData.map((data, colIndex) => {
        const hasSlots = data.allSlotsCount > 0;
        const accentBarClass = hasSlots ? 'bg-emerald-500' : 'bg-slate-200';

        const slotsHtml = data.displayedSlots.map(slot => {
          return `
            <button type="button"
              class="weekly-slot-btn w-full py-1.5 px-1 rounded-lg text-xs sm:text-sm font-semibold text-[#0F2545] hover:text-[#006194] hover:bg-sky-50 underline decoration-slate-300 hover:decoration-[#006194] underline-offset-4 transition-all text-center cursor-pointer active:scale-95"
              data-col-index="${colIndex}"
              data-slot="${slot}"
              title="Đặt học thử lúc ${slot} ${data.col.fullName} ngày ${data.dateStr}">
              ${slot}
            </button>
          `;
        }).join('');

        return `
          <div class="flex flex-col items-center min-w-[76px] sm:min-w-[82px] text-center">
            <!-- Vạch trạng thái trên đầu cột -->
            <div class="h-1 w-full rounded-full ${accentBarClass} mb-2.5"></div>
            
            <!-- Tiêu đề ngày & Ngày trong tháng -->
            <div class="pb-2 w-full">
              <span class="block text-xs font-semibold ${data.isToday ? 'text-primary font-bold' : (data.isPastDay ? 'text-slate-300' : 'text-slate-600')}">
                ${data.col.shortName}
              </span>
              <span class="block text-sm sm:text-base font-bold mt-0.5 ${data.isToday ? 'text-primary' : (data.isPastDay ? 'text-slate-300' : 'text-slate-900')}">
                ${data.colDate.getDate()}
              </span>
            </div>

            <!-- Danh sách các khung giờ trống dạng cột dọc -->
            <div class="w-full flex flex-col items-center gap-1 pt-1 min-h-[50px]">
              ${slotsHtml}
            </div>
          </div>
        `;
      }).join('');

      // Gắn sự kiện click slot trên bảng tuần: Mở popup modal đặt lịch với mốc giờ tương ứng đã chọn
      gridEl.querySelectorAll('.weekly-slot-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          const colIndex = Number(btn.dataset.colIndex);
          const slot = btn.dataset.slot;

          // Kiểm tra xác thực
          const token = localStorage.getItem('kizuna_token') || localStorage.getItem('token');
          if (!token) {
            const tutorDisplay = tutor.fullName ? `Sensei ${tutor.fullName}` : 'gia sư';
            if (typeof window.openLoginModal === 'function') {
              window.openLoginModal({
                tutorName: tutorDisplay,
                onSuccess: () => {
                  openBookingModalWithSlot(colIndex, slot);
                }
              });
              return;
            }
            const currentPath = window.location.pathname + window.location.search;
            window.location.href = `/login?redirect=${encodeURIComponent(currentPath)}`;
            return;
          }

          openBookingModalWithSlot(colIndex, slot);
        });
      });
    }

    function openBookingModalWithSlot(colIndex, slot) {
      weekOffset = weeklyOffset;
      selectedDayOffset = colIndex;
      selectedDurationMinutes = 30;

      renderDays();
      renderAvailableSlots(getSelectedDate(), selectedDurationMinutes, slot);
      openModal('connect-modal');
    }

    // Điều hướng tuần trên bảng tuần
    const prevWkBtn = document.getElementById('btn-weekly-prev');
    const nextWkBtn = document.getElementById('btn-weekly-next');

    if (prevWkBtn) {
      prevWkBtn.addEventListener('click', () => {
        if (weeklyOffset > 0) {
          weeklyOffset--;
          renderWeeklyCalendar();
        }
      });
    }

    if (nextWkBtn) {
      nextWkBtn.addEventListener('click', () => {
        weeklyOffset++;
        renderWeeklyCalendar();
      });
    }

    const toggleWkExpandBtn = document.getElementById('btn-toggle-weekly-slots');
    if (toggleWkExpandBtn) {
      toggleWkExpandBtn.addEventListener('click', () => {
        isWeeklySlotsExpanded = !isWeeklySlotsExpanded;
        renderWeeklyCalendar();
      });
    }

    // Khởi tạo bảng lịch tuần
    renderWeeklyCalendar();

    function openBookingModalWithoutPreselect() {
      selectedSlot = null;
      selectedDayOffset = getInitialDayOffset();
      renderDays();
      renderAvailableSlots(getSelectedDate(), selectedDurationMinutes);
      openModal('connect-modal');
    }

    // Nút "Đặt lịch học thử 1-1" ở cột bên phải: Mở popup modal đặt lịch
    const openBtn = document.getElementById('open-booking-btn');
    if (openBtn) {
      openBtn.addEventListener('click', () => {
        const token = localStorage.getItem('kizuna_token') || localStorage.getItem('token');
        if (!token) {
          const tutorDisplay = tutor.fullName ? `Sensei ${tutor.fullName}` : 'gia sư';
          if (typeof window.openLoginModal === 'function') {
            window.openLoginModal({
              tutorName: tutorDisplay,
              onSuccess: () => {
                openBookingModalWithoutPreselect();
              }
            });
            return;
          }
          const currentPath = window.location.pathname + window.location.search;
          const returnUrl = currentPath.includes('connect=true')
            ? currentPath
            : (currentPath + (currentPath.includes('?') ? '&' : '?') + 'connect=true');
          window.location.href = `/login?redirect=${encodeURIComponent(returnUrl)}`;
          return;
        }
        openBookingModalWithoutPreselect();
      });
    }

    // Tự động mở modal kết nối nếu có cờ connect=true
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get('connect') === 'true') {
      const token = localStorage.getItem('kizuna_token') || localStorage.getItem('token');
      if (token) {
        setTimeout(() => {
          openBookingModalWithoutPreselect();
        }, 350);
      } else {
        setTimeout(() => {
          const tutorDisplay = tutor.fullName ? `Sensei ${tutor.fullName}` : 'gia sư';
          if (typeof window.openLoginModal === 'function') {
            window.openLoginModal({
              tutorName: tutorDisplay,
              onSuccess: () => {
                openBookingModalWithoutPreselect();
              }
            });
          }
        }, 350);
      }
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
