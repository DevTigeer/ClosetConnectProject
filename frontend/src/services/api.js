import axios from 'axios';

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

// axios 인스턴스 생성
const api = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 요청 인터셉터: 모든 요청에 토큰 자동 추가
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = token.startsWith('Bearer ') ? token : `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 응답 인터셉터: 401 에러 시 로그인 페이지로 리다이렉트
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      console.log('🚨 401 Unauthorized - 토큰 만료 또는 유효하지 않음');

      // 현재 경로 저장 (로그인 후 돌아오기 위해)
      const currentPath = window.location.pathname + window.location.search;
      if (currentPath !== '/login' && currentPath !== '/signup' && currentPath !== '/home') {
        sessionStorage.setItem('redirectAfterLogin', currentPath);
        console.log('💾 로그인 후 복귀 경로 저장:', currentPath);
      }

      localStorage.removeItem('accessToken');

      // 커스텀 이벤트 발송하여 UI 업데이트 트리거
      window.dispatchEvent(new Event('auth-logout'));

      // 로그인 페이지로 리다이렉트 (state로 메시지 전달)
      window.location.href = '/login?session=expired';
    }
    return Promise.reject(error);
  }
);

// ========== Auth API ==========
export const authAPI = {
  // 회원가입
  signup: (data) => api.post('/api/v1/auth/signup', data),

  // 로그인
  login: (data) => api.post('/api/v1/auth/login', data),
};

// ========== Cloth API ==========
export const clothAPI = {
  // 옷 목록 조회 (페이징, 카테고리 필터)
  list: (params) => api.get('/api/v1/cloth', { params }),

  // 옷 단건 조회
  getOne: (id) => api.get(`/api/v1/cloth/${id}`),

  // 옷 등록
  create: (data) => api.post('/api/v1/cloth', data),

  // 이미지 업로드와 함께 옷 등록 (rembg 배경 제거 포함)
  uploadWithImage: (file, name, category, imageType = 'FULL_BODY') => {
    const formData = new FormData();
    formData.append('image', file);
    formData.append('name', name);
    formData.append('category', category);
    formData.append('imageType', imageType);
    return api.post('/api/v1/cloth/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
  },

  // 최종 이미지 선택 및 옷 확정
  confirmImage: (id, data) => api.post(`/api/v1/cloth/${id}/confirm-image`, data),

  // 처리 결과 거부 (저장하지 않고 삭제)
  reject: (id) => api.post(`/api/v1/cloth/${id}/reject`),

  // 옷 삭제
  delete: (id) => api.delete(`/api/v1/cloth/${id}`),
};

// ========== Upload API ==========
export const uploadAPI = {
  // 이미지 업로드
  upload: (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post('/api/v1/uploads', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
  },
};

// ========== Community Board API ==========
export const boardAPI = {
  // 공개 보드 목록
  listPublic: () => api.get('/api/v1/community/boards'),

  // 보드 단건 조회
  getBySlug: (slug) => api.get(`/api/v1/community/boards/${slug}`),
};

// ========== Post API ==========
export const postAPI = {
  // 게시글 목록
  list: (boardId, params) => api.get(`/api/v1/boards/${boardId}/posts`, { params }),

  // 게시글 단건 조회 (상세)
  getOne: (postId) => api.get(`/api/v1/posts/${postId}`),

  // 게시글 작성
  create: (boardId, data) => api.post(`/api/v1/boards/${boardId}/posts`, data),

  // 게시글 수정
  update: (postId, data) => api.patch(`/api/v1/posts/${postId}`, data),

  // 게시글 삭제
  delete: (postId) => api.delete(`/api/v1/posts/${postId}`),

  // 좋아요
  like: (postId) => api.post(`/api/v1/posts/${postId}/like`),

  // 좋아요 취소
  unlike: (postId) => api.delete(`/api/v1/posts/${postId}/like`),

  // 좋아요 정보 조회
  getLikes: (postId) => api.get(`/api/v1/posts/${postId}/likes`),

  // 좋아요 수 조회
  getLikeCount: (postId) => api.get(`/api/v1/posts/${postId}/likes/count`),

  // 첨부파일 업로드
  uploadAttachment: (postId, file) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post(`/api/v1/posts/${postId}/attachments`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
  },
};

// ========== Comment API ==========
export const commentAPI = {
  // 댓글 목록
  list: (postId, params) => api.get(`/api/v1/posts/${postId}/comments`, { params }),

  // 댓글 작성
  create: (postId, data) => api.post(`/api/v1/posts/${postId}/comments`, data),

  // 댓글 수정
  update: (postId, commentId, data) => api.patch(`/api/v1/posts/${postId}/comments/${commentId}`, data),

  // 댓글 삭제
  delete: (postId, commentId) => api.delete(`/api/v1/posts/${postId}/comments/${commentId}`),
};

// ========== User API ==========
export const userAPI = {
  // 내 정보 조회
  me: () => api.get('/api/v1/users/me'),

  // 특정 사용자 정보 조회
  getById: (id) => api.get(`/api/v1/users/${id}`),
};

// ========== Admin Board API ==========
export const adminBoardAPI = {
  // 모든 보드 목록 (관리자)
  listAll: () => api.get('/api/v1/admin/community/boards'),

  // 보드 생성
  create: (data) => api.post('/api/v1/admin/community/boards', data),

  // 보드 수정
  update: (id, data) => api.patch(`/api/v1/admin/community/boards/${id}`, data),

  // 공개범위 변경
  changeVisibility: (id, visibility) =>
    api.patch(`/api/v1/admin/community/boards/${id}/visibility`, { visibility }),

  // 보드 삭제
  delete: (id) => api.delete(`/api/v1/admin/community/boards/${id}`),
};

// ========== Admin Post API ==========
export const adminPostAPI = {
  // 게시글 목록 (관리자)
  list: (boardId, params) => api.get(`/api/v1/admin/boards/${boardId}/posts`, { params }),

  // 게시글 상태 변경
  updateStatus: (postId, status, reason) =>
    api.patch(`/api/v1/admin/posts/${postId}/status`, { status, reason }),

  // 게시글 핀 고정/해제
  pin: (postId, pinned) => api.patch(`/api/v1/admin/posts/${postId}/pin`, { pinned }),

  // 게시글 보드 이동
  move: (postId, toBoardId) =>
    api.patch(`/api/v1/admin/posts/${postId}/move`, { toBoardId }),

  // 게시글 하드 삭제
  hardDelete: (postId) => api.delete(`/api/v1/admin/posts/${postId}/hard`),
};

// ========== Weather API ==========
export const weatherAPI = {
  // 도시 프리셋 목록
  getCities: () => api.get('/api/v1/weather/cities'),

  // 현재 위치 날씨
  getCurrent: (latitude, longitude) =>
    api.get('/api/v1/weather/current', { params: { latitude, longitude } }),

  // 도시별 날씨
  getByCity: (cityCode) => api.get(`/api/v1/weather/city/${cityCode}`),

  // 기본 날씨 (서울)
  getDefault: () => api.get('/api/v1/weather/default'),

  // 도시별 옷 추천
  getRecommendationByCity: (cityCode) =>
    api.get(`/api/v1/weather/recommendation/${cityCode}`),

  // 현재 위치 옷 추천
  getRecommendationByLocation: (latitude, longitude) =>
    api.get('/api/v1/weather/recommendation', { params: { latitude, longitude } }),

  // 커스텀 옷 추천
  getCustomRecommendation: (temperature, weatherCondition, humidity) =>
    api.get('/api/v1/weather/recommendation/custom', {
      params: { temperature, weatherCondition, humidity },
    }),
};

// ========== Market Product API ==========
export const marketAPI = {
  // 상품 목록 조회
  list: (params) => api.get('/api/v1/market/products', { params }),

  // 상품 상세 조회
  getOne: (id) => api.get(`/api/v1/market/products/${id}`),

  // 상품 등록
  create: (data) => api.post('/api/v1/market/products', data),

  // 상품 수정
  update: (id, data) => api.patch(`/api/v1/market/products/${id}`, data),

  // 상품 상태 변경
  changeStatus: (id, status) =>
    api.patch(`/api/v1/market/products/${id}/status`, { status }),

  // 상품 삭제
  delete: (id) => api.delete(`/api/v1/market/products/${id}`),

  // 판매자별 상품 목록
  listBySeller: (sellerId, params) =>
    api.get(`/api/v1/market/products/seller/${sellerId}`, { params }),

  // 찜하기
  like: (id) => api.post(`/api/v1/market/products/${id}/like`),

  // 찜 취소
  unlike: (id) => api.delete(`/api/v1/market/products/${id}/like`),

  // 찜한 상품 목록
  getLiked: (params) => api.get('/api/v1/market/liked', { params }),
};

// ========== Market Comment API ==========
export const marketCommentAPI = {
  // 댓글 목록
  list: (productId) => api.get(`/api/v1/market/products/${productId}/comments`),

  // 댓글 작성
  create: (productId, data) =>
    api.post(`/api/v1/market/products/${productId}/comments`, data),

  // 댓글 삭제
  delete: (productId, commentId) =>
    api.delete(`/api/v1/market/products/${productId}/comments/${commentId}`),
};

// ========== Market Chat API ==========
export const marketChatAPI = {
  // 채팅방 목록
  listRooms: () => api.get('/api/v1/market/chat/rooms'),

  // 채팅방 생성
  createRoom: (data) => api.post('/api/v1/market/chat/rooms', data),

  // 채팅 메시지 목록
  listMessages: (roomId, params) =>
    api.get(`/api/v1/market/chat/rooms/${roomId}/messages`, { params }),

  // 메시지 전송 (HTTP)
  sendMessage: (roomId, data) =>
    api.post(`/api/v1/market/chat/rooms/${roomId}/messages`, data),

  // 읽음 처리
  markAsRead: (roomId) => api.post(`/api/v1/market/chat/rooms/${roomId}/read`),
};

// ========== Market Order API ==========
export const marketOrderAPI = {
  // 주문 생성
  create: (data) => api.post('/api/v1/market/orders', data),

  // 주문 상세 조회
  getOne: (orderId) => api.get(`/api/v1/market/orders/${orderId}`),

  // 내 구매 주문 목록
  getBuyerOrders: (params) => api.get('/api/v1/market/orders/buyer', { params }),

  // 내 판매 주문 목록
  getSellerOrders: (params) => api.get('/api/v1/market/orders/seller', { params }),

  // 발송 처리
  ship: (orderId, data) => api.post(`/api/v1/market/orders/${orderId}/ship`, data),

  // 구매 확정
  confirm: (orderId) => api.post(`/api/v1/market/orders/${orderId}/confirm`),

  // 주문 취소
  cancel: (orderId, data) => api.delete(`/api/v1/market/orders/${orderId}`, { data }),
};

// ========== Custom Request (유연한 API 호출) ==========
export const customRequest = (method, url, data = null, config = {}) => {
  return api.request({
    method,
    url,
    data,
    ...config,
  });
};

// marketAPI에 customRequest 추가
marketAPI.customRequest = customRequest;

// ========== Outfit Try-On API ==========
export const outfitAPI = {
  // Try-On 생성
  tryon: (data) => api.post('/api/v1/outfit/tryon', data),
};

// ========== OOTD API ==========
export const ootdAPI = {
  // OOTD 저장
  save: (data) => api.post('/api/v1/ootd', data),

  // 내 OOTD 목록 조회
  list: () => api.get('/api/v1/ootd'),

  // OOTD 삭제
  delete: (id) => api.delete(`/api/v1/ootd/${id}`),
};

export default api;
