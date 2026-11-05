/**
 * KizunaTutor - Tầng giao tiếp REST API dùng chung
 * Tập trung toàn bộ logic gọi Backend Spring Boot tại một nơi duy nhất.
 */

import { API_CONFIG } from './config/api.config.js';

const API_BASE_URL = API_CONFIG.BASE_URL;
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
      fields: data.fields || data.details,
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
     * Tìm kiếm và lọc danh sách gia sư (FE-04)
     * @param {Object} params - Các tiêu chí lọc (keyword, jlptLevel, minPrice, maxPrice, sortBy, page, size)
     */
    async search(params = {}) {
      const queryParams = new URLSearchParams();
      
      if (params.keyword) queryParams.set('keyword', params.keyword.trim());
      if (params.location && params.location.trim() && params.location !== 'all') queryParams.set('location', params.location.trim());
      if (params.jlptLevel && params.jlptLevel !== 'all') queryParams.set('jlptLevel', params.jlptLevel);
      if (params.minPrice) queryParams.set('minPrice', params.minPrice);
      if (params.maxPrice) queryParams.set('maxPrice', params.maxPrice);
      if (params.sortBy) queryParams.set('sortBy', params.sortBy);
      queryParams.set('page', params.page || 0);
      queryParams.set('size', params.size || 10);

      try {
        const response = await fetch(`${API_BASE_URL}/tutors?${queryParams.toString()}`, {
          headers: {
            'Accept': 'application/json'
          },
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
    /**
     * Lấy danh sách các tỉnh/thành phố có gia sư thực tế trong cơ sở dữ liệu (FE-04)
     */
    async getLocations() {
      try {
        const response = await fetch(`${API_BASE_URL}/tutors/locations`, {
          headers: { 'Accept': 'application/json' },
          signal: AbortSignal.timeout(8000)
        });
        if (!response.ok) return [];
        return await response.json();
      } catch (err) {
        console.error('API.tutors.getLocations error:', err);
        return [];
      }
    },

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
     * Tra cứu chi tiết hồ sơ gia sư theo mã tự tăng tutor_code (ví dụ: 10001)
     */
    async getByCode(tutorCode) {
      try {
        const response = await fetch(`${API_BASE_URL}/tutors/code/${tutorCode}`, {
          signal: AbortSignal.timeout(8000)
        });
        if (response.status === 404) {
          return null;
        }
        if (!response.ok) {
          throw new Error(`Không tìm thấy hồ sơ gia sư: HTTP ${response.status}`);
        }
        return await response.json();
      } catch (err) {
        console.error(`API.tutors.getByCode(${tutorCode}) error:`, err);
        throw err;
      }
    },

    /**
     * Lấy chi tiết hồ sơ một gia sư theo mã định danh UUID (tương thích ngược)
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
          body: JSON.stringify(requestData),
          signal: AbortSignal.timeout(10000)
        });

        if (!response.ok) {
          throw new Error(`Không thể gửi yêu cầu kết nối: HTTP ${response.status}`);
        }

        return await response.json();
      } catch (err) {
        console.error('API.connectionRequests.create error:', err);
        throw err;
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
  },

  // ===== FE-09: Gói hội viên và thanh toán (phản hồi dạng { success, message, data }) =====

  tutorMembership: {
    /** Gói đang mở bán (UC-104) */
    getPlans() {
      return authRequest('/tutor/memberships/plans').then((res) => res.data);
    },

    /** Gói hiện tại, gói gia hạn nối tiếp, lịch sử đăng ký (UC-97) */
    getMine() {
      return authRequest('/tutor/memberships/me').then((res) => res.data);
    },

    /** Tạo giao dịch và nhận đường dẫn thanh toán VNPay */
    createPayment(planId) {
      return authRequest('/tutor/memberships/payments', { method: 'POST', body: { planId } }).then((res) => res.data);
    },

    /** Lịch sử giao dịch của gia sư */
    getPayments(page = 0, size = 10) {
      return authRequest(`/tutor/memberships/payments?page=${page}&size=${size}`).then((res) => res.data);
    },

    /** Kiểm tra lại với cổng một giao dịch đang chờ của chính gia sư (đã trả tiền nhưng chưa xác nhận) */
    checkPayment(transactionId) {
      return authRequest(`/tutor/memberships/payments/${transactionId}/check`, { method: 'POST', timeoutMs: 45000 }).then((res) => res.data);
    },

    /** Mở lại trang thanh toán của giao dịch đang chờ, còn trong hạn 15 phút */
    resumePayment(transactionId) {
      return authRequest(`/tutor/memberships/payments/${transactionId}/resume`, { method: 'POST' }).then((res) => res.data);
    },

    /** Xác nhận kết quả khi VNPay chuyển về; queryString là nguyên phần "?vnp_..." */
    confirmReturn(queryString) {
      const query = String(queryString || '').replace(/^\?/, '');
      return authRequest(`/tutor/memberships/payments/vnpay-return?${query}`, { timeoutMs: 45000 }).then((res) => res.data);
    }
  },

  managerMembership: {
    getPlans() {
      return authRequest('/manager/membership-plans').then((res) => res.data);
    },

    /** planName, durationMonths, price, benefits[] (UC-99, UC-100) */
    createPlan(payload) {
      return authRequest('/manager/membership-plans', { method: 'POST', body: payload });
    },

    /** planName, price, benefits[]; số tháng không đổi được */
    updatePlan(planId, payload) {
      return authRequest(`/manager/membership-plans/${planId}`, { method: 'PUT', body: payload });
    },

    setActive(planId, active) {
      return authRequest(`/manager/membership-plans/${planId}/${active ? 'activate' : 'deactivate'}`, { method: 'POST' });
    },

    /** keyword, planId, status (ACTIVE | EXPIRED), page, size (UC-44) */
    getTutorMemberships(params = {}) {
      return authRequest(`/manager/tutor-memberships?${toQuery(params)}`).then((res) => res.data);
    },

    getTutorMembershipDetail(tutorId) {
      return authRequest(`/manager/tutor-memberships/${tutorId}`).then((res) => res.data);
    }
  },

  adminPayments: {
    /** status, from, to (yyyy-MM-dd), keyword, page, size (UC-98) */
    getTransactions(params = {}) {
      return authRequest(`/admin/payment-transactions?${toQuery(params)}`).then((res) => res.data);
    },

    getDetail(transactionId) {
      return authRequest(`/admin/payment-transactions/${transactionId}`).then((res) => res.data);
    },

    checkWithGateway(transactionId) {
      return authRequest(`/admin/payment-transactions/${transactionId}/gateway-check`, { method: 'POST', timeoutMs: 45000 });
    },

    addNote(transactionId, note) {
      return authRequest(`/admin/payment-transactions/${transactionId}/notes`, { method: 'POST', body: { note } });
    }
  }
};

/** Chuỗi truy vấn bỏ qua giá trị rỗng. */
function toQuery(params = {}) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim() !== '') query.append(key, value);
  });
  return query.toString();
}
