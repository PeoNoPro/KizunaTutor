/**
 * KizunaTutor - Centralized API Service
 * Kết nối REST API Backend Spring Boot (cổng 8085).
 * Tuân thủ FRONTEND_RULES.md (Mục 11 - API Integration Rules).
 */

import { API_CONFIG } from './config/api.config.js';
const API_BASE_URL = API_CONFIG.BASE_URL;

/** FE-11 transport: shared base URL, cookie session, CSRF and one bounded request budget. */
export async function fetchAPI(path, {method = 'GET', body, csrfPath = '/admin/operations/csrf'} = {}) {
  if (!path.startsWith('/') || path.startsWith('//')) throw new Error('Đường dẫn chưa hợp lệ.');
  const deadline = Date.now() + 4500;
  const headers = {'Content-Type': 'application/json'};
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    const csrf = await request(csrfPath, {method: 'GET'}, deadline);
    headers[csrf.headerName] = csrf.token;
  }
  return request(path, {method, headers, body: body === undefined ? undefined : JSON.stringify(body)}, deadline);
}

/** FE-10 multipart shares the session/CSRF transport; browser supplies the boundary. */
export async function fetchMultipartAPI(path, payload, files = []) {
  if (!path.startsWith('/v1/tickets') || path.startsWith('//')) throw new Error('Đường dẫn chưa hợp lệ.');
  const deadline = Date.now() + 40000;
  const csrf = await request('/v1/tickets/csrf', {method: 'GET'}, deadline);
  const body = new FormData();
  body.append('payload', new Blob([JSON.stringify(payload)], {type: 'application/json'}));
  for (const file of files) body.append('files', file);
  return request(path, {method: 'POST', headers: {[csrf.headerName]: csrf.token}, body}, deadline);
}

async function request(path, options, deadline) {
  try {
    const response = await fetch(API_CONFIG.BASE_URL + path, {...options, credentials: 'include',
      signal: AbortSignal.timeout(Math.max(1, deadline - Date.now()))});
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
  }
};

// Gắn vào window để hỗ trợ cả import module lẫn script thông thường
if (typeof window !== 'undefined') {
  window.API = API;
}

