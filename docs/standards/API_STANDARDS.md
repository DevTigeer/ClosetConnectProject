# API 표준 및 규약

> ClosetConnect API 설계 표준 문서 (MCP Reference)

**버전**: v1.0
**최종 수정**: 2025-12-31
**목적**: MCP가 API 질의에 정확하게 답변할 수 있도록 표준화된 API 규약 정의

---

## 📋 목차

1. [API 버저닝 전략](#api-버저닝-전략)
2. [표준 응답 포맷](#표준-응답-포맷)
3. [에러 코드 체계](#에러-코드-체계)
4. [인증 및 인가](#인증-및-인가)
5. [Rate Limiting 정책](#rate-limiting-정책)
6. [Idempotency 설계](#idempotency-설계)
7. [타임아웃 및 재시도](#타임아웃-및-재시도)

---

## API 버저닝 전략

### 현재 버전
- **API Version**: v1
- **Base URL**: `/api/v1`
- **Versioning Method**: URL Path Versioning

### 버전 관리 규칙

```
/api/v1/cloth        # 현재 안정 버전
/api/v2/cloth        # 향후 확장 (Breaking Change 발생 시)
```

#### Breaking Change 기준
- 필드 타입 변경 (String → Integer)
- 필수 필드 추가
- 응답 구조 변경
- 엔드포인트 URL 변경

#### Non-Breaking Change
- 선택적 필드 추가
- 새로운 엔드포인트 추가
- 에러 코드 추가

### 버전별 지원 정책

| 버전 | 상태 | 지원 기간 | Sunset 예정일 |
|-----|------|----------|--------------|
| v1  | Active | 24개월 | TBD |
| v2  | Planned | - | - |

---

## 표준 응답 포맷

### 성공 응답

#### 단일 리소스 조회
```json
{
  "id": 123,
  "name": "화이트 셔츠",
  "category": "UPPER",
  "imageUrl": "https://cdn.closetconnect.com/uploads/123.jpg",
  "createdAt": "2025-12-31T10:30:00Z",
  "updatedAt": "2025-12-31T10:30:00Z"
}
```

**필드 설명**:
- `id`: Long, 리소스 고유 식별자
- `createdAt`, `updatedAt`: ISO 8601 포맷, UTC 기준
- 모든 URL은 절대 경로 (CDN 포함)

#### 리스트 조회 (페이징)
```json
{
  "content": [
    {
      "id": 123,
      "name": "화이트 셔츠",
      "category": "UPPER"
    },
    {
      "id": 124,
      "name": "블랙 팬츠",
      "category": "LOWER"
    }
  ],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  }
}
```

**페이징 파라미터**:
- `page`: 0부터 시작 (Default: 0)
- `size`: 페이지당 항목 수 (Default: 20, Max: 100)
- `sort`: 정렬 기준 (예: `createdAt,desc`)

#### 생성/수정 성공
```json
{
  "id": 123,
  "message": "옷이 성공적으로 등록되었습니다.",
  "status": "PROCESSING"
}
```

**HTTP Status Code**: `201 Created` (생성) 또는 `200 OK` (수정)

### 에러 응답

#### 표준 에러 구조
```json
{
  "timestamp": "2025-12-31T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "INVALID_CATEGORY",
  "message": "유효하지 않은 카테고리입니다: 'INVALID'",
  "path": "/api/v1/cloth/upload",
  "details": {
    "field": "category",
    "rejectedValue": "INVALID",
    "allowedValues": ["UPPER", "LOWER", "OUTER", "DRESS", "SHOES", "ACC"]
  }
}
```

**필드 설명**:
- `timestamp`: 에러 발생 시각 (ISO 8601)
- `status`: HTTP 상태 코드
- `error`: HTTP 상태 메시지
- `code`: 애플리케이션 에러 코드 (대문자 스네이크 케이스)
- `message`: 사용자 친화적 에러 메시지 (한글)
- `path`: 에러가 발생한 엔드포인트
- `details`: 추가 디버깅 정보 (선택적)

---

## 에러 코드 체계

### 코드 네이밍 규칙
```
{DOMAIN}_{ERROR_TYPE}_{DETAIL}

예시:
- CLOTH_NOT_FOUND
- USER_UNAUTHORIZED
- PAYMENT_AMOUNT_MISMATCH
```

### 에러 코드 목록

#### 인증/인가 (AUTH_*)

| 코드 | HTTP Status | 설명 | 재시도 가능 |
|-----|-------------|------|------------|
| `AUTH_INVALID_TOKEN` | 401 | JWT 토큰이 유효하지 않음 | ❌ |
| `AUTH_TOKEN_EXPIRED` | 401 | JWT 토큰 만료 | ✅ (재로그인) |
| `AUTH_UNAUTHORIZED` | 403 | 권한 부족 | ❌ |
| `AUTH_MISSING_TOKEN` | 401 | Authorization 헤더 누락 | ❌ |

#### 리소스 (RESOURCE_*)

| 코드 | HTTP Status | 설명 | 재시도 가능 |
|-----|-------------|------|------------|
| `CLOTH_NOT_FOUND` | 404 | 옷 리소스를 찾을 수 없음 | ❌ |
| `USER_NOT_FOUND` | 404 | 사용자를 찾을 수 없음 | ❌ |
| `POST_NOT_FOUND` | 404 | 게시글을 찾을 수 없음 | ❌ |

#### 검증 (VALIDATION_*)

| 코드 | HTTP Status | 설명 | 재시도 가능 |
|-----|-------------|------|------------|
| `VALIDATION_FAILED` | 400 | 입력 검증 실패 | ❌ |
| `INVALID_CATEGORY` | 400 | 유효하지 않은 카테고리 | ❌ |
| `INVALID_IMAGE_FORMAT` | 400 | 지원하지 않는 이미지 포맷 | ❌ |
| `FILE_TOO_LARGE` | 413 | 파일 크기 초과 (Max: 5MB) | ❌ |

#### 비즈니스 로직 (BUSINESS_*)

| 코드 | HTTP Status | 설명 | 재시도 가능 |
|-----|-------------|------|------------|
| `PROCESSING_IN_PROGRESS` | 409 | 이미 처리 중 | ✅ (대기 후) |
| `ALREADY_CONFIRMED` | 409 | 이미 확정됨 | ❌ |
| `INSUFFICIENT_BALANCE` | 402 | 잔액 부족 | ❌ |

#### 외부 서비스 (EXTERNAL_*)

| 코드 | HTTP Status | 설명 | 재시도 가능 |
|-----|-------------|------|------------|
| `AI_SERVICE_UNAVAILABLE` | 503 | AI 서버 응답 없음 | ✅ (3회) |
| `PAYMENT_GATEWAY_ERROR` | 502 | 결제 게이트웨이 오류 | ✅ (1회) |
| `WEATHER_API_TIMEOUT` | 504 | 날씨 API 타임아웃 | ✅ (2회) |

#### Rate Limiting (RATE_*)

| 코드 | HTTP Status | 설명 | 재시도 가능 |
|-----|-------------|------|------------|
| `RATE_LIMIT_EXCEEDED` | 429 | API 호출 한도 초과 | ✅ (Retry-After) |

#### 서버 에러 (SERVER_*)

| 코드 | HTTP Status | 설명 | 재시도 가능 |
|-----|-------------|------|------------|
| `INTERNAL_SERVER_ERROR` | 500 | 서버 내부 오류 | ✅ (1회) |
| `DATABASE_ERROR` | 500 | 데이터베이스 오류 | ✅ (1회) |

### 에러 응답 예시

#### 1. 인증 실패
```http
HTTP/1.1 401 Unauthorized

{
  "timestamp": "2025-12-31T10:30:00Z",
  "status": 401,
  "error": "Unauthorized",
  "code": "AUTH_INVALID_TOKEN",
  "message": "유효하지 않은 인증 토큰입니다.",
  "path": "/api/v1/cloth/123"
}
```

#### 2. 검증 실패
```http
HTTP/1.1 400 Bad Request

{
  "timestamp": "2025-12-31T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_FAILED",
  "message": "입력 값 검증에 실패했습니다.",
  "path": "/api/v1/cloth/upload",
  "details": {
    "errors": [
      {
        "field": "name",
        "message": "옷 이름은 필수입니다.",
        "rejectedValue": null
      },
      {
        "field": "category",
        "message": "카테고리는 필수입니다.",
        "rejectedValue": null
      }
    ]
  }
}
```

#### 3. Rate Limit 초과
```http
HTTP/1.1 429 Too Many Requests
Retry-After: 60

{
  "timestamp": "2025-12-31T10:30:00Z",
  "status": 429,
  "error": "Too Many Requests",
  "code": "RATE_LIMIT_EXCEEDED",
  "message": "API 호출 한도를 초과했습니다. 60초 후 다시 시도해주세요.",
  "path": "/api/v1/cloth/upload",
  "details": {
    "limit": 10,
    "remaining": 0,
    "resetAt": "2025-12-31T10:31:00Z"
  }
}
```

---

## 인증 및 인가

### 인증 방식

#### JWT Bearer Token
```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**토큰 구조**:
```json
{
  "sub": "user@example.com",
  "role": "ROLE_USER",
  "uid": 123,
  "iat": 1735635000,
  "exp": 1735638600
}
```

**필드 설명**:
- `sub`: 사용자 이메일 (Subject)
- `role`: 사용자 역할 (`ROLE_USER`, `ROLE_ADMIN`)
- `uid`: 사용자 ID
- `iat`: 발급 시각 (Unix Timestamp)
- `exp`: 만료 시각 (Unix Timestamp, 발급 후 1시간)

### 인증 플로우

```mermaid
sequenceDiagram
    Client->>Backend: POST /api/v1/auth/login
    Backend->>Backend: 비밀번호 검증
    Backend->>Client: JWT Token
    Client->>Backend: GET /api/v1/cloth (+ Bearer Token)
    Backend->>Backend: JWT 검증
    Backend->>Client: 리소스 반환
```

### 권한 모델 (RBAC)

#### 역할 정의

| 역할 | 설명 | 권한 |
|-----|------|------|
| `ROLE_USER` | 일반 사용자 | 자신의 리소스 CRUD, 게시글 작성 |
| `ROLE_ADMIN` | 관리자 | 모든 리소스 CRUD, 사용자 관리 |

#### 엔드포인트별 권한

| 엔드포인트 | Method | 필요 권한 | 비고 |
|----------|--------|---------|------|
| `/api/v1/auth/**` | ANY | 없음 | 공개 |
| `/api/v1/cloth` | POST | `ROLE_USER` | 소유자만 |
| `/api/v1/cloth/{id}` | GET | `ROLE_USER` | 소유자만 |
| `/api/v1/cloth/{id}` | PUT | `ROLE_USER` | 소유자만 |
| `/api/v1/cloth/{id}` | DELETE | `ROLE_USER` | 소유자만 |
| `/api/v1/admin/**` | ANY | `ROLE_ADMIN` | 관리자만 |

#### 소유권 검증

```java
// 예시: 소유자만 수정 가능
if (!cloth.getUserId().equals(currentUser.getId())) {
    throw new UnauthorizedException("소유자만 수정할 수 있습니다.");
}
```

---

## Rate Limiting 정책

### 전략

#### 엔드포인트별 제한

| 엔드포인트 | 제한 | Window | 비고 |
|----------|-----|--------|------|
| `/api/v1/auth/login` | 5회 | 1분 | 브루트포스 방지 |
| `/api/v1/auth/signup` | 3회 | 10분 | 스팸 방지 |
| `/api/v1/cloth/upload` | 10회 | 1시간 | AI 비용 제어 |
| `/api/v1/posts` | 100회 | 1분 | 일반 조회 |
| `/api/v1/payments/confirm` | 1회 | 10초 | 중복 결제 방지 |

#### 사용자별 전역 제한
- **일반 사용자**: 1000 req/hour
- **관리자**: 5000 req/hour

### 구현 알고리즘

**Token Bucket Algorithm**:
```
Bucket Size: 100 tokens
Refill Rate: 10 tokens/minute
```

### 응답 헤더

```http
X-RateLimit-Limit: 10
X-RateLimit-Remaining: 7
X-RateLimit-Reset: 1735638600
```

### 초과 시 응답

```http
HTTP/1.1 429 Too Many Requests
Retry-After: 60

{
  "code": "RATE_LIMIT_EXCEEDED",
  "message": "API 호출 한도를 초과했습니다."
}
```

---

## Idempotency 설계

### Idempotency Key

#### 필요한 엔드포인트
- 결제 확정 (`POST /api/v1/payments/confirm`)
- 옷 업로드 (`POST /api/v1/cloth/upload`)

#### 요청 예시

```http
POST /api/v1/payments/confirm
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
Content-Type: application/json

{
  "orderId": "ORDER_123",
  "amount": 50000
}
```

#### 응답 캐싱

**첫 번째 요청**:
```http
HTTP/1.1 200 OK

{
  "paymentId": "PAY_456",
  "status": "COMPLETED"
}
```

**동일 Idempotency-Key로 재요청**:
```http
HTTP/1.1 200 OK
X-Idempotent-Replayed: true

{
  "paymentId": "PAY_456",
  "status": "COMPLETED"
}
```

### 구현 규칙

1. Idempotency-Key는 UUID v4 사용
2. 동일 키로 24시간 내 재요청 시 캐시된 응답 반환
3. 요청 Body가 다르면 `409 Conflict` 반환

---

## 타임아웃 및 재시도

### 타임아웃 정책

#### Client → Backend

| 엔드포인트 타입 | Read Timeout | Connect Timeout |
|--------------|--------------|-----------------|
| 일반 API | 30초 | 5초 |
| AI 처리 시작 | 10초 | 5초 |
| 파일 업로드 | 60초 | 10초 |

#### Backend → External Services

| 서비스 | Read Timeout | Connect Timeout | 비고 |
|-------|--------------|-----------------|------|
| AI Server | 120초 | 10초 | 장시간 처리 |
| Toss Payments | 30초 | 5초 | |
| Weather API | 5초 | 3초 | 빠른 응답 |
| RabbitMQ | 10초 | 5초 | |

### 재시도 정책

#### 재시도 대상

| 시나리오 | 재시도 횟수 | Backoff | 비고 |
|---------|-----------|---------|------|
| AI 서버 일시 장애 | 3회 | Exponential (1s, 2s, 4s) | |
| 결제 Gateway 오류 | 1회 | Fixed (5s) | 멱등성 보장 |
| 날씨 API 타임아웃 | 2회 | Linear (3s, 3s) | |

#### 재시도 제외 대상

- 4xx 에러 (클라이언트 오류)
- 인증 실패
- 검증 실패
- 비즈니스 로직 오류

### Circuit Breaker

```
Failure Threshold: 50% (10초 윈도우)
Open Duration: 60초
Half-Open Test Requests: 3회
```

---

## API 호출 예시

### 1. 옷 업로드

**Request**:
```http
POST /api/v1/cloth/upload HTTP/1.1
Host: api.closetconnect.com
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary

------WebKitFormBoundary
Content-Disposition: form-data; name="image"; filename="shirt.jpg"
Content-Type: image/jpeg

[Binary Data]
------WebKitFormBoundary
Content-Disposition: form-data; name="name"

화이트 셔츠
------WebKitFormBoundary
Content-Disposition: form-data; name="category"

UPPER
------WebKitFormBoundary
Content-Disposition: form-data; name="imageType"

SINGLE_ITEM
------WebKitFormBoundary--
```

**Response**:
```http
HTTP/1.1 201 Created
Content-Type: application/json

{
  "id": 123,
  "userId": 456,
  "name": "화이트 셔츠",
  "category": "UPPER",
  "status": "PROCESSING",
  "imageType": "SINGLE_ITEM",
  "createdAt": "2025-12-31T10:30:00Z"
}
```

### 2. 옷 목록 조회 (페이징)

**Request**:
```http
GET /api/v1/cloth?page=0&size=20&sort=createdAt,desc&category=UPPER HTTP/1.1
Host: api.closetconnect.com
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response**:
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "content": [
    {
      "id": 123,
      "name": "화이트 셔츠",
      "category": "UPPER",
      "imageUrl": "https://cdn.closetconnect.com/uploads/123.jpg",
      "status": "COMPLETED"
    }
  ],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  }
}
```

### 3. 결제 확정 (Idempotency)

**Request**:
```http
POST /api/v1/payments/confirm HTTP/1.1
Host: api.closetconnect.com
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
Content-Type: application/json

{
  "orderId": "ORDER_123",
  "paymentKey": "toss_payment_key_123",
  "amount": 50000
}
```

**Response**:
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "paymentId": "PAY_456",
  "orderId": "ORDER_123",
  "status": "COMPLETED",
  "amount": 50000,
  "approvedAt": "2025-12-31T10:30:00Z"
}
```

---

## MCP 질의 예시

### Q1: "결제 API 에러 처리 방법은?"

**A**: `PAYMENT_GATEWAY_ERROR` (502)가 발생하면:
1. 1회 재시도 (5초 후)
2. 여전히 실패 시 사용자에게 에러 반환
3. Idempotency-Key로 중복 방지

### Q2: "AI 업로드 API Rate Limit은?"

**A**: `/api/v1/cloth/upload`는 시간당 10회 제한. 초과 시 `429 Too Many Requests` 반환, `Retry-After` 헤더 참고.

### Q3: "인증 토큰 만료 시 처리는?"

**A**: `AUTH_TOKEN_EXPIRED` (401) 반환. 클라이언트는 `/api/v1/auth/login`으로 재로그인 후 새 토큰 획득.

---

**작성일**: 2025-12-31
**관리자**: Backend Team
**문서 상태**: Draft → **Under Review** → Approved
