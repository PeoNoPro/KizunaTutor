/**
 * KizunaTutor - Main Application Bootstrap & Utilities
 * Dùng chung cho toàn bộ các trang trong hệ thống
 */

// Định dạng tiền tệ VND
export function formatVND(amount) {
  if (!amount) return '0 đ';
  return new Intl.NumberFormat('vi-VN').format(amount) + ' đ';
}

// Debounce hỗ trợ ô nhập tìm kiếm
export function debounce(func, wait = 300) {
  let timeout;
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout);
      func(...args);
    };
    clearTimeout(timeout);
    timeout = setTimeout(later, wait);
  };
}

// Hiển thị thông báo Toast lịch sự (Sentence case theo AGENTS.md)
export function showToast(message, type = 'info') {
  let toastContainer = document.getElementById('toast-container');
  if (!toastContainer) {
    toastContainer = document.createElement('div');
    toastContainer.id = 'toast-container';
    toastContainer.className = 'fixed bottom-5 right-5 z-50 flex flex-col gap-2 pointer-events-none';
    document.body.appendChild(toastContainer);
  }

  const toast = document.createElement('div');
  const bgClass = type === 'success' ? 'bg-emerald-700 text-white' : (type === 'error' ? 'bg-rose-700 text-white' : 'bg-[#0F2545] text-white');
  
  toast.className = `${bgClass} px-4 py-3 rounded-xl shadow-lg text-xs md:text-sm font-medium flex items-center gap-2 transition-all duration-300 transform translate-y-2 opacity-0 pointer-events-auto max-w-md`;
  toast.innerHTML = `
    <span class="material-symbols-outlined text-[18px]">${type === 'success' ? 'check_circle' : (type === 'error' ? 'error' : 'info')}</span>
    <span>${message}</span>
  `;

  toastContainer.appendChild(toast);

  // Animation In
  setTimeout(() => {
    toast.classList.remove('translate-y-2', 'opacity-0');
  }, 10);

  // Auto dismiss sau 3.5s
  setTimeout(() => {
    toast.classList.add('translate-y-2', 'opacity-0');
    setTimeout(() => toast.remove(), 300);
  }, 3500);
}

// Khởi tạo trạng thái thanh điều hướng (Active Navigation)
document.addEventListener('DOMContentLoaded', () => {
  const currentPath = window.location.pathname;
  const navLinks = document.querySelectorAll('header nav a');

  navLinks.forEach(link => {
    const href = link.getAttribute('href');
    if (href && currentPath.includes(href.replace('.html', '').replace('/', ''))) {
      link.classList.add('bg-[#006194]', 'text-white', 'font-semibold', 'shadow-sm');
      link.classList.remove('text-[#3F4850]', 'hover:bg-[#FAF8F2]');
    }
  });

  // Toggle Mobile Menu
  const mobileMenuBtn = document.getElementById('mobileMenuBtn');
  const mobileMenu = document.getElementById('mobileMenu');
  if (mobileMenuBtn && mobileMenu) {
    mobileMenuBtn.addEventListener('click', () => {
      mobileMenu.classList.toggle('hidden');
    });
  }
});

// Gắn toàn cục
if (typeof window !== 'undefined') {
  window.formatVND = formatVND;
  window.debounce = debounce;
  window.showToast = showToast;
}

