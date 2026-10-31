/**
 * ====================================================================================================
 * KIZUNA TUTOR - QUẢN LÝ USER & PHÂN QUYỀN VAI TRÒ ADMIN (userManagement.js)
 * Chuẩn Retro-Brutalist Hard-Shadow Design System
 * ====================================================================================================
 */

const ROLE_CONFIGS = {
    ROLE_ADMIN: {
        label: 'Quản trị viên',
        badgeClass: 'bg-[#FFE4E6] text-rose-900 border border-black shadow-[1.5px_1.5px_0px_0px_#0F172A]',
        dotClass: 'bg-rose-600'
    },
    ROLE_MANAGER: {
        label: 'Quản lý vận hành',
        badgeClass: 'bg-[#F3E8FF] text-purple-900 border border-black shadow-[1.5px_1.5px_0px_0px_#0F172A]',
        dotClass: 'bg-purple-600'
    },
    ROLE_ACADEMIC_OFFICER: {
        label: 'Cán bộ học vụ',
        badgeClass: 'bg-[#E0E7FF] text-indigo-900 border border-black shadow-[1.5px_1.5px_0px_0px_#0F172A]',
        dotClass: 'bg-indigo-600'
    },
    ROLE_CS_STAFF: {
        label: 'Chăm sóc khách hàng',
        badgeClass: 'bg-[#CFFAFE] text-cyan-900 border border-black shadow-[1.5px_1.5px_0px_0px_#0F172A]',
        dotClass: 'bg-cyan-600'
    },
    ROLE_TUTOR: {
        label: 'Gia sư Kizuna',
        badgeClass: 'bg-[#FEF3C7] text-amber-900 border border-black shadow-[1.5px_1.5px_0px_0px_#0F172A]',
        dotClass: 'bg-amber-600'
    },
    ROLE_LEARNER: {
        label: 'Học viên',
        badgeClass: 'bg-[#DCFCE7] text-emerald-900 border border-black shadow-[1.5px_1.5px_0px_0px_#0F172A]',
        dotClass: 'bg-emerald-600'
    }
};

const adminUserState = {
    page: 0,
    size: 10,
    keyword: '',
    role: 'ALL',
    status: 'ALL',
    sort: 'createdAt',
    direction: 'desc',
    totalElements: 0,
    totalPages: 0,
    debounceTimer: null,
    editingUserId: null,
    pendingActionUserId: null,
    pendingActionStatus: null
};

document.addEventListener('DOMContentLoaded', function () {
    initUserManagementFilters();
    initModals();
    loadUsers();
});

/**
 * 1. Khởi tạo các sự kiện lọc, tìm kiếm và phân trang
 */
function initUserManagementFilters() {
    const searchInput = document.getElementById('searchUserKeyword');
    const roleSelect = document.getElementById('filterUserRole');
    const statusSelect = document.getElementById('filterUserStatus');
    const pageSizeSelect = document.getElementById('pageSizeSelect');

    if (searchInput) {
        searchInput.addEventListener('input', function () {
            clearTimeout(adminUserState.debounceTimer);
            adminUserState.debounceTimer = setTimeout(() => {
                adminUserState.keyword = this.value.trim();
                adminUserState.page = 0;
                loadUsers();
            }, 300);
        });
    }

    if (roleSelect) {
        roleSelect.addEventListener('change', function () {
            adminUserState.role = this.value;
            adminUserState.page = 0;
            loadUsers();
        });
    }

    if (statusSelect) {
        statusSelect.addEventListener('change', function () {
            adminUserState.status = this.value;
            adminUserState.page = 0;
            loadUsers();
        });
    }

    if (pageSizeSelect) {
        pageSizeSelect.addEventListener('change', function () {
            adminUserState.size = parseInt(this.value, 10) || 10;
            adminUserState.page = 0;
            loadUsers();
        });
    }
}

/**
 * 2. Gọi REST API lấy danh sách người dùng & cập nhật giao diện
 */
async function loadUsers() {
    const tbody = document.getElementById('userTableBody');
    const loadingState = document.getElementById('tableLoadingState');
    const emptyState = document.getElementById('tableEmptyState');

    if (loadingState) loadingState.classList.remove('hidden');
    if (emptyState) emptyState.classList.add('hidden');
    if (tbody) tbody.innerHTML = '';

    const params = new URLSearchParams({
        page: adminUserState.page,
        size: adminUserState.size,
        keyword: adminUserState.keyword,
        role: adminUserState.role,
        status: adminUserState.status,
        sort: adminUserState.sort,
        direction: adminUserState.direction
    });

    try {
        const response = await fetch(`/api/admin/users?${params.toString()}`, {
            headers: { 'Accept': 'application/json' }
        });

        if (!response.ok) {
            throw new Error('Lỗi khi tải dữ liệu người dùng từ máy chủ.');
        }

        const data = await response.json();
        adminUserState.totalElements = data.totalElements || 0;
        adminUserState.totalPages = data.totalPages || 0;

        // Cập nhật 4 thẻ KPI
        if (data.kpis) {
            updateKpiCards(data.kpis);
        }

        if (loadingState) loadingState.classList.add('hidden');

        if (!data.content || data.content.length === 0) {
            if (emptyState) emptyState.classList.remove('hidden');
            renderPagination(0, 0, 0);
            return;
        }

        // Render từng dòng dữ liệu người dùng
        renderUserRows(data.content);
        renderPagination(data.currentPage, data.totalPages, data.totalElements);

    } catch (err) {
        console.error('[UserManagement] Lỗi nạp danh sách:', err);
        if (loadingState) loadingState.classList.add('hidden');
        showAdminToast(err.message || 'Không thể tải danh sách người dùng', 'error');
    }
}

/**
 * 3. Render các dòng của bảng người dùng
 */
function renderUserRows(users) {
    const tbody = document.getElementById('userTableBody');
    if (!tbody) return;

    tbody.innerHTML = '';

    users.forEach(user => {
        const tr = document.createElement('tr');
        tr.className = 'hover:bg-amber-50/40 border-b border-black text-xs text-slate-800 transition-colors';

        const roleConfig = ROLE_CONFIGS[user.role] || {
            label: user.roleBadge || user.role,
            badgeClass: 'bg-slate-100 text-slate-800 border border-black shadow-[1.5px_1.5px_0px_0px_#0F172A]',
            dotClass: 'bg-slate-600'
        };

        const isSelfBadge = user.isSelf ? `
            <span class="ml-1.5 px-1.5 py-0.5 text-[10px] font-bold rounded-[5px] bg-slate-900 text-white border border-black shadow-[1px_1px_0px_0px_#0F172A]">Bạn</span>
        ` : '';

        // Trạng thái hoạt động
        let statusHtml = '';
        if (user.isDeleted) {
            statusHtml = `<span class="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-[5px] text-[11px] font-bold bg-slate-100 text-slate-700 border border-black shadow-[1.5px_1.5px_0px_0px_#0F172A]">
                <span class="w-1.5 h-1.5 rounded-full bg-slate-500"></span> Đã xóa mềm
            </span>`;
        } else if (user.status === 'ACTIVE') {
            statusHtml = `<span class="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-[5px] text-[11px] font-bold bg-[#DCFCE7] text-emerald-900 border border-black shadow-[1.5px_1.5px_0px_0px_#0F172A]">
                <span class="w-1.5 h-1.5 rounded-full bg-emerald-600"></span> Đang hoạt động
            </span>`;
        } else {
            statusHtml = `<span class="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-[5px] text-[11px] font-bold bg-[#FFE4E6] text-rose-900 border border-black shadow-[1.5px_1.5px_0px_0px_#0F172A]">
                <span class="w-1.5 h-1.5 rounded-full bg-rose-600"></span> ${escapeHtml(user.statusBadge || 'Tạm khóa')}
            </span>`;
        }

        // Nút thao tác
        const isSelf = user.isSelf;
        const lockActionLabel = (user.status === 'ACTIVE') ? 'Khóa' : 'Mở khóa';
        const lockActionIcon = (user.status === 'ACTIVE') ? '🔒' : '🔓';
        const nextStatus = (user.status === 'ACTIVE') ? 'INACTIVE' : 'ACTIVE';

        tr.innerHTML = `
            <!-- Cột 1: Người dùng (Avatar, Tên, Email) -->
            <td class="py-3 px-4">
                <div class="flex items-center gap-2.5">
                    <div class="w-8 h-8 rounded-full bg-slate-900 text-white font-bold flex items-center justify-center text-xs border border-black shadow-[1.5px_1.5px_0px_0px_#0F172A] shrink-0">
                        ${escapeHtml(user.avatarText || 'KZ')}
                    </div>
                    <div class="min-w-0">
                        <div class="font-bold text-slate-900 flex items-center truncate">
                            <span class="truncate">${escapeHtml(user.fullName)}</span>
                            ${isSelfBadge}
                        </div>
                        <div class="text-[11px] text-slate-500 truncate mt-0.5 font-mono">
                            ${escapeHtml(user.email)}
                        </div>
                    </div>
                </div>
            </td>

            <!-- Cột 2: Số điện thoại -->
            <td class="py-3 px-4 font-mono text-slate-700">
                ${escapeHtml(user.phone || '-')}
            </td>

            <!-- Cột 3: Vai trò -->
            <td class="py-3 px-4">
                <span class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-[5px] text-[11px] font-bold ${roleConfig.badgeClass}">
                    <span class="w-1.5 h-1.5 rounded-full ${roleConfig.dotClass}"></span>
                    <span>${escapeHtml(roleConfig.label)}</span>
                </span>
            </td>

            <!-- Cột 4: Trạng thái -->
            <td class="py-3 px-4">
                ${statusHtml}
            </td>

            <!-- Cột 5: Ngày tạo -->
            <td class="py-3 px-4 font-mono text-slate-600 text-[11px]">
                ${escapeHtml(user.createdAt)}
            </td>

            <!-- Cột 6: Thao tác -->
            <td class="py-3 px-4 text-right">
                <div class="inline-flex items-center gap-1.5">
                    <!-- Nút Gửi lại thông tin tài khoản -->
                    <button type="button"
                            onclick="confirmResendCredentials('${user.id}', '${escapeHtml(user.fullName)}', '${escapeHtml(user.email)}')"
                            ${isSelf ? 'disabled' : ''}
                            class="p-1.5 rounded-[5px] bg-white hover:bg-[#FEF3C7] text-slate-800 border border-black shadow-[1.5px_1.5px_0px_0px_#0F172A] transition-all active:translate-x-[0.5px] active:translate-y-[0.5px] active:shadow-none ${isSelf ? 'opacity-40 cursor-not-allowed' : ''}"
                            title="${isSelf ? 'Không thể cấp lại cho chính mình' : 'Cấp lại mật khẩu tạm và gửi email'}">
                        ✉️
                    </button>

                    <!-- Nút Sửa / Đổi quyền -->
                    <button type="button"
                            onclick="openEditUserModal('${user.id}')"
                            class="p-1.5 rounded-[5px] bg-white hover:bg-slate-100 text-slate-800 border border-black shadow-[1.5px_1.5px_0px_0px_#0F172A] transition-all active:translate-x-[0.5px] active:translate-y-[0.5px] active:shadow-none"
                            title="Chỉnh sửa thông tin & đổi vai trò">
                        ✏️
                    </button>

                    <!-- Nút Khóa / Mở khóa -->
                    <button type="button"
                            onclick="confirmToggleStatus('${user.id}', '${escapeHtml(user.fullName)}', '${nextStatus}')"
                            ${isSelf ? 'disabled' : ''}
                            class="p-1.5 rounded-[5px] bg-white hover:bg-slate-100 text-slate-800 border border-black shadow-[1.5px_1.5px_0px_0px_#0F172A] transition-all active:translate-x-[0.5px] active:translate-y-[0.5px] active:shadow-none ${isSelf ? 'opacity-40 cursor-not-allowed' : ''}"
                            title="${isSelf ? 'Không thể tự khóa tài khoản của chính mình' : lockActionLabel}">
                        ${lockActionIcon}
                    </button>

                    <!-- Nút Xóa mềm -->
                    <button type="button"
                            onclick="confirmDeleteUser('${user.id}', '${escapeHtml(user.fullName)}')"
                            ${isSelf ? 'disabled' : ''}
                            class="p-1.5 rounded-[5px] bg-white hover:bg-[#FFE4E6] text-slate-800 border border-black shadow-[1.5px_1.5px_0px_0px_#0F172A] transition-all active:translate-x-[0.5px] active:translate-y-[0.5px] active:shadow-none ${isSelf ? 'opacity-40 cursor-not-allowed' : ''}"
                            title="${isSelf ? 'Không thể tự xóa tài khoản của chính mình' : 'Xóa tài khoản'}">
                        🗑️
                    </button>
                </div>
            </td>
        `;

        tbody.appendChild(tr);
    });
}

/**
 * 4. Cập nhật số liệu trên 4 thẻ KPI
 */
function updateKpiCards(kpis) {
    const elTotal = document.getElementById('kpiTotalUsers');
    const elStaff = document.getElementById('kpiInternalStaff');
    const elTutors = document.getElementById('kpiActiveTutors');
    const elLocked = document.getElementById('kpiLockedUsers');

    if (elTotal) elTotal.textContent = Number(kpis.totalUsers || 0).toLocaleString('vi-VN');
    if (elStaff) elStaff.textContent = Number(kpis.internalStaff || 0).toLocaleString('vi-VN');
    if (elTutors) elTutors.textContent = Number(kpis.activeTutors || 0).toLocaleString('vi-VN');
    if (elLocked) elLocked.textContent = Number(kpis.lockedUsers || 0).toLocaleString('vi-VN');
}

/**
 * 5. Điều khiển phân trang (Pagination)
 */
function renderPagination(currentPage, totalPages, totalElements) {
    const container = document.getElementById('userPaginationControls');
    const infoText = document.getElementById('paginationInfoText');

    if (infoText) {
        const start = totalElements === 0 ? 0 : (currentPage * adminUserState.size) + 1;
        const end = Math.min((currentPage + 1) * adminUserState.size, totalElements);
        infoText.textContent = `Hiển thị ${start} - ${end} trên tổng số ${totalElements.toLocaleString('vi-VN')} người dùng`;
    }

    if (!container) return;
    container.innerHTML = '';

    if (totalPages <= 1) return;

    // Nút Trước
    const prevBtn = document.createElement('button');
    prevBtn.type = 'button';
    prevBtn.disabled = currentPage === 0;
    prevBtn.className = `px-2.5 py-1 rounded-[5px] border border-black text-xs font-semibold shadow-[1.5px_1.5px_0px_0px_#0F172A] transition-all ${currentPage === 0 ? 'opacity-40 cursor-not-allowed bg-slate-100 text-slate-400' : 'bg-white hover:bg-slate-100 text-slate-800 active:translate-x-[0.5px] active:translate-y-[0.5px] active:shadow-none'}`;
    prevBtn.innerHTML = '← Trước';
    prevBtn.onclick = () => { if (adminUserState.page > 0) { adminUserState.page--; loadUsers(); } };
    container.appendChild(prevBtn);

    // Danh sách số trang (tối đa 5 nút)
    const maxButtons = 5;
    let startPage = Math.max(0, currentPage - 2);
    let endPage = Math.min(totalPages - 1, startPage + maxButtons - 1);
    if (endPage - startPage + 1 < maxButtons) {
        startPage = Math.max(0, endPage - maxButtons + 1);
    }

    for (let p = startPage; p <= endPage; p++) {
        const pageBtn = document.createElement('button');
        pageBtn.type = 'button';
        pageBtn.className = `w-7 h-7 rounded-[5px] border border-black text-xs font-bold shadow-[1.5px_1.5px_0px_0px_#0F172A] transition-all ${p === currentPage ? 'bg-black text-white' : 'bg-white hover:bg-slate-100 text-slate-800 active:translate-x-[0.5px] active:translate-y-[0.5px] active:shadow-none'}`;
        pageBtn.textContent = p + 1;
        pageBtn.onclick = () => { adminUserState.page = p; loadUsers(); };
        container.appendChild(pageBtn);
    }

    // Nút Sau
    const nextBtn = document.createElement('button');
    nextBtn.type = 'button';
    nextBtn.disabled = currentPage >= totalPages - 1;
    nextBtn.className = `px-2.5 py-1 rounded-[5px] border border-black text-xs font-semibold shadow-[1.5px_1.5px_0px_0px_#0F172A] transition-all ${currentPage >= totalPages - 1 ? 'opacity-40 cursor-not-allowed bg-slate-100 text-slate-400' : 'bg-white hover:bg-slate-100 text-slate-800 active:translate-x-[0.5px] active:translate-y-[0.5px] active:shadow-none'}`;
    nextBtn.innerHTML = 'Sau →';
    nextBtn.onclick = () => { if (adminUserState.page < totalPages - 1) { adminUserState.page++; loadUsers(); } };
    container.appendChild(nextBtn);
}

/**
 * 6. Khởi tạo Modals và sự kiện Submit
 */
function initModals() {
    // Form Khởi tạo nhân sự mới
    const createForm = document.getElementById('createUserForm');
    if (createForm) {
        createForm.addEventListener('submit', async function (e) {
            e.preventDefault();

            const fullName = document.getElementById('createFullName').value.trim();
            const email = document.getElementById('createEmail').value.trim().toLowerCase();
            const phone = document.getElementById('createPhone').value.trim();
            const role = document.getElementById('createRole').value;
            const temporaryPassword = document.getElementById('createPassword').value;
            const status = document.getElementById('createStatus').value;
            const submitBtn = document.getElementById('btnSubmitCreateUser');

            if (!fullName || !email || !temporaryPassword) {
                showAdminToast('Vui lòng điền các trường bắt buộc (*)', 'error');
                return;
            }

            setBtnLoading(submitBtn, true, 'Đang tạo tài khoản...');

            try {
                const response = await fetch('/api/admin/users/create-staff', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    body: JSON.stringify({ fullName, email, phone, role, temporaryPassword, status })
                });

                const data = await response.json();

                if (!response.ok) {
                    throw new Error(data.message || 'Không thể khởi tạo tài khoản.');
                }

                showAdminToast(`Đã tạo tài khoản thành công! Thông tin đăng nhập đã được gửi tới ${email}.`, 'success');
                closeCreateUserModal();
                createForm.reset();
                loadUsers();

            } catch (err) {
                showAdminToast(err.message, 'error');
            } finally {
                setBtnLoading(submitBtn, false, 'Lưu tài khoản nhân viên');
            }
        });
    }

    // Form Chỉnh sửa thông tin & Vai trò
    const editForm = document.getElementById('editUserForm');
    if (editForm) {
        editForm.addEventListener('submit', async function (e) {
            e.preventDefault();

            if (!adminUserState.editingUserId) return;

            const fullName = document.getElementById('editFullName').value.trim();
            const phone = document.getElementById('editPhone').value.trim();
            const role = document.getElementById('editRole').value;
            const status = document.getElementById('editStatus').value;
            const submitBtn = document.getElementById('btnSubmitEditUser');

            setBtnLoading(submitBtn, true, 'Đang cập nhật...');

            try {
                const response = await fetch(`/api/admin/users/${adminUserState.editingUserId}`, {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    body: JSON.stringify({ fullName, phone, role, status })
                });

                const data = await response.json();

                if (!response.ok) {
                    throw new Error(data.message || 'Cập nhật tài khoản thất bại.');
                }

                showAdminToast('Cập nhật tài khoản và phân quyền thành công!', 'success');
                closeEditUserModal();
                loadUsers();

            } catch (err) {
                showAdminToast(err.message, 'error');
            } finally {
                setBtnLoading(submitBtn, false, 'Lưu thay đổi');
            }
        });
    }
}

/**
 * 7. Mở Modal Khởi tạo tài khoản
 */
function openCreateUserModal() {
    const modal = document.getElementById('createUserModal');
    if (modal) {
        modal.classList.remove('hidden');
        modal.classList.add('flex');
        const nameInput = document.getElementById('createFullName');
        if (nameInput) nameInput.focus();
    }
}

function closeCreateUserModal() {
    const modal = document.getElementById('createUserModal');
    if (modal) {
        modal.classList.add('hidden');
        modal.classList.remove('flex');
    }
}

/**
 * Sinh mật khẩu ngẫu nhiên
 */
function generateRandomPassword() {
    const uppercase = 'ABCDEFGHJKLMNPQRSTUVWXYZ';
    const lowercase = 'abcdefghijkmnopqrstuvwxyz';
    const numbers = '23456789';
    const specials = '@!#$';
    const all = uppercase + lowercase + numbers + specials;

    let passChars = [
        uppercase.charAt(Math.floor(Math.random() * uppercase.length)),
        lowercase.charAt(Math.floor(Math.random() * lowercase.length)),
        numbers.charAt(Math.floor(Math.random() * numbers.length)),
        specials.charAt(Math.floor(Math.random() * specials.length))
    ];

    const targetLength = 10 + Math.floor(Math.random() * 3);
    while (passChars.length < targetLength) {
        passChars.push(all.charAt(Math.floor(Math.random() * all.length)));
    }

    for (let i = passChars.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [passChars[i], passChars[j]] = [passChars[j], passChars[i]];
    }

    const pass = passChars.join('');
    const passInput = document.getElementById('createPassword');
    if (passInput) {
        passInput.value = pass;
        passInput.type = 'text';
    }
}

/**
 * 8. Mở Modal Chỉnh sửa thông tin & Vai trò
 */
async function openEditUserModal(userId) {
    adminUserState.editingUserId = userId;
    const modal = document.getElementById('editUserModal');

    try {
        const response = await fetch(`/api/admin/users/${userId}`, {
            headers: { 'Accept': 'application/json' }
        });

        if (!response.ok) throw new Error('Không thể tải thông tin người dùng.');

        const user = await response.json();

        document.getElementById('editFullName').value = user.fullName || '';
        document.getElementById('editEmailDisplay').value = user.email || '';
        document.getElementById('editPhone').value = (user.phone && user.phone !== '-') ? user.phone : '';
        document.getElementById('editRole').value = user.role || 'ROLE_LEARNER';
        document.getElementById('editStatus').value = user.status || 'ACTIVE';

        const selfAlert = document.getElementById('editSelfWarning');
        const roleSelect = document.getElementById('editRole');
        const statusSelect = document.getElementById('editStatus');

        if (user.isSelf) {
            if (selfAlert) selfAlert.classList.remove('hidden');
            if (roleSelect) roleSelect.disabled = true;
            if (statusSelect) statusSelect.disabled = true;
        } else {
            if (selfAlert) selfAlert.classList.add('hidden');
            if (roleSelect) roleSelect.disabled = false;
            if (statusSelect) statusSelect.disabled = false;
        }

        if (modal) {
            modal.classList.remove('hidden');
            modal.classList.add('flex');
        }

    } catch (err) {
        showAdminToast(err.message, 'error');
    }
}

function closeEditUserModal() {
    const modal = document.getElementById('editUserModal');
    if (modal) {
        modal.classList.add('hidden');
        modal.classList.remove('flex');
    }
    adminUserState.editingUserId = null;
}

/**
 * 9. Hộp thoại xác nhận Khóa / Mở khóa tài khoản
 */
function confirmToggleStatus(userId, fullName, nextStatus) {
    adminUserState.pendingActionUserId = userId;
    adminUserState.pendingActionStatus = nextStatus;

    const modal = document.getElementById('confirmActionModal');
    const titleEl = document.getElementById('confirmActionTitle');
    const descEl = document.getElementById('confirmActionDesc');
    const confirmBtn = document.getElementById('btnConfirmExecuteAction');

    const actionText = nextStatus === 'ACTIVE' ? 'Mở khóa' : 'Khóa';
    if (titleEl) titleEl.textContent = `Xác nhận ${actionText} tài khoản?`;
    if (descEl) descEl.textContent = `Bạn có chắc chắn muốn ${actionText.toLowerCase()} tài khoản của "${fullName}"? ${nextStatus === 'ACTIVE' ? 'Người dùng sẽ có thể đăng nhập bình thường.' : 'Người dùng sẽ bị chặn đăng nhập và hủy phiên làm việc hiện tại.'}`;

    if (confirmBtn) {
        confirmBtn.className = `px-3.5 py-1.5 rounded-[5px] text-xs font-bold text-slate-900 border border-black shadow-[2px_2px_0px_0px_#0F172A] transition-all active:translate-x-[1px] active:translate-y-[1px] active:shadow-none ${nextStatus === 'ACTIVE' ? 'bg-[#DCFCE7] hover:bg-[#BBF7D0]' : 'bg-[#FFE4E6] hover:bg-[#FECDD3]'}`;
        confirmBtn.textContent = actionText;
        confirmBtn.onclick = executeToggleStatus;
    }

    if (modal) {
        modal.classList.remove('hidden');
        modal.classList.add('flex');
    }
}

async function executeToggleStatus() {
    const userId = adminUserState.pendingActionUserId;
    const status = adminUserState.pendingActionStatus;
    const confirmBtn = document.getElementById('btnConfirmExecuteAction');

    setBtnLoading(confirmBtn, true, 'Đang cập nhật...');

    try {
        const response = await fetch(`/api/admin/users/${userId}/status`, {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({ status })
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.message || 'Cập nhật trạng thái thất bại.');
        }

        showAdminToast(`Đã ${status === 'ACTIVE' ? 'mở khóa' : 'tạm khóa'} tài khoản thành công!`, 'success');
        closeConfirmModal();
        loadUsers();

    } catch (err) {
        showAdminToast(err.message, 'error');
    } finally {
        setBtnLoading(confirmBtn, false, 'Xác nhận');
    }
}

/**
 * 10. Hộp thoại xác nhận Cấp lại mật khẩu tạm thời
 */
function confirmResendCredentials(userId, fullName, email) {
    adminUserState.pendingActionUserId = userId;

    const modal = document.getElementById('confirmActionModal');
    const titleEl = document.getElementById('confirmActionTitle');
    const descEl = document.getElementById('confirmActionDesc');
    const confirmBtn = document.getElementById('btnConfirmExecuteAction');

    if (titleEl) titleEl.textContent = 'Cấp lại thông tin đăng nhập?';
    if (descEl) descEl.textContent = `Hệ thống sẽ tạo mật khẩu ngẫu nhiên mới và gửi thông tin đăng nhập trực tiếp đến hộp thư "${email}" của người dùng "${fullName}".`;

    if (confirmBtn) {
        confirmBtn.className = 'px-3.5 py-1.5 rounded-[5px] text-xs font-bold text-slate-900 bg-[#FEF3C7] hover:bg-[#FDE68A] border border-black shadow-[2px_2px_0px_0px_#0F172A] transition-all active:translate-x-[1px] active:translate-y-[1px] active:shadow-none';
        confirmBtn.textContent = 'Gửi mật khẩu mới';
        confirmBtn.onclick = executeResendCredentials;
    }

    if (modal) {
        modal.classList.remove('hidden');
        modal.classList.add('flex');
    }
}

async function executeResendCredentials() {
    const userId = adminUserState.pendingActionUserId;
    const confirmBtn = document.getElementById('btnConfirmExecuteAction');

    setBtnLoading(confirmBtn, true, 'Đang gửi email...');

    try {
        const response = await fetch(`/api/admin/users/${userId}/resend-credentials`, {
            method: 'POST',
            headers: { 'Accept': 'application/json' }
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.message || 'Không thể cấp lại thông tin đăng nhập.');
        }

        showAdminToast('Đã cấp mật khẩu mới và gửi email thành công!', 'success');
        closeConfirmModal();

    } catch (err) {
        showAdminToast(err.message, 'error');
    } finally {
        setBtnLoading(confirmBtn, false, 'Gửi mật khẩu mới');
    }
}

/**
 * 11. Hộp thoại xác nhận Xóa mềm
 */
function confirmDeleteUser(userId, fullName) {
    adminUserState.pendingActionUserId = userId;

    const modal = document.getElementById('confirmActionModal');
    const titleEl = document.getElementById('confirmActionTitle');
    const descEl = document.getElementById('confirmActionDesc');
    const confirmBtn = document.getElementById('btnConfirmExecuteAction');

    if (titleEl) titleEl.textContent = 'Xác nhận xóa tài khoản?';
    if (descEl) descEl.textContent = `Hệ thống sẽ thực hiện xóa mềm (vô hiệu hóa) tài khoản "${fullName}". Lịch sử bài nộp và dữ liệu liên quan sẽ được bảo toàn nhưng tài khoản không thể đăng nhập.`;

    if (confirmBtn) {
        confirmBtn.className = 'px-3.5 py-1.5 rounded-[5px] text-xs font-bold text-slate-900 bg-[#FFE4E6] hover:bg-[#FECDD3] border border-black shadow-[2px_2px_0px_0px_#0F172A] transition-all active:translate-x-[1px] active:translate-y-[1px] active:shadow-none';
        confirmBtn.textContent = 'Xóa tài khoản';
        confirmBtn.onclick = executeDeleteUser;
    }

    if (modal) {
        modal.classList.remove('hidden');
        modal.classList.add('flex');
    }
}

async function executeDeleteUser() {
    const userId = adminUserState.pendingActionUserId;
    const confirmBtn = document.getElementById('btnConfirmExecuteAction');

    setBtnLoading(confirmBtn, true, 'Đang xóa...');

    try {
        const response = await fetch(`/api/admin/users/${userId}`, {
            method: 'DELETE',
            headers: { 'Accept': 'application/json' }
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.message || 'Xóa tài khoản thất bại.');
        }

        showAdminToast('Đã xóa mềm tài khoản thành công!', 'success');
        closeConfirmModal();
        loadUsers();

    } catch (err) {
        showAdminToast(err.message, 'error');
    } finally {
        setBtnLoading(confirmBtn, false, 'Xóa');
    }
}

function closeConfirmModal() {
    const modal = document.getElementById('confirmActionModal');
    if (modal) {
        modal.classList.add('hidden');
        modal.classList.remove('flex');
    }
    adminUserState.pendingActionUserId = null;
    adminUserState.pendingActionStatus = null;
}

/**
 * Toast thông báo chuẩn Retro-Brutalist
 */
function showAdminToast(message, type) {
    let toast = document.getElementById('adminToast');
    if (!toast) {
        toast = document.createElement('div');
        toast.id = 'adminToast';
        document.body.appendChild(toast);
    }

    if (type === 'success') {
        toast.className = 'fixed bottom-6 right-6 z-50 px-4 py-3 rounded-[5px] text-xs font-bold flex items-center gap-2 border border-black shadow-[3px_3px_0px_0px_#0F172A] bg-[#DCFCE7] text-emerald-950 transition-all transform duration-300 opacity-100 translate-y-0';
        toast.innerHTML = `<span>✅</span> <span>${message}</span>`;
    } else {
        toast.className = 'fixed bottom-6 right-6 z-50 px-4 py-3 rounded-[5px] text-xs font-bold flex items-center gap-2 border border-black shadow-[3px_3px_0px_0px_#0F172A] bg-[#FFE4E6] text-rose-950 transition-all transform duration-300 opacity-100 translate-y-0';
        toast.innerHTML = `<span>⚠️</span> <span>${message}</span>`;
    }

    setTimeout(() => {
        toast.classList.add('opacity-0', 'translate-y-2');
    }, 3500);
}

function setBtnLoading(btn, isLoading, defaultText) {
    if (!btn) return;
    if (isLoading) {
        btn.disabled = true;
        btn.innerHTML = `<span class="inline-block w-3 h-3 border-2 border-black/30 border-t-black rounded-full animate-spin mr-1"></span> <span>${defaultText}</span>`;
    } else {
        btn.disabled = false;
        btn.innerHTML = `<span>${defaultText}</span>`;
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
