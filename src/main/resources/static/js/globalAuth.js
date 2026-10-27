/**
 * ====================================================================================================
 * KIZUNA TUTOR - GLOBAL AUTHENTICATION & SESSION MANAGEMENT (globalAuth.js)
 * ====================================================================================================
 * Quản lý phiên đăng nhập và xử lý đăng xuất toàn cục an toàn (Global Logout Handler).
 * Áp dụng cơ chế Event Delegation ở cấp độ document:
 * - Không phụ thuộc vào thứ tự nạp DOM hay Dynamic Component Injection.
 * - Hoạt động ổn định trên 100% các màn hình của cả 6 Actor (Admin, Manager, CS, Academic, Tutor, Learner).
 * - Xóa sạch toàn bộ LocalStorage, SessionStorage và Cookie phiên làm việc.
 * - Gửi request hủy phiên tới API Backend (/api/auth/logout).
 * - Tương thích ngược với cả executeLogout() lẫn handleLogout().
 * ====================================================================================================
 */

/**
 * Hàm xử lý đăng xuất tập trung cho toàn bộ ứng dụng Kizuna
 */
function executeLogout() {
    try {
        console.info('[GlobalAuth] Đang tiến hành đăng xuất người dùng khỏi hệ thống...');

        // 1. Xóa toàn bộ dữ liệu phiên lưu trữ trên trình duyệt (LocalStorage & SessionStorage)
        const keysToRemove = [
            'access_token',
            'refresh_token',
            'current_user',
            'kizuna_token',
            'kizuna_user',
            'kizuna_role',
            'token'
        ];
        keysToRemove.forEach(key => {
            try {
                localStorage.removeItem(key);
            } catch (e) {
                console.warn(`[GlobalAuth] Không thể xóa key ${key} khỏi localStorage:`, e);
            }
        });

        try {
            sessionStorage.clear();
        } catch (e) {
            console.warn('[GlobalAuth] Không thể xóa sessionStorage:', e);
        }

        // 2. Xóa Cookie xác thực phiên làm việc (cả access_token và kizuna_token)
        const expiredDate = 'Thu, 01 Jan 1970 00:00:00 GMT';
        document.cookie = `access_token=; path=/; max-age=0; expires=${expiredDate}`;
        document.cookie = `kizuna_token=; path=/; max-age=0; expires=${expiredDate}`;
        document.cookie = `JSESSIONID=; path=/; max-age=0; expires=${expiredDate}`;

        // 3. Gửi thông báo hủy phiên đến máy chủ Backend (Background fetch, không chặn điều hướng)
        fetch('/api/auth/logout', {
            method: 'POST',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            }
        }).catch(err => {
            console.warn('[GlobalAuth] Gọi API logout Backend không thành công (có thể do offline hoặc phiên đã hết hạn):', err);
        });

        // 4. Chuyển hướng ngay lập tức về trang Đăng nhập
        window.location.replace('/login');
    } catch (err) {
        console.error('[GlobalAuth] Lỗi nghiêm trọng khi thực hiện đăng xuất:', err);
        // Fallback khẩn cấp: điều hướng thẳng về /login
        window.location.href = '/login';
    }
}

/**
 * Áp dụng cơ chế Event Delegation (Bắt sự kiện ủy quyền ở cấp độ document):
 * Mọi cú click vào phần tử chứa data-action="logout", class "btn-logout", hoặc id="logoutBtn"
 * đều được bắt và xử lý ngay lập tức, kể cả khi component được nạp động hoặc DOM bị re-render.
 */
document.addEventListener('click', function (e) {
    const logoutTrigger = e.target.closest('[data-action="logout"], .btn-logout, #logoutBtn');
    if (logoutTrigger) {
        e.preventDefault();
        e.stopPropagation();
        executeLogout();
    }
}, true); // Use capture phase để đảm bảo bắt sự kiện trước khi các listener khác ngăn chặn

// Gắn vào đối tượng window để gọi được từ thuộc tính onclick inline
window.executeLogout = executeLogout;

// Tương thích ngược 100% với các mã cũ đang gọi handleLogout()
window.handleLogout = executeLogout;

console.info('[GlobalAuth] Trình xử lý đăng xuất toàn cục Kizuna đã sẵn sàng.');
