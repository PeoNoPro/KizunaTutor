/**
 * FE-12: trang Thông báo, Tùy chọn nhận thông báo (và khung dùng chung cho trang Gửi thông báo) cho sáu vai trò.
 * Mỗi vai trò được đặt vào đúng khung giao diện của mình (học viên, gia sư, cán bộ học vụ, khu quản trị/CS).
 */
import {fetchAPI} from '../api.js';
import {showToast} from '../main.js';
import {mountAdminShell} from '../../components/admin-shell/admin-shell.js';
import {mountLearnerFrame} from './learner-frame.js';
import {mountTutorFrame} from './tutor-frame.js';
import {mountAcademicLayout} from '../../components/academic-layout/academic-layout.js';

const BASE = '/v1/notifications';
export const notificationApi = (path = '', options = {}) => fetchAPI(BASE + path, {csrfPath: BASE + '/csrf', ...options});
const $ = id => document.getElementById(id);
export const TYPE_ICONS = {SYSTEM: 'campaign', TRIAL: 'event_available', ASSIGNMENT: 'assignment', PAYMENT: 'payments', TICKET: 'support_agent'};

export function element(tag, text, className) {
  const node = document.createElement(tag);
  if (text !== undefined && text !== null) node.textContent = text;
  if (className) node.className = className;
  return node;
}

export function icon(name, className = '') {
  const node = element('span', name, 'material-symbols-outlined' + (className ? ' ' + className : ''));
  node.setAttribute('aria-hidden', 'true');
  return node;
}

export function formatDate(value) {
  if (!value) return '';
  return new Date(value).toLocaleString('vi-VN', {timeZone: 'Asia/Ho_Chi_Minh', day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit'});
}

/** "Vừa xong", "5 phút trước"... trong vòng 7 ngày; xa hơn thì hiện ngày giờ đầy đủ. */
export function relativeDate(value) {
  if (!value) return '';
  const minutes = Math.round((Date.now() - new Date(value).getTime()) / 60000);
  if (minutes < 1) return 'Vừa xong';
  if (minutes < 60) return `${minutes} phút trước`;
  if (minutes < 60 * 24) return `${Math.floor(minutes / 60)} giờ trước`;
  if (minutes < 60 * 24 * 7) return `${Math.floor(minutes / 1440)} ngày trước`;
  return formatDate(value);
}

function loadCss(href) {
  if (document.querySelector(`link[href="${href}"]`)) return Promise.resolve();
  return new Promise((resolve, reject) => {
    const link = document.createElement('link'); link.rel = 'stylesheet'; link.href = href;
    link.onload = resolve; link.onerror = () => reject(new Error('Chưa tải được giao diện. Vui lòng thử lại.'));
    document.head.append(link);
  });
}

function loadScript(src) {
  if (document.querySelector(`script[src="${src}"]`)) return Promise.resolve();
  return new Promise((resolve, reject) => {
    const script = document.createElement('script'); script.src = src;
    script.onload = resolve; script.onerror = () => reject(new Error('Chưa tải được giao diện. Vui lòng thử lại.'));
    document.head.append(script);
  });
}

const ACCOUNT_CACHE = 'kizuna-notification-account';

/**
 * Vai trò của người đang đăng nhập. Lần sau trong cùng phiên dùng bản đã nhớ để dựng khung ngay,
 * đồng thời vẫn hỏi lại máy chủ; nếu tài khoản đã đổi (đăng nhập người khác) thì tải lại trang.
 */
async function currentAccount() {
  const fresh = notificationApi('/me').then(account => {
    try { sessionStorage.setItem(ACCOUNT_CACHE, JSON.stringify({viewRole: account.viewRole, displayName: account.displayName})); } catch { /* Không nhớ được thì lần sau hỏi lại. */ }
    return account;
  });
  let cached = null;
  try { cached = JSON.parse(sessionStorage.getItem(ACCOUNT_CACHE) || 'null'); } catch { /* Bỏ qua bản nhớ hỏng. */ }
  if (!cached?.viewRole) return fresh;
  fresh.then(account => { if (account.viewRole !== cached.viewRole || account.displayName !== cached.displayName) location.reload(); })
    .catch(error => { try { sessionStorage.removeItem(ACCOUNT_CACHE); } catch { /* Bỏ qua. */ } if (error.status === 401) location.assign('/login'); });
  return cached;
}

/** Đặt trang vào khung của vai trò đang đăng nhập; Admin và Quản lý thấy thêm nút "Gửi thông báo". */
export async function mountFrame() {
  const account = await currentAccount();
  const main = document.querySelector('main');
  if (account.viewRole === 'learner') mountLearnerFrame('notifications', account.displayName);
  else if (account.viewRole === 'tutor') await mountTutorFrame(account.displayName, 'notifications');
  else if (account.viewRole === 'academic') {
    // Khung học vụ dùng Tailwind dựng lúc chạy giống các trang học vụ khác (CDN + theme chung với gia sư).
    await loadScript('https://cdn.tailwindcss.com');
    await loadScript('/components/tutor-layout/tailwind-theme.js');
    await loadCss('/styles/tutor-theme.css');
    const sidebar = element('div'); sidebar.id = 'academicSidebar';
    const column = element('div', null, 'lg:pl-72 flex flex-col min-h-screen');
    const topbar = element('div'); topbar.id = 'academicTopbar';
    main.replaceWith(column); column.append(topbar, main); column.before(sidebar);
    mountAcademicLayout('notifications', 'Thông báo');
  } else {
    await Promise.all(['/styles/admin.css', '/styles/operations.css', '/styles/admin-portal.css'].map(loadCss));
    document.body.classList.add('admin-dashboard', 'operations-page');
    if (account.viewRole === 'cs') document.body.dataset.workspace = 'cs';
    main.classList.add('dashboard-main');
    mountAdminShell();
  }
  const broadcast = $('broadcastButton');
  if (broadcast) broadcast.hidden = !['admin', 'manager'].includes(account.viewRole);
  document.body.dataset.notificationRole = account.viewRole;
  window.kizunaFrameReady?.(account.viewRole);
  // Ô ngày hiện theo ngày/tháng/năm cho mọi vai trò (học viên, gia sư, học vụ không dùng khung quản trị).
  import('/js/pages/date-input-vi.js').then(module => module.watchDateInputs()).catch(() => {});
  return account;
}

export function stateBox(symbol, title, message, retry) {
  const box = element('div', null, 'nt-state' + (retry ? ' nt-state-error' : ''));
  if (retry) box.setAttribute('role', 'alert');
  box.append(icon(symbol), element('strong', title));
  if (message) box.append(element('p', message));
  if (retry) { const button = element('button', 'Thử tải lại', 'nt-button'); button.type = 'button'; button.onclick = retry; box.append(button); }
  return box;
}

function showError(target, error, retry) {
  if (error?.status === 401) { location.assign('/login'); return; }
  target.replaceChildren(stateBox('error', 'Chưa tải được dữ liệu', error?.message || 'Chưa kết nối được hệ thống. Vui lòng thử lại sau.', retry));
}

/** Trang danh sách: lọc tất cả/chưa đọc và nhóm; mở một thông báo thì hiện hộp chi tiết và tự đánh dấu đã đọc. */
export async function startNotificationsPage() {
  const state = {read: 'all', type: '', from: '', to: '', page: 0, unread: 0, items: new Map()};
  const list = $('notificationList'), dialog = $('notificationDetail');
  // Danh sách tải song song với việc dựng khung, không đợi nhau.
  const firstPage = notificationApi('?read=all&page=0&size=20');
  firstPage.catch(() => {});
  try { await mountFrame(); }
  catch (error) { window.kizunaFrameReady?.(); document.body.dataset.notificationRole = 'error'; showError(list, error, () => location.reload()); return; }

  const setUnread = count => {
    state.unread = count;
    const line = $('notificationUnread');
    line.replaceChildren();
    if (count) line.append(element('strong', `${count} thông báo`), document.createTextNode(' chưa đọc'));
    else line.textContent = 'Bạn đã đọc hết thông báo';
    document.dispatchEvent(new CustomEvent('kizuna:notifications-unread', {detail: count}));
  };

  function renderItem(item) {
    const row = element('li', null, 'nt-item' + (item.isRead ? '' : ' is-unread')); row.dataset.id = item.id;
    const open = element('button', null, 'nt-item-main'); open.type = 'button';
    open.setAttribute('aria-label', (item.isRead ? '' : 'Chưa đọc: ') + item.title);
    const typeIcon = icon(TYPE_ICONS[item.type] || 'notifications', 'nt-type-icon'); typeIcon.dataset.type = item.type;
    const body = element('span', null, 'nt-item-body');
    const top = element('span', null, 'nt-item-top');
    const time = element('time', relativeDate(item.receivedAt)); time.title = formatDate(item.receivedAt);
    top.append(element('span', item.typeLabel, 'nt-chip'), time);
    body.append(top, element('span', item.title, 'nt-item-title'), element('span', item.preview, 'nt-item-preview'));
    open.append(typeIcon, body);
    if (!item.isRead) { const dot = element('span', null, 'nt-dot'); dot.setAttribute('aria-hidden', 'true'); open.append(dot); }
    open.onclick = () => openDetail(item.id);
    const actions = element('div', null, 'nt-item-actions');
    if (!item.isRead) {
      const mark = element('button', null, 'nt-icon-button'); mark.type = 'button'; mark.title = 'Đánh dấu đã đọc';
      mark.setAttribute('aria-label', 'Đánh dấu đã đọc: ' + item.title); mark.append(icon('done_all'));
      mark.onclick = () => markRead(item.id, mark);
      actions.append(mark);
    }
    const remove = element('button', null, 'nt-icon-button danger'); remove.type = 'button'; remove.title = 'Xóa';
    remove.setAttribute('aria-label', 'Xóa thông báo: ' + item.title); remove.append(icon('delete'));
    remove.onclick = () => removeItem(item.id, remove);
    actions.append(remove);
    row.append(open, actions);
    return row;
  }

  async function loadList(prefetched) {
    list.setAttribute('aria-busy', 'true');
    try {
      const params = new URLSearchParams({read: state.read, page: state.page, size: 20});
      if (state.type) params.set('type', state.type);
      if (state.from) params.set('fromDate', state.from);
      if (state.to) params.set('toDate', state.to);
      const data = await (prefetched || notificationApi('?' + params));
      setUnread(data.unreadCount);
      if (!data.items.length) {
        const filtered = state.type || state.from || state.to;
        list.replaceChildren(stateBox('notifications_off', state.read === 'unread' ? 'Không có thông báo chưa đọc' : filtered ? 'Không có thông báo phù hợp' : 'Chưa có thông báo',
          filtered ? 'Thử đổi nhóm hoặc khoảng ngày để xem thêm.' : 'Thông báo về học thử, bài tập, thanh toán, hỗ trợ và hệ thống sẽ hiện ở đây.'));
      } else {
        data.items.forEach(item => state.items.set(item.id, item));
        const ul = element('ul', null, 'nt-list');
        ul.append(...data.items.map(renderItem));
        list.replaceChildren(ul);
      }
      $('notificationPage').textContent = data.totalPages > 1 ? `Trang ${data.page + 1} / ${data.totalPages}` : '';
      $('notificationPrev').hidden = $('notificationNext').hidden = data.totalPages <= 1;
      $('notificationPrev').disabled = data.page === 0;
      $('notificationNext').disabled = data.page + 1 >= data.totalPages;
    } catch (error) {
      showError(list, error, () => loadList());
    } finally { list.setAttribute('aria-busy', 'false'); }
  }

  /** Vẽ hộp chi tiết; `loading` = đang lấy nội dung đầy đủ (danh sách chỉ có bản rút gọn). */
  function renderDetail(id, item, loading) {
    // Phần đầu tô màu theo nhóm thông báo (Hệ thống, Hỗ trợ, Thanh toán...), giống nhau ở cả 6 vai trò.
    dialog.dataset.type = item.type || 'SYSTEM';
    const hero = element('div', null, 'nt-hero');
    const close = element('button', null, 'nt-icon-button nt-hero-close'); close.type = 'button'; close.setAttribute('aria-label', 'Đóng'); close.append(icon('close'));
    close.onclick = () => dialog.close();
    const typeIcon = icon(TYPE_ICONS[item.type] || 'notifications', 'nt-hero-icon');
    const chip = element('span', null, 'nt-hero-chip'); chip.append(icon(TYPE_ICONS[item.type] || 'notifications'), document.createTextNode(item.typeLabel || 'Thông báo'));
    const title = element('h2', item.title); title.id = 'notificationDetailTitle'; title.tabIndex = -1;
    const meta = element('div', null, 'nt-hero-meta');
    const time = element('time', `${relativeDate(item.receivedAt)} · ${formatDate(item.receivedAt)}`); time.dateTime = item.receivedAt || '';
    meta.append(icon('schedule'), time);
    hero.append(close, typeIcon, chip, title, meta);

    const body = element('div', item.content ?? item.preview, 'nt-dialog-body');
    if (loading) body.setAttribute('aria-busy', 'true');
    const parts = [hero, body];
    // Thông báo tự động có đường dẫn tới yêu cầu/sự cố/lịch học liên quan (chỉ nhận đường dẫn nội bộ).
    if (item.actionPath && /^\/[^/]/.test(item.actionPath)) {
      const go = element('a', null, 'nt-related'); go.href = item.actionPath;
      const text = element('span', null, 'nt-related-text');
      text.append(element('strong', 'Mở trang liên quan'), element('span', 'Xem chi tiết và xử lý tiếp tại trang gốc'));
      go.append(icon('open_in_new', 'nt-related-icon'), text, icon('chevron_right', 'nt-related-arrow'));
      parts.push(go);
    }
    const status = element('p', '', 'nt-dialog-status'); status.setAttribute('role', 'status');
    const foot = element('div', null, 'nt-dialog-foot');
    const remove = element('button', null, 'nt-button ghost-danger'); remove.type = 'button'; remove.append(icon('delete'), document.createTextNode('Xóa thông báo'));
    remove.onclick = async () => { if (await removeItem(id, remove)) dialog.close(); };
    const done = element('button', 'Đã xem', 'nt-button primary'); done.type = 'button'; done.onclick = () => dialog.close();
    foot.append(remove, done);
    dialog.replaceChildren(...parts, status, foot);
    return {title, body, status};
  }

  /**
   * Mở hộp chi tiết ngay bằng dữ liệu đã có trong danh sách, rồi lấy nội dung đầy đủ; tải xong mới đánh dấu đã đọc
   * (lưu lỗi thì báo, không giả vờ đã giảm số).
   */
  async function openDetail(id) {
    const cached = state.items.get(id);
    let view = null;
    if (cached) view = renderDetail(id, cached, true);
    else dialog.replaceChildren(stateBox('hourglass_top', 'Đang tải thông báo…'));
    if (!dialog.open) dialog.showModal();
    view?.title.focus();
    let item;
    try { item = await notificationApi('/' + id); }
    catch (error) {
      if (error?.status === 401) { location.assign('/login'); return; }
      if (view) { view.status.textContent = 'Chưa tải được nội dung đầy đủ: ' + (error?.message || 'vui lòng thử lại.'); view.body.removeAttribute('aria-busy'); return; }
      const close = element('button', 'Đóng', 'nt-button'); close.type = 'button'; close.onclick = () => dialog.close();
      const foot = element('div', null, 'nt-dialog-foot'); foot.append(close);
      dialog.replaceChildren(stateBox('error', 'Không mở được thông báo', error?.message || 'Vui lòng thử lại.'), foot);
      return;
    }
    if (!dialog.open) return;
    if (view) { view.body.textContent = item.content; view.body.removeAttribute('aria-busy'); }
    else { view = renderDetail(id, item, false); view.title.focus(); }
    if (!item.isRead) {
      try {
        const result = await notificationApi(`/${id}/read`, {method: 'POST'});
        setUnread(result.unreadCount);
        // Cập nhật ngay dòng trong danh sách, không cần tải lại cả danh sách.
        state.items.set(id, {...(state.items.get(id) || item), isRead: true});
        const row = [...list.querySelectorAll('.nt-item')].find(node => node.dataset.id === id);
        row?.replaceWith(renderItem(state.items.get(id)));
      } catch (error) { view.status.textContent = 'Chưa lưu được trạng thái đã đọc: ' + error.message; }
    }
  }

  async function markRead(id, button) {
    button.disabled = true;
    try { const result = await notificationApi(`/${id}/read`, {method: 'POST'}); setUnread(result.unreadCount); await loadList(); }
    catch (error) { button.disabled = false; showToast(error.message, 'error'); }
  }

  async function removeItem(id, button) {
    if (!confirm('Xóa thông báo này khỏi danh sách của bạn? Không khôi phục được.')) return false;
    button.disabled = true;
    try {
      const result = await notificationApi('/' + id, {method: 'DELETE'});
      setUnread(result.unreadCount);
      showToast('Đã xóa thông báo.', 'success');
      await loadList();
      return true;
    } catch (error) { button.disabled = false; showToast(error.message, 'error'); return false; }
  }

  // Bấm ra ngoài hộp chi tiết thì đóng.
  dialog.addEventListener('click', event => { if (event.target === dialog) dialog.close(); });
  dialog.addEventListener('close', () => { if (new URLSearchParams(location.search).has('id')) history.replaceState(null, '', location.pathname); });
  document.querySelectorAll('[data-read-filter]').forEach(tab => tab.addEventListener('click', () => {
    state.read = tab.dataset.readFilter; state.page = 0;
    document.querySelectorAll('[data-read-filter]').forEach(other => other.setAttribute('aria-pressed', String(other === tab)));
    loadList();
  }));
  $('notificationType').addEventListener('change', event => { state.type = event.target.value; state.page = 0; loadList(); });
  // Lọc theo khoảng ngày nhận; ngày kết thúc không được trước ngày bắt đầu.
  const syncDates = () => {
    state.from = $('notificationFrom').value; state.to = $('notificationTo').value;
    $('notificationTo').min = state.from; $('notificationFrom').max = state.to;
    $('notificationClearDates').hidden = !state.from && !state.to;
    state.page = 0; loadList();
  };
  $('notificationFrom').addEventListener('change', syncDates);
  $('notificationTo').addEventListener('change', syncDates);
  $('notificationClearDates').addEventListener('click', () => { $('notificationFrom').value = ''; $('notificationTo').value = ''; syncDates(); });
  $('notificationPrev').onclick = () => { state.page = Math.max(0, state.page - 1); loadList(); };
  $('notificationNext').onclick = () => { state.page++; loadList(); };
  await loadList(firstPage);
  const requested = new URLSearchParams(location.search).get('id');
  if (requested && /^[0-9a-f-]{36}$/i.test(requested)) openDetail(requested);
}

/** Trang tùy chọn: năm nhóm × hai kênh, lưu tường minh; phiên bản cũ thì báo tải lại. */
export async function startPreferencesPage() {
  const form = $('preferenceForm'), status = $('preferenceStatus'), rows = $('preferenceRows');
  let version = null, dirty = false;
  const firstLoad = notificationApi('/preferences');
  firstLoad.catch(() => {});
  try { await mountFrame(); }
  catch (error) { window.kizunaFrameReady?.(); document.body.dataset.notificationRole = 'error'; showError(rows, error, () => location.reload()); return; }

  async function load(prefetched) {
    form.setAttribute('aria-busy', 'true');
    try {
      const data = await (prefetched || notificationApi('/preferences'));
      version = data.version; dirty = false;
      rows.replaceChildren(...data.groups.map(group => {
        const tr = element('tr');
        tr.dataset.type = group.type;
        const name = element('th'); name.scope = 'row';
        const typeIcon = icon(TYPE_ICONS[group.type] || 'notifications', 'nt-type-icon'); typeIcon.dataset.type = group.type;
        typeIcon.style.cssText = 'width:32px;height:32px;border-radius:10px;font-size:18px;margin-right:10px';
        name.append(typeIcon, document.createTextNode(group.typeLabel));
        tr.append(name);
        ['inAppEnabled', 'emailEnabled'].forEach(key => {
          const td = element('td'), box = element('input');
          box.type = 'checkbox'; box.name = key; box.checked = group[key];
          box.setAttribute('aria-label', `${key === 'inAppEnabled' ? 'Trong ứng dụng' : 'Email'} – ${group.typeLabel}`);
          td.append(box); tr.append(td);
        });
        return tr;
      }));
      $('savePreferences').disabled = false;
    } catch (error) {
      if (error?.status === 401) { location.assign('/login'); return; }
      const cell = element('td', error?.message || 'Chưa tải được tùy chọn.', 'nt-error'); cell.colSpan = 3;
      const tr = element('tr'); tr.append(cell); rows.replaceChildren(tr);
    } finally { form.setAttribute('aria-busy', 'false'); }
  }

  form.addEventListener('change', () => { dirty = true; status.textContent = 'Có thay đổi chưa lưu.'; status.className = 'nt-detail-status'; });
  form.addEventListener('submit', async event => {
    event.preventDefault();
    if (version === null) return;
    const groups = [...rows.querySelectorAll('tr')].map(tr => ({type: tr.dataset.type, inAppEnabled: tr.querySelector('[name=inAppEnabled]').checked, emailEnabled: tr.querySelector('[name=emailEnabled]').checked}));
    const button = $('savePreferences'); button.disabled = true; button.textContent = 'Đang lưu…';
    try {
      const data = await notificationApi('/preferences', {method: 'PUT', body: {expectedVersion: version, groups}});
      version = data.version; dirty = false;
      status.textContent = '✓ Đã lưu tùy chọn nhận thông báo.'; status.className = 'nt-detail-status nt-success';
      showToast('Đã lưu tùy chọn nhận thông báo.', 'success');
    } catch (error) {
      if (error.status === 401) { location.assign('/login'); return; }
      status.textContent = error.status === 409 ? error.message + ' Bấm "Tải lại" để xem giá trị mới.' : 'Chưa lưu: ' + error.message;
      status.className = 'nt-detail-status nt-error';
      $('reloadPreferences').hidden = error.status !== 409;
    } finally { button.disabled = false; button.textContent = 'Lưu tùy chọn'; }
  });
  $('reloadPreferences').onclick = async () => { $('reloadPreferences').hidden = true; status.textContent = ''; await load(); };
  window.addEventListener('beforeunload', event => { if (dirty) event.preventDefault(); });
  await load(firstLoad);
}
