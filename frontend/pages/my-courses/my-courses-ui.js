/**
 * KizunaTutor - Mảnh giao diện dùng chung của trang "Lớp học của tôi" (FE-06/FE-07, học viên).
 */

import { fileTypeLabel, escapeHtml } from '../../js/classroom-helpers.js';

const FILE_ICONS = { PDF: 'picture_as_pdf', DOC: 'description', DOCX: 'description', PNG: 'image', JPG: 'image', MP3: 'audio_file' };

/** Nút mở một tệp (tài liệu, đề bài, bài nộp): biểu tượng theo loại tệp, tên tệp, dung lượng nếu có. */
export function fileChipHtml(f, attrs, sizeText = '') {
  const icon = FILE_ICONS[String(f.fileType || '').toUpperCase()] || 'draft';
  const title = `${fileTypeLabel(f.fileType)}${sizeText ? ` · ${sizeText}` : ''}`;
  return `<button type="button" class="mc-file" ${attrs} title="${escapeHtml(title)}">
    <span class="material-symbols-outlined text-[18px] text-primary shrink-0" aria-hidden="true">${icon}</span>
    <span class="min-w-0 truncate">${escapeHtml(f.fileName)}</span>${sizeText ? `<span class="text-on-surface-variant shrink-0">· ${escapeHtml(sizeText)}</span>` : ''}</button>`;
}

/** Nút "Thử lại" trong khối báo lỗi tải dữ liệu. */
export const RETRY_BUTTON_HTML = '<button type="button" class="kz-btn mt-4" data-retry><span class="material-symbols-outlined text-[18px]" aria-hidden="true">refresh</span>Thử lại</button>';
