import { test } from 'node:test';
import assert from 'node:assert/strict';
import { paginationState, PAGE_SIZE_OPTIONS } from '../components/pagination/pagination.js';

test('cỡ trang mặc định và các lựa chọn 10/20/50', () => {
  assert.deepEqual(PAGE_SIZE_OPTIONS, [10, 20, 50]);
  assert.equal(paginationState({ totalElements: 5 }).size, 10);
});

test('khoảng "a - b" đúng ở trang đầu, trang giữa và trang cuối', () => {
  assert.deepEqual(pick(paginationState({ page: 0, size: 10, totalElements: 25 })), { from: 1, to: 10, totalPages: 3 });
  assert.deepEqual(pick(paginationState({ page: 1, size: 10, totalElements: 25 })), { from: 11, to: 20, totalPages: 3 });
  assert.deepEqual(pick(paginationState({ page: 2, size: 10, totalElements: 25 })), { from: 21, to: 25, totalPages: 3 });
});

test('không có dữ liệu vẫn có 1 trang và hiện "0 - 0"', () => {
  const s = paginationState({ page: 0, size: 10, totalElements: 0 });
  assert.equal(s.totalPages, 1);
  assert.equal(s.from, 0);
  assert.equal(s.to, 0);
  assert.deepEqual(s.pages, [0]);
});

test('nhận cả totalItems / total và giới hạn trang vượt quá số trang', () => {
  assert.equal(paginationState({ totalItems: 40, size: 20 }).totalPages, 2);
  assert.equal(paginationState({ total: 40, size: 20, page: 9 }).current, 1);
  assert.equal(paginationState({ total: 40, size: 20, page: -3 }).current, 0);
});

test('số trang hiện: đầu, cuối, hiện tại và hai trang kề', () => {
  assert.deepEqual(paginationState({ page: 5, size: 10, totalElements: 200 }).pages, [0, 4, 5, 6, 19]);
  assert.deepEqual(paginationState({ page: 0, size: 10, totalElements: 200 }).pages, [0, 1, 19]);
});

function pick(s) {
  return { from: s.from, to: s.to, totalPages: s.totalPages };
}
