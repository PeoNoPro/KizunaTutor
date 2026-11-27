import test from 'node:test';
import assert from 'node:assert/strict';
import { STATUS_PARAMS, fromVerification, fromPost, mergeRows, kpiCounts, breakdownText } from '../js/academic-dashboard-helpers.js';

test('STATUS_PARAMS ánh xạ "Cần bổ sung" sang trạng thái riêng của từng API', () => {
  assert.deepEqual(STATUS_PARAMS.NEEDS_INFO, { verification: 'ADDITIONAL_INFO_REQUIRED', post: 'NEEDS_INFO' });
});

test('fromVerification: yêu cầu cập nhật hồ sơ hiển thị là "Cập nhật hồ sơ", màu chờ duyệt', () => {
  const row = fromVerification({ tutorId: 7, fullName: 'An', email: 'a@x.vn', status: 'PROFILE_UPDATE', statusLabel: 'Chờ xét duyệt', highestJlptLevel: 'N2', experienceYears: 3 });
  assert.equal(row.typeLabel, 'Cập nhật hồ sơ');
  assert.equal(row.tone, 'PENDING');
  assert.equal(row.actionLabel, 'Thẩm định');
  assert.equal(row.href, '/academic/verifications/detail?tutorId=7');
  assert.equal(row.title, 'JLPT N2');
});

test('fromPost: lấy tiêu đề bài đăng, link về trang duyệt bài đăng', () => {
  const row = fromPost({ postId: 3, tutorName: 'Bình', reviewStatus: 'NEEDS_INFO', reviewStatusLabel: 'Cần bổ sung', draft: { title: 'Dạy N3' } });
  assert.equal(row.type, 'POST');
  assert.equal(row.title, 'Dạy N3');
  assert.equal(row.tone, 'NEEDS_INFO');
  assert.equal(row.href, '/academic/posts');
  assert.equal(row.actionLabel, 'Xem bài');
});

test('mergeRows: sắp theo ngày gửi, đơn chưa có ngày gửi ở cuối', () => {
  const v = [{ tutorId: 1, status: 'PENDING', submittedAt: '2026-11-20T10:00:00' }, { tutorId: 2, status: 'PENDING', submittedAt: null }];
  const p = [{ postId: 9, reviewStatus: 'PENDING', submittedAt: '2026-11-19T10:00:00' }];
  assert.deepEqual(mergeRows(v, p, 'oldest').map((r) => r.id), [9, 1, 2]);
  assert.deepEqual(mergeRows(v, p, 'newest').map((r) => r.id), [1, 9, 2]);
  assert.deepEqual(mergeRows(null, null), []);
});

test('kpiCounts cộng cả hai loại đơn và chịu được một nguồn bị lỗi', () => {
  const k = kpiCounts({ pendingCount: 2, profileUpdatePendingCount: 1, needsInfoCount: 1, approvedCount: 5, rejectedCount: 0 },
    { pendingCount: 4, needsInfoCount: 0, approvedCount: 3, rejectedCount: 2 });
  assert.deepEqual(k.pending, { total: 7, tutors: 3, posts: 4 });
  assert.equal(breakdownText(k.pending), '3 hồ sơ · 4 bài đăng');
  assert.equal(breakdownText(k.rejected), '2 bài đăng');
  assert.equal(kpiCounts(null, { pendingCount: 1 }).pending.total, 1);
  assert.equal(breakdownText(kpiCounts(null, null).approved), '');
});
