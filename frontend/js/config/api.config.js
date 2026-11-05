/**
 * KizunaTutor - API Configuration
 * Compliant with FRONTEND_RULES.md & AGENTS.md
 */

export const API_CONFIG = {
  BASE_URL: 'http://localhost:8085/api',
  TIMEOUT_MS: 15000,
  ENDPOINTS: {
    AUTH: {
      LOGIN: '/auth/login',
      REGISTER: '/auth/register',
      VERIFY_OTP: '/auth/verify-otp'
    },
    TUTORS: {
      SEARCH: '/tutors/search',
      FEATURED: '/tutors/featured',
      DETAIL: (id) => `/tutors/${id}`
    },
    BOOKINGS: {
      CREATE: '/bookings/trial',
      MY_REQUESTS: '/bookings/my-requests'
    },
    TUTOR_VERIFICATION: {
      ME: '/tutor/verification',
      DOCUMENTS: '/tutor/verification/documents',
      DOCUMENT: (id) => `/tutor/verification/documents/${id}`,
      DOCUMENT_FILE: (id) => `/tutor/verification/documents/${id}/file`,
      SUBMIT: '/tutor/verification/submit'
    },
    ACADEMIC_VERIFICATION: {
      SUMMARY: '/academic/verifications/summary',
      QUEUE: '/academic/verifications',
      DETAIL: (tutorId) => `/academic/verifications/${tutorId}`,
      HISTORY: (tutorId) => `/academic/verifications/${tutorId}/history`,
      DOCUMENT_FILE: (tutorId, docId) => `/academic/verifications/${tutorId}/documents/${docId}/file`,
      APPROVE: (tutorId) => `/academic/verifications/${tutorId}/approve`,
      REJECT: (tutorId) => `/academic/verifications/${tutorId}/reject`,
      REQUEST_INFO: (tutorId) => `/academic/verifications/${tutorId}/request-info`
    },
    TUTOR_MEMBERSHIP: {
      PLANS: '/tutor/memberships/plans',
      ME: '/tutor/memberships/me',
      PAYMENTS: '/tutor/memberships/payments',
      VNPAY_RETURN: '/tutor/memberships/payments/vnpay-return'
    },
    MANAGER_MEMBERSHIP: {
      PLANS: '/manager/membership-plans',
      PLAN: (planId) => `/manager/membership-plans/${planId}`,
      TUTOR_MEMBERSHIPS: '/manager/tutor-memberships',
      TUTOR_MEMBERSHIP: (tutorId) => `/manager/tutor-memberships/${tutorId}`
    },
    ADMIN_PAYMENTS: {
      TRANSACTIONS: '/admin/payment-transactions',
      TRANSACTION: (id) => `/admin/payment-transactions/${id}`,
      GATEWAY_CHECK: (id) => `/admin/payment-transactions/${id}/gateway-check`,
      NOTES: (id) => `/admin/payment-transactions/${id}/notes`
    }
  }
};

window.API_CONFIG = API_CONFIG;

