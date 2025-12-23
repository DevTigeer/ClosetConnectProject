const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080';

/**
 * 이미지 URL을 절대 경로로 변환
 * @param {string} imageUrl - API에서 받은 이미지 URL
 * @returns {string} - 완전한 이미지 URL
 */
export function getImageUrl(imageUrl) {
  if (!imageUrl) {
    return '/placeholder.jpg';
  }

  // 이미 http/https로 시작하는 절대 경로면 그대로 반환
  if (imageUrl.startsWith('http://') || imageUrl.startsWith('https://')) {
    return imageUrl;
  }

  // API_BASE가 비어있거나 /만 있으면 imageUrl 그대로 반환
  if (!API_BASE || API_BASE === '/') {
    return imageUrl.startsWith('/') ? imageUrl : `/${imageUrl}`;
  }

  // API_BASE와 imageUrl을 합칠 때 중복 슬래시 제거
  const base = API_BASE.endsWith('/') ? API_BASE.slice(0, -1) : API_BASE;
  const path = imageUrl.startsWith('/') ? imageUrl : `/${imageUrl}`;
  return `${base}${path}`;
}

/**
 * 옷 이미지 URL 가져오기
 */
export function getClothImageUrl(cloth) {
  return getImageUrl(cloth?.imageUrl);
}

/**
 * 상품 이미지 URL 가져오기
 */
export function getProductImageUrl(product) {
  return getImageUrl(product?.imageUrl);
}

/**
 * 이미지 로드 실패 시 대체 이미지 설정
 */
export function handleImageError(e, fallbackUrl = '/placeholder.jpg') {
  e.target.src = fallbackUrl;
}
