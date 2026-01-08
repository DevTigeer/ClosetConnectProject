# CloudRun Worker 웹 콘솔 배포 가이드

웹 브라우저에서 Google Cloud Console을 사용하여 CloudRun Worker를 배포하는 방법입니다.

## 📋 사전 준비

### 필요한 정보 준비:

1. **RabbitMQ 정보** (Railway에서 확인)
   - Host (Private URL)
   - Port: 5672
   - Username
   - Password

2. **CloudRun API URLs**
   - Segmentation: `https://cloest-connect-1054961990592.asia-northeast3.run.app`
   - Inpainting: `https://closetconnect-inpainting-1054961990592.asia-northeast3.run.app`

3. **Google API Key** (선택적, Imagen 사용 시)

---

## 🚀 배포 단계

### STEP 1: Docker 이미지 빌드 & 푸시

먼저 로컬에서 Docker 이미지를 빌드하고 Google Container Registry에 푸시해야 합니다.

```bash
cd /Users/grail/Documents/ClosetConnectProject/aiModel

# Google Cloud 프로젝트 ID 확인
gcloud config get-value project

# Docker 인증
gcloud auth configure-docker

# Docker 이미지 빌드 (YOUR_PROJECT_ID를 실제 프로젝트 ID로 변경)
docker build -f Dockerfile.worker-cloudrun -t gcr.io/YOUR_PROJECT_ID/closetconnect-worker:latest .

# Docker 이미지 푸시
docker push gcr.io/YOUR_PROJECT_ID/closetconnect-worker:latest
```

---

### STEP 2: Google Cloud Console 접속

1. 브라우저에서 **Google Cloud Console** 접속
   ```
   https://console.cloud.google.com/
   ```

2. 올바른 프로젝트가 선택되어 있는지 확인 (상단의 프로젝트 선택 드롭다운)

---

### STEP 3: Cloud Run 서비스 생성

#### 3-1. Cloud Run 페이지로 이동

1. 왼쪽 메뉴에서 **"Cloud Run"** 클릭
   - 또는 상단 검색창에서 "Cloud Run" 검색

2. **"서비스 만들기"** 또는 **"CREATE SERVICE"** 버튼 클릭

---

#### 3-2. 컨테이너 이미지 URL 설정

**컨테이너 이미지 URL** 섹션:

1. **"컨테이너 이미지 URL 선택"** 클릭

2. 목록에서 방금 푸시한 이미지 선택:
   ```
   gcr.io/YOUR_PROJECT_ID/closetconnect-worker:latest
   ```

3. **"SELECT"** 클릭

---

#### 3-3. 서비스 설정

**서비스 설정** 섹션:

- **서비스 이름**: `closetconnect-worker`
- **리전**: `asia-northeast3 (Seoul)`
- **CPU 할당 및 가격 책정**:
  - ✅ **"CPU is always allocated"** 선택 (중요!)
  - 이유: Worker는 HTTP 요청을 받지 않고 RabbitMQ를 Listening

---

#### 3-4. 인증 설정

**인증** 섹션:

- ✅ **"Require authentication"** (인증 필요) 선택
- Worker는 외부에서 직접 호출하지 않으므로 인증 필요

---

#### 3-5. 컨테이너 설정 (중요!)

**"CONTAINER, NETWORKING, SECURITY"** 탭 클릭

##### 1) 컨테이너 포트
- **Container port**: `8080` (기본값, 실제로는 사용 안 함)

##### 2) 용량
- **메모리**: `2 GiB`
- **CPU**: `1`

##### 3) 실행
- **최소 인스턴스 수**: `1` ⭐ (중요! 항상 실행)
- **최대 인스턴스 수**: `1` (비용 절감)

##### 4) 요청 제한 시간
- **요청 제한 시간**: `900` 초 (15분)

---

#### 3-6. 환경 변수 설정 (매우 중요!)

**"VARIABLES & SECRETS"** 탭 클릭

**"ADD VARIABLE"** 버튼을 클릭하여 다음 환경 변수들을 추가:

| 이름 | 값 | 설명 |
|------|-----|------|
| `RABBITMQ_HOST` | Railway RabbitMQ Host | Railway에서 복사 |
| `RABBITMQ_PORT` | `5672` | RabbitMQ 포트 |
| `RABBITMQ_USERNAME` | RabbitMQ Username | Railway에서 복사 |
| `RABBITMQ_PASSWORD` | RabbitMQ Password | Railway에서 복사 |
| `SEGMENTATION_API_URL` | `https://cloest-connect-1054961990592.asia-northeast3.run.app` | 세그멘테이션 API |
| `INPAINTING_API_URL` | `https://closetconnect-inpainting-1054961990592.asia-northeast3.run.app` | 인페인팅 API |
| `GOOGLE_API_KEY` | Your Google API Key | (선택적) Imagen 사용 시 |

**환경 변수 추가 방법:**
1. **"ADD VARIABLE"** 클릭
2. **Name**에 변수 이름 입력 (예: `RABBITMQ_HOST`)
3. **Value**에 값 입력
4. 각 변수마다 반복

---

### STEP 4: 배포 완료

1. 페이지 하단의 **"CREATE"** 버튼 클릭

2. 배포 진행 상황 확인 (1-3분 소요)

3. 배포 완료 후 서비스 상태가 **"Ready"** (초록색)인지 확인

---

## ✅ 배포 확인

### 1. 로그 확인

Cloud Run 서비스 페이지에서:

1. **"LOGS"** 탭 클릭

2. 다음 메시지가 보이는지 확인:
   ```
   ✅ Connected to RabbitMQ at ...
   🎯 Listening on queue: cloth.processing.queue
   ```

### 2. 서비스 상태 확인

- **Status**: ✅ Ready (초록색)
- **Instances**: 1 (항상 실행 중)

---

## 🧪 테스트

### 1. Vercel Frontend에서 옷 업로드

1. Vercel Frontend 접속
2. 로그인
3. 옷 이미지 업로드
4. 진행 상황 확인 (WebSocket)
5. 처리 완료 확인

### 2. Railway Spring Boot 로그 확인

Railway 대시보드에서 Spring Boot 로그 확인:
- RabbitMQ 메시지 발행 로그
- 결과 수신 로그

### 3. CloudRun Worker 로그 확인

Google Cloud Console → Cloud Run → closetconnect-worker → LOGS 탭:
- 메시지 수신 로그
- API 호출 로그
- 결과 전송 로그

---

## 🔧 환경 변수 수정

배포 후 환경 변수를 수정하려면:

1. Cloud Run 서비스 페이지에서 **"EDIT & DEPLOY NEW REVISION"** 클릭

2. **"VARIABLES & SECRETS"** 탭으로 이동

3. 수정할 환경 변수를 찾아서 값 변경

4. **"DEPLOY"** 클릭

---

## 🐛 문제 해결

### Worker가 RabbitMQ에 연결 안 됨

**로그에서 확인할 메시지:**
```
❌ Error: Connection refused
```

**해결 방법:**
1. Cloud Run 서비스 → **EDIT & DEPLOY NEW REVISION**
2. **VARIABLES & SECRETS** 탭에서 RabbitMQ 환경 변수 확인
3. Railway에서 RabbitMQ **Private URL** 사용하는지 확인
4. RabbitMQ Username/Password 정확한지 확인

---

### Worker가 자동으로 중지됨

**증상:**
- 옷 업로드 시 처리가 진행되지 않음
- CloudRun 로그에 아무것도 안 보임

**해결 방법:**
1. Cloud Run 서비스 → **EDIT & DEPLOY NEW REVISION**
2. **CONTAINER** 탭 → **Capacity** 섹션
3. **최소 인스턴스 수**를 `1`로 설정
4. **DEPLOY** 클릭

---

### CloudRun API 호출 실패

**로그에서 확인할 메시지:**
```
❌ Segmentation API call failed
```

**해결 방법:**

1. **Segmentation API가 실행 중인지 확인:**
   ```
   https://cloest-connect-1054961990592.asia-northeast3.run.app/health
   ```
   브라우저에서 접속하여 `{"status": "healthy"}` 응답 확인

2. **Inpainting API가 실행 중인지 확인:**
   ```
   https://closetconnect-inpainting-1054961990592.asia-northeast3.run.app/health
   ```

3. **환경 변수 확인:**
   - `SEGMENTATION_API_URL` 오타 없는지
   - `INPAINTING_API_URL` 오타 없는지

---

### 메모리 부족 오류

**로그에서 확인할 메시지:**
```
❌ Memory limit exceeded
```

**해결 방법:**
1. Cloud Run 서비스 → **EDIT & DEPLOY NEW REVISION**
2. **CONTAINER** 탭 → **Capacity**
3. **메모리**를 `4 GiB`로 증가
4. **DEPLOY** 클릭

---

## 💰 비용 확인

### 예상 비용 (아시아 리전 기준)

**min-instances = 1 (항상 실행):**
- CPU: 1 vCPU × 24시간 × 30일 = 약 $15-20/월
- 메모리: 2GB × 24시간 × 30일 = 약 $5-10/월
- **총 예상 비용: 약 $20-30/월**

**비용 절감 옵션:**
- **개발/테스트**: min-instances = 0 (사용 시에만 과금)
- **프로덕션**: min-instances = 1 (항상 실행)

### 비용 모니터링

1. Google Cloud Console 상단 메뉴
2. **"Billing"** (결제) 클릭
3. **"Reports"** (보고서)에서 CloudRun 비용 확인

---

## 📊 모니터링

### Cloud Run 대시보드

1. Cloud Run 서비스 페이지 → **"METRICS"** 탭

2. 확인 가능한 지표:
   - 요청 수 (Worker는 0이 정상)
   - 인스턴스 수 (1개 유지)
   - 메모리 사용량
   - CPU 사용량

### 로그 스트리밍

**실시간 로그 확인:**
1. Cloud Run 서비스 페이지 → **"LOGS"** 탭
2. 자동으로 새로운 로그가 표시됨

**로그 필터링:**
- **Severity**: ERROR만 보기
- **시간 범위**: 최근 1시간, 1일 등

---

## ✅ 배포 완료 체크리스트

- [ ] Docker 이미지가 GCR에 푸시됨
- [ ] Cloud Run 서비스 생성 완료
- [ ] 서비스 이름: `closetconnect-worker`
- [ ] 리전: `asia-northeast3`
- [ ] 최소 인스턴스: `1`
- [ ] 메모리: `2 GiB`
- [ ] CPU: `1`
- [ ] 타임아웃: `900초`
- [ ] RabbitMQ 환경 변수 7개 모두 설정
- [ ] 로그에서 "Connected to RabbitMQ" 확인
- [ ] 로그에서 "Listening on queue" 확인
- [ ] Vercel에서 옷 업로드 테스트 성공
- [ ] 진행 상황이 WebSocket으로 표시됨

---

## 🎉 완료!

이제 CloudRun Worker가 웹 콘솔을 통해 배포되었습니다!

옷 업로드 → RabbitMQ → CloudRun Worker → CloudRun APIs → 결과 반환의 전체 파이프라인이 작동합니다.

문제가 있으면 로그를 확인하고, 이 가이드의 "문제 해결" 섹션을 참고하세요.
