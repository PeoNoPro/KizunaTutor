/**
 * KizunaTutor - API Configuration
 * Compliant with FRONTEND_RULES.md & AGENTS.md
 */

const API_CONFIG = {
  BASE_URL: 'http://localhost:8080/api',
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
    }
  }
};

window.API_CONFIG = API_CONFIG;
