// ==========================================================================
// SmartToll AI - Modern Frontend Utilities & UI Components
// ==========================================================================

// Ensure Toast Portal Container exists
function getToastPortal() {
    let portal = document.getElementById('smarttoll-toast-portal');
    if (!portal) {
        portal = document.createElement('div');
        portal.id = 'smarttoll-toast-portal';
        portal.className = 'toast-portal';
        document.body.appendChild(portal);
    }
    return portal;
}

/**
 * Display a modern, non-blocking toast notification
 * @param {string} title 
 * @param {string} message 
 * @param {'success'|'error'|'warning'|'info'} type 
 * @param {number} durationMs 
 */
export function showToast(title, message, type = 'success', durationMs = 4500) {
    const portal = getToastPortal();
    const toast = document.createElement('div');
    toast.className = `toast-item toast-item-${type}`;

    let iconClass = 'fa-check-circle text-success';
    if (type === 'error') iconClass = 'fa-circle-exclamation text-danger';
    if (type === 'warning') iconClass = 'fa-triangle-exclamation text-warning';
    if (type === 'info') iconClass = 'fa-circle-info text-info';

    toast.innerHTML = `
        <div class="fs-5 mt-1"><i class="fas ${iconClass}"></i></div>
        <div class="flex-grow-1">
            <h6 class="fw-bold mb-1 fs-6">${title}</h6>
            <div class="text-secondary small mb-0">${message}</div>
        </div>
        <button type="button" class="btn-close btn-sm ms-2" aria-label="Close"></button>
    `;

    const closeBtn = toast.querySelector('.btn-close');
    const dismiss = () => {
        toast.classList.add('removing');
        setTimeout(() => toast.remove(), 300);
    };

    closeBtn.addEventListener('click', dismiss);
    if (durationMs > 0) {
        setTimeout(dismiss, durationMs);
    }

    portal.appendChild(toast);
}

/**
 * Backwards compatible alert banner in container
 */
export function showAlert(containerId, message, isError = true) {
    const container = document.getElementById(containerId);
    if (!container) {
        showToast(isError ? 'Action Failed' : 'Success', message, isError ? 'error' : 'success');
        return;
    }

    container.innerHTML = `
        <div class="alert ${isError ? 'alert-danger' : 'alert-success'} alert-dismissible fade show border-0 shadow-sm d-flex align-items-center gap-3 p-3 rounded-3" role="alert">
            <i class="fas ${isError ? 'fa-circle-exclamation fs-5' : 'fa-circle-check fs-5'}"></i>
            <div class="flex-grow-1 fw-medium">${message}</div>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    `;
    
    // Auto-dismiss after 6 seconds
    setTimeout(() => {
        if (container.innerHTML !== '') {
            container.innerHTML = '';
        }
    }, 6000);
}

/**
 * Modern Confirmation Modal (replaces window.confirm)
 * Returns a Promise that resolves to true (confirmed) or false (cancelled)
 */
export function showConfirmModal({
    title = 'Are you sure?',
    message = 'This action cannot be undone.',
    confirmText = 'Yes, Proceed',
    cancelText = 'Cancel',
    isDanger = true
}) {
    return new Promise((resolve) => {
        let modalEl = document.getElementById('smarttoll-confirm-modal');
        if (modalEl) modalEl.remove();

        modalEl = document.createElement('div');
        modalEl.id = 'smarttoll-confirm-modal';
        modalEl.className = 'modal fade';
        modalEl.tabIndex = -1;
        modalEl.innerHTML = `
            <div class="modal-dialog modal-dialog-centered" style="max-width: 440px;">
                <div class="modal-content border-0 shadow-lg rounded-4 p-2">
                    <div class="modal-body text-center p-4">
                        <div class="icon-box ${isDanger ? 'icon-box-danger' : 'icon-box-primary'} mx-auto mb-3" style="width: 60px; height: 60px; font-size: 1.6rem;">
                            <i class="fas ${isDanger ? 'fa-triangle-exclamation' : 'fa-circle-question'}"></i>
                        </div>
                        <h4 class="fw-bold mb-2">${title}</h4>
                        <p class="text-secondary small mb-4">${message}</p>
                        <div class="d-flex gap-2 justify-content-center">
                            <button type="button" class="btn btn-light-custom px-4 rounded-pill" id="confirm-modal-cancel">${cancelText}</button>
                            <button type="button" class="btn ${isDanger ? 'btn-danger' : 'btn-primary'} px-4 rounded-pill fw-semibold shadow-sm" id="confirm-modal-ok">${confirmText}</button>
                        </div>
                    </div>
                </div>
            </div>
        `;
        document.body.appendChild(modalEl);

        const bsModal = new bootstrap.Modal(modalEl, { backdrop: 'static' });
        
        const cancelBtn = modalEl.querySelector('#confirm-modal-cancel');
        const okBtn = modalEl.querySelector('#confirm-modal-ok');

        cancelBtn.addEventListener('click', () => {
            bsModal.hide();
            resolve(false);
        });

        okBtn.addEventListener('click', () => {
            bsModal.hide();
            resolve(true);
        });

        modalEl.addEventListener('hidden.bs.modal', () => {
            modalEl.remove();
        });

        bsModal.show();
    });
}

/**
 * Check Authentication State
 */
export function checkAuth() {
    const user = sessionStorage.getItem('smartTollUser');
    const isInPages = window.location.pathname.includes('/pages/');
    const loginTarget = isInPages ? 'login.html' : 'pages/login.html';

    if (!user) {
        window.location.href = loginTarget;
        return null;
    }
    try {
        return JSON.parse(user);
    } catch (e) {
        sessionStorage.removeItem('smartTollUser');
        window.location.href = loginTarget;
        return null;
    }
}

/**
 * Logout
 */
export function logout() {
    sessionStorage.removeItem('smartTollUser');
    const isInPages = window.location.pathname.includes('/pages/');
    window.location.href = isInPages ? '../index.html' : 'index.html';
}

/**
 * Initialize and render responsive, state-aware navbar
 */
export function setupNavbar() {
    const navLinks = document.getElementById('nav-links');
    if (!navLinks) return;

    const userStr = sessionStorage.getItem('smartTollUser');
    const isInPages = window.location.pathname.includes('/pages/');
    const prefix = isInPages ? '' : 'pages/';
    const currentFile = window.location.pathname.split('/').pop() || 'index.html';

    if (userStr) {
        let user;
        try {
            user = JSON.parse(userStr);
        } catch {
            user = {};
        }

        const role = user.role ? user.role.toUpperCase() : 'USER';
        const isOperator = role === 'ROLE_TOLL_OPERATOR';
        const isAdmin = role === 'ADMIN' || role === 'ROLE_ADMIN';
        
        let roleBadge = '<span class="badge bg-secondary ms-1 small" style="font-size: 0.65rem;">USER</span>';
        if (isAdmin) roleBadge = '<span class="badge bg-danger ms-1 small" style="font-size: 0.65rem;">ADMIN</span>';
        if (isOperator) roleBadge = '<span class="badge bg-info text-dark ms-1 small" style="font-size: 0.65rem;">OPERATOR</span>';

        const name = user.name || (isOperator ? 'Toll Operator' : 'User');
        const initials = name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase() || 'U';

        navLinks.innerHTML = `
            <ul class="navbar-nav ms-auto mb-2 mb-lg-0 align-items-lg-center gap-1">
                <li class="nav-item">
                    <a class="nav-link ${currentFile === 'dashboard.html' ? 'active' : ''}" href="${prefix}dashboard.html">
                        <i class="fas fa-chart-pie me-1"></i> Dashboard
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link ${currentFile === 'vehicles.html' ? 'active' : ''}" href="${prefix}vehicles.html">
                        <i class="fas fa-car me-1"></i> Vehicles
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link ${currentFile === 'tolls.html' ? 'active' : ''}" href="${prefix}tolls.html">
                        <i class="fas fa-road me-1"></i> Toll Plazas
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link ${currentFile === 'payments.html' ? 'active' : ''}" href="${prefix}payments.html">
                        <i class="fas fa-receipt me-1"></i> Payments
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link ${currentFile === 'anpr.html' ? 'active' : ''}" href="${prefix}anpr.html">
                        <i class="fas fa-camera-rotate me-1 text-info"></i> AI ANPR
                    </a>
                </li>
                <li class="nav-item ms-lg-3 my-2 my-lg-0">
                    <div class="user-avatar-pill">
                        <span class="avatar-initials">${initials}</span>
                        <span>${name}</span>
                        ${roleBadge}
                    </div>
                </li>
                <li class="nav-item ms-lg-2">
                    <button class="btn btn-outline-danger btn-sm btn-pill px-3 py-1" id="logout-btn" title="Sign Out">
                        <i class="fas fa-arrow-right-from-bracket"></i>
                        <span class="d-lg-none ms-1">Logout</span>
                    </button>
                </li>
            </ul>
        `;

        document.getElementById('logout-btn')?.addEventListener('click', async (e) => {
            e.preventDefault();
            const confirmed = await showConfirmModal({
                title: 'Sign Out?',
                message: 'Are you sure you want to log out of your session?',
                confirmText: 'Log Out',
                isDanger: false
            });
            if (confirmed) {
                logout();
            }
        });

    } else {
        // Public links
        navLinks.innerHTML = `
            <ul class="navbar-nav ms-auto mb-2 mb-lg-0 align-items-lg-center gap-2">
                <li class="nav-item">
                    <a class="nav-link ${currentFile === 'operator-login.html' ? 'active' : ''}" href="${prefix}operator-login.html">
                        <i class="fas fa-torii-gate me-1 text-info"></i> Operator Portal
                    </a>
                </li>
                <li class="nav-item">
                    <a class="btn btn-outline-light btn-pill px-4" href="${prefix}login.html">
                        <i class="fas fa-user me-1"></i> Sign In
                    </a>
                </li>
                <li class="nav-item">
                    <a class="btn btn-gradient btn-pill px-4" href="${prefix}register.html">
                        Get Started <i class="fas fa-arrow-right ms-1"></i>
                    </a>
                </li>
            </ul>
        `;
    }
}
