# 토스 페이먼츠 결제 연동 완성 가이드 🎉

## 개요
ClosetConnect의 중고거래 마켓에 토스 페이먼츠 결제 기능이 완전히 연동되었습니다!

---

## 📋 완성된 기능

### 1. 결제 흐름
```
상품 상세 페이지 → 구매하기 버튼 클릭
    ↓
주문 생성 (POST /api/v1/market/orders)
    ↓
토스 결제창 팝업 → 사용자 결제 진행
    ↓
결제 성공 → payment-success.html로 리다이렉트
    ↓
결제 승인 API 호출 (POST /api/v1/market/payments/confirm)
    ↓
주문 상태 업데이트 (PENDING → PAID)
    ↓
주문 상세 페이지로 이동
```

### 2. 완성된 페이지
- ✅ **market-detail.html**: 구매하기 버튼 추가 (판매중 상품만 표시)
- ✅ **payment-success.html**: 결제 승인 및 주문 정보 표시
- ✅ **payment-fail.html**: 결제 실패 처리
- ✅ **market-my-orders.html**: 내 구매/판매 주문 관리
- ✅ **market-order-detail.html**: 주문 상세 정보 및 배송 추적

### 3. 주요 API 엔드포인트
| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | /api/v1/market/orders | 주문 생성 (결제 전) |
| POST | /api/v1/market/payments/confirm | 결제 승인 |
| GET | /api/v1/market/orders/{id} | 주문 상세 조회 |
| GET | /api/v1/market/orders/buyer | 내 구매 목록 |
| GET | /api/v1/market/orders/seller | 내 판매 목록 |
| POST | /api/v1/market/orders/{id}/ship | 발송 처리 |
| POST | /api/v1/market/orders/{id}/confirm | 구매 확정 |
| DELETE | /api/v1/market/orders/{id} | 주문 취소 |
| DELETE | /api/v1/market/payments/{id} | 결제 취소 (환불) |

---

## 🚀 빠른 시작

### Step 1: 토스 페이먼츠 API 키 발급

#### 1-1. 테스트 키 사용 (개발 환경)
개발 단계에서는 토스 제공 테스트 키를 사용할 수 있습니다:

**application.properties 설정:**
```properties
# 토스 테스트 클라이언트 키
toss.payments.client-key=test_ck_Ba5PzR0ArnnqlZJ276kkrvmYnNeD

# 토스 테스트 시크릿 키
toss.payments.secret-key=test_sk_QbgMGZzorzwDBogO60x28l5E1em4
```

> ⚠️ **주의**: 테스트 키는 실제 결제가 발생하지 않습니다. 테스트용으로만 사용하세요!

#### 1-2. 실제 키 발급 (프로덕션 환경)
1. [토스 페이먼츠 개발자 센터](https://developers.tosspayments.com/) 접속
2. 회원가입 및 로그인
3. "내 앱" → "새 앱 만들기"
4. 상점 정보 입력
5. API 키 페이지에서 클라이언트 키와 시크릿 키 복사
6. application.properties에 키 입력:

```properties
toss.payments.client-key=live_ck_YOUR_KEY_HERE
toss.payments.secret-key=live_sk_YOUR_KEY_HERE
```

---

### Step 2: 프론트엔드 클라이언트 키 설정

**market-detail.html 수정 (316번째 줄):**
```javascript
const TOSS_CLIENT_KEY = 'test_ck_D5GePWvyJnrK0W0k6q8gLzN97Eoq'; // 여기를 수정
```

> 💡 **팁**: 프로덕션 환경에서는 이 값을 환경 변수나 백엔드 API에서 가져오는 것이 좋습니다!

---

### Step 3: 서버 실행 및 테스트

#### 3-1. 서버 실행
```bash
cd /Users/grail/Documents/ClosetConnectProject
./gradlew bootRun
```

#### 3-2. 로그인 (JWT 토큰 발급)
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com", "password":"password123"}'
```

브라우저 콘솔에서:
```javascript
localStorage.setItem('jwt_token', 'YOUR_JWT_TOKEN_HERE');
```

#### 3-3. 테스트 시나리오: 상품 구매

**1. 상품 등록 (판매자 A)**
```
http://localhost:8080/market-create.html
- 옷장 아이템 선택
- 상품 정보 입력 (가격: 25000원)
- 상품 등록
```

**2. 상품 구매 (구매자 B - 다른 계정)**
```
http://localhost:8080/market-demo.html
- 상품 클릭
- "구매하기" 버튼 클릭
- 토스 결제창에서 테스트 결제 진행
```

**3. 토스 테스트 카드 정보**
```
카드번호: 1111-2222-3333-4444
유효기간: 12/25
CVC: 123
비밀번호 앞 2자리: 00
```

**4. 결제 성공 확인**
```
- payment-success.html로 자동 이동
- 결제 승인 완료 메시지
- "주문 상세" 버튼 클릭 → 주문 정보 확인
```

**5. 주문 관리**
```
http://localhost:8080/market-my-orders.html
- 구매 내역 탭: 내가 구매한 상품
- 판매 내역 탭: 내가 판매한 상품
```

**6. 발송 처리 (판매자)**
```
http://localhost:8080/market-my-orders.html
- 판매 내역 탭
- "발송 처리" 버튼 클릭
- 택배사: "CJ대한통운"
- 운송장 번호: "1234567890"
```

**7. 구매 확정 (구매자)**
```
http://localhost:8080/market-my-orders.html
- 구매 내역 탭
- 상품 수령 후 "구매 확정" 버튼 클릭
```

---

## 📦 주문 상태 흐름

```
PENDING (주문 대기)
    ↓ (결제 승인)
PAID (결제 완료)
    ↓ (판매자 발송 처리)
SHIPPED (배송 중)
    ↓ (구매자 구매 확정)
CONFIRMED (구매 확정)
    ↓ (정산 완료)
SETTLED (정산 완료)
```

### 취소/환불 흐름
```
PENDING → CANCELLED (결제 전 취소)
PAID → REFUNDED (결제 후 환불)
```

---

## 🎨 UI/UX 특징

### 1. 상품 상세 페이지
- **구매하기 버튼**: 판매중(ON_SALE) 상품만 표시
- **채팅하기 버튼**: 판매자가 아닌 경우에만 표시
- **찜하기 버튼**: 로그인 사용자만 사용 가능

### 2. 주문 관리 페이지
- **구매/판매 탭 전환**: 한 페이지에서 모든 주문 관리
- **상태별 액션 버튼**: 주문 상태에 따라 동적으로 표시
  - 구매자: 구매 확정, 주문 취소, 환불 요청
  - 판매자: 발송 처리

### 3. 주문 상세 페이지
- **배송 추적**: 운송장 번호 클릭 시 배송 조회 페이지 이동
- **주문 타임라인**: 주문 생성 → 결제 → 발송 → 확정 흐름 시각화
- **실시간 상태 업데이트**: 액션 수행 시 즉시 반영

---

## 🔒 보안 설정

### CORS 설정
SecurityConfig에서 모든 origin 허용:
```java
config.addAllowedOriginPattern("*");
```

> ⚠️ **프로덕션 환경**: 실제 프론트엔드 도메인만 허용하도록 변경!

### API 권한 설정
```java
// 결제 승인은 공개 (토스에서 리다이렉트)
.requestMatchers(HttpMethod.POST, "/api/v1/market/payments/confirm").permitAll()

// 주문/결제 API는 인증 필요
.requestMatchers("/api/v1/market/**").authenticated()
```

---

## 🧪 테스트 체크리스트

### 기본 기능
- [ ] 상품 등록
- [ ] 상품 목록 조회
- [ ] 상품 상세 페이지
- [ ] 구매하기 버튼 클릭
- [ ] 토스 결제창 팝업
- [ ] 테스트 결제 진행
- [ ] 결제 성공 페이지 표시
- [ ] 결제 승인 API 호출
- [ ] 주문 상세 페이지 이동

### 주문 관리
- [ ] 내 구매 목록 조회
- [ ] 내 판매 목록 조회
- [ ] 발송 처리 (판매자)
- [ ] 구매 확정 (구매자)
- [ ] 주문 취소 (결제 전)
- [ ] 환불 요청 (결제 후)

### 에러 처리
- [ ] 로그인하지 않고 구매 시도
- [ ] 이미 판매된 상품 구매 시도
- [ ] 본인 상품 구매 시도
- [ ] 결제 취소 (토스 팝업에서 취소)
- [ ] 결제 실패 페이지 표시

---

## 🐛 트러블슈팅

### 1. "로그인이 필요합니다" 에러
**원인**: JWT 토큰이 없거나 만료됨

**해결**:
```javascript
// 브라우저 콘솔
localStorage.setItem('jwt_token', 'YOUR_TOKEN');
```

### 2. 토스 결제창이 안 뜨는 경우
**원인**: 클라이언트 키 오류 또는 네트워크 문제

**확인 사항**:
- market-detail.html의 TOSS_CLIENT_KEY 확인
- 브라우저 콘솔 에러 확인
- 토스 결제 SDK 로딩 확인

### 3. 결제 승인 실패
**원인**: 백엔드 시크릿 키 오류

**확인 사항**:
- application.properties의 toss.payments.secret-key 확인
- 서버 로그 확인
- 토스 API 상태 확인

### 4. 주문 목록이 안 보이는 경우
**원인**: JWT 토큰의 userId 추출 실패

**해결**:
```javascript
// JWT 토큰 페이로드 확인
const token = localStorage.getItem('jwt_token');
const payload = JSON.parse(atob(token.split('.')[1]));
console.log(payload); // uid 필드 확인
```

---

## 📚 추가 개발 계획

### 단기 (1-2주)
- [ ] 네비게이션 바 통일 (모든 페이지에 "내 주문" 링크 추가)
- [ ] 상품 썸네일 이미지 연동
- [ ] 결제 수단 다양화 (가상계좌, 계좌이체)
- [ ] 모바일 반응형 디자인 개선

### 중기 (1개월)
- [ ] 알림 기능 (주문 상태 변경 시)
- [ ] 판매자 평점 시스템
- [ ] 구매 후기 작성
- [ ] 배송 조회 API 연동
- [ ] 자동 정산 기능

### 장기 (3개월)
- [ ] React/Vue로 프론트엔드 마이그레이션
- [ ] 토스 페이먼츠 웹훅 구현
- [ ] 에스크로 결제 지원
- [ ] 분쟁 처리 시스템
- [ ] 관리자 대시보드

---

## 📞 문의

- **토스 페이먼츠 문의**: https://docs.tosspayments.com/
- **개발자 지원**: support@tosspayments.com
- **ClosetConnect 문의**: support@closetconnect.com

---

**개발 완료일**: 2025-11-17
**버전**: 3.0 (토스 결제 연동 완료)
