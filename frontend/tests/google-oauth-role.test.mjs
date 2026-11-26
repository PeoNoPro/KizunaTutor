import { test } from 'node:test';
import assert from 'node:assert/strict';
import { buildGoogleOAuthUrl, resolveLoginErrorMessage } from '../js/auth-oauth-helpers.js';

test('buildGoogleOAuthUrl gắn đúng role ROLE_TUTOR và ROLE_LEARNER', () => {
  assert.equal(buildGoogleOAuthUrl('ROLE_TUTOR'), '/oauth2/authorization/google?role=ROLE_TUTOR');
  assert.equal(buildGoogleOAuthUrl('ROLE_LEARNER'), '/oauth2/authorization/google?role=ROLE_LEARNER');
});

test('buildGoogleOAuthUrl chặn role ngoài whitelist và mặc định ROLE_LEARNER', () => {
  assert.equal(buildGoogleOAuthUrl('ROLE_ADMIN'), '/oauth2/authorization/google?role=ROLE_LEARNER');
  assert.equal(buildGoogleOAuthUrl(''), '/oauth2/authorization/google?role=ROLE_LEARNER');
  assert.equal(buildGoogleOAuthUrl(null), '/oauth2/authorization/google?role=ROLE_LEARNER');
});

test('resolveLoginErrorMessage trả đúng thông báo cho các mã lỗi OAuth2 mới và cũ', () => {
  assert.match(resolveLoginErrorMessage('account_blocked'), /tạm khóa/i);
  assert.match(resolveLoginErrorMessage('oauth2_not_configured'), /Client ID/i);
  assert.match(resolveLoginErrorMessage('email_not_provided'), /email/i);
  assert.match(resolveLoginErrorMessage('oauth2_failure'), /Google/i);
  assert.match(resolveLoginErrorMessage('oauth2_process_error'), /máy chủ/i);
});
