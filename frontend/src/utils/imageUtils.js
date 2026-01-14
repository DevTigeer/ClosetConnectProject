const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
const IS_PRODUCTION = import.meta.env.MODE === 'production';

/**
 * SVG placeholder 이미지 (data URL)
 * 외부 파일에 의존하지 않는 안전한 fallback
 */
const PLACEHOLDER_SVG = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjQwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjQwMCIgZmlsbD0iI2YzZjRmNiIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmb250LWZhbWlseT0iQXJpYWwiIGZvbnQtc2l6ZT0iMTgiIGZpbGw9IiM5Y2EzYWYiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIj7snbTrr7jsp4A8L3RleHQ+PC9zdmc+';

/**
 * 이미지 URL을 절대 경로로 변환
 * - 개발 환경: http://localhost:8080/uploads/...
 * - 운영 환경: Railway HTTPS URL + /uploads/...
 *
 * @param {string} imageUrl - API에서 받은 이미지 URL
 * @returns {string} - 완전한 이미지 URL
 */
export function getImageUrl(imageUrl) {
  if (!imageUrl) {
    return PLACEHOLDER_SVG;
  }

  // 이미 http/https로 시작하는 절대 경로면 그대로 반환
  if (imageUrl.startsWith('http://') || imageUrl.startsWith('https://')) {
    return imageUrl;
  }

  // API_BASE가 없거나 '/' 또는 빈 값인 경우
  if (!API_BASE || API_BASE === '/' || API_BASE === '') {
    // /uploads/... 형태로 반환
    return imageUrl.startsWith('/') ? imageUrl : `/${imageUrl}`;
  }

  // 개발/운영 환경 모두 API_BASE + imageUrl
  // API_BASE와 imageUrl을 합칠 때 중복 슬래시 제거
  const base = API_BASE.endsWith('/') ? API_BASE.slice(0, -1) : API_BASE;
  const path = imageUrl.startsWith('/') ? imageUrl : `/${imageUrl}`;
  return `${base}${path}`;
}

/**
 * 옷 이미지 URL 가져오기
 * 우선순위: imageUrl (사용자 선택) > inpainted > segmented > removedBg > original
 */
export function getClothImageUrl(cloth) {
  if (!cloth) return getImageUrl(null);

  // 사용자가 확정한 이미지가 최우선
  const selectedUrl =
    cloth.imageUrl ||               // 1순위: 사용자가 선택한 최종 이미지
    cloth.inpaintedImageUrl ||      // 2순위: 복원된 최종 이미지
    cloth.segmentedImageUrl ||      // 3순위: 세그멘테이션 결과
    cloth.removedBgImageUrl ||      // 4순위: 배경 제거 이미지
    cloth.originalImageUrl;         // 5순위: 원본 이미지

  return getImageUrl(selectedUrl);
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
export function handleImageError(e, fallbackUrl = PLACEHOLDER_SVG) {
  const originalSrc = e.target.src;

  // 이미 SVG placeholder를 사용 중이면 더 이상 fallback 시도하지 않음
  if (originalSrc === PLACEHOLDER_SVG) {
    console.warn(`[Image Error] Already using SVG placeholder, stopping fallback chain`);
    e.target.onerror = null;
    return;
  }

  console.warn(`[Image Error] Failed to load: ${originalSrc}, using fallback: ${fallbackUrl === PLACEHOLDER_SVG ? 'SVG placeholder' : fallbackUrl}`);
  e.target.src = fallbackUrl;
  e.target.onerror = null; // 무한 루프 방지
}

/**
 * 디버깅: 이미지 URL 변환 테스트
 */
export function debugImageUrl(imageUrl) {
  console.group('Image URL Debug');
  console.log('Input:', imageUrl);
  console.log('API_BASE:', API_BASE);
  console.log('IS_PRODUCTION:', IS_PRODUCTION);
  console.log('Output:', getImageUrl(imageUrl));
  console.groupEnd();
}
