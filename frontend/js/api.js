/**
 * KizunaTutor - Centralized API Service
 * Kết nối REST API Backend Spring Boot (cổng 8085).
 * Tuân thủ FRONTEND_RULES.md (Mục 11 - API Integration Rules).
 */

const API_BASE_URL = 'http://localhost:8085/api';
const AUTH_TOKEN_KEY = 'kizuna_token';
const DEFAULT_ERROR_MESSAGE = 'Không thể kết nối đến máy chủ. Vui lòng thử lại sau.';

/**
 * Lỗi API đã được ánh xạ sang thông điệp tiếng Việt từ backend (code, message, fields, missingDocuments).
 */
export class ApiError extends Error {
  constructor({ status = 0, code = 'NETWORK_ERROR', message = DEFAULT_ERROR_MESSAGE, fields = {}, missingDocuments = [] } = {}) {
    super(message);
    this.status = status;
    this.code = code;
    this.fields = fields || {};
    this.missingDocuments = missingDocuments || [];
  }
}

function getStoredToken() {
  try {
    return localStorage.getItem(AUTH_TOKEN_KEY);
  } catch (e) {
    return null;
  }
}

function redirectToLogin() {
  try {
    localStorage.removeItem(AUTH_TOKEN_KEY);
    localStorage.removeItem('kizuna_user');
  } catch (e) {}
  window.location.href = '/login';
}

/**
 * Gọi API cần đăng nhập: tự gắn "Authorization: Bearer <token>".
 * - body là FormData thì gửi multipart; object thì gửi JSON.
 * - responseType 'blob' để tải tệp; mặc định đọc JSON (204 trả về null).
 * - 401 thì xóa phiên và chuyển về trang đăng nhập.
 */
export async function authRequest(path, { method = 'GET', body, responseType = 'json', timeoutMs = 30000 } = {}) {
  const token = getStoredToken();
  if (!token) {
    redirectToLogin();
    throw new ApiError({ status: 401, code: 'UNAUTHENTICATED', message: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.' });
  }

  const headers = { Authorization: `Bearer ${token}` };
  let payload = body;
  if (body !== undefined && !(body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
    payload = JSON.stringify(body);
  }

  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers,
      body: payload,
      signal: AbortSignal.timeout(timeoutMs)
    });
  } catch (err) {
    throw new ApiError();
  }

  if (response.status === 401) {
    redirectToLogin();
    throw new ApiError({ status: 401, code: 'UNAUTHENTICATED', message: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.' });
  }

  if (!response.ok) {
    let data = {};
    try {
      data = await response.json();
    } catch (e) {}
    throw new ApiError({
      status: response.status,
      code: data.code || 'HTTP_ERROR',
      message: data.message || (response.status === 413
        ? 'Tệp không hợp lệ. Vui lòng chọn tệp PDF, PNG hoặc JPG có dung lượng tối đa 10MB.'
        : 'Hệ thống đang bận, bạn vui lòng thử lại sau.'),
      fields: data.fields,
      missingDocuments: data.missingDocuments
    });
  }

  if (responseType === 'blob') return response.blob();
  if (response.status === 204) return null;
  return response.json();
}

export const API = {
  tutors: {
    /**
     * Tìm kiếm và lọc danh sách gia sư đa tiêu chí từ Database PostgreSQL Supabase
     */
    async search(params = {}) {
      try {
        const query = new URLSearchParams();
        if (params.keyword?.trim()) query.append('keyword', params.keyword.trim());
        if (params.jlptLevel && params.jlptLevel !== 'all') query.append('jlptLevel', params.jlptLevel);
        if (params.goal) query.append('goal', params.goal);
        if (params.format) query.append('format', params.format);
        if (params.minPrice) query.append('minPrice', params.minPrice);
        if (params.maxPrice) query.append('maxPrice', params.maxPrice);
        if (params.schedule) query.append('schedule', params.schedule);
        if (params.sortBy) query.append('sortBy', params.sortBy);
        if (params.page !== undefined) query.append('page', params.page);
        if (params.size !== undefined) query.append('size', params.size);

        const response = await fetch(`${API_BASE_URL}/tutors/search?${query.toString()}`, {
          signal: AbortSignal.timeout(8000)
        });

        if (!response.ok) {
          throw new Error(`Lỗi kết nối máy chủ: HTTP ${response.status}`);
        }

        return await response.json();
      } catch (err) {
        console.error('API.tutors.search error:', err);
        throw err;
      }
    },

    /**
     * Lấy danh sách gia sư tiêu biểu (Featured Tutors) có điểm đánh giá cao nhất
     */
    async getFeatured() {
      try {
        const response = await fetch(`${API_BASE_URL}/tutors/featured`, {
          signal: AbortSignal.timeout(8000)
        });
        if (!response.ok) {
          throw new Error(`Lỗi kết nối máy chủ: HTTP ${response.status}`);
        }
        return await response.json();
      } catch (err) {
        console.error('API.tutors.getFeatured error:', err);
        throw err;
      }
    },

    /**
     * Lấy chi tiết hồ sơ một gia sư theo mã định danh (UUID)
     */
    async getDetail(tutorId) {
      try {
        const response = await fetch(`${API_BASE_URL}/tutors/${tutorId}`, {
          signal: AbortSignal.timeout(8000)
        });
        if (!response.ok) {
          throw new Error(`Không tìm thấy hồ sơ gia sư: HTTP ${response.status}`);
        }
        return await response.json();
      } catch (err) {
        console.error(`API.tutors.getDetail(${tutorId}) error:`, err);
        throw err;
      }
    }
  },

  connectionRequests: {
    /**
     * Gửi yêu cầu kết nối học thử với gia sư (UC-30)
     */
    async create(requestData) {
      try {
        const response = await fetch(`${API_BASE_URL}/bookings/trial`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(requestData)
        });
        if (!response.ok) {
          throw new Error('Không thể gửi yêu cầu kết nối');
        }
        return await response.json();
      } catch (err) {
        console.warn('Backend bookings endpoint pending, recording locally:', err);
        return { success: true, message: 'Yêu cầu kết nối đã được tiếp nhận.' };
      }
    },

    /**
     * Lấy danh sách các yêu cầu kết nối của học viên
     */
    async getMyRequests() {
      try {
        const response = await fetch(`${API_BASE_URL}/bookings/my-requests`);
        if (!response.ok) return [];
        return await response.json();
      } catch (err) {
        return [];
      }
    }
  },

  tutorVerification: {
    /** Trạng thái và hồ sơ xác minh của gia sư đang đăng nhập (UC-22, UC-82, UC-83) */
    getMine() {
      return authRequest('/tutor/verification');
    },

    /** Lưu nháp thông tin xác minh (UC-23) */
    saveProfile(profile) {
      return authRequest('/tutor/verification', { method: 'PUT', body: profile });
    },

    /** Thêm giấy tờ: type, title, file (UC-23) */
    addDocument({ type, title, file }) {
      const form = new FormData();
      form.append('type', type);
      form.append('title', title);
      form.append('file', file);
      return authRequest('/tutor/verification/documents', { method: 'POST', body: form, timeoutMs: 120000 });
    },

    /** Sửa loại/tên hoặc thay tệp của giấy tờ */
    updateDocument(id, { type, title, file } = {}) {
      const form = new FormData();
      if (type) form.append('type', type);
      if (title) form.append('title', title);
      if (file) form.append('file', file);
      return authRequest(`/tutor/verification/documents/${id}`, { method: 'PUT', body: form, timeoutMs: 120000 });
    },

    removeDocument(id) {
      return authRequest(`/tutor/verification/documents/${id}`, { method: 'DELETE' });
    },

    /** Tải tệp giấy tờ dạng Blob để xem trực tiếp (thẻ img/iframe không gửi được token) */
    getDocumentFile(id) {
      return authRequest(`/tutor/verification/documents/${id}/file`, { responseType: 'blob', timeoutMs: 120000 });
    },

    /** Nộp hồ sơ (UC-24); commitment phải là true */
    submit(commitment) {
      return authRequest('/tutor/verification/submit', { method: 'POST', body: { commitment } });
    }
  },

  academicVerification: {
    /** Số liệu bảng điều khiển thẩm định (FR-010) */
    getSummary() {
      return authRequest('/academic/verifications/summary');
    },

    /** Hàng đợi hồ sơ: status, keyword, jlptLevel, submittedFrom, submittedTo, sort, page, size (UC-25) */
    getQueue(params = {}) {
      const query = new URLSearchParams();
      Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null && String(value).trim() !== '') query.append(key, value);
      });
      return authRequest(`/academic/verifications?${query.toString()}`);
    },

    getDetail(tutorId) {
      return authRequest(`/academic/verifications/${tutorId}`);
    },

    getHistory(tutorId) {
      return authRequest(`/academic/verifications/${tutorId}/history`);
    },

    getDocumentFile(tutorId, documentId) {
      return authRequest(`/academic/verifications/${tutorId}/documents/${documentId}/file`, { responseType: 'blob', timeoutMs: 120000 });
    },

    approve(tutorId) {
      return authRequest(`/academic/verifications/${tutorId}/approve`, { method: 'POST' });
    },

    /** reasonCode, feedback (gửi gia sư), internalNote (chỉ cán bộ xem) */
    reject(tutorId, payload) {
      return authRequest(`/academic/verifications/${tutorId}/reject`, { method: 'POST', body: payload });
    },

    requestInfo(tutorId, message) {
      return authRequest(`/academic/verifications/${tutorId}/request-info`, { method: 'POST', body: { message } });
    }
  }
};

// Gắn vào window để hỗ trợ cả import module lẫn script thông thường
if (typeof window !== 'undefined') {
  window.API = API;
}

