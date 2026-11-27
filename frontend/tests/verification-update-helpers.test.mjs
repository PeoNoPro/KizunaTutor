import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  APPROVED_EDITABLE_FIELDS, hasPendingUpdate, canUploadDocuments, documentChangeable, newDocumentCount, profileUpdateError,
  canRequestRemoval, removalError, MSG_NO_CHANGES, MSG_REMOVE_KEEPS_REQUIRED,
  formatResubmitTime, rejectedProfileHint, rejectedReadOnlyNote
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

const idDoc = { id: "id", type: "IDENTITY_DOCUMENT", status: "VERIFIED", fileAvailable: true };
const oldJlpt = { id: "j1", type: "JLPT_CERTIFICATE", status: "VERIFIED", fileAvailable: true };
const newJlpt = { id: "j2", type: "JLPT_CERTIFICATE", status: "PENDING", fileAvailable: true };
const withDocs = (docs) => ({ ...approved, documents: docs });

test("chỉ đề nghị gỡ giấy tờ đã xác minh, và không khi đang có yêu cầu chờ", () => {
  assert.equal(canRequestRemoval(approved, verifiedDoc), true);
  assert.equal(canRequestRemoval(approved, newDoc), false, "giấy tờ mới gỡ trực tiếp");
  assert.equal(canRequestRemoval({ ...approved, profileUpdate: { status: "PENDING" } }, verifiedDoc), false);
  assert.equal(canRequestRemoval({ editable: true }, verifiedDoc), false, "hồ sơ chưa duyệt không qua yêu cầu cập nhật");
});

test("gỡ giấy tờ là một thay đổi hợp lệ để gửi yêu cầu", () => {
  const data = withDocs([idDoc, oldJlpt, { ...oldJlpt, id: "j3" }]);
  assert.equal(profileUpdateError(data, "N2", "", []), MSG_NO_CHANGES);
  assert.equal(profileUpdateError(data, "N2", "", ["j1"]), null);
});

test("sau khi gỡ vẫn phải còn giấy tờ tùy thân và chứng chỉ tiếng Nhật", () => {
  assert.equal(removalError(withDocs([idDoc, oldJlpt]), ["j1"]), MSG_REMOVE_KEEPS_REQUIRED);
  assert.equal(removalError(withDocs([idDoc, oldJlpt]), ["id"]), MSG_REMOVE_KEEPS_REQUIRED);
  assert.equal(removalError(withDocs([idDoc, oldJlpt, newJlpt]), ["j1"]), null, "đã tải chứng chỉ thay thế");
  assert.equal(removalError(withDocs([idDoc, oldJlpt, { ...newJlpt, fileAvailable: false }]), ["j1"]), MSG_REMOVE_KEEPS_REQUIRED);
  assert.equal(removalError(withDocs([idDoc, oldJlpt]), []), null);
});

test('FE-16: hồ sơ bị từ chối còn thời gian chờ nêu thời điểm nộp lại; hết chờ thì mời nộp lại với giấy tờ mới', () => {
  const at = new Date(2026, 11, 3, 9, 30).toISOString();
  assert.equal(formatResubmitTime(at), '09:30 ngày 03/12/2026');
  const waiting = { status: 'REJECTED', editable: false, resubmitAvailableAt: at };
  assert.match(rejectedProfileHint(waiting), /nộp lại hồ sơ từ 09:30 ngày 03\/12\/2026/);
  assert.equal(rejectedReadOnlyNote(waiting), 'Chỉ xem — được chỉnh sửa và nộp lại từ 09:30 ngày 03/12/2026');
  assert.match(rejectedProfileHint({ status: 'REJECTED', editable: true }), /tải lên giấy tờ mới/);
  assert.match(rejectedProfileHint({ status: 'REJECTED', editable: false }), /liên hệ bộ phận hỗ trợ/);
});
