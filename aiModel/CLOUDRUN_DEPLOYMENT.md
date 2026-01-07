# Cloud Run 배포 가이드

ClosetConnect AI 서비스를 Google Cloud Run에 배포하는 방법을 설명합니다.

## 📋 사전 요구사항

1. **Google Cloud Platform (GCP) 계정**
   - GCP 프로젝트 생성
   - 결제 계정 연결

2. **gcloud CLI 설치**
   ```bash
   # macOS
   brew install google-cloud-sdk

   # 또는 공식 설치 방법
   # https://cloud.google.com/sdk/docs/install
   ```

3. **gcloud 로그인 및 프로젝트 설정**
   ```bash
   # 로그인
   gcloud auth login

   # 프로젝트 ID 확인
   gcloud projects list

   # 프로젝트 설정
   gcloud config set project YOUR_PROJECT_ID
   ```

## 🚀 배포 방법

### 방법 1: 모든 서비스 한 번에 배포 (권장)

1. **deploy-cloudrun.sh 파일 수정**
   ```bash
   # PROJECT_ID를 실제 GCP 프로젝트 ID로 변경
   nano deploy-cloudrun.sh
   # PROJECT_ID="your-gcp-project-id" 부분을 수정
   ```

2. **배포 실행**
   ```bash
   cd aiModel
   ./deploy-cloudrun.sh
   ```

3. **배포되는 서비스**
   - `closetconnect-segmentation` (의상 세그멘테이션)
   - `closetconnect-inpainting` (이미지 인페인팅)
   - `closetconnect-tryon` (가상 착용)

### 방법 2: 개별 서비스 배포

특정 서비스만 배포하거나 업데이트하려는 경우:

```bash
# Segmentation API만 배포
./deploy-single-service.sh segmentation

# Inpainting API만 배포
./deploy-single-service.sh inpainting

# Try-on API만 배포
./deploy-single-service.sh tryon
```

### 방법 3: 수동 배포 (gcloud 명령어 직접 사용)

```bash
# 예: Segmentation API 배포
gcloud run deploy closetconnect-segmentation \
  --source . \
  --platform managed \
  --region asia-northeast3 \
  --allow-unauthenticated \
  --memory 2Gi \
  --cpu 2 \
  --timeout 300 \
  --port 8002 \
  --dockerfile Dockerfile.segmentation
```

## 📦 배포된 서비스 확인

```bash
# 배포된 서비스 목록 확인
gcloud run services list --region asia-northeast3

# 특정 서비스 상세 정보
gcloud run services describe closetconnect-segmentation --region asia-northeast3

# 서비스 URL 확인
gcloud run services describe closetconnect-segmentation \
  --region asia-northeast3 \
  --format 'value(status.url)'
```

## 🔧 배포 설정

각 서비스는 다음과 같이 구성됩니다:

- **메모리**: 2GB (AI 모델 로드에 필요)
- **CPU**: 2 vCPU
- **타임아웃**: 300초 (5분)
- **최대 인스턴스**: 10개
- **인증**: 비활성화 (allow-unauthenticated)

필요에 따라 `deploy-cloudrun.sh`에서 이 설정들을 조정할 수 있습니다.

## 🌐 서비스 URL

배포 후 각 서비스는 다음과 같은 형식의 URL을 받습니다:

```
https://closetconnect-segmentation-[hash]-an.a.run.app
https://closetconnect-inpainting-[hash]-an.a.run.app
https://closetconnect-tryon-[hash]-an.a.run.app
```

## 🔗 Backend 연동

배포 완료 후, Railway의 Spring Boot 애플리케이션에서 환경변수를 설정하세요:

```bash
# Railway 환경변수 설정
SEGMENTATION_API_URL=https://closetconnect-segmentation-xxx.a.run.app
INPAINTING_API_URL=https://closetconnect-inpainting-xxx.a.run.app
TRYON_API_URL=https://closetconnect-tryon-xxx.a.run.app
```

Spring Boot의 `application.yml` 또는 `application.properties`에서:

```yaml
ai:
  services:
    segmentation:
      url: ${SEGMENTATION_API_URL}
    inpainting:
      url: ${INPAINTING_API_URL}
    tryon:
      url: ${TRYON_API_URL}
```

## 🔍 로그 확인

```bash
# 실시간 로그 보기
gcloud run services logs tail closetconnect-segmentation --region asia-northeast3

# 최근 로그 보기
gcloud run services logs read closetconnect-segmentation --region asia-northeast3 --limit 50
```

## 💰 비용 최적화

Cloud Run은 사용한 만큼만 비용이 청구됩니다:

- **콜드 스타트 감소**: `--min-instances 1` 옵션으로 최소 인스턴스 유지
- **메모리 조정**: 실제 사용량에 맞게 `--memory` 조정
- **타임아웃 최적화**: 불필요하게 긴 타임아웃 방지

```bash
# 최소 인스턴스 설정 (콜드 스타트 방지)
gcloud run services update closetconnect-segmentation \
  --region asia-northeast3 \
  --min-instances 1
```

## 🔐 보안 설정 (선택사항)

현재는 `--allow-unauthenticated`로 설정되어 있어 누구나 접근 가능합니다.
프로덕션 환경에서는 인증을 활성화하는 것을 권장합니다:

```bash
# 인증 활성화
gcloud run services update closetconnect-segmentation \
  --region asia-northeast3 \
  --no-allow-unauthenticated

# 서비스 계정에 권한 부여
gcloud run services add-iam-policy-binding closetconnect-segmentation \
  --region asia-northeast3 \
  --member="serviceAccount:YOUR-SERVICE-ACCOUNT@YOUR-PROJECT.iam.gserviceaccount.com" \
  --role="roles/run.invoker"
```

## 📊 모니터링

GCP Console에서 모니터링:
1. Cloud Console → Cloud Run
2. 서비스 선택
3. "지표" 탭에서 다음 확인:
   - 요청 수
   - 응답 시간
   - 인스턴스 수
   - 오류율

## 🔄 업데이트 및 재배포

코드 수정 후 재배포:

```bash
# 전체 재배포
./deploy-cloudrun.sh

# 특정 서비스만 재배포
./deploy-single-service.sh segmentation
```

## ❌ 서비스 삭제

```bash
# 특정 서비스 삭제
gcloud run services delete closetconnect-segmentation --region asia-northeast3

# 모든 서비스 삭제
gcloud run services delete closetconnect-segmentation --region asia-northeast3
gcloud run services delete closetconnect-inpainting --region asia-northeast3
gcloud run services delete closetconnect-tryon --region asia-northeast3
```

## 🐛 트러블슈팅

### 1. 빌드 실패
```bash
# 로그 확인
gcloud builds list
gcloud builds log [BUILD_ID]
```

### 2. 메모리 부족
```bash
# 메모리 증가
gcloud run services update closetconnect-segmentation \
  --region asia-northeast3 \
  --memory 4Gi
```

### 3. 타임아웃
```bash
# 타임아웃 증가 (최대 3600초)
gcloud run services update closetconnect-segmentation \
  --region asia-northeast3 \
  --timeout 600
```

### 4. 모델 다운로드 시간 초과
- Dockerfile에서 모델을 미리 다운로드하도록 수정
- 또는 Google Cloud Storage에 모델을 저장하고 불러오기

## 📚 추가 리소스

- [Cloud Run 공식 문서](https://cloud.google.com/run/docs)
- [Cloud Run 가격 정책](https://cloud.google.com/run/pricing)
- [Cloud Run 할당량 및 한도](https://cloud.google.com/run/quotas)

## 🆘 도움이 필요한 경우

- [GCP 지원 포럼](https://cloud.google.com/support)
- [Stack Overflow - google-cloud-run](https://stackoverflow.com/questions/tagged/google-cloud-run)
