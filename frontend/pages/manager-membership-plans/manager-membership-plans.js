/**
 * KizunaTutor - Gói hội viên gia sư (FE-09: UC-99 cấu hình giá, UC-100 quản lý quyền lợi) cho Quản lý.
 * Nằm trong nhóm "Doanh thu & giao dịch" của sidebar Quản lý (trước đây là một phần của Cấu hình hệ thống của Admin).
 * API: /api/manager/membership-plans (GET / POST / PUT / PATCH active) — chỉ ROLE_MANAGER.
 */
import { byId, text, notice, badge, actionButton, createOperationsPage } from '../../js/pages/operations.js';
import { API } from '../../js/api.js';

const plansApi = API.managerMembership;
const state = { plans: [], plan: null, savingPlan: false };
const money = value => new Intl.NumberFormat('vi-VN').format(value) + ' đ';

function renderPlans(plans) {
  state.plans = plans;
  const rows = byId('planRows'); rows.replaceChildren();
  if (!plans.length) {
    const row = text('tr'), cell = text('td', 'Chưa có gói nào. Bấm "Thêm gói" để tạo gói đầu tiên.', 'table-empty');
    cell.colSpan = 5; row.append(cell); rows.append(row);
    return;
  }
  plans.forEach(plan => {
    const actions = text('td'), box = text('div', '', 'portal-actions');
    box.append(actionButton('Sửa', () => openPlan(plan)), actionButton(plan.active ? 'Ngừng bán' : 'Mở bán', () => togglePlan(plan), plan.active ? 'danger' : 'primary'));
    actions.append(box);
    const name = text('td'); name.append(text('strong', plan.planName));
    if (plan.benefits.length) {
      const list = text('ul', '', 'plan-benefits');
      plan.benefits.forEach(benefit => list.append(text('li', benefit)));
      name.append(list);
    }
    const status = text('td'); status.append(badge(plan.active ? 'Đang bán' : 'Ngừng bán'));
    const row = text('tr');
    row.append(name, text('td', plan.durationMonths + ' tháng'), text('td', money(plan.price)), status, actions);
    rows.append(row);
  });
}

async function load() {
  renderPlans(await plansApi.getPlans());
}

// ----- Hộp thoại gói hội viên: chọn nhanh thời hạn/giá, giá có dấu chấm ngăn cách, quyền lợi thêm từng dòng. -----
const planMoney = value => new Intl.NumberFormat('vi-VN').format(value);
const digits = value => Number(String(value).replace(/\D/g, '')) || 0;
function planMonths() { return Number(byId('planMonths').value) || 0; }
function setMonths(value) {
  const preset = ['1', '3', '6', '12'].includes(String(value));
  byId('planMonths').value = value === 'other' ? '' : value;
  byId('planMonthsOther').hidden = preset || !value;
  document.querySelectorAll('#planMonthChips button').forEach(chip => chip.setAttribute('aria-pressed', String(chip.dataset.months === (preset ? String(value) : value ? 'other' : ''))));
  if (value === 'other') byId('planMonths').focus();
  updatePerMonth();
}
function setPrice(value) { byId('planPrice').value = value ? planMoney(value) : ''; updatePerMonth(); }
function updatePerMonth() {
  const price = digits(byId('planPrice').value), months = planMonths();
  byId('planPerMonth').textContent = price && months > 1 ? 'Khoảng ' + planMoney(Math.round(price / months)) + ' đ mỗi tháng' : '';
  document.querySelectorAll('[data-price]').forEach(chip => chip.setAttribute('aria-pressed', String(Number(chip.dataset.price) === price)));
}
function planError(field, message) { const node = document.querySelector('[data-error="' + field + '"]'); if (node) node.textContent = message; }
function addBenefitRow(value = '') {
  const list = byId('planBenefitList');
  if (list.children.length >= 20) { planError('benefits', 'Mỗi gói có tối đa 20 quyền lợi.'); return; }
  const row = text('li'), input = text('input'), remove = text('button', '×', 'plan-remove');
  input.maxLength = 200; input.value = value; input.placeholder = 'Ví dụ: Hiển thị nổi bật trong tìm kiếm gia sư';
  input.setAttribute('aria-label', 'Quyền lợi ' + (list.children.length + 1));
  remove.type = 'button'; remove.setAttribute('aria-label', 'Bỏ quyền lợi này');
  remove.addEventListener('click', () => { row.remove(); if (!list.children.length) addBenefitRow(); });
  input.addEventListener('keydown', event => { if (event.key === 'Enter') { event.preventDefault(); addBenefitRow(); list.lastElementChild.querySelector('input').focus(); } });
  row.append(input, remove); list.append(row);
}

function openPlan(plan = null) {
  state.plan = plan; byId('planForm').reset(); byId('planNotice').textContent = '';
  document.querySelectorAll('.plan-error').forEach(node => { node.textContent = ''; });
  byId('planDialogTitle').textContent = plan ? 'Sửa gói hội viên' : 'Thêm gói hội viên';
  byId('planName').value = plan?.planName || '';
  setMonths(plan ? String(plan.durationMonths) : '1');
  // Thời hạn không đổi sau khi tạo để không ảnh hưởng gia sư đã mua gói.
  document.querySelectorAll('#planMonthChips button').forEach(chip => { chip.disabled = Boolean(plan); });
  byId('planMonths').disabled = Boolean(plan); byId('planMonthsLocked').hidden = !plan;
  setPrice(plan?.price || '');
  byId('planBenefitList').replaceChildren();
  (plan?.benefits.length ? plan.benefits : ['']).forEach(value => addBenefitRow(value));
  byId('planDialog').showModal(); byId('planName').focus();
}

async function savePlan(event) {
  event.preventDefault(); if (state.savingPlan) return;
  document.querySelectorAll('.plan-error').forEach(node => { node.textContent = ''; });
  const name = byId('planName').value.trim(), months = planMonths(), price = digits(byId('planPrice').value);
  const benefits = [...byId('planBenefitList').querySelectorAll('input')].map(input => input.value.trim()).filter(Boolean);
  let ok = true;
  if (!name) { planError('planName', 'Vui lòng nhập tên gói.'); ok = false; }
  if (!state.plan && (!Number.isInteger(months) || months < 1 || months > 36)) { planError('durationMonths', 'Chọn thời hạn từ 1 đến 36 tháng.'); ok = false; }
  if (price < 10000 || price > 100000000) { planError('price', 'Giá từ 10.000 đ đến 100.000.000 đ.'); ok = false; }
  if (benefits.some(line => line.length > 200)) { planError('benefits', 'Mỗi quyền lợi tối đa 200 ký tự.'); ok = false; }
  if (!ok) return;
  const payload = { planName: name, price, benefits };
  const editing = state.plan;
  state.savingPlan = true; byId('savePlan').disabled = true; byId('planNotice').textContent = '';
  try {
    if (editing) await plansApi.updatePlan(editing.planId, payload);
    else await plansApi.createPlan({ ...payload, durationMonths: months });
    byId('planDialog').close();
    await page.run(async () => { await load(); notice(editing ? 'Đã lưu thay đổi gói hội viên.' : 'Đã tạo gói hội viên.'); });
  } catch (error) {
    byId('planNotice').textContent = error.message || 'Chưa lưu được gói. Vui lòng thử lại.';
  } finally { state.savingPlan = false; byId('savePlan').disabled = false; }
}

async function togglePlan(plan) {
  if (plan.active && !confirm('Ngừng bán "' + plan.planName + '"? Gia sư đã mua vẫn dùng được đến hết hạn.')) return;
  await page.run(async () => {
    await plansApi.setActive(plan.planId, !plan.active);
    await load(); notice(plan.active ? 'Đã ngừng bán gói ' + plan.planName + '.' : 'Đã mở bán gói ' + plan.planName + '.');
  });
}

const page = createOperationsPage(load);
byId('createPlan').addEventListener('click', () => openPlan());
byId('planForm').addEventListener('submit', savePlan);
document.querySelectorAll('#planMonthChips button').forEach(chip => chip.addEventListener('click', () => setMonths(chip.dataset.months)));
byId('planMonths').addEventListener('input', updatePerMonth);
document.querySelectorAll('[data-price]').forEach(chip => chip.addEventListener('click', () => setPrice(Number(chip.dataset.price))));
// Đang gõ thì để số thường; rời ô mới thêm dấu chấm, ví dụ 229900 → 229.900.
byId('planPrice').addEventListener('input', () => { const field = byId('planPrice'), clean = field.value.replace(/[^\d.]/g, ''); if (clean !== field.value) field.value = clean; updatePerMonth(); });
byId('planPrice').addEventListener('focus', () => { const value = digits(byId('planPrice').value); byId('planPrice').value = value ? String(value) : ''; });
byId('planPrice').addEventListener('blur', () => { const value = digits(byId('planPrice').value); byId('planPrice').value = value ? planMoney(value) : ''; });
byId('addBenefit').addEventListener('click', () => { addBenefitRow(); byId('planBenefitList').lastElementChild.querySelector('input').focus(); });
['cancelPlan', 'closePlan'].forEach(id => byId(id).addEventListener('click', () => { if (!state.savingPlan) byId('planDialog').close(); }));
page.start();
