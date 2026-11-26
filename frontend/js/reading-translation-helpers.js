/**
 * Pure helpers tách câu tiếng Nhật và chuẩn hóa câu bản dịch cho trang Đọc hiểu (ai-reading).
 */

export function splitJapaneseSentences(text) {
  if (!text || typeof text !== 'string') return [];
  const parts = text.match(/[^。！？\n]+[。！？]?/g) || [];
  return parts.map((s) => s.trim()).filter(Boolean);
}

export function buildTranslationSentences(sentenceTranslations, fullTranslation) {
  let list = [];
  if (Array.isArray(sentenceTranslations)) {
    list = sentenceTranslations.map((s) => String(s || '').trim()).filter(Boolean);
  } else if (typeof sentenceTranslations === 'string' && sentenceTranslations.trim()) {
    list = [sentenceTranslations.trim()];
  }

  if (list.length > 0) {
    return list;
  }

  if (typeof fullTranslation === 'string' && fullTranslation.trim()) {
    return fullTranslation
      .trim()
      .split(/(?<=[.!?。！？])\s+/)
      .map((s) => s.trim())
      .filter(Boolean);
  }

  return [];
}

function normalizeJapaneseForMatch(str) {
  return String(str || '').replace(/[「」『』"'。！？、，．\s…・〜~]/g, '');
}

/**
 * Tìm câu tiếng Nhật trong văn bản gốc khớp nhất với đoạn dẫn chứng (dùng khi dẫn chứng có dấu ... hoặc sai khác nhẹ).
 */
export function findBestMatchingSentence(quote, originalText) {
  const normQuote = normalizeJapaneseForMatch(quote);
  if (!normQuote || !originalText) return '';

  const sentences = splitJapaneseSentences(originalText);
  if (sentences.length === 0) return '';

  for (const s of sentences) {
    const normS = normalizeJapaneseForMatch(s);
    if (normS && (normS.includes(normQuote) || normQuote.includes(normS))) {
      return s;
    }
  }

  if (normQuote.length < 2) return '';
  const quoteBigrams = new Set();
  for (let i = 0; i < normQuote.length - 1; i++) {
    quoteBigrams.add(normQuote.slice(i, i + 2));
  }

  let bestSentence = '';
  let bestScore = 0;
  for (const s of sentences) {
    const normS = normalizeJapaneseForMatch(s);
    if (normS.length < 2) continue;
    let score = 0;
    for (let i = 0; i < normS.length - 1; i++) {
      if (quoteBigrams.has(normS.slice(i, i + 2))) score++;
    }
    if (score > bestScore) {
      bestScore = score;
      bestSentence = s;
    }
  }

  const minRequired = Math.max(2, Math.floor(quoteBigrams.size * 0.25));
  return bestScore >= minRequired ? bestSentence : '';
}
