/**
 * JWT 토큰을 디코딩하여 payload를 반환합니다.
 * @param {string} token - JWT 토큰
 * @returns {object|null} - 디코딩된 payload 또는 null
 */
export function decodeJWT(token) {
  if (!token) return null;

  try {
    const parts = token.split('.');
    if (parts.length !== 3) {
      console.error('Invalid JWT format');
      return null;
    }

    const payload = parts[1];
    const decoded = JSON.parse(atob(payload));
    return decoded;
  } catch (error) {
    console.error('Failed to decode JWT:', error);
    return null;
  }
}

/**
 * localStorage에서 JWT 토큰을 가져와 userId를 반환합니다.
 * @returns {number|null} - 현재 로그인한 사용자의 userId 또는 null
 */
export function getCurrentUserId() {
  const token = localStorage.getItem('accessToken');
  if (!token) return null;

  const payload = decodeJWT(token);
  if (!payload) return null;

  // JWT payload에서 userId 추출
  // 백엔드에서 사용하는 필드명에 따라 조정 필요 (uid, userId, sub 등)
  return payload.uid || payload.userId || payload.sub || null;
}

/**
 * JWT 토큰이 만료되었는지 확인합니다.
 * @param {string} token - JWT 토큰
 * @returns {boolean} - 만료 여부
 */
export function isTokenExpired(token) {
  const payload = decodeJWT(token);
  if (!payload || !payload.exp) return true;

  const now = Math.floor(Date.now() / 1000);
  return payload.exp < now;
}
