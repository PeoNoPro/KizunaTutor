/**
 * KizunaTutor - Hồ sơ cá nhân của gia sư trong dashboard gia sư (FE-02, User Story 2), mở từ menu tài khoản.
 * Đặc tả: specs/002-profile-management/. Thông tin giảng dạy qua API.profile (T027-T029); đổi mật khẩu dùng khối chung.
 */

import { mountTutorLayout } from '/components/tutor-layout/tutor-layout.js';
import { mountChangePasswordForm } from '/components/change-password/change-password.js';

mountTutorLayout('profile');
mountChangePasswordForm(document.getElementById('changePasswordSection'));
