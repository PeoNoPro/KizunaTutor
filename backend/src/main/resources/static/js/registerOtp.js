/**
 * ====================================================================================================
 * KIZUNA TUTOR - QUẢN LÝ ĐĂNG KÝ TÀI KHOẢN VÀ XÁC THỰC MÃ OTP 6 CHỮ SỐ (registerOtp.js)
 * ====================================================================================================
 * Chức năng:
 * 1. Thu thập thông tin Form đăng ký và gửi API tạo mã OTP: POST /api/auth/register/send-otp
 * 2. Mở Modal hiển thị 6 ô số OTP độc lập với trải nghiệm người dùng tối ưu:
 *    - Tự động chuyển con trỏ sang ô tiếp theo khi nhập số.
 *    - Tự động xóa và lùi con trỏ về ô trước khi bấm Backspace.
 *    - Hỗ trợ dán (Paste) cả chuỗi 6 số từ Clipboard.
 * 3. Đồng hồ đếm ngược thời gian hiệu lực 5 phút (05:00 -> 00:00).
 * 4. Nút [ Gửi lại mã ] có cơ chế Cooldown đếm ngược 60 giây.
 * 5. Xác thực OTP: POST /api/auth/register/verify-otp, lưu JWT và tự động chuyển hướng vào Dashboard.
 * ====================================================================================================
 */

// Bảng ánh xạ Route Dashboard mặc định theo Role
const DASHBOARD_ROUTES = {
    ROLE_ADMIN: '/admin/dashboard',
    ROLE_MANAGER: '/manager/dashboard',
    ROLE_CS_STAFF: '/cs/dashboard',
    ROLE_ACADEMIC_OFFICER: '/academic/dashboard',
    ROLE_TUTOR: '/tutor/dashboard',
    ROLE_LEARNER: '/learner/dashboard'
};

// State quản lý luồng OTP
const otpState = {
    email: '',
    fullName: '',
    role: 'ROLE_LEARNER',
    targetJlpt: 'N3',
    password: '',
    countdownInterval: null,
    totalSecondsLeft: 300, // 5 phút (300 giây)
    resendInterval: null,
    resendSecondsLeft: 60  // 60 giây Cooldown
};

document.addEventListener('DOMContentLoaded', function () {
    initRegistrationForm();
    initOtpInputs();
    initRoleToggle();
});

/**
 * 1. Khởi tạo sự kiện cho Form Đăng ký ban đầu
 */
function initRegistrationForm() {
    const regForm = document.getElementById('registrationForm');
    if (!regForm) return;

    regForm.addEventListener('submit', async function (e) {
        e.preventDefault();

        const fullNameInput = document.getElementById('regFullName');
        const emailInput = document.getElementById('regEmail');
        const passwordInput = document.getElementById('regPassword');
        const roleInput = document.querySelector('input[name="userRole"]:checked') || document.getElementById('regRole');
        const targetJlptInput = document.getElementById('regTargetJlpt');
        const submitBtn = document.getElementById('btnSubmitRegistration');

        const fullName = fullNameInput ? fullNameInput.value.trim() : '';
        const email = emailInput ? emailInput.value.trim().toLowerCase() : '';
        const password = passwordInput ? passwordInput.value : '';
        const role = roleInput ? roleInput.value : 'ROLE_LEARNER';
        const targetJlpt = targetJlptInput ? targetJlptInput.value : 'N3';

        // Kiểm tra dữ liệu đầu vào cơ bản
        if (!fullName || !email || !password) {
            showToast('Vui lòng điền đầy đủ tất cả các trường thông tin.', 'error');
            return;
        }

        if (password.length < 6) {
            showToast('Mật khẩu phải có tối thiểu 6 ký tự.', 'error');
            return;
        }

        // Lưu vào state tạm
        otpState.fullName = fullName;
        otpState.email = email;
        otpState.password = password;
        otpState.role = role;
        otpState.targetJlpt = targetJlpt;

        // Cập nhật trạng thái nút bấm (loading...)
        setBtnLoading(submitBtn, true, 'Đang gửi mã xác thực...');

        try {
            const response = await fetch('/api/auth/register/send-otp', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify({
                    fullName: fullName,
                    email: email,
                    password: password,
                    role: role,
                    targetJlpt: targetJlpt
                })
            });

            const data = await response.json();

            if (!response.ok) {
                const errorMsg = data.message || data.error || 'Không thể gửi mã xác thực. Vui lòng thử lại.';
                showToast(errorMsg, 'error');
                return;
            }

            // Gửi OTP thành công -> Mở Modal nhập OTP
            showToast(data.message || 'Mã xác thực OTP đã được gửi đến Gmail của bạn!', 'success');
            openOtpModal(email);

        } catch (error) {
            console.error('[KizunaOtp] Lỗi mạng khi gọi send-otp:', error);
            showToast('Không thể kết nối đến máy chủ. Vui lòng kiểm tra lại kết nối mạng.', 'error');
        } finally {
            setBtnLoading(submitBtn, false, 'Nhận Mã Xác Thực OTP ✉️');
        }
    });
}

/**
 * 2. Đổi giao diện động khi chuyển đổi giữa vai trò Học viên và Gia sư
 */
function initRoleToggle() {
    const roleRadios = document.querySelectorAll('input[name="userRole"]');
    const jlptContainer = document.getElementById('jlptLevelContainer');

    roleRadios.forEach(radio => {
        radio.addEventListener('change', function () {
            if (jlptContainer) {
                if (this.value === 'ROLE_LEARNER') {
                    jlptContainer.classList.remove('hidden');
                } else {
                    jlptContainer.classList.add('hidden');
                }
            }
        });
    });
}

/**
 * 3. Khởi tạo sự kiện cho 6 ô nhập mã OTP (Focus tự động, Backspace, Paste)
 */
function initOtpInputs() {
    const otpInputs = document.querySelectorAll('.otp-digit-input');
    if (!otpInputs || otpInputs.length === 0) return;

    otpInputs.forEach((input, index) => {
        // Sự kiện gõ ký tự: Chỉ cho phép số, tự động nhảy sang ô tiếp theo
        input.addEventListener('input', function (e) {
            const val = this.value;

            // Loại bỏ ký tự không phải số
            if (!/^\d$/.test(val)) {
                this.value = '';
                return;
            }

            // Tự động nhảy sang ô tiếp theo nếu có
            if (val && index < otpInputs.length - 1) {
                otpInputs[index + 1].focus();
                otpInputs[index + 1].select();
            }

            // Kiểm tra nếu đã điền đủ 6 số thì kiểm tra tự động
            checkIfOtpFull();
        });

        // Sự kiện phím bấm: Backspace lùi về ô trước
        input.addEventListener('keydown', function (e) {
            if (e.key === 'Backspace') {
                if (!this.value && index > 0) {
                    otpInputs[index - 1].focus();
                    otpInputs[index - 1].value = '';
                }
            } else if (e.key === 'ArrowLeft' && index > 0) {
                otpInputs[index - 1].focus();
            } else if (e.key === 'ArrowRight' && index < otpInputs.length - 1) {
                otpInputs[index + 1].focus();
            } else if (e.key === 'Enter') {
                e.preventDefault();
                submitOtpVerification();
            }
        });

        // Hỗ trợ sự kiện Dán (Paste) toàn bộ chuỗi 6 ký tự từ Clipboard
        input.addEventListener('paste', function (e) {
            e.preventDefault();
            const pasteData = (e.clipboardData || window.clipboardData).getData('text');
            if (!pasteData) return;

            // Trích xuất các ký tự số đầu tiên
            const digits = pasteData.replace(/\D/g, '').slice(0, otpInputs.length);

            if (digits.length > 0) {
                digits.split('').forEach((char, i) => {
                    if (otpInputs[i]) {
                        otpInputs[i].value = char;
                    }
                });

                // Chuyển focus đến ô tiếp theo hoặc ô cuối cùng
                const nextFocusIndex = Math.min(digits.length, otpInputs.length - 1);
                otpInputs[nextFocusIndex].focus();

                checkIfOtpFull();
            }
        });
    });

    // Nút [ Xác Nhận & Đăng Nhập ]
    const btnVerify = document.getElementById('btnConfirmOtp');
    if (btnVerify) {
        btnVerify.addEventListener('click', submitOtpVerification);
    }

    // Nút [ Gửi lại mã ]
    const btnResend = document.getElementById('btnResendOtp');
    if (btnResend) {
        btnResend.addEventListener('click', handleResendOtp);
    }

    // Nút đóng modal / sửa lại email
    const btnCloseModal = document.getElementById('btnCloseOtpModal');
    if (btnCloseModal) {
        btnCloseModal.addEventListener('click', closeOtpModal);
    }
}

/**
 * 4. Mở Modal xác thực OTP
 */
function openOtpModal(email) {
    const modal = document.getElementById('otpModal');
    const targetEmailEl = document.getElementById('otpTargetEmailDisplay');
    const otpInputs = document.querySelectorAll('.otp-digit-input');

    if (targetEmailEl) {
        targetEmailEl.textContent = email;
    }

    // Xóa sạch dữ liệu cũ trong 6 ô
    otpInputs.forEach(input => {
        input.value = '';
        input.classList.remove('border-rose-500', 'border-emerald-500');
    });

    hideOtpAlert();

    if (modal) {
        modal.classList.remove('hidden');
        modal.classList.add('flex');
    }

    // Tự động focus vào ô đầu tiên sau khi modal hiện
    setTimeout(() => {
        if (otpInputs[0]) {
            otpInputs[0].focus();
        }
    }, 150);

    // Bắt đầu đếm ngược 5 phút (300 giây)
    startOtpCountdown(300);

    // Bắt đầu đếm ngược 60 giây gửi lại mã
    startResendCountdown(60);
}

/**
 * Đóng Modal OTP
 */
function closeOtpModal() {
    const modal = document.getElementById('otpModal');
    if (modal) {
        modal.classList.add('hidden');
        modal.classList.remove('flex');
    }
    clearInterval(otpState.countdownInterval);
    clearInterval(otpState.resendInterval);
}

/**
 * 5. Bộ đếm ngược thời gian hết hạn OTP (05:00 -> 00:00)
 */
function startOtpCountdown(seconds) {
    clearInterval(otpState.countdownInterval);
    otpState.totalSecondsLeft = seconds;

    const timerDisplay = document.getElementById('otpCountdownTimer');

    function updateDisplay() {
        const mins = Math.floor(otpState.totalSecondsLeft / 60);
        const secs = otpState.totalSecondsLeft % 60;
        const formatted = `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;

        if (timerDisplay) {
            timerDisplay.textContent = formatted;
            if (otpState.totalSecondsLeft <= 30) {
                timerDisplay.classList.add('text-rose-600', 'font-black');
            } else {
                timerDisplay.classList.remove('text-rose-600', 'font-black');
            }
        }

        if (otpState.totalSecondsLeft <= 0) {
            clearInterval(otpState.countdownInterval);
            showOtpAlert('Mã xác thực OTP đã hết hạn. Vui lòng bấm "Gửi lại mã" để nhận mã mới.', 'warning');
        } else {
            otpState.totalSecondsLeft--;
        }
    }

    updateDisplay();
    otpState.countdownInterval = setInterval(updateDisplay, 1000);
}

/**
 * 6. Bộ đếm ngược thời gian chờ gửi lại mã (Cooldown 60s)
 */
function startResendCountdown(seconds) {
    clearInterval(otpState.resendInterval);
    otpState.resendSecondsLeft = seconds;

    const btnResend = document.getElementById('btnResendOtp');
    const resendText = document.getElementById('resendCountdownText');

    if (!btnResend) return;

    btnResend.disabled = true;
    btnResend.classList.add('opacity-50', 'cursor-not-allowed');

    function updateResend() {
        if (otpState.resendSecondsLeft <= 0) {
            clearInterval(otpState.resendInterval);
            btnResend.disabled = false;
            btnResend.classList.remove('opacity-50', 'cursor-not-allowed');
            if (resendText) resendText.textContent = '';
            btnResend.innerHTML = '<span>🔄</span> <span>Gửi lại mã OTP</span>';
        } else {
            btnResend.innerHTML = `<span>⏳</span> <span>Gửi lại mã (${otpState.resendSecondsLeft}s)</span>`;
            otpState.resendSecondsLeft--;
        }
    }

    updateResend();
    otpState.resendInterval = setInterval(updateResend, 1000);
}

/**
 * 7. Xử lý Gửi lại mã OTP
 */
async function handleResendOtp() {
    const btnResend = document.getElementById('btnResendOtp');
    if (!btnResend || btnResend.disabled) return;

    btnResend.disabled = true;
    btnResend.innerHTML = '<span>⏳</span> <span>Đang gửi lại...</span>';

    try {
        const response = await fetch('/api/auth/register/resend-otp', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({ email: otpState.email })
        });

        const data = await response.json();

        if (!response.ok) {
            showOtpAlert(data.message || 'Chưa thể gửi lại mã vào lúc này.', 'error');
            btnResend.disabled = false;
            btnResend.innerHTML = '<span>🔄</span> <span>Gửi lại mã OTP</span>';
            return;
        }

        showOtpAlert('Đã gửi mã xác thực mới đến email của bạn!', 'success');

        // Reset lại bộ đếm 5 phút và 60s cooldown
        startOtpCountdown(300);
        startResendCountdown(60);

        // Xóa trắng các ô nhập
        const otpInputs = document.querySelectorAll('.otp-digit-input');
        otpInputs.forEach(i => i.value = '');
        if (otpInputs[0]) otpInputs[0].focus();

    } catch (e) {
        console.error('[KizunaOtp] Lỗi khi gửi lại OTP:', e);
        showOtpAlert('Không thể kết nối đến máy chủ khi gửi lại mã.', 'error');
        btnResend.disabled = false;
    }
}

/**
 * 8. Thu thập chuỗi 6 ký tự và gọi API Xác thực
 */
async function submitOtpVerification() {
    const otpInputs = document.querySelectorAll('.otp-digit-input');
    const btnVerify = document.getElementById('btnConfirmOtp');

    let fullOtp = '';
    otpInputs.forEach(input => fullOtp += input.value.trim());

    if (fullOtp.length !== 6) {
        showOtpAlert('Vui lòng nhập đầy đủ cả 6 chữ số mã xác thực.', 'error');
        return;
    }

    setBtnLoading(btnVerify, true, 'Đang xác thực & kích hoạt...');
    hideOtpAlert();

    try {
        const response = await fetch('/api/auth/register/verify-otp', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({
                email: otpState.email,
                otp: fullOtp
            })
        });

        const data = await response.json();

        if (!response.ok) {
            const errorMsg = data.message || data.error || 'Mã OTP không chính xác hoặc đã hết hạn.';
            showOtpAlert(errorMsg, 'error');

            // Đổi viền đỏ cảnh báo trên các ô nhập
            otpInputs.forEach(input => input.classList.add('border-rose-500'));
            return;
        }

        // 1. Lưu thông tin Token và User vào localStorage
        if (data.token) {
            localStorage.setItem('kizuna_token', data.token);
            localStorage.setItem('access_token', data.token);
        }
        if (data.user) {
            localStorage.setItem('kizuna_user', JSON.stringify(data.user));
        }

        // Đổi viền xanh thành công
        otpInputs.forEach(input => {
            input.classList.remove('border-rose-500');
            input.classList.add('border-emerald-500');
        });

        showOtpAlert('✅ Xác thực thành công! Đang chuyển hướng vào không gian làm việc...', 'success');

        // 2. Xác định Route điều hướng
        const userRole = (data.user && data.user.role) ? data.user.role : otpState.role;
        const targetUrl = data.redirectUrl || DASHBOARD_ROUTES[userRole] || '/learner/dashboard';

        console.log(`[KizunaOtp] Kích hoạt tài khoản thành công: ${data.user?.fullName}, chuyển hướng -> ${targetUrl}`);

        // 3. Tự động chuyển hướng sau 500ms
        setTimeout(() => {
            window.location.href = targetUrl;
        }, 500);

    } catch (err) {
        console.error('[KizunaOtp] Lỗi khi xác thực OTP:', err);
        showOtpAlert('Lỗi kết nối khi xác thực. Vui lòng kiểm tra lại mạng!', 'error');
    } finally {
        setBtnLoading(btnVerify, false, 'Xác Nhận & Đăng Nhập →');
    }
}

/**
 * Kiểm tra nếu đã nhập đủ 6 số
 */
function checkIfOtpFull() {
    const otpInputs = document.querySelectorAll('.otp-digit-input');
    let count = 0;
    otpInputs.forEach(i => { if (i.value) count++; });
    if (count === 6) {
        // Tự động gợi ý bấm xác nhận
        const btnVerify = document.getElementById('btnConfirmOtp');
        if (btnVerify) {
            btnVerify.classList.add('ring-2', 'ring-rose-500/50');
        }
    }
}

/**
 * Hiển thị thông báo trong Modal OTP
 */
function showOtpAlert(message, type) {
    const alertBox = document.getElementById('otpModalAlertBox');
    if (!alertBox) return;

    alertBox.classList.remove('hidden', 'bg-rose-50', 'text-rose-800', 'border-rose-200', 'bg-emerald-50', 'text-emerald-800', 'border-emerald-200', 'bg-amber-50', 'text-amber-800', 'border-amber-200');

    if (type === 'success') {
        alertBox.className = 'mb-4 p-3 rounded-lg text-xs font-semibold border bg-emerald-50 text-emerald-800 border-emerald-200 flex items-center gap-2';
        alertBox.innerHTML = `<span>✅</span> <span>${message}</span>`;
    } else if (type === 'warning') {
        alertBox.className = 'mb-4 p-3 rounded-lg text-xs font-semibold border bg-amber-50 text-amber-800 border-amber-200 flex items-center gap-2';
        alertBox.innerHTML = `<span>⚠️</span> <span>${message}</span>`;
    } else {
        alertBox.className = 'mb-4 p-3 rounded-lg text-xs font-semibold border bg-rose-50 text-rose-800 border-rose-200 flex items-center gap-2';
        alertBox.innerHTML = `<span>❌</span> <span>${message}</span>`;
    }
}

function hideOtpAlert() {
    const alertBox = document.getElementById('otpModalAlertBox');
    if (alertBox) {
        alertBox.className = 'hidden';
        alertBox.innerHTML = '';
    }
}

/**
 * Hiển thị Toast thông báo chung
 */
function showToast(message, type) {
    let toast = document.getElementById('globalToast');
    if (!toast) {
        toast = document.createElement('div');
        toast.id = 'globalToast';
        toast.className = 'fixed bottom-6 right-6 z-50 px-5 py-3 rounded-xl shadow-lg text-sm font-bold flex items-center gap-2 transition-all transform duration-300 opacity-0 translate-y-2';
        document.body.appendChild(toast);
    }

    if (type === 'success') {
        toast.className = 'fixed bottom-6 right-6 z-50 px-5 py-3 rounded-xl shadow-lg text-sm font-bold flex items-center gap-2 transition-all transform duration-300 bg-slate-900 text-white border border-slate-700 opacity-100 translate-y-0';
        toast.innerHTML = `<span>✅</span> <span>${message}</span>`;
    } else {
        toast.className = 'fixed bottom-6 right-6 z-50 px-5 py-3 rounded-xl shadow-lg text-sm font-bold flex items-center gap-2 transition-all transform duration-300 bg-rose-600 text-white shadow-rose-600/30 opacity-100 translate-y-0';
        toast.innerHTML = `<span>⚠️</span> <span>${message}</span>`;
    }

    setTimeout(() => {
        toast.classList.add('opacity-0', 'translate-y-2');
    }, 4000);
}

/**
 * Trợ giúp bật/tắt trạng thái tải của nút bấm
 */
function setBtnLoading(btn, isLoading, defaultText) {
    if (!btn) return;
    if (isLoading) {
        btn.disabled = true;
        btn.innerHTML = `
            <span class="inline-block w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
            <span>${defaultText}</span>
        `;
    } else {
        btn.disabled = false;
        btn.innerHTML = `<span>${defaultText}</span>`;
    }
}
