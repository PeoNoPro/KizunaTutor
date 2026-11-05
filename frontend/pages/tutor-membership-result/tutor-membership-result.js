/**
 * KizunaTutor - Trang kết quả thanh toán (FE-09, phương án C).
 * VNPay chuyển gia sư về đây kèm tham số vnp_*; backend kiểm chữ ký, tra cứu cổng nếu giao dịch còn chờ, rồi trả kết quả.
 * Trang không tự kết luận thành công từ tham số URL.
 */

import { API, ApiError } from '/js/api.js';
import { mountTutorLayout, setTutorIdentity } from '/components/tutor-layout/tutor-layout.js';
import { escapeHtml, formatMoney, formatDate } from '/js/membership-format.js';

const $ = (id) => document.getElementById(id);

const VIEW = {
  SUCCESS: { icon: 'check_circle', color: 'text-success', title: 'Thanh toán thành công' },
  FAILED: { icon: 'cancel', color: 'text-error', title: 'Thanh toán chưa thành công' },
  PENDING: { icon: 'hourglass_top', color: 'text-secondary', title: 'Đang chờ xác nhận' }
};

function show(id) {
  ['loadingState', 'resultState', 'errorState'].forEach((s) => $(s).classList.toggle('hidden', s !== id));
}

function detailRow(label, value) {
  return `<dt class="text-on-surface-variant">${escapeHtml(label)}</dt><dd class="font-semibold">${escapeHtml(value)}</dd>`;
}

function render(result) {
  const view = VIEW[result.status] || VIEW.PENDING;
  const icon = $('resultIcon');
  icon.textContent = view.icon;
  icon.className = `material-symbols-outlined text-6xl ${view.color}`;
  $('resultTitle').textContent = view.title;
  $('resultMessage').textContent = result.resultMessage || '';

  const rows = [detailRow('Gói', result.planName || '—'), detailRow('Số tiền', formatMoney(result.amount))];
  if (result.membership) {
    rows.push(detailRow('Hiệu lực', `${formatDate(result.membership.startDate)} – ${formatDate(result.membership.endDate)}`));
  }
  rows.push(detailRow('Trạng thái', result.statusLabel || '—'));
  $('resultDetails').innerHTML = rows.join('');

  $('checkAgainButton').classList.toggle('hidden', result.status !== 'PENDING');
  $('retryPayLink').classList.toggle('hidden', result.status !== 'FAILED');
  show('resultState');
}

async function confirm() {
  const query = window.location.search;
  if (!new URLSearchParams(query).get('vnp_TxnRef')) {
    $('errorMessage').textContent = 'Không tìm thấy thông tin giao dịch. Vui lòng xem lại lịch sử giao dịch trong trang gói hội viên.';
    $('retryButton').classList.add('hidden');
    show('errorState');
    return;
  }
  show('loadingState');
  try {
    render(await API.tutorMembership.confirmReturn(query));
  } catch (err) {
    $('errorMessage').textContent = err instanceof ApiError ? err.message : 'Hệ thống đang bận, bạn vui lòng thử lại sau.';
    show('errorState');
  }
}

async function loadIdentity() {
  try {
    const data = await API.tutorVerification.getMine();
    const profile = data.profile || {};
    setTutorIdentity({ fullName: profile.fullName, jlptLevel: profile.highestJlptLevel, statusLabel: data.statusLabel });
  } catch (e) {
    // Không chặn trang kết quả khi không tải được thông tin hiển thị trên sidebar
  }
}

mountTutorLayout('membership');
loadIdentity();
$('retryButton').addEventListener('click', confirm);
$('checkAgainButton').addEventListener('click', confirm);
confirm();
