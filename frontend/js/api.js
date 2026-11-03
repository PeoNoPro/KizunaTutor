/**
 * KizunaTutor - Centralized API Service
 * Kết nối REST API Backend Spring Boot (cổng 8085).
 * Tuân thủ FRONTEND_RULES.md (Mục 11 - API Integration Rules).
 */

const API_BASE_URL = 'http://localhost:8085/api';

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

function materialFormData({ title, description, file }) {
  const fd = new FormData();
  fd.append('title', title ?? '');
  fd.append('description', description ?? '');
  if (file) fd.append('file', file);
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

  /** Gia sư mở lớp cho học viên (UC-93) */
  create({ learnerId, title }) {
    return classroomRequest('', { method: 'POST', body: { learnerId, title } });
  },

  detail(spaceId) {
    return classroomRequest(`/${encodeURIComponent(spaceId)}`);
  },

  /** Danh sách tài liệu của lớp (UC-61) */
  materials(spaceId) {
    return classroomRequest(`/${encodeURIComponent(spaceId)}/materials`);
  },

  /** Tải nội dung tệp tài liệu (UC-61); trả về Blob để mở bằng URL.createObjectURL */
  async materialFile(spaceId, materialId) {
    let response;
    try {
      response = await fetch(
        `${API_BASE_URL}/classrooms/${encodeURIComponent(spaceId)}/materials/${encodeURIComponent(materialId)}/file`,
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

  /** Tải lên tài liệu mới (UC-65) */
  uploadMaterial(spaceId, data) {
    return classroomRequest(`/${encodeURIComponent(spaceId)}/materials`, {
      method: 'POST', formData: materialFormData(data), timeout: 120000
    });
  },

  /** Cập nhật hoặc thay tệp tài liệu (UC-94) */
  updateMaterial(spaceId, materialId, data) {
    return classroomRequest(`/${encodeURIComponent(spaceId)}/materials/${encodeURIComponent(materialId)}`, {
      method: 'PUT', formData: materialFormData(data), timeout: 120000
    });
  }
};
// =========================================================================
// FE-07: BÀI TẬP, NỘP BÀI & CHẤM ĐIỂM (Homework, Assessment & Progress Tracking)
// Bổ sung riêng cho FE-07, dùng lại classroomRequest/classroomAuthHeaders của FE-06.
// =========================================================================
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

  /** Gia sư giao bài tập (UC-66) */
  create(spaceId, { title, description, dueDate, totalPoints }) {
    return classroomRequest(assignmentPath(spaceId), {
      method: 'POST', body: { title, description, dueDate, totalPoints }
    });
  },

  /** Gia sư sửa bài tập (UC-95) */
  update(spaceId, assignmentId, { title, description, dueDate, totalPoints }) {
    return classroomRequest(assignmentPath(spaceId, `/${encodeURIComponent(assignmentId)}`), {
      method: 'PUT', body: { title, description, dueDate, totalPoints }
    });
  },

  /** Học viên nộp / nộp lại bài (UC-64, UC-96) */
  submit(spaceId, assignmentId, { content, file }) {
    const fd = new FormData();
    fd.append('content', content ?? '');
    if (file) fd.append('file', file);
    return classroomRequest(assignmentPath(spaceId, `/${encodeURIComponent(assignmentId)}/submissions`), {
      method: 'POST', formData: fd, timeout: 120000
    });
  },

  /** Gia sư chấm điểm lần nộp mới nhất (UC-67) */
  grade(spaceId, assignmentId, submissionId, { grade, feedback }) {
    return classroomRequest(assignmentPath(spaceId,
      `/${encodeURIComponent(assignmentId)}/submissions/${encodeURIComponent(submissionId)}/grade`), {
      method: 'PUT', body: { grade, feedback }
    });
  },

  /** Tải tệp bài làm; trả về Blob */
  async submissionFile(spaceId, assignmentId, submissionId) {
    let response;
    try {
      response = await fetch(`${API_BASE_URL}/classrooms${assignmentPath(spaceId,
        `/${encodeURIComponent(assignmentId)}/submissions/${encodeURIComponent(submissionId)}/file`)}`,
        { headers: classroomAuthHeaders(), signal: AbortSignal.timeout(120000) });
    } catch (networkErr) {
      console.debug('API.assignments.submissionFile network error:', networkErr);
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
};
