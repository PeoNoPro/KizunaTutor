/**
 * KizunaTutor - Tutor Profile Detail Page Logic (UC-31)
 * Hỗ trợ URL ngắn /gs/:tutorCode (ví dụ: /gs/10001), ?code=10001 hoặc ?id=<UUID>.
 * Lấy thông tin chi tiết từ Database Supabase qua REST API.
 */

import { API } from '../../js/api.js';
import { formatVND } from '../../js/main.js';
import { openModal, closeModal } from '../../components/modal/modal.js';

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

          <!-- Lịch rảnh & Cam kết -->
          <div class="bg-white border border-[#E5DECB] rounded-2xl p-6 md:p-8 shadow-sm space-y-4">
            <h3 class="text-lg font-bold text-[#0F2545] flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">event_available</span>
              Thời gian biểu &amp; Lịch giảng dạy
            </h3>
            <div class="p-4 rounded-xl bg-slate-50 border border-slate-200 text-center space-y-1">
              <p class="text-xs text-on-surface-variant">
                Gia sư hiện tại nhận lịch học linh hoạt theo thỏa thuận. Vui lòng bấm <strong>"Đặt lịch học thử 1-1"</strong> để trao đổi khung giờ học phù hợp nhất với bạn.
              </p>
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

    // Gắn sự kiện mở modal đặt lịch
    const openBtn = document.getElementById('open-booking-btn');
    if (openBtn) {
      openBtn.addEventListener('click', () => {
        openModal('connect-modal');
      });
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
