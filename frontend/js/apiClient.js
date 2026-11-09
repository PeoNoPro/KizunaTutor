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
  info(message) { this.show(message, 'info'); },
  warning(message) { this.show(message, 'warning'); }
};

if (typeof window !== 'undefined') {
  window.KizunaToast = KizunaToast;
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
        headers: { 'Accept': 'application/json' },
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
        } else {
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
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
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
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
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
