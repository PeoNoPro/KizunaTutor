/**
 * KizunaTutor - Danh sách trang công cụ học bị khóa khi học viên chưa vào lớp (không phụ thuộc DOM/API để test được).
 * Luôn mở: Tra cứu chữ Hán, Tra cứu viết tay, Đấu trường gõ Kana.
 * Thêm alias mới cho các trang dưới đây trong frontend/server.js thì phải thêm vào LOCKED_PATHS
 * (tests/learning-tools-access.test.mjs sẽ báo lỗi nếu quên).
 */

/** Key menu sidebar (components/learner-layout) của các công cụ bị khóa. */
export const LOCKED_MENU_KEYS = ['learning-support', 'grammar', 'shodo', 'ai-writing', 'flashcards'];

/** Thư mục trang (frontend/pages/<tên>) của các công cụ bị khóa. */
export const LOCKED_PAGE_DIRS = [
  'learning-support', 'grammar', 'shodo-practice', 'ai-writing',
  'flashcards', 'flashcards-detail', 'flashcards-match', 'flashcards-study', 'kanji-game'
];

/** Mọi đường dẫn (kể cả alias trong frontend/server.js) dẫn tới trang công cụ bị khóa. */
const LOCKED_PATHS = new Set([
  // Từ vựng
  '/learning-support', '/vocabulary', '/vocabulary.html', '/vocab', '/learner/vocabulary', '/learner/learning-support',
  // Ngữ pháp
  '/grammar', '/grammar.html', '/learner/grammar', '/learner/grammar-detail', '/grammar-detail', '/openjlpt',
  // Shodo
  '/shodo', '/shodo-practice', '/learner/practice-canvas', '/practice', '/kanji-practice',
  // Chữa câu AI
  '/ai-writing', '/ai-writing.html', '/learner/ai-writing', '/sakubun',
  // Bộ thẻ
  '/flashcards', '/flashcards.html', '/decks', '/learner/flashcards', '/learner/decks',
  '/flashcards-study', '/flashcards-study.html', '/flashcards-practice', '/learner/flashcards-study',
  '/flashcards-detail', '/learner/flashcards-detail', '/learner/deck-detail',
  '/flashcards-match', '/flashcards-match.html', '/learner/flashcards-match', '/match', '/learner/match',
  // Game Mưa chữ Hán
  '/kanji-game', '/kanji-game.html', '/game', '/rain',
  // Đường dẫn thật tới thư mục trang
  ...LOCKED_PAGE_DIRS.map((dir) => `/pages/${dir}`)
]);

/** Chuẩn hóa đường dẫn: bỏ query/hash, "/index.html" và dấu "/" ở cuối. */
export function normalizePath(pathname) {
  let path = String(pathname || '/').split(/[?#]/)[0];
  path = path.replace(/\/index\.html$/i, '');
  path = path.replace(/\/+$/, '');
  return path || '/';
}

export function isLockedToolPath(pathname) {
  return LOCKED_PATHS.has(normalizePath(pathname));
}
