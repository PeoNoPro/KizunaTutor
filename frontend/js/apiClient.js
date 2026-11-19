/**
 * KizunaTutor - REST API Client cho Phân hệ Học & Quản lý Flashcards
 * Fetch JSON, Zero-Reload, Tích hợp KizunaToast & Fallback LocalStorage an toàn
 */

const STORAGE_KEY = 'kizuna_flashcard_decks';

// =========================================================================
// 1. KIZUNA TOAST NOTIFICATION HELPER
// =========================================================================
export const KizunaToast = {
  show(message, type = 'info') {
    if (typeof window !== 'undefined' && typeof window.showToast === 'function') {
      window.showToast(message, type);
      return;
    }
    // Fallback UI toast nếu window.showToast chưa khởi tạo
    let container = document.getElementById('kizunaToastContainer');
    if (!container) {
      container = document.createElement('div');
      container.id = 'kizunaToastContainer';
      container.className = 'fixed bottom-5 right-5 z-[9999] flex flex-col gap-2 pointer-events-none';
      document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    const colorClass = type === 'success' 
      ? 'bg-emerald-600 text-white' 
      : type === 'error' 
        ? 'bg-rose-600 text-white' 
        : type === 'warning'
          ? 'bg-amber-500 text-black'
          : 'bg-[#0F172A] text-white';

    toast.className = `${colorClass} px-4 py-2.5 rounded-lg text-xs font-bold shadow-lg pointer-events-auto transition-all duration-300 transform translate-y-2 opacity-0 flex items-center gap-2`;
    toast.innerHTML = `<span>${type === 'success' ? '✓' : type === 'error' ? '✕' : 'ℹ'}</span><span>${message}</span>`;
    container.appendChild(toast);

    requestAnimationFrame(() => {
      toast.classList.remove('translate-y-2', 'opacity-0');
    });

    setTimeout(() => {
      toast.classList.add('opacity-0', 'translate-y-2');
      setTimeout(() => toast.remove(), 300);
    }, 3000);
  },
  success(message) { this.show(message, 'success'); },
  error(message) { this.show(message, 'error'); },
  danger(message) { this.show(message, 'error'); },
  info(message) { this.show(message, 'info'); },
  warning(message) { this.show(message, 'warning'); }
};

if (typeof window !== 'undefined') {
  window.KizunaToast = KizunaToast;
}

/**
 * Trích xuất các cụm Kana (Hiragana / Katakana) thuần túy từ chuỗi cách đọc / mặt sau thẻ,
 * loại bỏ hoàn toàn nghĩa tiếng Việt, dấu ngăn cách (•, -, ,) và dấu chấm okurigana.
 */
export function extractKanaReadingsFromText(rawText) {
  if (!rawText) return [];
  const str = String(rawText).trim();
  const tokens = [];

  const joinedOkurigana = str.replace(/([\u3040-\u309F\u30A0-\u30FF])\.([\u3040-\u309F\u30A0-\u30FF])/g, '$1$2');
  const joinedMatches = joinedOkurigana.match(/[\u3040-\u309F\u30A0-\u30FFー]+/g) || [];
  tokens.push(...joinedMatches);

  // 2. Nếu có dấu "." tách okurigana (vd: "あお.い"), chỉ bổ sung phần gốc trước dấu "."
  if (joinedOkurigana !== str) {
    str.split(/[,;/|\s]+/).forEach(part => {
      if (part.includes('.')) {
        const stem = part.split('.')[0].match(/[\u3040-\u309F\u30A0-\u30FFー]+/g);
        if (stem) tokens.push(...stem);
      }
    });
  }

  return Array.from(new Set(tokens.map(t => t.trim()).filter(Boolean)));
}

/**
 * Chuẩn hóa đối tượng thẻ Flashcard: luôn bóc tách sẵn trường reading nếu mặt sau có chứa Kana
 * để tương thích cả hiển thị 2 mặt lẫn luyện gõ Mưa chữ Kanji.
 */
export function enrichFlashcardItem(card = {}) {
  const front = String(card.char || card.front || card.word || '').trim();
  const back = String(card.meaning || card.back || '').trim();
  const explicitReading = String(card.reading || card.furigana || card.kunyomi || card.onyomi || '').trim();

  let reading = explicitReading;
  if (!reading || /[\s•\-/|]/.test(reading)) {
    const kanaFromExplicit = extractKanaReadingsFromText(explicitReading);
    const kanaFromBack = extractKanaReadingsFromText(back);
    const combined = [...kanaFromExplicit, ...kanaFromBack];
    if (combined.length > 0) {
      reading = combined[0];
    }
  }

  return {
    ...card,
    char: front || card.char,
    front: front || card.front,
    meaning: back || card.meaning,
    reading: reading || ''
  };
}

// =========================================================================
// 2. STORAGE FALLBACK HELPERS
// =========================================================================
function getStoredDecks() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch (e) {
    console.warn('Lỗi đọc localStorage flashcards:', e);
    return [];
  }
}

function saveStoredDecks(decks) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(decks));
  } catch (e) {
    console.warn('Lỗi ghi localStorage flashcards:', e);
  }
}

function getDeckHeaders(withJson = false) {
  const headers = { 'Accept': 'application/json' };
  if (withJson) headers['Content-Type'] = 'application/json';
  try {
    const token = localStorage.getItem('kizuna_token') || localStorage.getItem('access_token');
    if (token) headers['Authorization'] = `Bearer ${token}`;
  } catch (e) {}
  return headers;
}

// =========================================================================
// 3. API CLIENT IMPLEMENTATION
// =========================================================================
export const apiClient = {
  /**
   * Lấy danh sách tất cả các bộ thẻ
   * GET /api/learner/decks
   */
  async getDecks() {
    const localDecks = getStoredDecks();
    try {
      const res = await fetch('/api/learner/decks', {
        headers: getDeckHeaders(false),
        signal: AbortSignal.timeout(4000)
      }).catch(err => {
        console.warn('Lỗi kết nối /api/learner/decks:', err);
        return null;
      });

      if (res) {
        if (res.ok) {
          const data = await res.json();
          if (Array.isArray(data)) {
            const merged = [...data];
            localDecks.forEach(ld => {
              if (!merged.some(m => String(m.id) === String(ld.id) || m.title === ld.title)) {
                merged.push(ld);
              }
            });
            saveStoredDecks(merged);
            return merged;
          }
        } else if (res.status !== 401 && res.status !== 403) {
          // 401/403: chưa đăng nhập hoặc chưa vào lớp; màn hình khóa (js/learning-tools-access.js) đã giải thích.
          KizunaToast.warning(`Máy chủ bộ thẻ phản hồi mã ${res.status}. Đang hiển thị bản lưu cục bộ.`);
        }
      } else {
        if (localDecks.length > 0) {
          KizunaToast.info('Không thể kết nối máy chủ bộ thẻ. Đang hiển thị dữ liệu lưu trong máy.');
        } else {
          KizunaToast.warning('Không thể kết nối máy chủ bộ thẻ và chưa có dữ liệu lưu cục bộ.');
        }
      }
    } catch (e) {
      console.warn('API /api/learner/decks offline, dùng bộ nhớ cục bộ:', e);
      KizunaToast.warning('Đang chạy ở chế độ ngoại tuyến (dữ liệu cục bộ).');
    }
    return localDecks;
  },

  /**
   * Lấy chi tiết một bộ thẻ theo ID
   * GET /api/learner/decks/{id}
   */
  async getDeck(id) {
    try {
      const res = await fetch(`/api/learner/decks/${encodeURIComponent(id)}`, {
        headers: { 'Accept': 'application/json' },
        signal: AbortSignal.timeout(4000)
      }).catch(() => null);

      if (res && res.ok) {
        const data = await res.json();
        if (data && data.title) {
          return data;
        }
      }
    } catch (e) {
      console.debug(`API /api/learner/decks/${id} offline, tìm trong local:`, e);
    }

    const decks = getStoredDecks();
    return decks.find(d => String(d.id) === String(id)) || null;
  },

  /**
   * Tạo bộ thẻ mới
   * POST /api/learner/decks
   */
  async createDeck(deckData) {
    const newDeck = {
      id: deckData.id || `deck-${Date.now()}`,
      title: deckData.title || 'Bộ Thẻ Mới',
      description: deckData.description || '',
      color: deckData.color || 'blue',
      cards: deckData.cards || [],
      createdAt: new Date().toISOString()
    };

    try {
      const res = await fetch('/api/learner/decks', {
        method: 'POST',
        headers: getDeckHeaders(true),
        body: JSON.stringify(newDeck),
        signal: AbortSignal.timeout(4000)
      }).catch(() => null);

      if (res && res.ok) {
        const created = await res.json();
        if (created && created.title) {
          const decks = getStoredDecks();
          decks.unshift(created);
          saveStoredDecks(decks);
          return created;
        }
      }
    } catch (e) {
      console.debug('API POST /api/learner/decks offline:', e);
    }

    const decks = getStoredDecks();
    decks.unshift(newDeck);
    saveStoredDecks(decks);
    return newDeck;
  },

  /**
   * Cập nhật thông tin bộ thẻ (Tên, Mô tả, Danh sách thẻ)
   * PUT /api/learner/decks/{id}
   */
  async updateDeck(id, updateData) {
    const decks = getStoredDecks();
    const idx = decks.findIndex(d => String(d.id) === String(id));
    if (idx === -1) {
      throw new Error(`Không tìm thấy bộ thẻ với ID ${id}`);
    }

    const updatedDeck = {
      ...decks[idx],
      ...updateData,
      updatedAt: new Date().toISOString()
    };

    try {
      const res = await fetch(`/api/learner/decks/${encodeURIComponent(id)}`, {
        method: 'PUT',
        headers: getDeckHeaders(true),
        body: JSON.stringify(updatedDeck),
        signal: AbortSignal.timeout(4000)
      }).catch(() => null);

      if (res && res.ok) {
        const saved = await res.json();
        if (saved && saved.title) {
          decks[idx] = saved;
          saveStoredDecks(decks);
          return saved;
        }
      }
    } catch (e) {
      console.debug(`API PUT /api/learner/decks/${id} offline:`, e);
    }

    decks[idx] = updatedDeck;
    saveStoredDecks(decks);
    return updatedDeck;
  },

  /**
   * Xóa một bộ thẻ
   * DELETE /api/learner/decks/{id}
   */
  async deleteDeck(id) {
    try {
      await fetch(`/api/learner/decks/${encodeURIComponent(id)}`, {
        method: 'DELETE',
        headers: getDeckHeaders(false),
        signal: AbortSignal.timeout(4000)
      }).catch(() => null);
    } catch (e) {
      console.debug(`API DELETE /api/learner/decks/${id} offline:`, e);
    }

    const decks = getStoredDecks().filter(d => String(d.id) !== String(id));
    saveStoredDecks(decks);
    return true;
  },

  /**
   * Thêm danh sách thẻ con vào bộ thẻ
   * POST /api/learner/decks/{id}/cards
   */
  async addCards(id, cards) {
    const cardArray = Array.isArray(cards) ? cards : [cards];

    try {
      const res = await fetch(`/api/learner/decks/${encodeURIComponent(id)}/cards`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify({ cards: cardArray }),
        signal: AbortSignal.timeout(4000)
      }).catch(() => null);

      if (res && res.ok) {
        const result = await res.json();
        if (result && result.cards) {
          const decks = getStoredDecks();
          const target = decks.find(d => String(d.id) === String(id));
          if (target) {
            target.cards = result.cards;
            saveStoredDecks(decks);
          }
          return result;
        }
      }
    } catch (e) {
      console.debug(`API POST /api/learner/decks/${id}/cards offline:`, e);
    }

    // Fallback Local
    const decks = getStoredDecks();
    const target = decks.find(d => String(d.id) === String(id));
    if (!target) throw new Error(`Không tìm thấy bộ thẻ #${id}`);

    target.cards = target.cards || [];
    target.cards.push(...cardArray);
    saveStoredDecks(decks);
    return target;
  },

  /**
   * Xóa một thẻ con khỏi bộ
   * DELETE /api/learner/decks/{id}/cards/{cardId}
   */
  async deleteCard(id, cardIdOrIndex) {
    try {
      await fetch(`/api/learner/decks/${encodeURIComponent(id)}/cards/${encodeURIComponent(cardIdOrIndex)}`, {
        method: 'DELETE',
        signal: AbortSignal.timeout(4000)
      }).catch(() => null);
    } catch (e) {
      console.debug(`API DELETE /api/learner/decks/${id}/cards offline:`, e);
    }

    const decks = getStoredDecks();
    const target = decks.find(d => String(d.id) === String(id));
    if (!target || !target.cards) return false;

    if (typeof cardIdOrIndex === 'number') {
      target.cards.splice(cardIdOrIndex, 1);
    } else {
      target.cards = target.cards.filter((c, i) => String(c.id || i) !== String(cardIdOrIndex));
    }
    saveStoredDecks(decks);
    return true;
  },

  /**
   * Nhân bản bộ thẻ
   * POST /api/learner/decks/{id}/duplicate
   */
  async duplicateDeck(id) {
    try {
      const res = await fetch(`/api/learner/decks/${encodeURIComponent(id)}/duplicate`, {
        method: 'POST',
        headers: { 'Accept': 'application/json' },
        signal: AbortSignal.timeout(4000)
      }).catch(() => null);

      if (res && res.ok) {
        const cloned = await res.json();
        if (cloned && cloned.title) {
          const decks = getStoredDecks();
          decks.unshift(cloned);
          saveStoredDecks(decks);
          return cloned;
        }
      }
    } catch (e) {
      console.debug(`API POST /api/learner/decks/${id}/duplicate offline:`, e);
    }

    const decks = getStoredDecks();
    const original = decks.find(d => String(d.id) === String(id));
    if (!original) throw new Error('Không tìm thấy bộ thẻ để nhân bản!');

    const duplicated = {
      id: `deck-${Date.now()}`,
      title: `${original.title} (Bản sao)`,
      description: original.description || '',
      color: original.color || 'blue',
      cards: (original.cards || []).map(c => ({ ...c })),
      createdAt: new Date().toISOString()
    };

    decks.unshift(duplicated);
    saveStoredDecks(decks);
    return duplicated;
  },

  /**
   * Gộp 2 bộ thẻ
   * POST /api/learner/decks/merge
   */
  async mergeDecks(sourceId, targetId, deleteSource = false) {
    try {
      const res = await fetch('/api/learner/decks/merge', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify({ sourceId, targetId, deleteSource }),
        signal: AbortSignal.timeout(4000)
      }).catch(() => null);

      if (res && res.ok) {
        const merged = await res.json();
        if (merged && merged.title) {
          const decks = getStoredDecks().map(d => String(d.id) === String(targetId) ? merged : d);
          const filtered = deleteSource ? decks.filter(d => String(d.id) !== String(sourceId)) : decks;
          saveStoredDecks(filtered);
          return merged;
        }
      }
    } catch (e) {
      console.debug('API POST /api/learner/decks/merge offline:', e);
    }

    const decks = getStoredDecks();
    const source = decks.find(d => String(d.id) === String(sourceId));
    const target = decks.find(d => String(d.id) === String(targetId));

    if (!source || !target) {
      throw new Error('Không tìm thấy bộ thẻ nguồn hoặc bộ thẻ đích để gộp!');
    }

    const sourceCards = (source.cards || []).map(c => ({ ...c }));
    target.cards = (target.cards || []).concat(sourceCards);

    let updatedDecks = decks;
    if (deleteSource) {
      updatedDecks = decks.filter(d => String(d.id) !== String(sourceId));
    }
    saveStoredDecks(updatedDecks);
    return target;
  }
};

if (typeof window !== 'undefined') {
  window.apiClient = apiClient;
}
