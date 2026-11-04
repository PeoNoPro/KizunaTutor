/**
 * KizunaTutor - Quản lý gói hội viên của Quản lý vận hành (FE-09: UC-99 cấu hình giá, UC-100 quản lý quyền lợi).
 */

import { API, ApiError } from '/js/api.js';
import { mountStaffLayout } from '/components/staff-layout/staff-layout.js';
import { escapeHtml, formatMoney, badge, toast } from '/js/membership-format.js';

const $ = (id) => document.getElementById(id);
const state = { plans: [], editing: null, saving: false };

function show(id) {
  ['loadingState', 'errorState', 'contentState'].forEach((s) => $(s).classList.toggle('hidden', s !== id));
}

function errorText(err) {
  return err instanceof ApiError ? err.message : 'Hệ thống đang bận, bạn vui lòng thử lại sau.';
}

function render() {
  $('emptyState').classList.toggle('hidden', state.plans.length > 0);
  $('plansBody').innerHTML = state.plans.map((plan) => `
    <tr class="border-t border-surface-container align-top">
      <td class="px-4 py-3 font-semibold">${escapeHtml(plan.planName)}</td>
      <td class="px-4 py-3">${plan.durationMonths} tháng</td>
      <td class="px-4 py-3 text-right">${formatMoney(plan.price)}</td>
      <td class="px-4 py-3 text-on-surface-variant">${plan.benefits.length ? escapeHtml(plan.benefits.slice(0, 2).join('; ')) + (plan.benefits.length > 2 ? ` (+${plan.benefits.length - 2})` : '') : '—'}</td>
      <td class="px-4 py-3">${badge(plan.activeLabel, plan.active ? 'success' : 'neutral')}</td>
      <td class="px-4 py-3 text-right whitespace-nowrap">
        <button type="button" class="tv-btn-small" data-edit="${plan.planId}">Sửa</button>
        <button type="button" class="tv-btn-small" data-toggle="${plan.planId}">${plan.active ? 'Ngừng bán' : 'Mở bán'}</button>
      </td>
    </tr>`).join('');

  $('plansBody').querySelectorAll('[data-edit]').forEach((b) => b.addEventListener('click', () => openForm(b.dataset.edit)));
  $('plansBody').querySelectorAll('[data-toggle]').forEach((b) => b.addEventListener('click', () => toggleActive(b.dataset.toggle, b)));
}

async function load() {
  show('loadingState');
  try {
    state.plans = await API.managerMembership.getPlans();
    render();
    show('contentState');
  } catch (err) {
    $('errorMessage').textContent = errorText(err);
    show('errorState');
  }
}

function clearErrors() {
  document.querySelectorAll('[data-error-for]').forEach((el) => { el.textContent = ''; el.classList.add('hidden'); });
  $('formError').classList.add('hidden');
}

function fieldError(field, message) {
  const el = document.querySelector(`[data-error-for="${field}"]`);
  if (!el) return false;
  el.textContent = message;
  el.classList.remove('hidden');
  return true;
}

function openForm(planId) {
  const plan = planId ? state.plans.find((p) => p.planId === planId) : null;
  state.editing = plan;
  clearErrors();
  $('planModalTitle').textContent = plan ? 'Sửa gói hội viên' : 'Tạo gói mới';
  $('planName').value = plan ? plan.planName : '';
  $('durationMonths').value = plan ? plan.durationMonths : '';
  $('durationMonths').disabled = Boolean(plan);
  $('price').value = plan ? plan.price : '';
  $('benefits').value = plan ? plan.benefits.join('\n') : '';
  $('planModal').classList.remove('hidden');
  $('planName').focus();
}

function closeForm() {
  if (state.saving) return;
  $('planModal').classList.add('hidden');
}

function readForm() {
  clearErrors();
  const planName = $('planName').value.trim();
  const durationMonths = Number($('durationMonths').value);
  const price = Number($('price').value);
  const benefits = $('benefits').value.split('\n').map((b) => b.trim()).filter(Boolean);
  const errors = [];
  if (!planName) errors.push(['planName', 'Vui lòng nhập tên gói.']);
  if (!state.editing && (!Number.isInteger(durationMonths) || durationMonths < 1 || durationMonths > 36)) {
    errors.push(['durationMonths', 'Số tháng phải từ 1 đến 36.']);
  }
  if ($('price').value === '' || !Number.isFinite(price) || price < 10000 || price > 100000000) {
    errors.push(['price', 'Giá gói phải từ 10.000 đ đến 100.000.000 đ.']);
  }
  if (benefits.length > 20 || benefits.some((b) => b.length > 200)) {
    errors.push(['benefits', 'Mỗi gói có tối đa 20 quyền lợi, mỗi quyền lợi tối đa 200 ký tự.']);
  }
  errors.forEach(([field, message]) => fieldError(field, message));
  return errors.length ? null : { planName, durationMonths, price: Math.round(price), benefits };
}

async function save(event) {
  event.preventDefault();
  if (state.saving) return;
  const payload = readForm();
  if (!payload) return;
  state.saving = true;
  $('saveButton').disabled = true;
  try {
    const res = state.editing
      ? await API.managerMembership.updatePlan(state.editing.planId, { planName: payload.planName, price: payload.price, benefits: payload.benefits })
      : await API.managerMembership.createPlan(payload);
    state.saving = false;
    closeForm();
    toast(res.message, 'success');
    await load();
  } catch (err) {
    const fields = err instanceof ApiError ? err.fields : {};
    const shown = Object.entries(fields || {}).map(([f, m]) => fieldError(f, m)).some(Boolean);
    if (!shown) {
      $('formError').textContent = errorText(err);
      $('formError').classList.remove('hidden');
    }
  } finally {
    state.saving = false;
    $('saveButton').disabled = false;
  }
}

async function toggleActive(planId, button) {
  const plan = state.plans.find((p) => p.planId === planId);
  if (!plan) return;
  if (plan.active && !window.confirm(`Ngừng bán "${plan.planName}"? Gia sư đã mua vẫn dùng được đến hết hạn.`)) return;
  button.disabled = true;
  try {
    const res = await API.managerMembership.setActive(planId, !plan.active);
    toast(res.message, 'success');
    await load();
  } catch (err) {
    toast(errorText(err), 'error');
    button.disabled = false;
  }
}

if (mountStaffLayout('manager', 'membershipPlans', 'Gói hội viên')) {
$('createButton').addEventListener('click', () => openForm(null));
$('cancelButton').addEventListener('click', closeForm);
$('planForm').addEventListener('submit', save);
$('planModal').addEventListener('click', (event) => { if (event.target.id === 'planModal') closeForm(); });
document.addEventListener('keydown', (event) => { if (event.key === 'Escape') closeForm(); });
$('retryButton').addEventListener('click', load);
load();
}
