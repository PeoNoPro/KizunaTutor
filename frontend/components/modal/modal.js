/**
 * KizunaTutor Modal Component Controller
 */

export function openModal(modalId) {
  const modal = document.getElementById(modalId);
  if (!modal) return;
  modal.classList.remove('hidden');
  document.body.classList.add('overflow-hidden');
}

export function closeModal(modalId) {
  const modal = document.getElementById(modalId);
  if (!modal) return;
  modal.classList.add('hidden');
  document.body.classList.remove('overflow-hidden');
}

// Tự động đóng modal khi nhấn phím ESC hoặc click vào vùng backdrop
document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') {
    const visibleModals = document.querySelectorAll('.modal-overlay:not(.hidden)');
    visibleModals.forEach(m => {
      m.classList.add('hidden');
    });
    document.body.classList.remove('overflow-hidden');
  }
});

if (typeof window !== 'undefined') {
  window.openModal = openModal;
  window.closeModal = closeModal;
}

