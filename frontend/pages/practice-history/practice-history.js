/**
 * FE-13: trang Lịch sử thi của học viên (tách khỏi trang Luyện đề, 2026-11-18).
 * Mỗi trang 10 lần làm, mới nhất trước; số trang nằm trên địa chỉ (?page=2) để tải lại / quay lại vẫn đúng trang.
 */
import { api, el, dateTime } from '/js/pages/practice-common.js';

const PAGE_SIZE = 10;
// Khung học viên chỉ chép nội dung <main> khi chuyển trang, nên dấu nhận diện nằm ở thẻ bọc bên trong.
const root = document.querySelector('[data-practice-page="history"]');
if (root) init(root);

function init(root) {
  const $ = id => root.querySelector('#' + id);
  const attemptUrl = id => '/learner/practice/attempt?id=' + encodeURIComponent(id);
  // Trên địa chỉ đánh số từ 1 cho dễ đọc; API đánh số từ 0.
  let page = Math.max(1, parseInt(new URLSearchParams(location.search).get('page'), 10) || 1);

  function row(item) {
    const link = el('a', undefined, 'pl-attempt'); link.href = attemptUrl(item.attemptId);
    const info = el('div');
    info.append(el('strong', `${item.level} · ${item.examTitle || item.modeLabel}${item.wrongOnly ? ' · Làm lại câu sai' : ''}`),
      el('small', `${item.sectionLabels.join(', ')} · ${dateTime(item.startedAt)}`));
    const score = el('div', undefined, 'pl-score');
    if (item.submitted) { score.append(document.createTextNode(`${item.correctCount}/${item.questionCount}`), el('small', 'câu đúng')); }
    else score.append(el('span', 'Đang làm', 'pr-badge'));
    link.append(info, score, el('span', item.submitted ? 'Xem lại ›' : 'Làm tiếp ›', 'pr-btn pl-go'));
    return link;
  }

  /** Các số trang cần hiện: trang đầu, cuối và 2 trang quanh trang đang xem; chỗ cách quãng hiện "…". */
  function pageNumbers(total) {
    const wanted = new Set([1, total, page - 1, page, page + 1].filter(n => n >= 1 && n <= total));
    const sorted = [...wanted].sort((a, b) => a - b), out = [];
    sorted.forEach((n, i) => { if (i && n - sorted[i - 1] > 1) out.push(null); out.push(n); });
    return out;
  }

  function renderPager(total) {
    const nav = $('historyPager');
    nav.hidden = total <= 1;
    if (total <= 1) { nav.replaceChildren(); return; }
    const button = (label, target, aria) => {
      const b = el('button', label); b.type = 'button'; b.setAttribute('aria-label', aria);
      b.disabled = target < 1 || target > total || target === page;
      if (target === page && /^\d+$/.test(label)) { b.setAttribute('aria-current', 'page'); b.disabled = false; }
      b.onclick = () => { if (target !== page) go(target); };
      return b;
    };
    nav.replaceChildren(button('‹', page - 1, 'Trang trước'),
      ...pageNumbers(total).map(n => n === null ? el('span', '…') : button(String(n), n, 'Trang ' + n)),
      button('›', page + 1, 'Trang sau'));
  }

  function go(target) {
    page = target;
    const url = new URL(location.href);
    if (page > 1) url.searchParams.set('page', page); else url.searchParams.delete('page');
    history.pushState(null, '', url);
    load();
  }

  async function load() {
    const box = $('historyList');
    box.replaceChildren(el('p', 'Đang tải…', 'pr-empty'));
    try {
      const data = await api(`/learner/practice/attempts?page=${page - 1}&size=${PAGE_SIZE}`);
      const total = Math.max(1, Math.ceil(data.totalElements / PAGE_SIZE));
      if (page > total && data.totalElements) { go(total); return; }   // trang trên địa chỉ vượt quá số trang thật
      box.replaceChildren(...(data.items.length ? data.items.map(row)
        : [el('p', 'Bạn chưa làm bài nào. Vào Luyện đề và bấm "Bắt đầu luyện ngay" để làm bài đầu tiên.', 'pr-empty')]));
      $('historyCount').textContent = data.totalElements ? `${data.totalElements} lần làm · trang ${page}/${total}` : '';
      renderPager(data.totalElements ? total : 0);
      root.closest('main')?.scrollTo({ top: 0 });
    } catch (error) { box.replaceChildren(el('p', error.message, 'pr-notice')); }
  }

  window.addEventListener('popstate', () => {
    if (!root.isConnected) return;
    page = Math.max(1, parseInt(new URLSearchParams(location.search).get('page'), 10) || 1);
    load();
  });
  load();
}
