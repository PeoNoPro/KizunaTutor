/**
 * KizunaTutor - Hàm thuần cho trang "Nhập kho học liệu" của cán bộ học vụ (không đụng DOM, test bằng node --test).
 */

export const MAX_IMPORT_BYTES = 15 * 1024 * 1024;

/** type gửi lên API + tên hiển thị + tệp mẫu đang có trong thư mục /database */
export const CONTENT_TYPES = [
  { type: 'kanji', apiType: 'KANJI', label: 'Kanji', icon: 'translate', sample: 'kanji_vietnamese_full.xlsx' },
  { type: 'vocabulary', apiType: 'VOCABULARY', label: 'Từ vựng', icon: 'spellcheck', sample: 'Kho_Tu_Vung_OpenJLPT_Full_Tieng_Viet.xlsx' },
  { type: 'grammar', apiType: 'GRAMMAR', label: 'Ngữ pháp', icon: 'menu_book', sample: 'Kho_Ngu_Phap_OpenJLPT_Tieng_Viet.xlsx' }
];

export function findContentType(type) {
  return CONTENT_TYPES.find((t) => t.type === type) || CONTENT_TYPES[0];
}

/** Lỗi khi chọn tệp, hoặc null nếu hợp lệ. */
export function validateImportFile(file) {
  if (!file) return 'Vui lòng chọn tệp Excel cần nhập.';
  if (!/\.xlsx$/i.test(file.name || '')) return 'Chỉ nhận tệp Excel định dạng .xlsx.';
  if (file.size === 0) return 'Tệp đang trống.';
  if (file.size > MAX_IMPORT_BYTES) return 'Tệp vượt quá 15 MB. Vui lòng tách nhỏ tệp rồi nhập từng phần.';
  return null;
}

export function formatCount(value) {
  return Number(value || 0).toLocaleString('vi-VN');
}

/**
 * Các ô số liệu của báo cáo. Khi chỉ kiểm tra thì ghi "Sẽ thêm mới".
 * Mục đã có luôn giữ nguyên, không có cập nhật.
 * tone: success | muted | warning | error
 */
export function reportStats(report) {
  if (!report) return [];
  return [
    { key: 'totalRows', label: 'Dòng dữ liệu đọc được', value: report.totalRows, tone: 'muted' },
    { key: 'created', label: report.dryRun ? 'Sẽ thêm mới' : 'Đã thêm mới', value: report.created, tone: 'success' },
    { key: 'skippedExisting', label: 'Đã có, giữ nguyên', value: report.skippedExisting, tone: 'muted' },
    { key: 'duplicatesInFile', label: 'Trùng lặp trong tệp', value: report.duplicatesInFile, tone: 'warning' },
    { key: 'invalidRows', label: 'Dòng lỗi bị bỏ qua', value: report.invalidRows, tone: 'error' }
  ];
}

/** Chỉ cho bấm "Nhập vào hệ thống" sau một lần kiểm tra có mục mới cần thêm. */
export function canConfirmImport(report) {
  return Boolean(report && report.dryRun && report.created > 0);
}

export function issueLevelLabel(level) {
  return level === 'ERROR' ? 'Lỗi' : 'Lưu ý';
}

/** Lời nhắc dưới báo cáo kiểm tra. */
export function reportHint(report) {
  if (!report || !report.dryRun) return '';
  if (!canConfirmImport(report)) {
    return report.skippedExisting > 0
      ? 'Mọi mục hợp lệ trong tệp đã có trong hệ thống nên không có gì để thêm.'
      : 'Không có mục hợp lệ nào để nhập.';
  }
  return report.invalidRows > 0
    ? 'Các dòng lỗi sẽ bị bỏ qua, các mục mới hợp lệ vẫn được thêm. Mục đã có giữ nguyên.'
    : 'Tệp hợp lệ, có thể thêm vào hệ thống. Mục đã có giữ nguyên.';
}

// ===== Danh sách học liệu =====

/** Cột của bảng danh sách theo loại; key trùng tên trường API trả về. */
export const LIST_COLUMNS = {
  kanji: [
    { key: 'character', label: 'Kanji', className: 'text-2xl font-semibold' },
    { key: 'hanViet', label: 'Hán Việt' },
    { key: 'onyomi', label: 'Onyomi' },
    { key: 'kunyomi', label: 'Kunyomi' },
    { key: 'meaning', label: 'Nghĩa' },
    { key: 'strokes', label: 'Số nét' },
    { key: 'jlpt', label: 'JLPT' }
  ],
  vocabulary: [
    { key: 'word', label: 'Từ vựng', className: 'text-lg font-semibold' },
    { key: 'reading', label: 'Cách đọc' },
    { key: 'hanViet', label: 'Hán Việt' },
    { key: 'partOfSpeech', label: 'Từ loại' },
    { key: 'meaning', label: 'Nghĩa' },
    { key: 'jlpt', label: 'JLPT' }
  ],
  grammar: [
    { key: 'pattern', label: 'Mẫu ngữ pháp', className: 'text-lg font-semibold' },
    { key: 'code', label: 'Mã' },
    { key: 'romaji', label: 'Romaji' },
    { key: 'meaning', label: 'Ý nghĩa' },
    { key: 'jlpt', label: 'JLPT' }
  ]
};

/** Trường hiển thị trong hộp thoại chi tiết một mục. */
export const DETAIL_FIELDS = {
  kanji: [
    ['character', 'Kanji'], ['hanViet', 'Hán Việt'], ['onyomi', 'Onyomi'], ['kunyomi', 'Kunyomi'],
    ['meaning', 'Nghĩa tiếng Việt'], ['strokes', 'Số nét'], ['jlpt', 'Cấp độ JLPT'], ['createdAt', 'Ngày thêm']
  ],
  vocabulary: [
    ['word', 'Từ vựng'], ['reading', 'Cách đọc'], ['romaji', 'Romaji'], ['hanViet', 'Âm Hán Việt'],
    ['partOfSpeech', 'Từ loại'], ['meaning', 'Nghĩa tiếng Việt'], ['exampleSentence', 'Câu ví dụ'],
    ['exampleFurigana', 'Câu ví dụ Furigana'], ['exampleMeaning', 'Dịch nghĩa câu ví dụ'],
    ['jmdictId', 'Mã JMdict'], ['jlpt', 'Cấp độ JLPT'], ['createdAt', 'Ngày thêm']
  ],
  grammar: [
    ['pattern', 'Mẫu ngữ pháp'], ['code', 'Mã định danh'], ['romaji', 'Romaji'], ['meaning', 'Ý nghĩa'],
    ['formation', 'Công thức kết hợp'], ['notes', 'Giải thích & ghi chú'], ['exampleSentence', 'Câu ví dụ'],
    ['exampleFurigana', 'Câu ví dụ Furigana'], ['exampleMeaning', 'Dịch nghĩa câu ví dụ'],
    ['extendedExamples', 'Ví dụ mở rộng'], ['tags', 'Thẻ phân loại'], ['jlpt', 'Cấp độ JLPT'], ['createdAt', 'Ngày thêm']
  ]
};

export const JLPT_FILTERS = ['', 'N5', 'N4', 'N3', 'N2', 'N1'];

/** dd/MM/yyyy HH:mm theo giờ máy người dùng; trống → "—". */
export function formatDateTime(value) {
  if (!value) return '—';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return '—';
  const pad = (n) => String(n).padStart(2, '0');
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/** Giá trị một ô: ngày thì định dạng, trống thì "—". */
export function displayValue(key, value) {
  if (key === 'createdAt') return formatDateTime(value);
  if (value === null || value === undefined || String(value).trim() === '') return '—';
  return String(value);
}

/** Tóm tắt một lần nhập trong lịch sử. */
export function historySummary(item) {
  if (!item) return '';
  const parts = [`Thêm ${formatCount(item.created)} mục mới`];
  if (item.skippedExisting) parts.push(`${formatCount(item.skippedExisting)} mục đã có`);
  if (item.invalidRows) parts.push(`${formatCount(item.invalidRows)} dòng lỗi`);
  return parts.join(' · ');
}
