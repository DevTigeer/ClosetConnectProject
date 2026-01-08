# CloudRun Worker 배포 가이드

CloudRun API들을 호출하는 경량 Worker를 CloudRun에 배포하는 가이드입니다.

## 📋 시스템 아키텍처

```
[Vercel Frontend]
     ↓ 이미지 업로드
[Railway Spring Boot]
     ↓ RabbitMQ 메시지 발행
[CloudRun Worker] ← 새로 배포할 서비스!
  ├─ rembg (로컬 실행)
  ├─ CloudRun Segmentation API 호출
  ├─ Google Imagen API 호출 (선택적)
  └─ CloudRun Inpainting API 호출
     ↓ 결과 전송 (RabbitMQ)
[Railway Spring Boot]
     ↓ WebSocket
[Vercel Frontend]
```

## 🎯 장점

- **경량**: 큰 AI 모델을 포함하지 않음 (약 500MB)
- **유연성**: CloudRun API들을 독립적으로 스케일 가능
- **비용 효율적**: 필요한 만큼만 리소스 사용
- **유지보수**: CloudRun API들을 개별적으로 업데이트 가능

## 📦 사전 준비

### 1. CloudRun API 서비스들이 배포되어 있어야 함

```bash
# 현재 배포된 서비스 확인
gcloud run services list --region=asia-northeast3
```

필요한 서비스:
- `closetconnect-segmentation` (세그멘테이션)
- `closetconnect-inpainting` (인페인팅)

### 2. Railway RabbitMQ 정보 준비

Railway 대시보드에서 확인:
- RabbitMQ Host (Private URL 사용 권장)
- RabbitMQ Port (기본: 5672)
- RabbitMQ Username
- RabbitMQ Password

### 3. Google Cloud 프로젝트 설정

```bash
# 프로젝트 ID 확인
gcloud config get-value project

# 프로젝트 ID 설정 (필요시)
gcloud config set project YOUR_PROJECT_ID

# Docker 인증
gcloud auth configure-docker
```

## 🚀 배포 방법

### 방법 1: 자동 배포 스크립트 (권장)

```bash
cd /Users/grail/Documents/ClosetConnectProject/aiModel

# deploy-worker-cloudrun.sh 파일 열기
# PROJECT_ID 변수를 실제 프로젝트 ID로 수정

# 배포 실행
./deploy-worker-cloudrun.sh
```

스크립트가 요청하는 정보 입력:
- RabbitMQ Host
- RabbitMQ Port
- RabbitMQ Username
- RabbitMQ Password
- Segmentation API URL: `https://cloest-connect-1054961990592.asia-northeast3.run.app`
- Inpainting API URL: `https://closetconnect-inpainting-1054961990592.asia-northeast3.run.app`
- Google API Key (선택적, Imagen 사용 시)

### 방법 2: 수동 배포

```bash
cd /Users/grail/Documents/ClosetConnectProject/aiModel

# 1. Docker 이미지 빌드
docker build -f Dockerfile.worker-cloudrun -t gcr.io/YOUR_PROJECT_ID/closetconnect-worker:latest .

# 2. Docker 이미지 푸시
docker push gcr.io/YOUR_PROJECT_ID/closetconnect-worker:latest

# 3. CloudRun에 배포
gcloud run deploy closetconnect-worker \
  --image gcr.io/YOUR_PROJECT_ID/closetconnect-worker:latest \
  --region asia-northeast3 \
  --platform managed \
  --no-allow-unauthenticated \
  --memory 2Gi \
  --cpu 1 \
  --timeout 900 \
  --set-env-vars="RABBITMQ_HOST=your-rabbitmq-host,RABBITMQ_PORT=5672,RABBITMQ_USERNAME=your-username,RABBITMQ_PASSWORD=your-password,SEGMENTATION_API_URL=https://cloest-connect-1054961990592.asia-northeast3.run.app,INPAINTING_API_URL=https://closetconnect-inpainting-1054961990592.asia-northeast3.run.app"
```

## ⚙️ CloudRun Worker 설정

### 중요: Worker를 지속적으로 실행하기

CloudRun은 기본적으로 HTTP 요청이 없으면 스케일 다운됩니다. **Worker는 HTTP 요청을 받지 않고 RabbitMQ를 Listening하므로, 반드시 최소 인스턴스를 설정해야 합니다.**

```bash
# 최소 인스턴스 1개로 설정 (항상 실행)
gcloud run services update closetconnect-worker \
  --region asia-northeast3 \
  --min-instances 1
```

⚠️ **주의**: min-instances를 1로 설정하면 Worker가 항상 실행되어 비용이 발생합니다.

### 환경 변수 업데이트

```bash
# 환경 변수만 업데이트 (재배포 없이)
gcloud run services update closetconnect-worker \
  --region asia-northeast3 \
  --update-env-vars="GOOGLE_API_KEY=new-api-key"
```

### 로그 확인

```bash
# 실시간 로그 확인
gcloud run services logs tail closetconnect-worker --region asia-northeast3

# 최근 로그 확인
gcloud run services logs read closetconnect-worker --region asia-northeast3 --limit 100
```

## 🔧 Railway 설정

Railway Spring Boot 서비스의 환경 변수가 올바르게 설정되어 있는지 확인:

```bash
RABBITMQ_ENABLED=true
SPRING_RABBITMQ_ADDRESSES=${RABBITMQ_PRIVATE_URL}
```

Railway RabbitMQ 서비스가 실행 중이어야 합니다.

## 🧪 테스트

### 1. Worker가 실행 중인지 확인

```bash
# Worker 로그에서 연결 메시지 확인
gcloud run services logs tail closetconnect-worker --region asia-northeast3

# 다음 메시지가 보여야 함:
# "✅ Connected to RabbitMQ at ..."
# "🎯 Listening on queue: cloth.processing.queue"
```

### 2. Vercel Frontend에서 옷 업로드 테스트

1. Vercel Frontend 접속
2. 옷 이미지 업로드
3. 진행 상황 확인 (WebSocket)
4. 처리 완료 확인

### 3. 로그 확인

```bash
# Railway Spring Boot 로그 확인
# Railway 대시보드 → Spring Boot 서비스 → Logs

# CloudRun Worker 로그 확인
gcloud run services logs tail closetconnect-worker --region asia-northeast3
```

## 🐛 문제 해결

### Worker가 RabbitMQ에 연결되지 않음

```bash
# 환경 변수 확인
gcloud run services describe closetconnect-worker --region asia-northeast3 --format="value(spec.template.spec.containers[0].env)"

# Railway RabbitMQ Private URL 사용 확인
# CloudRun ↔ Railway 간 네트워크 연결 확인
```

### Worker가 CloudRun API를 호출할 수 없음

```bash
# Segmentation API health check
curl https://cloest-connect-1054961990592.asia-northeast3.run.app/health

# Inpainting API health check
curl https://closetconnect-inpainting-1054961990592.asia-northeast3.run.app/health

# CloudRun Worker 로그에서 API 호출 오류 확인
gcloud run services logs read closetconnect-worker --region asia-northeast3 --limit 50
```

### Worker가 자동으로 중지됨

```bash
# min-instances가 1로 설정되어 있는지 확인
gcloud run services describe closetconnect-worker --region asia-northeast3 --format="value(spec.template.metadata.annotations['autoscaling.knative.dev/minScale'])"

# min-instances 설정
gcloud run services update closetconnect-worker --region asia-northeast3 --min-instances 1
```

### 메모리 부족 오류

```bash
# 메모리 증가
gcloud run services update closetconnect-worker --region asia-northeast3 --memory 4Gi
```

## 💰 비용 최적화

### min-instances 조정

개발/테스트 환경:
```bash
# 비용 절감: 요청이 없으면 스케일 다운
gcloud run services update closetconnect-worker --region asia-northeast3 --min-instances 0
```

⚠️ **주의**: min-instances를 0으로 설정하면 Worker가 중지되어 옷 업로드가 처리되지 않습니다.

프로덕션 환경:
```bash
# 항상 실행
gcloud run services update closetconnect-worker --region asia-northeast3 --min-instances 1
```

### CPU/메모리 최적화

```bash
# CPU를 1개로 유지 (Worker는 CPU 집약적이지 않음)
# 메모리는 2Gi로 충분 (rembg + API 호출만)
gcloud run services update closetconnect-worker \
  --region asia-northeast3 \
  --cpu 1 \
  --memory 2Gi
```

## 📊 모니터링

### CloudRun 대시보드

https://console.cloud.google.com/run?project=YOUR_PROJECT_ID

- 요청 수
- 메모리 사용량
- CPU 사용량
- 오류율

### 로그 모니터링

```bash
# 실시간 로그 확인
gcloud run services logs tail closetconnect-worker --region asia-northeast3 --format json

# 오류만 필터링
gcloud run services logs read closetconnect-worker --region asia-northeast3 --log-filter="severity>=ERROR"
```

## 🔄 업데이트

Worker 코드를 수정한 후:

```bash
cd /Users/grail/Documents/ClosetConnectProject/aiModel

# 재배포
./deploy-worker-cloudrun.sh
```

또는:

```bash
# Docker 이미지 다시 빌드 & 푸시
docker build -f Dockerfile.worker-cloudrun -t gcr.io/YOUR_PROJECT_ID/closetconnect-worker:latest .
docker push gcr.io/YOUR_PROJECT_ID/closetconnect-worker:latest

# CloudRun 업데이트 (환경 변수 유지)
gcloud run deploy closetconnect-worker \
  --image gcr.io/YOUR_PROJECT_ID/closetconnect-worker:latest \
  --region asia-northeast3
```

## 📝 체크리스트

배포 완료 후 확인:

- [ ] CloudRun Worker 서비스가 실행 중
- [ ] min-instances가 1로 설정됨
- [ ] Worker 로그에서 "✅ Connected to RabbitMQ" 메시지 확인
- [ ] Worker 로그에서 "🎯 Listening on queue" 메시지 확인
- [ ] Railway Spring Boot가 RabbitMQ에 연결됨
- [ ] Vercel Frontend에서 옷 업로드 테스트 성공
- [ ] WebSocket 진행상황이 정상적으로 표시됨

## 🎉 완료!

이제 CloudRun Worker가 배포되었고, 옷 업로드 시 CloudRun API들을 호출하여 처리합니다.

### 전체 흐름:

1. 사용자가 Vercel Frontend에서 옷 이미지 업로드
2. Railway Spring Boot가 RabbitMQ에 메시지 발행
3. **CloudRun Worker**가 메시지 수신
4. Worker가 rembg로 배경 제거 (로컬)
5. Worker가 **CloudRun Segmentation API** 호출
6. Worker가 Google Imagen API로 이미지 확장 (선택적)
7. Worker가 **CloudRun Inpainting API** 호출
8. Worker가 결과를 RabbitMQ로 전송
9. Railway Spring Boot가 결과 수신 및 DB 저장
10. WebSocket으로 프론트엔드에 알림
11. 사용자가 처리 완료된 이미지 확인

---

문제가 발생하면 다음을 확인:
1. CloudRun Worker 로그
2. Railway Spring Boot 로그
3. RabbitMQ 연결 상태
4. CloudRun API들의 health check
