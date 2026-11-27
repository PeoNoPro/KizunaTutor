/**
 * KizunaTutor - FE-07 Trang "Quản lý bài tập" chung của gia sư: /tutor/classrooms?view=assignments
 * - Gom bài nộp tệp + bài làm trực tiếp của mọi lớp (dùng API sẵn có của từng lớp, không cần API mới).
 * - 4 nhóm: Cần chấm · Đang chờ · Quá hạn · Đã chấm; lọc theo lớp, loại bài, từ khóa.
 * - Thẻ: hạn sắp tới, bài nháp, tổng quan.
 * - Hộp thoại "Giao bài tập": chọn lớp → loại bài → (làm trực tiếp) nguồn câu hỏi → hạn nộp nhanh
 *   (trước buổi học tới / +3 ngày / +1 tuần / chọn ngày / không hạn) → mở form giao bài nộp tệp hoặc trình soạn.
 */

import { API } from '../../js/api.js';
import { escapeHtml, friendlyErrorMessage } from '../../js/classroom-helpers.js';
import { formatDateTime, toLocalInput, fromLocalInput } from '../../js/exercise-helpers.js';
import {
  BUCKETS,
  fileAssignmentRow,
  exerciseRow,
  countBuckets,
  filterRows,
  sortRows,
  nextLessonDeadline,
  quickDeadline,
  ymdLocal
} from '../../js/assignment-overview-helpers.js';

const $ = (id) => document.getElementById(id);

const ov = {
  toast: () => {},
  classes: [],
  rows: [],
  drafts: [],
  exercises: [],
  bucket: 'review',
  filters: { spaceId: '', kind: '', keyword: '' },
  loading: false,
  dialog: { spaceId: '', kind: 'online', source: 'new', copyFrom: '', deadline: 'next', custom: '' },
  sessions: null
};

// ============================================================ khung trang

export function initAssignmentOverview({ showToast }) {
  ov.toast = showToast || (() => {});
  ensureDialog();
}

export function hideAssignmentOverview() {
  $('assignmentOverview')?.classList.add('hidden');
}

export async function showAssignmentOverview() {
  const root = $('assignmentOverview');
  if (!root) return;
  root.classList.remove('hidden');
  root.innerHTML = `
    <section class="kz-card p-6 flex flex-wrap items-center gap-4">
      <div class="w-14 h-14 rounded-2xl bg-tertiary-fixed text-on-tertiary-fixed-variant flex items-center justify-center shrink-0"><span class="material-symbols-outlined text-[28px]" aria-hidden="true">assignment</span></div>
      <div class="flex-1 min-w-[240px]">
        <h1 class="font-headline-md text-headline-md text-on-surface tracking-tight">Quản lý bài tập</h1>
        <p class="font-body-sm text-body-sm text-on-surface-variant mt-0.5">Bài tập của mọi lớp: cần chấm, đang chờ nộp, quá hạn, đã chấm.</p>
      </div>
      <button type="button" id="ovAssignBtn" class="kz-btn kz-btn-primary h-11 px-5 text-[15px]"><span class="material-symbols-outlined text-[20px]" aria-hidden="true">add</span>Giao bài tập</button>
    </section>
    <section class="kz-card p-4 md:p-5 space-y-4">
      <div id="ovTabs" role="tablist" aria-label="Nhóm bài tập" class="grid grid-cols-2 lg:grid-cols-4 gap-2.5"></div>
      <div class="flex flex-wrap gap-2.5">
        <label class="flex-1 min-w-[220px] flex items-center gap-2 h-11 px-3 rounded-xl bg-surface-container-low text-on-surface-variant">
          <span class="material-symbols-outlined text-[20px]" aria-hidden="true">search</span>
          <input id="ovSearch" type="text" placeholder="Tìm theo học viên, lớp hoặc tên bài" aria-label="Tìm bài tập" class="flex-1 bg-transparent outline-none text-on-surface">
        </label>
        <select id="ovClass" aria-label="Lọc theo lớp" class="kz-input w-auto h-11 py-0"><option value="">Tất cả lớp</option></select>
        <select id="ovKind" aria-label="Lọc theo loại bài" class="kz-input w-auto h-11 py-0">
          <option value="">Mọi loại bài</option><option value="file">Nộp file</option><option value="online">Làm trực tiếp</option>
        </select>
      </div>
      <div id="ovTable" class="overflow-x-auto"><p class="kz-item text-center text-on-surface-variant py-8">Đang tải bài tập của các lớp…</p></div>
    </section>
    <section id="ovCards" class="grid gap-4 md:grid-cols-3"></section>`;
  $('ovAssignBtn').addEventListener('click', () => openDialog());
  $('ovSearch').addEventListener('input', (e) => { ov.filters.keyword = e.target.value; renderTable(); });
  $('ovClass').addEventListener('change', (e) => { ov.filters.spaceId = e.target.value; renderTabs(); renderTable(); });
  $('ovKind').addEventListener('change', (e) => { ov.filters.kind = e.target.value; renderTabs(); renderTable(); });
  await load();
}

async function load() {
  ov.loading = true;
  try {
    const classes = (await API.classrooms.list()) || [];
    ov.classes = classes;
    const data = await Promise.all(classes.map(async (cls) => {
      const [files, exercises] = await Promise.all([
        API.assignments.list(cls.spaceId).catch(() => []),
        API.exercises.list(cls.spaceId).catch(() => [])
      ]);
      return { cls, files: files || [], exercises: exercises || [] };
    }));
    ov.rows = [];
    ov.drafts = [];
    ov.exercises = [];
    data.forEach(({ cls, files, exercises }) => {
      files.forEach((a) => ov.rows.push(fileAssignmentRow(cls, a)));
      exercises.forEach((e) => {
        ov.exercises.push({ cls, e });
        const row = exerciseRow(cls, e);
        if (row) ov.rows.push(row);
        else ov.drafts.push({ cls, e });
      });
    });
    // Mở lần đầu: nếu không có bài cần chấm thì mở nhóm "Đang chờ"
    if (!countBuckets(ov.rows).review && countBuckets(ov.rows).waiting) ov.bucket = 'waiting';
    $('ovClass').innerHTML = '<option value="">Tất cả lớp</option>'
      + classes.map((c) => `<option value="${escapeHtml(c.spaceId)}">${escapeHtml(c.title)} · ${escapeHtml(c.learnerName || 'Học viên')}</option>`).join('');
    renderTabs();
    renderTable();
    renderCards();
  } catch (err) {
    $('ovTable').innerHTML = `<div class="kz-item text-center py-8 space-y-3"><p>${escapeHtml(friendlyErrorMessage(err))}</p>
      <button type="button" id="ovRetry" class="kz-btn">Thử lại</button></div>`;
    $('ovRetry')?.addEventListener('click', load);
  } finally {
    ov.loading = false;
  }
}

function scopedRows() {
  return filterRows(ov.rows, { spaceId: ov.filters.spaceId, kind: ov.filters.kind });
}

function renderTabs() {
  const counts = countBuckets(scopedRows());
  const tone = { review: 'text-primary', waiting: 'text-primary', overdue: 'text-error', graded: 'text-[#1b6b3a]' };
  $('ovTabs').innerHTML = BUCKETS.map((b) => {
    const active = ov.bucket === b.key;
    return `<button type="button" role="tab" aria-selected="${active}" data-bucket="${b.key}"
      class="flex items-center justify-center gap-2 h-12 rounded-xl font-label-md text-[15px] font-semibold transition-colors ${active ? 'bg-primary text-on-primary' : 'border border-surface-container-high bg-surface-container-lowest text-on-surface hover:bg-surface-container-low'}">
      <span class="material-symbols-outlined text-[20px] ${active ? '' : tone[b.key]}" aria-hidden="true">${b.icon}</span>${b.label}
      <span class="min-w-[24px] px-2 rounded-full text-[13px] ${active ? 'bg-white/20' : (b.key === 'overdue' && counts[b.key] ? 'bg-error-container text-on-error-container' : 'bg-surface-container')}">${counts[b.key]}</span>
    </button>`;
  }).join('');
  $('ovTabs').querySelectorAll('[data-bucket]').forEach((btn) => btn.addEventListener('click', () => {
    ov.bucket = btn.dataset.bucket;
    renderTabs();
    renderTable();
  }));
}

function renderTable() {
  const box = $('ovTable');
  if (ov.loading && !ov.rows.length) return;
  const rows = sortRows(filterRows(ov.rows, { bucket: ov.bucket, ...ov.filters }), ov.bucket);
  if (!ov.classes.length) {
    box.innerHTML = '<p class="kz-item text-center text-on-surface-variant py-8">Bạn chưa có lớp học nào. Tạo lớp ở mục "Học viên của tôi" trước khi giao bài tập.</p>';
    return;
  }
  if (!rows.length) {
    const empty = { review: 'Không có bài nào cần chấm.', waiting: 'Không có bài nào đang chờ học viên.', overdue: 'Không có bài nào quá hạn.', graded: 'Chưa có bài nào đã chấm.' }[ov.bucket];
    box.innerHTML = `<div class="kz-item text-center py-10 space-y-3">
      <span class="material-symbols-outlined text-[36px] text-primary" aria-hidden="true">task_alt</span>
      <p class="font-title-sm text-title-sm">${empty}</p>
      <button type="button" class="kz-btn kz-btn-primary" data-empty-assign>Giao bài tập</button></div>`;
    box.querySelector('[data-empty-assign]').addEventListener('click', () => openDialog());
    return;
  }
  box.innerHTML = `
    <table class="w-full min-w-[820px] text-left border-separate border-spacing-0">
      <thead>
        <tr class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wide">
          <th class="px-4 py-2.5 bg-surface-container-low rounded-l-xl">Học viên · Lớp</th>
          <th class="px-4 py-2.5 bg-surface-container-low">Bài tập</th>
          <th class="px-4 py-2.5 bg-surface-container-low">Hạn</th>
          <th class="px-4 py-2.5 bg-surface-container-low">Tình trạng</th>
          <th class="px-4 py-2.5 bg-surface-container-low rounded-r-xl"><span class="sr-only">Thao tác</span></th>
        </tr>
      </thead>
      <tbody>
        ${rows.map((r) => {
          const initials = r.learnerName.split(/\s+/).slice(-2).map((w) => w.charAt(0)).join('').toUpperCase();
          return `
          <tr class="align-middle">
            <td class="px-4 py-3.5 border-b border-surface-container-high">
              <div class="flex items-center gap-2.5">
                <div class="w-9 h-9 rounded-full bg-primary-fixed text-on-primary-fixed-variant flex items-center justify-center font-semibold text-[13px] shrink-0">${escapeHtml(initials || 'HV')}</div>
                <div class="min-w-0"><p class="font-semibold truncate">${escapeHtml(r.learnerName)}</p><p class="font-caption text-caption text-on-surface-variant truncate">${escapeHtml(r.classTitle)}</p></div>
              </div>
            </td>
            <td class="px-4 py-3.5 border-b border-surface-container-high">
              <p class="font-semibold [overflow-wrap:anywhere]">${escapeHtml(r.title)}</p>
              <p class="flex flex-wrap gap-1.5 mt-1">
                <span class="kz-badge" ${r.kind === 'online' ? 'data-tone="active"' : 'style="background:#dce1ff;color:#1d3989"'}>${escapeHtml(r.typeLabel)}</span>
                ${r.meta ? `<span class="kz-badge">${escapeHtml(r.meta)}</span>` : ''}
                ${r.late ? '<span class="kz-badge" style="background:#ffdad6;color:#93000a">Nộp muộn</span>' : ''}
              </p>
            </td>
            <td class="px-4 py-3.5 border-b border-surface-container-high text-on-surface-variant whitespace-nowrap">${escapeHtml(r.dateLabel)}</td>
            <td class="px-4 py-3.5 border-b border-surface-container-high">${escapeHtml(r.need)}</td>
            <td class="px-4 py-3.5 border-b border-surface-container-high text-right">
              <a href="${escapeHtml(r.action.href)}" class="kz-btn ${r.action.primary ? 'kz-btn-primary' : ''} whitespace-nowrap">${escapeHtml(r.action.label)}</a>
            </td>
          </tr>`;
        }).join('')}
      </tbody>
    </table>`;
}

function renderCards() {
  const upcoming = sortRows(ov.rows.filter((r) => r.bucket === 'waiting' && r.sortTime < Number.MAX_SAFE_INTEGER
    && r.sortTime > Date.now()), 'waiting')[0];
  const draftsHtml = ov.drafts.slice(0, 3).map(({ cls, e }) => `
    <a class="block hover:underline text-primary [overflow-wrap:anywhere]" href="/pages/exercise-editor/index.html?space=${encodeURIComponent(cls.spaceId)}&exercise=${encodeURIComponent(e.exerciseId)}">${escapeHtml(e.title)}</a>
    <span class="block font-caption text-caption text-on-surface-variant">${escapeHtml(cls.title)} · ${e.questionCount} câu</span>`).join('');
  const total = ov.rows.length + ov.drafts.length;
  const card = (label, body) => `<div class="kz-card p-5"><p class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wide">${label}</p><div class="mt-2 space-y-1">${body}</div></div>`;
  $('ovCards').innerHTML = [
    card('Hạn sắp tới', upcoming
      ? `<p class="font-semibold [overflow-wrap:anywhere]">${escapeHtml(upcoming.title)}</p><p class="text-on-surface-variant">${escapeHtml(upcoming.classTitle)} · ${escapeHtml(upcoming.dateLabel)}</p>`
      : '<p class="text-on-surface-variant">Không có bài nào sắp tới hạn.</p>'),
    card(`Bài nháp (${ov.drafts.length})`, draftsHtml || '<p class="text-on-surface-variant">Không có bài nháp.</p>'),
    card('Tổng quan', `<p><b class="text-[18px]">${ov.classes.length}</b> lớp · <b class="text-[18px]">${total}</b> bài tập</p>
      <p class="text-on-surface-variant">${countBuckets(ov.rows).review} bài cần chấm · ${countBuckets(ov.rows).overdue} bài quá hạn</p>`)
  ].join('');
}

// ============================================================ hộp thoại "Giao bài tập"

function ensureDialog() {
  if ($('assignDialog')) return;
  const el = document.createElement('div');
  el.id = 'assignDialog';
  el.className = 'fixed inset-0 z-50 hidden kz-modal-backdrop overflow-y-auto p-4';
  el.setAttribute('role', 'dialog');
  el.setAttribute('aria-modal', 'true');
  el.setAttribute('aria-labelledby', 'adTitle');
  el.innerHTML = `
    <div class="kz-card shadow-xl w-full max-w-2xl mx-auto my-8 flex flex-col">
      <div class="flex items-center gap-3 px-6 py-5 border-b border-surface-container-high">
        <h2 id="adTitle" class="flex-1 font-headline-sm text-headline-sm">Giao bài tập</h2>
        <button type="button" data-ad-close class="kz-btn w-11 h-11 p-0" aria-label="Đóng"><span class="material-symbols-outlined">close</span></button>
      </div>
      <form id="adForm" class="px-6 py-5 space-y-6" novalidate>
        <fieldset class="space-y-2">
          <legend class="flex items-center gap-2.5 font-title-sm text-title-sm"><span class="w-7 h-7 rounded-full bg-primary text-on-primary text-[14px] flex items-center justify-center">1</span>Giao cho lớp nào</legend>
          <select id="adClass" class="kz-input" aria-label="Lớp học"></select>
          <p class="font-caption text-caption text-on-surface-variant">Chỉ hiện lớp đang hoạt động mà bạn quản lý được.</p>
        </fieldset>
        <fieldset class="space-y-2">
          <legend class="flex items-center gap-2.5 font-title-sm text-title-sm"><span class="w-7 h-7 rounded-full bg-primary text-on-primary text-[14px] flex items-center justify-center">2</span>Loại bài</legend>
          <div class="grid gap-3 sm:grid-cols-2">
            <button type="button" data-ad-kind="file" class="text-left p-4 rounded-xl border flex gap-3">
              <span class="material-symbols-outlined text-[26px] text-secondary" aria-hidden="true">upload_file</span>
              <span><span class="block font-semibold">Nộp file</span><span class="block font-body-sm text-body-sm text-on-surface-variant">Học viên tải bài làm lên, gia sư chấm sau hạn nộp.</span></span>
            </button>
            <button type="button" data-ad-kind="online" class="text-left p-4 rounded-xl border flex gap-3">
              <span class="material-symbols-outlined text-[26px] text-primary" aria-hidden="true">quiz</span>
              <span><span class="block font-semibold">Làm trực tiếp</span><span class="block font-body-sm text-body-sm text-on-surface-variant">Câu hỏi trên web, máy chấm câu có đáp án.</span></span>
            </button>
          </div>
        </fieldset>
        <fieldset id="adSourceBox" class="space-y-2">
          <legend class="flex items-center gap-2.5 font-title-sm text-title-sm"><span class="w-7 h-7 rounded-full bg-primary text-on-primary text-[14px] flex items-center justify-center">3</span>Câu hỏi lấy từ đâu</legend>
          <div class="inline-flex flex-wrap gap-1 p-1 rounded-xl bg-surface-container-low">
            <button type="button" data-ad-source="new" class="h-10 px-4 rounded-lg font-label-md text-label-md">Tự soạn</button>
            <button type="button" data-ad-source="copy" class="h-10 px-4 rounded-lg font-label-md text-label-md">Từ bài đã có</button>
            <button type="button" data-ad-source="excel" class="h-10 px-4 rounded-lg font-label-md text-label-md">Nhập file Excel</button>
          </div>
          <div id="adCopyBox" class="hidden space-y-1.5">
            <select id="adCopyFrom" class="kz-input" aria-label="Bài để sao chép"></select>
            <p class="font-caption text-caption text-on-surface-variant">Câu hỏi và cài đặt được sao chép thành bài nháp mới; sửa bản sao không ảnh hưởng bài cũ. File nghe cần tải lại cho bản sao.</p>
          </div>
          <p id="adExcelNote" class="hidden font-caption text-caption text-on-surface-variant">Trình soạn sẽ mở sẵn hộp "Nhập từ Excel": tải file mẫu, điền câu hỏi rồi chọn file để xem trước.</p>
        </fieldset>
        <fieldset class="space-y-2">
          <legend class="flex items-center gap-2.5 font-title-sm text-title-sm"><span id="adDeadlineStep" class="w-7 h-7 rounded-full bg-primary text-on-primary text-[14px] flex items-center justify-center">4</span>Hạn nộp</legend>
          <div class="flex flex-wrap gap-2">
            <button type="button" data-ad-deadline="next" class="h-11 px-4 rounded-full border font-label-md text-label-md">Trước buổi học tới</button>
            <button type="button" data-ad-deadline="3d" class="h-11 px-4 rounded-full border font-label-md text-label-md">+3 ngày</button>
            <button type="button" data-ad-deadline="1w" class="h-11 px-4 rounded-full border font-label-md text-label-md">+1 tuần</button>
            <button type="button" data-ad-deadline="custom" class="h-11 px-4 rounded-full border font-label-md text-label-md inline-flex items-center gap-1.5"><span class="material-symbols-outlined text-[18px]" aria-hidden="true">calendar_month</span>Chọn ngày…</button>
            <button type="button" data-ad-deadline="none" class="h-11 px-4 rounded-full border font-label-md text-label-md">Không đặt hạn</button>
          </div>
          <input id="adCustom" type="datetime-local" class="kz-input hidden" aria-label="Ngày giờ hạn nộp">
          <p id="adDeadlineText" class="flex items-center gap-2 rounded-xl bg-surface-container-low px-4 py-2.5 text-on-secondary-container"></p>
        </fieldset>
        <p id="adError" class="field-error hidden"></p>
      </form>
      <div class="flex gap-3 px-6 py-4 border-t border-surface-container-high">
        <button type="button" data-ad-close class="kz-btn flex-1 h-12">Hủy</button>
        <button type="button" id="adContinue" class="kz-btn kz-btn-primary flex-[1.4] h-12 text-[15px]">Tiếp tục</button>
      </div>
    </div>`;
  document.body.appendChild(el);
  el.querySelectorAll('[data-ad-close]').forEach((b) => b.addEventListener('click', closeDialog));
  el.addEventListener('click', (e) => { if (e.target === el) closeDialog(); });
  document.addEventListener('keydown', (e) => { if (e.key === 'Escape' && !el.classList.contains('hidden')) closeDialog(); });
  $('adClass').addEventListener('change', (e) => { ov.dialog.spaceId = e.target.value; renderDialog(); });
  $('adCopyFrom').addEventListener('change', (e) => { ov.dialog.copyFrom = e.target.value; });
  $('adCustom').addEventListener('change', (e) => { ov.dialog.custom = e.target.value; renderDialog(); });
  el.querySelectorAll('[data-ad-kind]').forEach((b) => b.addEventListener('click', () => { ov.dialog.kind = b.dataset.adKind; renderDialog(); }));
  el.querySelectorAll('[data-ad-source]').forEach((b) => b.addEventListener('click', () => { ov.dialog.source = b.dataset.adSource; renderDialog(); }));
  el.querySelectorAll('[data-ad-deadline]').forEach((b) => b.addEventListener('click', () => { ov.dialog.deadline = b.dataset.adDeadline; renderDialog(); }));
  $('adContinue').addEventListener('click', submitDialog);
}

function manageableClasses() {
  return ov.classes.filter((c) => c.canManage && !c.readOnly && c.status === 'ACTIVE');
}

async function openDialog() {
  const classes = manageableClasses();
  if (!classes.length) {
    ov.toast('Bạn chưa có lớp đang hoạt động để giao bài tập.', 'error');
    return;
  }
  ov.dialog = { spaceId: ov.filters.spaceId && classes.some((c) => c.spaceId === ov.filters.spaceId) ? ov.filters.spaceId : classes[0].spaceId,
    kind: 'online', source: 'new', copyFrom: '', deadline: 'next', custom: '' };
  $('adClass').innerHTML = classes.map((c) => `<option value="${escapeHtml(c.spaceId)}">${escapeHtml(c.title)} · ${escapeHtml(c.learnerName || 'Học viên')}</option>`).join('');
  $('adClass').value = ov.dialog.spaceId;
  const copyable = ov.exercises;
  $('adCopyFrom').innerHTML = copyable.length
    ? copyable.map(({ cls, e }) => `<option value="${escapeHtml(cls.spaceId)}|${escapeHtml(e.exerciseId)}">${escapeHtml(e.title)} · ${escapeHtml(cls.title)} · ${e.questionCount} câu${e.status === 'DRAFT' ? ' (nháp)' : ''}</option>`).join('')
    : '<option value="">Chưa có bài làm trực tiếp nào để sao chép</option>';
  ov.dialog.copyFrom = $('adCopyFrom').value;
  setFieldError('');
  $('assignDialog').classList.remove('hidden');
  renderDialog();
  if (ov.sessions === null) {
    // Buổi học 60 ngày tới của mọi lớp (để tính "trước buổi học tới")
    const now = new Date();
    const to = new Date(now);
    to.setDate(to.getDate() + 60);
    try {
      ov.sessions = (await API.classrooms.sessions(ymdLocal(now), ymdLocal(to))) || [];
    } catch (err) {
      ov.sessions = [];
    }
    renderDialog();
  }
}

function closeDialog() {
  $('assignDialog')?.classList.add('hidden');
}

/** Hạn nộp đang chọn: Date, null (không hạn) hoặc undefined (chưa xác định được). */
function chosenDeadline() {
  const d = ov.dialog;
  if (d.deadline === 'none') return null;
  if (d.deadline === '3d') return quickDeadline(3);
  if (d.deadline === '1w') return quickDeadline(7);
  if (d.deadline === 'custom') return d.custom ? new Date(fromLocalInput(d.custom)) : undefined;
  if (ov.sessions === null) return undefined;
  return nextLessonDeadline(ov.sessions, d.spaceId) || undefined;
}

function renderDialog() {
  const d = ov.dialog;
  const on = 'border-2 border-primary bg-surface-container-low';
  const off = 'border-surface-container-high bg-surface-container-lowest';
  document.querySelectorAll('[data-ad-kind]').forEach((b) => {
    b.className = `text-left p-4 rounded-xl border flex gap-3 ${b.dataset.adKind === d.kind ? on : off}`;
    b.setAttribute('aria-pressed', String(b.dataset.adKind === d.kind));
  });
  $('adSourceBox').classList.toggle('hidden', d.kind !== 'online');
  $('adDeadlineStep').textContent = d.kind === 'online' ? '4' : '3';
  document.querySelectorAll('[data-ad-source]').forEach((b) => {
    const active = b.dataset.adSource === d.source;
    b.className = `h-10 px-4 rounded-lg font-label-md text-label-md ${active ? 'bg-surface-container-lowest text-primary shadow-sm font-semibold' : 'text-on-surface-variant'}`;
    b.setAttribute('aria-pressed', String(active));
  });
  $('adCopyBox').classList.toggle('hidden', d.source !== 'copy');
  $('adExcelNote').classList.toggle('hidden', d.source !== 'excel');
  const next = ov.sessions ? nextLessonDeadline(ov.sessions, d.spaceId) : null;
  document.querySelectorAll('[data-ad-deadline]').forEach((b) => {
    const active = b.dataset.adDeadline === d.deadline;
    b.className = `h-11 px-4 rounded-full border font-label-md text-label-md inline-flex items-center gap-1.5 ${active ? 'bg-primary text-on-primary border-primary' : 'border-surface-container-high bg-surface-container-lowest'}`;
    b.setAttribute('aria-pressed', String(active));
    if (b.dataset.adDeadline === 'next') b.disabled = ov.sessions !== null && !next;
  });
  $('adCustom').classList.toggle('hidden', d.deadline !== 'custom');
  if (d.deadline === 'custom' && !$('adCustom').value) $('adCustom').value = toLocalInput(quickDeadline(3).toISOString());
  const due = chosenDeadline();
  const label = d.kind === 'online' ? 'Đóng đề lúc' : 'Hạn nộp';
  let text;
  if (d.deadline === 'next' && ov.sessions === null) text = 'Đang tìm buổi học tới của lớp…';
  else if (d.deadline === 'next' && !next) text = 'Lớp chưa có buổi học nào sắp tới. Hãy chọn cách đặt hạn khác.';
  else if (due === null) text = d.kind === 'online' ? 'Không đóng đề: học viên làm được bất cứ lúc nào.' : 'Không đặt hạn nộp.';
  else if (due) text = `${label} ${formatDateTime(due.toISOString())}${d.deadline === 'next' ? ' — 1 phút trước buổi học tới của lớp.' : '.'}`;
  else text = 'Chọn ngày giờ hạn nộp.';
  $('adDeadlineText').innerHTML = `<span class="material-symbols-outlined text-[18px]" aria-hidden="true">event</span><span>${escapeHtml(text)}</span>`;
}

function setFieldError(message) {
  const el = $('adError');
  if (!el) return;
  el.textContent = message || '';
  el.classList.toggle('hidden', !message);
}

function submitDialog() {
  const d = ov.dialog;
  const due = chosenDeadline();
  if (due === undefined) {
    setFieldError(d.deadline === 'next' ? 'Lớp chưa có buổi học sắp tới. Hãy chọn "+3 ngày", "+1 tuần" hoặc chọn ngày.' : 'Vui lòng chọn ngày giờ hạn nộp.');
    return;
  }
  if (due && due.getTime() <= Date.now()) {
    setFieldError('Hạn nộp phải ở tương lai.');
    return;
  }
  if (d.kind === 'file') {
    const q = new URLSearchParams({ space: d.spaceId, view: 'assignments', newFile: '1' });
    if (due) q.set('due', due.toISOString());
    window.location.href = `/tutor/classrooms?${q.toString()}`;
    return;
  }
  const q = new URLSearchParams({ space: d.spaceId });
  if (due) q.set('closes', due.toISOString());
  if (d.source === 'copy') {
    const [fromSpace, exerciseId] = String(d.copyFrom || '').split('|');
    if (!exerciseId) {
      setFieldError('Chưa có bài làm trực tiếp nào để sao chép. Hãy chọn "Tự soạn".');
      return;
    }
    q.set('copyFrom', exerciseId);
    q.set('fromSpace', fromSpace);
  }
  if (d.source === 'excel') q.set('import', '1');
  window.location.href = `/pages/exercise-editor/index.html?${q.toString()}`;
}
