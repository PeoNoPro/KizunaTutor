/**
 * Pure helpers dùng chung cho Google OAuth2 trên trang đăng ký & đăng nhập.
 */

const ALLOWED_OAUTH_ROLES = new Set(['ROLE_LEARNER', 'ROLE_TUTOR']);

export function buildGoogleOAuthUrl(role) {
  const safeRole = ALLOWED_OAUTH_ROLES.has(role) ? role : 'ROLE_LEARNER';
  return `/oauth2/authorization/google?role=${encodeURIComponent(safeRole)}`;
}

const LOGIN_ERROR_MESSAGES = {
  email_not_provided: 'Dịch vụ xác thực không cung cấp thông tin email của bạn.',
  oauth2_failure: 'Đăng nhập bằng tài khoản Google thất bại hoặc bạn đã hủy ủy quyền.',
  oauth2_process_error: 'Lỗi xử lý tài khoản Google phía máy chủ.',
  oauth2_not_configured: 'Đăng nhập bằng Google chưa được cấu hình Client ID trên máy chủ.',
  account_blocked: 'Tài khoản của bạn hiện đang bị tạm khóa/cấm. Vui lòng liên hệ bộ phận hỗ trợ.'
};

export function resolveLoginErrorMessage(errorCode) {
  return LOGIN_ERROR_MESSAGES[errorCode] || 'Đăng nhập không thành công. Vui lòng kiểm tra lại.';
}
