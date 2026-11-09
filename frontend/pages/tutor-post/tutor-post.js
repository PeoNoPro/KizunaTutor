/**
 * KizunaTutor - Bài đăng của gia sư.
 * Gia sư có gói hội viên còn hạn soạn bài -> gửi cán bộ học vụ duyệt -> bài hiển thị ở trang Tìm gia sư.
 * API: GET/PUT /api/tutor/post.
 */

import { API, ApiError } from '/js/api.js';
import { mountTutorLayout, setTutorIdentity } from '/components/tutor-layout/tutor-layout.js';
import { formatDateTime, toast } from '/js/membership-format.js';

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
  let text = '';
  if (post.reviewStatus === 'PENDING') {
    text = `Gửi lúc ${formatDateTime(post.submittedAt)}. Bạn sẽ thấy kết quả tại đây sau khi cán bộ học vụ xem xét.`;
  } else if (post.reviewStatus === 'NEEDS_INFO') {
    text = `Nội dung cần bổ sung: ${post.reviewFeedback || '—'}\nHãy sửa bài bên dưới rồi bấm "Gửi lại".`;
  } else if (post.reviewStatus === 'REJECTED') {
    text = `Lý do: ${post.reviewFeedback || '—'}\nĐây là quyết định cuối cùng của cán bộ học vụ.`;
  } else {
    text = `Duyệt lúc ${formatDateTime(post.reviewedAt)}. Học viên có thể xem bài và kết nối với bạn trên trang Tìm gia sư.`;
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
  $('rateInput').value = d && d.hourlyRate != null ? Number(d.hourlyRate) : '';
  updateCounters();
}

/** Chỉ cho soạn/gửi khi chưa có bài hoặc được yêu cầu bổ sung; các trạng thái khác chỉ xem. */
function applyEditable(editable, post) {
  $('postForm').querySelectorAll('input, textarea, select').forEach((el) => { el.disabled = !editable; });
  $('submitRow').classList.toggle('hidden', !editable);
  $('submitText').textContent = post ? 'Gửi lại' : 'Gửi duyệt';
  $('formHint').textContent = editable
    ? (post ? 'Sửa các thông tin được yêu cầu rồi gửi lại để cán bộ học vụ duyệt.'
            : 'Điền thông tin và gửi để cán bộ học vụ duyệt trước khi hiển thị cho học viên.')
    : 'Bài đăng đang ở chế độ chỉ xem.';
}

function updateCounters() {
  $('titleCounter').textContent = $('titleInput').value.length;
  $('descriptionCounter').textContent = $('descriptionInput').value.length.toLocaleString('vi-VN');
}

function validate(p) {
  const errors = [];
  if (p.title.length < 10) errors.push('Tiêu đề tối thiểu 10 ký tự');
  if (p.description.length < 50) errors.push('Giới thiệu tối thiểu 50 ký tự');
  if (!p.teachingFormat) errors.push('Chọn hình thức dạy');
  if (!(p.hourlyRate >= 10000 && p.hourlyRate <= 10000000)) errors.push('Học phí mỗi giờ từ 10.000 đ đến 10.000.000 đ');
  return errors;
}

async function submit(event) {
  event.preventDefault();
  const payload = {
    title: $('titleInput').value.trim(),
    description: $('descriptionInput').value.trim(),
    specialties: $('specialtiesInput').value.trim(),
    teachingFormat: $('formatInput').value,
    hourlyRate: Number($('rateInput').value)
  };
  const errors = validate(payload);
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

function renderPost(post, editable) {
  renderStatus(post);
  fillForm(post);
  applyEditable(editable, post);
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
$('postForm').addEventListener('submit', submit);
['titleInput', 'descriptionInput'].forEach((id) => $(id).addEventListener('input', updateCounters));
loadIdentity();
load();
