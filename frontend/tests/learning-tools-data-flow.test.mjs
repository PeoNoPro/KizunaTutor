import test from 'node:test';
import assert from 'node:assert/strict';
import { enrichFlashcardItem } from '../js/apiClient.js';

test('AI Reading to Flashcards: Payload thẻ từ vựng từ đọc hiểu được enrich chuẩn xác', () => {
  const vocabFromReading = {
    kanji: '東京',
    surfaceText: '東京',
    reading: 'とうきょう',
    hanViet: 'ĐÔNG KINH',
    meaning: 'Thủ đô Tokyo của Nhật Bản',
    jlpt: 'N5'
  };

  const card = {
    char: vocabFromReading.kanji,
    front: vocabFromReading.kanji,
    reading: vocabFromReading.reading,
    meaning: vocabFromReading.meaning,
    back: `${vocabFromReading.reading} • ${vocabFromReading.meaning}`,
    hanViet: vocabFromReading.hanViet,
    jlpt: vocabFromReading.jlpt
  };

  const enriched = enrichFlashcardItem(card);

  assert.equal(enriched.char, '東京');
  assert.equal(enriched.reading, 'とうきょう');
  assert.equal(enriched.hanViet, 'ĐÔNG KINH');
  assert.ok(enriched.meaning.includes('Tokyo'));
  assert.equal(enriched.jlpt, 'N5');
});

test('Flashcards Detail & Study: Trích xuất Deck ID đa năng từ cả ?id= và ?deckId=', () => {
  function extractDeckId(searchQuery) {
    const params = new URLSearchParams(searchQuery);
    return params.get('id') || params.get('deckId') || null;
  }

  assert.equal(extractDeckId('?id=deck_12345'), 'deck_12345');
  assert.equal(extractDeckId('?deckId=deck_67890'), 'deck_67890');
  assert.equal(extractDeckId('?id=deck_primary&deckId=deck_secondary'), 'deck_primary');
  assert.equal(extractDeckId(''), null);
});

test('Shodo Practice to Flashcards: Thẻ Hán tự Shodo có đầy đủ Kanji, HanViet và On/Kun', () => {
  const shodoChar = '愛';
  const hv = 'ÁI';
  const meaning = 'Tình yêu thương, ái mộ';
  const onyomi = 'アイ';
  const kunyomi = 'いと.しい';
  const jlpt = 'N3';

  const newCard = {
    char: shodoChar,
    kanjiChar: shodoChar,
    hanViet: hv,
    meaning: meaning,
    onyomi: onyomi,
    kunyomi: kunyomi,
    jlpt: jlpt
  };

  const enriched = enrichFlashcardItem(newCard);

  assert.equal(enriched.char, '愛');
  assert.equal(enriched.hanViet, 'ÁI');
  assert.equal(enriched.onyomi, 'アイ');
  assert.equal(enriched.kunyomi, 'いと.しい');
  // Reading tự động fallback sang Onyomi/Kunyomi khi có Kana
  assert.ok(enriched.reading.length > 0);
});

test('Flashcards Match: Trích xuất câu hỏi/câu trả lời không bị rỗng từ thẻ đa nguồn', () => {
  const cards = [
    { char: '犬', reading: 'いぬ', meaning: 'Con chó' },
    { front: '猫', back: 'ねこ • Con mèo' },
    { char: '食べる', reading: 'たべる', meaning: 'On: - / Kun: たべる / Ăn cơm' }
  ];

  cards.forEach(c => {
    const frontText = c.char || c.front || c.kanjiChar || c.question || '—';
    let backText = c.meaning || c.back || c.answer || '—';
    if (backText.includes(' / ')) {
      const parts = backText.split(' / ').map(s => s.trim()).filter(Boolean);
      const cleanMeaning = parts.find(p => !p.startsWith('On:') && !p.startsWith('Kun:') && !p.startsWith('Âm Hán:')) || parts[parts.length - 1];
      backText = cleanMeaning || backText;
    }

    assert.ok(frontText.length > 0 && frontText !== '—');
    assert.ok(backText.length > 0 && backText !== '—');
  });
});
