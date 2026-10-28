// Smart API Base URL: Uses custom URL from localStorage, or localhost for local dev
export const getApiBase = () => {
    const custom = localStorage.getItem('SMARTTOLL_API_URL');
    if (custom) return custom.replace(/\/$/, '');
    if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
        return 'http://localhost:8080';
    }
    // Production cloud backend: Automatically used when opened on mobile, Vercel, or any other device!
    return 'https://smarttoll-ai.onrender.com';
};

// Helper for users to switch backend URL easily in production
window.setBackendUrl = (url) => {
    if (url) {
        localStorage.setItem('SMARTTOLL_API_URL', url.trim());
        console.log('SmartToll backend URL updated to: ' + url);
    } else {
        localStorage.removeItem('SMARTTOLL_API_URL');
        console.log('Reset SmartToll backend URL to default.');
    }
    window.location.reload();
};

const API_BASE = getApiBase();

// Helper to get auth headers
function getHeaders() {
    const headers = { 'Content-Type': 'application/json' };
    const userStr = sessionStorage.getItem('smartTollUser');
    if (userStr) {
        const user = JSON.parse(userStr);
        if (user.token) {
            headers['Authorization'] = `Bearer ${user.token}`;
        }
    }
    return headers;
}

// Helper to handle API responses
async function handleResponse(response) {
    if (!response.ok) {
        const error = await response.text();
        throw new Error(error || 'API Request Failed');
    }
    // Handle empty responses
    const text = await response.text();
    return text ? JSON.parse(text) : {};
}

// User API
export const apiLogin = async (email, password) => {
    const response = await fetch(`${API_BASE}/user/login`, {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify({ email, password })
    });
    return handleResponse(response);
};

export const apiRegister = async (name, email, phone, password) => {
    const response = await fetch(`${API_BASE}/user/register`, {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify({ name, email, phone, password })
    });
    return handleResponse(response);
};

// Vehicle API
export const apiGetVehicles = async () => {
    const response = await fetch(`${API_BASE}/vehicle/getall`, {
        headers: getHeaders()
    });
    return handleResponse(response);
};

export const apiAddVehicle = async (vehicleNumber, vehicleType) => {
    const userStr = sessionStorage.getItem('smartTollUser');
    const user = userStr ? JSON.parse(userStr) : null;
    
    const response = await fetch(`${API_BASE}/vehicle/add`, {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify({ 
            vehicleNumber, 
            vehicleType,
            user: user ? { id: user.id } : null
        })
    });
    return handleResponse(response);
};

export const apiDeleteVehicle = async (id) => {
    const response = await fetch(`${API_BASE}/vehicle/${id}`, {
        method: 'DELETE',
        headers: getHeaders()
    });
    return handleResponse(response);
};

// Toll API
export const apiGetTolls = async () => {
    const response = await fetch(`${API_BASE}/toll/getall`, {
        headers: getHeaders()
    });
    return handleResponse(response);
};

export const apiAddToll = async (name, location, amount, email, password) => {
    const response = await fetch(`${API_BASE}/toll/add`, {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify({ name, location, amount, email, password })
    });
    return handleResponse(response);
};

export const apiGetTollById = async (id) => {
    const response = await fetch(`${API_BASE}/toll/${id}`, {
        headers: getHeaders()
    });
    return handleResponse(response);
};

export const apiTollLogin = async (email, password) => {
    const response = await fetch(`${API_BASE}/toll/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
    });
    return handleResponse(response);
};

export const apiDeleteToll = async (id) => {
    const response = await fetch(`${API_BASE}/toll/${id}`, {
        method: 'DELETE',
        headers: getHeaders()
    });
    return handleResponse(response);
};

// ANPR API
export const apiProcessANPR = async (imageFile) => {
    const formData = new FormData();
    formData.append('image', imageFile);

    // We MUST NOT set 'Content-Type': 'application/json' for FormData
    // The browser will automatically set 'Content-Type': 'multipart/form-data' with the correct boundary
    const headers = getHeaders();
    delete headers['Content-Type'];

    const response = await fetch(`${API_BASE}/anpr/process`, {
        method: 'POST',
        headers: headers,
        body: formData
    });
    return handleResponse(response);
};

// Payment API
export const apiGetPayments = async () => {
    const response = await fetch(`${API_BASE}/payment/getall`, {
        headers: getHeaders()
    });
    return handleResponse(response);
};

export const apiAddPayment = async (vehicleId, tollId, amount) => {
    const transactionId = 'TXN' + Date.now();
    const now = new Date();
    const validUntil = new Date(now.getTime() + 24 * 60 * 60 * 1000); // Valid for 24 hours
    
    const response = await fetch(`${API_BASE}/payment/add`, {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify({
            vehicle: { id: vehicleId },
            toll: { id: tollId },
            amount: amount,
            transactionId: transactionId,
            paymentStatus: 'SUCCESS',
            paymentTime: now.toISOString(),
            validUntil: validUntil.toISOString()
        })
    });
    return handleResponse(response);
};
