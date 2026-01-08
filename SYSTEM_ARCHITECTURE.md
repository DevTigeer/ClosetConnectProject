# ClosetConnect 시스템 아키텍처 총정리

## 📋 목차

1. [시스템 개요](#시스템-개요)
2. [전체 아키텍처](#전체-아키텍처)
3. [서비스별 상세 설정](#서비스별-상세-설정)
4. [데이터 흐름](#데이터-흐름)
5. [배포 환경](#배포-환경)
6. [네트워크 구성](#네트워크-구성)
7. [보안 설정](#보안-설정)
8. [모니터링 및 로깅](#모니터링-및-로깅)

---

## 시스템 개요

**ClosetConnect**는 AI 기반 옷장 관리 및 가상 피팅 서비스입니다.

### 핵심 기능

1. **AI 옷 분석**: 업로드한 옷 이미지를 AI로 분석하여 자동 분류
2. **배경 제거 및 세그멘테이션**: 옷 영역만 추출
3. **이미지 복원**: 잘린 부분을 AI로 복원
4. **가상 착용 (Try-On)**: 여러 옷을 조합하여 가상으로 착용
5. **커뮤니티**: 코디 공유 및 소통
6. **마켓플레이스**: 중고 거래
7. **결제 시스템**: Toss Payments 연동

---

## 전체 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                        사용자 (브라우저)                          │
└──────────────────────┬──────────────────────────────────────────┘
                       │ HTTPS
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Vercel (Frontend)                            │
│  - React 18                                                      │
│  - TypeScript                                                    │
│  - Vite                                                          │
│  - SockJS/STOMP (WebSocket Client)                              │
│  - Axios (HTTP Client)                                           │
└──────────────────────┬──────────────────────────────────────────┘
                       │ HTTPS REST API / WebSocket
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                  Railway (Backend - Spring Boot)                 │
│  - Java 17                                                       │
│  - Spring Boot 3.2.0                                             │
│  - Spring Security + JWT                                         │
│  - WebSocket (STOMP)                                             │
│  - RabbitMQ Client                                               │
│  - MariaDB (MySQL)                                               │
│  - Toss Payments API                                             │
└──────┬──────────────────┬────────────────────────────────────────┘
       │                  │
       │ RabbitMQ AMQP   │ TCP (MySQL)
       │                  │
       ▼                  ▼
┌──────────────┐   ┌──────────────┐
│   RabbitMQ   │   │   MariaDB    │
│  (Railway)   │   │  (Railway)   │
│              │   │              │
│ - Message    │   │ - Users      │
│   Queue      │   │ - Clothes    │
│ - Pub/Sub    │   │ - Posts      │
│              │   │ - Orders     │
└──────┬───────┘   └──────────────┘
       │
       │ AMQP (Public URL)
       │
       ▼
┌─────────────────────────────────────────────────────────────────┐
│              CloudRun Worker (AI Processing)                     │
│  - Python 3.11                                                   │
│  - RabbitMQ Consumer                                             │
│  - rembg (배경 제거)                                              │
│  - HTTP Client (API 호출)                                         │
└──────┬──────────────────┬───────────────────────────────────────┘
       │                  │
       │ HTTPS            │ HTTPS
       │                  │
       ▼                  ▼
┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│ CloudRun     │   │ CloudRun     │   │ CloudRun     │
│ Segmentation │   │ Inpainting   │   │ Try-On       │
│              │   │              │   │              │
│ - SegFormer  │   │ - Stable     │   │ - Gemini     │
│ - FastAPI    │   │   Diffusion  │   │ - Flask      │
└──────────────┘   └──────────────┘   └──────────────┘
                                              │
                                              │ HTTPS
                                              ▼
                                    ┌──────────────────┐
                                    │   Google AI      │
                                    │   Gemini API     │
                                    │                  │
                                    │ - Image          │
                                    │   Generation     │
                                    └──────────────────┘
```

---

## 서비스별 상세 설정

### 1. Vercel (Frontend)

#### 기술 스택
- **Framework**: React 18 + TypeScript
- **Build Tool**: Vite
- **Styling**: CSS Modules / Styled Components
- **State Management**: Context API
- **WebSocket**: SockJS + STOMP
- **HTTP Client**: Axios

#### 배포 설정
```yaml
Framework Preset: Vite
Build Command: npm run build
Output Directory: dist
Install Command: npm install
Node Version: 18.x
```

#### 환경 변수
```bash
VITE_API_BASE_URL=https://your-railway-app.railway.app
VITE_WS_BASE_URL=https://your-railway-app.railway.app
```

#### 도메인 설정
- **Production**: `your-app.vercel.app`
- **Custom Domain**: (선택적) `yourdomain.com`

#### 특징
- ✅ **자동 배포**: GitHub Push 시 자동 배포
- ✅ **Preview 배포**: PR마다 Preview URL 생성
- ✅ **CDN**: 전 세계 Edge Network
- ✅ **HTTPS**: 자동 SSL 인증서
- ✅ **Zero Config**: 설정 파일 불필요

---

### 2. Railway (Backend)

#### 2-1. Spring Boot Service

##### 기술 스택
- **Language**: Java 17
- **Framework**: Spring Boot 3.2.0
- **Security**: Spring Security + JWT
- **Database**: MariaDB (JDBC)
- **WebSocket**: Spring WebSocket (STOMP)
- **Message Queue**: RabbitMQ (AMQP)
- **Payment**: Toss Payments API

##### 배포 설정
```yaml
Build Command: ./gradlew clean bootJar
Start Command: java -Xmx384m -Xms256m -XX:MaxMetaspaceSize=128m -jar build/libs/ClosetConnectProject-0.0.1-SNAPSHOT.jar
Port: 8080
```

##### 환경 변수
```bash
# Spring Profile
SPRING_PROFILES_ACTIVE=prod

# Database
DB_URL=jdbc:mariadb://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}
DB_USERNAME=${{MySQL.MYSQLUSER}}
DB_PASSWORD=${{MySQL.MYSQLPASSWORD}}

# JWT
JWT_SECRET=your-very-long-random-secret-key
JWT_TOKEN_VALIDITY=3600

# RabbitMQ
RABBITMQ_ENABLED=true
SPRING_RABBITMQ_ADDRESSES=${{RabbitMQ.RABBITMQ_PRIVATE_URL}}

# CloudRun APIs
TRYON_API_URL=https://closetconnect-tryon-xxx.run.app
CLOTH_SEGMENTATION_SERVER_URL=https://closetconnect-segmentation-xxx.run.app
INPAINTING_SERVER_URL=https://closetconnect-inpainting-xxx.run.app

# Toss Payments
TOSS_CLIENT_KEY=test_gck_xxx
TOSS_SECRET_KEY=test_gsk_xxx

# File Upload
UPLOAD_DIR=./uploads

# Memory Optimization
JAVA_OPTS=-Xmx384m -Xms256m -XX:MaxMetaspaceSize=128m
```

##### CORS 설정
```java
@Configuration
public class WebConfig {
    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    // Vercel 도메인 허용
    // https://*.vercel.app
}
```

##### WebSocket 설정
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig {
    // STOMP over SockJS
    // Endpoint: /ws
    // Destinations: /topic/*, /queue/*
}
```

---

#### 2-2. MariaDB Service

##### 설정
- **Engine**: MariaDB 10.11
- **Storage**: 1GB (Auto-scaling)
- **Backup**: 자동 백업 활성화

##### 데이터베이스 스키마
```sql
-- 주요 테이블
- users (사용자)
- clothes (옷)
- ootd (코디)
- posts (게시글)
- comments (댓글)
- market_products (상품)
- orders (주문)
- chat_messages (채팅)
```

##### 연결 정보
```bash
MYSQLHOST=containers-us-west-xxx.railway.app
MYSQLPORT=6789
MYSQLDATABASE=railway
MYSQLUSER=root
MYSQLPASSWORD=xxx
```

---

#### 2-3. RabbitMQ Service

##### 설정
- **Version**: RabbitMQ 3.12
- **Management UI**: 활성화
- **Port**: 5672 (AMQP), 15672 (Management)

##### Exchange & Queue 구조
```
Exchange: cloth.exchange (direct)
├─ Queue: cloth.processing.queue
│  └─ Routing Key: cloth.processing
│  └─ Consumer: CloudRun Worker
├─ Queue: cloth.result.queue
│  └─ Routing Key: cloth.result
│  └─ Consumer: Spring Boot
└─ Queue: cloth.progress.queue
   └─ Routing Key: cloth.progress
   └─ Consumer: Spring Boot (WebSocket 전송)
```

##### 메시지 흐름
```
1. Spring Boot → RabbitMQ (cloth.processing.queue)
   - 옷 업로드 이벤트 발행

2. CloudRun Worker → RabbitMQ 구독
   - 메시지 수신 및 AI 처리

3. CloudRun Worker → RabbitMQ (cloth.progress.queue)
   - 진행 상황 업데이트 (10%, 25%, 50%, 75%, 95%)

4. CloudRun Worker → RabbitMQ (cloth.result.queue)
   - 최종 결과 전송

5. Spring Boot → DB 저장 & WebSocket 전송
   - 결과 저장 및 클라이언트에 알림
```

##### 연결 정보
```bash
# Private URL (Railway 내부)
RABBITMQ_PRIVATE_URL=amqp://user:pass@rabbitmq.railway.internal:5672

# Public URL (외부 접근 - CloudRun Worker 사용)
RABBITMQ_URL=amqp://user:pass@xxx.proxy.rlwy.net:xxxxx
```

---

### 3. CloudRun (AI Services)

#### 3-1. CloudRun Worker (메인 처리)

##### 기술 스택
- **Language**: Python 3.11
- **Dependencies**:
  - `rembg[cpu]` - 배경 제거
  - `pika` - RabbitMQ 클라이언트
  - `requests` - HTTP 클라이언트
  - `google-generativeai` - Gemini API

##### 배포 설정
```yaml
Region: asia-northeast3 (Seoul)
Memory: 2 GiB
CPU: 1
Min Instances: 1 (항상 실행)
Max Instances: 1
Timeout: 900s (15분)
Concurrency: 1
CPU Allocation: Always allocated
```

##### 환경 변수
```bash
RABBITMQ_HOST=xxx.proxy.rlwy.net
RABBITMQ_PORT=xxxxx
RABBITMQ_USERNAME=xxx
RABBITMQ_PASSWORD=xxx
SEGMENTATION_API_URL=https://closetconnect-segmentation-xxx.run.app
INPAINTING_API_URL=https://closetconnect-inpainting-xxx.run.app
GOOGLE_API_KEY=AIzaSyA...
```

##### 처리 파이프라인
```
1. RabbitMQ에서 메시지 수신 (옷 이미지 업로드)
   └─ Progress: 0%

2. rembg로 배경 제거 (로컬)
   └─ Progress: 25%
   └─ Output: removed_bg.png

3. CloudRun Segmentation API 호출
   └─ Progress: 50%
   └─ Output: segmented.png (옷 영역만 크롭)

4. Google Imagen API로 이미지 확장 (선택적)
   └─ Progress: 70%
   └─ Output: expanded.png

5. CloudRun Inpainting API 호출
   └─ Progress: 95%
   └─ Output: inpainted.png (복원된 이미지)

6. RabbitMQ로 결과 전송
   └─ Progress: 100%
```

---

#### 3-2. Segmentation API

##### 기술 스택
- **Framework**: FastAPI
- **Model**: SegFormer (Hugging Face)
  - `mattmdjaga/segformer_b2_clothes`
- **Device**: CPU

##### 엔드포인트
```
GET  /health
POST /segment
```

##### 배포 설정
```yaml
Region: asia-northeast3
Memory: 2 GiB
CPU: 2
Allow Unauthenticated: Yes
```

##### 기능
- 의류 세그멘테이션 (18개 클래스)
- 상의/하의/신발/가방 등 자동 감지
- 바운딩 박스 추출
- 크롭 이미지 생성

---

#### 3-3. Inpainting API

##### 기술 스택
- **Framework**: FastAPI
- **Model**: Stable Diffusion Inpainting
- **Device**: CPU

##### 엔드포인트
```
GET  /health
POST /inpaint
```

##### 배포 설정
```yaml
Region: asia-northeast3
Memory: 2 GiB
CPU: 2
Timeout: 300s
Allow Unauthenticated: Yes
```

##### 기능
- 잘린 옷 이미지 복원
- 캔버스 확장 (extend_ratio)
- 자연스러운 이미지 생성

---

#### 3-4. Try-On API

##### 기술 스택
- **Framework**: Flask
- **Model**: Google Gemini 2.0 Flash
- **API**: Google AI Studio

##### 엔드포인트
```
GET  /health
POST /tryon
POST /tryon/url
```

##### 배포 설정
```yaml
Region: asia-northeast3
Memory: 2 GiB
CPU: 2
Timeout: 300s
Allow Unauthenticated: Yes
```

##### 기능
- 여러 옷 조합 (상의 + 하의 + 신발 + 악세서리)
- Gemini API로 가상 착용 이미지 생성
- Base64 이미지 입출력

---

## 데이터 흐름

### 1. 옷 업로드 흐름

```
[사용자] Vercel Frontend
   ↓ 1. 이미지 선택 & 업로드 버튼 클릭
   ↓    POST /api/v1/clothes/upload (multipart/form-data)
   ↓
[Spring Boot] Railway Backend
   ↓ 2. 원본 이미지 저장 (/uploads/original/)
   ↓ 3. Cloth 엔티티 생성 (status: PROCESSING)
   ↓ 4. RabbitMQ 메시지 발행 (cloth.processing.queue)
   ↓    { clothId, userId, imageBytes, imageType }
   ↓
[RabbitMQ] Railway
   ↓ 5. 메시지 큐에 저장
   ↓
[CloudRun Worker]
   ↓ 6. 메시지 수신 (Consumer)
   ↓ 7. 배경 제거 (rembg) → Progress 25%
   ↓ 8. Segmentation API 호출 → Progress 50%
   ↓ 9. Imagen 확장 (선택) → Progress 70%
   ↓ 10. Inpainting API 호출 → Progress 95%
   ↓ 11. RabbitMQ로 결과 전송 (cloth.result.queue)
   ↓
[Spring Boot] Railway Backend
   ↓ 12. 결과 수신 (Consumer)
   ↓ 13. DB 업데이트
   ↓     - removed_bg_url
   ↓     - segmented_url
   ↓     - inpainted_url
   ↓     - suggested_category
   ↓     - status: READY_FOR_REVIEW
   ↓ 14. WebSocket 메시지 전송
   ↓     /queue/cloth/progress/{userId}
   ↓
[사용자] Vercel Frontend
   ↓ 15. WebSocket으로 실시간 진행 상황 수신
   ↓ 16. 처리 완료 시 결과 이미지 표시
   └─ 17. 카테고리 확인 & 최종 이미지 선택
```

---

### 2. 가상 착용 (Try-On) 흐름

```
[사용자] Vercel Frontend
   ↓ 1. 옷 여러 개 선택 (상의, 하의, 신발 등)
   ↓ 2. "가상 착용" 버튼 클릭
   ↓    POST /api/v1/outfit/tryon
   ↓    { upperClothesId, lowerClothesId, shoesId, ... }
   ↓
[Spring Boot] Railway Backend
   ↓ 3. DB에서 옷 정보 조회
   ↓ 4. 이미지 파일 로드 (inpainted 우선)
   ↓ 5. Base64 인코딩
   ↓ 6. CloudRun Try-On API 호출
   ↓    POST https://closetconnect-tryon-xxx.run.app/tryon
   ↓    { upperClothes: base64, lowerClothes: base64, ... }
   ↓
[CloudRun Try-On API]
   ↓ 7. Base64 디코딩
   ↓ 8. Google Gemini API 호출
   ↓    - Prompt: "이 옷들을 입은 사람을 생성해줘"
   ↓ 9. Gemini가 가상 착용 이미지 생성
   ↓ 10. Base64로 반환
   ↓     { success: true, image: "data:image/png;base64,..." }
   ↓
[Spring Boot] Railway Backend
   ↓ 11. Base64 디코딩 및 파일 저장
   ↓     /uploads/tryon/tryon_{userId}_{timestamp}.png
   ↓ 12. 클라이언트에 URL 반환
   ↓
[사용자] Vercel Frontend
   └─ 13. 가상 착용 이미지 표시
```

---

### 3. WebSocket 실시간 통신

```
[Vercel Frontend]
   ↓ 1. SockJS 연결
   ↓    new SockJS('https://railway-app.railway.app/ws')
   ↓ 2. STOMP 연결
   ↓    stompClient.connect()
   ↓ 3. 구독
   ↓    /queue/cloth/progress/{userId}
   ↓
[Spring Boot] WebSocket Server
   ↓ 4. 연결 수락
   ↓ 5. RabbitMQ에서 진행 상황 수신
   ↓ 6. WebSocket으로 브로드캐스트
   ↓    { clothId, status, currentStep, progressPercentage }
   ↓
[Vercel Frontend]
   └─ 7. 실시간 UI 업데이트
      - Progress Bar
      - 현재 단계 텍스트
      - 로딩 스피너
```

---

## 배포 환경

### 인프라 비교

| 항목 | Vercel | Railway | CloudRun |
|------|--------|---------|----------|
| **타입** | Static/SSR | Container | Container |
| **스케일링** | 자동 (무제한) | 수동 | 자동 (0-N) |
| **가격** | Free (Hobby) | $5/월~ | 사용량 기반 |
| **지역** | Global CDN | US West | Asia Northeast3 |
| **배포 방식** | Git Push | Git Push | Docker Image |
| **SSL** | 자동 | 자동 | 자동 |
| **환경 변수** | UI/CLI | UI/CLI | UI/CLI |
| **로그** | Real-time | Real-time | Cloud Logging |

---

### 비용 예상 (월간)

| 서비스 | 사양 | 예상 비용 |
|--------|------|----------|
| **Vercel** | Hobby Plan | $0 (무료) |
| **Railway** | |  |
| └─ Spring Boot | 512MB RAM | $5-10 |
| └─ MariaDB | 1GB Storage | $2-5 |
| └─ RabbitMQ | 256MB RAM | $2-3 |
| **CloudRun** | |  |
| └─ Worker | 2GB, 1 CPU, Always On | $20-30 |
| └─ Segmentation | 2GB, 2 CPU, On-demand | $5-10 |
| └─ Inpainting | 2GB, 2 CPU, On-demand | $5-10 |
| └─ Try-On | 2GB, 2 CPU, On-demand | $5-10 |
| **Google AI** | |  |
| └─ Gemini API | Flash 모델 | $0-10 |
| **합계** | | **$44-88/월** |

> 💡 **비용 절감 팁**:
> - CloudRun Worker의 min-instances를 0으로 설정 (개발 환경)
> - Gemini API 무료 티어 활용
> - Railway의 Trial Credits 사용

---

## 네트워크 구성

### 도메인 및 엔드포인트

```
Frontend:
  https://your-app.vercel.app

Backend API:
  https://your-railway-app.railway.app
  ├─ /api/v1/auth/*          (인증)
  ├─ /api/v1/users/*         (사용자)
  ├─ /api/v1/clothes/*       (옷)
  ├─ /api/v1/outfit/*        (Try-On)
  ├─ /api/v1/posts/*         (게시글)
  ├─ /api/v1/market/*        (마켓)
  └─ /ws                     (WebSocket)

CloudRun APIs:
  https://closetconnect-worker-xxx.run.app        (Worker, 직접 호출 X)
  https://closetconnect-segmentation-xxx.run.app  (Segmentation)
  https://closetconnect-inpainting-xxx.run.app    (Inpainting)
  https://closetconnect-tryon-xxx.run.app         (Try-On)
```

---

### CORS 설정

**Spring Boot (application-prod.properties)**
```java
@CrossOrigin(
    origins = {
        "https://*.vercel.app",
        "http://localhost:5173"
    },
    allowedHeaders = "*",
    allowCredentials = "true"
)
```

**CloudRun APIs (FastAPI/Flask)**
```python
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:8080",
        "https://*.railway.app",
        "https://*.vercel.app"
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
```

---

## 보안 설정

### 1. 인증 및 인가

#### JWT 기반 인증
```java
// Spring Security 설정
- JWT 토큰 발급 (Access Token)
- 유효 기간: 1시간
- 암호화: HS256
- Secret Key: 환경 변수로 관리
```

#### 권한 관리
```java
// 사용자 권한
- ROLE_USER: 일반 사용자
- ROLE_ADMIN: 관리자

// API 엔드포인트 보호
@PreAuthorize("hasRole('USER')")
@PreAuthorize("hasRole('ADMIN')")
```

---

### 2. 환경 변수 보안

**민감 정보 관리**
- ✅ DB 비밀번호: Railway 환경 변수
- ✅ JWT Secret: Railway 환경 변수
- ✅ Toss Secret Key: Railway 환경 변수
- ✅ Google API Key: CloudRun Secret Manager
- ✅ RabbitMQ 비밀번호: Railway 환경 변수

**절대 Git에 커밋하지 않음**
- `.env` 파일
- `application-prod.properties` (템플릿만)

---

### 3. HTTPS/TLS

- ✅ Vercel: 자동 SSL (Let's Encrypt)
- ✅ Railway: 자동 SSL
- ✅ CloudRun: 자동 SSL (Google-managed)

---

### 4. API 보안

**Rate Limiting** (Spring Boot)
```java
// Bucket4j 사용
@RateLimit(permits = 100, timeUnit = TimeUnit.MINUTES)
```

**Input Validation**
```java
// Spring Validation
@Valid @RequestBody CreateRequest request
```

---

## 모니터링 및 로깅

### 1. Vercel

**Analytics**
- Page Views
- Deployment Status
- Build Logs

**Logs**
```bash
# Vercel CLI
vercel logs
```

---

### 2. Railway

**Metrics**
- CPU Usage
- Memory Usage
- Network I/O
- Request Count

**Logs**
- Real-time Logs (Dashboard)
- Log Filtering
- Log Export

---

### 3. CloudRun

**Cloud Monitoring**
```
- Request Count
- Request Latency
- Instance Count
- Memory/CPU Usage
- Error Rate
```

**Cloud Logging**
```
- Application Logs
- Request Logs
- Error Logs
- Filter by Severity
```

**Alerts** (선택적)
```
- Error Rate > 5%
- Memory Usage > 80%
- CPU Usage > 80%
```

---

## 트러블슈팅 가이드

### 1. Frontend (Vercel)

**문제**: API 호출 실패
```
해결:
1. VITE_API_BASE_URL 환경 변수 확인
2. CORS 설정 확인 (Spring Boot)
3. Railway 서비스 상태 확인
```

**문제**: WebSocket 연결 실패
```
해결:
1. SockJS fallback 확인
2. Railway WebSocket 지원 확인
3. CORS allowedOrigins 확인
```

---

### 2. Backend (Railway)

**문제**: 메모리 부족 (OOM)
```
해결:
1. JAVA_OPTS 메모리 설정 조정
   -Xmx384m → -Xmx512m
2. Railway Plan 업그레이드
```

**문제**: RabbitMQ 연결 실패
```
해결:
1. RABBITMQ_PRIVATE_URL 확인
2. RabbitMQ 서비스 상태 확인
3. Credentials 확인
```

---

### 3. CloudRun

**문제**: Worker가 중지됨
```
해결:
1. min-instances = 1 설정 확인
2. "CPU is always allocated" 확인
```

**문제**: rembg 오류
```
해결:
1. requirements: rembg[cpu] 확인
2. onnxruntime 설치 확인
```

---

## 향후 개선 계획

### 단기 (1-3개월)
- [ ] Redis 캐싱 추가 (옷 목록, 사용자 정보)
- [ ] S3/GCS로 이미지 스토리지 이전
- [ ] Prometheus + Grafana 모니터링
- [ ] Sentry 에러 트래킹
- [ ] CI/CD 파이프라인 개선

### 중기 (3-6개월)
- [ ] Kubernetes 마이그레이션 (CloudRun → GKE)
- [ ] Multi-region 배포 (글로벌 서비스)
- [ ] CDN 최적화 (이미지 전송 속도)
- [ ] API Gateway 도입
- [ ] Service Mesh (Istio)

### 장기 (6-12개월)
- [ ] AI 모델 자체 훈련 및 최적화
- [ ] Real-time Try-On (웹캠 연동)
- [ ] Mobile App (React Native)
- [ ] Microservices 아키텍처 전환
- [ ] Multi-cloud (AWS, Azure 추가)

---

## 참고 문서

- [Vercel Documentation](https://vercel.com/docs)
- [Railway Documentation](https://docs.railway.app)
- [Google Cloud Run Documentation](https://cloud.google.com/run/docs)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [RabbitMQ Documentation](https://www.rabbitmq.com/documentation.html)

---

**마지막 업데이트**: 2026-01-08
**작성자**: ClosetConnect Team
