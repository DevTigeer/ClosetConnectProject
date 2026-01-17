# Vercel 프론트엔드에 Swagger UI 임베드하기

## 📋 개요

Railway 백엔드의 OpenAPI 스펙을 Vercel 프론트엔드에서 Swagger UI로 표시하는 방법입니다.

---

## 방법 1: iframe으로 임베드 (간단)

### React 컴포넌트 예시

```jsx
// src/pages/ApiDocs.jsx
import React from 'react';

const ApiDocs = () => {
  const RAILWAY_SWAGGER_URL = import.meta.env.VITE_API_BASE_URL + '/swagger-ui/index.html';

  return (
    <div style={{ width: '100%', height: '100vh' }}>
      <iframe
        src={RAILWAY_SWAGGER_URL}
        style={{ width: '100%', height: '100%', border: 'none' }}
        title="API Documentation"
      />
    </div>
  );
};

export default ApiDocs;
```

### 장점
- 구현이 매우 간단
- Railway의 Swagger UI를 그대로 사용

### 단점
- iframe이므로 스타일 커스터마이징 제한
- CORS 이슈 가능성 (Railway에서 X-Frame-Options 설정 필요)

---

## 방법 2: swagger-ui-react 라이브러리 사용 (권장)

### 1. 패키지 설치

```bash
npm install swagger-ui-react
```

### 2. React 컴포넌트 작성

```jsx
// src/pages/ApiDocs.jsx
import React from 'react';
import SwaggerUI from 'swagger-ui-react';
import 'swagger-ui-react/swagger-ui.css';

const ApiDocs = () => {
  // Railway 백엔드의 OpenAPI JSON 경로
  const OPENAPI_URL = import.meta.env.VITE_API_BASE_URL + '/v3/api-docs';

  return (
    <div className="api-docs-container">
      <SwaggerUI
        url={OPENAPI_URL}
        docExpansion="list"
        defaultModelsExpandDepth={-1}
      />
    </div>
  );
};

export default ApiDocs;
```

### 3. 라우팅 설정

```jsx
// src/App.jsx
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import ApiDocs from './pages/ApiDocs';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* 기존 라우트들... */}
        <Route path="/api-docs" element={<ApiDocs />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
```

### 4. 네비게이션에 링크 추가

```jsx
// src/components/Header.jsx
<nav>
  <Link to="/">홈</Link>
  <Link to="/closet">옷장</Link>
  <Link to="/api-docs">API 문서</Link>
</nav>
```

### 장점
- Vercel 프론트엔드 디자인과 통합 가능
- 커스터마이징 자유로움
- CORS 이슈 없음 (OpenAPI JSON만 fetch)

### 단점
- 패키지 설치 필요
- 번들 사이즈 증가 (약 1MB)

---

## 방법 3: 단순 링크 버튼 (가장 간단)

### React 컴포넌트 예시

```jsx
// src/components/ApiDocsButton.jsx
import React from 'react';

const ApiDocsButton = () => {
  const RAILWAY_SWAGGER_URL = import.meta.env.VITE_API_BASE_URL + '/swagger-ui/index.html';

  return (
    <a
      href={RAILWAY_SWAGGER_URL}
      target="_blank"
      rel="noopener noreferrer"
      className="api-docs-button"
    >
      📚 API 문서 보기
    </a>
  );
};

export default ApiDocsButton;
```

### 스타일 예시

```css
.api-docs-button {
  display: inline-block;
  padding: 10px 20px;
  background-color: #4CAF50;
  color: white;
  text-decoration: none;
  border-radius: 5px;
  font-weight: bold;
}

.api-docs-button:hover {
  background-color: #45a049;
}
```

### 장점
- 구현이 가장 간단
- 번들 사이즈 증가 없음
- Railway의 공식 Swagger UI 사용

### 단점
- 별도 탭/창으로 열림
- 프론트엔드와 완전히 분리됨

---

## Railway CORS 설정 (iframe 사용 시 필요)

iframe으로 임베드하려면 Railway 백엔드에서 CORS 및 X-Frame-Options 설정이 필요합니다.

### SecurityConfig.java 수정

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .cors(Customizer.withDefaults())
        .csrf(csrf -> csrf.disable())
        .headers(headers -> headers
            .frameOptions(frameOptions -> frameOptions
                .sameOrigin()  // 또는 .disable() (보안상 권장하지 않음)
            )
        )
        // 기존 설정들...
}
```

### CorsConfigurationSource 수정

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();

    // Vercel 도메인 허용
    config.addAllowedOriginPattern("https://*.vercel.app");

    // 기존 설정들...
    config.addAllowedMethod("*");
    config.addAllowedHeader("*");
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

---

## 환경변수 설정

### Vercel 환경변수

Vercel 프로젝트 → Settings → Environment Variables:

```bash
VITE_API_BASE_URL=https://your-railway-app.up.railway.app
```

### .env 파일 (로컬 개발)

```bash
# frontend/.env
VITE_API_BASE_URL=http://localhost:8080
```

```bash
# frontend/.env.production
VITE_API_BASE_URL=https://your-railway-app.up.railway.app
```

---

## 권장 방법

### 개발자용 API 문서
- **방법 2 (swagger-ui-react)** 사용
- Vercel 프론트엔드의 `/api-docs` 경로에 통합
- 프론트엔드 개발자들이 쉽게 접근

### 일반 사용자용
- API 문서를 노출할 필요 없음
- 필요시 관리자 페이지에만 추가

### 코드 예시 (개발자 전용 페이지)

```jsx
// src/pages/DeveloperDocs.jsx
import React from 'react';
import SwaggerUI from 'swagger-ui-react';
import 'swagger-ui-react/swagger-ui.css';
import { useAuth } from '../hooks/useAuth';

const DeveloperDocs = () => {
  const { user } = useAuth();
  const OPENAPI_URL = import.meta.env.VITE_API_BASE_URL + '/v3/api-docs';

  // 개발자 계정만 접근 가능
  if (!user || user.role !== 'DEVELOPER') {
    return <div>접근 권한이 없습니다.</div>;
  }

  return (
    <div className="container">
      <h1>ClosetConnect API 문서</h1>
      <p>개발자 전용 API 문서입니다.</p>
      <SwaggerUI
        url={OPENAPI_URL}
        docExpansion="list"
        defaultModelsExpandDepth={-1}
        tryItOutEnabled={true}
      />
    </div>
  );
};

export default DeveloperDocs;
```

---

## 요약

| 방법 | 난이도 | 통합도 | 권장도 |
|------|--------|--------|--------|
| **직접 Railway 접속** | ⭐ | ❌ | ⭐⭐⭐ (개발 단계) |
| **iframe 임베드** | ⭐⭐ | ⭐⭐ | ⭐⭐ |
| **swagger-ui-react** | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ (프로덕션) |
| **단순 링크 버튼** | ⭐ | ⭐ | ⭐⭐⭐⭐ (간단) |

---

## 작성일
2026-01-17
