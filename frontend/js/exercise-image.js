/**
 * KizunaTutor - FE-07 Ảnh minh họa cho câu hỏi bài làm trực tiếp (dùng chung trang soạn bài và trang làm bài).
 * Máy chủ lưu ảnh trên Cloudflare R2 khi bật, ngược lại trong DB; trình duyệt luôn tải qua API có kiểm quyền.
 */

import { API } from './api.js';

export const IMAGE_MAX_BYTES = 5 * 1024 * 1024;
export const IMAGE_EXTENSIONS = ['png', 'jpg', 'jpeg', 'webp', 'gif'];
export const IMAGE_ACCEPT = 'image/png,image/jpeg,image/webp,image/gif,.png,.jpg,.jpeg,.webp,.gif';
export const IMAGE_RULES_TEXT = 'Ảnh PNG, JPG, WEBP hoặc GIF, tối đa 5 MB.';

/** @returns {string} lỗi (rỗng nếu hợp lệ) */
export function validateImageFile(file) {
  if (!file) return 'Vui lòng chọn ảnh.';
  const ext = String(file.name || '').split('.').pop().toLowerCase();
  if (!IMAGE_EXTENSIONS.includes(ext)) return `Định dạng không hỗ trợ. ${IMAGE_RULES_TEXT}`;
  if (file.size > IMAGE_MAX_BYTES) return `Ảnh vượt quá 5 MB. ${IMAGE_RULES_TEXT}`;
  if (file.size === 0) return 'Ảnh rỗng, vui lòng chọn ảnh khác.';
  return '';
}

const cache = new Map(); // imageId -> Promise<objectURL>

/** Link tạm (object URL) của ảnh, dùng lại trong phiên để không tải lại khi đổi câu. */
export function imageObjectUrl(spaceId, imageId) {
  if (!cache.has(imageId)) {
    const p = API.exercises.imageBlob(spaceId, imageId).then((blob) => URL.createObjectURL(blob));
    p.catch(() => cache.delete(imageId));
    cache.set(imageId, p);
  }
  return cache.get(imageId);
}

/**
 * Vẽ ảnh của câu vào container. Bấm vào ảnh để mở cỡ lớn ở tab mới.
 * @param {HTMLElement} container
 * @param {{imageId: string, fileName?: string}} image
 */
export function mountQuestionImage(container, spaceId, image, { maxHeight = 360 } = {}) {
  if (!container || !image?.imageId) return;
  container.innerHTML = `<div class="rounded-xl bg-surface-container-low p-3 text-on-surface-variant text-[13px]">Đang tải ảnh...</div>`;
  imageObjectUrl(spaceId, image.imageId).then((url) => {
    const alt = image.fileName ? `Ảnh minh họa: ${image.fileName}` : 'Ảnh minh họa của câu hỏi';
    container.innerHTML = '';
    const link = document.createElement('a');
    link.href = url;
    link.target = '_blank';
    link.rel = 'noopener';
    link.title = 'Bấm để xem ảnh cỡ lớn';
    link.className = 'inline-block rounded-xl overflow-hidden bg-surface-container-low max-w-full';
    const img = document.createElement('img');
    img.src = url;
    img.alt = alt;
    img.className = 'block max-w-full h-auto object-contain';
    img.style.maxHeight = `${maxHeight}px`;
    link.appendChild(img);
    container.appendChild(link);
  }).catch(() => {
    container.innerHTML = `<div class="rounded-xl bg-error-container text-on-error-container p-3 text-[13px]">Không tải được ảnh. Vui lòng tải lại trang.</div>`;
  });
}
