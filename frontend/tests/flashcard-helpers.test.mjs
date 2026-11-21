import { test } from 'node:test';
import assert from 'node:assert/strict';
import { enrichFlashcardItem, extractKanaReadingsFromText } from '../js/apiClient.js';

test('extractKanaReadingsFromText trích xuất chuẩn xác các cụm Kana', () => {
  assert.deepEqual(extractKanaReadingsFromText('べんきょう, Học tập'), ['べんきょう']);
  assert.deepEqual(extractKanaReadingsFromText('ニチ, ジツ • Mặt trời'), ['ニチ', 'ジツ']);
  assert.deepEqual(extractKanaReadingsFromText('ひ, -び, ほ- (Lửa)'), ['ひ', 'び', 'ほ']);
  assert.deepEqual(extractKanaReadingsFromText('Mặt trời không có kana'), []);
});

test('enrichFlashcardItem tự động gán reading nếu từ vựng mặt trước là Kana thuần', () => {
  const card = enrichFlashcardItem({
    char: 'こんにちは',
    meaning: 'Xin chào'
  });
  assert.equal(card.char, 'こんにちは');
  assert.equal(card.reading, 'こんにちは');
  assert.equal(card.meaning, 'Xin chào');
  assert.ok(card.id, 'Phải tự sinh ID nếu thiếu');
});

test('enrichFlashcardItem trích xuất reading khi nạp thẻ 2 cột chứa kana ở mặt sau', () => {
  const card = enrichFlashcardItem({
    char: '勉強',
    meaning: 'べんきょう, Việc học tập'
  });
  assert.equal(card.char, '勉強');
  assert.equal(card.reading, 'べんきょう');
  assert.equal(card.meaning, 'べんきょう, Việc học tập');
});

test('enrichFlashcardItem giữ nguyên reading rõ ràng khi nạp thẻ 3 cột', () => {
  const card = enrichFlashcardItem({
    char: '猫',
    reading: 'ねこ',
    meaning: 'Con mèo'
  });
  assert.equal(card.char, '猫');
  assert.equal(card.reading, 'ねこ');
  assert.equal(card.meaning, 'Con mèo');
});

test('enrichFlashcardItem bóc tách chuẩn khi nạp thẻ đa cột Kanji đầy đủ', () => {
  const card = enrichFlashcardItem({
    char: '日',
    hanViet: 'NHẬT',
    onyomi: 'ニチ, ジツ',
    kunyomi: 'ひ, -び',
    meaning: 'Mặt trời, ngày',
    jlpt: 'N5'
  });
  assert.equal(card.char, '日');
  assert.equal(card.hanViet, 'NHẬT');
  assert.equal(card.reading, 'ニチ');
  assert.equal(card.jlpt, 'N5');
});

test('enrichFlashcardItem an toàn khi dữ liệu rỗng hoặc thiếu trường', () => {
  const card = enrichFlashcardItem({});
  assert.equal(card.char, '');
  assert.equal(card.reading, '');
  assert.equal(card.meaning, '');
  assert.ok(card.id);
});
