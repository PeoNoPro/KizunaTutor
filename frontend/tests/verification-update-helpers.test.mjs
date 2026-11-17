import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  APPROVED_EDITABLE_FIELDS, hasPendingUpdate, canUploadDocuments, documentChangeable, newDocumentCount, profileUpdateError
} from '../js/verification-update-helpers.js';

const verifiedDoc = { id: '1', status: 'VERIFIED', fileAvailable: true };
const newDoc = { id: '2', status: 'PENDING', fileAvailable: true };
const approved = { status: 'APPROVED', editable: false, updatable: true, profile: { highestJlptLevel: 'N2' }, documents: [verifiedDoc] };

test('hồ sơ đã phê duyệt chỉ sửa thông tin liên hệ, không sửa họ tên / ngày sinh / JLPT trực tiếp', () => {
  assert.deepEqual(APPROVED_EDITABLE_FIELDS, ['phone', 'address', 'experienceYears', 'bio']);
});

test('đã phê duyệt: tải thêm giấy tờ được, giấy tờ đã xác minh thì không sửa / gỡ', () => {
  assert.equal(canUploadDocuments(approved), true);
  assert.equal(documentChangeable(approved, verifiedDoc), false);
  assert.equal(documentChangeable(approved, newDoc), true);
  assert.equal(documentChangeable({ editable: true }, verifiedDoc), true, 'hồ sơ chưa nộp sửa tự do');
});

test('đang có yêu cầu chờ thẩm định: khóa phần giấy tờ', () => {
  const pending = { ...approved, profileUpdate: { status: 'PENDING' }, documents: [verifiedDoc, newDoc] };
  assert.equal(hasPendingUpdate(pending), true);
  assert.equal(canUploadDocuments(pending), false);
  assert.equal(documentChangeable(pending, newDoc), false);
  assert.equal(hasPendingUpdate({ ...approved, profileUpdate: { status: 'REJECTED' } }), false);
});

test('chỉ gửi yêu cầu khi đổi JLPT hoặc có giấy tờ mới', () => {
  assert.match(profileUpdateError(approved, 'N2', ''), /Chưa có thay đổi/);
  assert.equal(profileUpdateError(approved, 'N1', ''), null);
  const withNewDoc = { ...approved, documents: [verifiedDoc, newDoc, { status: 'PENDING', fileAvailable: false }] };
  assert.equal(newDocumentCount(withNewDoc), 1);
  assert.equal(profileUpdateError(withNewDoc, 'N2', ''), null);
  assert.equal(profileUpdateError(approved, 'N1', 'x'.repeat(1001)), 'Ghi chú tối đa 1.000 ký tự.');
});
