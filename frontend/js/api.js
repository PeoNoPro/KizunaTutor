/**
 * KizunaTutor - Tầng giao tiếp REST API dùng chung
 * Tập trung toàn bộ logic gọi Backend Spring Boot tại một nơi duy nhất.
 */

import { API_CONFIG } from './config/api.config.js';

const API_BASE_URL = API_CONFIG.BASE_URL;

export const API = {
  tutors: {
    /**
     * Tìm kiếm và lọc danh sách gia sư (FE-04)
     * @param {Object} params - Các tiêu chí lọc (keyword, jlptLevel, minPrice, maxPrice, sortBy, page, size)
     */
    async search(params = {}) {
      const queryParams = new URLSearchParams();
      
      if (params.keyword) queryParams.set('keyword', params.keyword.trim());
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
  }
};
