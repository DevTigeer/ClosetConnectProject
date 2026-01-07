# Cloud Run 웹 콘솔 배포 가이드

GCP 웹 콘솔을 통해 Python AI 서비스를 Cloud Run에 배포하는 방법입니다.

## 📋 사전 준비

1. **Google Cloud Platform 계정**
   - https://console.cloud.google.com/ 접속
   - 새 프로젝트 생성 또는 기존 프로젝트 선택
   - 결제 계정 연결 (무료 크레딧 $300 제공)

2. **GitHub 저장소 준비**
   - 코드를 GitHub에 푸시해야 합니다
   - 또는 로컬 소스를 직접 업로드할 수도 있습니다

---

## 🚀 방법 1: GitHub 연동 배포 (권장)

### 1단계: Cloud Run 페이지 이동

1. GCP 콘솔(https://console.cloud.google.com/) 접속
2. 왼쪽 메뉴에서 **"Cloud Run"** 클릭
3. 처음이라면 **"Cloud Run API 사용 설정"** 클릭

### 2단계: 서비스 생성 (Segmentation API 예시)

1. **"서비스 만들기"** 버튼 클릭

2. **소스 선택**
   - "소스 리포지토리에서 지속적으로 배포" 선택
   - **"CLOUD BUILD 설정"** 클릭

3. **리포지토리 연결**
   - "GitHub에 연결" 선택
   - GitHub 계정 인증
   - 저장소 선택: `ClosetConnectProject`
   - 브랜치: `main` (또는 사용 중인 브랜치)
   - **"다음"** 클릭

4. **빌드 구성**
   - 빌드 유형: **"Dockerfile"** 선택
   - Dockerfile 경로: `/aiModel/Dockerfile.segmentation`
   - 빌드 컨텍스트 디렉토리: `/aiModel`
   - **"저장"** 클릭

5. **서비스 설정**
   - 서비스 이름: `closetconnect-segmentation`
   - 리전: **"asia-northeast3 (서울)"** (또는 가까운 리전)
   - CPU 할당: "요청을 처리하는 동안만 CPU 할당"
   - 인증: **"인증되지 않은 호출 허용"** 체크 ✅

6. **컨테이너, 네트워킹, 보안 설정**

   **컨테이너 탭:**
   - 컨테이너 포트: `8002`
   - 메모리: **2 GiB**
   - CPU: **2**
   - 최대 요청 수: `80`
   - 요청 제한 시간: `300` (5분)

   **변수 및 보안 비밀 탭:**
   - 환경 변수 추가:
     - `PORT`: `8002`
     - `PYTHONUNBUFFERED`: `1`

   **연결 탭:**
   - 최소 인스턴스 수: `0` (비용 절감) 또는 `1` (콜드 스타트 방지)
   - 최대 인스턴스 수: `10`

7. **"만들기"** 클릭

### 3단계: 배포 대기

- 빌드 및 배포에 5-10분 정도 소요됩니다
- 진행 상황을 "수정 버전" 탭에서 확인할 수 있습니다
- 완료되면 서비스 URL이 표시됩니다 (예: `https://closetconnect-segmentation-xxx.a.run.app`)

### 4단계: 다른 서비스들도 동일하게 배포

**Inpainting API:**
- 서비스 이름: `closetconnect-inpainting`
- Dockerfile: `/aiModel/Dockerfile.inpainting`
- 포트: `8003`
- 환경변수 `PORT`: `8003`

**Try-on API:**
- 서비스 이름: `closetconnect-tryon`
- Dockerfile: `/aiModel/Dockerfile.tryon`
- 포트: `5001`
- 환경변수:
  - `PORT`: `5001`
  - `GOOGLE_API_KEY`: (Gemini API 키 - Secret Manager 사용 권장)

---

## 🚀 방법 2: 로컬 소스 직접 업로드

GitHub 없이 로컬에서 직접 배포하는 방법입니다.

### 1단계: Google Cloud Shell 실행

1. GCP 콘솔 우측 상단의 **"Cloud Shell 활성화"** 버튼 클릭 (터미널 아이콘)
2. Cloud Shell 터미널이 하단에 열립니다

### 2단계: 코드 업로드

**방법 A: GitHub에서 클론**
```bash
git clone https://github.com/YOUR_USERNAME/ClosetConnectProject.git
cd ClosetConnectProject/aiModel
```

**방법 B: 로컬 파일 업로드**
1. Cloud Shell 우측 상단의 **"⋮" (더보기) > "파일 업로드"** 클릭
2. `aiModel` 폴더를 압축(zip)하여 업로드
3. Cloud Shell에서:
```bash
unzip aiModel.zip
cd aiModel
```

### 3단계: Cloud Shell에서 배포

#### Segmentation API 배포
```bash
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

#### Inpainting API 배포
```bash
gcloud run deploy closetconnect-inpainting \
  --source . \
  --platform managed \
  --region asia-northeast3 \
  --allow-unauthenticated \
  --memory 2Gi \
  --cpu 2 \
  --timeout 300 \
  --port 8003 \
  --dockerfile Dockerfile.inpainting
```

#### Try-on API 배포
```bash
gcloud run deploy closetconnect-tryon \
  --source . \
  --platform managed \
  --region asia-northeast3 \
  --allow-unauthenticated \
  --memory 2Gi \
  --cpu 2 \
  --timeout 300 \
  --port 5001 \
  --dockerfile Dockerfile.tryon
```

---

## 🚀 방법 3: Artifact Registry + 웹 콘솔

Docker 이미지를 먼저 빌드한 후 배포하는 방법입니다.

### 1단계: Cloud Shell에서 이미지 빌드

```bash
# 프로젝트 ID 설정
export PROJECT_ID=your-project-id

# Artifact Registry 리포지토리 생성
gcloud artifacts repositories create closetconnect-ai \
  --repository-format=docker \
  --location=asia-northeast3

# Docker 인증 설정
gcloud auth configure-docker asia-northeast3-docker.pkg.dev

# 이미지 빌드 및 푸시 (Segmentation API 예시)
cd aiModel

docker build -t asia-northeast3-docker.pkg.dev/${PROJECT_ID}/closetconnect-ai/segmentation:latest \
  -f Dockerfile.segmentation .

docker push asia-northeast3-docker.pkg.dev/${PROJECT_ID}/closetconnect-ai/segmentation:latest
```

### 2단계: 웹 콘솔에서 배포

1. **Cloud Run** 페이지로 이동
2. **"서비스 만들기"** 클릭
3. **"기존 컨테이너 이미지에서 서비스 배포"** 선택
4. **"컨테이너 이미지 URL 선택"** 클릭
   - Artifact Registry에서 방금 푸시한 이미지 선택
5. 나머지는 **방법 1**의 5-7단계와 동일

---

## 🔍 배포 확인

### 서비스 URL 확인
1. Cloud Run 페이지에서 서비스 클릭
2. 상단에 **서비스 URL** 표시 (예: `https://closetconnect-segmentation-xxx.a.run.app`)

### 헬스 체크
브라우저나 curl로 확인:
```
https://closetconnect-segmentation-xxx.a.run.app/health
```

예상 응답:
```json
{
  "status": "healthy",
  "model_loaded": true,
  "device": "cpu"
}
```

---

## 🔗 Railway Backend 연동

배포 완료 후 Railway에서 환경변수 설정:

1. Railway 프로젝트 페이지 접속
2. Spring Boot 서비스 선택
3. **"Variables"** 탭 클릭
4. 새 환경변수 추가:

```
AI_SEGMENTATION_URL=https://closetconnect-segmentation-xxx.a.run.app
AI_INPAINTING_URL=https://closetconnect-inpainting-xxx.a.run.app
AI_TRYON_URL=https://closetconnect-tryon-xxx.a.run.app
```

5. **"Deploy"** 클릭하여 재배포

---

## 📊 모니터링

### 로그 확인
1. Cloud Run 서비스 페이지에서 **"로그"** 탭 클릭
2. 실시간 로그 및 오류 확인 가능

### 지표 확인
1. **"지표"** 탭에서 다음 확인:
   - 요청 수
   - 응답 시간
   - 오류율
   - 활성 인스턴스 수

---

## 💰 비용 관리

### 무료 한도 (매월)
- 요청 200만 건
- CPU 시간 360,000 vCPU-초
- 메모리 시간 180,000 GiB-초
- 네트워크 송신 1GB

### 비용 절감 팁
1. **최소 인스턴스를 0으로 설정** (사용하지 않을 때 비용 없음)
2. **메모리를 필요한 만큼만 할당** (테스트 후 조정)
3. **CPU를 "요청 처리 시에만"으로 설정**

### 비용 알림 설정
1. **결제** 메뉴에서 **"예산 및 알림"** 클릭
2. 예산 생성 (예: 월 $10)
3. 알림 임계값 설정 (50%, 90%, 100%)

---

## 🔐 보안 강화 (선택사항)

### Secret Manager 사용 (API 키 관리)

1. **Secret Manager API 사용 설정**
   - 검색창에 "Secret Manager" 입력
   - API 사용 설정

2. **Secret 생성**
   - Secret Manager 페이지에서 **"Secret 만들기"** 클릭
   - 이름: `google-api-key`
   - Secret 값: Gemini API 키 입력
   - **"Secret 만들기"** 클릭

3. **Cloud Run에서 Secret 사용**
   - Cloud Run 서비스 편집
   - **"변수 및 보안 비밀"** 탭
   - **"참조"** 선택
   - Secret: `google-api-key`
   - 환경 변수 이름: `GOOGLE_API_KEY`

---

## 🔄 업데이트 및 재배포

### GitHub 연동 배포의 경우
- 코드를 푸시하면 **자동으로 재배포**됩니다
- Cloud Build 트리거에서 자동 배포 설정 가능

### 수동 재배포
1. Cloud Run 서비스 페이지
2. **"새 수정 버전 편집 및 배포"** 클릭
3. 설정 변경 후 **"배포"** 클릭

---

## ❌ 서비스 삭제

1. Cloud Run 서비스 페이지
2. 서비스 선택 (체크박스)
3. 상단의 **"삭제"** 버튼 클릭
4. 확인

---

## 🐛 문제 해결

### 빌드 실패
- **로그 확인**: Cloud Build > 기록 > 실패한 빌드 클릭
- **Dockerfile 경로 확인**: 경로가 정확한지 확인
- **메모리 부족**: Cloud Build 설정에서 메모리 증가

### 배포는 성공했지만 서비스 오류
- **로그 확인**: Cloud Run > 서비스 > 로그 탭
- **포트 설정 확인**: 환경변수 `PORT`와 컨테이너 포트 일치 여부
- **메모리 부족**: 서비스 편집에서 메모리 2GiB → 4GiB로 증가

### 모델 로딩 실패
- **타임아웃 증가**: 요청 제한 시간을 300초 → 600초로 증가
- **메모리 증가**: 2GiB → 4GiB 또는 8GiB
- **CPU 증가**: 2 → 4

### CORS 오류
- 이미 코드에서 설정했지만, 추가 도메인이 필요한 경우:
  - `src/api/cloth_segmentation_api.py` 등에서 `allow_origins` 수정
  - 재배포

---

## 📚 추가 참고 자료

- [Cloud Run 공식 문서](https://cloud.google.com/run/docs)
- [Cloud Run 빠른 시작](https://cloud.google.com/run/docs/quickstarts)
- [Cloud Run 가격](https://cloud.google.com/run/pricing)
- [Cloud Run 할당량](https://cloud.google.com/run/quotas)

---

## ✅ 체크리스트

배포 전:
- [ ] GCP 계정 생성 및 프로젝트 설정
- [ ] 결제 계정 연결
- [ ] Cloud Run API 사용 설정
- [ ] GitHub 저장소에 코드 푸시 (방법 1 사용 시)

배포:
- [ ] Segmentation API 배포
- [ ] Inpainting API 배포
- [ ] Try-on API 배포
- [ ] 각 서비스 URL 확인

연동:
- [ ] Railway에 환경변수 설정
- [ ] Spring Boot 재배포
- [ ] 전체 시스템 테스트

모니터링:
- [ ] 각 서비스 로그 확인
- [ ] 헬스 체크 성공 확인
- [ ] 비용 알림 설정
