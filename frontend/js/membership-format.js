/**
 * KizunaTutor - Định dạng dùng chung cho các trang gói hội viên và thanh toán (FE-09).
 * Số tiền "199.000 đ", ngày "dd/MM/yyyy"; mọi chuỗi chèn vào HTML phải qua escapeHtml.
 */

export function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text == null ? '' : String(text);
  return div.innerHTML;
}

export function formatMoney(amount) {
  const value = Number(amount || 0);
  return `${new Intl.NumberFormat('vi-VN').format(value)} đ`;
}

export function formatDate(value) {
  if (!value) return '—';
  const text = String(value);
  const match = text.match(/^(\d{4})-(\d{2})-(\d{2})/);
  if (match && text.length === 10) return `${match[3]}/${match[2]}/${match[1]}`;
  const d = new Date(text);
  if (Number.isNaN(d.getTime())) return '—';
  return `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}/${d.getFullYear()}`;
}

export function formatDateTime(value) {
  if (!value) return '—';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return '—';
  return `${formatDate(value)} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}

const TONES = {
  success: 'bg-emerald-50 text-emerald-700',
  warning: 'bg-amber-50 text-amber-700',
  danger: 'bg-red-50 text-red-700',
  info: 'bg-sky-50 text-sky-700',
  neutral: 'bg-surface-container text-on-surface-variant'
};

/** Màu nhãn theo mã trạng thái nội bộ; chữ hiển thị luôn là nhãn tiếng Việt từ API. */
export function statusTone(code, label = '') {
  if (code === 'SUCCESS') return 'success';
  if (code === 'FAILED') return 'danger';
  if (code === 'PENDING') return 'warning';
  if (code === 'ACTIVE') return String(label).startsWith('Đã gia hạn') ? 'info' : 'success';
  if (code === 'EXPIRED' || code === 'CANCELLED') return 'neutral';
  return 'neutral';
}

export function badge(label, tone = 'neutral') {
  return `<span class="inline-flex items-center px-2.5 py-1 rounded-full text-label-sm whitespace-nowrap ${TONES[tone] || TONES.neutral}">${escapeHtml(label)}</span>`;
}

export function statusBadge(code, label) {
  return badge(label, statusTone(code, label));
}

export async function toast(message, type = 'info') {
  try {
    const { showToast } = await import('/js/main.js');
    showToast(message, type);
  } catch (e) {
    window.alert(message);
  }
}
