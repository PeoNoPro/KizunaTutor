/**
 * KizunaTutor - Home Page Logic
 * Tải danh sách gia sư tiêu biểu từ REST API Database Supabase (UC-77)
 */

import { API } from '../../js/api.js';
import { formatVND } from '../../js/main.js';

document.addEventListener('DOMContentLoaded', async () => {
  const featuredContainer = document.getElementById('featured-tutors-grid');
  if (!featuredContainer) return;

  try {
    const featuredTutors = await API.tutors.getFeatured();

    if (!featuredTutors || featuredTutors.length === 0) {
      featuredContainer.innerHTML = `
        <div class="col-span-full p-8 text-center bg-white rounded-2xl border border-[#E5DECB]">
          <p class="text-sm text-on-surface-variant">Hiện chưa có gia sư tiêu biểu nào trong hệ thống.</p>
        </div>
      `;
      return;
    }

    featuredContainer.innerHTML = featuredTutors.map(tutor => `
      <article class="bg-white border border-[#E5DECB] rounded-2xl p-6 shadow-sm hover:shadow-md transition-all flex flex-col justify-between group">
        <div class="space-y-4">
          <div class="flex items-start gap-4">
            <img class="w-16 h-16 rounded-full object-cover border border-[#E5DECB] shrink-0" 
                 src="${tutor.avatarUrl || 'https://lh3.googleusercontent.com/aida-public/AB6AXuDfWr7LjTJCoD0Gf2VNK0QdLAElZ9cyCzJkQ8IOJ7ZhfKFRiPoiK-N6PJaofpuz4ZSh0MCTyzeYN1pX3x0Uaq5OcxZn8yQJ0H4V9S7DtsrI2aXdgxRQ0o8NMGuIO63NK6Y74ZNBD0G7rqPan3Z9H-N80LBRdViJvA9zYNeJLWG53XmbteXaiLIE6gVUR1BHJBqSmUiPALj-L4qvCcRXCinX5eARKyo5T6tbevnMHOs'}" 
                 alt="${tutor.fullName}">
            <div class="flex-1 min-w-0">
              <div class="flex items-center justify-between gap-1">
                <h3 class="font-bold text-[#0F2545] text-base group-hover:text-primary transition-colors truncate">
                  ${tutor.fullName}
                </h3>
                <div class="flex items-center text-amber-500 font-bold text-xs shrink-0">
                  <span class="material-symbols-outlined text-[16px]" style="font-variation-settings: 'FILL' 1;">star</span>
                  <span>${Number(tutor.avgRating || 5.0).toFixed(1)}</span>
                </div>
              </div>
              <p class="text-xs text-primary font-medium truncate mt-0.5">${tutor.headline || 'Gia sư tiếng Nhật'}</p>
              <div class="flex items-center gap-2 mt-2">
                <span class="px-2 py-0.5 rounded bg-sky-50 text-sky-800 text-[11px] font-bold border border-sky-200">
                  JLPT ${tutor.highestJlptLevel || 'N1'}
                </span>
                <span class="text-xs text-on-surface-variant">${tutor.experienceYears || 0} năm kinh nghiệm</span>
              </div>
            </div>
          </div>
          <p class="text-xs text-on-surface-variant line-clamp-2 leading-relaxed bg-[#FAF8F2] p-3 rounded-xl border border-[#ECE8DE]">
            ${tutor.bio || 'Nhiệt tình, chú trọng phương pháp trực quan và luyện chữ Hán chuẩn nét.'}
          </p>
        </div>

        <div class="pt-4 mt-4 border-t border-[#ECE8DE] flex items-center justify-between">
          <div>
            <span class="text-[11px] text-on-surface-variant block">Học phí:</span>
            <span class="font-bold text-primary text-sm">${formatVND(tutor.hourlyRate)} <span class="text-[10px] font-normal text-on-surface-variant">/ giờ</span></span>
          </div>
          <a href="/pages/tutor-profile/?id=${tutor.tutorId}" class="px-4 py-2 rounded-lg bg-primary hover:bg-primary-container text-white text-xs font-semibold shadow-sm transition-all">
            Xem hồ sơ
          </a>
        </div>
      </article>
    `).join('');

  } catch (err) {
    console.error('Không thể tải gia sư tiêu biểu:', err);
    featuredContainer.innerHTML = `
      <div class="col-span-full p-8 text-center bg-white rounded-2xl border border-[#E5DECB]">
        <p class="text-sm text-on-surface-variant">Đang kết nối cơ sở dữ liệu gia sư tiêu biểu...</p>
      </div>
    `;
  }
});

