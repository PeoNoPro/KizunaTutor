/**
 * ====================================================================================================
 * KIZUNA TUTOR - AUTH REDIRECT & SESSION MANAGEMENT (authRedirect.js)
 * ====================================================================================================
 * Quản lý luồng xác thực đăng nhập, lưu trữ JWT Token và điều hướng chính xác 6 Actor:
 * 1. ROLE_ADMIN           -> /admin/dashboard
 * 2. ROLE_MANAGER         -> /manager/dashboard
 * 3. ROLE_CS_STAFF        -> /cs/dashboard
 * 4. ROLE_ACADEMIC_OFFICER -> /academic/dashboard
 * 5. ROLE_TUTOR           -> /tutor/dashboard
 * 6. ROLE_LEARNER         -> /learner/dashboard
 * ====================================================================================================
 */

// BẢNG MÃ HÓA ROUTE ĐIỀU HƯỚNG TƯƠNG ỨNG 6 ACTOR
const DASHBOARD_ROUTES = {
    ROLE_ADMIN: '/admin/dashboard',
    ROLE_MANAGER: '/manager/dashboard',
    ROLE_CS_STAFF: '/cs/dashboard',
    ROLE_ACADEMIC_OFFICER: '/academic/dashboard',
    ROLE_TUTOR: '/tutor/dashboard',
    ROLE_LEARNER: '/learner/dashboard',
    ROLE_STUDENT: '/learner/dashboard'
};

const STORAGE_KEYS = {
    TOKEN: 'kizuna_token',
    USER: 'kizuna_user'
};

/**
 * Lưu trữ phiên đăng nhập vào LocalStorage
 */
function setAuthSession(token, user) {
    if (token) {
        localStorage.setItem(STORAGE_KEYS.TOKEN, token);
    }
    if (user) {
        localStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(user));
    }
}

/**
 * Lấy JWT Token từ LocalStorage
 */
function getAuthToken() {
    return localStorage.getItem(STORAGE_KEYS.TOKEN);
}

/**
 * Lấy thông tin User từ LocalStorage
 */
function getAuthUser() {
    const raw = localStorage.getItem(STORAGE_KEYS.USER);
    if (!raw) return null;
    try {
        return JSON.parse(raw);
    } catch (e) {
        console.error('Lỗi phân tích JSON User từ LocalStorage:', e);
        return null;
    }
}

/**
 * Xóa sạch phiên đăng nhập (Token và User)
 */
function clearAuthSession() {
    localStorage.removeItem(STORAGE_KEYS.TOKEN);
    localStorage.removeItem(STORAGE_KEYS.USER);
    // Xóa cookie nếu có
    document.cookie = "access_token=; path=/; max-age=0; expires=Thu, 01 Jan 1970 00:00:00 GMT";
}

/**
 * Xử lý sự kiện gửi form Đăng nhập qua AJAX REST API
 */
async function handleLoginSubmit(event) {
    if (event) event.preventDefault();

    const form = document.getElementById('loginForm');
    const emailInput = document.getElementById('loginEmail');
    const passwordInput = document.getElementById('loginPassword');
    const submitBtn = document.getElementById('btnLoginSubmit');
    const alertBox = document.getElementById('loginAlertBox');

    if (!emailInput || !passwordInput) return;

    const email = emailInput.value.trim();
    const password = passwordInput.value;

    if (!email || !password) {
        showLoginAlert('Vui lòng nhập đầy đủ Email và Mật khẩu', 'danger');
        return;
    }

    // Hiển thị trạng thái đang xử lý
    if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.innerHTML = `
            <span class="inline-block animate-spin mr-2">⏳</span>
            <span>Đang xác thực bảo mật...</span>
        `;
    }
    hideLoginAlert();

    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({ email, password })
        });

        const data = await response.json();

        if (!response.ok) {
            const errorMsg = data.message || data.error || 'Đăng nhập không thành công. Vui lòng kiểm tra lại tài khoản.';
            showLoginAlert(errorMsg, 'danger');
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.innerHTML = `<span>Đăng Nhập</span>`;
            }
            return;
        }

        // 1. Lưu thông tin Token và User vào LocalStorage
        setAuthSession(data.token, data.user);

        // 2. Kiểm tra cờ bắt buộc đổi mật khẩu lần đầu (First-time Password Change Enforcement)
        if (data.mustChangePassword || (data.user && data.user.mustChangePassword)) {
            console.log('[KizunaAuth] Tài khoản yêu cầu đổi mật khẩu lần đầu! Hiển thị Modal bắt buộc.');
            showFirstTimeChangePasswordModal(data.token, data.user, password);
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.innerHTML = `<span>Đăng Nhập</span>`;
            }
            return;
        }

        showLoginAlert('Đăng nhập thành công! Đang tự động chuyển hướng...', 'success');

        // 2. Xác định Route điều hướng chính xác
        const userRole = (data.user && data.user.role) ? data.user.role : 'ROLE_LEARNER';
        const targetUrl = data.redirectUrl || DASHBOARD_ROUTES[userRole] || '/learner/dashboard';

        console.log(`[KizunaAuth] Xác thực thành công: User=${data.user?.fullName}, Role=${userRole}, Chuyển hướng tới: ${targetUrl}`);

        // 3. Tự động chuyển hướng ngay lập tức
        setTimeout(() => {
            window.location.href = targetUrl;
        }, 350);

    } catch (err) {
        console.error('[KizunaAuth] Lỗi kết nối mạng khi đăng nhập:', err);
        showLoginAlert('Không thể kết nối đến máy chủ. Vui lòng thử lại sau!', 'danger');
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.innerHTML = `<span>Đăng Nhập</span>`;
        }
    }
}

/**
 * Đăng nhập nhanh tài khoản mẫu dành cho Giám khảo / Kiểm thử viên (1-Click Demo Login)
 */
function quickLogin(email, password) {
    const emailInput = document.getElementById('loginEmail');
    const passwordInput = document.getElementById('loginPassword');
    if (emailInput && passwordInput) {
        emailInput.value = email;
        passwordInput.value = password;
        handleLoginSubmit();
    }
}

/**
 * Xử lý Đăng xuất an toàn:
 * - Xóa sạch LocalStorage
 * - Gọi API hủy Cookie trên Backend
 * - Chuyển hướng về trang Đăng nhập
 */
async function handleLogout() {
    clearAuthSession();
    try {
        await fetch('/api/auth/logout', {
            method: 'POST',
            headers: { 'Accept': 'application/json' }
        });
    } catch (e) {
        console.warn('Gửi request logout backend không thành công:', e);
    }
    window.location.href = '/login';
}

/**
 * Hiển thị thông báo trên Form Đăng nhập
 */
function showLoginAlert(message, type) {
    const alertBox = document.getElementById('loginAlertBox');
    if (!alertBox) return;

    alertBox.classList.remove('hidden', 'bg-red-50', 'text-red-700', 'border-red-200', 'bg-emerald-50', 'text-emerald-700', 'border-emerald-200');

    if (type === 'success') {
        alertBox.className = 'mb-4 p-3.5 rounded-lg text-sm font-medium border bg-emerald-50 text-emerald-800 border-emerald-200 flex items-center gap-2';
        alertBox.innerHTML = `<span>✅</span> <span>${message}</span>`;
    } else {
        alertBox.className = 'mb-4 p-3.5 rounded-lg text-sm font-medium border bg-red-50 text-red-800 border-red-200 flex items-center gap-2';
        alertBox.innerHTML = `<span>⚠️</span> <span>${message}</span>`;
    }
}

function hideLoginAlert() {
    const alertBox = document.getElementById('loginAlertBox');
    if (alertBox) {
        alertBox.className = 'hidden';
        alertBox.innerHTML = '';
    }
}

/**
 * Tự động đồng bộ hóa thông tin người dùng từ LocalStorage lên Header của Dashboard nếu có
 */
document.addEventListener('DOMContentLoaded', function() {
    const user = getAuthUser();
    if (user) {
        const topbarName = document.getElementById('topbarUserName');
        const topbarAvatar = document.getElementById('topbarUserAvatar');
        const welcomeHeroName = document.getElementById('welcomeHeroName');

        if (topbarName && user.fullName) {
            topbarName.textContent = user.fullName;
        }
        if (welcomeHeroName && user.fullName) {
            welcomeHeroName.textContent = user.fullName;
        }
        if (topbarAvatar && user.fullName) {
            const initials = user.fullName.split(' ').map(n => n[0]).slice(-2).join('').toUpperCase();
            topbarAvatar.textContent = initials || 'KZ';
        }
    }

    // Gắn sự kiện submit cho form đăng nhập nếu tồn tại
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', handleLoginSubmit);
    }
});

// =========================================================================
// QUẢN LÝ LUỒNG BẮT BUỘC ĐỔI MẬT KHẨU LẦN ĐẦU (FIRST-TIME PASSWORD CHANGE)
// =========================================================================

// Biến lưu trữ phiên đổi mật khẩu
let firstTimeAuthData = {
    token: null,
    user: null
};

/**
 * Hiển thị Modal bắt buộc đổi mật khẩu lần đầu (Static Backdrop, không thể đóng)
 */
function showFirstTimeChangePasswordModal(token, user, currentPassword) {
    firstTimeAuthData.token = token;
    firstTimeAuthData.user = user;

    const modal = document.getElementById('firstTimeChangePasswordModal');
    if (!modal) {
        console.warn('Không tìm thấy #firstTimeChangePasswordModal trên trang login');
        return;
    }

    modal.classList.remove('hidden');
    modal.classList.add('flex');

    const currPassInput = document.getElementById('firstTimeCurrentPassword');
    const newPassInput = document.getElementById('firstTimeNewPassword');

    if (currPassInput && currentPassword) {
        currPassInput.value = currentPassword;
    }
    if (newPassInput) {
        newPassInput.focus();
    }
}

/**
 * Hiển thị thông báo Alert bên trong Modal Đổi mật khẩu
 */
function showFirstTimeModalAlert(message, type) {
    const alertBox = document.getElementById('firstTimeModalAlert');
    if (!alertBox) return;

    alertBox.className = 'mb-4 p-3 rounded-xl text-xs font-semibold';
    if (type === 'danger' || type === 'error') {
        alertBox.classList.add('bg-rose-50', 'text-rose-700', 'border', 'border-rose-200');
    } else {
        alertBox.classList.add('bg-emerald-50', 'text-emerald-700', 'border', 'border-emerald-200');
    }
    alertBox.textContent = message;
    alertBox.classList.remove('hidden');
}

function hideFirstTimeModalAlert() {
    const alertBox = document.getElementById('firstTimeModalAlert');
    if (alertBox) alertBox.classList.add('hidden');
}

/**
 * Xử lý sự kiện gửi Form Đổi mật khẩu lần đầu
 */
async function handleFirstTimeChangePassword(event) {
    if (event) event.preventDefault();

    const currPassInput = document.getElementById('firstTimeCurrentPassword');
    const newPassInput = document.getElementById('firstTimeNewPassword');
    const confirmPassInput = document.getElementById('firstTimeConfirmPassword');
    const submitBtn = document.getElementById('btnSubmitFirstTimePassword');

    if (!currPassInput || !newPassInput || !confirmPassInput) return;

    const currentPassword = currPassInput.value;
    const newPassword = newPassInput.value;
    const confirmPassword = confirmPassInput.value;

    hideFirstTimeModalAlert();

    // 1. Kiểm tra khớp mật khẩu mới
    if (newPassword !== confirmPassword) {
        showFirstTimeModalAlert('Mật khẩu mới và mật khẩu xác nhận không trùng khớp!', 'danger');
        return;
    }

    // 2. Không được trùng mật khẩu tạm
    if (newPassword === currentPassword) {
        showFirstTimeModalAlert('Mật khẩu mới không được trùng với mật khẩu tạm thời hiện tại!', 'danger');
        return;
    }

    // 3. Kiểm tra độ mạnh mật khẩu (tối thiểu 8 ký tự, chữ hoa, số hoặc ký tự đặc biệt)
    const hasUpper = /[A-Z]/.test(newPassword);
    const hasDigitOrSpecial = /[0-9!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(newPassword);

    if (newPassword.length < 8 || !hasUpper || !hasDigitOrSpecial) {
        showFirstTimeModalAlert('Mật khẩu phải có tối thiểu 8 ký tự, bao gồm ít nhất 1 chữ hoa, và 1 chữ số hoặc ký tự đặc biệt!', 'danger');
        return;
    }

    const token = firstTimeAuthData.token || getAuthToken();
    if (!token) {
        showFirstTimeModalAlert('Phiên làm việc đã hết hạn. Vui lòng tải lại trang và đăng nhập lại.', 'danger');
        return;
    }

    // Hiển thị trạng thái đang cập nhật
    if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.innerHTML = `
            <span class="inline-block animate-spin mr-2">⏳</span>
            <span>Đang cập nhật mật khẩu bảo mật...</span>
        `;
    }

    try {
        const response = await fetch('/api/auth/change-password-first-time', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
                'Authorization': 'Bearer ' + token
            },
            body: JSON.stringify({ currentPassword, newPassword, confirmPassword })
        });

        const data = await response.json();

        if (!response.ok) {
            showFirstTimeModalAlert(data.message || 'Không thể đổi mật khẩu. Vui lòng kiểm tra lại.', 'danger');
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.innerHTML = `
                    <span>Cập Nhật Mật Khẩu &amp; Vào Hệ Thống</span>
                    <span>→</span>
                `;
            }
            return;
        }

        // Cập nhật thông tin User trong LocalStorage (xóa cờ mustChangePassword)
        const currentUser = getAuthUser() || firstTimeAuthData.user;
        if (currentUser) {
            currentUser.mustChangePassword = false;
            setAuthSession(token, currentUser);
        }

        showFirstTimeModalAlert('✅ ' + (data.message || 'Đổi mật khẩu thành công! Đang chuyển hướng vào bảng làm việc...'), 'success');

        // Xác định đường dẫn điều hướng chính thức
        const userRole = (currentUser && currentUser.role) ? currentUser.role : 'ROLE_LEARNER';
        const targetUrl = data.redirectUrl || DASHBOARD_ROUTES[userRole] || '/learner/dashboard';

        setTimeout(() => {
            window.location.href = targetUrl;
        }, 600);

    } catch (err) {
        console.error('[KizunaAuth] Lỗi kết nối khi đổi mật khẩu lần đầu:', err);
        showFirstTimeModalAlert('Không thể kết nối đến máy chủ. Vui lòng thử lại sau!', 'danger');
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.innerHTML = `
                <span>Cập Nhật Mật Khẩu &amp; Vào Hệ Thống</span>
                <span>→</span>
            `;
        }
    }
}
