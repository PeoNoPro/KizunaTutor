import { test } from 'node:test';
import assert from 'node:assert/strict';
import { splitJapaneseSentences, buildTranslationSentences, findBestMatchingSentence } from '../js/reading-translation-helpers.js';

test('splitJapaneseSentences tách đúng câu tiếng Nhật theo dấu 。！？ và xuống dòng', () => {
  const input = '私は毎日日本語を勉強します。週末は友達と映画を見ます！\n楽しいですか？';
  assert.deepEqual(splitJapaneseSentences(input), [
    '私は毎日日本語を勉強します。',
    '週末は友達と映画を見ます！',
    '楽しいですか？'
  ]);
});

test('buildTranslationSentences ưu tiên mảng sentenceTranslations khi có dữ liệu', () => {
  const list = buildTranslationSentences(
    ['Tôi học tiếng Nhật mỗi ngày.', 'Cuối tuần tôi xem phim với bạn.'],
    'Đoạn dịch gộp.'
  );
  assert.deepEqual(list, [
    'Tôi học tiếng Nhật mỗi ngày.',
    'Cuối tuần tôi xem phim với bạn.'
  ]);
});

test('buildTranslationSentences fallback tách fullTranslation khi thiếu sentenceTranslations', () => {
  const list = buildTranslationSentences([], 'Tôi học tiếng Nhật mỗi ngày. Cuối tuần tôi xem phim với bạn.');
  assert.deepEqual(list, [
    'Tôi học tiếng Nhật mỗi ngày.',
    'Cuối tuần tôi xem phim với bạn.'
  ]);
});

test('findBestMatchingSentence tìm đúng câu gốc kể cả khi dẫn chứng có dấu ... hoặc lệch nhẹ', () => {
  const original = '私は毎日日本語を勉強します。週末は友達と映画を見ます。';
  assert.equal(
    findBestMatchingSentence('週末は…映画を見ます', original),
    '週末は友達と映画を見ます。'
  );
  assert.equal(
    findBestMatchingSentence('「毎日日本語を勉強します」', original),
    '私は毎日日本語を勉強します。'
  );
  assert.equal(findBestMatchingSentence('関係ない文', original), '');
});
