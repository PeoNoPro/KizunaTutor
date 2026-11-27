import test from 'node:test';
import assert from 'node:assert/strict';
import { validateImageFile, IMAGE_MAX_BYTES } from '../js/exercise-image.js';
import { questionPayload } from '../js/exercise-helpers.js';

test('ảnh câu hỏi: chỉ PNG/JPG/WEBP/GIF, tối đa 5 MB', () => {
  assert.equal(validateImageFile({ name: 'tranh.PNG', size: 1000 }), '');
  assert.equal(validateImageFile({ name: 'a.jpeg', size: IMAGE_MAX_BYTES }), '');
  assert.match(validateImageFile({ name: 'a.bmp', size: 10 }), /Định dạng không hỗ trợ/);
  assert.match(validateImageFile({ name: 'a.png', size: IMAGE_MAX_BYTES + 1 }), /5 MB/);
  assert.match(validateImageFile({ name: 'a.png', size: 0 }), /rỗng/);
  assert.match(validateImageFile(null), /chọn ảnh/);
});

test('dữ liệu gửi lên kèm mã ảnh của câu', () => {
  const q = { type: 'TRUE_FALSE', prompt: 'Đúng hay sai?', points: 1, data: { correct: true }, image: { imageId: 'img-1', fileName: 'a.png' } };
  assert.equal(questionPayload(q).imageId, 'img-1');
  assert.equal(questionPayload({ ...q, image: null }).imageId, null);
});
