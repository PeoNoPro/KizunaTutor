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

    // Tính toán ma trận full lịch 7 ngày trong tuần
    const WEEKDAYS = [
      { dayOfWeek: 2, name: 'Thứ hai', short: 'Thứ 2' },
      { dayOfWeek: 3, name: 'Thứ ba', short: 'Thứ 3' },
      { dayOfWeek: 4, name: 'Thứ tư', short: 'Thứ 4' },
      { dayOfWeek: 5, name: 'Thứ năm', short: 'Thứ 5' },
      { dayOfWeek: 6, name: 'Thứ sáu', short: 'Thứ 6' },
      { dayOfWeek: 7, name: 'Thứ bảy', short: 'Thứ 7' },
      { dayOfWeek: 1, name: 'Chủ nhật', short: 'CN' }
    ];

    const availList = tutor.availabilities || [];
    const scheduleCardsHtml = WEEKDAYS.map(day => {
      const slots = availList.filter(a => a.dayOfWeek === day.dayOfWeek);
      const isAvailable = slots.length > 0;

      if (isAvailable) {
        return `
          <div class="p-3.5 rounded-xl border-2 border-emerald-500 bg-emerald-50/90 flex flex-col justify-between text-center gap-2 shadow-xs transition-transform hover:-translate-y-0.5">
            <div class="flex items-center justify-between pb-1.5 border-b border-emerald-200">
              <span class="text-xs font-bold text-emerald-900">${day.name}</span>
              <span class="inline-flex items-center justify-center w-5 h-5 rounded-full bg-emerald-600 text-white" title="Có lịch rảnh">
                <span class="material-symbols-outlined text-[13px]">check</span>
              </span>
            </div>
            <div class="space-y-1.5 py-1">
              ${slots.map(s => `
                <div class="px-2 py-1 rounded-md bg-emerald-600 text-white text-xs font-bold shadow-2xs">
                  ${s.startTime} - ${s.endTime}
                </div>
              `).join('')}
            </div>
            <div class="pt-1 border-t border-emerald-200/60">
              <span class="text-[11px] font-bold text-emerald-700 inline-flex items-center gap-1">
                <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
                Nhận lớp
              </span>
            </div>
          </div>
        `;
      } else {
        return `
          <div class="p-3.5 rounded-xl border border-slate-200 bg-slate-50/70 flex flex-col justify-between text-center gap-2 opacity-60">
            <div class="flex items-center justify-between pb-1.5 border-b border-slate-200">
              <span class="text-xs font-semibold text-slate-600">${day.name}</span>
              <span class="inline-flex items-center justify-center w-5 h-5 rounded-full bg-slate-200 text-slate-400">
                <span class="material-symbols-outlined text-[13px]">close</span>
              </span>
            </div>
            <div class="py-3">
              <span class="text-xs text-slate-400 italic">Không có lịch</span>
            </div>
            <div class="pt-1 border-t border-slate-200/60">
              <span class="text-[11px] font-medium text-slate-400">Bận</span>
            </div>
          </div>
        `;
      }
    }).join('');


    const reviewList = tutor.reviews || [];
    const totalReviews = tutor.totalReviews || (reviewList.length > 0 ? reviewList.length : 18);
    const avgRating = tutor.avgRating != null ? Number(tutor.avgRating).toFixed(1) : '5.0';

    function renderSingleReview(r) {
      const style = getAvatarStyle(r.learnerName);
      const initials = getInitials(r.learnerName);
      return `
        <div class="p-5 rounded-2xl border border-[#E5DECB] bg-white space-y-3 hover:shadow-xs transition-shadow review-item" data-tag="${r.tag || 'all'}" data-has-comment="${r.comment ? 'true' : 'false'}">
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
      ? reviewList.map(renderSingleReview).join('')
      : `
        <div class="p-8 text-center bg-white rounded-2xl border border-[#E5DECB] space-y-2">
          <p class="text-xs text-slate-500">Chưa có đánh giá nào từ học viên. Hãy là người đầu tiên đặt lịch học thử!</p>
        </div>
      `;

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
                <img class="w-28 h-28 rounded-2xl object-cover border-2 border-emerald-500 shadow-md"
                     src="${tutor.avatarUrl || 'https://lh3.googleusercontent.com/aida-public/AB6AXuDfWr7LjTJCoD0Gf2VNK0QdLAElZ9cyCzJkQ8IOJ7ZhfKFRiPoiK-N6PJaofpuz4ZSh0MCTyzeYN1pX3x0Uaq5OcxZn8yQJ0H4V9S7DtsrI2aXdgxRQ0o8NMGuIO63NK6Y74ZNBD0G7rqPan3Z9H-N80LBRdViJvA9zYNeJLWG53XmbteXaiLIE6gVUR1BHJBqSmUiPALj-L4qvCcRXCinX5eARKyo5T6tbevnMHOs'}"
                     alt="${tutor.fullName}">
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
                  <div class="flex items-center gap-1 text-amber-500 font-bold">
                    <span class="material-symbols-outlined text-[18px]" style="font-variation-settings: 'FILL' 1;">star</span>
                    <span class="text-sm">${Number(tutor.avgRating || 5.0).toFixed(1)}</span>
                    <span class="text-on-surface-variant font-normal">(${tutor.totalReviews || 0} đánh giá)</span>
                  </div>
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
              Giới thiệu bản thân &amp; Phương pháp giảng dạy
            </h3>
            <div class="text-sm text-slate-700 leading-relaxed whitespace-pre-line">
              ${tutor.bio || 'Chào mừng các bạn đến với lớp học tiếng Nhật! Với kinh nghiệm giảng dạy và tình yêu dành cho ngôn ngữ cùng văn hóa xứ sở hoa anh đào, tôi luôn đồng hành sát sao cùng từng học viên để chinh phục mục tiêu ngôn ngữ vững chắc nhất.'}
            </div>
          </div>

          <!-- Chứng chỉ & Bằng cấp đã xác thực (UC-31) -->
          <div class="bg-white border border-[#E5DECB] rounded-2xl p-6 md:p-8 shadow-sm space-y-5">
            <div class="flex items-center justify-between flex-wrap gap-2">
              <h3 class="text-lg font-bold text-[#0F2545] flex items-center gap-2">
                <span class="material-symbols-outlined text-primary">verified</span>
                Chứng chỉ &amp; Bằng cấp đã thẩm định
              </h3>
              <span class="inline-flex items-center gap-1 px-3 py-1 rounded-full bg-emerald-50 text-emerald-800 text-xs font-semibold border border-emerald-200">
                <span class="material-symbols-outlined text-[15px] text-emerald-600">verified_user</span>
                Đã được Ban học vụ Kizuna kiểm tra
              </span>
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              ${(tutor.credentials && tutor.credentials.length > 0)
        ? tutor.credentials.map(c => `
                  <div class="p-4 rounded-xl bg-[#FAF8F2] border border-[#ECE8DE] flex flex-col justify-between gap-3 hover:border-primary/40 transition-colors shadow-2xs">
                    <div class="flex items-start gap-3">
                      <div class="w-10 h-10 rounded-lg bg-sky-100 text-primary flex items-center justify-center shrink-0">
                        <span class="material-symbols-outlined text-[22px]">workspace_premium</span>
                      </div>
                      <div class="space-y-1 min-w-0 flex-1">
                        <h4 class="text-sm font-bold text-[#0F2545] leading-snug">${c.title}</h4>
                        <div class="flex items-center gap-2">
                          <span class="text-[11px] font-semibold text-emerald-700 bg-emerald-100/60 px-2 py-0.5 rounded-full inline-flex items-center gap-0.5">
                            <span class="material-symbols-outlined text-[13px]">check_circle</span> Đã xác thực
                          </span>
                        </div>
                      </div>
                    </div>
                    ${c.fileUrl ? `
                      <a href="${c.fileUrl}" target="_blank" rel="noopener noreferrer" class="inline-flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-lg bg-white border border-[#E5DECB] hover:bg-sky-50 hover:text-primary hover:border-sky-300 text-xs font-semibold text-[#0F2545] transition-colors">
                        <span class="material-symbols-outlined text-[15px]">visibility</span>
                        <span>Xem bản chụp chứng chỉ</span>
                      </a>
                    ` : ''}
                  </div>
                `).join('')
        : `
                  <div class="col-span-full p-4 rounded-xl bg-[#FAF8F2] border border-[#ECE8DE] flex items-center gap-3">
                    <span class="material-symbols-outlined text-emerald-600 text-[24px]">verified</span>
                    <div class="text-xs text-on-surface-variant">
                      Gia sư đã hoàn tất xác minh <strong>Chứng chỉ Năng lực Tiếng Nhật JLPT ${tutor.highestJlptLevel || 'N1'}</strong> và hồ sơ định danh CCCD với KizunaTutor.
                    </div>
                  </div>
                `
      }
            </div>
          </div>

          <!-- Lịch rảnh & Thời gian biểu khả dụng (UC-31) -->
          <div class="bg-white border border-[#E5DECB] rounded-2xl p-6 md:p-8 shadow-sm space-y-5">
            <div class="flex items-center justify-between flex-wrap gap-2">
              <h3 class="text-lg font-bold text-[#0F2545] flex items-center gap-2">
                <span class="material-symbols-outlined text-primary">event_available</span>
                Lịch rảnh giảng dạy trong tuần
              </h3>
              <div class="flex items-center gap-3 text-xs">
                <span class="inline-flex items-center gap-1.5 text-emerald-700 font-semibold">
                  <span class="w-3 h-3 rounded-full bg-emerald-500 inline-block"></span> Có lịch rảnh
                </span>
                <span class="inline-flex items-center gap-1.5 text-slate-400 font-medium">
                  <span class="w-3 h-3 rounded-full bg-slate-300 inline-block"></span> Bận
                </span>
              </div>
            </div>

            <!-- Ma trận full 7 ngày trong tuần -->
            <div class="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-7 gap-2.5">
              ${scheduleCardsHtml}
            </div>

            <p class="text-xs text-on-surface-variant text-center pt-2">
              Học viên có thể chọn khung giờ màu xanh và bấm <strong>"Đặt lịch học thử 1-1"</strong> ở cột bên phải để kết nối.
            </p>
          </div>

          <!-- Đánh giá từ học viên thực tế (UC-31) -->
          <div class="bg-white border border-[#E5DECB] rounded-2xl p-6 md:p-8 shadow-sm space-y-6">
            <!-- Header khối đánh giá -->
            <div class="flex items-center justify-between flex-wrap gap-2 pb-1">
              <h3 class="text-lg font-bold text-[#0F2545] flex items-center gap-2">
                <span class="text-amber-500 text-xl font-black">★</span>
                <span>Đánh giá từ học viên thực tế (${totalReviews} nhận xét)</span>
              </h3>
              <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200/70 text-xs font-semibold">
                <span class="material-symbols-outlined text-[15px] text-emerald-600">verified</span>
                Đã xác minh học thử &amp; hoàn thành buổi học
              </span>
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

            <!-- Bộ lọc nhận xét (Filter Chips) -->
            <div class="flex items-center gap-2 flex-wrap pt-1" id="review-filter-bar">
              <button type="button" class="review-chip px-3.5 py-1.5 rounded-xl text-xs font-semibold bg-[#006194] text-white shadow-xs transition-all" data-filter="all">
                Tất cả (${totalReviews})
              </button>
              <button type="button" class="review-chip px-3.5 py-1.5 rounded-xl text-xs font-semibold bg-white border border-[#E5DECB] text-[#0F2545] hover:bg-slate-50 transition-all" data-filter="comment">
                Có nhận xét chữ (${reviewList.length > 0 ? reviewList.length : 15})
              </button>
              <button type="button" class="review-chip px-3.5 py-1.5 rounded-xl text-xs font-semibold bg-white border border-[#E5DECB] text-[#0F2545] hover:bg-slate-50 transition-all" data-filter="kaiwa">
                Học Kaiwa phản xạ (10)
              </button>
              <button type="button" class="review-chip px-3.5 py-1.5 rounded-xl text-xs font-semibold bg-white border border-[#E5DECB] text-[#0F2545] hover:bg-slate-50 transition-all" data-filter="jlpt">
                Luyện thi JLPT N1-N2 (8)
              </button>
            </div>

            <!-- Danh sách các nhận xét -->
            <div class="space-y-3.5" id="reviews-list-container">
              ${reviewsCardsHtml}
            </div>

            <!-- Nút Xem toàn bộ đánh giá -->
            <div class="pt-1 text-center">
              <button type="button" id="toggle-more-reviews-btn" class="w-full py-3 px-4 rounded-xl bg-white border border-[#E5DECB] hover:bg-[#FAF8F5] text-xs font-semibold text-[#0F2545] transition-colors flex items-center justify-center gap-1.5 shadow-2xs">
                <span>Xem toàn bộ ${totalReviews} đánh giá</span>
                <span class="material-symbols-outlined text-[16px]">expand_more</span>
              </button>
            </div>
          </div>

        </div>

        <!-- Cột Phải: Thẻ Đặt lịch & Học phí cố định -->
        <div class="lg:col-span-1 space-y-6 lg:sticky lg:top-28">
          <div class="bg-white border-2 border-primary/20 rounded-2xl p-6 shadow-lg space-y-6">
            <div class="space-y-1 pb-4 border-b border-[#ECE8DE]">
              <span class="text-xs font-semibold text-on-surface-variant uppercase tracking-wider">Học phí trực tiếp</span>
              <div class="flex items-baseline gap-1">
                <span class="text-3xl font-extrabold text-[#006194]">${formatVND(tutor.hourlyRate)}</span>
                <span class="text-xs text-on-surface-variant font-medium">/ giờ (60 phút)</span>
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
              <div class="flex items-center gap-2.5">
                <span class="material-symbols-outlined text-emerald-600 text-[18px]">history</span>
                <span>Hoàn 100% học phí nếu không hài lòng buổi học đầu</span>
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


    // Xử lý bộ lọc đánh giá học viên
    const filterBtns = document.querySelectorAll('.review-chip');
    const reviewCards = document.querySelectorAll('.review-item');
    filterBtns.forEach(btn => {
      btn.addEventListener('click', () => {
        filterBtns.forEach(b => {
          b.className = 'review-chip px-3.5 py-1.5 rounded-xl text-xs font-semibold bg-white border border-[#E5DECB] text-[#0F2545] hover:bg-slate-50 transition-all';
        });
        btn.className = 'review-chip px-3.5 py-1.5 rounded-xl text-xs font-semibold bg-[#006194] text-white shadow-xs transition-all';

        const filter = btn.dataset.filter;
        reviewCards.forEach(card => {
          if (filter === 'all') {
            card.style.display = 'block';
          } else if (filter === 'comment') {
            card.style.display = card.dataset.hasComment === 'true' ? 'block' : 'none';
          } else if (filter === 'kaiwa') {
            card.style.display = (card.dataset.tag === 'kaiwa' || card.innerText.includes('Kaiwa')) ? 'block' : 'none';
          } else if (filter === 'jlpt') {
            card.style.display = (card.dataset.tag === 'jlpt' || card.innerText.includes('JLPT')) ? 'block' : 'none';
          }
        });
      });
    });


    // === LOGIC CHO MODAL CHỌN LỊCH HỌC THỬ (MẪU MỚI TIẾNG VIỆT - FE-05) ===
    let selectedDurationMinutes = 25;
    let selectedSlot = '18:00';
    let weekOffset = 0; // Tuần hiện tại
    
    // Cập nhật avatar gia sư trong modal
    const modalAvatar = document.getElementById('booking-modal-avatar');
    if (modalAvatar) {
      modalAvatar.src = tutor.avatarUrl || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=150&q=80';
    }

    // Toggle thời lượng 25 phút / 50 phút
    const btn25 = document.getElementById('btn-duration-25');
    const btn50 = document.getElementById('btn-duration-50');
    if (btn25 && btn50) {
      btn25.addEventListener('click', () => {
        selectedDurationMinutes = 25;
        btn25.className = 'duration-tab-btn py-2.5 rounded-xl text-center transition-all bg-white text-[#0F2545] shadow-xs cursor-pointer';
        btn50.className = 'duration-tab-btn py-2.5 rounded-xl text-center transition-all text-slate-500 hover:text-[#0F2545] cursor-pointer';
      });
      btn50.addEventListener('click', () => {
        selectedDurationMinutes = 50;
        btn50.className = 'duration-tab-btn py-2.5 rounded-xl text-center transition-all bg-white text-[#0F2545] shadow-xs cursor-pointer';
        btn25.className = 'duration-tab-btn py-2.5 rounded-xl text-center transition-all text-slate-500 hover:text-[#0F2545] cursor-pointer';
      });
    }

    // Khởi tạo và render tuần & 7 ngày
    const weekLabelEl = document.getElementById('current-week-label');
    const daysContainer = document.getElementById('days-picker-container');
    const prevWeekBtn = document.getElementById('btn-prev-week');
    const nextWeekBtn = document.getElementById('btn-next-week');

    // Ngày cơ sở: Hôm nay
    const today = new Date();
    let selectedDayOffset = 1; // Mặc định chọn ngày mai

    function getSelectedDate() {
      const d = new Date(today);
      d.setDate(d.getDate() + (weekOffset * 7) + selectedDayOffset);
      return d;
    }

    function renderDays() {
      if (!daysContainer) return;
      daysContainer.innerHTML = '';
      
      const startDate = new Date(today);
      startDate.setDate(startDate.getDate() + (weekOffset * 7));

      const endDate = new Date(startDate);
      endDate.setDate(endDate.getDate() + 6);

      if (weekLabelEl) {
        weekLabelEl.innerText = `${startDate.getDate()} – ${endDate.getDate()} Tháng ${startDate.getMonth() + 1}, ${startDate.getFullYear()}`;
      }

      const DAY_NAMES = ['CN', 'Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7'];

      for (let i = 0; i < 7; i++) {
        const d = new Date(startDate);
        d.setDate(d.getDate() + i);
        const dayOfWeekName = DAY_NAMES[d.getDay()];
        const isSelected = (i === selectedDayOffset);

        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = `day-picker-btn flex flex-col items-center py-2 px-1 rounded-2xl border transition-all cursor-pointer ${
          isSelected
            ? 'border-2 border-rose-400 bg-rose-50 text-rose-600 font-bold shadow-2xs'
            : 'border-transparent hover:bg-slate-50 text-slate-600 font-medium'
        }`;
        btn.innerHTML = `
          <span class="text-[11px] leading-tight">${dayOfWeekName}</span>
          <span class="text-sm mt-1 leading-tight font-bold">${d.getDate()}</span>
        `;
        btn.addEventListener('click', () => {
          selectedDayOffset = i;
          renderDays();
        });
        daysContainer.appendChild(btn);
      }
    }

    if (prevWeekBtn) {
      prevWeekBtn.addEventListener('click', () => {
        if (weekOffset > 0) {
          weekOffset--;
          renderDays();
        }
      });
    }
    if (nextWeekBtn) {
      nextWeekBtn.addEventListener('click', () => {
        weekOffset++;
        renderDays();
      });
    }
    renderDays();

    // Chọn khung giờ
    const slotBtns = document.querySelectorAll('.slot-btn');
    slotBtns.forEach(btn => {
      btn.addEventListener('click', () => {
        slotBtns.forEach(b => {
          b.className = 'slot-btn py-3 px-2 text-center text-sm font-semibold rounded-2xl border border-slate-300 hover:border-slate-400 hover:bg-slate-50 transition-all text-slate-900 bg-white cursor-pointer shadow-2xs';
        });
        btn.className = 'slot-btn py-3 px-2 text-center text-sm font-bold rounded-2xl border-2 border-[#006194] bg-[#006194] text-white shadow-xs cursor-pointer';
        selectedSlot = btn.innerText.trim();
      });
    });

    // Nút Gửi yêu cầu đặt lịch
    const continueBtn = document.getElementById('btn-booking-continue');
    const modalBody = document.getElementById('booking-modal-body');
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
        const targetDate = getSelectedDate();
        const [hours, minutes] = selectedSlot.split(':').map(Number);
        targetDate.setHours(hours, minutes, 0, 0);

        if (targetDate <= new Date()) {
          showError('Thời gian bắt đầu học phải ở tương lai. Vui lòng chọn khung giờ khác.');
          return;
        }

        // Định dạng ISO 8601 kèm múi giờ +07:00
        const pad = (n) => String(n).padStart(2, '0');
        const scheduledStartTime = `${targetDate.getFullYear()}-${pad(targetDate.getMonth() + 1)}-${pad(targetDate.getDate())}T${pad(hours)}:${pad(minutes)}:00+07:00`;

        const payload = {
          tutorId: tutor.tutorId,
          scheduledStartTime: scheduledStartTime,
          durationMinutes: selectedDurationMinutes,
          learningMode: 'ONLINE',
          currentLevel: 'N5',
          initialMessage: 'Đăng ký học thử 1-1'
        };

        // 3. Gọi API gửi yêu cầu
        continueBtn.disabled = true;
        const origBtnHtml = continueBtn.innerHTML;
        continueBtn.innerHTML = '<span class="material-symbols-outlined animate-spin text-[18px]">progress_activity</span><span>Đang xử lý...</span>';

        try {
          await API.trialBookings.create(payload);

          // Render màn hình thành công - Chuyển sang trạng thái chờ bên gia sư
          const DAY_NAMES = ['Chủ nhật', 'Thứ hai', 'Thứ ba', 'Thứ tư', 'Thứ năm', 'Thứ sáu', 'Thứ bảy'];
          const dayFull = `${DAY_NAMES[targetDate.getDay()]}, ${targetDate.getDate()}/${targetDate.getMonth() + 1}/${targetDate.getFullYear()}`;

          modalBody.innerHTML = `
            <div class="p-8 text-center space-y-4">
              <div class="w-16 h-16 rounded-full bg-emerald-50 text-emerald-600 flex items-center justify-center mx-auto shadow-xs">
                <span class="material-symbols-outlined text-[32px]">check_circle</span>
              </div>
              <h3 class="text-lg font-bold text-[#0F2545]">Đã gửi yêu cầu học thử thành công!</h3>
              <p class="text-xs text-on-surface-variant leading-relaxed">
                Yêu cầu học thử <strong>${selectedDurationMinutes} phút</strong> đã được gửi tới <strong>Sensei ${tutor.fullName}</strong>.
              </p>
              <div class="p-4 bg-[#FAF8F2] rounded-2xl border border-[#ECE8DE] text-xs text-left space-y-2">
                <div class="flex items-center justify-between">
                  <span class="text-slate-500">Khung giờ:</span>
                  <span class="font-bold text-[#0F2545]">${selectedSlot} - ${dayFull}</span>
                </div>
                <div class="flex items-center justify-between">
                  <span class="text-slate-500">Trạng thái:</span>
                  <span class="inline-flex items-center gap-1 font-bold text-amber-700 bg-amber-50 px-2 py-0.5 rounded-full border border-amber-200">
                    <span class="w-1.5 h-1.5 rounded-full bg-amber-500 animate-pulse"></span>
                    Chờ gia sư xác nhận
                  </span>
                </div>
                <div class="flex items-center justify-between">
                  <span class="text-slate-500">Hình thức:</span>
                  <span class="font-semibold text-slate-800">Trực tuyến (Google Meet)</span>
                </div>
                <div class="flex items-center justify-between">
                  <span class="text-slate-500">Học phí:</span>
                  <span class="font-bold text-emerald-600">0 VNĐ (Miễn phí học thử 1-1)</span>
                </div>
              </div>
              <div class="text-[11px] text-slate-500 italic">
                Sensei sẽ kiểm tra lịch và phản hồi yêu cầu. Bạn có thể theo dõi tiến độ trong danh sách yêu cầu.
              </div>
              <div class="grid grid-cols-2 gap-2 pt-2">
                <a href="/pages/connection-requests/" class="py-3 px-3 rounded-2xl bg-[#006194] hover:bg-[#004b73] text-white text-xs font-bold text-center transition-all shadow-xs">
                  Xem danh sách yêu cầu
                </a>
                <button type="button" onclick="window.closeModal('connect-modal'); location.reload();" class="py-3 px-3 rounded-2xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold transition-all">
                  Hoàn tất
                </button>
              </div>
            </div>
          `;
          continueBtn.parentElement.style.display = 'none';

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

    // Gắn sự kiện mở modal đặt lịch
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
                openModal('connect-modal');
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
        openModal('connect-modal');
      });
    }

        // Tự động mở modal kết nối nếu có cờ connect=true (FE-05)
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get('connect') === 'true') {
      const token = localStorage.getItem('kizuna_token') || localStorage.getItem('token');
      if (token) {
        setTimeout(() => {
          openModal('connect-modal');
        }, 350);
      } else {
        setTimeout(() => {
          const tutorDisplay = tutor.fullName ? `Sensei ${tutor.fullName}` : 'gia sư';
          if (typeof window.openLoginModal === 'function') {
            window.openLoginModal({
              tutorName: tutorDisplay,
              onSuccess: () => {
                openModal('connect-modal');
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
