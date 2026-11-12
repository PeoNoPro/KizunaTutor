/**
 * KizunaTutor - Quản lý & Luyện tập Bộ thẻ ghi nhớ Flashcards
 * Feature-Based architecture module, tuân thủ nghiêm ngặt FRONTEND_RULES.md
 * TUYỆT ĐỐI KHÔNG DÙNG DỮ LIỆU MẪU (NO MOCK DATA INJECTION)
 */

import { showToast, debounce } from '/js/main.js';
import { apiClient, KizunaToast, enrichFlashcardItem } from '/js/apiClient.js';
import { initLearnerShell } from '/components/learner-layout/learner-layout.js';

// Khởi tạo khung giao diện học viên dùng chung
initLearnerShell({
  activeMenu: 'flashcards',
  pageTitle: 'Bộ thẻ Flashcards',
  badge: 'Bộ nhớ siêu tốc'
});

// =========================================================================
// 1. TRẠNG THÁI TOÀN CỤC CỦA TRANG
// =========================================================================
let userDecks = [];
let sourceMergeDeckId = null;
let activeEditingDeck = null;
let editingCards = [];
let isDeckLoadError = false;

// =========================================================================
// 2. TẢI DỮ LIỆU BỘ THẺ QUA APICLIENT
// =========================================================================
async function loadDecks() {
  isDeckLoadError = false;
  try {
    userDecks = await apiClient.getDecks();
  } catch (e) {
    console.error('Lỗi nạp bộ thẻ:', e);
    isDeckLoadError = true;
    userDecks = [];
    KizunaToast.error('Không thể tải dữ liệu bộ thẻ từ máy chủ! (' + (e.message || 'Lỗi mạng') + ')');
  }
  renderDecks();
}

function escapeHtml(text) {
  if (text === null || text === undefined) return '';
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

// =========================================================================
// 3. RENDER DANH SÁCH BỘ THẺ (DECK CARD COMPONENT)
// =========================================================================
function renderDecks(filterQuery = '') {
  const grid = document.getElementById('deckGrid');
  const emptyState = document.getElementById('deckEmptyState');
  const countBadge = document.getElementById('deckCountBadge');

  if (!grid || !emptyState) return;

  const q = (filterQuery || '').trim().toLowerCase();
  const filtered = userDecks.filter(d => 
    (d.title || '').toLowerCase().includes(q) || ((d.description || '').toLowerCase().includes(q))
  );

  if (countBadge) {
    countBadge.textContent = `${filtered.length} bộ thẻ`;
  }

  // Khi chưa có dữ liệu hoặc tìm kiếm không khớp -> Hiển thị Empty State
  if (filtered.length === 0) {
    grid.innerHTML = '';
    grid.classList.add('hidden');
    emptyState.classList.remove('hidden');

    const emptyTitle = emptyState.querySelector('h3');
    const emptyDesc = emptyState.querySelector('p');
    const emptyBtn = document.getElementById('btnEmptyCreateDeck');

    if (isDeckLoadError) {
      if (emptyTitle) emptyTitle.textContent = 'Mất kết nối với máy chủ bộ thẻ';
      if (emptyDesc) emptyDesc.textContent = 'Hệ thống không thể tải danh sách bộ thẻ lúc này. Vui lòng kiểm tra lại kết nối và bấm thử lại.';
      if (emptyBtn) {
        emptyBtn.innerHTML = '<span class="material-symbols-outlined text-[16px]">refresh</span><span>Thử tải lại</span>';
        emptyBtn.onclick = () => loadDecks();
      }
    } else {
      if (emptyTitle) emptyTitle.textContent = filterQuery ? 'Không tìm thấy bộ thẻ phù hợp' : 'Chưa có bộ thẻ ghi nhớ nào';
      if (emptyDesc) emptyDesc.textContent = filterQuery ? 'Không có bộ thẻ nào khớp với từ khóa tìm kiếm của bạn.' : 'Tạo ngay một bộ thẻ mới để cất giữ những từ vựng khó nhớ và luyện tập lật thẻ mỗi ngày nhé!';
      if (emptyBtn) {
        emptyBtn.innerHTML = '<span class="material-symbols-outlined text-[16px]">add</span><span>Tạo bộ thẻ đầu tiên</span>';
        emptyBtn.onclick = () => openCreateDeckModal();
      }
    }
    return;
  }

  emptyState.classList.add('hidden');
  grid.classList.remove('hidden');

  let html = '';

  filtered.forEach(deck => {
    const cardCount = deck.cards ? deck.cards.length : 0;

    html += `
      <div class="deck-card relative flex flex-col justify-between p-4 bg-white border border-[#0F172A] rounded-[5px] shadow-[3px_3px_0px_0px_#0F172A] hover:-translate-y-0.5 transition-transform group" data-deck-id="${deck.id}">
        <!-- Phần thông tin thư mục -->
        <div>
          <div class="flex items-start justify-between gap-2">
            <h3 class="font-['Itim'] text-base font-bold text-slate-900 truncate flex-1 cursor-pointer hover:text-[#006194] transition" 
                title="Xem chi tiết bộ thẻ: ${escapeHtml(deck.title)}"
                data-action="view-detail" data-id="${deck.id}">
              🌸 ${escapeHtml(deck.title)}
            </h3>
            
            <!-- Nút Thao Tác Thêm (Dropdown Trigger) -->
            <div class="relative dropdown-container shrink-0">
              <button type="button" 
                      data-action="toggle-menu" data-id="${deck.id}"
                      class="w-7 h-7 flex items-center justify-center border border-[#0F172A] rounded-[4px] bg-slate-100 hover:bg-slate-200 text-xs shadow-[1px_1px_0px_0px_#0F172A] cursor-pointer transition font-bold"
                      title="Tùy chọn khác">
                ⋮
              </button>

              <!-- Dropdown Menu Nội dung -->
              <div id="menu-${deck.id}" class="deck-dropdown-menu hidden absolute right-0 top-8 w-48 bg-white border border-[#0F172A] rounded-[5px] shadow-[3px_3px_0px_0px_#0F172A] z-30 py-1 text-xs">
                <button type="button" data-action="view-detail" data-id="${deck.id}" class="w-full text-left px-3 py-2 hover:bg-slate-100 flex items-center gap-2 cursor-pointer text-slate-800">
                  <span>🔍</span> Xem trang chi tiết
                </button>
                <button type="button" data-action="print-deck" data-id="${deck.id}" class="w-full text-left px-3 py-2 hover:bg-slate-100 flex items-center gap-2 cursor-pointer text-slate-800 border-t border-slate-100">
                  <span>🖨️</span> In bộ thẻ (PDF)
                </button>
                <button type="button" data-action="duplicate-deck" data-id="${deck.id}" class="w-full text-left px-3 py-2 hover:bg-slate-100 flex items-center gap-2 cursor-pointer text-slate-800">
                  <span>📋</span> Nhân bản bộ thẻ
                </button>
                <button type="button" data-action="merge-deck" data-id="${deck.id}" class="w-full text-left px-3 py-2 hover:bg-slate-100 flex items-center gap-2 border-t border-slate-100 cursor-pointer text-slate-800">
                  <span>🔗</span> Gộp với bộ thẻ khác
                </button>
              </div>
            </div>
          </div>

          <!-- Mô tả thư mục -->
          <p class="text-xs text-slate-600 mt-1 line-clamp-2" title="${escapeHtml(deck.description || '')}">
            ${escapeHtml(deck.description || 'Chưa có ghi chú mô tả cho bộ thẻ này.')}
          </p>
          <div class="mt-2 text-[11px] font-semibold text-pink-600 flex items-center gap-1.5">
            <span>🏷️</span>
            <span>${cardCount} thẻ học</span>
          </div>
        </div>

        <!-- Cụm nút hành động chính ở chân thẻ (Theo quy chuẩn Ponytail & Impeccable) -->
        <div class="grid grid-cols-3 gap-2 mt-4 pt-3 border-t border-[#0F172A]/15">
          <!-- Nút Chỉnh Sửa -->
          <button type="button" 
                  data-action="edit-deck" data-id="${deck.id}" 
                  class="py-1.5 px-2 bg-white hover:bg-slate-100 text-[#0F172A] text-xs font-bold border border-[#0F172A] rounded-[4px] shadow-[2px_2px_0px_0px_#0F172A] flex items-center justify-center gap-1 cursor-pointer truncate transition active:scale-95"
                  title="Chỉnh sửa bộ thẻ & nạp dữ liệu từ Excel">
            <span>✏️</span> Chỉnh Sửa
          </button>

          <!-- Nút Xóa -->
          <button type="button" 
                  data-action="delete-deck" data-id="${deck.id}" 
                  class="py-1.5 px-2 bg-rose-50 hover:bg-rose-100 text-rose-700 text-xs font-bold border border-[#0F172A] rounded-[4px] shadow-[2px_2px_0px_0px_#0F172A] flex items-center justify-center gap-1 cursor-pointer truncate transition active:scale-95"
                  title="Xóa bộ thẻ này">
            <span>🗑️</span> Xóa
          </button>

          <!-- Nút Chi Tiết Độc Lập -->
          <button type="button" 
                  data-action="view-detail" data-id="${deck.id}" 
                  class="py-1.5 px-2 bg-[#006194] hover:bg-[#004B73] text-white text-xs font-bold border border-[#0F172A] rounded-[4px] shadow-[2px_2px_0px_0px_#0F172A] flex items-center justify-center gap-1 cursor-pointer truncate transition active:scale-95"
                  title="Mở trang chi tiết bộ thẻ độc lập (/learner/deck-detail)">
            <span>📖</span> Chi Tiết
          </button>
        </div>
      </div>
    `;
  });

  grid.innerHTML = html;

  // Click vào card để điều hướng sang trang độc lập (Master-Detail Route)
  grid.querySelectorAll('.deck-card').forEach(card => {
    card.addEventListener('click', (e) => {
      if (e.target.closest('button') || e.target.closest('.dropdown-container')) return;
      const deckId = card.dataset.deckId;
      window.location.href = `/learner/deck-detail?id=${encodeURIComponent(deckId)}`;
    });
  });

  grid.querySelectorAll('[data-action="view-detail"]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      window.location.href = `/learner/deck-detail?id=${encodeURIComponent(btn.dataset.id)}`;
    });
  });

  grid.querySelectorAll('[data-action="study-deck"]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      window.location.href = `/flashcards-study?id=${encodeURIComponent(btn.dataset.id)}`;
    });
  });

  grid.querySelectorAll('[data-action="match-deck"]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      closeAllMenus();
      window.location.href = `/learner/flashcards-match?id=${encodeURIComponent(btn.dataset.id)}`;
    });
  });

  grid.querySelectorAll('[data-action="rain-deck"]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      closeAllMenus();
      window.location.href = `/kanji-game?deckId=${encodeURIComponent(btn.dataset.id)}`;
    });
  });

  grid.querySelectorAll('[data-action="edit-deck"]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      openEditDeckModal(btn.dataset.id);
    });
  });

  grid.querySelectorAll('[data-action="delete-deck"]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      handleDeleteDeck(btn.dataset.id);
    });
  });

  grid.querySelectorAll('[data-action="toggle-menu"]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      toggleDeckMenu(e, btn.dataset.id);
    });
  });

  grid.querySelectorAll('[data-action="print-deck"]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      closeAllMenus();
      handlePrintDeck(btn.dataset.id);
    });
  });

  grid.querySelectorAll('[data-action="duplicate-deck"]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      closeAllMenus();
      handleDuplicateDeck(btn.dataset.id);
    });
  });

  grid.querySelectorAll('[data-action="merge-deck"]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      closeAllMenus();
      openMergeModal(btn.dataset.id);
    });
  });
}

function toggleDeckMenu(event, deckId) {
  event.stopPropagation();
  const targetMenu = document.getElementById(`menu-${deckId}`);
  if (!targetMenu) return;

  const isHidden = targetMenu.classList.contains('hidden');
  closeAllMenus();
  if (isHidden) {
    targetMenu.classList.remove('hidden');
  }
}

function closeAllMenus() {
  document.querySelectorAll('.deck-dropdown-menu').forEach(m => m.classList.add('hidden'));
}

// =========================================================================
// 4. TẠO & XÓA BỘ THẺ (CRUD LOGIC QUA APICLIENT)
// =========================================================================
function openCreateDeckModal() {
  document.getElementById('modalCreateDeck').classList.remove('hidden');
  document.getElementById('deckNameInput').value = '';
  document.getElementById('deckDescInput').value = '';
}

function closeCreateDeckModal() {
  document.getElementById('modalCreateDeck').classList.add('hidden');
}

async function saveNewDeck() {
  const nameInput = document.getElementById('deckNameInput');
  const descInput = document.getElementById('deckDescInput');
  const title = (nameInput.value || '').trim();
  const desc = (descInput.value || '').trim();

  if (!title) {
    KizunaToast.warning('Vui lòng nhập tên cho bộ thẻ nhé.');
    nameInput.focus();
    return;
  }

  try {
    const created = await apiClient.createDeck({
      title: title,
      description: desc,
      color: 'blue',
      cards: []
    });
    userDecks.unshift(created);
    closeCreateDeckModal();
    renderDecks();
    KizunaToast.success(`Đã tạo thành công bộ thẻ "${title}".`);
  } catch (err) {
    console.error(err);
    KizunaToast.error('Không thể tạo bộ thẻ mới. Vui lòng thử lại!');
  }
}

async function handleDeleteDeck(deckId) {
  const target = userDecks.find(d => String(d.id) === String(deckId));
  if (!target) return;

  if (!confirm(`Bạn có chắc chắn muốn xóa vĩnh viễn bộ thẻ "${target.title}" kèm tất cả thẻ con không?`)) {
    return;
  }

  try {
    await apiClient.deleteDeck(deckId);
    userDecks = userDecks.filter(d => String(d.id) !== String(deckId));
    renderDecks();
    KizunaToast.info(`Đã xóa bộ thẻ "${target.title}".`);
  } catch (err) {
    console.error(err);
    KizunaToast.error('Không thể xóa bộ thẻ. Vui lòng thử lại!');
  }
}

// =========================================================================
// 5. CÁC TÙY CHỌN NÂNG CAO: IN BỘ THẺ, NHÂN BẢN, GỘP BỘ THẺ
// =========================================================================

// 1. In bộ thẻ (Print Mode A4 / Xuất PDF 2 mặt)
function handlePrintDeck(deckId) {
  const deck = userDecks.find(d => String(d.id) === String(deckId));
  if (!deck) return;
  const cards = deck.cards || [];
  if (cards.length === 0) {
    KizunaToast.warning('Bộ thẻ này chưa có thẻ học nào để in!');
    return;
  }

  const printWindow = window.open('', '_blank');
  if (!printWindow) {
    KizunaToast.error('Vui lòng cho phép popup trên trình duyệt để mở giao diện in ấn.');
    return;
  }

  const printHtml = `
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="utf-8">
  <title>In Bộ Thẻ - ${escapeHtml(deck.title)}</title>
  <style>
    @page {
      size: A4;
      margin: 12mm;
    }
    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
      margin: 0;
      padding: 0;
      color: #0F172A;
      background: #FFF;
    }
    .toolbar {
      padding: 10px 14px;
      background: #F1F5F9;
      margin-bottom: 16px;
      border-radius: 6px;
      display: flex;
      gap: 10px;
    }
    .btn {
      padding: 7px 16px;
      background: #006194;
      color: white;
      border: 1px solid #0F172A;
      border-radius: 4px;
      font-weight: bold;
      font-size: 12px;
      cursor: pointer;
    }
    .print-header {
      border-bottom: 2px solid #0F172A;
      padding-bottom: 8px;
      margin-bottom: 16px;
      display: flex;
      justify-content: space-between;
      align-items: flex-end;
    }
    .print-header h1 {
      margin: 0;
      font-size: 18px;
      font-weight: 800;
    }
    .print-header p {
      margin: 4px 0 0;
      font-size: 12px;
      color: #64748B;
    }
    .print-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 12px;
    }
    .print-card {
      border: 1.5px dashed #0F172A;
      border-radius: 6px;
      padding: 12px 14px;
      page-break-inside: avoid;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      min-height: 110px;
      background: #FAFAFA;
      box-sizing: border-box;
    }
    .card-front {
      font-size: 16px;
      font-weight: bold;
      color: #0F2545;
      margin-bottom: 6px;
      border-bottom: 1px solid #E2E8F0;
      padding-bottom: 6px;
    }
    .card-back {
      font-size: 12px;
      color: #334155;
      line-height: 1.4;
    }
    .card-footer {
      display: flex;
      justify-content: space-between;
      font-size: 10px;
      color: #94A3B8;
      margin-top: 8px;
      padding-top: 4px;
    }
    @media print {
      .no-print { display: none !important; }
      body { background: white; }
    }
  </style>
</head>
<body>
  <div class="toolbar no-print">
    <button class="btn" onclick="window.print()">🖨️ In Ngay (Print / Lưu PDF)</button>
    <button class="btn" style="background:#64748B;" onclick="window.close()">Đóng Cửa Sổ</button>
  </div>
  <div class="print-header">
    <div>
      <h1>🌸 ${escapeHtml(deck.title)}</h1>
      <p>${escapeHtml(deck.description || 'Bộ thẻ Kizuna Flashcards')} • Tổng số: ${cards.length} thẻ</p>
    </div>
    <div style="font-size: 11px; font-weight: bold; color: #64748B;">KizunaTutor Flashcards</div>
  </div>
  <div class="print-grid">
    ${cards.map((c, i) => `
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

// 2. Nhân bản bộ thẻ (Instant Duplicate API)
async function handleDuplicateDeck(deckId) {
  const target = userDecks.find(d => String(d.id) === String(deckId));
  if (!target) return;

  if (!confirm(`Bạn có muốn nhân bản bộ thẻ "${target.title}" kèm toàn bộ thẻ con không?`)) return;

  try {
    const duplicated = await apiClient.duplicateDeck(deckId);
    userDecks.unshift(duplicated);
    renderDecks();
    KizunaToast.success(`Đã nhân bản bộ thẻ "${duplicated.title}" thành công!`);
  } catch (err) {
    console.error(err);
    KizunaToast.error('Lỗi khi nhân bản bộ thẻ!');
  }
}

// 3. Gộp bộ thẻ (Merge Modal & API)
function openMergeModal(deckId) {
  const sourceDeck = userDecks.find(d => String(d.id) === String(deckId));
  if (!sourceDeck) return;

  const otherDecks = userDecks.filter(d => String(d.id) !== String(deckId));
  if (otherDecks.length === 0) {
    KizunaToast.warning('Bạn cần có ít nhất 2 bộ thẻ để có thể thực hiện gộp thẻ!');
    return;
  }

  sourceMergeDeckId = deckId;
  document.getElementById('mergeSourceDeckTitle').textContent = sourceDeck.title;
  document.getElementById('mergeSourceDeckCount').textContent = `${(sourceDeck.cards || []).length} thẻ`;

  const select = document.getElementById('mergeTargetDeckSelect');
  select.innerHTML = otherDecks.map(d => `
    <option value="${d.id}">${escapeHtml(d.title)} (${(d.cards || []).length} thẻ)</option>
  `).join('');

  document.getElementById('checkDeleteSourceAfterMerge').checked = false;
  document.getElementById('mergeDeckModal').classList.remove('hidden');
}

function closeMergeModal() {
  document.getElementById('mergeDeckModal').classList.add('hidden');
  sourceMergeDeckId = null;
}

async function executeMergeDecks() {
  if (!sourceMergeDeckId) return;
  const select = document.getElementById('mergeTargetDeckSelect');
  const targetDeckId = select.value;
  const shouldDeleteSource = document.getElementById('checkDeleteSourceAfterMerge').checked;

  try {
    const merged = await apiClient.mergeDecks(sourceMergeDeckId, targetDeckId, shouldDeleteSource);
    userDecks = await apiClient.getDecks();
    renderDecks();
    closeMergeModal();
    KizunaToast.success(`Đã gộp thành công vào bộ thẻ "${merged.title}"!`);
  } catch (err) {
    console.error(err);
    KizunaToast.error('Lỗi khi gộp bộ thẻ!');
  }
}

// =========================================================================
// 6. MODAL CHỈNH SỬA BỘ THẺ & TÍCH HỢP IMPORT EXCEL 2 CỘT
// =========================================================================
function openEditDeckModal(deckId) {
  const deck = userDecks.find(d => String(d.id) === String(deckId));
  if (!deck) return;

  activeEditingDeck = deck;
  editingCards = (deck.cards || []).map(c => ({ ...c }));

  document.getElementById('editDeckNameInput').value = deck.title || '';
  document.getElementById('editDeckDescInput').value = deck.description || '';
  document.getElementById('quickAddFrontInput').value = '';
  document.getElementById('quickAddBackInput').value = '';

  renderEditingCardsList();
  document.getElementById('editDeckModal').classList.remove('hidden');
}

function closeEditDeckModal() {
  document.getElementById('editDeckModal').classList.add('hidden');
  activeEditingDeck = null;
  editingCards = [];
}

function renderEditingCardsList() {
  const container = document.getElementById('deckCardsListContainer');
  const countBadge = document.getElementById('deckCardCountBadge');
  if (!container) return;

  if (countBadge) {
    countBadge.textContent = `${editingCards.length} thẻ`;
  }

  if (editingCards.length === 0) {
    container.innerHTML = `
      <div class="p-6 text-center text-slate-400 text-xs border border-dashed border-slate-300 rounded-[4px] bg-slate-50">
        Chưa có thẻ nào trong bộ. Hãy thêm từ file Excel đa cột hoặc nhập nhanh ở trên nhé!
      </div>
    `;
    return;
  }

  container.innerHTML = editingCards.map((c, idx) => `
    <div class="flex items-center justify-between gap-2 p-2 bg-white border border-slate-200 rounded-[4px] text-xs hover:border-[#0F172A] transition">
      <div class="flex-1 min-w-0 grid grid-cols-1 sm:grid-cols-2 gap-1.5 items-center">
        <div class="flex items-center gap-1.5 truncate">
          <span class="font-bold text-[#0F172A] font-japanese" title="${escapeHtml(c.char)}">
            ${escapeHtml(c.char)}
          </span>
          ${c.hanViet ? `<span class="text-[10px] text-[#881337] font-semibold uppercase">(${escapeHtml(c.hanViet)})</span>` : ''}
          ${c.jlpt ? `<span class="px-1 text-[9px] font-bold rounded bg-rose-50 text-rose-800 border border-rose-200">${escapeHtml(c.jlpt)}</span>` : ''}
        </div>
        <div class="text-[#334155] truncate text-xs" title="${escapeHtml(c.meaning || '')}">
          ${escapeHtml(c.meaning || '—')}
          ${(c.onyomi && c.onyomi !== '—') ? `<span class="text-[10px] text-sky-700 ml-1.5 font-japanese">[On: ${escapeHtml(c.onyomi)}]</span>` : ''}
        </div>
      </div>
      <button type="button" data-action="remove-editing-card" data-index="${idx}"
              class="w-6 h-6 flex items-center justify-center rounded border border-rose-200 text-rose-600 hover:bg-rose-50 cursor-pointer shrink-0 font-bold"
              title="Xóa thẻ này">
        ✕
      </button>
    </div>
  `).join('');

  container.querySelectorAll('[data-action="remove-editing-card"]').forEach(btn => {
    btn.addEventListener('click', () => {
      const idx = parseInt(btn.dataset.index, 10);
      editingCards.splice(idx, 1);
      renderEditingCardsList();
    });
  });
}
function quickAddCardToEditList() {
  const frontInput = document.getElementById('quickAddFrontInput');
  const backInput = document.getElementById('quickAddBackInput');
  const front = (frontInput.value || '').trim();
  const back = (backInput.value || '').trim();

  if (!front) {
    KizunaToast.warning('Vui lòng nhập nội dung mặt trước (câu hỏi/từ vựng).');
    frontInput.focus();
    return;
  }

  editingCards.push({
    char: front,
    hanViet: '',
    meaning: back || '—',
    onyomi: '—',
    kunyomi: '—',
    jlpt: 'Tự do'
  });

  frontInput.value = '';
  backInput.value = '';
  renderEditingCardsList();
  KizunaToast.success(`Đã thêm thẻ [${front}] vào danh sách.`);
}

/**
 * Đọc file Excel Đa Cột Chuẩn & Tương Thích Ngược 2 Cột
 */
function handleExcelUpload(event) {
  const file = event.target.files[0];
  if (!file) return;

  if (typeof XLSX === 'undefined') {
    KizunaToast.error('Thư viện Excel chưa tải xong, vui lòng thử lại sau giây lát!');
    event.target.value = '';
    return;
  }

  const reader = new FileReader();
  reader.onload = function(e) {
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

        // Bỏ qua dòng tiêu đề nếu chứa từ khóa thông dụng
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

        // Định dạng đa cột chuẩn (>= 4 cột: Kanji, Hán Việt, On, Kun, Nghĩa, JLPT)
        if (row.length >= 4) {
          const char = col0;
          const hanViet = String(row[1] || '').trim();
          const onyomi = String(row[2] || '').trim();
          const kunyomi = String(row[3] || '').trim();
          const meaning = String(row[4] || '').trim();
          const jlpt = String(row[5] || '').trim();

          newCards.push(enrichFlashcardItem({
            char: char,
            hanViet: hanViet,
            onyomi: onyomi || '—',
            kunyomi: kunyomi || '—',
            meaning: meaning || '—',
            jlpt: jlpt || 'Tự do'
          }));
        } else if (row.length === 3) {
          // Định dạng 3 cột: Mặt trước | Cách đọc Kana | Nghĩa tiếng Việt
          const readingCol = String(row[1] || '').trim();
          const meaningCol = String(row[2] || '').trim();
          newCards.push(enrichFlashcardItem({
            char: col0,
            reading: readingCol,
            meaning: readingCol && meaningCol ? `${readingCol} • ${meaningCol}` : (meaningCol || readingCol || '—'),
            jlpt: 'Tự do'
          }));
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

          newCards.push(enrichFlashcardItem({
            char: col0,
            hanViet: hanViet,
            onyomi: onyomi,
            kunyomi: kunyomi,
            meaning: meaning || '—',
            jlpt: 'Tự do'
          }));
        }
      });

      if (newCards.length === 0) {
        KizunaToast.warning('Không tìm thấy dữ liệu hợp lệ trong file Excel!');
        return;
      }

      editingCards = editingCards.concat(newCards);
      renderEditingCardsList();
      KizunaToast.success(`Đã đọc thành công ${newCards.length} thẻ từ file Excel!`);
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
    KizunaToast.error('Thư viện Excel chưa sẵn sàng, vui lòng thử lại sau giây lát!');
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
async function saveDeckChanges() {
  if (!activeEditingDeck) return;
  const nameInput = document.getElementById('editDeckNameInput');
  const descInput = document.getElementById('editDeckDescInput');
  const title = (nameInput.value || '').trim();
  const desc = (descInput.value || '').trim();

  if (!title) {
    KizunaToast.warning('Vui lòng nhập tên bộ thẻ nhé.');
    nameInput.focus();
    return;
  }

  try {
    const updated = await apiClient.updateDeck(activeEditingDeck.id, {
      title: title,
      description: desc,
      cards: editingCards
    });

    const idx = userDecks.findIndex(d => String(d.id) === String(activeEditingDeck.id));
    if (idx !== -1) {
      userDecks[idx] = updated;
    }

    renderDecks();
    closeEditDeckModal();
    KizunaToast.success(`Đã lưu thay đổi bộ thẻ "${title}" thành công!`);
  } catch (err) {
    console.error(err);
    KizunaToast.error('Lỗi khi lưu thay đổi bộ thẻ!');
  }
}

// =========================================================================
// 7. KHỞI TẠO DOM & LẮNG NGHE SỰ KIỆN (DOM READY)
// =========================================================================
document.addEventListener('DOMContentLoaded', () => {
  loadDecks();

  const searchInput = document.getElementById('inputSearchDecks');
  if (searchInput) {
    searchInput.addEventListener('input', debounce((e) => {
      renderDecks(e.target.value);
    }, 250));
  }

  // Nút tạo bộ thẻ mới
  document.getElementById('btnOpenCreateDeck')?.addEventListener('click', openCreateDeckModal);
  document.getElementById('btnEmptyCreateDeck')?.addEventListener('click', openCreateDeckModal);
  document.getElementById('btnCloseCreateDeckModal')?.addEventListener('click', closeCreateDeckModal);
  document.getElementById('btnCancelDeck')?.addEventListener('click', closeCreateDeckModal);
  document.getElementById('btnSaveDeck')?.addEventListener('click', saveNewDeck);

  // Modal Chỉnh Sửa Bộ Thẻ & Excel
  document.getElementById('btnCloseEditDeckModal')?.addEventListener('click', closeEditDeckModal);
  document.getElementById('btnCancelEditDeck')?.addEventListener('click', closeEditDeckModal);
  document.getElementById('btnSaveDeckChanges')?.addEventListener('click', saveDeckChanges);
  document.getElementById('btnTriggerExcelUpload')?.addEventListener('click', () => {
    document.getElementById('excelFileInput')?.click();
  });
  document.getElementById('excelFileInput')?.addEventListener('change', handleExcelUpload);
  document.getElementById('btnDownloadSampleExcel')?.addEventListener('click', downloadSampleExcel);
  document.getElementById('btnQuickAddCard')?.addEventListener('click', quickAddCardToEditList);

  // Modal Gộp Bộ Thẻ
  document.getElementById('btnCloseMergeModal')?.addEventListener('click', closeMergeModal);
  document.getElementById('btnCancelMerge')?.addEventListener('click', closeMergeModal);
  document.getElementById('btnConfirmMerge')?.addEventListener('click', executeMergeDecks);

  // Đóng dropdown menus khi click bên ngoài
  document.addEventListener('click', (e) => {
    if (!e.target.closest('.dropdown-container')) {
      closeAllMenus();
    }
  });
});
