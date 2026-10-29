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
    const response = await fetch(`${API_BASE}/vehicle/delete/${id}`, {
        method: 'DELETE',
        headers: getHeaders()
    });
    return handleResponse(response);
};

// Toll Plaza API
export const apiGetTolls = async () => {
    const response = await fetch(`${API_BASE}/toll/getall`, {
        headers: getHeaders()
    });
    return handleResponse(response);
};

export const apiGetTollById = async (id) => {
    const response = await fetch(`${API_BASE}/toll/${id}`, {
        headers: getHeaders()
    });
    return handleResponse(response);
};

export const apiAddToll = async (nameOrObj, location, amount, email, password) => {
    let tollData;
    if (typeof nameOrObj === 'object') {
        tollData = nameOrObj;
    } else {
        tollData = { name: nameOrObj, location, amount, email, password };
    }
    const response = await fetch(`${API_BASE}/toll/add`, {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify(tollData)
    });
    return handleResponse(response);
};

export const apiLoginToll = async (email, password) => {
    const response = await fetch(`${API_BASE}/toll/login`, {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify({ email, password })
    });
    return handleResponse(response);
};

// Alias so both apiLoginToll and apiTollLogin work seamlessly across all pages
export const apiTollLogin = apiLoginToll;

export const apiDeleteToll = async (id) => {
    const response = await fetch(`${API_BASE}/toll/${id}`, {
        method: 'DELETE',
        headers: getHeaders()
    });
    return handleResponse(response);
};

/**
 * Resizes large camera photos (e.g. 5-12MB from mobile devices) to max 800px.
 * Reduces upload payload from ~8MB to ~35KB for near-instant mobile transmission and inference.
 */
async function resizeImageForUpload(file, maxDim = 800, quality = 0.8) {
    if (!file) return file;
    return new Promise((resolve) => {
        const img = new Image();
        const url = URL.createObjectURL(file);
        img.onload = () => {
            URL.revokeObjectURL(url);
            let w = img.width;
            let h = img.height;
            if (w > maxDim || h > maxDim) {
                if (w > h) {
                    h = Math.round((h * maxDim) / w);
                    w = maxDim;
                } else {
                    w = Math.round((w * maxDim) / h);
                    h = maxDim;
                }
            }
            const canvas = document.createElement('canvas');
            canvas.width = w;
            canvas.height = h;
            const ctx = canvas.getContext('2d');
            ctx.drawImage(img, 0, 0, w, h);
            canvas.toBlob((blob) => {
                resolve(blob || file);
            }, 'image/jpeg', quality);
        };
        img.onerror = () => {
            URL.revokeObjectURL(url);
            resolve(file);
        };
        img.src = url;
    });
}

// ANPR API
export const apiProcessANPR = async (imageFile) => {
    const optimizedFile = await resizeImageForUpload(imageFile);
    const formData = new FormData();
    formData.append('image', optimizedFile, 'vehicle.jpg');

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
    // Valid for 24 hours
    const expiresAt = new Date(now.getTime() + 24 * 60 * 60 * 1000);

    const formatDateTime = (date) => {
        return date.toISOString().slice(0, 19);
    };

    const response = await fetch(`${API_BASE}/payment/add`, {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify({
            vehicle: { id: vehicleId },
            toll: { id: tollId },
            amount: amount,
            transactionId: transactionId,
            paymentTime: formatDateTime(now),
            expiresAt: formatDateTime(expiresAt)
        })
    });
    return handleResponse(response);
};

export const apiCheckVehiclePayment = async (vehicleId, tollId) => {
    const response = await fetch(`${API_BASE}/payment/check/${vehicleId}/${tollId}`, {
        headers: getHeaders()
    });
    return handleResponse(response);
};
