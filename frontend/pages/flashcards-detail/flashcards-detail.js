/**
 * KizunaTutor - Trang Chi Tiết Bộ Thẻ Flashcard Độc Lập
 * Master-Detail Route Architecture: /learner/deck-detail?id=... (hoặc /flashcards-detail?id=...)
 * REST API Client (Zero-Reload) & Tích Hợp KizunaToast
 */

import { debounce } from '/js/main.js';
import { voicevoxClient } from '/js/voicevoxClient.js';
import { apiClient, KizunaToast } from '/js/apiClient.js';
import { initLearnerShell } from '/components/learner-layout/learner-layout.js';

// 1. Khởi tạo Shell học viên
initLearnerShell({
  activeMenu: 'flashcards',
  pageTitle: 'Chi Tiết Bộ Thẻ Flashcard',
  badge: 'Bộ nhớ siêu tốc'
});

let currentDeck = null;
let currentDeckId = null;
let currentCards = [];
let editingCardIndex = -1;

// 2. Tiện ích An Toàn Chuỗi
function escapeHtml(text) {
  if (text === null || text === undefined) return '';
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

// 3. Phát âm từ vựng tiếng Nhật (VOICEVOX AI & Web Speech API)
function speakWord(text) {
  if (!text) return;
  voicevoxClient.speak(text);
}

// 4. Tải dữ liệu bộ thẻ
async function loadDeckDetail() {
  const urlParams = new URLSearchParams(window.location.search);
  currentDeckId = urlParams.get('id');

  if (!currentDeckId) {
    // Thử lấy từ pathname nếu theo cấu trúc /learner/flashcards/:id
    const parts = window.location.pathname.split('/').filter(Boolean);
    if (parts.length > 0 && !parts[parts.length - 1].includes('detail')) {
      currentDeckId = parts[parts.length - 1];
    }
  }

  if (!currentDeckId) {
    KizunaToast.error('Không tìm thấy mã định danh bộ thẻ!');
    setTimeout(() => { window.location.href = '/learner/flashcards'; }, 1200);
    return;
  }

  try {
    currentDeck = await apiClient.getDeck(currentDeckId);
  } catch (err) {
    console.debug('Lỗi tải chi tiết bộ thẻ:', err);
  }

  if (!currentDeck) {
    KizunaToast.error('Không tìm thấy bộ thẻ tương ứng!');
    setTimeout(() => { window.location.href = '/learner/flashcards'; }, 1200);
    return;
  }

  currentCards = currentDeck.cards || [];
  renderOverview();
  renderCardsList();

  // Kiểm tra nếu có param ?edit=1 thì mở popup sửa
  if (urlParams.get('edit') === '1') {
    openEditDeckModal();
  }
}

// 5. Render Thông Tin Tổng Quan
function renderOverview() {
  if (!currentDeck) return;
  document.getElementById('overviewTitle').textContent = currentDeck.title || 'Chưa đặt tên';
  document.getElementById('overviewDesc').textContent = currentDeck.description || 'Chưa có ghi chú mô tả cho bộ thẻ này.';
  document.getElementById('badgeCardCount').textContent = `${currentCards.length} thẻ`;
}

// 6. Render Danh Sách Thẻ Con
function renderCardsList(filterQuery = '') {
  const container = document.getElementById('detailListContainer');
  const emptyState = document.getElementById('emptyCardsState');
  const countBadge = document.getElementById('filteredCountBadge');

  if (!container || !emptyState) return;

  const q = (filterQuery || '').trim().toLowerCase();
  const filtered = currentCards.filter(c => 
    String(c.char || '').toLowerCase().includes(q) ||
    String(c.meaning || '').toLowerCase().includes(q) ||
    String(c.hanViet || '').toLowerCase().includes(q) ||
    String(c.onyomi || '').toLowerCase().includes(q) ||
    String(c.kunyomi || '').toLowerCase().includes(q)
  );

  countBadge.textContent = `(${filtered.length} / ${currentCards.length} thẻ)`;

  if (filtered.length === 0) {
    container.innerHTML = '';
    container.classList.add('hidden');
    emptyState.classList.remove('hidden');
    return;
  }

  emptyState.classList.add('hidden');
  container.classList.remove('hidden');

  container.innerHTML = filtered.map((c) => {
    const originalIdx = currentCards.indexOf(c);
    const isKanji = (c.char && c.char.length === 1 && !c.char.match(/[a-zA-Z0-9\s]/));
    const hasOn = c.onyomi && c.onyomi !== '—';
    const hasKun = c.kunyomi && c.kunyomi !== '—';

    return `
      <div class="p-3 bg-white border border-[#E2E8F0] hover:border-[#CBD5E1] rounded-xl transition flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 group">
        <!-- Cột trước: Từ vựng / Câu hỏi -->
        <div class="flex items-center gap-3 flex-1 min-w-0">
          <span class="w-6 h-6 rounded bg-slate-100 border border-[#E2E8F0] flex items-center justify-center text-[10px] font-mono text-slate-500 font-bold shrink-0">
            ${originalIdx + 1}
          </span>
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2">
              <span class="font-japanese font-bold text-base text-[#0F172A] truncate" title="${escapeHtml(c.char)}">
                ${escapeHtml(c.char)}
              </span>
              ${c.jlpt ? `<span class="px-1.5 py-0.5 rounded text-[10px] font-bold bg-[#FFE5EC] text-[#831843] border border-[#FBCFE8]">${escapeHtml(c.jlpt)}</span>` : ''}
            </div>
            ${c.hanViet ? `<div class="text-[11px] font-semibold text-[#831843] uppercase tracking-wider">${escapeHtml(c.hanViet)}</div>` : ''}
            ${(hasOn || hasKun) ? `
              <div class="text-[10px] text-slate-500 flex flex-wrap gap-2 mt-0.5 font-japanese">
                ${hasOn ? `<span class="text-sky-700 bg-sky-50 px-1 rounded border border-sky-200">On: ${escapeHtml(c.onyomi)}</span>` : ''}
                ${hasKun ? `<span class="text-rose-700 bg-rose-50 px-1 rounded border border-rose-200">Kun: ${escapeHtml(c.kunyomi)}</span>` : ''}
              </div>
            ` : ''}
          </div>
        </div>

        <!-- Cột sau: Nghĩa / Đáp án -->
        <div class="flex-1 min-w-0 text-xs text-[#334155] sm:border-l sm:border-[#E2E8F0] sm:pl-3">
          <span class="font-medium leading-relaxed">${escapeHtml(c.meaning || '—')}</span>
        </div>

        <!-- Cụm nút thao tác thẻ: Phát âm [ 🔊 ], Luyện viết, Sửa [ ✏️ ], Xóa [ 🗑️ ] -->
        <div class="flex items-center gap-1.5 shrink-0 self-end sm:self-center">
          <button type="button" data-action="speak-card" data-text="${escapeHtml(c.char)}"
                  class="w-7 h-7 flex items-center justify-center rounded-lg border border-[#CBD5E1] bg-white text-slate-700 hover:bg-slate-50 cursor-pointer text-xs transition"
                  title="Phát âm từ vựng">
            🔊
          </button>
          ${isKanji ? `
            <a href="/learner/practice-canvas?char=${encodeURIComponent(c.char)}" 
               class="py-1 px-2 text-[11px] font-semibold rounded-lg border border-[#FBCFE8] bg-[#FFE5EC] text-[#831843] hover:bg-[#FCE7F3] flex items-center gap-1 transition no-underline cursor-pointer"
               title="Luyện viết chữ này trên Canvas Shodo">
              <span>🖌️</span>
              <span>Luyện viết</span>
            </a>
          ` : ''}
          <button type="button" data-action="edit-card" data-index="${originalIdx}"
                  class="w-7 h-7 flex items-center justify-center rounded-lg border border-[#CBD5E1] bg-white text-slate-700 hover:bg-slate-50 cursor-pointer font-semibold text-xs transition"
                  title="Chỉnh sửa thẻ này">
            ✏️
          </button>
          <button type="button" data-action="delete-card" data-index="${originalIdx}"
                  class="w-7 h-7 flex items-center justify-center rounded-lg border border-rose-200 bg-rose-50 text-rose-700 hover:bg-rose-100 cursor-pointer font-semibold text-xs transition"
                  title="Xóa thẻ này khỏi bộ">
            🗑️
          </button>
        </div>
      </div>
    `;
  }).join('');

  // Gắn sự kiện phát âm
  container.querySelectorAll('[data-action="speak-card"]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      speakWord(btn.dataset.text);
    });
  });

  // Gắn sự kiện sửa thẻ lẻ
  container.querySelectorAll('[data-action="edit-card"]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      const idx = parseInt(btn.dataset.index, 10);
      openEditCardModal(idx);
    });
  });

  // Gắn sự kiện xóa thẻ lẻ
  container.querySelectorAll('[data-action="delete-card"]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      const idx = parseInt(btn.dataset.index, 10);
      deleteSingleCard(idx);
    });
  });
}

// 7. Xóa 1 thẻ lẻ qua API
async function deleteSingleCard(index) {
  if (index < 0 || index >= currentCards.length) return;
  const target = currentCards[index];
  if (!confirm(`Bạn có chắc muốn xóa thẻ "${target?.char}" khỏi bộ không?`)) return;

  const removed = currentCards.splice(index, 1);
  try {
    await apiClient.deleteCard(currentDeckId, index);
    KizunaToast.info(`Đã xóa thẻ [${removed[0]?.char}] khỏi bộ.`);
  } catch (err) {
    console.error(err);
    KizunaToast.error('Không thể xóa thẻ qua API!');
  }

  renderOverview();
  renderCardsList(document.getElementById('internalSearchInput')?.value || '');
}

// 8. Thêm thẻ mới thủ công (Hỗ trợ đầy đủ các trường Kanji & JLPT)
async function submitNewCard() {
  const charInput = document.getElementById('inputNewChar');
  const hanVietInput = document.getElementById('inputNewHanViet');
  const onyomiInput = document.getElementById('inputNewOnyomi');
  const kunyomiInput = document.getElementById('inputNewKunyomi');
  const jlptInput = document.getElementById('inputNewJlpt');
  const meaningInput = document.getElementById('inputNewMeaning');

  const char = (charInput?.value || '').trim();
  const hanViet = (hanVietInput?.value || '').trim();
  const onyomi = (onyomiInput?.value || '').trim();
  const kunyomi = (kunyomiInput?.value || '').trim();
  const jlpt = (jlptInput?.value || 'N5').trim();
  const meaning = (meaningInput?.value || '').trim();

  if (!char) {
    KizunaToast.warning('Vui lòng nhập nội dung chữ Hán / từ vựng mặt trước.');
    charInput?.focus();
    return;
  }

  if (!meaning) {
    KizunaToast.warning('Vui lòng nhập giải nghĩa mặt sau.');
    meaningInput?.focus();
    return;
  }

  const newCard = {
    char: char,
    hanViet: hanViet,
    onyomi: onyomi || '—',
    kunyomi: kunyomi || '—',
    meaning: meaning,
    jlpt: jlpt || 'Tự do'
  };

  currentCards.push(newCard);

  try {
    await apiClient.addCards(currentDeckId, [newCard]);
    KizunaToast.success(`Đã thêm thẻ [${char}] vào bộ!`);
  } catch (err) {
    console.error(err);
    KizunaToast.error('Không thể lưu thẻ mới qua API!');
  }

  closeAddCardModal();
  renderOverview();
  renderCardsList();
}

function openAddCardModal() {
  if (document.getElementById('inputNewChar')) document.getElementById('inputNewChar').value = '';
  if (document.getElementById('inputNewHanViet')) document.getElementById('inputNewHanViet').value = '';
  if (document.getElementById('inputNewOnyomi')) document.getElementById('inputNewOnyomi').value = '';
  if (document.getElementById('inputNewKunyomi')) document.getElementById('inputNewKunyomi').value = '';
  if (document.getElementById('inputNewJlpt')) document.getElementById('inputNewJlpt').value = 'N5';
  if (document.getElementById('inputNewMeaning')) document.getElementById('inputNewMeaning').value = '';
  document.getElementById('modalAddCard').classList.remove('hidden');
  document.getElementById('inputNewChar')?.focus();
}

function closeAddCardModal() {
  document.getElementById('modalAddCard').classList.add('hidden');
}

// 9. Sửa thẻ lẻ
function openEditCardModal(index) {
  if (index < 0 || index >= currentCards.length) return;
  editingCardIndex = index;
  const card = currentCards[index];

  if (document.getElementById('inputEditChar')) document.getElementById('inputEditChar').value = card.char || '';
  if (document.getElementById('inputEditHanViet')) document.getElementById('inputEditHanViet').value = card.hanViet || '';
  if (document.getElementById('inputEditOnyomi')) document.getElementById('inputEditOnyomi').value = (card.onyomi && card.onyomi !== '—') ? card.onyomi : '';
  if (document.getElementById('inputEditKunyomi')) document.getElementById('inputEditKunyomi').value = (card.kunyomi && card.kunyomi !== '—') ? card.kunyomi : '';
  if (document.getElementById('inputEditJlpt')) document.getElementById('inputEditJlpt').value = card.jlpt || 'N5';
  if (document.getElementById('inputEditMeaning')) document.getElementById('inputEditMeaning').value = card.meaning || '';

  document.getElementById('modalEditCard').classList.remove('hidden');
  document.getElementById('inputEditChar')?.focus();
}

function closeEditCardModal() {
  document.getElementById('modalEditCard').classList.add('hidden');
  editingCardIndex = -1;
}

async function saveEditCard() {
  if (editingCardIndex < 0 || editingCardIndex >= currentCards.length) return;
  const char = (document.getElementById('inputEditChar')?.value || '').trim();
  const hanViet = (document.getElementById('inputEditHanViet')?.value || '').trim();
  const onyomi = (document.getElementById('inputEditOnyomi')?.value || '').trim();
  const kunyomi = (document.getElementById('inputEditKunyomi')?.value || '').trim();
  const jlpt = (document.getElementById('inputEditJlpt')?.value || 'N5').trim();
  const meaning = (document.getElementById('inputEditMeaning')?.value || '').trim();

  if (!char) {
    KizunaToast.warning('Vui lòng nhập nội dung mặt trước.');
    return;
  }

  currentCards[editingCardIndex].char = char;
  currentCards[editingCardIndex].hanViet = hanViet;
  currentCards[editingCardIndex].onyomi = onyomi || '—';
  currentCards[editingCardIndex].kunyomi = kunyomi || '—';
  currentCards[editingCardIndex].jlpt = jlpt;
  currentCards[editingCardIndex].meaning = meaning || '—';

  try {
    await apiClient.updateDeck(currentDeckId, { cards: currentCards });
    KizunaToast.success('Đã cập nhật thông tin thẻ!');
  } catch (err) {
    console.error(err);
    KizunaToast.error('Không thể lưu thay đổi thẻ qua API!');
  }

  closeEditCardModal();
  renderOverview();
  renderCardsList(document.getElementById('internalSearchInput')?.value || '');
}

// 10. Chỉnh sửa thông tin bộ thẻ (Tên & Mô tả)
function openEditDeckModal() {
  if (!currentDeck) return;
  document.getElementById('inputEditDeckName').value = currentDeck.title || '';
  document.getElementById('inputEditDeckDesc').value = currentDeck.description || '';
  document.getElementById('modalEditDeckInfo').classList.remove('hidden');
  document.getElementById('inputEditDeckName').focus();
}

function closeEditDeckModal() {
  document.getElementById('modalEditDeckInfo').classList.add('hidden');
}

async function saveEditDeckInfo() {
  const title = (document.getElementById('inputEditDeckName').value || '').trim();
  const desc = (document.getElementById('inputEditDeckDesc').value || '').trim();

  if (!title) {
    KizunaToast.warning('Vui lòng nhập tên bộ thẻ!');
    return;
  }

  currentDeck.title = title;
  currentDeck.description = desc;

  try {
    await apiClient.updateDeck(currentDeckId, { title, description: desc });
    KizunaToast.success(`Đã cập nhật bộ thẻ "${title}"!`);
  } catch (err) {
    console.error(err);
    KizunaToast.error('Lỗi khi cập nhật thông tin bộ thẻ!');
  }

  renderOverview();
  closeEditDeckModal();
}

// 11. Đọc file Excel Đa Cột Chuẩn & Tương Thích Ngược 2 Cột
function handleExcelImport(event) {
  const file = event.target.files[0];
  if (!file) return;

  if (typeof XLSX === 'undefined') {
    KizunaToast.error('Thư viện Excel đang tải, vui lòng thử lại sau giây lát!');
    event.target.value = '';
    return;
  }

  const reader = new FileReader();
  reader.onload = async function(e) {
    try {
      const data = new Uint8Array(e.target.result);
      const workbook = XLSX.read(data, { type: 'array' });
      const firstSheetName = workbook.SheetNames[0];
      const worksheet = workbook.Sheets[firstSheetName];
      const rawRows = XLSX.utils.sheet_to_json(worksheet, { header: 1 });
      const newCards = [];

      rawRows.forEach((row, index) => {
        if (!row || row.length === 0) return;
        const col0 = String(row[0] || '').trim();
        if (!col0) return;

        // Bỏ qua dòng tiêu đề nếu chứa từ khóa
        const lower0 = col0.toLowerCase();
        if (index === 0 && (
          lower0.includes('mặt trước') || 
          lower0.includes('câu hỏi') || 
          lower0.includes('chữ hán') || 
          lower0.includes('từ vựng') || 
          lower0.includes('front') || 
          lower0.includes('kanji') ||
          lower0.includes('thuật ngữ')
        )) {
          return;
        }

        // Kiểm tra số cột: Nếu từ 3 cột trở lên => Định dạng đa cột chuẩn
        if (row.length >= 3) {
          const char = col0;
          const hanViet = String(row[1] || '').trim();
          const onyomi = String(row[2] || '').trim();
          const kunyomi = String(row[3] || '').trim();
          const meaning = String(row[4] || '').trim();
          const jlpt = String(row[5] || '').trim();

          newCards.push({
            char: char,
            hanViet: hanViet,
            onyomi: onyomi || '—',
            kunyomi: kunyomi || '—',
            meaning: meaning || '—',
            jlpt: jlpt || 'Tự do'
          });
        } else {
          // Tương thích ngược: Định dạng 2 cột
          const col1 = String(row[1] || '').trim();
          let hanViet = '';
          let onyomi = '—';
          let kunyomi = '—';
          let meaning = col1;

          // Bóc tách nhanh nếu có tiền tố On/Kun/Hán-Việt
          const hvMatch = col1.match(/(?:Hán[- ]?Việt|HV)[:\s]+([^;\n\r]+)/i);
          if (hvMatch) hanViet = hvMatch[1].trim();

          const onMatch = col1.match(/(?:Onyomi|On|Âm On)[:\s]+([^;\n\r]+)/i);
          if (onMatch) onyomi = onMatch[1].trim();

          const kunMatch = col1.match(/(?:Kunyomi|Kun|Âm Kun)[:\s]+([^;\n\r]+)/i);
          if (kunMatch) kunyomi = kunMatch[1].trim();

          newCards.push({
            char: col0,
            hanViet: hanViet,
            onyomi: onyomi,
            kunyomi: kunyomi,
            meaning: meaning || '—',
            jlpt: 'Tự do'
          });
        }
      });

      if (newCards.length === 0) {
        KizunaToast.warning('Không tìm thấy dữ liệu hợp lệ trong file Excel!');
        return;
      }

      currentCards.push(...newCards);
      await apiClient.addCards(currentDeckId, newCards);
      closeAddCardModal();
      renderOverview();
      renderCardsList();
      KizunaToast.success(`Đã nạp thành công ${newCards.length} thẻ từ file Excel!`);
    } catch (err) {
      console.error(err);
      KizunaToast.error('Lỗi khi đọc file Excel. Vui lòng kiểm tra lại định dạng file!');
    } finally {
      event.target.value = '';
    }
  };
  reader.readAsArrayBuffer(file);
}

function downloadSampleExcel() {
  if (typeof XLSX === 'undefined') {
    KizunaToast.error('Thư viện Excel chưa sẵn sàng, vui lòng thử lại sau!');
    return;
  }
  const sampleData = [
    [
      "Mặt trước (Chữ Hán / Từ vựng)*",
      "Âm Hán-Việt",
      "Onyomi (Âm On)",
      "Kunyomi (Âm Kun)",
      "Nghĩa tiếng Việt*",
      "Cấp độ JLPT",
      "Ví dụ / Ghi chú"
    ],
    ["日", "NHẬT", "ニチ, ジツ", "ひ, -び, -か", "Mặt trời, ngày, Nhật Bản", "N5", "日曜日 (Chủ nhật)"],
    ["月", "NGUYỆT", "ゲツ, ガツ", "つき", "Mặt trăng, tháng", "N5", "月曜日 (Thứ hai)"],
    ["火", "HỎA", "カ", "ひ, -び, ほ-", "Lửa, sao Hỏa", "N5", "火曜日 (Thứ ba)"],
    ["水", "THỦY", "スイ", "みず", "Nước", "N5", "水曜日 (Thứ tư)"],
    ["木", "MỘC", "ボク, モク", "き, こ-", "Cây cối, gỗ", "N5", "木曜日 (Thứ năm)"],
    ["金", "KIM", "キン, コン", "かね, かな-", "Vàng, tiền kim loại", "N5", "金曜日 (Thứ sáu)"],
    ["土", "THỔ", "ド, ト", "つち", "Đất đai, thổ nhưỡng", "N5", "土曜日 (Thứ bảy)"],
    ["山", "SƠN", "サン, セン", "やま", "Núi non", "N5", "富士山 (Núi Phú Sĩ)"],
    ["川", "XUYÊN", "セン", "かわ", "Sông suối", "N5", "川 (Con sông)"],
    ["桜", "ANH", "オウ", "さくら", "Hoa anh đào Nhật Bản", "N3", "桜の花 (Hoa anh đào)"]
  ];
  const ws = XLSX.utils.aoa_to_sheet(sampleData);
  ws['!cols'] = [
    { wch: 28 },
    { wch: 15 },
    { wch: 18 },
    { wch: 18 },
    { wch: 32 },
    { wch: 12 },
    { wch: 25 }
  ];
  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, "Flashcards_Kanji");
  XLSX.writeFile(wb, "mau-flashcard-tieng-nhat-chuan.xlsx");
  KizunaToast.success('Đã tải xuống file Excel mẫu đa cột chuẩn tiếng Nhật!');
}

// 12. In bộ thẻ (Print Mode A4 / Xuất PDF 2 mặt)
function printCurrentDeck() {
  if (!currentDeck || currentCards.length === 0) {
    KizunaToast.warning('Bộ thẻ này chưa có thẻ học nào để in!');
    return;
  }

  const printWindow = window.open('', '_blank');
  if (!printWindow) {
    KizunaToast.error('Vui lòng cho phép popup để mở trang in ấn.');
    return;
  }

  const printHtml = `
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <title>In Bộ Thẻ - ${escapeHtml(currentDeck.title || 'Flashcards')}</title>
  <link rel="stylesheet" href="/styles/variables.css">
  <style>
    @page { size: A4; margin: 10mm; }
    body { font-family: 'Be Vietnam Pro', system-ui, sans-serif; margin: 0; padding: 10px; color: #0F172A; }
    .page-header { text-align: center; margin-bottom: 20px; border-bottom: 2px solid #0F172A; padding-bottom: 10px; }
    .grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
    .card { border: 2px solid #0F172A; border-radius: 8px; padding: 16px; page-break-inside: avoid; display: flex; flex-direction: column; justify-content: space-between; min-height: 140px; }
    .front { font-family: 'Shippori Mincho', serif; font-size: 28px; font-weight: bold; text-align: center; margin-bottom: 8px; }
    .hanviet { font-size: 13px; font-weight: bold; color: #831843; text-align: center; margin-bottom: 8px; }
    .details { font-size: 11px; color: #475569; border-top: 1px dashed #CBD5E1; padding-top: 8px; }
    .badge { display: inline-block; padding: 2px 6px; font-size: 10px; font-weight: bold; border-radius: 4px; background: #FFE5EC; color: #831843; }
    @media print {
      .no-print { display: none !important; }
    }
  </style>
</head>
<body>
  <div class="no-print" style="margin-bottom: 15px; display: flex; justify-content: space-between; align-items: center;">
    <div><strong>KizunaTutor</strong> - Xem trước trang in A4</div>
    <button onclick="window.print()" style="padding: 6px 16px; background: #0F172A; color: white; border: none; border-radius: 6px; cursor: pointer; font-weight: bold;">
      🖨️ In Ngay (Print / PDF)
    </button>
  </div>
  <div class="page-header">
    <h2 style="margin: 0 0 5px 0;">${escapeHtml(currentDeck.title)}</h2>
    <p style="margin: 0; font-size: 13px; color: #475569;">${escapeHtml(currentDeck.description || '')} • Tổng cộng ${currentCards.length} thẻ</p>
  </div>
  <div class="grid">
    ${currentCards.map(c => `
      <div class="card">
        <div>
          <div class="front">${escapeHtml(c.char)}</div>
          ${c.hanViet ? `<div class="hanviet">${escapeHtml(c.hanViet)}</div>` : ''}
        </div>
        <div class="details">
          ${c.jlpt ? `<span class="badge">${escapeHtml(c.jlpt)}</span> ` : ''}
          <span style="font-weight: 500;">${escapeHtml(c.meaning || '—')}</span>
          ${(c.onyomi && c.onyomi !== '—') ? `<div style="margin-top: 4px; font-size: 10px; color: #0284c7;">On: ${escapeHtml(c.onyomi)}</div>` : ''}
          ${(c.kunyomi && c.kunyomi !== '—') ? `<div style="font-size: 10px; color: #e11d48;">Kun: ${escapeHtml(c.kunyomi)}</div>` : ''}
        </div>
      </div>
    `).join('')}
  </div>
</body>
</html>
  `;

  printWindow.document.open();
  printWindow.document.write(printHtml);
  printWindow.document.close();
  printWindow.focus();
}

// =========================================================================
// 13. KHỞI TẠO DOM & LẮNG NGHE SỰ KIỆN (DOM READY)
// =========================================================================
document.addEventListener('DOMContentLoaded', () => {
  loadDeckDetail();

  // Nút quay lại danh sách
  document.getElementById('btnBackToList')?.addEventListener('click', () => {
    window.location.href = '/learner/flashcards';
  });

  // Thanh công cụ tác vụ
  document.getElementById('btnStartStudy')?.addEventListener('click', () => {
    if (currentDeckId) {
      window.location.href = `/flashcards-study?id=${encodeURIComponent(currentDeckId)}`;
    }
  });
  document.getElementById('btnStartStudyFromDetail')?.addEventListener('click', () => {
    if (currentDeckId) {
      window.location.href = `/flashcards-study?id=${encodeURIComponent(currentDeckId)}`;
    }
  });
  document.getElementById('btnPlayMatchGame')?.addEventListener('click', () => {
    if (currentDeckId) {
      window.location.href = `/learner/flashcards-match?id=${encodeURIComponent(currentDeckId)}`;
    }
  });
  document.getElementById('btnPlayRainGame')?.addEventListener('click', () => {
    if (currentDeckId) {
      window.location.href = `/kanji-game?deckId=${encodeURIComponent(currentDeckId)}`;
    } else {
      window.location.href = '/kanji-game';
    }
  });
  document.getElementById('btnPrintDeck')?.addEventListener('click', printCurrentDeck);
  document.getElementById('btnEditDeck')?.addEventListener('click', openEditDeckModal);

  // Tìm kiếm nội bộ
  const searchInput = document.getElementById('internalSearchInput');
  if (searchInput) {
    searchInput.addEventListener('input', debounce((e) => {
      renderCardsList(e.target.value);
    }, 200));
  }

  // Thêm thẻ thủ công
  document.getElementById('btnOpenAddCardModal')?.addEventListener('click', openAddCardModal);
  document.getElementById('btnEmptyAddCard')?.addEventListener('click', openAddCardModal);
  document.getElementById('btnCloseAddCardModal')?.addEventListener('click', closeAddCardModal);
  document.getElementById('btnCancelAddCard')?.addEventListener('click', closeAddCardModal);
  document.getElementById('btnSubmitAddCard')?.addEventListener('click', submitNewCard);

  // Import Excel từ trang chi tiết
  document.getElementById('btnTriggerDetailExcel')?.addEventListener('click', () => {
    document.getElementById('inputDetailExcel')?.click();
  });
  document.getElementById('inputDetailExcel')?.addEventListener('change', handleExcelImport);
  document.getElementById('btnQuickExcelImport')?.addEventListener('click', () => {
    document.getElementById('inputDetailExcel')?.click();
  });
  document.getElementById('btnDownloadSample')?.addEventListener('click', downloadSampleExcel);

  // Sửa thẻ con lẻ
  document.getElementById('btnCloseEditCardModal')?.addEventListener('click', closeEditCardModal);
  document.getElementById('btnCancelEditCard')?.addEventListener('click', closeEditCardModal);
  document.getElementById('btnSaveEditCard')?.addEventListener('click', saveEditCard);

  // Sửa thông tin bộ thẻ
  document.getElementById('btnCloseEditDeckModal')?.addEventListener('click', closeEditDeckModal);
  document.getElementById('btnCancelEditDeck')?.addEventListener('click', closeEditDeckModal);
  document.getElementById('btnSaveEditDeck')?.addEventListener('click', saveEditDeckInfo);
});
