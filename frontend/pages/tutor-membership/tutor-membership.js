/**
 * KizunaTutor - Trang gói hội viên của gia sư (FE-09: UC-97 xem gói và ngày hết hạn, UC-104 mua gói qua VNPay).
 * Dữ liệu lấy từ API thật; không có dữ liệu thì hiển thị trạng thái rỗng.
 */

import { API, ApiError } from '/js/api.js';
import { mountTutorLayout, setTutorIdentity } from '/components/tutor-layout/tutor-layout.js';
import { escapeHtml, formatMoney, formatDate, formatDateTime, statusBadge, badge, toast } from '/js/membership-format.js';

const PAGE_SIZE = 10;
const $ = (id) => document.getElementById(id);

const state = { mine: null, plans: [], paymentsPage: 0, selectedPlan: null, submitting: false };

function show(section) {
  ['loadingState', 'errorState', 'contentState'].forEach((id) => $(id).classList.toggle('hidden', id !== section));
}

function errorText(err) {
  return err instanceof ApiError ? err.message : 'Hệ thống đang bận, bạn vui lòng thử lại sau.';
}

function renderCurrent() {
  const { mine } = state;
  $('tutorStateBadge').innerHTML = mine.tutorStateLabel ? badge(mine.tutorStateLabel, mine.tutorState === 'ACTIVE_PAID' ? 'success' : 'neutral') : '';
  const current = mine.current;
  const block = $('currentBlock');

  if (current) {
    block.innerHTML = `
      <div class="flex flex-col md:flex-row md:items-center justify-between gap-4 rounded-xl bg-surface-container-low p-4">
        <div>
          <p class="text-title-md font-semibold">${escapeHtml(current.planName)}</p>
          <p class="text-body-sm text-on-surface-variant mt-1">Từ ${formatDate(current.startDate)} đến ${formatDate(current.endDate)}</p>
        </div>
        <div class="flex items-center gap-3 flex-wrap">
          ${statusBadge(current.status, current.statusLabel)}
          <span class="text-body-md">Còn <strong>${current.daysLeft}</strong> ngày</span>
        </div>
      </div>
      ${mine.expiresOn && mine.expiresOn !== current.endDate
        ? `<p class="text-body-sm text-on-surface-variant mt-3">Tính cả các gói đã gia hạn, gói của bạn có hiệu lực đến <strong>${formatDate(mine.expiresOn)}</strong>.</p>` : ''}`;
  } else {
    const hadBefore = (mine.history || []).length > 0;
    block.innerHTML = `
      <div class="rounded-xl bg-surface-container-low p-6 text-center">
        <span class="material-symbols-outlined text-4xl text-outline">${hadBefore ? 'event_busy' : 'workspace_premium'}</span>
        <p class="text-title-md mt-2">${hadBefore ? 'Gói hội viên của bạn đã hết hạn' : 'Bạn chưa có gói hội viên'}</p>
        <p class="text-body-sm text-on-surface-variant mt-1">${hadBefore ? 'Gia hạn để tiếp tục nhận học viên và dùng đầy đủ tính năng giảng dạy.' : 'Chọn một gói bên dưới để bắt đầu nhận học viên.'}</p>
      </div>`;
  }

  const upcoming = mine.upcoming || [];
  $('upcomingBlock').classList.toggle('hidden', upcoming.length === 0);
  $('upcomingList').innerHTML = upcoming.map((m) => `
    <li class="flex items-center justify-between gap-3 rounded-lg bg-surface-container-low px-4 py-3">
      <span class="text-body-md">${escapeHtml(m.planName)} · ${formatDate(m.startDate)} – ${formatDate(m.endDate)}</span>
      ${statusBadge(m.status, m.statusLabel)}
    </li>`).join('');

  $('ineligibleBanner').classList.toggle('hidden', mine.eligible);
  $('ineligibleText').textContent = mine.ineligibleReason || '';
}

function renderPlans() {
  const grid = $('plansGrid');
  $('plansEmpty').classList.toggle('hidden', state.plans.length > 0);
  const hasValid = Boolean(state.mine.expiresOn);
  grid.innerHTML = state.plans.map((plan) => `
    <article class="tv-card p-6 flex flex-col">
      <h3 class="text-title-md font-semibold">${escapeHtml(plan.planName)}</h3>
      <p class="text-body-sm text-on-surface-variant mt-1">${plan.durationMonths} tháng</p>
      <p class="text-headline-sm text-primary mt-3">${formatMoney(plan.price)}</p>
      <ul class="mt-4 flex flex-col gap-2 flex-1">
        ${(plan.benefits || []).map((b) => `
          <li class="flex items-start gap-2 text-body-sm"><span class="material-symbols-outlined text-[18px] text-primary">check_circle</span><span>${escapeHtml(b)}</span></li>`).join('')
        || '<li class="text-body-sm text-on-surface-variant">Chưa có mô tả quyền lợi.</li>'}
      </ul>
      <button type="button" class="tv-btn tv-btn-primary mt-6 w-full" data-plan-id="${plan.planId}" ${state.mine.eligible ? '' : 'disabled'}>
        ${hasValid ? 'Gia hạn bằng gói này' : 'Đăng ký gói'}
      </button>
    </article>`).join('');

  grid.querySelectorAll('[data-plan-id]').forEach((button) => {
    button.addEventListener('click', () => openConfirm(button.dataset.planId));
  });
}

async function loadPayments(page = 0) {
  const block = $('paymentsBlock');
  try {
    const data = await API.tutorMembership.getPayments(page, PAGE_SIZE);
    state.paymentsPage = data.page;
    if (!data.items.length) {
      block.innerHTML = '<p class="text-body-md text-on-surface-variant">Bạn chưa có giao dịch nào.</p>';
      $('paymentsPager').innerHTML = '';
      return;
    }
    block.innerHTML = `
      <table class="w-full text-left text-body-sm min-w-[560px]">
        <thead class="text-on-surface-variant"><tr>
          <th class="py-2 pr-4 font-semibold">Thời gian</th><th class="py-2 pr-4 font-semibold">Gói</th>
          <th class="py-2 pr-4 font-semibold text-right">Số tiền</th><th class="py-2 pr-4 font-semibold">Trạng thái</th>
          <th class="py-2"></th>
        </tr></thead>
        <tbody>
          ${data.items.map((p) => `
            <tr class="border-t border-surface-container">
              <td class="py-3 pr-4">${formatDateTime(p.transactionTime)}</td>
              <td class="py-3 pr-4">${escapeHtml(p.planName)}</td>
              <td class="py-3 pr-4 text-right">${formatMoney(p.amount)}</td>
              <td class="py-3 pr-4">${statusBadge(p.status, p.statusLabel)}</td>
              <td class="py-3 text-right whitespace-nowrap">${p.status === 'PENDING' ? `
                <button type="button" class="tv-btn-small" data-check="${p.transactionId}" title="Dùng khi bạn đã thanh toán nhưng gói chưa được kích hoạt">Kiểm tra kết quả</button>
                ${p.resumable ? `<button type="button" class="tv-btn-small" data-resume="${p.transactionId}">Tiếp tục thanh toán</button>` : ''}` : ''}
              </td>
            </tr>`).join('')}
        </tbody>
      </table>`;
    block.querySelectorAll('[data-check]').forEach((b) => b.addEventListener('click', () => checkPayment(b.dataset.check, b)));
    block.querySelectorAll('[data-resume]').forEach((b) => b.addEventListener('click', () => resumePayment(b.dataset.resume, b)));
    renderPager(data);
  } catch (err) {
    block.innerHTML = `<p class="text-body-md text-error">${escapeHtml(errorText(err))}</p>`;
  }
}

async function checkPayment(transactionId, button) {
  button.disabled = true;
  button.textContent = 'Đang kiểm tra…';
  try {
    const result = await API.tutorMembership.checkPayment(transactionId);
    toast(result.resultMessage, result.status === 'SUCCESS' ? 'success' : result.status === 'FAILED' ? 'error' : 'info');
    await load();
  } catch (err) {
    toast(errorText(err), 'error');
    button.disabled = false;
    button.textContent = 'Kiểm tra kết quả';
  }
}

async function resumePayment(transactionId, button) {
  button.disabled = true;
  try {
    const resumed = await API.tutorMembership.resumePayment(transactionId);
    window.location.href = resumed.paymentUrl;
  } catch (err) {
    toast(errorText(err), 'error');
    button.disabled = false;
    loadPayments(state.paymentsPage);
  }
}

function renderPager(data) {
  const pager = $('paymentsPager');
  if (data.totalPages <= 1) {
    pager.innerHTML = '';
    return;
  }
  pager.innerHTML = `
    <button type="button" class="tv-btn-small" data-page="${data.page - 1}" ${data.page <= 0 ? 'disabled' : ''}>Trang trước</button>
    <span class="text-body-sm text-on-surface-variant">Trang ${data.page + 1}/${data.totalPages}</span>
    <button type="button" class="tv-btn-small" data-page="${data.page + 1}" ${data.page + 1 >= data.totalPages ? 'disabled' : ''}>Trang sau</button>`;
  pager.querySelectorAll('[data-page]').forEach((b) => b.addEventListener('click', () => loadPayments(Number(b.dataset.page))));
}

function openConfirm(planId) {
  const plan = state.plans.find((p) => p.planId === planId);
  if (!plan) return;
  state.selectedPlan = plan;
  $('confirmPlan').textContent = plan.planName;
  $('confirmDuration').textContent = `${plan.durationMonths} tháng`;
  $('confirmAmount').textContent = formatMoney(plan.price);
  const expiresOn = state.mine.expiresOn;
  $('confirmStackNote').classList.toggle('hidden', !expiresOn);
  if (expiresOn) {
    $('confirmStackNote').textContent = `Bạn đang có gói hiệu lực đến ${formatDate(expiresOn)}. Gói mới sẽ bắt đầu ngay sau ngày này.`;
  }
  $('confirmError').classList.add('hidden');
  $('confirmModal').classList.remove('hidden');
  $('confirmSubmit').focus();
}

function closeConfirm() {
  if (state.submitting) return;
  $('confirmModal').classList.add('hidden');
}

async function submitPayment() {
  if (!state.selectedPlan || state.submitting) return;
  state.submitting = true;
  const button = $('confirmSubmit');
  button.disabled = true;
  button.lastChild.textContent = 'Đang chuyển tới VNPay…';
  try {
    const created = await API.tutorMembership.createPayment(state.selectedPlan.planId);
    window.location.href = created.paymentUrl;
  } catch (err) {
    $('confirmError').textContent = errorText(err);
    $('confirmError').classList.remove('hidden');
    state.submitting = false;
    button.disabled = false;
    button.lastChild.textContent = 'Thanh toán qua VNPay';
  }
}

async function load() {
  show('loadingState');
  try {
    const [mine, plans] = await Promise.all([API.tutorMembership.getMine(), API.tutorMembership.getPlans()]);
    state.mine = mine;
    state.plans = plans || [];
    renderCurrent();
    renderPlans();
    show('contentState');
    loadPayments(0);
  } catch (err) {
    $('errorMessage').textContent = errorText(err);
    show('errorState');
  }
}

async function loadIdentity() {
  try {
    const data = await API.tutorVerification.getMine();
    const profile = data.profile || {};
    setTutorIdentity({ fullName: profile.fullName, jlptLevel: profile.highestJlptLevel, statusLabel: data.statusLabel });
  } catch (e) {
    // Không chặn trang khi không tải được thông tin hiển thị trên sidebar
  }
}

mountTutorLayout('membership');
$('retryButton').addEventListener('click', load);
$('confirmCancel').addEventListener('click', closeConfirm);
$('confirmSubmit').addEventListener('click', submitPayment);
$('confirmModal').addEventListener('click', (event) => { if (event.target.id === 'confirmModal') closeConfirm(); });
document.addEventListener('keydown', (event) => { if (event.key === 'Escape') closeConfirm(); });
loadIdentity();
load();
