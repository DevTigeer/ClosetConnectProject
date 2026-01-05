# ClosetConnect Market (중고거래) 사용 가이드

## 📚 목차
1. [프로젝트 실행 방법](#1-프로젝트-실행-방법)
2. [기본 사용 흐름](#2-기본-사용-흐름)
3. [HTML 페이지 가이드](#3-html-페이지-가이드)
4. [API 엔드포인트](#4-api-엔드포인트)
5. [채팅 기능 사용법](#5-채팅-기능-사용법)
6. [테스트 시나리오](#6-테스트-시나리오)

---

## 1. 프로젝트 실행 방법

### 1-1. 의존성 설치
```bash
# WebSocket 의존성이 자동으로 추가됩니다
./gradlew clean build
```

### 1-2. 데이터베이스 확인
MariaDB가 실행 중이고 다음 설정이 되어있는지 확인:
- 데이터베이스: `closetConnectProject`
- 사용자: `root`
- 비밀번호: `root1234`
- 포트: `3306`

### 1-3. 애플리케이션 실행
```bash
./gradlew bootRun
```

서버가 `http://localhost:8080`에서 실행됩니다.

---

## 2. 기본 사용 흐름

### 📌 전체 흐름
```
1. 회원가입/로그인
   ↓
2. 옷장에 아이템 등록 (Cloth)
   ↓
3. 옷장 아이템을 선택해서 판매 상품 등록
   ↓
4. 다른 사용자가 상품 조회
   ↓
5. 상품에 찜하기 or 댓글 작성
   ↓
6. 채팅하기 버튼으로 판매자와 1:1 채팅 시작
   ↓
7. 거래 협의 후 상태 변경 (판매중 → 예약중 → 거래완료)
```

---

## 3. HTML 페이지 가이드

### 3-1. 상품 목록 페이지
**URL:** `http://localhost:8080/market-demo.html`

**기능:**
- 전체 상품 목록 조회
- 필터링: 상태(판매중/예약중/거래완료), 지역, 키워드
- 정렬: 최신순, 낮은가격순, 높은가격순
- 상품 카드 클릭 → 상세 페이지 이동

**사용법:**
```
1. 브라우저에서 http://localhost:8080/market-demo.html 접속
2. 필터를 선택하고 "검색" 버튼 클릭
3. 상품 카드를 클릭하면 상세 페이지로 이동
```

---

### 3-2. 상품 등록 페이지
**URL:** `http://localhost:8080/market-create.html`

**기능:**
- 옷장 아이템 선택
- 상품 정보 입력 (제목, 가격, 설명, 상태, 지역, 브랜드, 사이즈, 성별)
- 상품 등록

**사용법:**
```
1. 로그인 필요 (localStorage에 jwt_token 저장되어 있어야 함)
2. 페이지 로드 시 자동으로 내 옷장 아이템 목록 표시
3. 판매할 아이템을 클릭하여 선택 (선택되면 파란색 테두리)
4. 아래 폼에 상품 정보 입력
   - 필수: 상품명, 가격, 설명, 상품 상태
   - 선택: 거래 지역, 브랜드, 사이즈, 성별
5. "상품 등록" 버튼 클릭
6. 등록 성공 시 자동으로 상품 상세 페이지로 이동
```

**주의사항:**
- 옷장에 아이템이 없으면 먼저 옷장에 아이템을 추가해야 합니다
- 로그인하지 않으면 "로그인이 필요합니다" 메시지 표시

---

### 3-3. 상품 상세 페이지
**URL:** `http://localhost:8080/market-detail.html?id={상품ID}`

**기능:**
- 상품 정보 표시 (이미지, 제목, 가격, 설명, 판매자, 상태 등)
- 찜하기/찜 취소 (로그인 시)
- 댓글 작성/삭제
- 채팅하기 버튼 (구현 예정)

**사용법:**
```
1. 상품 목록에서 카드 클릭 or 직접 URL 접속
2. 상품 정보 확인
3. 로그인한 경우:
   - 오른쪽 상단의 ❤️ 버튼으로 찜하기/취소
   - 하단 댓글창에 문의 작성 가능
   - 내가 작성한 댓글은 삭제 버튼 표시
4. 판매자 댓글은 "(판매자)" 태그 표시
```

---

### 3-4. 내 판매 상품 관리 페이지
**URL:** `http://localhost:8080/market-my-products.html`

**기능:**
- 내가 등록한 판매 상품 목록 조회
- 상품 상태 변경 (판매중 ↔ 예약중 ↔ 거래완료)
- 상품 삭제

**사용법:**
```
1. 로그인 필요 (JWT 토큰에서 userId 자동 추출)
2. 내가 등록한 상품 목록 자동 로드
3. 각 상품마다 액션 버튼 표시:
   - "상세보기": 상품 상세 페이지로 이동
   - "예약중으로 변경": 상태를 예약중으로 변경
   - "거래완료로 변경": 상태를 거래완료로 변경
   - "재판매하기": 거래완료 상품을 다시 판매중으로
   - "삭제": 상품 영구 삭제 (확인 메시지 표시)
4. 상태 변경 시 채팅방에 자동으로 시스템 메시지 전송
```

---

### 3-5. 찜한 상품 페이지
**URL:** `http://localhost:8080/market-liked.html`

**기능:**
- 내가 찜한 상품 목록 조회
- 상품 카드 클릭 → 상세 페이지

**사용법:**
```
1. 로그인 필요
2. 찜한 상품 목록 자동 로드
3. 상품 카드 클릭하면 상세 페이지로 이동
```

---

### 3-6. 채팅 페이지
**URL:** `http://localhost:8080/market-chat.html`

**기능:**
- 채팅방 목록 조회 (읽지 않은 메시지 개수 표시)
- 실시간 1:1 채팅 (WebSocket/STOMP)
- 시스템 메시지 수신 (거래 상태 변경 알림)

**사용법:**
```
1. 로그인 필요
2. 페이지 로드 시 자동으로 채팅방 목록 로드
3. 왼쪽 목록에서 채팅방 클릭
4. 오른쪽에 채팅 내역 표시
5. 하단 입력창에 메시지 입력 후 "전송" 버튼 또는 Enter
6. 실시간으로 상대방 메시지 수신
7. 시스템 메시지는 노란색 배경으로 표시
```

---

## 4. API 엔드포인트

### 4-1. 상품 API

#### 상품 목록 조회 (공개)
```http
GET /api/v1/market/products?status={상태}&region={지역}&keyword={검색어}&page={페이지}&size={크기}&sort={정렬}
```

**Query Parameters:**
- `status` (optional): `ON_SALE`, `RESERVED`, `SOLD`
- `region` (optional): 지역명 (부분 검색)
- `keyword` (optional): 검색 키워드 (제목, 설명에서 검색)
- `page` (default: 0): 페이지 번호
- `size` (default: 20): 페이지 크기
- `sort` (default: LATEST): `LATEST`, `PRICE_LOW`, `PRICE_HIGH`

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "title": "나이키 반팔티",
      "price": 25000,
      "status": "ON_SALE",
      "thumbnailUrl": "http://...",
      "region": "서울 강남구",
      "likeCount": 5,
      "viewCount": 120,
      "createdAt": "2025-11-16T10:00:00"
    }
  ],
  "totalElements": 100,
  "totalPages": 5
}
```

---

#### 상품 상세 조회 (공개)
```http
GET /api/v1/market/products/{id}
```

**Response:**
```json
{
  "id": 1,
  "title": "나이키 반팔티",
  "price": 25000,
  "description": "거의 새것입니다",
  "status": "ON_SALE",
  "productCondition": "EXCELLENT",
  "region": "서울 강남구",
  "brand": "나이키",
  "size": "M",
  "gender": "남성",
  "viewCount": 121,
  "likeCount": 5,
  "liked": true,
  "seller": {
    "userId": 1,
    "nickname": "판매자닉네임"
  },
  "clothId": 10,
  "clothName": "반팔티",
  "clothCategory": "TOP",
  "images": [
    {
      "id": 1,
      "imageUrl": "http://...",
      "orderIndex": 0
    }
  ],
  "createdAt": "2025-11-16T10:00:00",
  "updatedAt": "2025-11-16T10:00:00"
}
```

---

#### 상품 등록 (인증 필요)
```http
POST /api/v1/market/products
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "clothId": 10,
  "title": "나이키 반팔티 판매",
  "price": 25000,
  "description": "거의 새것입니다",
  "productCondition": "EXCELLENT",
  "region": "서울 강남구",
  "brand": "나이키",
  "size": "M",
  "gender": "남성",
  "additionalImageUrls": []
}
```

---

#### 상품 수정 (인증 필요)
```http
PATCH /api/v1/market/products/{id}
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "title": "수정된 제목",
  "price": 20000,
  "description": "수정된 설명",
  "productCondition": "GOOD",
  "region": "서울 강남구",
  "brand": "나이키",
  "size": "M",
  "gender": "남성"
}
```

---

#### 상품 상태 변경 (인증 필요)
```http
PATCH /api/v1/market/products/{id}/status
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "status": "RESERVED"
}
```

---

#### 상품 삭제 (인증 필요)
```http
DELETE /api/v1/market/products/{id}
Authorization: Bearer {JWT_TOKEN}
```

---

#### 판매자별 상품 목록 조회
```http
GET /api/v1/market/products/seller/{sellerId}?page=0&size=20
```

---

### 4-2. 댓글 API

#### 댓글 목록 조회 (공개)
```http
GET /api/v1/market/products/{productId}/comments?page=0&size=20
```

#### 댓글 작성 (인증 필요)
```http
POST /api/v1/market/products/{productId}/comments
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "content": "사이즈가 어떻게 되나요?",
  "parentId": null
}
```

#### 댓글 수정 (인증 필요)
```http
PATCH /api/v1/market/products/{productId}/comments/{commentId}
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "content": "수정된 댓글 내용"
}
```

#### 댓글 삭제 (인증 필요)
```http
DELETE /api/v1/market/products/{productId}/comments/{commentId}
Authorization: Bearer {JWT_TOKEN}
```

---

### 4-3. 찜/좋아요 API

#### 찜 추가 (인증 필요)
```http
POST /api/v1/market/products/{productId}/like
Authorization: Bearer {JWT_TOKEN}
```

**Response:**
```json
{
  "liked": true,
  "likeCount": 6
}
```

#### 찜 취소 (인증 필요)
```http
DELETE /api/v1/market/products/{productId}/like
Authorization: Bearer {JWT_TOKEN}
```

#### 찜 토글 (인증 필요)
```http
PUT /api/v1/market/products/{productId}/like
Authorization: Bearer {JWT_TOKEN}
```

#### 내가 찜한 상품 목록 (인증 필요)
```http
GET /api/v1/market/liked?page=0&size=20
Authorization: Bearer {JWT_TOKEN}
```

---

### 4-4. 채팅 API

#### 채팅방 생성/조회 (인증 필요)
```http
POST /api/v1/market/chat/rooms
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "productId": 1
}
```

**Response:**
```json
{
  "roomId": 1,
  "productId": 1,
  "productTitle": "나이키 반팔티",
  "productThumbnail": "http://...",
  "otherUserNickname": "판매자닉네임",
  "otherUserId": 2,
  "lastMessage": "안녕하세요",
  "lastMessageAt": "2025-11-16T10:00:00",
  "unreadCount": 0
}
```

---

#### 내 채팅방 목록 조회 (인증 필요)
```http
GET /api/v1/market/chat/rooms
Authorization: Bearer {JWT_TOKEN}
```

---

#### 채팅방 메시지 목록 조회 (인증 필요)
```http
GET /api/v1/market/chat/rooms/{roomId}/messages
Authorization: Bearer {JWT_TOKEN}
```

---

#### 메시지 읽음 처리 (인증 필요)
```http
POST /api/v1/market/chat/rooms/{roomId}/read
Authorization: Bearer {JWT_TOKEN}
```

---

### 4-5. WebSocket 채팅

#### 연결
```javascript
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({ 'Authorization': 'Bearer ' + token }, function() {
  console.log('연결 성공');
});
```

#### 메시지 전송
```javascript
stompClient.send('/app/chat.send', {}, JSON.stringify({
  roomId: 1,
  messageType: 'TEXT',
  content: '안녕하세요'
}));
```

#### 메시지 수신
```javascript
stompClient.subscribe('/queue/chat/' + roomId, function(message) {
  const msg = JSON.parse(message.body);
  console.log('수신:', msg);
});
```

---

## 5. 채팅 기능 사용법

### 5-1. 채팅 시작하기 (구매자)
```
1. 상품 상세 페이지에서 "채팅하기" 버튼 클릭 (추후 구현 예정)
   또는 직접 API 호출:

   POST /api/v1/market/chat/rooms
   { "productId": 1 }

2. 채팅방이 생성되거나 기존 채팅방이 반환됨
3. http://localhost:8080/market-chat.html 접속
4. 왼쪽 목록에서 채팅방 선택
5. 메시지 입력 후 전송
```

### 5-2. 채팅 확인하기 (판매자)
```
1. http://localhost:8080/market-chat.html 접속
2. 왼쪽 목록에 구매자들과의 채팅방 표시
3. 읽지 않은 메시지는 빨간색 뱃지로 개수 표시
4. 채팅방 클릭하여 대화
```

### 5-3. 시스템 메시지
```
판매자가 상품 상태를 변경하면 자동으로 시스템 메시지 전송:

- "판매자가 상품 상태를 '예약중'로 변경했습니다."
- "판매자가 상품 상태를 '거래완료'로 변경했습니다."

시스템 메시지는 노란색 배경으로 표시됨
```

---

## 6. 테스트 시나리오

### 시나리오 1: 상품 등록부터 판매까지
```
[판매자 A]
1. 로그인
2. 옷장에 "나이키 반팔티" 아이템 등록
3. http://localhost:8080/market-create.html 접속
4. "나이키 반팔티" 선택 후 판매 정보 입력
5. 상품 등록 완료

[구매자 B]
6. http://localhost:8080/market-demo.html 접속
7. "나이키" 검색
8. 상품 클릭하여 상세 페이지 진입
9. ❤️ 버튼으로 찜하기
10. 댓글로 "사이즈가 어떻게 되나요?" 문의

[판매자 A]
11. 상품 상세 페이지에서 댓글 확인
12. "M 사이즈입니다" 답변

[구매자 B]
13. POST /api/v1/market/chat/rooms { "productId": 1 } 호출
14. http://localhost:8080/market-chat.html 접속
15. 채팅방에서 "구매하고 싶습니다" 메시지 전송

[판매자 A]
16. http://localhost:8080/market-chat.html 접속
17. 구매자 B와 채팅 확인
18. "네, 거래 가능합니다" 응답
19. http://localhost:8080/market-my-products.html 접속
20. 상품 상태를 "예약중"으로 변경
21. 채팅방에 시스템 메시지 자동 전송

[구매자 B]
22. 채팅에서 시스템 메시지 확인
23. 거래 완료 후 판매자에게 알림

[판매자 A]
24. 상품 상태를 "거래완료"로 변경
```

---

### 시나리오 2: 찜한 상품 관리
```
[사용자]
1. 로그인
2. http://localhost:8080/market-demo.html 접속
3. 여러 상품에 ❤️ 찜하기
4. http://localhost:8080/market-liked.html 접속
5. 찜한 상품 목록 확인
6. 관심 없는 상품은 상세 페이지에서 찜 취소
```

---

## 7. 주요 로컬스토리지 데이터

### JWT 토큰 저장 (로그인 시)
```javascript
// 로그인 후 토큰 저장
localStorage.setItem('jwt_token', 'eyJhbGciOiJIUzI1NiIsInR5cCI6...');

// 토큰에서 userId 추출
function getUserIdFromToken() {
  const token = localStorage.getItem('jwt_token');
  const payload = JSON.parse(atob(token.split('.')[1]));
  return payload.userId;
}
```

---

## 8. 트러블슈팅

### 문제 1: "로그인이 필요합니다" 메시지
**해결:** localStorage에 `jwt_token`을 저장해야 합니다.
```javascript
localStorage.setItem('jwt_token', 'YOUR_JWT_TOKEN');
```

### 문제 2: 옷장 아이템이 없음
**해결:** 먼저 Cloth API로 옷장 아이템을 등록해야 합니다.

### 문제 3: WebSocket 연결 실패
**해결:**
1. 서버가 실행 중인지 확인
2. CORS 설정 확인
3. 브라우저 콘솔에서 에러 메시지 확인

### 문제 4: 채팅 메시지가 전송되지 않음
**해결:**
1. WebSocket 연결 상태 확인 (`stompClient.connected`)
2. JWT 토큰이 유효한지 확인
3. 채팅방 권한 확인 (참여자만 메시지 전송 가능)

---

## 9. 다음 단계 개발 계획 (3단계)

1. **상품 상세 페이지에 "채팅하기" 버튼 추가**
2. **판매자 프로필 페이지** - 거래 내역, 판매 상품 수, 소개
3. **후기/평점 시스템** - 거래 완료 후 후기 작성
4. **알림 기능** - 댓글, 찜, 채팅 메시지 알림
5. **이미지 업로드** - 채팅에서 이미지 전송
6. **React 프론트엔드** - 완전한 SPA 구현

---

## 10. 참고 자료

- Spring WebSocket 문서: https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#websocket
- STOMP 프로토콜: https://stomp.github.io/
- SockJS: https://github.com/sockjs/sockjs-client

---

**개발 완료일:** 2025-11-16
**버전:** 2.0 (1단계: CRUD + 댓글 + 찜, 2단계: 실시간 채팅)
