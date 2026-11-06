/**
 * KizunaTutor - Decoupled REST API Client (Zero-Reload)
 * Compliant with Graphify Architecture & Impeccable standards
 */

const API_BASE_URL = typeof window !== 'undefined' && window.location.port === '3000'
  ? '/api'
  : 'http://localhost:8085/api';

const AGENT_MEMORY_URL = typeof window !== 'undefined' && window.location.port === '3000'
  ? '/memory'
  : 'http://localhost:8081/memory';

class ApiClient {
  constructor() {
    this.baseUrl = API_BASE_URL;
    this.memoryUrl = AGENT_MEMORY_URL;
  }

  getAuthHeaders() {
    const headers = {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    try {
      const token = localStorage.getItem('kizuna_token');
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
    } catch (e) {}
    return headers;
  }

  async request(endpoint, options = {}) {
    const url = endpoint.startsWith('http') ? endpoint : `${this.baseUrl}${endpoint.startsWith('/') ? '' : '/'}${endpoint}`;
    const config = {
      ...options,
      headers: {
        ...this.getAuthHeaders(),
        ...(options.headers || {})
      }
    };

    try {
      const response = await fetch(url, config);
      const contentType = response.headers.get('content-type') || '';
      
      let resData = null;
      if (contentType.includes('application/json')) {
        resData = await response.json();
      } else {
        const text = await response.text();
        try {
          resData = JSON.parse(text);
        } catch {
          resData = text;
        }
      }

      if (!response.ok) {
        const message = (resData && (resData.message || resData.error)) || `Lỗi máy chủ: HTTP ${response.status}`;
        throw new Error(message);
      }

      // Chuẩn hóa cấu trúc gói tin thống nhất { status, success, message, data }
      if (resData && typeof resData === 'object' && ('success' in resData || 'data' in resData)) {
        return {
          status: resData.status || response.status,
          success: resData.success !== false,
          message: resData.message || 'Thao tác thành công',
          data: resData.data !== undefined ? resData.data : resData
        };
      }

      return {
        status: response.status,
        success: true,
        message: 'Thao tác thành công',
        data: resData
      };
    } catch (err) {
      console.warn(`[ApiClient] Request to ${endpoint} failed:`, err);
      return {
        status: err.status || 500,
        success: false,
        message: err.message || 'Không thể kết nối đến máy chủ',
        data: null,
        error: err
      };
    }
  }

  get(endpoint, params = {}) {
    const query = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') {
        query.append(k, v);
      }
    });
    const queryString = query.toString();
    const fullUrl = queryString ? `${endpoint}${endpoint.includes('?') ? '&' : '?'}${queryString}` : endpoint;
    return this.request(fullUrl, { method: 'GET' });
  }

  post(endpoint, data = {}) {
    return this.request(endpoint, {
      method: 'POST',
      body: JSON.stringify(data)
    });
  }

  put(endpoint, data = {}) {
    return this.request(endpoint, {
      method: 'PUT',
      body: JSON.stringify(data)
    });
  }

  delete(endpoint) {
    return this.request(endpoint, { method: 'DELETE' });
  }

  // =========================================================================
  // KANJI & VOCABULARY APIS
  // =========================================================================
  kanji = {
    search: (params) => this.get('/learner/kanji/search', params),
    detail: (char) => this.get('/learner/kanji/detail', { char }),
    getById: (id) => this.get('/learner/kanji/detail', { id }),
    getByChar: (char) => this.get(`/kanji/${encodeURIComponent(char)}`)
  };

  // =========================================================================
  // FLASHCARD DECKS APIS
  // =========================================================================
  decks = {
    getAll: () => this.get('/user/kanji-folders'),
    getById: (id) => this.get(`/user/kanji-folders/${encodeURIComponent(id)}`),
    create: (deckData) => this.post('/user/kanji-folders', deckData),
    update: (id, deckData) => this.put(`/user/kanji-folders/${encodeURIComponent(id)}`, deckData),
    delete: (id) => this.delete(`/user/kanji-folders/${encodeURIComponent(id)}`)
  };

  // =========================================================================
  // AI WRITING & GRAMMAR
  // =========================================================================
  aiWriting = {
    check: (payload) => this.post('/learner/ai-writing/check', payload),
    getPrompts: (level = 'N5') => this.get('/learner/ai-writing/prompts', { level })
  };

  // =========================================================================
  // TENCENTDB AGENT MEMORY (PORT 8081 / PROXY)
  // =========================================================================
  memory = {
    /**
     * Gửi ngữ cảnh học tập / nét vẽ / lỗi sai vào bộ nhớ dài hạn Sensei AI
     */
    append: async (memoryItem) => {
      const payload = {
        userId: memoryItem.userId || 'learner-default',
        sessionId: memoryItem.sessionId || 'session-' + Date.now(),
        feature: memoryItem.feature || 'shodo', // 'shodo', 'flashcard', 'ai-writing'
        content: memoryItem.content,
        metadata: {
          timestamp: new Date().toISOString(),
          score: memoryItem.score,
          mistakes: memoryItem.mistakes,
          targetChar: memoryItem.targetChar,
          jlptLevel: memoryItem.jlptLevel,
          ...(memoryItem.metadata || {})
        }
      };

      try {
        const res = await fetch(`${AGENT_MEMORY_URL}/append`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });
        if (res.ok) {
          return await res.json();
        }
      } catch (e) {
        console.debug('[AgentMemory] Direct memory server offline, storing in local fallback:', e);
      }

      // Fallback lưu cục bộ để không bao giờ đánh mất lịch sử
      try {
        const key = `kizuna_agent_memory_${payload.feature}`;
        const existing = JSON.parse(localStorage.getItem(key) || '[]');
        existing.unshift(payload);
        if (existing.length > 50) existing.pop();
        localStorage.setItem(key, JSON.stringify(existing));
      } catch (e) {}

      return {
        status: 200,
        success: true,
        message: 'Đã ghi nhận vào bộ nhớ Sensei AI (Local Cache)',
        data: payload
      };
    },

    /**
     * Lấy ngữ cảnh gợi ý từ bộ nhớ gần nhất để đưa ra nhận xét cá nhân hóa
     */
    getRecentAdvice: (feature = 'shodo') => {
      try {
        const key = `kizuna_agent_memory_${feature}`;
        const history = JSON.parse(localStorage.getItem(key) || '[]');
        return history.slice(0, 5);
      } catch (e) {
        return [];
      }
    }
  };
}

export const apiClient = new ApiClient();
export default apiClient;

// Gắn toàn cục cho phép sử dụng thuận tiện ở mọi script
if (typeof window !== 'undefined') {
  window.apiClient = apiClient;
  window.API_CLIENT = apiClient;
}
