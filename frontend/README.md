# ClosetConnect Frontend

React 기반 ClosetConnect 프론트엔드 애플리케이션입니다.

## 기술 스택

- **React** 18
- **Vite** - 빌드 도구
- **React Router** - 라우팅
- **Axios** - HTTP 클라이언트

## 주요 기능

### 1. 인증
- 로그인 / 회원가입
- JWT 토큰 기반 인증
- 자동 토큰 갱신

### 2. 옷장 (Closet)
- 카테고리별 옷 목록 보기
- 옷 추가/삭제
- 이미지 업로드
- 옷 상세 정보 보기

### 3. 커뮤니티
- 보드 목록 보기
- 게시글 목록 보기
- 페이지네이션

### 4. 마이페이지
- 사용자 정보 확인

## 시작하기

### 1. 의존성 설치

```bash
npm install
```

### 2. 환경 변수 설정

`.env` 파일을 생성하고 다음 내용을 추가하세요:

```env
VITE_API_BASE=http://localhost:8080
```

### 3. 개발 서버 실행

```bash
npm run dev
```

브라우저에서 http://localhost:5173 을 열어 확인하세요.

### 4. 프로덕션 빌드

```bash
npm run build
```

빌드된 파일은 `dist/` 디렉토리에 생성됩니다.

## 프로젝트 구조

```
src/
├── components/        # 재사용 가능한 컴포넌트
│   ├── Layout.jsx
│   ├── Sidebar.jsx
│   ├── ClothCard.jsx
│   ├── ClothDetailModal.jsx
│   └── AddClothModal.jsx
├── pages/            # 페이지 컴포넌트
│   ├── LoginPage.jsx
│   ├── SignupPage.jsx
│   ├── ClosetPage.jsx
│   ├── CommunityPage.jsx
│   ├── BoardPage.jsx
│   └── MyPage.jsx
├── services/         # API 서비스
│   └── api.js
├── App.jsx           # 메인 앱 컴포넌트
└── main.jsx          # 엔트리 포인트
```

## API 엔드포인트

백엔드 API는 다음 엔드포인트를 제공합니다:

- `POST /api/v1/auth/login` - 로그인
- `POST /api/v1/auth/signup` - 회원가입
- `GET /api/v1/cloth` - 옷 목록 조회
- `POST /api/v1/cloth` - 옷 등록
- `DELETE /api/v1/cloth/:id` - 옷 삭제
- `POST /api/v1/uploads` - 이미지 업로드
- `GET /api/v1/community/boards` - 보드 목록
- `GET /api/v1/boards/:boardId/posts` - 게시글 목록

## 참고사항

- 백엔드 서버가 실행 중이어야 합니다 (기본: http://localhost:8080)
- 인증이 필요한 페이지는 자동으로 로그인 페이지로 리다이렉트됩니다
- 토큰은 localStorage에 저장됩니다
