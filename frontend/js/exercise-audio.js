/**
 * KizunaTutor - FE-07 File nghe của câu hỏi bài làm trực tiếp (lưu trên Cloudflare R2).
 * - Máy chủ trả link nghe tạm (có hạn) của R2; thẻ <audio> phát thẳng từ link đó (tua được, không phải tải hết trước).
 *   Link hết hạn giữa chừng thì tự xin link mới.
 * - Số lần nghe tối đa chỉ được canh trên trình duyệt (đếm mỗi lần bắt đầu phát, lưu trong localStorage theo lượt làm bài).
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

function readPlays(key) {
  if (!key) return 0;
  try {
    return Number(localStorage.getItem(key)) || 0;
  } catch (e) {
    return 0;
  }
}

function writePlays(key, value) {
  if (!key) return;
  try {
    localStorage.setItem(key, String(value));
  } catch (e) {
    /* không lưu được thì chỉ đếm trong lần mở trang này */
  }
}

/** Khóa đếm số lần nghe của một câu trong một lượt làm bài */
export function playsKey(attemptId, questionId) {
  return `kz_listen_${attemptId}_${questionId}`;
}

/**
 * Gắn trình phát vào container.
 * @param {{audioId:string, fileName?:string, fileSize?:number, maxPlays?:number|null}} audio
 * @param {{limitKey?:string|null, showName?:boolean}} options limitKey: bật đếm số lần nghe (học viên đang làm bài)
 */
export function mountAudioPlayer(container, spaceId, audio, { limitKey = null, showName = false } = {}) {
  if (!container || !audio?.audioId) return;
  const maxPlays = limitKey ? audio.maxPlays || null : null;
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
      <audio data-audio class="w-full hidden" controls controlsList="nodownload noplaybackrate" preload="metadata"></audio>
    </div>`;
  const el = container.querySelector('[data-audio]');
  const status = container.querySelector('[data-audio-status]');
  const playsEl = container.querySelector('[data-plays]');
  let used = readPlays(limitKey);
  let playing = false;

  const refresh = () => {
    const left = remainingPlays(maxPlays, used);
    playsEl.textContent = maxPlays ? `Còn ${left}/${maxPlays} lần nghe` : (limitKey ? 'Nghe không giới hạn' : '');
    if (maxPlays && left === 0 && !playing) {
      el.pause();
      el.classList.add('hidden');
      status.textContent = `Bạn đã nghe hết ${maxPlays} lần cho câu này.`;
      status.classList.remove('hidden');
    }
  };

  el.addEventListener('play', () => {
    if (playing || !maxPlays) return;
    if (remainingPlays(maxPlays, used) === 0) {
      el.pause();
      refresh();
      return;
    }
    playing = true;
    used += 1;
    writePlays(limitKey, used);
    refresh();
  });
  el.addEventListener('ended', () => {
    playing = false;
    el.currentTime = 0;
    refresh();
  });

  const showError = (message) => {
    el.classList.add('hidden');
    status.textContent = message;
    status.classList.remove('hidden');
    status.classList.add('text-error');
  };

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

  audioUrl(spaceId, audio.audioId).then((url) => {
    if (!container.isConnected) return;
    el.src = url;
    el.classList.remove('hidden');
    status.classList.add('hidden');
    refresh();
  }).catch((err) => showError(`Không tải được file nghe. ${friendlyErrorMessage(err)}`));
  refresh();
}
