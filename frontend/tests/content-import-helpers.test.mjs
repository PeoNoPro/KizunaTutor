import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  validateImportFile, reportStats, canConfirmImport, reportHint, findContentType, MAX_IMPORT_BYTES
} from '../js/content-import-helpers.js';

const baseReport = {
  dryRun: true, totalRows: 10, created: 2, skippedExisting: 8, duplicatesInFile: 0, invalidRows: 0
};

test('chỉ nhận tệp .xlsx tối đa 15 MB', () => {
  assert.match(validateImportFile(null), /chọn tệp/);
  assert.match(validateImportFile({ name: 'a.csv', size: 10 }), /\.xlsx/);
  assert.match(validateImportFile({ name: 'a.xlsx', size: MAX_IMPORT_BYTES + 1 }), /15 MB/);
  assert.match(validateImportFile({ name: 'a.xlsx', size: 0 }), /trống/);
  assert.equal(validateImportFile({ name: 'Kho.XLSX', size: 100 }), null);
});

test('chỉ cho nhập sau khi kiểm tra và có mục mới', () => {
  assert.equal(canConfirmImport(baseReport), true);
  assert.equal(canConfirmImport({ ...baseReport, created: 0 }), false);
  assert.equal(canConfirmImport({ ...baseReport, dryRun: false }), false, 'đã nhập rồi thì không nhập lại');
  assert.equal(canConfirmImport(null), false);
});

test('chỉ có số liệu thêm mới, không có cập nhật', () => {
  const keys = reportStats(baseReport).map((s) => s.key);
  assert.deepEqual(keys, ['totalRows', 'created', 'skippedExisting', 'duplicatesInFile', 'invalidRows']);
  assert.equal(reportStats(baseReport)[1].label, 'Sẽ thêm mới');
  assert.equal(reportStats({ ...baseReport, dryRun: false })[1].label, 'Đã thêm mới');
});

test('lời nhắc khi mọi mục đã có / có dòng lỗi', () => {
  assert.match(reportHint({ ...baseReport, created: 0 }), /đã có trong hệ thống/);
  assert.match(reportHint({ ...baseReport, invalidRows: 2 }), /dòng lỗi sẽ bị bỏ qua/i);
  assert.equal(findContentType('unknown').type, 'kanji');
});

test('danh sách: cột theo loại, ô trống hiện gạch ngang', async () => {
  const { LIST_COLUMNS, DETAIL_FIELDS, displayValue } = await import('../js/content-import-helpers.js');
  assert.deepEqual(Object.keys(LIST_COLUMNS), ['kanji', 'vocabulary', 'grammar']);
  assert.equal(LIST_COLUMNS.kanji[0].key, 'character');
  assert.ok(DETAIL_FIELDS.grammar.some(([k]) => k === 'extendedExamples'));
  assert.equal(displayValue('meaning', null), '—');
  assert.equal(displayValue('meaning', '  '), '—');
  assert.equal(displayValue('strokes', 14), '14');
});

test('lịch sử: định dạng ngày và tóm tắt số liệu', async () => {
  const { formatDateTime, historySummary } = await import('../js/content-import-helpers.js');
  assert.equal(formatDateTime(null), '—');
  assert.equal(formatDateTime('abc'), '—');
  assert.match(formatDateTime('2026-11-10T09:05:00Z'), /^10\/11\/2026 \d{2}:05$/);
  assert.equal(historySummary({ created: 3, skippedExisting: 6, invalidRows: 4 }), 'Thêm 3 mục mới · 6 mục đã có · 4 dòng lỗi');
  assert.equal(historySummary({ created: 0, skippedExisting: 0, invalidRows: 0 }), 'Thêm 0 mục mới');
});
