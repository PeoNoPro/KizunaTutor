/**
 * KizunaTutor - Tutor Profile Detail Page Logic (UC-31)
 * Hỗ trợ URL ngắn /gs/:tutorCode (ví dụ: /gs/10001), ?code=10001 hoặc ?id=<UUID>.
 * Lấy thông tin chi tiết từ Database Supabase qua REST API.
 */

import { API } from '../../js/api.js';
import { formatVND } from '../../js/main.js';
import { openModal, closeModal } from '../../components/modal/modal.js';
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
                  Lịch rảnh giảng dạy
                </h3>
                <p class="text-xs text-on-surface-variant leading-relaxed">
                  Giờ còn trống trong 4 tuần tới. Bấm vào một giờ để đặt học thử 30 phút. Muốn học lâu dài theo lịch cố định, bấm "Kết nối với gia sư".
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
              <button type="button" id="open-booking-btn" class="w-full h-12 px-4 rounded-xl bg-[#006194] hover:bg-[#004b73] border border-[#004b73] text-white font-semibold text-[15px] transition-all active:scale-[0.98] flex items-center justify-center cursor-pointer">
                Đặt lịch học thử 1-1
              </button>
              <button type="button" id="open-connect-btn" class="w-full h-12 px-4 rounded-xl bg-white hover:bg-slate-50 ring-1 ring-inset ring-slate-200 hover:ring-slate-300 text-[#0F2545] font-semibold text-[15px] transition-all active:scale-[0.98] flex items-center justify-center cursor-pointer">
                Kết nối với gia sư
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

    // ---------------------------------------------------------------- Lịch rảnh theo tuần (4 tuần tới)
    // Server chỉ trả về khung giờ còn trống: đã trừ lịch lớp, ca đã có học viên, buổi học thử
    // (ở ngày đó và các tuần sau, vì kết nối là ca lặp lại hằng tuần) và giờ đã qua.

    const weekCache = new Map(); // weekStart "YYYY-MM-DD" -> dữ liệu tuần
    let weekData = null;         // tuần đang xem
    let weekLoadSeq = 0;

    const fmtDM = (iso) => { const [, mo, d] = String(iso).split('-'); return `${d}/${mo}`; };
    const fmtDMY = (iso) => { const [y, mo, d] = String(iso).split('-'); return `${d}/${mo}/${y}`; };
    const addDaysIso = (iso, n) => {
      const [y, mo, d] = String(iso).split('-').map(Number);
      const dt = new Date(Date.UTC(y, mo - 1, d + n));
      return dt.toISOString().slice(0, 10);
    };
    const todayIsoVN = () => new Date(Date.now() + 7 * 3600 * 1000).toISOString().slice(0, 10);

    function dayOf(dow) {
      return weekData ? (weekData.days || []).find(d => Number(d.dayOfWeek) === dow) : null;
    }

    /** Khung giờ còn trống của một thứ trong tuần đang xem (phút từ 00:00). */
    function freeRangesOf(dow) {
      const day = dayOf(dow);
      return (day ? day.freeRanges || [] : [])
        .map(r => ({ start: toMinutes(r.startTime), end: toMinutes(r.endTime) }))
        .sort((x, y) => x.start - y.start);
    }

    async function loadWeek(weekStart = null) {
      const key = weekStart || '__current__';
      if (weekCache.has(key)) return weekCache.get(key);
      // Khoảng trống từ 30 phút (đủ cho một buổi học thử)
      const data = await API.tutors.getWeeklySchedule(tutor.tutorId, weekStart, TRIAL_DURATION_MINUTES);
      weekCache.set(data.weekStart, data);
      if (!weekStart) weekCache.set(key, data);
      return data;
    }

    /** Chuyển sang tuần khác: tải dữ liệu, bỏ các ca đã chọn không còn trống ở tuần mới, vẽ lại bảng và form. */
    async function showWeek(weekStart = null) {
      const seq = ++weekLoadSeq;
      const gridEl = document.getElementById('weekly-calendar-grid');
      if (gridEl && !weekData) {
        gridEl.innerHTML = '<div class="col-span-7 py-8 text-center text-xs text-slate-500">Đang tải lịch rảnh...</div>';
      }
      setWeekNavBusy(true);
      try {
        const data = await loadWeek(weekStart);
        if (seq !== weekLoadSeq) return;
        weekData = data;
        renderWeeklyCalendar();
        renderConnectForm();
        renderTrialPicker();
        schedulePreview(true); // tuần bắt đầu đổi → kiểm tra lại từng buổi
      } catch (err) {
        if (seq !== weekLoadSeq) return;
        console.error('Lỗi khi tải lịch rảnh theo tuần:', err);
        if (gridEl) {
          gridEl.innerHTML = `
            <div class="col-span-7 py-8 text-center space-y-2">
              <div class="text-sm font-bold text-[#0F2545]">Không tải được lịch rảnh</div>
              <div class="text-xs text-slate-500">${escapeHtml(friendlyError(err, 'Vui lòng thử lại sau.'))}</div>
              <button type="button" id="week-retry-btn" class="mt-1 py-1.5 px-4 rounded-lg border border-slate-300 text-xs font-bold text-slate-700 hover:bg-slate-50 cursor-pointer">Thử lại</button>
            </div>`;
          const retry = document.getElementById('week-retry-btn');
          if (retry) retry.addEventListener('click', () => showWeek(weekStart));
        }
      } finally {
        if (seq === weekLoadSeq) setWeekNavBusy(false);
      }
    }

    function setWeekNavBusy(busy) {
      const canPrev = !!weekData && weekData.weekStart > weekData.minWeekStart;
      const canNext = !!weekData && weekData.weekStart < weekData.maxWeekStart;
      ['week-prev-btn', 'dc-week-prev', 'trial-week-prev'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.disabled = busy || !canPrev;
      });
      ['week-next-btn', 'dc-week-next', 'trial-week-next'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.disabled = busy || !canNext;
      });
    }

    function shiftWeek(delta) {
      if (!weekData) return;
      const target = addDaysIso(weekData.weekStart, delta * 7);
      if (target < weekData.minWeekStart || target > weekData.maxWeekStart) return;
      showWeek(target);
    }

    /** Ngày bắt đầu học gửi lên server: hôm nay nếu đang xem tuần này, ngược lại là Thứ Hai của tuần đang xem. */
    function connectStartDate() {
      if (!weekData) return null;
      const today = todayIsoVN();
      return weekData.weekStart < today ? today : weekData.weekStart;
    }

    function renderWeekHeader() {
      const rangeEl = document.getElementById('week-range-label');
      if (weekData && rangeEl) {
        rangeEl.textContent = `${fmtDM(weekData.weekStart)} – ${fmtDMY(weekData.weekEnd)}`;
      }
      const startLabel = document.getElementById('dc-start-label');
      if (startLabel && weekData) {
        startLabel.textContent = `${fmtDM(weekData.weekStart)} – ${fmtDMY(weekData.weekEnd)}`;
      }
      setWeekNavBusy(false);
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
      if (!weekData) return;
      renderWeekHeader();

      const expandWrapper = document.getElementById('weekly-expand-wrapper');
      const expandText = document.getElementById('toggle-weekly-slots-text');
      const expandIcon = document.getElementById('toggle-weekly-slots-icon');
      const today = todayIsoVN();
      let anyColumnTooLong = false;

      gridEl.innerHTML = WEEKDAY_COLUMNS.map(col => {
        const day = dayOf(col.dow);
        const date = day ? day.date : null;
        const isPast = !!date && date < today;
        const isToday = date === today;
        // Chỉ hiện giờ còn trống đủ cho một buổi học thử; giờ đã có lịch bị ẩn hẳn
        const starts = gridStartTimesOf(col.dow, TRIAL_DURATION_MINUTES);
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
          requireLogin(() => openTrialModal({ date, start }), 'connect=true');
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

    /** Các giờ bắt đầu (bước 30 phút) đủ chỗ cho một ca dài `duration` phút trong các khoảng trống cho trước. */
    function startTimesFrom(ranges, duration) {
      const starts = [];
      ranges.forEach(r => {
        for (let t = Math.ceil(r.start / 30) * 30; t + duration <= r.end && t + duration < 24 * 60; t += 30) {
          starts.push(t);
        }
      });
      return [...new Set(starts)].sort((a, b) => a - b);
    }

    /**
     * Danh sách giờ chia nhóm Buổi sáng / Buổi chiều / Buổi tối, mỗi nhóm một lưới 3 cột.
     * renderItem(t) trả về HTML của một ô giờ (t = phút từ 00:00).
     */
    const TIME_GROUPS = [
      { label: 'Buổi sáng', icon: 'wb_twilight', from: 0, to: 12 * 60 },
      { label: 'Buổi chiều', icon: 'light_mode', from: 12 * 60, to: 17 * 60 },
      { label: 'Buổi tối', icon: 'nights_stay', from: 17 * 60, to: 24 * 60 }
    ];
    function groupedTimesHtml(starts, renderItem) {
      return TIME_GROUPS.map(g => {
        const items = starts.filter(t => t >= g.from && t < g.to);
        if (!items.length) return '';
        return `
          <div class="space-y-2">
            <div class="flex items-center gap-2 text-sm font-bold text-[#0F2545]">
              <span class="material-symbols-outlined text-[20px] text-slate-600">${g.icon}</span>${g.label}
            </div>
            <div class="grid grid-cols-3 gap-2">${items.map(renderItem).join('')}</div>
          </div>`;
      }).join('');
    }
    const TIME_BTN = 'w-full py-2.5 px-1 rounded-xl text-sm text-center transition-all';
    const TIME_BTN_IDLE = `${TIME_BTN} border border-slate-200 bg-white hover:border-[#006194] hover:bg-sky-50 text-slate-800 font-medium cursor-pointer`;
    const TIME_BTN_ACTIVE = `${TIME_BTN} border-2 border-[#006194] bg-[#006194] text-white font-bold cursor-pointer`;
    const TIME_BTN_DISABLED = `${TIME_BTN} border border-slate-100 bg-slate-50 text-slate-300 font-medium cursor-not-allowed`;

    /** Bảng lịch theo ngày: giờ trống của đúng tuần đang xem. */
    function gridStartTimesOf(dow, duration) {
      return startTimesFrom(freeRangesOf(dow), duration);
    }

    /**
     * Form kết nối: MẪU lịch rảnh hằng tuần (lịch rảnh trừ lịch lớp chính thức, lặp mọi tuần).
     * Trùng lịch theo từng ngày cụ thể (học thử, buổi của khóa khác) do server kiểm tra ở "Lịch học dự kiến".
     */
    function patternRangesOf(dow) {
      const busy = [...(tutor.classSchedules || []), ...(tutor.lockedSlots || [])]
        .filter(b => Number(b.dayOfWeek) === dow && b.startTime && b.endTime)
        .map(b => ({ start: toMinutes(b.startTime), end: toMinutes(b.endTime) }));
      const result = [];
      (tutor.availabilities || [])
        .filter(a => Number(a.dayOfWeek) === dow && a.startTime && a.endTime)
        .forEach(a => {
          let pieces = [{ start: toMinutes(a.startTime), end: toMinutes(a.endTime) || 24 * 60 }];
          busy.forEach(b => {
            pieces = pieces.flatMap(p => {
              if (!overlaps(p.start, p.end, b.start, b.end)) return [p];
              const parts = [];
              if (b.start > p.start) parts.push({ start: p.start, end: b.start });
              if (b.end < p.end) parts.push({ start: b.end, end: p.end });
              return parts;
            });
          });
          result.push(...pieces);
        });
      return result.sort((x, y) => x.start - y.start);
    }

    function startTimesOf(dow, duration = connectDuration) {
      return startTimesFrom(patternRangesOf(dow), duration);
    }

    // ---------------------------------------------------------------- Lịch học dự kiến (kiểm tra từng buổi)

    const MAX_SESSIONS = 12;
    let totalSessions = 8;
    let preview = null;         // kết quả server cho lựa chọn hiện tại
    let previewError = '';
    let previewLoading = false;
    let previewSeq = 0;
    let previewTimer = null;
    const slotKey = (dow, start) => `${dow}-${start}`;

    function previewPayload() {
      return {
        tutorId: tutor.tutorId,
        slots: selectedSlots.map(s => ({ dayOfWeek: s.dow, startTime: toHHMM(s.start), durationMinutes: s.duration })),
        startDate: connectStartDate(),
        totalSessions
      };
    }

    /** Gọi lại server (chờ 300 ms để gộp các lần bấm liên tiếp). */
    function schedulePreview(immediate = false) {
      clearTimeout(previewTimer);
      preview = null;
      previewError = '';
      if (!selectedSlots.length || !weekData) {
        previewLoading = false;
        renderPreview();
        return;
      }
      previewLoading = true;
      renderPreview();
      const seq = ++previewSeq;
      previewTimer = setTimeout(async () => {
        try {
          const data = await API.connectionRequests.preview(previewPayload());
          if (seq !== previewSeq) return;
          preview = data;
        } catch (err) {
          if (seq !== previewSeq) return;
          previewError = friendlyError(err, 'Không kiểm tra được lịch học dự kiến. Vui lòng thử lại.');
        } finally {
          if (seq === previewSeq) {
            previewLoading = false;
            renderPreview();
          }
        }
      }, immediate ? 0 : 300);
    }

    /** Các ca hằng tuần (dow-start) có ít nhất một buổi bị kín. */
    function conflictSlotKeys() {
      const keys = new Set();
      if (!preview) return keys;
      (preview.sessions || []).filter(s => !s.available).forEach(s => {
        keys.add(slotKey(Number(s.dayOfWeek), toMinutes(s.startTime)));
      });
      return keys;
    }

    function renderPreview() {
      const listEl = document.getElementById('dc-preview');
      const summaryEl = document.getElementById('dc-preview-summary');
      const conflictEl = document.getElementById('dc-conflict');
      const countEl = document.getElementById('dc-sessions-count');
      const minusBtn = document.getElementById('dc-sessions-minus');
      const plusBtn = document.getElementById('dc-sessions-plus');
      if (countEl) countEl.textContent = `${totalSessions} buổi`;
      if (minusBtn) minusBtn.disabled = totalSessions <= 1;
      if (plusBtn) plusBtn.disabled = totalSessions >= MAX_SESSIONS;

      const conflicts = preview ? Number(preview.conflictCount || 0) : 0;
      const canSubmit = selectedSlots.length > 0 && !!preview && conflicts === 0 && !previewLoading;
      if (directSubmitBtn) directSubmitBtn.disabled = !canSubmit;
      const submitText = document.getElementById('btn-direct-submit-text');
      if (submitText) {
        submitText.textContent = !selectedSlots.length ? 'Gửi yêu cầu kết nối'
          : previewLoading ? 'Đang kiểm tra lịch...'
          : conflicts ? `Còn ${conflicts} buổi bị kín`
          : `Gửi yêu cầu kết nối (${totalSessions} buổi)`;
      }

      // Đánh dấu ca bị vướng trong danh sách "Ca đã chọn"
      const badKeys = conflictSlotKeys();
      document.querySelectorAll('#dc-selected [data-slot-key]').forEach(chip => {
        const bad = badKeys.has(chip.dataset.slotKey);
        chip.classList.toggle('bg-amber-50', bad);
        chip.classList.toggle('border-amber-300', bad);
        chip.classList.toggle('text-amber-700', bad);
      });

      if (!listEl) return;
      if (!selectedSlots.length) {
        listEl.innerHTML = '<div class="p-3 text-xs text-slate-500">Chọn ít nhất một ca để xem lịch học dự kiến.</div>';
        if (summaryEl) summaryEl.textContent = '';
        if (conflictEl) conflictEl.classList.add('hidden');
        return;
      }
      if (previewLoading) {
        listEl.innerHTML = '<div class="p-3 text-xs text-slate-500 flex items-center gap-2"><span class="material-symbols-outlined animate-spin text-[16px]">progress_activity</span>Đang kiểm tra từng buổi với lịch của gia sư...</div>';
        if (summaryEl) summaryEl.textContent = '';
        if (conflictEl) conflictEl.classList.add('hidden');
        return;
      }
      if (previewError) {
        listEl.innerHTML = `<div class="p-3 text-xs text-rose-600">${escapeHtml(previewError)}</div>`;
        if (summaryEl) summaryEl.textContent = '';
        if (conflictEl) conflictEl.classList.add('hidden');
        return;
      }
      if (!preview) return;

      const sessions = preview.sessions || [];
      if (summaryEl) {
        summaryEl.textContent = sessions.length
          ? `${sessions.length} buổi · ${fmtDM(preview.startDate)} – ${fmtDMY(preview.endDate)}`
          : '';
      }
      listEl.innerHTML = sessions.map((s, i) => `
          <div class="flex items-center gap-2.5 px-3 py-2 text-xs ${s.available ? '' : 'bg-amber-50'}">
            <span class="w-5 text-right font-semibold text-slate-400">${i + 1}</span>
            <span class="material-symbols-outlined text-[16px] ${s.available ? 'text-emerald-600' : 'text-amber-600'}">${s.available ? 'check_circle' : 'warning'}</span>
            <span class="font-semibold text-[#0F2545] min-w-[120px]">${escapeHtml(s.dayName)} ${fmtDMY(s.date)}</span>
            <span class="text-slate-600">${s.startTime}–${s.endTime}</span>
            ${s.available ? '' : `<span class="ml-auto text-right text-amber-700 font-semibold">Đã kín · ${escapeHtml(s.reason || '')}</span>`}
          </div>`).join('');

      if (!conflictEl) return;
      if (!conflicts) {
        conflictEl.classList.add('hidden');
        conflictEl.innerHTML = '';
        return;
      }
      const busySessions = sessions.filter(s => !s.available);
      const weeksText = [...new Set(busySessions.map(s => {
        const monday = addDaysIso(s.date, -((new Date(`${s.date}T00:00:00Z`).getUTCDay() + 6) % 7));
        return `${fmtDM(monday)} – ${fmtDM(addDaysIso(monday, 6))}`;
      }))].join(', ');
      const suggested = preview.suggestedStartWeek;
      conflictEl.classList.remove('hidden');
      conflictEl.innerHTML = `
        <div class="p-3 rounded-xl bg-amber-50 border border-amber-200 space-y-2.5">
          <div class="flex items-start gap-2 text-xs text-amber-800">
            <span class="material-symbols-outlined text-[18px] flex-shrink-0">event_busy</span>
            <div>
              <div class="font-bold">${conflicts} buổi bị kín ở tuần ${weeksText}</div>
              <div class="mt-0.5">${busySessions.map(s => `${escapeHtml(s.dayName)} ${fmtDM(s.date)} ${s.startTime}: ${escapeHtml(s.reason || 'gia sư bận')}`).join('<br>')}</div>
              <div class="mt-1">Bạn có thể chọn một trong các cách sau:</div>
            </div>
          </div>
          <div class="grid grid-cols-1 sm:grid-cols-3 gap-2">
            <button type="button" id="dc-opt-shift" ${suggested ? '' : 'disabled'}
              class="py-2 px-2 rounded-lg bg-white border border-amber-300 text-[11px] font-bold text-amber-800 hover:bg-amber-100 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
              title="${suggested ? 'Tất cả các buổi đều trống nếu bắt đầu từ tuần này' : 'Không có tuần nào trong 4 tuần tới mà tất cả các buổi đều trống'}">
              ${suggested ? `Bắt đầu từ tuần ${fmtDM(suggested)}` : 'Không có tuần trống phù hợp'}
            </button>
            <button type="button" id="dc-opt-other" class="py-2 px-2 rounded-lg bg-white border border-amber-300 text-[11px] font-bold text-amber-800 hover:bg-amber-100 cursor-pointer">
              Chọn ca khác
            </button>
            <button type="button" id="dc-opt-propose" disabled class="py-2 px-2 rounded-lg bg-white border border-slate-200 text-[11px] font-bold text-slate-400 cursor-not-allowed" title="Tính năng đề xuất giờ ngoài lịch rảnh sẽ có trong bản cập nhật tiếp theo">
              Đề xuất giờ khác <span class="font-medium">(sắp có)</span>
            </button>
          </div>
        </div>`;
      const shiftBtn = document.getElementById('dc-opt-shift');
      if (shiftBtn && suggested) shiftBtn.addEventListener('click', () => showWeek(suggested));
      const otherBtn = document.getElementById('dc-opt-other');
      if (otherBtn) otherBtn.addEventListener('click', () => {
        connectDay = Number(busySessions[0].dayOfWeek);
        renderConnectForm();
        const daysEl = document.getElementById('dc-days');
        if (daysEl) daysEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
      });
    }

    function changeSessions(delta) {
      const next = Math.min(MAX_SESSIONS, Math.max(1, totalSessions + delta));
      if (next === totalSessions) return;
      totalSessions = next;
      schedulePreview();
    }
    const sessionsMinusBtn = document.getElementById('dc-sessions-minus');
    const sessionsPlusBtn = document.getElementById('dc-sessions-plus');
    if (sessionsMinusBtn) sessionsMinusBtn.addEventListener('click', () => changeSessions(-1));
    if (sessionsPlusBtn) sessionsPlusBtn.addEventListener('click', () => changeSessions(1));

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
      if (weekData) renderWeekHeader();
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
          timesEl.innerHTML = `<div class="py-4 text-center text-xs text-slate-500">${connectDay
            ? `Không còn khung ${connectDuration} phút trống vào ${dayNameOf(connectDay)}. Hãy chọn thứ khác hoặc thời lượng ngắn hơn.`
            : 'Gia sư chưa có khung giờ trống phù hợp.'}</div>`;
        } else {
          timesEl.innerHTML = groupedTimesHtml(allStarts, t => {
            const chosen = daySelected.find(s => s.start === t);
            const blocked = !chosen && daySelected.some(s => overlaps(t, t + connectDuration, s.start, s.start + s.duration));
            if (chosen) {
              return `<button type="button" data-start="${t}" class="${TIME_BTN_ACTIVE}" title="Bỏ chọn">
                ${toHHMM(t)}–${toHHMM(t + chosen.duration)}</button>`;
            }
            if (blocked) {
              return `<div class="${TIME_BTN_DISABLED}" title="Trùng với ca bạn đã chọn">${toHHMM(t)}</div>`;
            }
            return `<button type="button" data-start="${t}" class="${TIME_BTN_IDLE}"
              title="${dayNameOf(connectDay)} ${toHHMM(t)}–${toHHMM(t + connectDuration)}">${toHHMM(t)}</button>`;
          });
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
              <span data-slot-key="${slotKey(s.dow, s.start)}" class="inline-flex items-center gap-1.5 pl-3 pr-1.5 py-1 rounded-full bg-sky-50 border border-sky-200 text-xs font-semibold text-[#006194]">
                ${slotLabel(s)}
                <button type="button" data-remove-index="${i}" class="w-5 h-5 rounded-full hover:bg-sky-100 flex items-center justify-center cursor-pointer" aria-label="Bỏ chọn ${slotLabel(s)}">
                  <span class="material-symbols-outlined text-[14px]">close</span>
                </button>
              </span>`).join('')
          : '<span class="text-xs text-slate-500">Chưa chọn ca nào. Hãy chọn thứ rồi bấm vào giờ bắt đầu.</span>';
        selEl.querySelectorAll('[data-remove-index]').forEach(btn => btn.addEventListener('click', () => {
          selectedSlots.splice(Number(btn.dataset.removeIndex), 1);
          renderConnectForm();
          schedulePreview();
        }));
      }

      renderPreview();
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
      schedulePreview();
    }

    /** preselect: { dow, start } khi học viên bấm một giờ trên bảng lịch rảnh. */
    function openDirectModal(preselect = null) {
      if (!weekData) {
        // Lịch tuần chưa tải xong (hoặc tải lỗi): tải rồi mở lại form
        showWeek().then(() => { if (weekData) openDirectModal(preselect); });
        return;
      }
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
      schedulePreview(true);
      openModal('direct-connect-modal');
    }

    if (directSubmitBtn) {
      directSubmitBtn.addEventListener('click', async () => {
        showDirectError('');
        if (!selectedSlots.length) {
          showDirectError('Hãy chọn ít nhất một ca học hằng tuần.');
          return;
        }
        if (!preview || Number(preview.conflictCount || 0) > 0) {
          showDirectError('Lịch học dự kiến còn buổi bị kín. Hãy chọn tuần bắt đầu khác hoặc ca khác.');
          return;
        }
        const level = document.getElementById('direct-level');
        const goals = document.getElementById('direct-goals');
        const notes = document.getElementById('direct-notes');
        const payload = {
          ...previewPayload(),
          currentLevel: level ? level.value : null,
          learningGoals: goals ? (goals.value.trim() || null) : null,
          initialMessage: notes ? (notes.value.trim() || null) : null
        };

        directSubmitBtn.disabled = true;
        const original = directSubmitBtn.innerHTML;
        directSubmitBtn.innerHTML = '<span class="material-symbols-outlined animate-spin text-[18px]">progress_activity</span><span>Đang gửi...</span>';
        try {
          const created = await API.connectionRequests.create(payload);
          const createdSessions = (created && created.sessions) || [];
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
                  ${selectedSlots.map(s => `<li class="font-semibold text-[#0F2545]">${slotLabel(s)} · hằng tuần</li>`).join('')}
                  ${createdSessions.length ? `<li class="pt-1 text-slate-500">${createdSessions.length} buổi · từ ${fmtDMY(createdSessions[0].date)} đến ${fmtDMY(createdSessions[createdSessions.length - 1].date)}</li>` : ''}
                </ul>
                <div class="grid grid-cols-2 gap-2.5 pt-2">
                  <a href="/pages/learner-requests/?tab=direct" class="py-2.5 px-3 rounded-xl bg-[#006194] hover:bg-[#004b73] text-white text-xs font-bold text-center transition-all shadow-xs flex items-center justify-center gap-1.5">
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
          if (err && err.status === 409 && weekData) {
            // Có người vừa nhận giờ này: tải lại lịch để ẩn giờ đã kín
            weekCache.clear();
            showWeek(weekData.weekStart);
          }
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

    [['week-prev-btn', -1], ['week-next-btn', 1], ['dc-week-prev', -1], ['dc-week-next', 1]].forEach(([id, delta]) => {
      const el = document.getElementById(id);
      if (el) el.addEventListener('click', () => shiftWeek(delta));
    });

    if ((tutor.availabilities || []).length) {
      showWeek();
    } else {
      renderWeeklyCalendar();
    }

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

    // Hai cách chọn giờ học thử:
    //  'calendar' (mặc định): chọn giờ trống trong lịch rảnh của gia sư.
    //  'propose': "Đề xuất giờ khác" — tự chọn ngày giờ ngoài lịch rảnh, gia sư đồng ý hoặc từ chối.
    let trialMode = 'calendar';
    let trialDay = null;   // ngày đang xem trong lịch ("YYYY-MM-DD")
    let trialPick = null;  // { date: "YYYY-MM-DD", start: phút từ 00:00 }

    function trialStartsOfDate(date) {
      const day = weekData ? (weekData.days || []).find(d => d.date === date) : null;
      if (!day) return [];
      const ranges = (day.freeRanges || []).map(r => ({ start: toMinutes(r.startTime), end: toMinutes(r.endTime) }));
      return startTimesFrom(ranges, TRIAL_DURATION_MINUTES);
    }

    function renderTrialPicker() {
      const calEl = document.getElementById('trial-pick-calendar');
      const propEl = document.getElementById('trial-pick-propose');
      if (calEl) calEl.classList.toggle('hidden', trialMode !== 'calendar');
      if (propEl) propEl.classList.toggle('hidden', trialMode !== 'propose');
      if (trialMode !== 'calendar' || !weekData) {
        updateTrialSummary();
        return;
      }

      const weekLabel = document.getElementById('trial-week-label');
      if (weekLabel) weekLabel.textContent = `${fmtDM(weekData.weekStart)} – ${fmtDMY(weekData.weekEnd)}`;

      const days = weekData.days || [];
      if (!trialDay || !days.some(d => d.date === trialDay)) {
        const firstFree = days.find(d => trialStartsOfDate(d.date).length > 0);
        trialDay = firstFree ? firstFree.date : (days[0] ? days[0].date : null);
      }
      if (trialPick && !trialStartsOfDate(trialPick.date).includes(trialPick.start)) trialPick = null;

      const daysEl = document.getElementById('trial-days');
      if (daysEl) {
        daysEl.innerHTML = days.map(d => {
          const count = trialStartsOfDate(d.date).length;
          const active = d.date === trialDay;
          const col = WEEKDAY_COLUMNS.find(c => c.dow === Number(d.dayOfWeek));
          const short = col ? (col.shortName === 'Chủ nhật' ? 'CN' : col.shortName.replace('Thứ ', 'T')) : '';
          const cls = active
            ? 'border-2 border-[#006194] bg-sky-50 text-[#006194] font-bold'
            : count ? 'border border-slate-200 hover:bg-slate-50 text-slate-800 font-semibold' : 'border border-transparent text-slate-300';
          return `
            <button type="button" data-trial-day="${d.date}" ${count ? '' : 'disabled'}
              class="py-1.5 rounded-xl text-[11px] leading-tight transition-all ${count ? 'cursor-pointer' : 'cursor-not-allowed'} ${cls}">
              <span class="block">${short}</span><span class="block text-[10px] font-medium">${fmtDM(d.date)}</span>
            </button>`;
        }).join('');
        daysEl.querySelectorAll('[data-trial-day]').forEach(btn => btn.addEventListener('click', () => {
          trialDay = btn.dataset.trialDay;
          clearError();
          renderTrialPicker();
        }));
      }

      const timesEl = document.getElementById('trial-times');
      if (timesEl) {
        const starts = trialDay ? trialStartsOfDate(trialDay) : [];
        timesEl.innerHTML = starts.length ? groupedTimesHtml(starts, t => {
          const chosen = trialPick && trialPick.date === trialDay && trialPick.start === t;
          return `<button type="button" data-trial-start="${t}" class="${chosen ? TIME_BTN_ACTIVE : TIME_BTN_IDLE}"
            title="${toHHMM(t)}–${toHHMM(t + TRIAL_DURATION_MINUTES)}">${toHHMM(t)}</button>`;
        }) : `<div class="py-3 text-center text-xs text-slate-500">Tuần này gia sư không còn giờ trống. Hãy xem tuần sau hoặc đề xuất giờ khác.</div>`;
        timesEl.querySelectorAll('[data-trial-start]').forEach(btn => btn.addEventListener('click', () => {
          trialPick = { date: trialDay, start: Number(btn.dataset.trialStart) };
          clearError();
          renderTrialPicker();
        }));
      }
      updateTrialSummary();
    }

    function setTrialMode(mode) {
      trialMode = mode;
      clearError();
      renderTrialPicker();
    }

    /** Ngày giờ học thử học viên chọn (giờ Việt Nam), hoặc null nếu chưa đủ. */
    function getTrialStart() {
      if (trialMode === 'calendar') {
        if (!trialPick) return null;
        const [y, mo, d] = trialPick.date.split('-').map(Number);
        return new Date(y, mo - 1, d, Math.floor(trialPick.start / 60), trialPick.start % 60, 0, 0);
      }
      if (!trialDateInput || !trialTimeInput || !trialDateInput.value || !trialTimeInput.value) return null;
      const [y, mo, d] = trialDateInput.value.split('-').map(Number);
      const [h, mi] = trialTimeInput.value.split(':').map(Number);
      return new Date(y, mo - 1, d, h, mi, 0, 0);
    }

    function updateTrialSummary() {
      const start = getTrialStart();
      if (continueBtn) continueBtn.disabled = !start;
      const btnText = continueBtn ? continueBtn.querySelector('span') : null;
      if (btnText) btnText.textContent = trialMode === 'propose' ? 'Gửi đề xuất học thử' : 'Xác nhận gửi lịch học thử';
      if (!trialSummary) return;
      if (!start) {
        trialSummary.textContent = trialMode === 'propose'
          ? 'Chọn ngày và giờ bạn muốn đề xuất. Gia sư sẽ xem và đồng ý hoặc từ chối.'
          : 'Chọn một giờ trống trong lịch rảnh của gia sư.';
        return;
      }
      const end = new Date(start.getTime() + TRIAL_DURATION_MINUTES * 60000);
      const when = `${DAY_NAMES[start.getDay()]}, ${pad(start.getDate())}/${pad(start.getMonth() + 1)}/${start.getFullYear()} · ${pad(start.getHours())}:${pad(start.getMinutes())}–${pad(end.getHours())}:${pad(end.getMinutes())}`;
      trialSummary.innerHTML = trialMode === 'propose'
        ? `Bạn đề xuất học thử vào <strong class="text-[#0F2545]">${when}</strong> <span class="text-amber-700 font-semibold">(ngoài lịch rảnh)</span>. Gia sư sẽ đồng ý hoặc từ chối.`
        : `Bạn muốn học thử vào <strong class="text-[#0F2545]">${when}</strong>. Gia sư sẽ xác nhận lại với bạn.`;
    }

    const proposeToggle = document.getElementById('trial-propose-toggle');
    if (proposeToggle) proposeToggle.addEventListener('click', () => setTrialMode('propose'));
    const backToCalendar = document.getElementById('trial-back-to-calendar');
    if (backToCalendar) backToCalendar.addEventListener('click', () => setTrialMode('calendar'));
    [['trial-week-prev', -1], ['trial-week-next', 1]].forEach(([id, delta]) => {
      const el = document.getElementById(id);
      if (el) el.addEventListener('click', () => shiftWeek(delta));
    });

    if (trialDateInput) {
      const today = new Date();
      trialDateInput.min = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`;
      trialDateInput.addEventListener('change', () => { clearError(); updateTrialSummary(); });
    }
    if (trialTimeInput) {
      trialTimeInput.addEventListener('change', () => { clearError(); updateTrialSummary(); });
    }
    updateTrialSummary();

    /** preselect: { date: "YYYY-MM-DD", start: phút } khi học viên bấm một giờ trên bảng lịch rảnh. */
    function openTrialModal(preselect = null) {
      if (!weekData && (tutor.availabilities || []).length) {
        showWeek().then(() => { if (weekData) openTrialModal(preselect); });
        return;
      }
      clearError();
      // Gia sư chưa khai báo lịch rảnh thì chỉ còn cách đề xuất giờ
      trialMode = (tutor.availabilities || []).length ? 'calendar' : 'propose';
      if (preselect) {
        trialDay = preselect.date;
        trialPick = { date: preselect.date, start: preselect.start };
      }
      renderTrialPicker();
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
        initialMessage: notesInput ? (notesInput.value.trim() || 'Đăng ký học thử trực tuyến 1–1') : 'Đăng ký học thử trực tuyến 1–1',
        offSchedule: trialMode === 'propose'
      };

      continueBtn.disabled = true;
      const origBtnHtml = continueBtn.innerHTML;
      continueBtn.innerHTML = '<span class="material-symbols-outlined animate-spin text-[18px]">progress_activity</span><span>Đang xử lý...</span>';

      try {
        const createdTrial = await API.trialBookings.create(payload);
        const isProposal = !!(createdTrial && createdTrial.offSchedule) || payload.offSchedule;
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
              ${isProposal ? `
              <div class="flex items-center justify-between">
                <span class="text-slate-500 font-medium">Loại yêu cầu:</span>
                <span class="font-semibold text-amber-700">Đề xuất ngoài lịch rảnh</span>
              </div>` : ''}
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
              <a href="/pages/learner-requests/?tab=trial" class="py-2.5 px-3 rounded-xl bg-[#006194] hover:bg-[#004b73] text-white text-xs font-bold text-center transition-all shadow-xs flex items-center justify-center gap-1.5">
                <span class="material-symbols-outlined text-[16px]">list_alt</span>
                <span>Xem yêu cầu của tôi</span>
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
        if (err && err.status === 409 && weekData && trialMode === 'calendar') {
          // Giờ vừa có người đặt: tải lại lịch để bỏ giờ đã kín
          weekCache.clear();
          showWeek(weekData.weekStart);
        }
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
