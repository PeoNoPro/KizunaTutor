import test from 'node:test';
import assert from 'node:assert/strict';
import {
  validateAudioFile,
  playLimitLabel,
  remainingPlays,
  AUDIO_MAX_BYTES
} from '../js/exercise-audio.js';
import { questionPayload, defaultQuestion, validateQuestion, needsAudio, AUDIO_MISSING_MESSAGE } from '../js/exercise-helpers.js';

test('câu nghe chưa có file: báo thiếu file sau khi nội dung đã đủ; gửi kèm cờ câu nghe + số lần nghe', () => {
  const q = { ...defaultQuestion('TRUE_FALSE'), prompt: 'Nghe', points: 1, data: { correct: true }, listening: { maxPlays: 2 } };
  assert.equal(needsAudio(q), true);
  assert.equal(validateQuestion(q), AUDIO_MISSING_MESSAGE);
  // Nội dung còn thiếu thì báo lỗi nội dung trước
  assert.match(validateQuestion({ ...q, data: { correct: null } }), /Đúng hoặc Sai/);
  const p = questionPayload(q);
  assert.equal(p.audioId, null);
  assert.equal(p.listening, true);
  assert.equal(p.maxPlays, 2);
  // Đã gắn file → hết thiếu, số lần nghe lấy theo file
  const withFile = { ...q, audio: { audioId: 'x', maxPlays: 3 } };
  assert.equal(needsAudio(withFile), false);
  assert.equal(validateQuestion(withFile), null);
  assert.equal(questionPayload(withFile).maxPlays, 3);
  // Câu thường
  const plain = questionPayload({ ...defaultQuestion('TRUE_FALSE'), prompt: 'x', points: 1, data: { correct: false } });
  assert.equal(plain.listening, false);
});

test('file nghe: MP3/M4A/WAV/OGG/WEBM, tối đa 25 MB, không rỗng (khớp R2StorageService)', () => {
  assert.equal(validateAudioFile({ name: 'bai-nghe.mp3', size: 1024 }), null);
  assert.equal(validateAudioFile({ name: 'BAI.M4A', size: AUDIO_MAX_BYTES }), null);
  assert.equal(validateAudioFile({ name: 'ghi-am.wav', size: 1024 }), null);
  assert.equal(AUDIO_MAX_BYTES, 25 * 1024 * 1024);
  assert.match(validateAudioFile({ name: 'bai.flac', size: 1024 }), /MP3, M4A/);
  assert.match(validateAudioFile({ name: 'khongduoi', size: 1024 }), /MP3, M4A/);
  assert.match(validateAudioFile({ name: 'bai.mp3', size: AUDIO_MAX_BYTES + 1 }), /25 MB/);
  assert.match(validateAudioFile({ name: 'bai.mp3', size: 0 }), /trống/);
  assert.match(validateAudioFile(null), /chọn file/);
});

test('số lần nghe tối đa và số lần còn lại', () => {
  assert.equal(playLimitLabel(2), 'Nghe tối đa 2 lần');
  assert.equal(playLimitLabel(null), 'Nghe không giới hạn');
  assert.equal(remainingPlays(null, 7), null);
  assert.equal(remainingPlays(2, 0), 2);
  assert.equal(remainingPlays(2, 1), 1);
  assert.equal(remainingPlays(2, 5), 0);
});

test('câu có file nghe gửi kèm audioId và số lần nghe; câu không có file gửi null', () => {
  const q = { ...defaultQuestion('SINGLE_CHOICE'), prompt: 'Nghe', points: 2,
    audio: { audioId: 'abc', fileName: 'x.mp3', fileSize: 10, maxPlays: 2 } };
  const p = questionPayload(q);
  assert.equal(p.audioId, 'abc');
  assert.equal(p.maxPlays, 2);
  const unlimited = questionPayload({ ...q, audio: { audioId: 'abc', maxPlays: null } });
  assert.equal(unlimited.maxPlays, null);
  const none = questionPayload(defaultQuestion('TRUE_FALSE'));
  assert.equal(none.audioId, null);
  assert.equal(none.maxPlays, null);
});
