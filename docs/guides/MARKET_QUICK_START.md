# Market 기능 빠른 시작 가이드 🚀

## 1분만에 Market 기능 테스트하기!

### Step 1: 서버 실행
```bash
cd /Users/grail/Documents/ClosetConnectProject
./gradlew bootRun
```

서버가 `http://localhost:8080`에서 실행됩니다.

---

### Step 2: 브라우저에서 테스트

#### 📋 준비사항
- 먼저 로그인해서 JWT 토큰을 받아야 합니다
- 토큰을 브라우저 콘솔에서 localStorage에 저장:

```javascript
// 브라우저 개발자 도구 (F12) → Console에서 실행
localStorage.setItem('jwt_token', 'YOUR_JWT_TOKEN_HERE');
```

**JWT 토큰 받는 방법:**
```bash
# 로그인 API 호출
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com", "password":"password123"}'

# 응답에서 token 값을 복사
```

---

### Step 3: 페이지별 테스트

#### 🛍️ 1. 상품 목록 보기
```
http://localhost:8080/market-demo.html
```
- 등록된 상품 목록 확인
- 필터/검색/정렬 테스트
- 상품 카드 클릭 → 상세 페이지

---

#### ➕ 2. 상품 등록하기
```
http://localhost:8080/market-create.html
```

**테스트 순서:**
1. 로그인 상태 확인 (토큰 있어야 함)
2. 옷장 아이템 목록 자동 로드
3. 아이템 1개 선택 (파란색 테두리)
4. 폼 입력:
   - 상품명: "테스트 상품"
   - 가격: 10000
   - 설명: "테스트용 상품입니다"
   - 상품 상태: "상 (거의 새것)"
   - 지역: "서울"
5. "상품 등록" 버튼 클릭
6. 성공하면 상세 페이지로 자동 이동

**⚠️ 주의:** 옷장에 아이템이 없으면 먼저 아이템을 등록해야 합니다!

---

#### 📝 3. 상품 상세 보기 + 댓글 + 찜
```
http://localhost:8080/market-detail.html?id=1
```

**테스트 순서:**
1. 상품 정보 확인
2. 오른쪽 상단 ❤️ 버튼 클릭 → 찜하기
3. 다시 클릭 → 찜 취소
4. 하단 댓글창에 "안녕하세요" 입력 → "댓글 작성" 클릭
5. 댓글 목록에 내 댓글 표시
6. 본인 댓글은 "삭제" 버튼 표시

---

#### 💬 4. 채팅하기 (핵심 기능!)
```
http://localhost:8080/market-detail.html?id=1
```

**테스트 순서:**
1. 상품 상세 페이지에서 "💬 채팅하기" 버튼 클릭
2. 채팅방 생성 성공 메시지
3. 자동으로 채팅 페이지로 이동
4. 왼쪽에 채팅방 목록
5. 오른쪽에 채팅 메시지 입력창
6. 메시지 입력 후 "전송" 클릭
7. 실시간으로 메시지 표시

**⚠️ 주의:**
- 본인 상품에는 채팅 버튼이 표시되지 않습니다
- 다른 사용자 계정으로 테스트해야 합니다

---

#### 📦 5. 내 판매 상품 관리
```
http://localhost:8080/market-my-products.html
```

**테스트 순서:**
1. 내가 등록한 상품 목록 확인
2. "예약중으로 변경" 버튼 클릭
3. 확인 메시지 → "확인" 클릭
4. 페이지 새로고침 → 상태 변경 확인
5. 채팅방에 자동으로 시스템 메시지 전송됨!
6. "삭제" 버튼 → 상품 영구 삭제

---

#### ❤️ 6. 찜한 상품 목록
```
http://localhost:8080/market-liked.html
```

**테스트 순서:**
1. 여러 상품에 찜하기
2. 찜한 상품 페이지 접속
3. 찜한 상품 목록 확인
4. 상품 클릭 → 상세 페이지로 이동

---

### Step 4: API 직접 테스트 (선택)

#### 상품 목록 조회
```bash
curl http://localhost:8080/api/v1/market/products
```

#### 상품 상세 조회
```bash
curl http://localhost:8080/api/v1/market/products/1
```

#### 상품 등록 (토큰 필요)
```bash
curl -X POST http://localhost:8080/api/v1/market/products \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clothId": 1,
    "title": "API 테스트 상품",
    "price": 15000,
    "description": "API로 등록한 상품입니다",
    "productCondition": "GOOD",
    "region": "서울"
  }'
```

#### 찜 추가
```bash
curl -X POST http://localhost:8080/api/v1/market/products/1/like \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### 댓글 작성
```bash
curl -X POST http://localhost:8080/api/v1/market/products/1/comments \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"content": "API 테스트 댓글입니다"}'
```

#### 채팅방 생성
```bash
curl -X POST http://localhost:8080/api/v1/market/chat/rooms \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productId": 1}'
```

---

## 🎯 실전 시나리오: 완전한 거래 흐름

### 시나리오: A가 상품을 등록하고 B가 구매

#### [판매자 A - Chrome 브라우저]
```
1. http://localhost:8080/api/v1/auth/login → A 계정 로그인
2. 토큰을 localStorage에 저장
3. http://localhost:8080/market-create.html → 상품 등록
4. "나이키 반팔티" 등록 (가격: 25000원)
```

#### [구매자 B - Firefox 브라우저 (또는 시크릿 모드)]
```
5. http://localhost:8080/api/v1/auth/login → B 계정 로그인
6. 토큰을 localStorage에 저장
7. http://localhost:8080/market-demo.html → 상품 목록 조회
8. "나이키" 검색 → A의 상품 클릭
9. ❤️ 찜하기 클릭
10. "사이즈가 어떻게 되나요?" 댓글 작성
11. "💬 채팅하기" 버튼 클릭
12. 채팅방에서 "구매하고 싶습니다" 메시지 전송
```

#### [판매자 A]
```
13. http://localhost:8080/market-detail.html?id=1 → 댓글 확인
14. "M 사이즈입니다" 댓글 답변 (판매자 태그 표시됨)
15. http://localhost:8080/market-chat.html → 채팅 확인
16. "네, 거래 가능합니다" 응답
17. http://localhost:8080/market-my-products.html → 내 상품 관리
18. "예약중으로 변경" 버튼 클릭
```

#### [구매자 B]
```
19. 채팅방에서 시스템 메시지 수신:
    "판매자가 상품 상태를 '예약중'로 변경했습니다."
20. "감사합니다. 내일 거래할게요" 메시지 전송
```

#### [판매자 A - 거래 완료 후]
```
21. http://localhost:8080/market-my-products.html
22. "거래완료로 변경" 버튼 클릭
23. 채팅방에 자동으로 시스템 메시지 전송
```

---

## 🐛 자주 발생하는 문제 해결

### 1. "로그인이 필요합니다" 에러
**원인:** JWT 토큰이 없음

**해결:**
```javascript
// 브라우저 콘솔에서
localStorage.setItem('jwt_token', 'YOUR_TOKEN');
```

---

### 2. "옷장 아이템이 없습니다"
**원인:** Cloth 테이블에 데이터 없음

**해결:** 먼저 Cloth API로 아이템 등록
```bash
curl -X POST http://localhost:8080/api/v1/cloth \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "테스트 옷",
    "category": "TOP",
    "imageUrl": "http://example.com/image.jpg"
  }'
```

---

### 3. WebSocket 연결 실패
**원인:** CORS 또는 서버 미실행

**확인사항:**
- 서버가 실행 중인지 확인: `./gradlew bootRun`
- 브라우저 콘솔에서 에러 메시지 확인
- WebSocket 엔드포인트: `ws://localhost:8080/ws`

---

### 4. 채팅 메시지가 안 보임
**원인:** WebSocket 구독 안됨

**해결:**
1. 페이지 새로고침
2. 브라우저 콘솔에서 "WebSocket 연결 성공" 확인
3. 네트워크 탭에서 WS 연결 확인

---

## 📊 데이터베이스 확인

### MariaDB 접속
```bash
mysql -u root -p
# 비밀번호: root1234

USE closetConnectProject;
```

### 테이블 확인
```sql
-- 상품 목록
SELECT * FROM market_product;

-- 댓글 목록
SELECT * FROM market_product_comment;

-- 찜 목록
SELECT * FROM market_product_like;

-- 채팅방 목록
SELECT * FROM chat_room;

-- 채팅 메시지 목록
SELECT * FROM chat_message;
```

---

## 🎉 축하합니다!

Market 기능을 모두 테스트했습니다!

### 구현된 기능 요약:
✅ 상품 CRUD (등록/조회/수정/삭제)
✅ 상품 검색/필터링/정렬
✅ 댓글 시스템 (작성/삭제, 판매자 태그)
✅ 찜/좋아요 기능
✅ 실시간 1:1 채팅 (WebSocket/STOMP)
✅ 시스템 메시지 (거래 상태 변경 알림)
✅ 반응형 HTML UI (당근마켓 스타일)

### 다음 단계:
- React로 프론트엔드 마이그레이션
- 이미지 업로드 기능 추가
- 판매자 프로필/통계
- 후기/평점 시스템
- 푸시 알림

---

**문의사항이 있으면 MARKET_USAGE_GUIDE.md를 참고하세요!**
