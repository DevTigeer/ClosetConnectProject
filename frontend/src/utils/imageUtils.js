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

  // /로 시작하는 상대 경로면 API_BASE와 합침
  if (imageUrl.startsWith('/')) {
    return `${API_BASE}${imageUrl}`;
  }

  // 그 외의 경우 /를 추가해서 합침
  return `${API_BASE}/${imageUrl}`;
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
