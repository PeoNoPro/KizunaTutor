/**
 * KizunaTutor - Trang Chi Tiáº¿t Bá»™ Tháº» Flashcard Äá»™c Láº­p
 * Master-Detail Route Architecture: /learner/deck-detail?id=... (hoáº·c /flashcards-detail?id=...)
 * REST API Client (Zero-Reload) & TÃ­ch Há»£p KizunaToast
 */

import { debounce } from '/js/main.js';
import { voicevoxClient } from '/js/voicevoxClient.js';
import { apiClient, KizunaToast } from '/js/apiClient.js';
import { initLearnerShell } from '/components/learner-layout/learner-layout.js';

// 1. Khá»Ÿi táº¡o Shell há»c viÃªn
initLearnerShell({
  activeMenu: 'flashcards',
  pageTitle: 'Chi Tiáº¿t Bá»™ Tháº» Flashcard',
  badge: 'Bá»™ nhá»› siÃªu tá»‘c'
});

let currentDeck = null;
let currentDeckId = null;
let currentCards = [];
let editingCardIndex = -1;

// 2. Tiá»‡n Ã­ch An ToÃ n Chuá»—i
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

// 4. Táº£i dá»¯ liá»‡u bá»™ tháº»
async function loadDeckDetail() {
  const urlParams = new URLSearchParams(window.location.search);
  currentDeckId = urlParams.get('id');

  if (!currentDeckId) {
    // Thá»­ láº¥y tá»« pathname náº¿u theo cáº¥u trÃºc /learner/flashcards/:id
    const parts = window.location.pathname.split('/').filter(Boolean);
    if (parts.length > 0 && !parts[parts.length - 1].includes('detail')) {
      currentDeckId = parts[parts.length - 1];
    }
  }

  if (!currentDeckId) {
    KizunaToast.error('KhÃ´ng tÃ¬m tháº¥y mÃ£ Ä‘á»‹nh danh bá»™ tháº»!');
    setTimeout(() => { window.location.href = '/learner/flashcards'; }, 1200);
    return;
  }

  try {
    currentDeck = await apiClient.getDeck(currentDeckId);
  } catch (err) {
    console.debug('Lá»—i táº£i chi tiáº¿t bá»™ tháº»:', err);
  }

  if (!currentDeck) {
    KizunaToast.error('KhÃ´ng tÃ¬m tháº¥y bá»™ tháº» tÆ°Æ¡ng á»©ng!');
    setTimeout(() => { window.location.href = '/learner/flashcards'; }, 1200);
    return;
  }

  currentCards = currentDeck.cards || [];
  renderOverview();
  renderCardsList();

  // Kiá»ƒm tra náº¿u cÃ³ param ?edit=1 thÃ¬ má»Ÿ popup sá»­a
  if (urlParams.get('edit') === '1') {
    openEditDeckModal();
  }
}

// 5. Render ThÃ´ng Tin Tá»•ng Quan
function renderOverview() {
  if (!currentDeck) return;
  document.getElementById('overviewTitle').textContent = currentDeck.title || 'ChÆ°a Ä‘áº·t tÃªn';
  document.getElementById('overviewDesc').textContent = currentDeck.description || 'ChÆ°a cÃ³ ghi chÃº mÃ´ táº£ cho bá»™ tháº» nÃ y.';
  document.getElementById('badgeCardCount').textContent = `${currentCards.length} tháº»`;
}

// 6. Render Danh SÃ¡ch Tháº» Con
function renderCardsList(filterQuery = '') {
  const container = document.getElementById('detailListContainer');
  const emptyState = document.getElementById('emptyCardsState');
  const countBadge = document.getElementById('filteredCountBadge');

  if (!container || !emptyState) return;

  const q = (filterQuery || '').trim().toLowerCase();
  const filtered = currentCards.filter(c => 
    String(c.char || '').toLowerCase().includes(q) ||
    String(c.meaning || '').toLowerCase().includes(q) ||
    String(c.hanViet || '').toLowerCase().includes(q)
  );

  countBadge.textContent = `(${filtered.length} / ${currentCards.length} tháº»)`;

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

    return `
      <div class="p-3 bg-white border border-[#0F172A] rounded-[4px] shadow-[2px_2px_0px_0px_#0F172A] hover:bg-slate-50 transition flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 group">
        <!-- Cá»™t trÆ°á»›c: Tá»« vá»±ng / CÃ¢u há»i -->
        <div class="flex items-center gap-3 flex-1 min-w-0">
          <span class="w-6 h-6 rounded bg-slate-100 border border-slate-300 flex items-center justify-center text-[10px] font-mono text-slate-500 font-bold shrink-0">
            ${originalIdx + 1}
          </span>
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2">
              <span class="font-japanese font-bold text-base text-[#0F172A] truncate" title="${escapeHtml(c.char)}">
                ${escapeHtml(c.char)}
              </span>
            </div>
            ${c.hanViet ? `<span class="text-[11px] font-semibold text-[#006194] uppercase tracking-wider">${escapeHtml(c.hanViet)}</span>` : ''}
          </div>
        </div>

        <!-- Cá»™t sau: NghÄ©a / ÄÃ¡p Ã¡n -->
        <div class="flex-1 min-w-0 text-xs text-[#334155] sm:border-l sm:border-slate-200 sm:pl-3">
          <span class="font-medium leading-relaxed">${escapeHtml(c.meaning || 'â€”')}</span>
        </div>

        <!-- Cá»¥m nÃºt thao tÃ¡c tháº»: PhÃ¡t Ã¢m [ ðŸ”Š ], Luyá»‡n viáº¿t, Sá»­a [ âœï¸ ], XÃ³a [ ðŸ—‘ï¸ ] -->
        <div class="flex items-center gap-1.5 shrink-0 self-end sm:self-center">
          <button type="button" data-action="speak-card" data-text="${escapeHtml(c.char)}"
                  class="w-7 h-7 flex items-center justify-center rounded border border-sky-300 bg-sky-50 text-sky-700 hover:bg-sky-100 cursor-pointer text-xs"
                  title="PhÃ¡t Ã¢m tá»« vá»±ng">
            ðŸ”Š
          </button>
          ${isKanji ? `
            <a href="/learner/practice-canvas?char=${encodeURIComponent(c.char)}" 
               class="py-1 px-2 text-[11px] font-bold rounded border border-pink-300 bg-[#FFE5EC] text-[#831843] hover:bg-pink-100 flex items-center gap-1 transition no-underline"
               title="Luyá»‡n viáº¿t chá»¯ nÃ y trÃªn Canvas Shodo">
              <span>ðŸ–Œï¸</span>
              <span>Luyá»‡n viáº¿t</span>
            </a>
          ` : ''}
          <button type="button" data-action="edit-card" data-index="${originalIdx}"
                  class="w-7 h-7 flex items-center justify-center rounded border border-slate-300 text-slate-700 hover:bg-slate-100 cursor-pointer font-bold text-xs"
                  title="Chá»‰nh sá»­a tháº» nÃ y">
            âœï¸
          </button>
          <button type="button" data-action="delete-card" data-index="${originalIdx}"
                  class="w-7 h-7 flex items-center justify-center rounded border border-rose-200 text-rose-600 hover:bg-rose-50 cursor-pointer font-bold text-xs"
                  title="XÃ³a tháº» nÃ y khá»i bá»™">
            ðŸ—‘ï¸
          </button>
        </div>
      </div>
    `;
  }).join('');

  // Gáº¯n sá»± kiá»‡n phÃ¡t Ã¢m
  container.querySelectorAll('[data-action="speak-card"]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      speakWord(btn.dataset.text);
    });
  });

  // Gáº¯n sá»± kiá»‡n sá»­a tháº» láº»
  container.querySelectorAll('[data-action="edit-card"]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      const idx = parseInt(btn.dataset.index, 10);
      openEditCardModal(idx);
    });
  });

  // Gáº¯n sá»± kiá»‡n xÃ³a tháº» láº»
  container.querySelectorAll('[data-action="delete-card"]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      const idx = parseInt(btn.dataset.index, 10);
      deleteSingleCard(idx);
    });
  });
}

// 7. XÃ³a 1 tháº» láº» qua API
async function deleteSingleCard(index) {
  if (index < 0 || index >= currentCards.length) return;
  const target = currentCards[index];
  if (!confirm(`Báº¡n cÃ³ cháº¯c muá»‘n xÃ³a tháº» "${target?.char}" khá»i bá»™ khÃ´ng?`)) return;

  const removed = currentCards.splice(index, 1);
  try {
    await apiClient.deleteCard(currentDeckId, index);
    KizunaToast.info(`ÄÃ£ xÃ³a tháº» [${removed[0]?.char}] khá»i bá»™.`);
  } catch (err) {
    console.error(err);
    KizunaToast.error('KhÃ´ng thá»ƒ xÃ³a tháº» qua API!');
  }

  renderOverview();
  renderCardsList(document.getElementById('internalSearchInput')?.value || '');
}

// 8. ThÃªm tháº» má»›i thá»§ cÃ´ng
async function submitNewCard() {
  const charInput = document.getElementById('inputNewChar');
  const meaningInput = document.getElementById('inputNewMeaning');
  const char = (charInput.value || '').trim();
  const meaning = (meaningInput.value || '').trim();

  if (!char) {
    KizunaToast.warning('Vui lÃ²ng nháº­p ná»™i dung cÃ¢u há»i / tá»« vá»±ng máº·t trÆ°á»›c.');
    charInput.focus();
    return;
  }

  const newCard = {
    char: char,
    hanViet: '',
    meaning: meaning || 'â€”',
    onyomi: 'â€”',
    kunyomi: 'â€”',
    jlpt: 'Tá»± do'
  };

  currentCards.push(newCard);

  try {
    await apiClient.addCards(currentDeckId, [newCard]);
    KizunaToast.success(`ÄÃ£ thÃªm tháº» [${char}] vÃ o bá»™!`);
  } catch (err) {
    console.error(err);
    KizunaToast.error('KhÃ´ng thá»ƒ lÆ°u tháº» má»›i qua API!');
  }

  closeAddCardModal();
  renderOverview();
  renderCardsList();
}

function openAddCardModal() {
  document.getElementById('inputNewChar').value = '';
  document.getElementById('inputNewMeaning').value = '';
  document.getElementById('modalAddCard').classList.remove('hidden');
  document.getElementById('inputNewChar').focus();
}

function closeAddCardModal() {
  document.getElementById('modalAddCard').classList.add('hidden');
}

// 9. Sá»­a tháº» láº»
function openEditCardModal(index) {
  if (index < 0 || index >= currentCards.length) return;
  editingCardIndex = index;
  const card = currentCards[index];

  document.getElementById('inputEditChar').value = card.char || '';
  document.getElementById('inputEditMeaning').value = card.meaning || '';
  document.getElementById('modalEditCard').classList.remove('hidden');
  document.getElementById('inputEditChar').focus();
}

function closeEditCardModal() {
  document.getElementById('modalEditCard').classList.add('hidden');
  editingCardIndex = -1;
}

async function saveEditCard() {
  if (editingCardIndex < 0 || editingCardIndex >= currentCards.length) return;
  const char = (document.getElementById('inputEditChar').value || '').trim();
  const meaning = (document.getElementById('inputEditMeaning').value || '').trim();

  if (!char) {
    KizunaToast.warning('Vui lÃ²ng nháº­p ná»™i dung máº·t trÆ°á»›c.');
    return;
  }

  currentCards[editingCardIndex].char = char;
  currentCards[editingCardIndex].meaning = meaning || 'â€”';

  try {
    await apiClient.updateDeck(currentDeckId, { cards: currentCards });
    KizunaToast.success('ÄÃ£ cáº­p nháº­t thÃ´ng tin tháº»!');
  } catch (err) {
    console.error(err);
    KizunaToast.error('Lá»—i khi cáº­p nháº­t tháº»!');
  }

  closeEditCardModal();
  renderOverview();
  renderCardsList(document.getElementById('internalSearchInput')?.value || '');
}

// 10. Chá»‰nh sá»­a thÃ´ng tin bá»™ tháº» (TÃªn & MÃ´ táº£)
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
    KizunaToast.warning('Vui lÃ²ng nháº­p tÃªn bá»™ tháº»!');
    return;
  }

  currentDeck.title = title;
  currentDeck.description = desc;

  try {
    await apiClient.updateDeck(currentDeckId, { title, description: desc });
    KizunaToast.success(`ÄÃ£ cáº­p nháº­t bá»™ tháº» "${title}"!`);
  } catch (err) {
    console.error(err);
    KizunaToast.error('Lá»—i khi cáº­p nháº­t thÃ´ng tin bá»™ tháº»!');
  }

  renderOverview();
  closeEditDeckModal();
}

// 11. Äá»c file Excel 2 cá»™t (Cá»™t 1: CÃ¢u há»i / Tá»« vá»±ng, Cá»™t 2: ÄÃ¡p Ã¡n / NghÄ©a)
function handleExcelImport(event) {
  const file = event.target.files[0];
  if (!file) return;

  if (typeof XLSX === 'undefined') {
    KizunaToast.error('ThÆ° viá»‡n Excel Ä‘ang táº£i, vui lÃ²ng thá»­ láº¡i sau giÃ¢y lÃ¡t!');
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
        if (!row || row.length < 2) return;
        const col1 = String(row[0] || '').trim();
        const col2 = String(row[1] || '').trim();

        // Bá» qua dÃ²ng tiÃªu Ä‘á» náº¿u chá»©a tá»« khÃ³a
        const lowerCol1 = col1.toLowerCase();
        if (index === 0 && (
          lowerCol1.includes('cÃ¢u há»i') || 
          lowerCol1.includes('máº·t trÆ°á»›c') || 
          lowerCol1.includes('question') || 
          lowerCol1.includes('front') || 
          lowerCol1.includes('thuáº­t ngá»¯') || 
          lowerCol1.includes('tá»« vá»±ng')
        )) {
          return;
        }

        if (col1) {
          newCards.push({
            char: col1,
            hanViet: '',
            meaning: col2 || 'â€”',
            onyomi: 'â€”',
            kunyomi: 'â€”',
            jlpt: 'Excel'
          });
        }
      });

      if (newCards.length === 0) {
        KizunaToast.warning('KhÃ´ng tÃ¬m tháº¥y dá»¯ liá»‡u há»£p lá»‡! Äáº£m báº£o Cá»™t 1 lÃ  CÃ¢u há»i, Cá»™t 2 lÃ  ÄÃ¡p Ã¡n.');
        return;
      }

      currentCards.push(...newCards);
      await apiClient.addCards(currentDeckId, newCards);
      closeAddCardModal();
      renderOverview();
      renderCardsList();
      KizunaToast.success(`ÄÃ£ náº¡p thÃ nh cÃ´ng ${newCards.length} tháº» tá»« file Excel!`);
    } catch (err) {
      console.error(err);
      KizunaToast.error('Lá»—i khi Ä‘á»c file Excel. Vui lÃ²ng kiá»ƒm tra láº¡i Ä‘á»‹nh dáº¡ng file!');
    } finally {
      event.target.value = '';
    }
  };
  reader.readAsArrayBuffer(file);
}

function downloadSampleExcel() {
  if (typeof XLSX === 'undefined') {
    KizunaToast.error('ThÆ° viá»‡n Excel chÆ°a sáºµn sÃ ng, vui lÃ²ng thá»­ láº¡i sau!');
    return;
  }
  const sampleData = [
    ["CÃ¢u há»i / Tá»« vá»±ng (Máº·t trÆ°á»›c)", "ÄÃ¡p Ã¡n / NghÄ©a (Máº·t sau)"],
    ["æ¡œ (ã•ãã‚‰)", "Hoa anh Ä‘Ã o - Biá»ƒu tÆ°á»£ng cá»§a mÃ¹a xuÃ¢n Nháº­t Báº£n"],
    ["å‹‰å¼· (ã¹ã‚“ãã‚‡ã†)", "Há»c táº­p, rÃ¨n luyá»‡n kiáº¿n thá»©c"],
    ["å…ˆç”Ÿ (ã›ã‚“ã›ã„)", "Tháº§y cÃ´ giÃ¡o, giÃ¡o viÃªn"],
    ["æ„› (ã‚ã„)", "TÃ¬nh yÃªu thÆ°Æ¡ng, Ã¡i tÃ¬nh, trÃ¢n quÃ½"],
    ["ã‚ã‚ŠãŒã¨ã†ã”ã–ã„ã¾ã™", "Cáº£m Æ¡n báº¡n ráº¥t nhiá»u (lá»‹ch sá»±)"],
    ["ã“ã‚“ã«ã¡ã¯", "Xin chÃ o (lá»i chÃ o ban ngÃ y)"]
  ];
  const ws = XLSX.utils.aoa_to_sheet(sampleData);
  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, "Flashcards");
  XLSX.writeFile(wb, "mau-the-kizuna-2cot.xlsx");
  KizunaToast.success('ÄÃ£ táº£i xuá»‘ng file Excel máº«u 2 cá»™t!');
}

// 12. In bá»™ tháº» (Print Mode A4 / Xuáº¥t PDF 2 máº·t)
function printCurrentDeck() {
  if (!currentDeck || currentCards.length === 0) {
    KizunaToast.warning('Bá»™ tháº» nÃ y chÆ°a cÃ³ tháº» há»c nÃ o Ä‘á»ƒ in!');
    return;
  }

  const printWindow = window.open('', '_blank');
  if (!printWindow) {
    KizunaToast.error('Vui lÃ²ng cho phÃ©p popup Ä‘á»ƒ má»Ÿ trang in áº¥n.');
    return;
  }

  const printHtml = `
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="utf-8">
  <title>In Bá»™ Tháº» - ${escapeHtml(currentDeck.title)}</title>
  <style>
    @page { size: A4; margin: 12mm; }
    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; color: #0F172A; background: #FFF; margin: 0; padding: 0; }
    .toolbar { padding: 10px 14px; background: #F1F5F9; margin-bottom: 16px; border-radius: 6px; display: flex; gap: 10px; }
    .btn { padding: 7px 16px; background: #006194; color: white; border: 1px solid #0F172A; border-radius: 4px; font-weight: bold; font-size: 12px; cursor: pointer; }
    .print-header { border-bottom: 2px solid #0F172A; padding-bottom: 8px; margin-bottom: 16px; display: flex; justify-content: space-between; align-items: flex-end; }
    .print-header h1 { margin: 0; font-size: 18px; font-weight: 800; }
    .print-header p { margin: 4px 0 0; font-size: 12px; color: #64748B; }
    .print-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
    .print-card { border: 1.5px dashed #0F172A; border-radius: 6px; padding: 12px 14px; page-break-inside: avoid; display: flex; flex-direction: column; justify-content: space-between; min-height: 110px; background: #FAFAFA; box-sizing: border-box; }
    .card-front { font-size: 16px; font-weight: bold; color: #0F2545; margin-bottom: 6px; border-bottom: 1px solid #E2E8F0; padding-bottom: 6px; }
    .card-back { font-size: 12px; color: #334155; line-height: 1.4; }
    .card-footer { display: flex; justify-content: space-between; font-size: 10px; color: #94A3B8; margin-top: 8px; padding-top: 4px; }
    @media print { .no-print { display: none !important; } body { background: white; } }
  </style>
</head>
<body>
  <div class="toolbar no-print">
    <button class="btn" onclick="window.print()">ðŸ–¨ï¸ In Ngay (Print / LÆ°u PDF)</button>
    <button class="btn" style="background:#64748B;" onclick="window.close()">ÄÃ³ng Cá»­a Sá»•</button>
  </div>
  <div class="print-header">
    <div>
      <h1>ðŸŒ¸ ${escapeHtml(currentDeck.title)}</h1>
      <p>${escapeHtml(currentDeck.description || 'Bá»™ tháº» Kizuna Flashcards')} â€¢ Tá»•ng sá»‘: ${currentCards.length} tháº»</p>
    </div>
    <div style="font-size: 11px; font-weight: bold; color: #64748B;">KizunaTutor Flashcards</div>
  </div>
  <div class="print-grid">
    ${currentCards.map((c, i) => `
      <div class="print-card">
        <div>
          <div class="card-front">${escapeHtml(c.char || '')}</div>
          <div class="card-back">${escapeHtml(c.meaning || c.hanViet || '')}</div>
        </div>
        <div class="card-footer">
          <span>Kizuna Flashcards</span>
          <span>#${i + 1}</span>
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
// 13. KHá»žI Táº O DOM & Láº®NG NGHE Sá»° KIá»†N (DOM READY)
// =========================================================================
document.addEventListener('DOMContentLoaded', () => {
  loadDeckDetail();

  // NÃºt quay láº¡i danh sÃ¡ch
  document.getElementById('btnBackToList')?.addEventListener('click', () => {
    window.location.href = '/learner/flashcards';
  });

  // Thanh cÃ´ng cá»¥ tÃ¡c vá»¥
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

  // TÃ¬m kiáº¿m ná»™i bá»™
  const searchInput = document.getElementById('internalSearchInput');
  if (searchInput) {
    searchInput.addEventListener('input', debounce((e) => {
      renderCardsList(e.target.value);
    }, 200));
  }

  // ThÃªm tháº» thá»§ cÃ´ng
  document.getElementById('btnOpenAddCardModal')?.addEventListener('click', openAddCardModal);
  document.getElementById('btnEmptyAddCard')?.addEventListener('click', openAddCardModal);
  document.getElementById('btnCloseAddCardModal')?.addEventListener('click', closeAddCardModal);
  document.getElementById('btnCancelAddCard')?.addEventListener('click', closeAddCardModal);
  document.getElementById('btnSubmitAddCard')?.addEventListener('click', submitNewCard);

  // Import Excel tá»« trang chi tiáº¿t
  document.getElementById('btnTriggerDetailExcel')?.addEventListener('click', () => {
    document.getElementById('inputDetailExcel')?.click();
  });
  document.getElementById('inputDetailExcel')?.addEventListener('change', handleExcelImport);
  document.getElementById('btnQuickExcelImport')?.addEventListener('click', () => {
    document.getElementById('inputDetailExcel')?.click();
  });
  document.getElementById('btnDownloadSample')?.addEventListener('click', downloadSampleExcel);

  // Sá»­a tháº» con láº»
  document.getElementById('btnCloseEditCardModal')?.addEventListener('click', closeEditCardModal);
  document.getElementById('btnCancelEditCard')?.addEventListener('click', closeEditCardModal);
  document.getElementById('btnSaveEditCard')?.addEventListener('click', saveEditCard);

  // Sá»­a thÃ´ng tin bá»™ tháº»
  document.getElementById('btnCloseEditDeckModal')?.addEventListener('click', closeEditDeckModal);
  document.getElementById('btnCancelEditDeck')?.addEventListener('click', closeEditDeckModal);
  document.getElementById('btnSaveEditDeck')?.addEventListener('click', saveEditDeckInfo);
});
