/**
 * Ô ngày hiển thị theo kiểu Việt Nam (ngày/tháng/năm) cho mọi trình duyệt.
 * Ô <input type="date"> gốc tự đổi thứ tự theo ngôn ngữ cài trên máy (Chrome tiếng Anh ra tháng/ngày/năm), trang web
 * không chỉnh được, nên ô gốc được giữ lại (ẩn) làm nơi chứa giá trị yyyy-mm-dd như cũ; người dùng gõ hoặc chọn lịch ở
 * một ô chữ "dd/mm/yyyy" đặt chồng lên. Code cũ vẫn đọc input.value, FormData và nghe sự kiện change như trước.
 * Áp dụng cả <input type="datetime-local"> (dd/mm/yyyy hh:mm).
 */
const STYLE = `
.vi-date{position:relative;display:inline-flex;align-items:center;width:100%;min-width:150px}
.vi-date>input.vi-date-text{width:100%;padding-right:38px!important;margin:0}
.vi-date>.vi-date-button{position:absolute;right:6px;top:50%;transform:translateY(-50%);display:inline-flex;align-items:center;justify-content:center;width:28px;height:28px;min-height:0!important;padding:0!important;border:0!important;border-radius:6px;background:transparent!important;color:#6b7280;cursor:pointer;box-shadow:none!important}
.vi-date>.vi-date-button:hover{background:rgba(0,97,148,.08)!important;color:#006194}
.vi-date>.vi-date-button:disabled{cursor:default;opacity:.5}
.vi-date>.vi-date-button svg{width:18px;height:18px}
.vi-date[hidden]{display:none!important}`;
const CALENDAR = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M16 3v4M8 3v4M3 10h18"/></svg>';
const pad = value => String(value).padStart(2, '0');
const valueDescriptor = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value');

function injectStyle() {
  if (document.getElementById('viDateStyle')) return;
  const style = document.createElement('style'); style.id = 'viDateStyle'; style.textContent = STYLE; document.head.append(style);
}

/** yyyy-mm-dd(Thh:mm) → dd/mm/yyyy( hh:mm). */
function toDisplay(value, withTime) {
  if (!value) return '';
  const [day, time] = value.split('T'), [y, m, d] = day.split('-');
  if (!y || !m || !d) return '';
  return `${d}/${m}/${y}` + (withTime && time ? ' ' + time.slice(0, 5) : '');
}

/** dd/mm/yyyy( hh:mm) → yyyy-mm-dd(Thh:mm); sai ngày (31/02...) hoặc sai dạng thì trả null. Chấp nhận dấu - hoặc . */
function parse(text, withTime) {
  const match = text.trim().replace(/[.-]/g, '/').match(withTime ? /^(\d{1,2})\/(\d{1,2})\/(\d{4})[ ,T]+(\d{1,2}):(\d{2})$/ : /^(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
  if (!match) return null;
  const [, d, m, y, h = '0', mi = '0'] = match;
  const date = new Date(Number(y), Number(m) - 1, Number(d));
  if (date.getFullYear() !== Number(y) || date.getMonth() !== Number(m) - 1 || date.getDate() !== Number(d)) return null;
  if (withTime && (Number(h) > 23 || Number(mi) > 59)) return null;
  return `${y}-${pad(m)}-${pad(d)}` + (withTime ? `T${pad(h)}:${pad(mi)}` : '');
}

function labelText(input) {
  if (input.getAttribute('aria-label')) return input.getAttribute('aria-label');
  const label = input.id ? document.querySelector(`label[for="${CSS.escape(input.id)}"]`) : input.closest('label');
  return (label?.textContent || input.title || '').trim();
}

export function enhanceDateInput(input) {
  if (input.dataset.viDate || !input.isConnected) return;
  input.dataset.viDate = 'on';
  const withTime = input.type === 'datetime-local';
  const wrap = document.createElement('span'); wrap.className = 'vi-date';
  const text = document.createElement('input');
  text.type = 'text'; text.className = (input.className ? input.className + ' ' : '') + 'vi-date-text';
  text.inputMode = 'numeric'; text.autocomplete = 'off'; text.spellcheck = false;
  text.placeholder = withTime ? 'dd/mm/yyyy hh:mm' : 'dd/mm/yyyy';
  text.title = (labelText(input) ? labelText(input) + ' ' : '') + `(${text.placeholder})`;
  if (labelText(input)) text.setAttribute('aria-label', labelText(input) + ', dạng ' + (withTime ? 'ngày/tháng/năm giờ:phút' : 'ngày/tháng/năm'));
  const button = document.createElement('button');
  button.type = 'button'; button.className = 'vi-date-button'; button.innerHTML = CALENDAR;
  button.setAttribute('aria-label', 'Mở lịch chọn ngày'); button.tabIndex = -1;

  input.replaceWith(wrap);
  // Ô chữ đứng trước để <label> bọc ngoài trỏ vào ô chữ.
  wrap.append(text, button, input);
  input.style.cssText += ';position:absolute!important;left:0!important;bottom:0!important;width:100%!important;height:1px!important;opacity:0!important;pointer-events:none!important;padding:0!important;border:0!important;margin:0!important';
  input.tabIndex = -1; input.setAttribute('aria-hidden', 'true');

  const show = () => { text.value = toDisplay(valueDescriptor.get.call(input), withTime); text.setCustomValidity(''); text.removeAttribute('aria-invalid'); };
  // Code đặt input.value = ... (giá trị mặc định, reset) thì ô chữ cũng đổi theo.
  Object.defineProperty(input, 'value', {configurable: true, get() { return valueDescriptor.get.call(this); }, set(value) { valueDescriptor.set.call(this, value); show(); }});
  const mirror = () => {
    wrap.hidden = input.hidden;
    text.disabled = button.disabled = input.disabled;
    text.required = input.required;
  };
  new MutationObserver(mirror).observe(input, {attributes: true, attributeFilter: ['hidden', 'disabled', 'required']});
  mirror(); show();

  input.addEventListener('change', show);
  input.addEventListener('input', show);
  input.form?.addEventListener('reset', () => setTimeout(show));
  button.addEventListener('click', () => {
    try { input.showPicker(); } catch { text.focus(); }
  });

  const commit = () => {
    const raw = text.value.trim();
    const iso = raw ? parse(raw, withTime) : '';
    const outOfRange = iso && ((input.min && iso < input.min) || (input.max && iso > input.max));
    if (iso === null || outOfRange) {
      text.setCustomValidity(iso === null ? `Vui lòng nhập theo dạng ${text.placeholder}.` : 'Ngày nằm ngoài khoảng cho phép.');
      text.setAttribute('aria-invalid', 'true'); text.reportValidity();
      return;
    }
    if (iso === valueDescriptor.get.call(input)) { show(); return; }
    valueDescriptor.set.call(input, iso); show();
    input.dispatchEvent(new Event('input', {bubbles: true}));
    input.dispatchEvent(new Event('change', {bubbles: true}));
  };
  text.addEventListener('change', commit);
  text.addEventListener('keydown', event => { if (event.key === 'Enter') { commit(); if (text.getAttribute('aria-invalid') === 'true') event.preventDefault(); } });
  text.addEventListener('input', () => { text.setCustomValidity(''); text.removeAttribute('aria-invalid'); });
}

export function enhanceDateInputs(root = document) {
  injectStyle();
  root.querySelectorAll('input[type=date]:not([data-vi-date]),input[type=datetime-local]:not([data-vi-date])').forEach(enhanceDateInput);
}

let watching = false;
/** Gắn cho các ô đang có và các ô được vẽ thêm sau này (hộp thoại, bộ lọc dựng bằng JS). */
export function watchDateInputs() {
  enhanceDateInputs();
  if (watching) return;
  watching = true;
  let queued = false;
  new MutationObserver(() => {
    if (queued) return;
    queued = true;
    requestAnimationFrame(() => { queued = false; enhanceDateInputs(); });
  }).observe(document.body, {childList: true, subtree: true});
}
