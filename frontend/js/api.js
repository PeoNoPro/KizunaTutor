/**
 * KizunaTutor - Tầng giao tiếp REST API dùng chung
 * Tập trung toàn bộ logic gọi Backend Spring Boot tại một nơi duy nhất.
 */

import { API_CONFIG } from './config/api.config.js';
const API_BASE_URL = API_CONFIG.BASE_URL;

/** FE-11 transport: shared base URL, cookie session, CSRF and one bounded request budget. */
export async function fetchAPI(path, { method = 'GET', body, csrfPath = '/admin/operations/csrf' } = {}) {
  if (!path.startsWith('/') || path.startsWith('//')) throw new Error('Đường dẫn chưa hợp lệ.');
  const deadline = Date.now() + 12000;
  const headers = { 'Content-Type': 'application/json' };
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    const csrf = await request(csrfPath, { method: 'GET' }, deadline);
    headers[csrf.headerName] = csrf.token;
  }
  return request(path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) }, deadline);
}

/** FE-10 multipart shares the session/CSRF transport; browser supplies the boundary. */
export async function fetchMultipartAPI(path, payload, files = []) {
  if (!path.startsWith('/v1/tickets') || path.startsWith('//')) throw new Error('Đường dẫn chưa hợp lệ.');
  const deadline = Date.now() + 40000;
  const csrf = await request('/v1/tickets/csrf', { method: 'GET' }, deadline);
  const body = new FormData();
  body.append('payload', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
  for (const file of files) body.append('files', file);
  return request(path, { method: 'POST', headers: { [csrf.headerName]: csrf.token }, body }, deadline);
}

async function request(path, options, deadline) {
  try {
    const response = await fetch(API_CONFIG.BASE_URL + path, {
      ...options, credentials: 'include',
      signal: AbortSignal.timeout(Math.max(1, deadline - Date.now()))
    });
    const result = await response.json();
    if (!response.ok || result.success === false) {
      const error = new Error(result.fieldErrors?.length ? result.fieldErrors.map(f => f.message).join(' ') :
        (result.message || 'Chưa thực hiện được thao tác. Vui lòng thử lại.'));
      error.status = response.status; throw error;
    }
    return result.data;
  } catch (error) {
    if (error.status) throw error;
    throw new Error('Chưa kết nối được hệ thống. Vui lòng thử lại sau.');
  }
}

const AUTH_TOKEN_KEY = 'kizuna_token';
const DEFAULT_ERROR_MESSAGE = 'Không thể kết nối đến máy chủ. Vui lòng thử lại sau.';

/**
 * Lỗi API đã được ánh xạ sang thông điệp tiếng Việt từ backend (code, message, fields, missingDocuments).
 */
export class ApiError extends Error {
  constructor({ status = 0, code = 'NETWORK_ERROR', message = DEFAULT_ERROR_MESSAGE, fields = {}, fieldErrors = [], missingDocuments = [] } = {}) {
    super(message);
    this.status = status;
    this.code = code;
    this.fields = fields || {};
    this.fieldErrors = fieldErrors || [];
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
  } catch (e) { }
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
    } catch (e) { }
    throw new ApiError({
      status: response.status,
      code: data.code || 'HTTP_ERROR',
      message: data.message || (response.status === 413
        ? 'Tệp không hợp lệ. Vui lòng chọn tệp PDF, PNG hoặc JPG có dung lượng tối đa 10MB.'
        : 'Hệ thống đang bận, bạn vui lòng thử lại sau.'),
      fields: data.fields || data.details,
      fieldErrors: Array.isArray(data.fieldErrors) ? data.fieldErrors : [],
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
    },

    /**
     * Khung giờ còn trống của gia sư trong một tuần (chỉ tuần hiện tại đến 3 tuần sau).
     * @param {string} tutorId
     * @param {string|null} weekStart - "YYYY-MM-DD" (Thứ Hai); null = tuần hiện tại
     */
    async getWeeklySchedule(tutorId, weekStart = null, minDuration = null) {
      const params = new URLSearchParams();
      if (weekStart) params.set('weekStart', weekStart);
      if (minDuration) params.set('minDuration', minDuration);
      const qs = params.toString() ? `?${params.toString()}` : '';
      const response = await fetch(`${API_BASE_URL}/tutors/${tutorId}/weekly-schedule${qs}`, {
        headers: { 'Accept': 'application/json' },
        signal: AbortSignal.timeout(8000)
      });
      if (!response.ok) {
        let message = `Không tải được lịch rảnh: HTTP ${response.status}`;
        try {
          const body = await response.json();
          if (body && body.message) message = body.message;
        } catch (_) { /* giữ thông báo mặc định */ }
        const err = new Error(message);
        err.status = response.status;
        throw err;
      }
      return response.json();
    }
  },

  /**
   * Kết nối gia sư trực tiếp (không cần học thử): học viên chọn các ca hằng tuần trong lịch rảnh, gia sư duyệt.
   */
  connectionRequests: {
    async _call(path, { method = 'GET', body, timeout = 10000 } = {}) {
      const token = localStorage.getItem('kizuna_token') || localStorage.getItem('token');
      const response = await fetch(`${API_BASE_URL}/v1/connection-requests${path}`, {
        method,
        headers: {
          'Accept': 'application/json',
          ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
          ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        },
        ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
        signal: AbortSignal.timeout(timeout)
      });
      const resData = await response.json().catch(() => ({}));
      if (!response.ok) {
        const err = new Error(resData.message || `Lỗi máy chủ (${response.status})`);
        err.status = response.status;
        err.data = resData;
        throw err;
      }
      return resData.data || resData;
    },

    /** payload: { tutorId, slots: [{ dayOfWeek, startTime: 'HH:mm', durationMinutes }], currentLevel, learningGoals, initialMessage } */
    create(payload) {
      return this._call('', { method: 'POST', body: payload });
    },

    /**
     * Xem trước lịch học dự kiến: { tutorId, slots, startDate: 'YYYY-MM-DD', totalSessions }
     * → { sessions: [{ date, dayName, startTime, endTime, available, reason }], conflictCount, suggestedStartWeek, ... }
     */
    preview(payload) {
      return this._call('/preview', { method: 'POST', body: payload, timeout: 8000 });
    },

    /** Danh sách yêu cầu kết nối trực tiếp của tôi (học viên hoặc gia sư). */
    getMyRequests(status = 'ALL', page = 0, size = 20) {
      const query = new URLSearchParams({ status, page, size });
      return this._call(`/my-requests?${query.toString()}`, { timeout: 8000 });
    },

    /** Gia sư phản hồi: { action: 'ACCEPT' | 'DECLINE', message } */
    respond(requestId, payload) {
      return this._call(`/${requestId}/respond`, { method: 'PUT', body: payload });
    },

    /** Học viên hủy yêu cầu đang chờ. */
    cancel(requestId) {
      return this._call(`/${requestId}/cancel`, { method: 'PUT' });
    }
  },

  trialBookings: {
    /**
     * Gửi yêu cầu đăng ký học thử 1-1 với gia sư (FE-05)
     */
    async create(requestData) {
      const token = localStorage.getItem('kizuna_token') || localStorage.getItem('token');
      try {
        const response = await fetch(`${API_BASE_URL}/v1/trial-bookings`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            ...(token ? { 'Authorization': `Bearer ${token}` } : {})
          },
          body: JSON.stringify(requestData),
          signal: AbortSignal.timeout(10000)
        });

        const resData = await response.json().catch(() => ({}));
        if (!response.ok) {
          const err = new Error(resData.message || `Lỗi máy chủ (${response.status})`);
          err.status = response.status;
          err.data = resData;
          throw err;
        }
        return resData.data || resData;
      } catch (err) {
        console.error('API.trialBookings.create error:', err);
        throw err;
      }
    },

    /**
     * Lấy danh sách yêu cầu học thử của tôi (FE-05)
     */
    async getMyRequests(status = 'ALL', page = 0, size = 10) {
      const token = localStorage.getItem('kizuna_token') || localStorage.getItem('token');
      const queryParams = new URLSearchParams({
        status,
        page,
        size
      });
      try {
        const response = await fetch(`${API_BASE_URL}/v1/trial-bookings/my-requests?${queryParams.toString()}`, {
          headers: {
            'Accept': 'application/json',
            ...(token ? { 'Authorization': `Bearer ${token}` } : {})
          },
          signal: AbortSignal.timeout(8000)
        });

        const resData = await response.json().catch(() => ({}));
        if (!response.ok) {
          const err = new Error(resData.message || `Lỗi máy chủ (${response.status})`);
          err.status = response.status;
          throw err;
        }
        return resData.data || resData;
      } catch (err) {
        console.error('API.trialBookings.getMyRequests error:', err);
        throw err;
      }
    },

    /**
     * Xem chi tiết buổi học thử (FE-05)
     */
    async getDetail(bookingId) {
      const token = localStorage.getItem('kizuna_token') || localStorage.getItem('token');
      try {
        const response = await fetch(`${API_BASE_URL}/v1/trial-bookings/${bookingId}`, {
          headers: {
            'Accept': 'application/json',
            ...(token ? { 'Authorization': `Bearer ${token}` } : {})
          },
          signal: AbortSignal.timeout(8000)
        });

        const resData = await response.json().catch(() => ({}));
        if (!response.ok) {
          const err = new Error(resData.message || `Lỗi máy chủ (${response.status})`);
          err.status = response.status;
          throw err;
        }
        return resData.data || resData;
      } catch (err) {
        console.error(`API.trialBookings.getDetail(${bookingId}) error:`, err);
        throw err;
      }
    },

    /**
     * Gia sư phản hồi yêu cầu học thử: Chấp nhận hoặc Từ chối (FE-05)
     */
    async respond(bookingId, payload) {
      const token = localStorage.getItem('kizuna_token') || localStorage.getItem('token');
      try {
        const response = await fetch(`${API_BASE_URL}/v1/trial-bookings/${bookingId}/respond`, {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            ...(token ? { 'Authorization': `Bearer ${token}` } : {})
          },
          body: JSON.stringify(payload),
          signal: AbortSignal.timeout(10000)
        });

        const resData = await response.json().catch(() => ({}));
        if (!response.ok) {
          const err = new Error(resData.message || `Lỗi máy chủ (${response.status})`);
          err.status = response.status;
          throw err;
        }
        return resData.data || resData;
      } catch (err) {
        console.error(`API.trialBookings.respond(${bookingId}) error:`, err);
        throw err;
      }
    },

    /**
     * Hủy yêu cầu học thử (FE-05)
     */
    async cancel(bookingId, payload) {
      const token = localStorage.getItem('kizuna_token') || localStorage.getItem('token');
      try {
        const response = await fetch(`${API_BASE_URL}/v1/trial-bookings/${bookingId}/cancel`, {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            ...(token ? { 'Authorization': `Bearer ${token}` } : {})
          },
          body: JSON.stringify(payload),
          signal: AbortSignal.timeout(10000)
        });

        const resData = await response.json().catch(() => ({}));
        if (!response.ok) {
          const err = new Error(resData.message || `Lỗi máy chủ (${response.status})`);
          err.status = response.status;
          throw err;
        }
        return resData.data || resData;
      } catch (err) {
        console.error(`API.trialBookings.cancel(${bookingId}) error:`, err);
        throw err;
      }
    },
    /** Đổi lịch / dời lịch học thử sang khung giờ mới (SRS 5.3) */
    async reschedule(bookingId, payload) {
      const token = localStorage.getItem('kizuna_token') || localStorage.getItem('token');
      try {
        const response = await fetch(`${API_BASE_URL}/v1/trial-bookings/${bookingId}/reschedule`, {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
            ...(token ? { 'Authorization': `Bearer ${token}` } : {})
          },
          body: JSON.stringify(payload),
          signal: AbortSignal.timeout(10000)
        });
        const resData = await response.json().catch(() => ({}));
        if (!response.ok) {
          const err = new Error(resData.message || `Lỗi máy chủ (${response.status})`);
          err.status = response.status;
          throw err;
        }
        return resData.data || resData;
      } catch (err) {
        console.error(`API.trialBookings.reschedule(${bookingId}) error:`, err);
        throw err;
      }
    },
    /** Xác nhận hoàn thành buổi học thử */
    async complete(bookingId) {
      const token = localStorage.getItem('kizuna_token') || localStorage.getItem('token');
      try {
        const response = await fetch(`${API_BASE_URL}/v1/trial-bookings/${bookingId}/complete`, {
          method: 'PUT',
          headers: {
            'Accept': 'application/json',
            ...(token ? { 'Authorization': `Bearer ${token}` } : {})
          },
          signal: AbortSignal.timeout(10000)
        });
        const resData = await response.json().catch(() => ({}));
        if (!response.ok) {
          const err = new Error(resData.message || `Lỗi máy chủ (${response.status})`);
          err.status = response.status;
          throw err;
        }
        return resData.data || resData;
      } catch (err) {
        console.error(`API.trialBookings.complete(${bookingId}) error:`, err);
        throw err;
      }
    },
    /** Gia sư đồng ý kết nối sau khi buổi học thử đã hoàn thành (sau đó tạo lớp ở trang Lớp học FE-06) */
    async agreeConnection(bookingId) {
      const token = localStorage.getItem('kizuna_token') || localStorage.getItem('token');
      try {
        const response = await fetch(`${API_BASE_URL}/v1/trial-bookings/${bookingId}/agree-connection`, {
          method: 'PUT',
          headers: {
            'Accept': 'application/json',
            ...(token ? { 'Authorization': `Bearer ${token}` } : {})
          },
          signal: AbortSignal.timeout(10000)
        });
        const resData = await response.json().catch(() => ({}));
        if (!response.ok) {
          const err = new Error(resData.message || 'Không thể đồng ý kết nối lúc này. Vui lòng thử lại.');
          err.status = response.status;
          throw err;
        }
        return resData.data || resData;
      } catch (err) {
        console.error(`API.trialBookings.agreeConnection(${bookingId}) error:`, err);
        throw err;
      }
    }
  },
  tutorSchedule: {
    /** Lấy danh sách ca rảnh giảng dạy của gia sư */
    async getMySchedule() {
      const token = localStorage.getItem('kizuna_token') || localStorage.getItem('token');
      try {
        const response = await fetch(`${API_BASE_URL}/v1/tutor/availability`, {
          headers: {
            'Accept': 'application/json',
            ...(token ? { 'Authorization': `Bearer ${token}` } : {})
          },
          signal: AbortSignal.timeout(8000)
        });
        const resData = await response.json().catch(() => ({}));
        if (!response.ok) {
          const err = new Error(resData.message || `Lỗi máy chủ (${response.status})`);
          err.status = response.status;
          throw err;
        }
        return resData.data || resData;
      } catch (err) {
        console.error('API.tutorSchedule.getMySchedule error:', err);
        throw err;
      }
    },

    /** Thêm ca rảnh mới */
    async addSlot(payload) {
      const token = localStorage.getItem('kizuna_token') || localStorage.getItem('token');
      try {
        const response = await fetch(`${API_BASE_URL}/v1/tutor/availability`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
            ...(token ? { 'Authorization': `Bearer ${token}` } : {})
          },
          body: JSON.stringify(payload),
          signal: AbortSignal.timeout(10000)
        });
        const resData = await response.json().catch(() => ({}));
        if (!response.ok) {
          const err = new Error(resData.message || `Lỗi máy chủ (${response.status})`);
          err.status = response.status;
          throw err;
        }
        return resData.data || resData;
      } catch (err) {
        console.error('API.tutorSchedule.addSlot error:', err);
        throw err;
      }
    },

    /** Xóa ca rảnh */
    async removeSlot(availabilityId) {
      const token = localStorage.getItem('kizuna_token') || localStorage.getItem('token');
      try {
        const response = await fetch(`${API_BASE_URL}/v1/tutor/availability/${availabilityId}`, {
          method: 'DELETE',
          headers: {
            'Accept': 'application/json',
            ...(token ? { 'Authorization': `Bearer ${token}` } : {})
          },
          signal: AbortSignal.timeout(10000)
        });
        const resData = await response.json().catch(() => ({}));
        if (!response.ok) {
          const err = new Error(resData.message || `Lỗi máy chủ (${response.status})`);
          err.status = response.status;
          throw err;
        }
        return resData.data || resData;
      } catch (err) {
        console.error(`API.tutorSchedule.removeSlot(${availabilityId}) error:`, err);
        throw err;
      }
    },

    /** Bật/tắt ca rảnh */
    async toggleSlot(availabilityId, active) {
      const token = localStorage.getItem('kizuna_token') || localStorage.getItem('token');
      try {
        const response = await fetch(`${API_BASE_URL}/v1/tutor/availability/${availabilityId}/toggle?active=${active}`, {
          method: 'PATCH',
          headers: {
            'Accept': 'application/json',
            ...(token ? { 'Authorization': `Bearer ${token}` } : {})
          },
          signal: AbortSignal.timeout(10000)
        });
        const resData = await response.json().catch(() => ({}));
        if (!response.ok) {
          const err = new Error(resData.message || `Lỗi máy chủ (${response.status})`);
          err.status = response.status;
          throw err;
        }
        return resData.data || resData;
      } catch (err) {
        console.error(`API.tutorSchedule.toggleSlot(${availabilityId}) error:`, err);
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
    },

    /** Hồ sơ đã phê duyệt gửi yêu cầu cập nhật: { highestJlptLevel, note } */
    submitProfileUpdate(payload) {
      return authRequest('/tutor/verification/profile-update', { method: 'POST', body: payload });
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
    },

    /** Chấp nhận yêu cầu cập nhật hồ sơ của gia sư đã phê duyệt */
    approveProfileUpdate(tutorId) {
      return authRequest(`/academic/verifications/${tutorId}/profile-update/approve`, { method: 'POST' });
    },

    /** Không chấp nhận yêu cầu cập nhật; feedback gửi gia sư */
    rejectProfileUpdate(tutorId, feedback) {
      return authRequest(`/academic/verifications/${tutorId}/profile-update/reject`, { method: 'POST', body: { feedback } });
    }
  },

  // ===== Bài đăng gia sư (cần gói hội viên còn hạn) =====
  tutorPost: {
    /** { canPost, blockedReason, membershipExpiresOn, post } */
    getMine() {
      return authRequest('/tutor/post');
    },

    /** { title, description, specialties, teachingFormat, hourlyRate } → { message, post } */
    submit(payload) {
      return authRequest('/tutor/post', { method: 'PUT', body: payload });
    }
  },

  // ===== Cán bộ học vụ: duyệt bài đăng gia sư =====
  academicPosts: {
    getSummary() {
      return authRequest('/academic/posts/summary');
    },

    /** status (PENDING | APPROVED | REJECTED | ALL), keyword, page, size */
    list(params = {}) {
      const query = new URLSearchParams();
      Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null && String(value).trim() !== '') query.append(key, value);
      });
      return authRequest(`/academic/posts?${query.toString()}`);
    },

    approve(postId) {
      return authRequest(`/academic/posts/${postId}/approve`, { method: 'POST' });
    },

    reject(postId, reason) {
      return authRequest(`/academic/posts/${postId}/reject`, { method: 'POST', body: { reason } });
    },

    /** Yêu cầu gia sư bổ sung; nội dung yêu cầu gửi trong trường reason */
    requestInfo(postId, message) {
      return authRequest(`/academic/posts/${postId}/request-info`, { method: 'POST', body: { reason: message } });
    }
  },

  // ===== Cán bộ học vụ: lịch sử xét duyệt + hồ sơ cá nhân =====
  academicOfficer: {
    /** action (APPROVE | REJECT | REQUEST_INFO | ALL), keyword, mine, page, size */
    getReviews(params = {}) {
      const query = new URLSearchParams();
      Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null && String(value).trim() !== '') query.append(key, value);
      });
      return authRequest(`/academic/reviews?${query.toString()}`);
    },

    getProfile() {
      return authRequest('/academic/profile');
    },

    /** { fullName, phone } → { message, profile } */
    updateProfile(payload) {
      return authRequest('/academic/profile', { method: 'PUT', body: payload });
    }
  },

  // ===== Cán bộ học vụ: nhập kho học liệu (Kanji, từ vựng, ngữ pháp) từ Excel =====
  academicContent: {
    // FE-13: phản hồi dạng { success, message, data } — các hàm trả về phần data
    /** Số mục đang có + cột cần có của từng loại */
    getSummary() {
      return authRequest('/academic/content-imports/summary').then((res) => res.data);
    },

    /**
     * type: kanji | vocabulary | grammar. Chỉ thêm mục mới, mục đã có giữ nguyên.
     * dryRun = true chỉ kiểm tra, không ghi gì.
     */
    importFile(type, file, { dryRun = true } = {}) {
      const form = new FormData();
      form.append('file', file);
      const query = new URLSearchParams({ dryRun: String(dryRun) });
      return authRequest(`/academic/content-imports/${type}?${query.toString()}`, {
        method: 'POST', body: form, timeoutMs: 300000
      }).then((res) => res.data);
    },

    /** Danh sách học liệu: { keyword, jlpt, sort: LEVEL | NEWEST, page, size } */
    getItems(type, params = {}) {
      return authRequest(`/academic/content-imports/${type}/items?${toQuery(params)}`).then((res) => res.data);
    },

    /** Lịch sử nhập: { type, page, size } */
    getHistory(params = {}) {
      return authRequest(`/academic/content-imports/history?${toQuery(params)}`).then((res) => res.data);
    },

    /** Chi tiết một lần nhập, gồm các mục đã thêm */
    getHistoryDetail(id) {
      return authRequest(`/academic/content-imports/history/${id}`).then((res) => res.data);
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
  },

  // FE-02: Quản lý hồ sơ (specs/002-profile-management/contracts/profile-api.md)
  learningTools: {
    /**
     * Trạng thái mở khóa công cụ học: { allowed, openClassCount, message, loginRequired }.
     * Không tự chuyển về trang đăng nhập (khác authRequest) vì được gọi ngầm cả ở trang chủ:
     * chưa đăng nhập hoặc phiên hết hạn thì trả loginRequired = true.
     */
    async getAccess() {
      const loginRequired = { allowed: false, openClassCount: 0, loginRequired: true };
      const token = getStoredToken();
      if (!token) return loginRequired;
      const response = await fetch(`${API_BASE_URL}/learner/learning-tools/access`, {
        headers: { Authorization: `Bearer ${token}`, Accept: 'application/json' },
        signal: AbortSignal.timeout(8000)
      });
      if (response.status === 401) return loginRequired;
      if (!response.ok) throw new ApiError({ status: response.status });
      const json = await response.json();
      return { ...json.data, loginRequired: false };
    }
  },

  profile: {
    /** Tải ảnh đại diện (JPG, PNG, WEBP, tối đa 5MB); trả { avatarUrl, fileName, size } */
    uploadAvatar(file) {
      const form = new FormData();
      form.append('file', file);
      return authRequest('/profile/avatar', { method: 'POST', body: form, timeoutMs: 60000 }).then((res) => res.data);
    },

    /** Hồ sơ của học viên đang đăng nhập */
    getLearnerProfile() {
      return authRequest('/profile/learner/me').then((res) => res.data);
    },

    /** data: fullName, phone, avatarUrl, currentLevel, targetLevel, learningGoals (email chỉ đọc) */
    updateLearnerProfile(data) {
      return authRequest('/profile/learner/me', { method: 'PUT', body: data }).then((res) => res.data);
    },

    /** Học viên hoặc gia sư đổi mật khẩu: currentPassword, newPassword, confirmPassword */
    changePassword(data) {
      return authRequest('/profile/password', { method: 'PUT', body: data });
    },

    /** Hồ sơ cá nhân và giảng dạy của gia sư đang đăng nhập */
    getTutorProfile() {
      return authRequest('/profile/tutor/me').then((res) => res.data);
    },

    /** Gia sư cập nhật hồ sơ cá nhân và giảng dạy (email chỉ đọc) */
    updateTutorProfile(data) {
      return authRequest('/profile/tutor/me', { method: 'PUT', body: data }).then((res) => res.data);
    },

    /** Quản lý / Quản trị viên tra cứu danh sách người dùng phân trang */
    getUsers(params = {}) {
      const q = toQuery(params);
      return authRequest(`/profile/users${q ? `?${q}` : ''}`).then((res) => res.data);
    },

    /** Quản lý / Quản trị viên xem chi tiết một tài khoản Học viên hoặc Gia sư */
    getUserDetail(userId) {
      return authRequest(`/profile/users/${userId}`).then((res) => res.data);
    },

    /** Quản lý / Quản trị viên cập nhật trạng thái tài khoản kèm lý do bắt buộc */
    updateUserStatus(userId, data) {
      return authRequest(`/profile/users/${userId}/status`, { method: 'PUT', body: data }).then((res) => res.data);
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

// =========================================================================
// FE-06: LỚP HỌC & TÀI LIỆU HỌC TẬP (Classroom & Learning Materials)
// Bổ sung riêng cho FE-06, không thay đổi các nhóm API phía trên.
// =========================================================================
function classroomAuthHeaders() {
  let token = null;
  try {
    token = localStorage.getItem('kizuna_token');
  } catch (e) {
    token = null;
  }
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function classroomRequest(path, { method = 'GET', body, formData, timeout = 15000 } = {}) {
  const headers = { ...classroomAuthHeaders() };
  let payload;
  if (formData) {
    payload = formData;
  } else if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    payload = JSON.stringify(body);
  }

  let response;
  try {
    response = await fetch(`${API_BASE_URL}/classrooms${path}`, {
      method,
      headers,
      body: payload,
      signal: AbortSignal.timeout(timeout)
    });
  } catch (networkErr) {
    console.debug('API.classrooms network error:', networkErr);
    const err = new Error('network');
    err.status = 0;
    err.userMessage = 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối và thử lại.';
    throw err;
  }

  let json = null;
  try {
    json = await response.json();
  } catch (parseErr) {
    json = null;
  }

  if (response.status === 401) {
    window.location.href = '/login';
  }
  if (!response.ok || (json && json.success === false)) {
    const err = new Error('request-failed');
    err.status = response.status;
    err.userMessage = json && typeof json.message === 'string' ? json.message : '';
    err.fieldErrors = json && json.data && json.data.fieldErrors ? json.data.fieldErrors : null;
    throw err;
  }
  return json ? json.data : null;
}

function materialFormData({ title, description, file, files, removeFileIds }) {
  const fd = new FormData();
  fd.append('title', title ?? '');
  fd.append('description', description ?? '');
  (files ? Array.from(files) : (file ? [file] : [])).forEach((f) => fd.append('file', f));
  (removeFileIds || []).forEach((id) => fd.append('removeFileIds', id));
  return fd;
}

API.classrooms = {
  /** Danh sách lớp của gia sư hoặc học viên hiện tại (UC-93, UC-58) */
  list() {
    return classroomRequest('');
  },

  /** Học viên đã kết nối, đủ điều kiện thêm vào lớp (UC-93) */
  eligibleLearners() {
    return classroomRequest('/eligible-learners');
  },

  /** Gia sư tạo lớp cho học viên đã kết nối, kèm lịch học hằng tuần (UC-93) */
  create({ learnerId, title, schedules }) {
    return classroomRequest('', { method: 'POST', body: { learnerId, title, schedules } });
  },

  /**
   * Thời khóa biểu của người đang đăng nhập: lịch hằng tuần của mọi lớp đang mở.
   * Mỗi phần tử: {scheduleId, spaceId, classTitle, partnerName, classStatus, dayOfWeek (1 = Thứ Hai), startTime, endTime, classStartedAt}
   */
  timetable() {
    return classroomRequest('/schedules/me');
  },

  /** Gia sư thêm một buổi học hằng tuần ({dayOfWeek, startTime, endTime}) */
  addSchedule(spaceId, slot) {
    return classroomRequest(`/${encodeURIComponent(spaceId)}/schedules`, { method: 'POST', body: slot });
  },

  /** Gia sư xóa một buổi học (chỉ khi lớp còn từ 2 buổi trở lên) */
  deleteSchedule(spaceId, scheduleId) {
    return classroomRequest(`/${encodeURIComponent(spaceId)}/schedules/${encodeURIComponent(scheduleId)}`, { method: 'DELETE' });
  },

  /** Gia sư sửa thứ/giờ của một buổi học */
  updateSchedule(spaceId, scheduleId, slot) {
    return classroomRequest(`/${encodeURIComponent(spaceId)}/schedules/${encodeURIComponent(scheduleId)}`,
      { method: 'PUT', body: slot });
  },

  detail(spaceId) {
    return classroomRequest(`/${encodeURIComponent(spaceId)}`);
  },

  /** Gia sư đổi tên lớp học */
  rename(spaceId, title) {
    return classroomRequest(`/${encodeURIComponent(spaceId)}`, { method: 'PUT', body: { title } });
  },

  /** Danh sách tài liệu của lớp (UC-61) */
  materials(spaceId) {
    return classroomRequest(`/${encodeURIComponent(spaceId)}/materials`);
  },

  /** Tải nội dung một tệp của tài liệu (UC-61); trả về Blob để mở bằng URL.createObjectURL */
  async materialFile(spaceId, materialId, fileId) {
    let response;
    try {
      response = await fetch(
        `${API_BASE_URL}/classrooms/${encodeURIComponent(spaceId)}/materials/${encodeURIComponent(materialId)}/files/${encodeURIComponent(fileId)}`,
        { headers: classroomAuthHeaders(), signal: AbortSignal.timeout(120000) }
      );
    } catch (networkErr) {
      console.debug('API.classrooms.materialFile network error:', networkErr);
      const err = new Error('network');
      err.status = 0;
      err.userMessage = 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối và thử lại.';
      throw err;
    }
    if (response.status === 401) {
      window.location.href = '/login';
    }
    if (!response.ok) {
      let json = null;
      try {
        json = await response.json();
      } catch (parseErr) {
        json = null;
      }
      const err = new Error('request-failed');
      err.status = response.status;
      err.userMessage = json && typeof json.message === 'string' ? json.message : '';
      throw err;
    }
    return await response.blob();
  },

  /** Tạo tài liệu mới (UC-65): {title, description, files} — một tài liệu gồm 1–5 tệp */
  uploadMaterial(spaceId, data) {
    return classroomRequest(`/${encodeURIComponent(spaceId)}/materials`, {
      method: 'POST', formData: materialFormData(data), timeout: 120000
    });
  },

  /** Cập nhật tài liệu (UC-94): {title, description, files (tệp thêm), removeFileIds (tệp gỡ)} */
  updateMaterial(spaceId, materialId, data) {
    return classroomRequest(`/${encodeURIComponent(spaceId)}/materials/${encodeURIComponent(materialId)}`, {
      method: 'PUT', formData: materialFormData(data), timeout: 120000
    });
  },

  /** Xóa tài liệu khỏi lớp (Delete Learning Material) */
  deleteMaterial(spaceId, materialId) {
    return classroomRequest(`/${encodeURIComponent(spaceId)}/materials/${encodeURIComponent(materialId)}`, {
      method: 'DELETE'
    });
  }
};
// =========================================================================
// FE-07: BÀI TẬP, NỘP BÀI & CHẤM ĐIỂM (Homework, Assessment & Progress Tracking)
// Bổ sung riêng cho FE-07, dùng lại classroomRequest/classroomAuthHeaders của FE-06.
// =========================================================================
function assignmentFormData({ title, description, dueDate, totalPoints, files, removeFileIds }) {
  const fd = new FormData();
  fd.append('title', title ?? '');
  fd.append('description', description ?? '');
  if (dueDate) fd.append('dueDate', dueDate);
  if (totalPoints !== null && totalPoints !== undefined && totalPoints !== '') fd.append('totalPoints', String(totalPoints));
  Array.from(files || []).forEach((f) => fd.append('file', f));
  (removeFileIds || []).forEach((id) => fd.append('removeFileIds', id));
  return fd;
}

function assignmentPath(spaceId, rest = '') {
  return `/${encodeURIComponent(spaceId)}/assignments${rest}`;
}

API.assignments = {
  /** Danh sách bài tập của lớp (UC-63) */
  list(spaceId) {
    return classroomRequest(assignmentPath(spaceId));
  },

  /** Chi tiết bài tập + các lần nộp (UC-63) */
  detail(spaceId, assignmentId) {
    return classroomRequest(assignmentPath(spaceId, `/${encodeURIComponent(assignmentId)}`));
  },

  /**
   * Gia sư giao bài tập (UC-66) — MỘT lần gửi: thông tin bài tập + tệp đề bài (tối đa 5).
   * Máy chủ kiểm tra tất cả; lỗi bất kỳ (kể cả một tệp) thì không lưu gì.
   */
  create(spaceId, { title, description, dueDate, totalPoints, files }) {
    return classroomRequest(assignmentPath(spaceId), {
      method: 'POST', formData: assignmentFormData({ title, description, dueDate, totalPoints, files }), timeout: 300000
    });
  },

  /** Gia sư sửa bài tập (UC-95) — MỘT lần gửi: thông tin + tệp thêm (`files`) + tệp gỡ (`removeFileIds`). */
  update(spaceId, assignmentId, { title, description, dueDate, totalPoints, files, removeFileIds }) {
    return classroomRequest(assignmentPath(spaceId, `/${encodeURIComponent(assignmentId)}`), {
      method: 'PUT',
      formData: assignmentFormData({ title, description, dueDate, totalPoints, files, removeFileIds }),
      timeout: 300000
    });
  },

  /** Học viên nộp bài lần đầu (UC-64): nội dung + nhiều tệp */
  submit(spaceId, assignmentId, { content, files }) {
    const fd = new FormData();
    fd.append('content', content ?? '');
    Array.from(files || []).forEach((f) => fd.append('file', f));
    return classroomRequest(assignmentPath(spaceId, `/${encodeURIComponent(assignmentId)}/submissions`), {
      method: 'POST', formData: fd, timeout: 300000
    });
  },

  /** Học viên sửa bài đã nộp (UC-96): nội dung mới, thêm tệp, gỡ tệp theo id */
  updateSubmission(spaceId, assignmentId, submissionId, { content, files, removeFileIds }) {
    const fd = new FormData();
    fd.append('content', content ?? '');
    Array.from(files || []).forEach((f) => fd.append('file', f));
    (removeFileIds || []).forEach((id) => fd.append('removeFileIds', id));
    return classroomRequest(assignmentPath(spaceId,
      `/${encodeURIComponent(assignmentId)}/submissions/${encodeURIComponent(submissionId)}`), {
      method: 'PUT', formData: fd, timeout: 300000
    });
  },

  /** Gia sư chấm điểm lần nộp mới nhất (UC-67) */
  grade(spaceId, assignmentId, submissionId, { grade, feedback }) {
    return classroomRequest(assignmentPath(spaceId,
      `/${encodeURIComponent(assignmentId)}/submissions/${encodeURIComponent(submissionId)}/grade`), {
      method: 'PUT', body: { grade, feedback }
    });
  },

  /** Tải một tệp đề bài; trả về Blob */
  attachmentFile(spaceId, assignmentId, fileId) {
    return assignmentBlob(assignmentPath(spaceId,
      `/${encodeURIComponent(assignmentId)}/files/${encodeURIComponent(fileId)}`));
  },

  /** Tải một tệp bài làm; trả về Blob */
  submissionFile(spaceId, assignmentId, submissionId, fileId) {
    return assignmentBlob(assignmentPath(spaceId,
      `/${encodeURIComponent(assignmentId)}/submissions/${encodeURIComponent(submissionId)}/files/${encodeURIComponent(fileId)}`));
  }
};

/** Tải nội dung tệp của FE-07 (không bọc JSON); lỗi vẫn đọc thông điệp JSON chuẩn. */
async function assignmentBlob(path) {
  let response;
  try {
    response = await fetch(`${API_BASE_URL}/classrooms${path}`,
      { headers: classroomAuthHeaders(), signal: AbortSignal.timeout(120000) });
  } catch (networkErr) {
    console.debug('API.assignments file network error:', networkErr);
    const err = new Error('network');
    err.status = 0;
    err.userMessage = 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối và thử lại.';
    throw err;
  }
  if (response.status === 401) {
    window.location.href = '/login';
  }
  if (!response.ok) {
    let json = null;
    try {
      json = await response.json();
    } catch (parseErr) {
      json = null;
    }
    const err = new Error('request-failed');
    err.status = response.status;
    err.userMessage = json && typeof json.message === 'string' ? json.message : '';
    throw err;
  }
  return await response.blob();
}
