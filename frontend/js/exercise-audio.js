/**
 * KizunaTutor - FE-07 File nghe của câu hỏi bài làm trực tiếp (lưu trên Cloudflare R2).
 * - Máy chủ trả link nghe tạm (có hạn) của R2; thẻ <audio> phát thẳng từ link đó (tua được, không phải tải hết trước).
 *   Link hết hạn giữa chừng thì tự xin link mới.
 * - Câu có giới hạn số lần nghe (lúc làm bài): máy chủ đếm, mỗi lần bấm "Nghe" máy chủ cộng 1 lần rồi mới cấp link;
 *   đổi máy / trình duyệt hay xóa dữ liệu trình duyệt không nghe thêm được. Xem lại sau khi nộp: không giới hạn.
 */

import { API } from './api.js';
import { escapeHtml, formatFileSize, friendlyErrorMessage } from './classroom-helpers.js';

/** Khớp R2StorageService của máy chủ: MP3, WAV, OGG, M4A, WEBM; tối đa 25 MB. */
export const AUDIO_MAX_BYTES = 25 * 1024 * 1024;
export const AUDIO_EXTENSIONS = ['mp3', 'wav', 'ogg', 'm4a', 'webm'];
export const AUDIO_ACCEPT = '.mp3,.wav,.ogg,.m4a,.webm,audio/mpeg,audio/wav,audio/ogg,audio/mp4,audio/webm';
export const AUDIO_RULES_TEXT = 'MP3, M4A, WAV, OGG hoặc WEBM, tối đa 25 MB';
export const MAX_PLAYS_OPTIONS = [1, 2, 3, 4, 5];

/** @returns {string|null} lỗi khi chọn file nghe */
export function validateAudioFile(file) {
  if (!file) return 'Vui lòng chọn file nghe.';
  const name = String(file.name || '').toLowerCase();
  const ext = name.includes('.') ? name.slice(name.lastIndexOf('.') + 1) : '';
  if (!AUDIO_EXTENSIONS.includes(ext)) return 'File nghe chỉ nhận MP3, M4A, WAV, OGG hoặc WEBM.';
  if (file.size > AUDIO_MAX_BYTES) return 'File nghe tối đa 25 MB.';
  if (file.size === 0) return 'File nghe đang trống.';
  return null;
}

/** "Nghe tối đa 2 lần" / "Nghe không giới hạn" */
export function playLimitLabel(maxPlays) {
  return maxPlays ? `Nghe tối đa ${maxPlays} lần` : 'Nghe không giới hạn';
}

/** Số lần còn được nghe (null = không giới hạn) */
export function remainingPlays(maxPlays, used) {
  if (!maxPlays) return null;
  return Math.max(0, maxPlays - (Number(used) || 0));
}

/** audioId → { promise, expiresAt } — dùng lại link tạm trong trang, xin link mới trước khi hết hạn 1 phút. */
const urlCache = new Map();

function audioUrl(spaceId, audioId, { fresh = false } = {}) {
  const hit = urlCache.get(audioId);
  if (!fresh && hit && hit.expiresAt > Date.now()) return hit.promise;
  const entry = { expiresAt: Date.now() + 60_000 };
  entry.promise = API.exercises.audioUrl(spaceId, audioId).then((res) => {
    const ttl = Number(res?.expiresInSeconds) || 900;
    entry.expiresAt = Date.now() + Math.max(30, ttl - 60) * 1000;
    return res.url;
  });
  entry.promise.catch(() => urlCache.delete(audioId));
  urlCache.set(audioId, entry);
  return entry.promise;
}

/**
 * Gắn trình phát vào container.
 * @param {{audioId:string, fileName?:string, fileSize?:number, maxPlays?:number|null}} audio
 * @param {{listen?:{attemptId:string, questionId:string}|null, showName?:boolean}} options
 *   listen: học viên đang làm bài — câu có giới hạn số lần nghe thì máy chủ đếm (mỗi lần bấm nghe tính 1 lần).
 */
export function mountAudioPlayer(container, spaceId, audio, { listen = null, showName = false } = {}) {
  if (!container || !audio?.audioId) return;
  const counted = !!(listen && audio.maxPlays);
  container.innerHTML = `
    <div class="rounded-xl bg-surface-container-low px-4 py-3 space-y-2">
      <div class="flex flex-wrap items-center gap-2 font-label-md text-label-md">
        <span class="material-symbols-outlined text-[20px] text-primary" aria-hidden="true">headphones</span>
        <span>File nghe</span>
        ${showName && audio.fileName ? `<span class="font-caption text-caption text-on-surface-variant [overflow-wrap:anywhere]">${escapeHtml(audio.fileName)}${audio.fileSize ? ` · ${escapeHtml(formatFileSize(audio.fileSize))}` : ''}</span>` : ''}
        <span class="flex-1"></span>
        <span data-plays class="font-caption text-caption text-on-surface-variant"></span>
      </div>
      <p data-audio-status class="font-caption text-caption text-on-surface-variant">Đang tải file nghe…</p>
      ${counted ? `<button type="button" data-listen class="kz-btn kz-btn-primary hidden">
        <span class="material-symbols-outlined text-[18px]" aria-hidden="true">play_arrow</span><span data-listen-label>Nghe</span></button>` : ''}
      <audio data-audio class="w-full hidden" controls controlsList="nodownload noplaybackrate" preload="metadata"></audio>
    </div>`;
  const el = container.querySelector('[data-audio]');
  const status = container.querySelector('[data-audio-status]');
  const playsEl = container.querySelector('[data-plays]');

  const showStatus = (message, isError = false) => {
    status.textContent = message;
    status.classList.toggle('text-error', isError);
    status.classList.remove('hidden');
  };
  const showError = (message) => {
    el.classList.add('hidden');
    showStatus(message, true);
  };

  if (counted) {
    mountCountedPlayer({ container, spaceId, audio, listen, el, status, playsEl, showStatus });
    return;
  }

  playsEl.textContent = listen ? 'Nghe không giới hạn' : '';
  // Link tạm hết hạn (để trang mở lâu) → xin link mới một lần rồi phát tiếp từ vị trí cũ
  let retried = false;
  el.addEventListener('error', () => {
    if (retried || !el.src) {
      showError('Không phát được file nghe. Vui lòng tải lại trang.');
      return;
    }
    retried = true;
    const at = el.currentTime;
    audioUrl(spaceId, audio.audioId, { fresh: true }).then((url) => {
      el.src = url;
      el.currentTime = at;
    }).catch((err) => showError(`Không tải được file nghe. ${friendlyErrorMessage(err)}`));
  });
  el.addEventListener('ended', () => { el.currentTime = 0; });

  audioUrl(spaceId, audio.audioId).then((url) => {
    if (!container.isConnected) return;
    el.src = url;
    el.classList.remove('hidden');
    status.classList.add('hidden');
  }).catch((err) => showError(`Không tải được file nghe. ${friendlyErrorMessage(err)}`));
}

/**
 * Câu có giới hạn số lần nghe trong lúc làm bài: chưa có link cho tới khi bấm "Nghe";
 * mỗi lần bấm, máy chủ cộng 1 lần nghe rồi mới cấp link. Nghe xong thì đóng trình phát, còn lượt thì hiện lại nút.
 */
function mountCountedPlayer({ container, spaceId, audio, listen, el, status, playsEl, showStatus }) {
  const btn = container.querySelector('[data-listen]');
  const btnLabel = container.querySelector('[data-listen-label]');
  const max = audio.maxPlays;
  let left = null;
  let playing = false;
  let resumed = false;

  const render = () => {
    playsEl.textContent = left === null ? playLimitLabel(max) : `Còn ${left}/${max} lần nghe`;
    if (playing) {
      btn.classList.add('hidden');
      return;
    }
    el.classList.add('hidden');
    if (left === 0) {
      btn.classList.add('hidden');
      showStatus(`Bạn đã nghe hết ${max} lần cho câu này.`);
      return;
    }
    btnLabel.textContent = left === null ? 'Nghe' : `Nghe (còn ${left} lần)`;
    btn.classList.remove('hidden');
  };

  const stop = () => {
    playing = false;
    el.pause();
    el.removeAttribute('src');
    el.load();
    render();
  };

  btn.addEventListener('click', async () => {
    btn.disabled = true;
    try {
      const grant = await API.exercises.listen(spaceId, listen.attemptId, listen.questionId, false);
      left = grant?.remaining ?? left;
      playing = true;
      resumed = false;
      status.classList.add('hidden');
      el.src = grant.url;
      el.classList.remove('hidden');
      render();
      el.play().catch(() => { /* trình duyệt chặn tự phát: học viên bấm nút phát của trình phát */ });
    } catch (err) {
      if (err?.status === 409 && /nghe hết/.test(String(err?.userMessage || ''))) left = 0;
      render();
      if (left !== 0) showStatus(friendlyErrorMessage(err), true);
    } finally {
      btn.disabled = false;
    }
  });

  el.addEventListener('ended', stop);
  // Link hết hạn giữa lúc đang nghe: xin lại link không tính thêm lần (máy chủ cho tối đa 2 lần)
  el.addEventListener('error', async () => {
    if (!playing || !el.getAttribute('src')) return;
    if (resumed) {
      stop();
      showStatus('Không phát tiếp được. Bấm Nghe để nghe lại (tính thêm 1 lần).', true);
      return;
    }
    resumed = true;
    const at = el.currentTime;
    try {
      const grant = await API.exercises.listen(spaceId, listen.attemptId, listen.questionId, true);
      el.src = grant.url;
      el.currentTime = at;
      el.play().catch(() => {});
    } catch (err) {
      stop();
      showStatus(friendlyErrorMessage(err), true);
    }
  });

  API.exercises.listenStatus(spaceId, listen.attemptId).then((list) => {
    if (!container.isConnected) return;
    const mine = (list || []).find((x) => x.questionId === listen.questionId);
    left = mine ? (mine.remaining ?? null) : remainingPlays(max, 0);
    status.classList.add('hidden');
    render();
  }).catch((err) => {
    render();
    showStatus(`Không tải được số lần nghe. ${friendlyErrorMessage(err)}`, true);
  });
}
