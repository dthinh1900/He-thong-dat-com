import { apiRequest, clearSession, saveSession } from './apiClient';

export async function signIn({ username, password, remember }) {
  if (!username.trim() || !password) {
    throw new Error('Vui lòng nhập tên đăng nhập và mật khẩu.');
  }
  const result = await apiRequest('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username: username.trim(), password }),
  });
  saveSession({ token: result.token, user: result.user }, remember);
  return result.user;
}

export async function getCurrentUser() {
  return apiRequest('/api/users/me');
}

export async function updateCurrentUser(profile) {
  return apiRequest('/api/users/me', {
    method: 'PATCH',
    body: JSON.stringify(profile),
  });
}

export function signOut() {
  clearSession();
}
