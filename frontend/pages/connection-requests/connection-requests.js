/**
 * KizunaTutor - Connection Requests Page Logic (UC-30)
 * Hiển thị danh sách yêu cầu kết nối học thử từ Database
 */

import { API } from '../../js/api.js';

document.addEventListener('DOMContentLoaded', async () => {
  const container = document.getElementById('requests-list');
  const emptyState = document.getElementById('requests-empty-state');
  if (!container) return;

  try {
    const requests = await API.connectionRequests.getMyRequests();

    if (!requests || requests.length === 0) {
      if (emptyState) emptyState.classList.remove('hidden');
      container.innerHTML = '';
      return;
    }

    if (emptyState) emptyState.classList.add('hidden');
    container.innerHTML = requests.map(req => `
      <div class="card-base space-y-4">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-3">
            <span class="w-10 h-10 rounded-full bg-primary/10 text-primary flex items-center justify-center font-bold">KT</span>
            <div>
              <h4 class="font-bold text-[#0F2545] text-sm">${req.tutorName || 'Gia sư tiếng Nhật'}</h4>
              <p class="text-xs text-on-surface-variant">Ngày gửi: ${new Date(req.createdAt || Date.now()).toLocaleDateString('vi-VN')}</p>
            </div>
          </div>
          <span class="badge-verified">${req.status || 'Đang chờ xử lý'}</span>
        </div>
        <p class="text-xs text-on-surface-variant bg-[#FAF8F2] p-3 rounded-xl border border-[#ECE8DE]">
          ${req.initialMessage || 'Yêu cầu kết nối học thử 1-1'}
        </p>
      </div>
    `).join('');

  } catch (err) {
    if (emptyState) emptyState.classList.remove('hidden');
    container.innerHTML = '';
  }
});

