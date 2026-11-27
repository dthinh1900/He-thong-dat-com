const API_BASE_URL = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '');
const SESSION_KEY = 'meal-booking-session';
const REMEMBER_KEY = 'meal-booking-remember';
export const SESSION_EXPIRED = 'meal-booking-session-expired';

function readJson(storage, key) {
  try {
    const value = storage.getItem(key);
    return value ? JSON.parse(value) : null;
  } catch {
    return null;
  }
}

export function getStoredSession() {
  return readJson(sessionStorage, SESSION_KEY) ?? readJson(localStorage, REMEMBER_KEY);
}

export function saveSession(session, remember) {
  sessionStorage.removeItem(SESSION_KEY);
  localStorage.removeItem(REMEMBER_KEY);
  const storage = remember ? localStorage : sessionStorage;
  storage.setItem(remember ? REMEMBER_KEY : SESSION_KEY, JSON.stringify(session));
}

export function clearSession() {
  sessionStorage.removeItem(SESSION_KEY);
  localStorage.removeItem(REMEMBER_KEY);
}

function buildHeaders(options = {}) {
  const session = getStoredSession();
  const headers = new Headers(options.headers ?? {});
  const isFormData = typeof FormData !== 'undefined' && options.body instanceof FormData;

  // Let the browser set the multipart boundary for FormData uploads.
  if (options.body !== undefined && !isFormData && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  if (session?.token) headers.set('Authorization', `Bearer ${session.token}`);
  return headers;
}

async function send(path, options = {}) {
  try {
    return await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers: buildHeaders(options),
    });
  } catch {
    throw new Error('Không thể kết nối đến Backend. Hãy kiểm tra Backend và PostgreSQL đã chạy chưa.');
  }
}

async function readError(response) {
  const contentType = response.headers.get('content-type') ?? '';
  const payload = contentType.includes('application/json')
    ? await response.json().catch(() => null)
    : await response.text().catch(() => '');
  if (response.status === 401 && getStoredSession()) {
    clearSession();
    window.dispatchEvent(new Event(SESSION_EXPIRED));
  }
  if (response.status >= 500 && !contentType.includes('application/json')) {
    return 'Không thể kết nối đến Backend. Hãy kiểm tra Backend và PostgreSQL đã chạy chưa.';
  }
  return typeof payload === 'object' && payload?.message
    ? payload.message
    : (typeof payload === 'string' && payload.trim()
      ? payload
      : `Yêu cầu thất bại (HTTP ${response.status}).`);
}

export async function apiRequest(path, options = {}) {
  const response = await send(path, options);
  if (!response.ok) throw new Error(await readError(response));
  if (response.status === 204) return null;

  const contentType = response.headers.get('content-type') ?? '';
  if (contentType.includes('application/json')) return response.json();
  const text = await response.text();
  return text || null;
}

export async function apiBlobRequest(path, options = {}) {
  const response = await send(path, options);
  if (!response.ok) throw new Error(await readError(response));
  return {
    blob: await response.blob(),
    headers: response.headers,
  };
}
