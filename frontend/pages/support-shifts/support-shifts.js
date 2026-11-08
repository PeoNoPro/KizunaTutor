/** Phân ca nhân viên hỗ trợ (FE-10): Quản lý xếp nhân viên vào 3 ca cố định mỗi ngày trên lịch tuần. */
import { byId, text, notice, avatar, createOperationsPage } from '../../js/pages/operations.js';
import { fetchAPI } from '../../js/api.js';

const BASE = '/v1/tickets/shifts';
// Lịch ca nằm trong khu yêu cầu hỗ trợ nên lấy mã CSRF của khu đó (cho cả Quản lý và nhân viên hỗ trợ).
const call = (path, method = 'GET', body) => fetchAPI(path, {method, body, csrfPath: '/v1/tickets/csrf'});
const state = {data: null, weekStart: null};
const WEEKDAYS = ['Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7', 'Chủ nhật'];
const dm = value => value.slice(8, 10) + '/' + value.slice(5, 7);
// Cộng ngày theo UTC để không bị lệch ngày do múi giờ +7.
const addDays = (iso, days) => { const [y, m, d] = iso.split('-').map(Number); return new Date(Date.UTC(y, m - 1, d + days)).toISOString().slice(0, 10); };

function currentShiftCode(data) {
  const now = new Date(), minutes = now.getHours() * 60 + now.getMinutes();
  return data.shiftTypes.find(s => { const [sh, sm] = s.start.split(':').map(Number), [eh, em] = s.end.split(':').map(Number); return minutes >= sh * 60 + sm && minutes < eh * 60 + em; })?.code;
}

function render() {
  const data = state.data, editable = data.canEdit, liveCode = currentShiftCode(data);
  const title = editable ? 'Xếp lịch làm việc CS' : 'Lịch làm việc'; byId('shiftTitle').textContent = title; document.querySelector('.shift-rule').hidden = !editable; document.title = title + ' - KizunaTutor';
  const crumb = document.querySelector('.topbar-breadcrumb strong'); if (crumb) crumb.textContent = title;
  byId('weekLabel').textContent = 'Tuần ' + dm(data.days[0].date) + ' – ' + dm(data.days[6].date);
  const head = text('tr'); head.append(text('th', 'Ca'));
  data.days.forEach((day, i) => { const th = text('th'); th.scope = 'col'; th.append(text('span', WEEKDAYS[i]), text('small', dm(day.date))); if (day.date === data.today) th.classList.add('is-today'); head.append(th); });
  byId('shiftHead').replaceChildren(head);
  const rows = data.shiftTypes.map(type => {
    const row = text('tr'), label = text('th'); label.scope = 'row';
    label.append(text('strong', type.label), text('small', type.start + ' – ' + type.end)); row.append(label);
    data.days.forEach(day => {
      const cell = text('td'), list = text('div', '', 'shift-people'), people = day.shifts[type.code] || [];
      if (day.date === data.today) cell.classList.add('is-today');
      if (day.date === data.today && type.code === liveCode) cell.classList.add('is-live');
      const past = day.date < data.today;
      people.forEach(person => {
        const chip = text('span', '', 'shift-person'); chip.append(avatar(person.displayName), text('span', person.displayName));
        if (editable && !past) {
          const remove = text('button', '×', 'shift-remove'); remove.type = 'button'; remove.setAttribute('aria-label', 'Gỡ ' + person.displayName + ' khỏi ' + type.label.toLowerCase() + ' ngày ' + dm(day.date));
          remove.addEventListener('click', () => page.run(async () => { state.data = await call(BASE + '/' + encodeURIComponent(person.shiftId) + '?weekStart=' + state.weekStart, 'DELETE'); render(); notice('Đã gỡ ' + person.displayName + ' khỏi ca.'); }));
          chip.append(remove);
        }
        list.append(chip);
      });
      if (!people.length) list.append(text('span', past ? '—' : 'Chưa có ai', 'shift-empty'));
      cell.append(list);
      if (editable && !past) {
        const taken = new Set(people.map(p => String(p.staffId))), free = data.staff.filter(s => !taken.has(String(s.userId)));
        if (free.length) {
          const select = text('select', '', 'shift-add'); select.setAttribute('aria-label', 'Thêm nhân viên vào ' + type.label.toLowerCase() + ' ngày ' + dm(day.date));
          const first = text('option', '+ Thêm nhân viên'); first.value = ''; select.append(first);
          free.forEach(s => { const o = text('option', s.displayName); o.value = s.userId; select.append(o); });
          select.addEventListener('change', () => { if (!select.value) return; const staffId = select.value; page.run(async () => { state.data = await call(BASE, 'POST', {staffId, date: day.date, shiftCode: type.code}); render(); notice('Đã xếp ca.'); }); });
          cell.append(select);
        }
      }
      row.append(cell);
    });
    return row;
  });
  byId('shiftBody').replaceChildren(...rows);
  byId('shiftHint').textContent = editable ? (data.staff.length ? '' : 'Chưa có nhân viên hỗ trợ đang hoạt động để xếp ca.') : '';
}

async function load() {
  state.data = await call(BASE + (state.weekStart ? '?weekStart=' + state.weekStart : ''));
  state.weekStart = state.data.weekStart; render();
}

const page = createOperationsPage(load);
byId('prevWeek').addEventListener('click', () => page.run(async () => { state.weekStart = addDays(state.weekStart, -7); await load(); }));
byId('nextWeek').addEventListener('click', () => page.run(async () => { state.weekStart = addDays(state.weekStart, 7); await load(); }));
byId('thisWeek').addEventListener('click', () => page.run(async () => { state.weekStart = null; await load(); }));
page.start();
