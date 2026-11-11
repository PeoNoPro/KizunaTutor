/**
 * FE-12: chuông thông báo dùng chung cho mọi khung giao diện (khu quản trị, CS, học vụ, gia sư, học viên, header công khai).
 * Nút có thuộc tính data-notification-bell: hiện số chưa đọc, bấm thì mở khung xem nhanh vài thông báo mới nhất.
 * Link tới /pages/notifications/ trên sidebar cũng được gắn số chưa đọc.
 */
import {fetchAPI} from '../api.js';

const BASE = '/v1/notifications';
const STYLE = `
.kz-bell{position:relative}
.learner-frame #learnerSidebar [data-notification-bell],#learnerSidebar [data-notification-bell]{border:0;padding:6px;background:transparent}
.kz-bell-count{position:absolute;top:-4px;right:-6px;min-width:18px;height:18px;padding:0 5px;border-radius:999px;background:#b42318;color:#fff;font:700 11px/18px 'Plus Jakarta Sans',system-ui,sans-serif;text-align:center;pointer-events:none;box-shadow:0 0 0 2px #fff}
.kz-bell-count[hidden],.kz-link-count[hidden]{display:none!important}
.kz-link-count{margin-left:auto;min-width:20px;height:20px;padding:0 6px;border-radius:999px;background:#b42318;color:#fff;font:700 11px/20px 'Plus Jakarta Sans',system-ui,sans-serif;text-align:center}
.kz-bell-panel{position:fixed;z-index:1000;width:min(380px,calc(100vw - 24px));max-height:min(520px,calc(100vh - 96px));display:flex;flex-direction:column;background:#fff;border:1px solid #e5e1d8;border-radius:14px;box-shadow:0 16px 40px rgba(15,37,69,.16);font-family:'Plus Jakarta Sans',system-ui,sans-serif;color:#0f2545;overflow:hidden;text-align:left}
.kz-bell-panel[hidden]{display:none}
.kz-bell-head{display:flex;align-items:center;justify-content:space-between;gap:8px;padding:14px 16px;border-bottom:1px solid #eee9df}
.kz-bell-head strong{font-size:16px}.kz-bell-head span{font-size:12px;color:#6b7280}
.kz-bell-head a{font-size:13px;font-weight:600;color:#006194;text-decoration:none}
.kz-bell-list{list-style:none;margin:0;padding:0;overflow-y:auto}
.kz-bell-item a{display:flex;flex-direction:column;gap:3px;padding:12px 16px;border-bottom:1px solid #f1ede4;color:inherit;text-decoration:none}
.kz-bell-item a:hover,.kz-bell-item a:focus-visible{background:#f6f9fb;outline:none}
.kz-bell-item.is-unread a{background:#eef5fa}
.kz-bell-item.is-unread a:hover{background:#e3eef6}
.kz-bell-meta{display:flex;align-items:center;gap:8px;font-size:11px;color:#6b7280}
.kz-bell-chip{padding:1px 8px;border-radius:999px;background:#faf8f2;border:1px solid #e5e1d8;font-weight:600;color:#3f4850}
.kz-bell-title{font-size:14px;line-height:20px;font-weight:600}
.kz-bell-item.is-unread .kz-bell-title{font-weight:700}
.kz-bell-item.is-unread .kz-bell-title::before{content:"";display:inline-block;width:8px;height:8px;border-radius:50%;background:#006194;margin-right:6px;vertical-align:middle}
.kz-bell-preview{font-size:13px;line-height:18px;color:#3f4850;overflow:hidden;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical}
.kz-bell-state{padding:28px 16px;text-align:center;font-size:14px;color:#3f4850}
.kz-bell-foot{display:flex;gap:8px;padding:10px 12px;border-top:1px solid #eee9df;background:#faf8f2}
.kz-bell-foot a{flex:1;text-align:center;padding:8px 10px;border-radius:8px;font-size:13px;font-weight:700;text-decoration:none;color:#006194;border:1px solid #cfe0ea;background:#fff}
.kz-bell-foot a.primary{background:#006194;border-color:#006194;color:#fff}`;

let unreadRequest = null;
let current = 0;

function injectStyle() {
  if (document.getElementById('kzBellStyle')) return;
  const style = document.createElement('style'); style.id = 'kzBellStyle'; style.textContent = STYLE; document.head.append(style);
}

function element(tag, text, className) {
  const node = document.createElement(tag);
  if (text !== undefined && text !== null) node.textContent = text;
  if (className) node.className = className;
  return node;
}

function formatDate(value) {
  return value ? new Date(value).toLocaleString('vi-VN', {timeZone: 'Asia/Ho_Chi_Minh', day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit'}) : '';
}

function render(count) {
  current = count || 0;
  document.querySelectorAll('[data-notification-bell]').forEach(button => {
    const badge = button.querySelector('.kz-bell-count');
    if (badge) { badge.hidden = !current; badge.textContent = current > 99 ? '99+' : current ? String(current) : ''; }
    button.setAttribute('aria-label', current ? `Thông báo, ${current} chưa đọc` : 'Thông báo');
  });
  document.querySelectorAll('a[href="/pages/notifications/"]').forEach(link => {
    if (link.closest('.kz-bell-panel')) return;
    let badge = link.querySelector('.kz-link-count');
    if (!badge) { badge = element('span', '', 'kz-link-count'); link.append(badge); }
    badge.hidden = !current; badge.textContent = current > 99 ? '99+' : current ? String(current) : '';
  });
  const dot = document.getElementById('notificationBadge');
  if (dot) dot.classList.toggle('hidden', !current);
}

function refresh() {
  if (document.hidden) return Promise.resolve();
  unreadRequest = fetchAPI(BASE + '/unread-count').then(data => render(data.unreadCount)).catch(() => {});
  return unreadRequest;
}

/** Khung xem nhanh: tối đa 6 thông báo mới nhất; bấm một dòng thì mở trang chi tiết (trang đó tự đánh dấu đã đọc). */
function attachPanel(button) {
  const panel = element('div', null, 'kz-bell-panel');
  panel.hidden = true; panel.id = 'kzBellPanel' + Math.random().toString(36).slice(2, 8);
  panel.setAttribute('role', 'dialog'); panel.setAttribute('aria-label', 'Thông báo mới');
  document.body.append(panel);
  button.setAttribute('aria-haspopup', 'dialog'); button.setAttribute('aria-expanded', 'false'); button.setAttribute('aria-controls', panel.id);

  const place = () => {
    const rect = button.getBoundingClientRect(), width = panel.offsetWidth || 380;
    panel.style.top = Math.round(rect.bottom + 8) + 'px';
    panel.style.left = Math.round(Math.max(12, Math.min(window.innerWidth - width - 12, rect.right - width))) + 'px';
  };
  const close = () => { panel.hidden = true; button.setAttribute('aria-expanded', 'false'); };

  async function open() {
    panel.hidden = false; button.setAttribute('aria-expanded', 'true'); place();
    const head = element('div', null, 'kz-bell-head'), title = element('div');
    title.append(element('strong', 'Thông báo'), document.createElement('br'), element('span', current ? `${current} chưa đọc` : 'Không có thông báo chưa đọc'));
    const settings = element('a', 'Tùy chọn'); settings.href = '/pages/notification-preferences/';
    head.append(title, settings);
    const body = element('div', null, 'kz-bell-state'); body.textContent = 'Đang tải…';
    const foot = element('div', null, 'kz-bell-foot');
    const all = element('a', 'Xem tất cả', 'primary'); all.href = '/pages/notifications/'; foot.append(all);
    if (button.dataset.canBroadcast === 'true') { const send = element('a', 'Gửi thông báo'); send.href = '/pages/notification-broadcasts/'; foot.append(send); }
    panel.replaceChildren(head, body, foot); place();
    try {
      const data = await fetchAPI(BASE + '?read=all&page=0&size=6');
      render(data.unreadCount);
      title.lastChild.textContent = data.unreadCount ? `${data.unreadCount} chưa đọc` : 'Không có thông báo chưa đọc';
      if (!data.items.length) { body.textContent = 'Chưa có thông báo nào.'; return; }
      const list = element('ul', null, 'kz-bell-list');
      data.items.forEach(item => {
        const row = element('li', null, 'kz-bell-item' + (item.isRead ? '' : ' is-unread'));
        const link = element('a'); link.href = '/pages/notifications/?id=' + encodeURIComponent(item.id);
        // Thông báo có trang liên quan (yêu cầu hỗ trợ, sự cố...): bấm là đánh dấu đã đọc rồi mở thẳng trang đó.
        if (item.actionPath && /^\/[^/]/.test(item.actionPath)) {
          link.href = item.actionPath;
          link.addEventListener('click', async event => {
            if (event.ctrlKey || event.metaKey || event.shiftKey || event.button) return;
            event.preventDefault();
            if (!item.isRead) { try { await fetchAPI(BASE + '/' + encodeURIComponent(item.id) + '/read', {method: 'POST', csrfPath: BASE + '/csrf'}); } catch { /* Vẫn mở trang dù chưa lưu được trạng thái đọc. */ } }
            location.assign(item.actionPath);
          });
        }
        const meta = element('span', null, 'kz-bell-meta');
        meta.append(element('span', item.typeLabel, 'kz-bell-chip'), element('time', formatDate(item.receivedAt)));
        link.append(meta, element('span', item.title, 'kz-bell-title'), element('span', item.preview, 'kz-bell-preview'));
        row.append(link); list.append(row);
      });
      body.replaceWith(list); place();
    } catch (error) {
      if (error?.status === 401) { location.assign('/login'); return; }
      body.textContent = error?.message || 'Chưa tải được thông báo. Vui lòng thử lại.';
    }
  }

  button.addEventListener('click', event => {
    event.preventDefault(); event.stopPropagation();
    if (panel.hidden) open(); else close();
  });
  document.addEventListener('click', event => { if (!panel.hidden && !panel.contains(event.target) && !button.contains(event.target)) close(); });
  document.addEventListener('keydown', event => { if (event.key === 'Escape' && !panel.hidden) { close(); button.focus(); } });
  window.addEventListener('resize', () => { if (!panel.hidden) place(); });
  window.addEventListener('scroll', () => { if (!panel.hidden) place(); }, true);
}

let started = false;

/** Gắn chuông và số chưa đọc cho mọi nút/link có trên trang; gọi lại an toàn khi khung vẽ thêm nút mới. */
export function attachNotificationBadges() {
  injectStyle();
  document.querySelectorAll('[data-notification-bell]').forEach(button => {
    if (button.dataset.bellReady) return;
    button.dataset.bellReady = 'true'; button.disabled = false; button.classList.add('kz-bell');
    // Header công khai đã có chấm đỏ riêng (#notificationBadge) nên không thêm số đè lên.
    if (!button.querySelector('.kz-bell-count, #notificationBadge')) { const badge = element('span', '', 'kz-bell-count'); badge.hidden = true; button.append(badge); }
    attachPanel(button);
  });
  if (!started) {
    started = true;
    document.addEventListener('visibilitychange', refresh);
    setInterval(refresh, 30000);
    return refresh();
  }
  render(current);
  return unreadRequest || Promise.resolve();
}

document.addEventListener('kizuna:notifications-unread', event => render(event.detail));

/** Hỏi lại số chưa đọc ngay (ví dụ vừa gửi thông báo mà chính mình cũng là người nhận). */
export function refreshNotificationBadges() { return refresh(); }
