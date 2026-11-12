/**
 * KizunaTutor - Bài đăng của gia sư.
 * Gia sư có gói hội viên còn hạn soạn bài -> gửi cán bộ học vụ duyệt -> bài hiển thị ở trang Tìm gia sư.
 * API: GET/PUT /api/tutor/post.
 */

import { API, ApiError } from '/js/api.js';
import { mountTutorLayout, setTutorIdentity } from '/components/tutor-layout/tutor-layout.js';
import { formatDateTime, toast } from '/js/membership-format.js';
import { validatePostDraft, submitLabel, priceRowsFromContent, isUpdateMode } from '/js/tutor-post-helpers.js';

const $ = (id) => document.getElementById(id);
const STATES = ['loadingState', 'errorState', 'blockedState', 'contentState'];

const STATUS_VIEW = {
  PENDING: { icon: 'hourglass_top', box: 'bg-tertiary-fixed text-on-surface', iconColor: 'text-tertiary', title: 'Đang chờ cán bộ học vụ duyệt' },
  NEEDS_INFO: { icon: 'edit_note', box: 'bg-secondary-fixed text-on-surface', iconColor: 'text-secondary', title: 'Cán bộ học vụ yêu cầu bổ sung thông tin' },
  APPROVED: { icon: 'verified', box: 'bg-[#e3f4e8] text-on-surface', iconColor: 'text-success', title: 'Bài đăng đã được duyệt và đang hiển thị cho học viên' },
  REJECTED: { icon: 'block', box: 'bg-error-container text-on-surface', iconColor: 'text-error', title: 'Bài đăng đã bị từ chối' }
};

function show(id) {
  STATES.forEach((s) => $(s).classList.toggle('hidden', s !== id));
}

function errorText(err) {
  return err instanceof ApiError ? err.message : 'Hệ thống đang bận, bạn vui lòng thử lại sau.';
}

function renderStatus(post) {
  const banner = $('statusBanner');
  if (!post) {
    banner.classList.add('hidden');
    return;
  }
  const view = STATUS_VIEW[post.reviewStatus] || STATUS_VIEW.PENDING;
  banner.className = `rounded-2xl p-4 flex items-start gap-3 ${view.box}`;
  $('statusIcon').className = `material-symbols-outlined text-[22px] ${view.iconColor}`;
  $('statusIcon').textContent = view.icon;
  $('statusTitle').textContent = view.title;
  // Đã có bản đang hiển thị thì lần gửi gần nhất là bản cập nhật
  const isUpdate = Boolean(post.published) && post.reviewStatus !== 'APPROVED';
  const keepShown = '\nHọc viên vẫn thấy bản đã duyệt trước đó trên trang Tìm gia sư.';
  let text = '';
  if (post.reviewStatus === 'PENDING') {
    if (isUpdate) $('statusTitle').textContent = 'Bản cập nhật đang chờ cán bộ học vụ duyệt';
    text = `Gửi lúc ${formatDateTime(post.submittedAt)}. Bạn sẽ thấy kết quả tại đây sau khi cán bộ học vụ xem xét.`
      + (isUpdate ? keepShown : '');
  } else if (post.reviewStatus === 'NEEDS_INFO') {
    text = `Nội dung cần bổ sung: ${post.reviewFeedback || '—'}\nHãy sửa bài bên dưới rồi bấm "Gửi lại".`
      + (isUpdate ? keepShown : '');
  } else if (post.reviewStatus === 'REJECTED') {
    if (isUpdate) {
      $('statusTitle').textContent = 'Bản cập nhật bị từ chối';
      text = `Lý do: ${post.reviewFeedback || '—'}${keepShown}\nBạn có thể chỉnh sửa và gửi bản cập nhật khác.`;
    } else {
      text = `Lý do: ${post.reviewFeedback || '—'}\nĐây là quyết định cuối cùng của cán bộ học vụ.`;
    }
  } else {
    text = `Duyệt lúc ${formatDateTime(post.reviewedAt)}. Học viên có thể xem bài và kết nối với bạn trên trang Tìm gia sư.`
      + '\nBấm "Chỉnh sửa bài đăng" để gửi bản cập nhật; bản hiện tại vẫn hiển thị cho tới khi bản mới được duyệt.';
  }
  $('statusText').textContent = text;
  banner.classList.remove('hidden');
}

function fillForm(post) {
  const d = post ? post.draft : null;
  $('titleInput').value = d ? d.title || '' : '';
  $('descriptionInput').value = d ? d.description || '' : '';
  $('specialtiesInput').value = d ? d.specialties || '' : '';
  $('formatInput').value = d ? d.teachingFormat || '' : '';
  renderPriceRows(priceRowsFromContent(d));
  updateCounters();
}

// ---------------------------------------------------------------------------
// Bảng học phí theo môn học / cấp độ (danh sách lựa chọn lấy từ backend: priceLevels)
// ---------------------------------------------------------------------------
const priceState = { levels: [], editable: false };
// post: bài hiện tại; allowed: backend cho phép gửi; editing: đang mở form để sửa
const pageState = { post: null, allowed: false, editing: false };

function escapeAttr(text) {
  return String(text == null ? '' : text).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;');
}

function priceRowHtml(row) {
  const options = priceState.levels.map((l) =>
    `<option value="${escapeAttr(l.code)}" ${l.code === row.level ? 'selected' : ''}>${escapeAttr(l.label)}</option>`).join('');
  const rate = row.hourlyRate != null && row.hourlyRate !== '' ? Number(row.hourlyRate) : '';
  return `
    <div class="price-row grid grid-cols-[minmax(0,1fr)_minmax(0,11rem)_auto] gap-2 items-center">
      <select class="tv-input" data-price-level aria-label="Môn học / cấp độ">
        <option value="">Chọn môn học / cấp độ</option>${options}
      </select>
      <input class="tv-input" data-price-rate type="number" min="10000" max="10000000" step="1000"
             placeholder="Ví dụ: 250000" aria-label="Học phí mỗi giờ (đ)" value="${rate}">
      <button type="button" data-price-remove class="p-2 rounded-lg text-on-surface-variant hover:bg-error-container hover:text-error" aria-label="Xóa mức giá">
        <span class="material-symbols-outlined text-[20px]">delete</span>
      </button>
    </div>`;
}

function renderPriceRows(rows) {
  $('priceRows').innerHTML = rows.map(priceRowHtml).join('');
  syncPriceControls();
}

function collectPrices() {
  return [...$('priceRows').querySelectorAll('.price-row')].map((row) => ({
    level: row.querySelector('[data-price-level]').value,
    // Ô trống giữ '' để không hiện "0" khi vẽ lại; kiểm tra hợp lệ vẫn coi là sai
    hourlyRate: row.querySelector('[data-price-rate]').value === '' ? '' : Number(row.querySelector('[data-price-rate]').value)
  }));
}

/** Khóa/mở các ô theo chế độ sửa; ẩn môn đã chọn ở dòng khác; giới hạn số dòng bằng số môn. */
function syncPriceControls() {
  const rows = [...$('priceRows').querySelectorAll('.price-row')];
  const chosen = rows.map((r) => r.querySelector('[data-price-level]').value);
  rows.forEach((row, index) => {
    const select = row.querySelector('[data-price-level]');
    [...select.options].forEach((opt) => {
      opt.disabled = opt.value !== '' && chosen.some((v, i) => i !== index && v === opt.value);
    });
    select.disabled = !priceState.editable;
    row.querySelector('[data-price-rate]').disabled = !priceState.editable;
    row.querySelector('[data-price-remove]').classList.toggle('hidden', !priceState.editable || rows.length <= 1);
  });
  const add = $('addPriceButton');
  add.classList.toggle('hidden', !priceState.editable);
  add.disabled = rows.length >= Math.max(1, priceState.levels.length);
}

/** Chỉ cho soạn/gửi khi chưa có bài hoặc được yêu cầu bổ sung; các trạng thái khác chỉ xem. */
function applyEditable(editable, post) {
  priceState.editable = editable;
  $('postForm').querySelectorAll('input, textarea, select').forEach((el) => { el.disabled = !editable; });
  syncPriceControls();
  const label = submitLabel(editable, post);
  $('submitRow').classList.toggle('hidden', !label);
  if (label) $('submitText').textContent = label;
  const updateMode = isUpdateMode(pageState.allowed, post);
  $('editButton').classList.toggle('hidden', !updateMode || editable);
  $('cancelEditButton').classList.toggle('hidden', !updateMode || !editable);
  let hint = 'Bài đăng đang ở chế độ chỉ xem.';
  if (editable && updateMode) {
    hint = 'Sửa nội dung rồi bấm "Gửi bản cập nhật". Bản đang hiển thị giữ nguyên cho tới khi cán bộ học vụ duyệt bản mới.';
  } else if (editable) {
    hint = post ? 'Sửa các thông tin được yêu cầu rồi gửi lại để cán bộ học vụ duyệt.'
                : 'Điền thông tin và gửi để cán bộ học vụ duyệt trước khi hiển thị cho học viên.';
  } else if (updateMode) {
    hint = 'Bấm "Chỉnh sửa bài đăng" để cập nhật nội dung hoặc học phí.';
  }
  $('formHint').textContent = hint;
}

function updateCounters() {
  $('titleCounter').textContent = $('titleInput').value.length;
  $('descriptionCounter').textContent = $('descriptionInput').value.length.toLocaleString('vi-VN');
}


async function submit(event) {
  event.preventDefault();
  const payload = {
    title: $('titleInput').value.trim(),
    description: $('descriptionInput').value.trim(),
    specialties: $('specialtiesInput').value.trim(),
    teachingFormat: $('formatInput').value,
    prices: collectPrices()
  };
  const errors = validatePostDraft(payload);
  $('formError').textContent = errors.join('. ') + (errors.length ? '.' : '');
  $('formError').classList.toggle('hidden', errors.length === 0);
  if (errors.length) return;

  $('submitButton').disabled = true;
  try {
    const res = await API.tutorPost.submit(payload);
    toast(res.message, 'success');
    // Sau khi gửi, bài chuyển sang chờ duyệt: khóa form và ẩn nút gửi
    renderPost(res.post, false);
  } catch (err) {
    if (err instanceof ApiError && err.status === 401) return;
    $('formError').textContent = errorText(err);
    $('formError').classList.remove('hidden');
  } finally {
    $('submitButton').disabled = false;
  }
}

/** editable: backend cho phép gửi. Bài đã duyệt mở ở chế độ xem, bấm "Chỉnh sửa bài đăng" mới sửa được. */
function renderPost(post, editable) {
  pageState.post = post;
  pageState.allowed = Boolean(editable);
  pageState.editing = pageState.allowed && !isUpdateMode(pageState.allowed, post);
  renderStatus(post);
  fillForm(post);
  applyEditable(pageState.editing, post);
  $('formError').classList.add('hidden');
}

function startEditing() {
  pageState.editing = true;
  applyEditable(true, pageState.post);
  $('titleInput').focus();
}

function cancelEditing() {
  pageState.editing = false;
  fillForm(pageState.post);
  applyEditable(false, pageState.post);
  $('formError').classList.add('hidden');
}

async function load() {
  show('loadingState');
  try {
    const data = await API.tutorPost.getMine();
    if (!data.canPost && !data.post) {
      const needMembership = /gói hội viên/i.test(data.blockedReason || '');
      $('blockedText').textContent = data.blockedReason;
      $('blockedLink').href = needMembership ? '/tutor/membership' : '/tutor/verification';
      $('blockedLinkText').textContent = needMembership ? 'Xem gói hội viên' : 'Mở hồ sơ gia sư';
      show('blockedState');
      return;
    }
    priceState.levels = Array.isArray(data.priceLevels) ? data.priceLevels : [];
    renderPost(data.post, data.editable);
    // Gói hết hạn nhưng đã có bài: vẫn xem được, bài tạm ẩn khỏi trang Tìm gia sư
    if (!data.canPost) {
      $('formError').textContent = `${data.blockedReason} Bài đăng tạm ẩn khỏi trang Tìm gia sư cho tới khi bạn gia hạn.`;
      $('formError').classList.remove('hidden');
    }
    show('contentState');
  } catch (err) {
    if (err instanceof ApiError && err.status === 401) return;
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
    // Không chặn trang khi không tải được thông tin sidebar
  }
}

mountTutorLayout('post');
$('retryButton').addEventListener('click', load);
$('addPriceButton').addEventListener('click', () => {
  const rows = collectPrices();
  if (rows.length >= priceState.levels.length) return;
  renderPriceRows(rows.concat({ level: '', hourlyRate: '' }));
});
$('priceRows').addEventListener('click', (event) => {
  const remove = event.target.closest('[data-price-remove]');
  if (!remove) return;
  remove.closest('.price-row').remove();
  syncPriceControls();
});
$('priceRows').addEventListener('change', (event) => {
  if (event.target.matches('[data-price-level]')) syncPriceControls();
});
$('postForm').addEventListener('submit', submit);
$('editButton').addEventListener('click', startEditing);
$('cancelEditButton').addEventListener('click', cancelEditing);
['titleInput', 'descriptionInput'].forEach((id) => $(id).addEventListener('input', updateCounters));
loadIdentity();
load();
