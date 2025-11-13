import axios from 'axios';

const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080';

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
      localStorage.removeItem('accessToken');
      window.location.href = '/login';
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

  // 게시글 조회
  getOne: (boardId, postId) => api.get(`/api/v1/boards/${boardId}/posts/${postId}`),

  // 게시글 작성
  create: (boardId, data) => api.post(`/api/v1/boards/${boardId}/posts`, data),

  // 게시글 수정
  update: (boardId, postId, data) => api.put(`/api/v1/boards/${boardId}/posts/${postId}`, data),

  // 게시글 삭제
  delete: (boardId, postId) => api.delete(`/api/v1/boards/${boardId}/posts/${postId}`),
};

// ========== User API ==========
export const userAPI = {
  // 내 정보 조회
  me: () => api.get('/api/v1/users/me'),
};

export default api;
