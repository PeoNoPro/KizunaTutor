/**
 * KizunaTutor - Từ Điển Chữ Hán & Bút Thuận (Kanji Dictionary & Stroke Order)
 * Phục dựng 100% chuẩn giao diện & tính năng từ phiên bản gốc (legacy dictionary)
 * Giữ nguyên cấu trúc Layout dùng chung cho Học Viên (Header, Sidebar, Footer)
 * Tuân thủ FRONTEND_RULES: Zero mock data, Zero exception leak
 */

import { initLearnerShell } from '/components/learner-layout/learner-layout.js';
import { showToast } from '/js/main.js';
import { voicevoxClient } from '/js/voicevoxClient.js';

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
let currentSearchController = null;
let isSearching = false;

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
/**
 * Đọc từ khóa tìm kiếm từ URL mà không bị lỗi Double-Encoding
 * và khôi phục từ sessionStorage khi quay lại trang
 */
/**
 * Chuẩn hóa giải mã URL an toàn (hỗ trợ single/double encode, không lỗi ký tự đặc biệt)
 */
function safeDecodeQuery(raw) {
  if (!raw) return '';
  let str = String(raw).trim();
  // Decode tối đa 2 lần nếu chuỗi chứa mẫu %XX hợp lệ
  for (let i = 0; i < 2; i++) {
    if (/%[0-9A-Fa-f]{2}/.test(str)) {
      try {
        str = decodeURIComponent(str);
      } catch (e) {
        break;
      }
    } else {
      break;
    }
  }
  return str.trim();
}

function getNormalizedSearchQuery() {
  const params = new URLSearchParams(window.location.search);
  const rawQuery = params.get('q') ?? params.get('keyword') ?? params.get('char') ?? null;
  
  if (rawQuery !== null && rawQuery !== '') {
    return safeDecodeQuery(rawQuery);
  }

  // Nếu trên URL không có query param (ví dụ click từ Sidebar vào /kanji), giữ màn hình chờ sạch sẽ
  return '';
}

function getNormalizedFilterParams() {
  const params = new URLSearchParams(window.location.search);
  const jlpt = params.get('jlpt') || '';
  const stroke = params.get('stroke') || params.get('strokeCount') || '';
  const radical = params.get('radical') || '';
  return { jlpt, stroke, radical };
}

function onSearchInputChange() {
  const input = document.getElementById('learnerSearchInput') || document.getElementById('kanjiSearchInput');
  const clearBtn = document.getElementById('btnClearSearchInput');
  if (clearBtn) {
    if (input && input.value.trim().length > 0) clearBtn.classList.remove('hidden');
    else clearBtn.classList.add('hidden');
  }
  debounceLearnerSearch();
}

function clearLearnerSearchInput() {
  const input = document.getElementById('learnerSearchInput') || document.getElementById('kanjiSearchInput');
  if (input) input.value = '';
  const clearBtn = document.getElementById('btnClearSearchInput');
  if (clearBtn) clearBtn.classList.add('hidden');
  sessionStorage.removeItem('kizuna_dict_last_query');
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
    const searchInput = document.getElementById('learnerSearchInput') || document.getElementById('kanjiSearchInput');
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
  const inputEl = document.getElementById('learnerSearchInput') || document.getElementById('kanjiSearchInput');
  const inputVal = inputEl ? inputEl.value.trim() : '';
  const kw = currentDictRadical ? (inputVal ? `${inputVal} ${currentDictRadical}` : currentDictRadical) : inputVal;
  const jlptSelect = document.getElementById('learnerJlptSelect');
  const jlpt = jlptSelect ? jlptSelect.value : 'ALL';
  const hasStroke = currentDictStroke !== null;
  const hasRadical = currentDictRadical !== null;
  const hasJlpt = jlpt && jlpt !== 'ALL';
  const hasKeyword = !!kw;

  // Nếu không có bất kỳ bộ lọc nào, hiển thị màn hình chờ Standby
  if (!hasKeyword && !hasJlpt && !hasStroke && !hasRadical) {
    sessionStorage.removeItem('kizuna_dict_last_query');
    if (window.location.search) {
      window.history.replaceState({}, '', window.location.pathname);
    }
    renderStandbyState();
    return;
  }

  // Lưu từ khóa vào sessionStorage để khi rời trang sang Shodo/Flashcards rồi quay lại không bị mất
  if (inputVal) {
    sessionStorage.setItem('kizuna_dict_last_query', inputVal);
  }

  // Đồng bộ URL sạch sẽ trên thanh địa chỉ không gây reload trang
  const searchParams = new URLSearchParams();
  if (inputVal) searchParams.set('q', inputVal);
  if (jlpt && jlpt !== 'ALL') searchParams.set('jlpt', jlpt);
  if (currentDictStroke !== null) searchParams.set('stroke', currentDictStroke);
  if (currentDictRadical) searchParams.set('radical', currentDictRadical);
  if (page > 0) searchParams.set('page', page);

  const queryString = searchParams.toString();
  const newUrl = queryString ? `${window.location.pathname}?${queryString}` : window.location.pathname;
  window.history.replaceState({ query: inputVal, page }, '', newUrl);

  // Hủy request đang chạy trước đó nếu có (chống race condition & in-flight leak)
  if (currentSearchController) {
    currentSearchController.abort();
  }
  const controller = new AbortController();
  currentSearchController = controller;

  const grid = document.getElementById('learnerKanjiGrid');
  if (grid) {
    grid.innerHTML = '<div class="col-span-full py-12 text-center text-[#64748B] font-meta text-xs">Đang lật mở các trang từ điển Kanji... 🔍</div>';
  }

  isSearching = true;

  try {
    let url = `/api/learner/kanji/search?page=${page}&size=18`;
    if (kw) url += `&keyword=${encodeURIComponent(kw)}`;
    if (jlpt && jlpt !== 'ALL') url += `&jlpt=${encodeURIComponent(jlpt)}`;
    if (currentDictStroke !== null) url += `&strokeCount=${encodeURIComponent(currentDictStroke)}`;

    const fetchHeaders = {
      'Accept': 'application/json',
      'Cache-Control': 'no-cache',
      'Pragma': 'no-cache'
    };

    let res = await fetch(url, {
      method: 'GET',
      headers: fetchHeaders,
      signal: controller.signal
    }).catch(err => {
      if (err.name === 'AbortError') throw err;
      return null;
    });

    if (!res || !res.ok) {
      // Fallback endpoint công khai
      let fallbackUrl = `/api/kanji/search?page=${page}&size=18`;
      if (kw) fallbackUrl += `&keyword=${encodeURIComponent(kw)}`;
      if (jlpt && jlpt !== 'ALL') fallbackUrl += `&jlpt=${encodeURIComponent(jlpt)}`;
      if (currentDictStroke !== null) fallbackUrl += `&strokeCount=${encodeURIComponent(currentDictStroke)}`;
      res = await fetch(fallbackUrl, {
        method: 'GET',
        headers: fetchHeaders,
        signal: controller.signal
      }).catch(err => {
        if (err.name === 'AbortError') throw err;
        return null;
      });
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
    // Nếu request bị abort do người dùng chuyển trang hoặc gõ tiếp, BỎ QUA không render lỗi hay rỗng!
    if (e.name === 'AbortError') {
      return;
    }
    if (grid) {
      grid.innerHTML = '<div class="col-span-full py-12 text-center text-[#DC2626] font-meta text-xs font-bold">Lỗi kết nối từ điển: ' + escapeHtml(e.message) + '</div>';
    }
  } finally {
    if (currentSearchController === controller) {
      isSearching = false;
      currentSearchController = null;
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
    <div class="bg-white rounded-xl border-2 border-[#0F172A] shadow-[3px_3px_0px_0px_#0F172A] hover:translate-x-[-1px] hover:translate-y-[-1px] p-4 flex flex-col justify-between transition-all relative">
      <div class="space-y-3">
        <!-- Header thông tin: Cấp độ JLPT, số nét, bộ thủ -->
        <div class="flex items-center justify-between border-b border-[#E2E8F0] pb-2">
          <span class="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold bg-[#FFE5EC] text-[#831843] border border-[#FBCFE8]">${escapeHtml(k.jlpt || 'N5')}</span>
          <div class="flex items-center gap-1.5">
            <span class="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold bg-slate-100 text-slate-700 border border-slate-200">${k.strokeCount || 1} nét</span>
            ${k.radical ? `<span class="text-[10px] text-[#64748B] font-meta">Bộ: ${escapeHtml(k.radical)}</span>` : ''}
          </div>
        </div>

        <!-- Nội dung chính của chữ Hán: Nằm liền mạch ngay dưới Header, không bị khoảng trống -->
        <div class="space-y-2.5">
          <div class="flex items-center gap-4">
            <div class="w-16 h-16 rounded-[8px] bg-[#FAF8F2] border-2 border-[#0F172A] shadow-[2px_2px_0px_0px_#0F172A] flex items-center justify-center font-japanese text-3xl font-bold text-[#0F172A] shrink-0 select-none">
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

      <!-- CỤM NÚT HÀNH ĐỘNG: PHÁT ÂM + LƯU THẺ + LUYỆN NÉT -->
      <div class="pt-3 mt-4 border-t border-[#E2E8F0] flex items-center gap-2 shrink-0">
        <button type="button" 
                data-action="speak-kanji"
                data-char="${escapeHtml(k.kanjiChar)}"
                class="w-8 h-8 rounded-lg border border-[#CBD5E1] bg-slate-50 hover:bg-slate-100 text-[#1E293B] flex items-center justify-center transition active:scale-95 cursor-pointer shrink-0"
                title="Nghe phát âm chuẩn trọng âm (VOICEVOX AI TTS)">
          🔊
        </button>
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
           class="flex-1 py-1.5 px-2 rounded-lg text-xs font-semibold bg-[#FFE5EC] hover:bg-[#FCE7F3] border border-[#FBCFE8] text-[#831843] flex items-center justify-center gap-1 no-underline transition active:scale-95 cursor-pointer"
           title="Tập viết chữ này trên Canvas Shodo">
          <span>✍️</span> Shodo
        </a>
        <a href="/kanji-game?jlpt=${encodeURIComponent(k.jlptLevel || 'N5')}" 
           class="py-1.5 px-2 rounded-lg text-xs font-semibold bg-[#EFF6FF] hover:bg-[#DBEAFE] border border-[#BFDBFE] text-[#1E40AF] flex items-center justify-center gap-1 no-underline transition active:scale-95 cursor-pointer"
           title="Thử thách Game Mưa chữ Hán theo cấp độ này">
          <span>🌧️</span> Game
        </a>
      </div>
    </div>
  `).join('');

  // Gắn sự kiện click phát âm bằng VOICEVOX
  grid.querySelectorAll('[data-action="speak-kanji"]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      const char = btn.getAttribute('data-char');
      btn.classList.add('scale-110');
      voicevoxClient.speak(char).finally(() => {
        setTimeout(() => btn.classList.remove('scale-110'), 300);
      });
    });
  });

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
  html += `<span class="w-8 h-8 rounded-lg bg-[#9F1239] text-white flex items-center justify-center font-bold text-xs">${page + 1}</span>`;
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
    const res = await fetch('/api/learner/decks', { headers }).catch(() => null);
    if (res && res.ok) {
      const folders = await res.json();
      if (Array.isArray(folders) && folders.length > 0) {
        options = folders.map(f => ({ id: f.id, name: f.title || f.folderName || 'Bộ thẻ', count: (Array.isArray(f.cards) ? f.cards.length : (f.kanjiCount || 0)) }));
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
          if (!options.some(o => (d.id && o.id === d.id) || o.name === d.title)) {
            options.push({ id: d.id, name: d.title, count: (d.cards ? d.cards.length : 0) });
          }
        });
      }
    }
  } catch (e) {}

  if (options.length === 0) {
    options = [{ id: 'deck_default', name: 'Bộ Thẻ Cá Nhân', count: 0 }];
  }

  let htmlOptions = options.map(f => `<option value="${escapeHtml(f.id || f.name)}" data-id="${escapeHtml(f.id || '')}" data-name="${escapeHtml(f.name)}">${escapeHtml(f.name)} (${f.count} chữ)</option>`).join('');
  htmlOptions += '<option value="__NEW__">➕ Tạo bộ thẻ mới...</option>';
  select.innerHTML = htmlOptions;

  const newDeckWrapper = document.getElementById('newDeckInputWrapper');
  const newDeckInput = document.getElementById('modalNewDeckNameInput');
  if (newDeckWrapper) newDeckWrapper.classList.add('hidden');
  if (newDeckInput) newDeckInput.value = '';

  select.onchange = () => {
    if (select.value === '__NEW__') {
      newDeckWrapper?.classList.remove('hidden');
      newDeckInput?.focus();
    } else {
      newDeckWrapper?.classList.add('hidden');
    }
  };
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

  const select = document.getElementById('modalDeckSelect');
  const selectedOption = select && select.options ? select.options[select.selectedIndex] : null;
  const isNewDeck = select && select.value === '__NEW__';
  const customNameInput = document.getElementById('modalNewDeckNameInput');

  let targetDeckId = isNewDeck ? null : (selectedOption ? (selectedOption.getAttribute('data-id') || selectedOption.value) : null);
  let targetDeckName = isNewDeck
    ? ((customNameInput && customNameInput.value.trim()) ? customNameInput.value.trim() : 'Bộ Thẻ Mới')
    : (selectedOption ? (selectedOption.getAttribute('data-name') || selectedOption.textContent.replace(/\s*\(\d+\s*chữ\)$/, '')) : 'Bộ Thẻ Cá Nhân');

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

  // 1. Lưu vào LocalStorage & Đồng bộ lên REST API Backend (/api/learner/decks)
  try {
    const raw = localStorage.getItem('kizuna_flashcard_decks');
    let decks = raw ? JSON.parse(raw) : [];
    if (!Array.isArray(decks)) decks = [];

    let deck = null;
    if (targetDeckId) {
      deck = decks.find(d => String(d.id) === String(targetDeckId));
    }
    if (!deck) {
      deck = decks.find(d => d.title === targetDeckName);
    }

    if (!deck) {
      deck = {
        id: targetDeckId || ('deck_' + Date.now()),
        title: targetDeckName,
        description: 'Bộ thẻ học viên tạo từ từ điển',
        cards: [],
        color: 'blue',
        createdAt: new Date().toISOString()
      };
      decks.unshift(deck);
    }

    deck.cards = deck.cards || [];
    const existingIdx = deck.cards.findIndex(c => (c.char || c.kanjiChar) === charToSave);

    // Ghép mặt sau: âm Ôn, âm Kun, Hán việt, Nghĩa (phân cách bằng dấu phẩy và khoảng trắng ", ")
    const backParts = [];
    if (onyomiToSave && onyomiToSave !== '—' && onyomiToSave.trim()) backParts.push(onyomiToSave.trim());
    if (kunyomiToSave && kunyomiToSave !== '—' && kunyomiToSave.trim()) backParts.push(kunyomiToSave.trim());
    if (hvToSave && hvToSave !== '—' && hvToSave.trim()) backParts.push(hvToSave.trim());
    if (meaningToSave && meaningToSave !== '—' && meaningToSave.trim()) backParts.push(meaningToSave.trim());
    const formattedBack = backParts.length > 0 ? backParts.join(', ') : (meaningToSave || 'Chưa cập nhật nghĩa');

    const cleanOn = (onyomiToSave && onyomiToSave !== '—') ? onyomiToSave.trim() : '';
    const cleanKun = (kunyomiToSave && kunyomiToSave !== '—') ? kunyomiToSave.trim() : '';
    const newCard = {
      char: charToSave,
      front: charToSave,
      hanViet: hvToSave,
      onyomi: cleanOn,
      kunyomi: cleanKun,
      reading: cleanKun || cleanOn,
      jlpt: jlptToSave,
      meaning: formattedBack,
      back: formattedBack
    };

    if (existingIdx >= 0) {
      deck.cards[existingIdx] = {
        ...deck.cards[existingIdx],
        ...newCard
      };
    } else {
      deck.cards.push(newCard);
    }
    localStorage.setItem('kizuna_flashcard_decks', JSON.stringify(decks));

    // 2. Đồng bộ trực tiếp đến API Backend /api/learner/decks
    let synced = false;
    try {
      const deckHeaders = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
      const authToken = localStorage.getItem('kizuna_token') || localStorage.getItem('access_token');
      if (authToken) deckHeaders['Authorization'] = 'Bearer ' + authToken;

      const putRes = await fetch('/api/learner/decks/' + encodeURIComponent(deck.id), {
        method: 'PUT',
        headers: deckHeaders,
        body: JSON.stringify({
          title: deck.title,
          description: deck.description || '',
          cards: deck.cards
        })
      }).catch(() => null);

      if (putRes && putRes.ok) {
        const savedData = await putRes.json().catch(() => null);
        if (savedData && (savedData.id || savedData.title)) {
          synced = true;
        }
      }

      if (!synced) {
        await fetch('/api/learner/decks', {
          method: 'POST',
          headers: deckHeaders,
          body: JSON.stringify(deck)
        }).catch(() => null);
      }
    } catch (apiErr) {
      console.debug('Lỗi đồng bộ API /api/learner/decks:', apiErr);
    }

    closeDeckModal();
    showAlert(`Đã lưu "${charToSave}" vào bộ thẻ "${targetDeckName}" thành công! 🗂️`, true);
  } catch (err) {
    console.error('Lỗi lưu thẻ:', err);
    showAlert('Có lỗi khi lưu thẻ: ' + (err.message || 'Vui lòng thử lại!'), false);
  } finally {
    if (btn) {
      btn.disabled = false;
      btn.textContent = 'Cất Vào Thẻ Ngay';
    }
  }
}

// =========================================================================
// 11. KHỞI TẠO TRANG TỪ ĐIỂN & LẮNG NGHE SỰ KIỆN
// =========================================================================
async function initDictionaryPage() {
  // 1. Reset các cờ trạng thái cũ chống rò rỉ dữ liệu phiên trước
  isSearching = false;
  if (currentSearchController) {
    currentSearchController.abort();
    currentSearchController = null;
  }

  // 2. Lấy từ khóa chuẩn hóa từ URL hoặc sessionStorage
  const targetQuery = getNormalizedSearchQuery();
  const searchInput = document.getElementById('learnerSearchInput') || document.getElementById('kanjiSearchInput');
  const clearBtn = document.getElementById('btnClearSearchInput');

  if (targetQuery && searchInput) {
    searchInput.value = targetQuery;
    searchInput.setAttribute('value', targetQuery);
    if (clearBtn) clearBtn.classList.remove('hidden');
  }

  const { jlpt, stroke, radical } = getNormalizedFilterParams();

  if (jlpt) {
    const jlptSelect = document.getElementById('learnerJlptSelect');
    if (jlptSelect) jlptSelect.value = jlpt;
  }

  if (stroke) {
    const strokeInput = document.getElementById('dictStrokeInput');
    if (strokeInput) {
      strokeInput.value = stroke;
      currentDictStroke = parseInt(stroke, 10);
      const clearStroke = document.getElementById('btnClearStrokeInput');
      if (clearStroke) clearStroke.classList.remove('hidden');
    }
  }

  if (radical) {
    currentDictRadical = radical;
    const radicalSelect = document.getElementById('dictRadicalSelect');
    if (radicalSelect) radicalSelect.value = radical;
  }

  // 3. Thực thi tìm kiếm nếu có từ khóa hoặc bộ lọc, ngược lại hiển thị Standby
  const inputVal = searchInput ? searchInput.value.trim() : '';
  const hasKeyword = !!inputVal;
  const hasJlpt = jlpt && jlpt !== 'ALL';
  const hasStroke = currentDictStroke !== null;
  const hasRadical = currentDictRadical !== null;

  if (hasKeyword || hasJlpt || hasStroke || hasRadical) {
    await loadLearnerDictionary(0);
  } else {
    renderStandbyState();
  }
}

document.addEventListener('DOMContentLoaded', async () => {
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

  // Lắng nghe sự kiện popstate của trình duyệt (Back/Forward)
  window.addEventListener('popstate', () => {
    initDictionaryPage();
  });

  // Nạp trạng thái ban đầu sạch sẽ, an toàn
  await initDictionaryPage();
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
