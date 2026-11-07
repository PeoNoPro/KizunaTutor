import { OPERATIONS_BASE as base, api, byId, text, date, notice, paging, emptyState, badge, actionButton, createOperationsPage } from '../../js/pages/operations.js';
import { API, fetchAPI } from '../../js/api.js';
import { API_CONFIG } from '../../js/config/api.config.js';

const state = {historyPage: 0, dirty: false, senderDirty: false, aiDirty: false, version: null, current: null, senderVersion: null, sender: null, ai: null, plans: [], plan: null, savingPlan: false};
const PATH = base + '/settings/attachment-limit';
const SENDER_PATH = base + '/settings/mail-sender';
const AI_PATH = base + '/settings/ai';
// Gói hội viên dùng API của phân hệ thanh toán (FE-09, đăng nhập bằng token): Admin tạo/sửa, Quản lý chỉ xem.
const plansApi = API.managerMembership;
const money = value => new Intl.NumberFormat('vi-VN').format(value) + ' đ';

function renderAi(ai) {
    state.ai = ai;
    const select = byId('aiProvider');
    if (!state.aiDirty) {
        select.replaceChildren(...ai.providers.map(item => { const option = text('option', item.label); option.value = item.code; return option; }));
        select.value = ai.provider; byId('aiModel').value = ai.model; byId('aiApiKey').value = '';
        byId('aiBaseUrl').value = ai.providers.find(item => item.code === 'CUSTOM')?.baseUrl || '';
    }
    byId('aiCurrent').textContent = 'Đang dùng: ' + ai.providerLabel + ' · ' + ai.model + (ai.keyConfigured ? '' : '. Chưa có API key nên đang chấm bằng bộ dự phòng.');
    syncAiNote();
}

/** Ô API key không bao giờ hiện key cũ; chỉ hiện 4 ký tự cuối để Admin biết đã có key. Để trống = giữ key cũ. */
function syncAiNote() {
    const chosen = state.ai?.providers.find(item => item.code === byId('aiProvider').value);
    if (!chosen) return;
    const custom = chosen.code === 'CUSTOM';
    byId('aiBaseUrlField').hidden = !custom; byId('aiBaseUrl').required = custom;
    byId('aiApiKey').required = !chosen.keyConfigured;
    byId('aiApiKey').placeholder = chosen.keyConfigured ? 'Đã có key ' + chosen.maskedKey + ', để trống nếu giữ nguyên' : 'Dán API key của ' + chosen.label;
    byId('aiKeyNote').textContent = custom
        ? 'Địa chỉ máy chủ AI là đường dẫn hệ thống gửi bài viết tới để AI chấm, ghi trong tài liệu của nhà cung cấp. Ví dụ Groq: https://api.groq.com/openai/v1 · DeepSeek: https://api.deepseek.com/v1 · OpenRouter: https://openrouter.ai/api/v1.'
        : chosen.keySource === 'server' ? 'Đang dùng API key trong cấu hình server. Nhập key mới nếu muốn thay.' : '';
}

function renderPlans(plans) {
    state.plans = plans;
    const rows = byId('planRows'); rows.replaceChildren();
    if (!plans.length) {
        const row = text('tr'), cell = text('td', 'Chưa có gói hội viên. Chọn "Thêm gói" để tạo gói đầu tiên.', 'table-empty'); cell.colSpan = 5; row.append(cell); rows.append(row); return;
    }
    plans.forEach(plan => {
        const row = text('tr'), actions = text('td'), box = text('div', '', 'portal-actions');
        box.append(actionButton('Sửa', () => openPlan(plan)), actionButton(plan.active ? 'Ngừng bán' : 'Mở bán', () => togglePlan(plan), plan.active ? 'danger' : 'primary'));
        actions.append(box);
        const name = text('td'); name.append(text('strong', plan.planName));
        if (plan.benefits.length) name.append(text('p', plan.benefits.length + ' quyền lợi', 'portal-muted'));
        const status = text('td'); status.append(badge(plan.active ? 'Đang bán' : 'Ngừng bán'));
        row.append(name, text('td', plan.durationMonths + ' tháng'), text('td', money(plan.price)), status, actions);
        rows.append(row);
    });
}

async function loadPlans() {
    try { renderPlans(await plansApi.getPlans()); }
    catch (error) {
        if (error.status === 401) throw error;
        const rows = byId('planRows'), row = text('tr'), cell = text('td', error.message || 'Chưa tải được danh sách gói.', 'table-empty');
        cell.colSpan = 5; row.append(cell); rows.replaceChildren(row);
    }
}

async function load() {
    const [data, sender, ai, history] = await Promise.all([api(PATH), api(SENDER_PATH), api(AI_PATH), api(base + '/settings/history?page=' + state.historyPage)]);
    state.version = data.version; state.current = data.maxMegabytes;
    byId('attachmentLimit').max = data.ceilingMegabytes;
    byId('attachmentCeiling').textContent = 'Từ 1 đến ' + data.ceilingMegabytes + ' MB.';
    if (!state.dirty) byId('attachmentLimit').value = data.maxMegabytes;
    byId('attachmentCurrent').textContent = 'Hiện hành: ' + data.maxMegabytes + ' MB mỗi tệp.';

    state.senderVersion = sender.version; state.sender = sender.senderEmail;
    state.mail = sender;
    if (!state.senderDirty) {
        byId('senderEmail').value = sender.senderEmail || ''; byId('senderPassword').value = '';
        byId('smtpHost').value = sender.smtpHost || ''; byId('smtpPort').value = sender.smtpPort || 587;
        // Máy chủ đã lưu không phải loại tự đoán được thì hiện ô để Admin thấy.
        showSmtp(Boolean(sender.smtpHost) && !['smtp.gmail.com', 'smtp.office365.com', 'smtp.mail.yahoo.com'].includes(sender.smtpHost));
    }
    syncMailPlaceholder();
    byId('senderCurrent').textContent = sender.ownAccount
        ? 'Đang gửi bằng: ' + sender.senderEmail
        : 'Đang gửi bằng tài khoản thư của server' + (sender.senderEmail ? ' (' + sender.senderEmail + ')' : '') + '. Nhập email và mật khẩu ứng dụng để gửi bằng email của bạn.';
    renderAi(ai);
    await loadPlans();

    byId('configHistory').replaceChildren();
    if (!history.items.length) byId('configHistory').append(emptyState('Chưa có thay đổi cấu hình', 'Lịch sử sẽ hiện ở đây sau lần lưu đầu tiên.'));
    const unit = row => row.unit ? ' ' + row.unit : '';
    history.items.forEach(row => byId('configHistory').append(text('p', row.settingLabel + '\n' + row.actorDisplayName + ' · ' + date(row.occurredAt) + '\n' + row.before + unit(row) + ' → ' + row.after + unit(row) + '\nLý do: ' + row.reason)));
    paging('configHistory', history, state.historyPage);
    notice('');
}

async function save() {
    const maxMegabytes = Number(byId('attachmentLimit').value);
    if (!confirm('Đổi giới hạn tệp đính kèm hỗ trợ từ ' + state.current + ' MB thành ' + maxMegabytes + ' MB? Chỉ áp dụng cho lần tải mới, tệp đã lưu giữ nguyên.')) return;
    await api(PATH, 'PUT', {maxMegabytes, expectedVersion: state.version, reason: byId('configReason').value});
    state.dirty = false; byId('configReason').value = '';
    await load(); notice('Đã lưu giới hạn tệp đính kèm.');
}

/** Mật khẩu cũ không bao giờ hiện lại; chỉ báo đã có (4 ký tự cuối). Để trống = giữ mật khẩu cũ. */
function syncMailPlaceholder() {
    const same = byId('senderEmail').value.trim().toLowerCase() === (state.mail?.senderEmail || '').toLowerCase();
    byId('senderPassword').placeholder = same && state.mail?.ownAccount ? 'Đã có ' + state.mail.maskedPassword + ', để trống nếu giữ nguyên' : 'Gmail: mật khẩu ứng dụng 16 ký tự';
}

function mailPayload() {
    return {senderEmail: byId('senderEmail').value.trim(), appPassword: byId('senderPassword').value.trim(), smtpHost: byId('smtpHost').value.trim(), smtpPort: Number(byId('smtpPort').value) || 587};
}

/** Gọi API có thể chờ máy chủ bên ngoài (SMTP, AI) quá 4,5 giây nên không dùng fetchAPI. */
async function slowPost(path, payload) {
    const csrf = await fetchAPI(base + '/csrf');
    const response = await fetch(API_CONFIG.BASE_URL + path, {method: 'POST', credentials: 'include', signal: AbortSignal.timeout(45000),
        headers: {'Content-Type': 'application/json', [csrf.headerName]: csrf.token}, body: JSON.stringify(payload)});
    const result = await response.json();
    if (!response.ok || result.success === false) throw new Error(result.fieldErrors?.length ? result.fieldErrors.map(f => f.message).join(' ') : result.message || 'Chưa thực hiện được.');
    return result.data;
}

function showResult(box, data) {
    const seconds = (data.latencyMs / 1000).toLocaleString('vi-VN', {maximumFractionDigits: 1});
    box.dataset.tone = data.ok ? 'success' : 'danger';
    box.replaceChildren(text('strong', (data.ok ? '✓ ' : '✗ ') + data.message), text('span', ' (' + seconds + ' giây)'));
    if (data.reply) box.append(text('p', 'AI trả lời: ' + data.reply));
}

async function testMail() {
    const box = byId('mailTestResult'), button = byId('testMail');
    box.hidden = false; box.dataset.tone = ''; box.textContent = 'Đang gửi thư thử tới email của bạn…'; button.disabled = true;
    try { showResult(box, await slowPost(SENDER_PATH + '/test', mailPayload())); }
    catch (error) { box.dataset.tone = 'danger'; box.textContent = '✗ ' + (error.name === 'TimeoutError' ? 'Máy chủ thư không phản hồi sau 45 giây.' : error.message); }
    finally { button.disabled = false; }
}

async function saveSender() {
    const payload = mailPayload();
    if (!confirm('Lưu email gửi thư ' + payload.senderEmail + (payload.appPassword ? ' (kèm mật khẩu ứng dụng mới)' : '') + '? Các thư gửi sau sẽ dùng email này.')) return;
    await api(SENDER_PATH, 'PUT', {...payload, expectedVersion: state.senderVersion, reason: byId('senderReason').value});
    state.senderDirty = false; byId('senderReason').value = ''; byId('senderPassword').value = '';
    await load(); notice('Đã lưu email người gửi.');
}

/** Gọi AI thật có thể mất tới vài chục giây nên không dùng fetchAPI (giới hạn 4,5 giây). */
async function testAi() {
    const box = byId('aiTestResult'), button = byId('testAi');
    const provider = byId('aiProvider').value;
    const payload = {provider, model: byId('aiModel').value.trim(), apiKey: byId('aiApiKey').value.trim(), baseUrl: provider === 'CUSTOM' ? byId('aiBaseUrl').value.trim() : ''};
    box.hidden = false; box.dataset.tone = ''; box.textContent = 'Đang gửi câu hỏi thử tới AI…'; button.disabled = true;
    try {
        showResult(box, await slowPost(AI_PATH + '/test', payload));
    } catch (error) {
        box.dataset.tone = 'danger'; box.textContent = '✗ ' + (error.name === 'TimeoutError' ? 'AI không phản hồi sau 45 giây.' : error.message);
    } finally { button.disabled = false; }
}

async function saveAi() {
    const provider = byId('aiProvider').value, model = byId('aiModel').value.trim(), apiKey = byId('aiApiKey').value.trim();
    const label = state.ai.providers.find(item => item.code === provider)?.label || provider;
    if (!confirm('Lưu cấu hình AI: ' + label + ' · ' + model + (apiKey ? ' (kèm API key mới)' : '') + '? Áp dụng ngay cho lần chấm bài tiếp theo.')) return;
    await api(AI_PATH, 'PUT', {provider, model, apiKey, baseUrl: provider === 'CUSTOM' ? byId('aiBaseUrl').value.trim() : '', expectedVersion: state.ai.version, reason: byId('aiReason').value});
    state.aiDirty = false; byId('aiReason').value = ''; byId('aiApiKey').value = '';
    await load(); notice('Đã lưu cấu hình AI.');
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
function planError(field, message) { const node = document.querySelector('[data-error="' + field + '"]'); if (node) node.textContent = message; }

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
    const payload = {planName: name, price, benefits};
    state.savingPlan = true; byId('savePlan').disabled = true; byId('planNotice').textContent = '';
    try {
        if (state.plan) await plansApi.updatePlan(state.plan.planId, payload);
        else await plansApi.createPlan({...payload, durationMonths: months});
        byId('planDialog').close();
        await page.run(async () => { await loadPlans(); notice(state.plan ? 'Đã lưu thay đổi gói hội viên.' : 'Đã tạo gói hội viên.'); });
    } catch (error) {
        byId('planNotice').textContent = error.message || 'Chưa lưu được gói. Vui lòng thử lại.';
    } finally { state.savingPlan = false; byId('savePlan').disabled = false; }
}

async function togglePlan(plan) {
    if (plan.active && !confirm('Ngừng bán "' + plan.planName + '"? Gia sư đã mua vẫn dùng được đến hết hạn.')) return;
    await page.run(async () => {
        await plansApi.setActive(plan.planId, !plan.active);
        await loadPlans(); notice(plan.active ? 'Đã ngừng bán gói ' + plan.planName + '.' : 'Đã mở bán gói ' + plan.planName + '.');
    });
}

const page = createOperationsPage(load);
page.bind('configForm', 'submit', save);
page.bind('senderForm', 'submit', saveSender);
page.bind('aiForm', 'submit', saveAi);
byId('testAi').addEventListener('click', testAi);
byId('configForm').addEventListener('input', () => { state.dirty = true; });
byId('senderForm').addEventListener('input', () => { state.senderDirty = true; });
byId('testMail').addEventListener('click', testMail);
// Gõ email Gmail/Outlook thì tự điền máy chủ SMTP tương ứng.
byId('senderEmail').addEventListener('change', () => {
    const domain = byId('senderEmail').value.split('@')[1]?.toLowerCase() || '';
    const host = {'gmail.com': 'smtp.gmail.com', 'googlemail.com': 'smtp.gmail.com', 'outlook.com': 'smtp.office365.com', 'hotmail.com': 'smtp.office365.com', 'live.com': 'smtp.office365.com', 'yahoo.com': 'smtp.mail.yahoo.com'}[domain];
    if (host) { byId('smtpHost').value = host; byId('smtpPort').value = 587; }
    else if (domain) { byId('smtpHost').value = ''; showSmtp(true); }
    syncMailPlaceholder();
});
/** Máy chủ và cổng tự điền với Gmail/Outlook/Yahoo nên ẩn đi; chỉ mở khi email thuộc dịch vụ khác. */
function showSmtp(show) {
    document.querySelectorAll('[data-smtp-field]').forEach(field => { field.hidden = !show; });
    byId('toggleSmtp').hidden = show;
}
byId('toggleSmtp').addEventListener('click', () => showSmtp(true));

byId('aiForm').addEventListener('input', () => { state.aiDirty = true; });
byId('aiProvider').addEventListener('change', () => {
    const chosen = state.ai?.providers.find(item => item.code === byId('aiProvider').value);
    if (chosen && chosen.code !== state.ai.provider) byId('aiModel').value = chosen.defaultModel;
    byId('aiApiKey').value = '';
    syncAiNote();
});
byId('createPlan').addEventListener('click', () => openPlan());
byId('planForm').addEventListener('submit', savePlan);
document.querySelectorAll('#planMonthChips button').forEach(chip => chip.addEventListener('click', () => setMonths(chip.dataset.months)));
byId('planMonths').addEventListener('input', updatePerMonth);
document.querySelectorAll('[data-price]').forEach(chip => chip.addEventListener('click', () => setPrice(Number(chip.dataset.price))));
// Đang gõ thì để số thường (xóa, sửa giữa chừng không bị nhảy con trỏ); rời ô mới thêm dấu chấm, ví dụ 229900 → 229.900.
byId('planPrice').addEventListener('input', () => { const field = byId('planPrice'), clean = field.value.replace(/[^\d.]/g, ''); if (clean !== field.value) field.value = clean; updatePerMonth(); });
byId('planPrice').addEventListener('focus', () => { const value = digits(byId('planPrice').value); byId('planPrice').value = value ? String(value) : ''; });
byId('planPrice').addEventListener('blur', () => { const value = digits(byId('planPrice').value); byId('planPrice').value = value ? planMoney(value) : ''; });
byId('addBenefit').addEventListener('click', () => { addBenefitRow(); byId('planBenefitList').lastElementChild.querySelector('input').focus(); });
['cancelPlan', 'closePlan'].forEach(id => byId(id).addEventListener('click', () => { if (!state.savingPlan) byId('planDialog').close(); }));
page.bind('configHistoryPrev', 'click', async () => { state.historyPage = Math.max(0, state.historyPage - 1); await load(); });
page.bind('configHistoryNext', 'click', async () => { state.historyPage++; await load(); });
page.start();
