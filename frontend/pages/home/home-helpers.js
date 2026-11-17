/**
 * Các hàm thuần túy phục vụ trang chủ KizunaTutor (xây URL lọc, làm sạch dữ liệu,
 * chọn danh sách gia sư thực tế và dữ liệu lộ trình JLPT).
 */

export const JLPT_ROADMAP = {
  N5: {
    level: 'N5',
    kanjiMark: '初級 • N5',
    title: 'N5 — Nền tảng nhập môn tiếng Nhật',
    summary: 'Làm quen bảng chữ cái Hiragana, Katakana, mẫu câu sinh hoạt hàng ngày và các chữ Hán cơ bản đầu tiên.',
    kanjiCount: 'Khoảng 100 chữ Hán',
    vocabCount: 'Khoảng 800 từ vựng',
    outcome: 'Đọc hiểu câu ngắn quen thuộc và tự giới thiệu, hỏi đường, mua sắm cơ bản.',
    recommendedFocus: 'Phù hợp người mới bắt đầu cần gia sư kèm phát âm chuẩn ngay từ buổi đầu.',
    toolHref: '/kana-quiz',
    toolLabel: 'Luyện Kana nhập môn'
  },
  N4: {
    level: 'N4',
    kanjiMark: '基礎 • N4',
    title: 'N4 — Giao tiếp đời sống & Chia thể động từ',
    summary: 'Nắm chắc các thể động từ (て, た, ない, khả năng, bị động) để diễn đạt dự định, kinh nghiệm và hội thoại tự nhiên.',
    kanjiCount: 'Khoảng 300 chữ Hán',
    vocabCount: 'Khoảng 1.500 từ vựng',
    outcome: 'Hiểu đoạn hội thoại tốc độ vừa trong đời sống, công việc bán thời gian và lớp học.',
    recommendedFocus: 'Kết hợp 2 buổi Kaiwa 1-1 mỗi tuần cùng bộ thẻ Flashcard ôn thể động từ.',
    toolHref: '/flashcards',
    toolLabel: 'Ôn bộ thẻ Flashcards'
  },
  N3: {
    level: 'N3',
    kanjiMark: '中級 • N3',
    title: 'N3 — Cầu nối trung cấp & Làm việc thực tế',
    summary: 'Bước chuyển quan trọng từ tiếng Nhật sách giáo khoa sang đọc báo ngắn, nghe hiểu ý chính và giao tiếp công sở.',
    kanjiCount: 'Khoảng 650 chữ Hán',
    vocabCount: 'Khoảng 3.700 từ vựng',
    outcome: 'Đủ nền tảng ứng tuyển các vị trí thực tập, kỹ sư IT hoặc du học bậc chuyên môn.',
    recommendedFocus: 'Ưu tiên gia sư luyện đề đọc hiểu N3 kết hợp sửa lỗi câu tự do Sakubun.',
    toolHref: '/grammar',
    toolLabel: 'Tra cẩm nang ngữ pháp'
  },
  N2: {
    level: 'N2',
    kanjiMark: '中上級 • N2',
    title: 'N2 — Tiếng Nhật thương mại & Phỏng vấn chuyên môn',
    summary: 'Sử dụng thuần thục kính ngữ (Keigo), đọc tài liệu chuyên ngành, email doanh nghiệp và xử lý tình huống công việc.',
    kanjiCount: 'Khoảng 1.000 chữ Hán',
    vocabCount: 'Khoảng 6.000 từ vựng',
    outcome: 'Đạt chuẩn tuyển dụng phổ biến của doanh nghiệp Nhật Bản và kỹ sư cầu nối (BrSE).',
    recommendedFocus: 'Luyện phỏng vấn giả lập Tenshoku và viết đoạn văn chuyên đề cùng Sensei N1/bản xứ.',
    toolHref: '/ai-writing',
    toolLabel: 'Luyện viết Sakubun AI'
  },
  N1: {
    level: 'N1',
    kanjiMark: '上級 • N1',
    title: 'N1 — Học thuật, Diễn đạt tinh tế & Bản xứ hóa',
    summary: 'Đọc phân tích văn bản phức tạp, nắm sắc thái tinh tế trong đàm phán cấp cao và trình bày lập luận mạch lạc.',
    kanjiCount: 'Khoảng 2.000+ chữ Hán',
    vocabCount: 'Khoảng 10.000+ từ vựng',
    outcome: 'Làm chủ ngữ cảnh văn học, báo chí, thuyết trình và làm việc độc lập hoàn toàn bằng tiếng Nhật.',
    recommendedFocus: 'Đồng hành cùng giảng viên chuyên ngành hoặc gia sư bản xứ phản biện trực tiếp 1-1.',
    toolHref: '/kanji-dictionary',
    toolLabel: 'Tra cứu Kanji nâng cao'
  }
};

export function escapeHtml(value) {
  if (value === null || value === undefined) return '';
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

const GPS_PATTERN = /[-+]?\d{1,3}\.\d{4,}/;
const STREET_DETAIL_PATTERN = /(?:^|[\s,])(?:\d+[A-Za-z]?(?:\/\d+)*\s+\S+|(?:số\s*nhà|ngõ|ngách|hẻm|kiệt|căn\s*hộ|tòa\s*nhà|đường)\s+\S+|số\s+\d+|(?<!thành\s)phố\s+\S+)/iu;

/**
 * Đảm bảo quy tắc GB-15: chỉ hiển thị khu vực hành chính chung (Quận/Huyện, Tỉnh/Thành),
 * không hiển thị số nhà, tên đường chi tiết hay tọa độ GPS lên trang chủ.
 */
export function sanitizePublicLocation(location) {
  if (!location || typeof location !== 'string') return '';
  const trimmed = location.trim();
  if (!trimmed || GPS_PATTERN.test(trimmed)) return '';

  const parts = trimmed.split(',').map((part) => part.trim()).filter(Boolean);
  const safeParts = parts.filter((part) => !STREET_DETAIL_PATTERN.test(part));
  if (safeParts.length === 0) return '';
  return safeParts.slice(-2).join(', ');
}

export function buildDiscoveryUrl({ keyword = '', jlpt = '', goal = '', format = '', price = '', location = '' } = {}) {
  const params = new URLSearchParams();
  if (keyword && String(keyword).trim()) params.set('q', String(keyword).trim());
  if (jlpt && String(jlpt).trim()) params.set('jlpt', String(jlpt).trim());
  if (goal && String(goal).trim()) params.set('goal', String(goal).trim());
  if (format && String(format).trim()) params.set('format', String(format).trim());
  if (price && String(price).trim()) params.set('price', String(price).trim());
  if (location && String(location).trim()) params.set('location', String(location).trim());

  const query = params.toString();
  return query ? `/tutors?${query}` : '/tutors';
}

/**
 * Ghép danh sách gia sư tiêu biểu và gia sư đã duyệt xếp hạng cao từ DB thật (tối đa 4 thẻ),
 * không trùng lặp theo tutorId và tuyệt đối không chèn dữ liệu giả.
 */
export function selectDisplayTutors(featuredList = [], searchList = [], maxCount = 4) {
  const seen = new Set();
  const merged = [];

  const appendUnique = (items, sourceTag) => {
    if (!Array.isArray(items)) return;
    for (const item of items) {
      if (!item) continue;
      const key = item.tutorId || item.tutorCode || item.fullName;
      if (!key || seen.has(key)) continue;
      seen.add(key);
      merged.push({ ...item, _sourceTag: sourceTag });
      if (merged.length >= maxCount) break;
    }
  };

  appendUnique(featuredList, 'featured');
  const featuredUsed = merged.length;
  if (merged.length < maxCount) {
    appendUnique(searchList, 'top-rated');
  }

  return {
    tutors: merged,
    hasFeatured: featuredUsed > 0,
    usedFallbackSearch: merged.length > featuredUsed
  };
}

export function getTutorInitials(fullName) {
  const clean = String(fullName || '').trim();
  if (!clean) return 'GS';
  const words = clean.split(/\s+/).filter(Boolean);
  if (words.length >= 2) {
    return `${words[0][0]}${words[words.length - 1][0]}`.toUpperCase();
  }
  return clean.slice(0, 2).toUpperCase();
}

export function getGridColumnsClass(tutorCount) {
  if (tutorCount <= 1) return 'grid-cols-1 lg:grid-cols-2';
  if (tutorCount === 2) return 'grid-cols-1 md:grid-cols-2';
  if (tutorCount === 3) return 'grid-cols-1 md:grid-cols-3';
  return 'grid-cols-1 md:grid-cols-2 lg:grid-cols-4';
}

export function getJlptTrackData(level = 'N3') {
  const key = String(level || 'N3').toUpperCase();
  return JLPT_ROADMAP[key] || JLPT_ROADMAP.N3;
}
