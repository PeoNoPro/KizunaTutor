/**
 * KizunaTutor - Main Application Bootstrap & Utilities
 * Dùng chung cho toàn bộ các trang trong hệ thống.
 * Cung cấp cơ chế nạp Header / Footer từ components/ để đồng bộ toàn dự án.
 */

// 1. Định dạng tiền tệ VND
export function formatVND(amount) {
  if (!amount) return '0 đ';
  return new Intl.NumberFormat('vi-VN').format(amount) + ' đ';
}

// 2. Debounce hỗ trợ ô nhập tìm kiếm
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

// 3. Hiển thị thông báo Toast lịch sự (Sentence case theo AGENTS.md)
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

// 4. Khởi tạo trạng thái thanh điều hướng (Active Navigation)
export function initNavigation() {
  const currentPath = window.location.pathname;
  const navLinks = document.querySelectorAll('header nav a');

  navLinks.forEach(link => {
    const href = link.getAttribute('href');
    if (!href) return;

    // Kiểm tra active cho trang Tìm gia sư (hỗ trợ cả /tutors, /pages/tutor-discovery/)
    const isTutorDiscovery = (currentPath.startsWith('/tutors') || currentPath.includes('tutor-discovery')) &&
                             (href.includes('tutor-discovery') || href === '/tutors');

    // Kiểm tra active cho trang Yêu cầu đã gửi
    const isRequests = currentPath.includes('connection-requests') && href.includes('connection-requests');

    // Kiểm tra active cho Trang chủ
    const isHome = (currentPath === '/' || currentPath === '/home' || currentPath.includes('/pages/home/')) &&
                   (href === '/pages/home/' || href === '/' || href === '/home');

    if (isTutorDiscovery || isRequests || (isHome && !href.includes('#'))) {
      link.className = 'inline-flex items-center gap-2 px-3.5 py-2 rounded-xl text-sm font-semibold transition-colors bg-[#E0EFF7] text-[#006194]';
      link.setAttribute('aria-current', 'page');
    } else {
      link.className = 'inline-flex items-center gap-2 px-3.5 py-2 rounded-xl text-sm font-medium text-[#3F4850] hover:text-[#006194] hover:bg-[#FAF8F2] transition-colors';
      link.removeAttribute('aria-current');
    }
  });

  // Toggle Mobile Menu
  const mobileMenuBtn = document.getElementById('mobileMenuBtn');
  const mobileMenu = document.getElementById('mobileMenu');
  if (mobileMenuBtn && mobileMenu && !mobileMenuBtn.hasAttribute('data-has-listener')) {
    mobileMenuBtn.setAttribute('data-has-listener', 'true');
    mobileMenuBtn.addEventListener('click', () => {
      mobileMenu.classList.toggle('hidden');
    });
  }
}

// 5. Nạp Header / Footer dùng chung từ thư mục components/ (Component Loader)
export async function loadSharedComponents() {
  // Nạp Header từ components/header/header.html
  const headerPlaceholder = document.getElementById('header-placeholder') || 
                            document.getElementById('header-container') || 
                            document.querySelector('[data-component="header"]');
  if (headerPlaceholder) {
    try {
      const res = await fetch('/components/header/header.html');
      if (res.ok) {
        const html = await res.text();
        headerPlaceholder.outerHTML = html;
        initNavigation();
      } else {
        console.error('Không tìm thấy file header.html:', res.status);
      }
    } catch (err) {
      console.error('Lỗi khi nạp Header component:', err);
    }
  } else {
    initNavigation();
  }

  // Nạp Footer từ components/footer/footer.html
  const footerPlaceholder = document.getElementById('footer-placeholder') || 
                            document.getElementById('footer-container') || 
                            document.querySelector('[data-component="footer"]');
  if (footerPlaceholder) {
    try {
      const res = await fetch('/components/footer/footer.html');
      if (res.ok) {
        const html = await res.text();
        footerPlaceholder.outerHTML = html;
      } else {
        console.error('Không tìm thấy file footer.html:', res.status);
      }
    } catch (err) {
      console.error('Lỗi khi nạp Footer component:', err);
    }
  }
}

// Khởi chạy tự động khi DOM sẵn sàng
document.addEventListener('DOMContentLoaded', () => {
  loadSharedComponents();
});

// Gắn toàn cục
if (typeof window !== 'undefined') {
  window.formatVND = formatVND;
  window.debounce = debounce;
  window.showToast = showToast;
  window.loadSharedComponents = loadSharedComponents;
  window.initNavigation = initNavigation;
}
