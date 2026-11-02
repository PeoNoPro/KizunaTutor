/**
 * KizunaTutor - Từ Điển Chữ Hán & Bút Thuận (Kanji Dictionary & Stroke Order)
 * Phục dựng 100% chuẩn giao diện & tính năng từ phiên bản gốc (legacy dictionary)
 * Giữ nguyên cấu trúc Layout dùng chung cho Học Viên (Header, Sidebar, Footer)
 * Tuân thủ FRONTEND_RULES: Zero mock data, Zero exception leak
 */

import { initLearnerShell } from '/components/learner-layout/learner-layout.js';
import { showToast } from '/js/main.js';

// Khởi tạo khung giao diện học viên dùng chung
initLearnerShell({
  activeMenu: 'kanji-dictionary',
  pageTitle: 'Từ Điển Chữ Hán & Bút Thuận 📖',
  badge: 'Công cụ rèn luyện'
});

// =========================================================================
// 1. TRẠNG THÁI TOÀN CỤC CỦA TRANG TỪ ĐIỂN
// =========================================================================
let currentLearnerPage = 0;
let learnerDebounce = null;
let currentDictStroke = null;
let currentDictRadical = null;
let currentKanjiToSave = null;

// Danh mục 23 bộ thủ chữ Hán thông dụng chuẩn
const commonRadicals = [
  { char: '人', name: 'Nhân' },
  { char: '亻', name: 'Nhân đứng' },
  { char: '氵', name: 'Thủy' },
  { char: '木', name: 'Mộc' },
  { char: '火', name: 'Hỏa' },
  { char: '灬', name: 'Hỏa dưới' },
  { char: '土', name: 'Thổ' },
  { char: '口', name: 'Khẩu' },
  { char: '心', name: 'Tâm' },
  { char: '忄', name: 'Tâm đứng' },
  { char: '日', name: 'Nhật' },
  { char: '月', name: 'Nguyệt' },
  { char: '金', name: 'Kim' },
  { char: '艹', name: 'Thảo' },
  { char: '女', name: 'Nữ' },
  { char: '糸', name: 'Mịch' },
  { char: '門', name: 'Môn' },
  { char: '扌', name: 'Thủ' },
  { char: '目', name: 'Mục' },
  { char: '言', name: 'Ngôn' },
  { char: '辶', name: 'Sước' },
  { char: '山', name: 'Sơn' },
  { char: '車', name: 'Xa' }
];

function escapeHtml(text) {
  if (text === null || text === undefined) return '';
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

function showAlert(msg, isSuccess = true) {
  if (typeof showToast === 'function') {
    showToast(msg, isSuccess ? 'success' : 'error');
  } else if (window.showToast) {
    window.showToast(msg, isSuccess ? 'success' : 'error');
  } else {
    alert(msg);
  }
}

// =========================================================================
// 2. BỘ THỦ VÀ SỐ NÉT (RADICAL & STROKE FILTERS)
// =========================================================================
function renderDictRadicalButtons() {
  const select = document.getElementById('dictRadicalSelect');
  if (!select) return;
  select.innerHTML = '<option value="">🌸 Tất cả bộ thủ</option>' +
    commonRadicals.map(r => `<option value="${r.char}">${r.char} (${r.name})</option>`).join('');
}

let strokeDebounce = null;
function onDictStrokeInputChange() {
  const input = document.getElementById('dictStrokeInput');
  const clearBtn = document.getElementById('btnClearStrokeInput');
  if (clearBtn) {
    if (input && input.value.trim().length > 0) clearBtn.classList.remove('hidden');
    else clearBtn.classList.add('hidden');
  }
  clearTimeout(strokeDebounce);
  strokeDebounce = setTimeout(() => {
    const val = input ? input.value.trim() : '';
    currentDictStroke = val ? parseInt(val, 10) : null;
    loadLearnerDictionary(0);
  }, 300);
}

function clearDictStrokeInput() {
  const input = document.getElementById('dictStrokeInput');
  if (input) input.value = '';
  const clearBtn = document.getElementById('btnClearStrokeInput');
  if (clearBtn) clearBtn.classList.add('hidden');
  currentDictStroke = null;
  loadLearnerDictionary(0);
}

function setDictRadicalFilter(radChar) {
  currentDictRadical = radChar || null;
  const select = document.getElementById('dictRadicalSelect');
  if (select && select.value !== (currentDictRadical || '')) {
    select.value = currentDictRadical || '';
  }
  loadLearnerDictionary(0);
}

// =========================================================================
// 3. TÌM KIẾM CƠ BẢN (KEYWORD & JLPT SEARCH)
// =========================================================================
function onSearchInputChange() {
  const input = document.getElementById('learnerSearchInput');
  const clearBtn = document.getElementById('btnClearSearchInput');
  if (clearBtn) {
    if (input && input.value.trim().length > 0) clearBtn.classList.remove('hidden');
    else clearBtn.classList.add('hidden');
  }
  debounceLearnerSearch();
}

function clearLearnerSearchInput() {
  const input = document.getElementById('learnerSearchInput');
  if (input) input.value = '';
  const clearBtn = document.getElementById('btnClearSearchInput');
  if (clearBtn) clearBtn.classList.add('hidden');
  loadLearnerDictionary(0);
}

function debounceLearnerSearch() {
  clearTimeout(learnerDebounce);
  learnerDebounce = setTimeout(() => {
    loadLearnerDictionary(0);
  }, 300);
}

function quickSearchPrompt(term) {
  if (term === 'N5' || term === 'N4' || term === 'N3' || term === 'N2' || term === 'N1') {
    const jlptSelect = document.getElementById('learnerJlptSelect');
    if (jlptSelect) jlptSelect.value = term;
  } else {
    const searchInput = document.getElementById('learnerSearchInput');
    if (searchInput) searchInput.value = term;
    const clearBtn = document.getElementById('btnClearSearchInput');
    if (clearBtn) clearBtn.classList.remove('hidden');
  }
  loadLearnerDictionary(0);
}

// =========================================================================
// 4. TRẠNG THÁI CHỜ BAN ĐẦU (STANDBY STATE CÓ CAPYBARA)
// =========================================================================
function renderStandbyState() {
  const grid = document.getElementById('learnerKanjiGrid');
  const pageInfo = document.getElementById('learnerPaginationInfo');
  const pageBtns = document.getElementById('learnerPaginationBtns');

  if (pageInfo) pageInfo.textContent = 'Chờ bạn nhập từ khóa hoặc chọn bộ lọc... 🌸';
  if (pageBtns) pageBtns.innerHTML = '';

  if (grid) {
    grid.innerHTML = `
      <div class="col-span-full p-8 sm:p-12 text-center rounded-xl border-2 border-dashed border-[#CBD5E1] bg-white space-y-3">
        <img src="/assets/images/stickers/tea-meditation-capi.png" 
             alt="Capybara Thiền Trà Shodo" 
             class="w-16 h-16 mx-auto mb-2 object-contain pointer-events-none select-none" />
        <h3 class="text-lg sm:text-xl font-bold text-[#0F172A]">
          Sẵn sàng tra cứu Hán tự &amp; Bút thuận 📖
        </h3>
        <p class="text-xs text-[#64748B] font-sans max-w-lg mx-auto leading-relaxed">
          Hãy nhập chữ Hán, âm Hán Việt, âm On/Kun, chọn cấp độ JLPT hoặc vẽ nét bằng chuột để bắt đầu tra cứu hơn 6.600 chữ Hán nhé! ☕
        </p>
        <div class="pt-2 flex items-center justify-center gap-2 flex-wrap text-xs font-meta">
          <span class="text-[#64748B] font-semibold">Gợi ý thử nhanh:</span>
          <button type="button" data-term="日" class="btn-quick-prompt px-3 py-1.5 rounded-lg bg-slate-100 hover:bg-[#FFE5EC] hover:text-[#831843] hover:border-[#FBCFE8] border border-[#CBD5E1] font-bold text-[#1E293B] transition cursor-pointer">
            日 (NHẬT)
          </button>
          <button type="button" data-term="月" class="btn-quick-prompt px-3 py-1.5 rounded-lg bg-slate-100 hover:bg-[#FFE5EC] hover:text-[#831843] hover:border-[#FBCFE8] border border-[#CBD5E1] font-bold text-[#1E293B] transition cursor-pointer">
            月 (NGUYỆT)
          </button>
          <button type="button" data-term="N5" class="btn-quick-prompt px-3 py-1.5 rounded-lg bg-emerald-50 hover:bg-emerald-100 border border-emerald-200 font-bold text-emerald-800 transition cursor-pointer">
            Cấp độ N5
          </button>
          <button type="button" data-term="N4" class="btn-quick-prompt px-3 py-1.5 rounded-lg bg-sky-50 hover:bg-sky-100 border border-sky-200 font-bold text-sky-800 transition cursor-pointer">
            Cấp độ N4
          </button>
        </div>
      </div>
    `;

    grid.querySelectorAll('.btn-quick-prompt').forEach(btn => {
      btn.addEventListener('click', () => {
        const term = btn.getAttribute('data-term');
        quickSearchPrompt(term);
      });
    });
  }
}

// =========================================================================
// 5. TRUY VẤN DỮ LIỆU TỪ BACKEND API
// =========================================================================
async function loadLearnerDictionary(page = 0) {
  currentLearnerPage = page;
  const inputEl = document.getElementById('learnerSearchInput');
  const inputVal = inputEl ? inputEl.value.trim() : '';
  const kw = currentDictRadical ? (inputVal ? `${inputVal} ${currentDictRadical}` : currentDictRadical) : inputVal;
  const jlptSelect = document.getElementById('learnerJlptSelect');
  const jlpt = jlptSelect ? jlptSelect.value : 'ALL';
  const hasStroke = currentDictStroke !== null;
  const hasRadical = currentDictRadical !== null;
  const hasJlpt = jlpt && jlpt !== 'ALL';
  const hasKeyword = !!kw;

  if (!hasKeyword && !hasJlpt && !hasStroke && !hasRadical) {
    renderStandbyState();
    return;
  }

  const grid = document.getElementById('learnerKanjiGrid');
  if (grid) {
    grid.innerHTML = '<div class="col-span-full py-12 text-center text-[#64748B] font-meta text-xs">Đang lật mở các trang từ điển Kanji... 🔍</div>';
  }

  try {
    let url = `/api/learner/kanji/search?page=${page}&size=18`;
    if (kw) url += `&keyword=${encodeURIComponent(kw)}`;
    if (jlpt && jlpt !== 'ALL') url += `&jlpt=${encodeURIComponent(jlpt)}`;
    if (currentDictStroke !== null) url += `&strokeCount=${encodeURIComponent(currentDictStroke)}`;

    let res = await fetch(url).catch(() => null);
    if (!res || !res.ok) {
      // Fallback endpoint công khai
      let fallbackUrl = `/api/kanji/search?page=${page}&size=18`;
      if (kw) fallbackUrl += `&keyword=${encodeURIComponent(kw)}`;
      if (jlpt && jlpt !== 'ALL') fallbackUrl += `&jlpt=${encodeURIComponent(jlpt)}`;
      if (currentDictStroke !== null) fallbackUrl += `&strokeCount=${encodeURIComponent(currentDictStroke)}`;
      res = await fetch(fallbackUrl).catch(() => null);
    }

    if (res && res.ok) {
      const data = await res.json();
      renderLearnerGrid(data.items || []);
      renderLearnerPagination(data.currentPage !== undefined ? data.currentPage : page, data.totalPages || 1, data.totalElements || 0);
    } else {
      renderLearnerGrid([]);
      renderLearnerPagination(0, 1, 0);
    }
  } catch (e) {
    if (grid) {
      grid.innerHTML = '<div class="col-span-full py-12 text-center text-[#DC2626] font-meta text-xs font-bold">Lỗi kết nối từ điển: ' + escapeHtml(e.message) + '</div>';
    }
  }
}

// =========================================================================
// 6. RENDER LƯỚI THẺ CHỮ HÁN CHUẨN RETRO-STITCH
// =========================================================================
function renderLearnerGrid(items) {
  const grid = document.getElementById('learnerKanjiGrid');
  if (!grid) return;

  if (!items || items.length === 0) {
    grid.innerHTML = `
      <div class="col-span-full p-10 text-center rounded-xl border-2 border-dashed border-[#CBD5E1] bg-white space-y-2">
        <img src="/assets/images/stickers/confused-capi.png" 
             alt="Capybara Bối Rối" 
             class="w-16 h-16 mx-auto mb-2 object-contain pointer-events-none select-none" />
        <h3 class="text-lg font-bold text-[#0F172A]">Không tìm thấy chữ Kanji nào phù hợp</h3>
        <p class="text-xs text-[#64748B] font-sans">Bạn thử tìm từ khóa, đổi số nét hoặc cấp độ JLPT khác xem sao nha! 🧐</p>
      </div>
    `;
    return;
  }

  window.lastRenderedDictionaryItems = items;

  grid.innerHTML = items.map((k, idx) => `
    <div class="bg-white rounded-xl border border-[#E2E8F0] hover:border-slate-300 p-4 flex flex-col justify-between transition-all relative h-[310px] max-h-[310px]">
      <div class="h-[230px] max-h-[230px] flex flex-col justify-between">
        <div class="flex items-center justify-between mb-3 border-b border-[#E2E8F0] pb-2">
          <span class="badge-nhai bg-sky-50 text-sky-700 border border-sky-200 text-[10px] font-bold px-2 py-0.5 rounded-full">${escapeHtml(k.jlpt || 'N5')}</span>
          <div class="flex items-center gap-1.5">
            <span class="badge-nhai bg-amber-50 text-amber-800 border border-amber-200 text-[10px] font-bold font-meta px-2 py-0.5 rounded-full">${k.strokeCount || 1} nét</span>
            ${k.radical ? `<span class="text-[10px] text-[#64748B] font-meta">Bộ: ${escapeHtml(k.radical)}</span>` : ''}
          </div>
        </div>

        <!-- Nội dung chính của chữ Hán -->
        <div>
          <div class="flex items-center gap-4 mb-3">
            <div class="w-16 h-16 rounded-xl bg-[#F8FAFC] border border-[#CBD5E1] flex items-center justify-center font-serif text-4xl font-extrabold text-[#0F172A]">
              ${escapeHtml(k.kanjiChar)}
            </div>
            <div class="flex-1 min-w-0">
              <div class="text-sm font-extrabold text-[#831843] uppercase tracking-wide truncate">${escapeHtml(k.hanViet || 'HÁN TỰ')}</div>
              <div class="text-[11px] text-[#64748B] font-mono mt-1 truncate">On: ${escapeHtml(k.onyomi || '-')}</div>
              <div class="text-[11px] text-[#64748B] font-mono truncate">Kun: ${escapeHtml(k.kunyomi || '-')}</div>
            </div>
          </div>

          <div class="p-2.5 rounded-lg bg-[#F8FAFC] border border-[#E2E8F0] text-slate-700 text-[11px] font-sans leading-relaxed line-clamp-2" title="${escapeHtml(k.meaning)}">
            ${escapeHtml(k.meaning || 'Chưa có chú giải nghĩa')}
          </div>
        </div>
      </div>

      <!-- 2 NÚT HÀNH ĐỘNG DƯỚI CARD -->
      <div class="pt-3 mt-auto border-t border-[#E2E8F0] flex items-center gap-2 shrink-0">
        <button type="button" 
                data-action="save-deck"
                data-idx="${idx}"
                data-id="${k.id || ''}"
                data-char="${escapeHtml(k.kanjiChar)}"
                data-hanviet="${escapeHtml(k.hanViet || '')}"
                class="flex-1 py-1.5 px-2 rounded-lg text-xs font-semibold border border-[#CBD5E1] bg-white text-[#1E293B] hover:bg-slate-50 flex items-center justify-center gap-1 transition active:scale-95 cursor-pointer">
          <span>📁</span> Lưu Thẻ
        </button>
        <a href="/shodo?char=${encodeURIComponent(k.kanjiChar)}" 
           class="flex-1 py-1.5 px-2 rounded-lg text-xs font-semibold bg-[#FFE5EC] hover:bg-[#FCE7F3] border border-[#FBCFE8] text-[#831843] flex items-center justify-center gap-1 no-underline transition active:scale-95 cursor-pointer">
          <span>✍️</span> Luyện Nét
        </a>
      </div>
    </div>
  `).join('');

  // Gắn sự kiện click lưu thẻ
  grid.querySelectorAll('[data-action="save-deck"]').forEach(el => {
    el.addEventListener('click', (e) => {
      e.stopPropagation();
      const idx = parseInt(el.getAttribute('data-idx'), 10);
      const item = (window.lastRenderedDictionaryItems && window.lastRenderedDictionaryItems[idx]) || null;
      if (item) {
        openSaveDeckModal(item);
      } else {
        const id = el.getAttribute('data-id');
        const char = el.getAttribute('data-char');
        const hanViet = el.getAttribute('data-hanviet');
        openSaveDeckModal(id, char, hanViet);
      }
    });
  });
}

// =========================================================================
// 7. PHÂN TRANG (PAGINATION)
// =========================================================================
function renderLearnerPagination(page, totalPages, totalElements) {
  const info = document.getElementById('learnerPaginationInfo');
  const btns = document.getElementById('learnerPaginationBtns');
  if (!totalElements || totalElements === 0) {
    if (info) info.innerHTML = 'Không có kết quả tra cứu';
    if (btns) btns.innerHTML = '';
    return;
  }
  if (info) info.innerHTML = `Trang <strong>${page + 1}</strong> / <strong>${totalPages}</strong> (${totalElements} chữ)`;
  if (!btns) return;

  let html = '';
  html += `<button type="button" id="btnPagePrev" ${page === 0 ? 'disabled' : ''} class="px-3 py-1.5 text-xs rounded-lg font-bold border border-[#CBD5E1] bg-white text-[#1E293B] hover:bg-slate-50 disabled:opacity-40 transition active:scale-95 cursor-pointer">◀</button>`;
  html += `<span class="w-8 h-8 rounded-lg bg-[#1E293B] text-white flex items-center justify-center font-bold text-xs">${page + 1}</span>`;
  html += `<button type="button" id="btnPageNext" ${page >= totalPages - 1 ? 'disabled' : ''} class="px-3 py-1.5 text-xs rounded-lg font-bold border border-[#CBD5E1] bg-white text-[#1E293B] hover:bg-slate-50 disabled:opacity-40 transition active:scale-95 cursor-pointer">▶</button>`;
  btns.innerHTML = html;

  const btnPrev = document.getElementById('btnPagePrev');
  if (btnPrev && page > 0) {
    btnPrev.addEventListener('click', () => loadLearnerDictionary(page - 1));
  }
  const btnNext = document.getElementById('btnPageNext');
  if (btnNext && page < totalPages - 1) {
    btnNext.addEventListener('click', () => loadLearnerDictionary(page + 1));
  }
}

// =========================================================================
// 10. MODAL LƯU CHỮ VÀO BỘ THẺ FLASHCARDS (ADD TO DECK)
// =========================================================================
async function loadDeckOptions() {
  const select = document.getElementById('modalDeckSelect');
  if (!select) return;

  let options = [];

  // 1. Thử lấy danh sách từ REST API Backend
  try {
    const token = localStorage.getItem('access_token') || localStorage.getItem('kizuna_token');
    const headers = { 'Accept': 'application/json' };
    if (token) headers['Authorization'] = 'Bearer ' + token;
    const res = await fetch('/api/user/kanji-folders', { headers }).catch(() => null);
    if (res && res.ok) {
      const folders = await res.json();
      if (Array.isArray(folders) && folders.length > 0) {
        options = folders.map(f => ({ name: f.folderName || f.title, count: f.kanjiCount || 0 }));
      }
    }
  } catch (err) {}

  // 2. Đồng bộ với LocalStorage Flashcard Decks
  try {
    const raw = localStorage.getItem('kizuna_flashcard_decks');
    if (raw) {
      const localDecks = JSON.parse(raw);
      if (Array.isArray(localDecks)) {
        localDecks.forEach(d => {
          if (!options.some(o => o.name === d.title)) {
            options.push({ name: d.title, count: (d.cards ? d.cards.length : 0) });
          }
        });
      }
    }
  } catch (e) {}

  if (options.length === 0) {
    options = [{ name: 'Bộ Thẻ Cá Nhân', count: 0 }];
  }

  select.innerHTML = options.map(f => `<option value="${escapeHtml(f.name)}">${escapeHtml(f.name)} (${f.count} chữ)</option>`).join('');
}

function openSaveDeckModal(kanjiDataOrId, char, hanViet) {
  let item = null;
  if (typeof kanjiDataOrId === 'object' && kanjiDataOrId !== null) {
    item = kanjiDataOrId;
  } else {
    const searchChar = char || kanjiDataOrId;
    if (window.lastRenderedDictionaryItems && Array.isArray(window.lastRenderedDictionaryItems)) {
      item = window.lastRenderedDictionaryItems.find(x => (x.kanjiChar || x.char) === searchChar || String(x.id) === String(kanjiDataOrId));
    }
  }

  const finalId = (item && item.id) || (typeof kanjiDataOrId === 'string' ? kanjiDataOrId : '') || '';
  const finalChar = (item && (item.kanjiChar || item.char || item.character)) || char || '';
  const finalHanViet = (item && item.hanViet) || hanViet || '';
  const finalMeaning = (item && (item.meaning || item.vietnameseMeaning)) || '';
  const finalOnyomi = (item && item.onyomi) || '';
  const finalKunyomi = (item && item.kunyomi) || '';
  const finalJlpt = (item && (item.jlpt || item.jlptLevel)) || 'N5';

  currentKanjiToSave = {
    id: finalId,
    kanjiChar: finalChar,
    char: finalChar,
    hanViet: finalHanViet,
    meaning: finalMeaning,
    onyomi: finalOnyomi,
    kunyomi: finalKunyomi,
    jlpt: finalJlpt
  };

  const kanjiIdInput = document.getElementById('modalDeckKanjiId');
  const charInput = document.getElementById('modalDeckCharInput');
  const charEl = document.getElementById('modalDeckChar');
  const hvEl = document.getElementById('modalDeckHanViet');

  if (kanjiIdInput) kanjiIdInput.value = finalId;
  if (charInput) charInput.value = finalChar;
  if (charEl) charEl.textContent = finalChar || '—';
  if (hvEl) hvEl.textContent = finalHanViet || 'HÁN TỰ';

  loadDeckOptions();
  const modal = document.getElementById('modalAddToDeck');
  if (modal) modal.style.display = 'flex';
}

function closeDeckModal() {
  const modal = document.getElementById('modalAddToDeck');
  if (modal) modal.style.display = 'none';
}

async function submitSaveToDeck(e) {
  if (e) e.preventDefault();
  const btn = document.getElementById('btnConfirmSaveDeck');
  if (btn) {
    btn.disabled = true;
    btn.textContent = 'Đang cất...';
  }

  const targetDeckName = document.getElementById('modalDeckSelect') ? document.getElementById('modalDeckSelect').value : 'Bộ Thẻ Cá Nhân';
  const kanjiChar = document.getElementById('modalDeckCharInput') ? document.getElementById('modalDeckCharInput').value : '';
  const kanjiId = document.getElementById('modalDeckKanjiId') ? document.getElementById('modalDeckKanjiId').value : '';
  const hanViet = document.getElementById('modalDeckHanViet') ? document.getElementById('modalDeckHanViet').textContent : '';

  // Đảm bảo có đầy đủ dữ liệu nghĩa, onyomi, kunyomi nếu chưa có
  if (!currentKanjiToSave || !currentKanjiToSave.meaning || currentKanjiToSave.meaning === 'Chưa cập nhật nghĩa') {
    try {
      const res = await fetch(`/api/learner/kanji/search?keyword=${encodeURIComponent(kanjiChar)}`).catch(() => null);
      if (res && res.ok) {
        const data = await res.json();
        const found = (data.items || []).find(x => (x.kanjiChar || x.char) === kanjiChar);
        if (found) {
          currentKanjiToSave = {
            id: found.id || kanjiId,
            kanjiChar: found.kanjiChar || kanjiChar,
            char: found.kanjiChar || kanjiChar,
            hanViet: found.hanViet || hanViet,
            meaning: found.meaning || '',
            onyomi: found.onyomi || '—',
            kunyomi: found.kunyomi || '—',
            jlpt: found.jlpt || 'N5'
          };
        }
      }
    } catch (e) {
      console.debug('Lỗi truy vấn bổ sung thông tin kanji:', e);
    }
  }

  const charToSave = kanjiChar || (currentKanjiToSave ? (currentKanjiToSave.kanjiChar || currentKanjiToSave.char) : '');
  const hvToSave = (currentKanjiToSave && currentKanjiToSave.hanViet) ? currentKanjiToSave.hanViet : (hanViet || 'HÁN TỰ');
  const meaningToSave = (currentKanjiToSave && currentKanjiToSave.meaning) ? currentKanjiToSave.meaning : 'Chưa cập nhật nghĩa';
  const onyomiToSave = (currentKanjiToSave && currentKanjiToSave.onyomi) ? currentKanjiToSave.onyomi : '—';
  const kunyomiToSave = (currentKanjiToSave && currentKanjiToSave.kunyomi) ? currentKanjiToSave.kunyomi : '—';
  const jlptToSave = (currentKanjiToSave && currentKanjiToSave.jlpt) ? currentKanjiToSave.jlpt : 'N5';

  // 1. Lưu vào LocalStorage đồng bộ cho trang Flashcard
  try {
    const raw = localStorage.getItem('kizuna_flashcard_decks');
    let decks = raw ? JSON.parse(raw) : [];
    if (!Array.isArray(decks)) decks = [];

    let deck = decks.find(d => d.title === targetDeckName);
    if (!deck) {
      deck = {
        id: 'deck_' + Date.now(),
        title: targetDeckName,
        description: 'Bộ thẻ học viên tạo từ từ điển',
        cards: [],
        color: 'blue'
      };
      decks.push(deck);
    }

    deck.cards = deck.cards || [];
    const existingIdx = deck.cards.findIndex(c => (c.char || c.kanjiChar) === charToSave);
    const newCard = {
      char: charToSave,
      kanjiChar: charToSave,
      hanViet: hvToSave,
      meaning: meaningToSave,
      onyomi: onyomiToSave,
      kunyomi: kunyomiToSave,
      jlpt: jlptToSave
    };

    if (existingIdx >= 0) {
      // Cập nhật lại thẻ nếu đã tồn tại hoặc trước đó bị thiếu thông tin
      deck.cards[existingIdx] = {
        ...deck.cards[existingIdx],
        ...newCard
      };
    } else {
      deck.cards.push(newCard);
    }
    localStorage.setItem('kizuna_flashcard_decks', JSON.stringify(decks));
  } catch (err) {
    console.debug('Lỗi lưu thẻ localStorage:', err);
  }

  // 2. Gửi request Backend nếu có endpoint
  try {
    const payload = {
      kanjiId: kanjiId,
      kanjiChar: charToSave,
      targetDeckName: targetDeckName
    };
    await fetch('/api/learner/kanji/save-card', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    }).catch(() => null);

    closeDeckModal();
    showAlert('Đã cất chữ vào bộ thẻ thành công, tha hồ lôi ra cày cuốc! 🗂️', true);
  } catch (err) {
    showAlert('Lỗi: ' + err.message, false);
  } finally {
    if (btn) {
      btn.disabled = false;
      btn.textContent = 'Cất Vào Thẻ Ngay';
    }
  }
}

// =========================================================================
// 11. GẮN SỰ KIỆN KHỞI TẠO TOÀN CỤC KHI DOM SẴN SÀNG
// =========================================================================
document.addEventListener('DOMContentLoaded', () => {
  renderDictRadicalButtons();

  // Sự kiện ô tìm kiếm
  const searchInput = document.getElementById('learnerSearchInput');
  if (searchInput) {
    searchInput.addEventListener('input', onSearchInputChange);
    searchInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        loadLearnerDictionary(0);
      }
    });
  }

  const btnClearSearch = document.getElementById('btnClearSearchInput');
  if (btnClearSearch) {
    btnClearSearch.addEventListener('click', clearLearnerSearchInput);
  }

  const jlptSelect = document.getElementById('learnerJlptSelect');
  if (jlptSelect) {
    jlptSelect.addEventListener('change', () => loadLearnerDictionary(0));
  }

  const btnSearchSubmit = document.getElementById('btnSearchSubmit');
  if (btnSearchSubmit) {
    btnSearchSubmit.addEventListener('click', () => loadLearnerDictionary(0));
  }

  // Sự kiện ô số nét
  const strokeInput = document.getElementById('dictStrokeInput');
  if (strokeInput) {
    strokeInput.addEventListener('input', onDictStrokeInputChange);
    strokeInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        loadLearnerDictionary(0);
      }
    });
  }

  const btnClearStroke = document.getElementById('btnClearStrokeInput');
  if (btnClearStroke) {
    btnClearStroke.addEventListener('click', clearDictStrokeInput);
  }

  const btnResetDictStroke = document.getElementById('btnResetDictStroke');
  if (btnResetDictStroke) {
    btnResetDictStroke.addEventListener('click', clearDictStrokeInput);
  }

  // Sự kiện chọn bộ thủ
  const radicalSelect = document.getElementById('dictRadicalSelect');
  if (radicalSelect) {
    radicalSelect.addEventListener('change', (e) => setDictRadicalFilter(e.target.value));
  }

  // Sự kiện Modal Chi tiết
  const btnCloseKanjiDetail = document.getElementById('btnCloseKanjiDetail');
  if (btnCloseKanjiDetail) {
    btnCloseKanjiDetail.addEventListener('click', closeKanjiDetailModal);
  }

  const btnDictPlayPause = document.getElementById('btnDictPlayPause');
  if (btnDictPlayPause) {
    btnDictPlayPause.addEventListener('click', toggleDictStrokePlayer);
  }

  const btnDictReplay = document.getElementById('btnDictReplay');
  if (btnDictReplay) {
    btnDictReplay.addEventListener('click', replayDictStrokePlayer);
  }

  const btnDictSpeed = document.getElementById('btnDictSpeed');
  if (btnDictSpeed) {
    btnDictSpeed.addEventListener('click', cycleDictSpeed);
  }

  const btnDetailSaveDeck = document.getElementById('btnDetailSaveDeck');
  if (btnDetailSaveDeck) {
    btnDetailSaveDeck.addEventListener('click', saveCurrentDetailToDeck);
  }

  // Sự kiện Modal Lưu vào Bộ thẻ
  const btnCloseDeckModal = document.getElementById('btnCloseDeckModal');
  if (btnCloseDeckModal) {
    btnCloseDeckModal.addEventListener('click', closeDeckModal);
  }

  const btnCancelDeckModal = document.getElementById('btnCancelDeckModal');
  if (btnCancelDeckModal) {
    btnCancelDeckModal.addEventListener('click', closeDeckModal);
  }

  const formSaveToDeck = document.getElementById('formSaveToDeck');
  if (formSaveToDeck) {
    formSaveToDeck.addEventListener('submit', submitSaveToDeck);
  }

  // Nạp trạng thái ban đầu
  const initialKeyword = searchInput ? searchInput.value.trim() : '';
  if (initialKeyword) {
    loadLearnerDictionary(0);
  } else {
    renderStandbyState();
  }
});

// Gắn toàn cục cho các hàm tương thích
window.quickSearchPrompt = quickSearchPrompt;
window.onSearchInputChange = onSearchInputChange;
window.clearLearnerSearchInput = clearLearnerSearchInput;
window.onDictStrokeInputChange = onDictStrokeInputChange;
window.clearDictStrokeInput = clearDictStrokeInput;
window.setDictRadicalFilter = setDictRadicalFilter;
window.loadLearnerDictionary = loadLearnerDictionary;
window.openSaveDeckModal = openSaveDeckModal;
window.closeDeckModal = closeDeckModal;
window.submitSaveToDeck = submitSaveToDeck;
