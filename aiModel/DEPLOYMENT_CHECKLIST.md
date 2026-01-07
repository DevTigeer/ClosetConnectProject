# Cloud Run 배포 체크리스트

3개의 독립적인 서비스를 Cloud Run에 배포하는 가이드입니다.

---

## 🎯 서비스 1: Segmentation API (의상 세그멘테이션)

### 📋 설정값

| 항목 | 값 |
|------|-----|
| **서비스 이름** | `closetconnect-segmentation` |
| **GitHub 브랜치** | `main` |
| **Dockerfile 경로** | `aiModel/Dockerfile.segmentation` |
| **빌드 컨텍스트** | `aiModel` |
| **리전** | `asia-northeast3` (서울) |
| **컨테이너 포트** | `8002` |
| **메모리** | `2 GiB` |
| **CPU** | `2` |
| **타임아웃** | `300` (5분) |
| **인증** | ✅ 인증되지 않은 호출 허용 |

### 📝 환경변수
```
PORT=8002
PYTHONUNBUFFERED=1
```

### ✅ 배포 후 확인
```
https://closetconnect-segmentation-xxx.a.run.app/health
```

---

## 🎯 서비스 2: Inpainting API (이미지 복원)

### 📋 설정값

| 항목 | 값 |
|------|-----|
| **서비스 이름** | `closetconnect-inpainting` |
| **GitHub 브랜치** | `main` |
| **Dockerfile 경로** | `aiModel/Dockerfile.inpainting` |
| **빌드 컨텍스트** | `aiModel` |
| **리전** | `asia-northeast3` (서울) |
| **컨테이너 포트** | `8003` |
| **메모리** | `2 GiB` |
| **CPU** | `2` |
| **타임아웃** | `300` (5분) |
| **인증** | ✅ 인증되지 않은 호출 허용 |

### 📝 환경변수
```
PORT=8003
PYTHONUNBUFFERED=1
```

### ✅ 배포 후 확인
```
https://closetconnect-inpainting-xxx.a.run.app/health
```

---

## 🎯 서비스 3: Try-on API (가상 착용)

### 📋 설정값

| 항목 | 값 |
|------|-----|
| **서비스 이름** | `closetconnect-tryon` |
| **GitHub 브랜치** | `main` |
| **Dockerfile 경로** | `aiModel/Dockerfile.tryon` |
| **빌드 컨텍스트** | `aiModel` |
| **리전** | `asia-northeast3` (서울) |
| **컨테이너 포트** | `5001` |
| **메모리** | `2 GiB` |
| **CPU** | `2` |
| **타임아웃** | `300` (5분) |
| **인증** | ✅ 인증되지 않은 호출 허용 |

### 📝 환경변수
```
PORT=5001
PYTHONUNBUFFERED=1
GOOGLE_API_KEY=your-gemini-api-key-here
```

⚠️ **중요**: `GOOGLE_API_KEY`는 Secret Manager 사용 권장

### ✅ 배포 후 확인
```
https://closetconnect-tryon-xxx.a.run.app/health
```

---

## 🔗 Railway 환경변수 설정

3개 서비스 모두 배포 완료 후, Railway에서 다음 환경변수 추가:

```bash
AI_SEGMENTATION_URL=https://closetconnect-segmentation-xxx.a.run.app
AI_INPAINTING_URL=https://closetconnect-inpainting-xxx.a.run.app
AI_TRYON_URL=https://closetconnect-tryon-xxx.a.run.app
```

---

## 📝 배포 순서

### Step 1: Segmentation API 배포 (지금 하고 계신 것)
1. ✅ Cloud Run > "서비스 만들기"
2. ✅ "소스 리포지토리에서 지속적으로 배포" 선택
3. ✅ GitHub 연결
4. ✅ 위의 "서비스 1" 설정값 입력
5. ✅ "만들기" 클릭
6. ⏳ 빌드 완료 대기 (5-10분)
7. ✅ URL 복사 및 저장

### Step 2: Inpainting API 배포
1. 다시 "서비스 만들기" 클릭
2. 위의 "서비스 2" 설정값 입력
3. **주의**: Dockerfile 경로만 `aiModel/Dockerfile.inpainting`으로 변경
4. 나머지는 동일

### Step 3: Try-on API 배포
1. 다시 "서비스 만들기" 클릭
2. 위의 "서비스 3" 설정값 입력
3. **주의**: Dockerfile 경로를 `aiModel/Dockerfile.tryon`으로, 포트를 `5001`로 변경
4. 환경변수에 `GOOGLE_API_KEY` 추가

---

## 🐛 트러블슈팅

### 빌드 실패 시
- Dockerfile 경로 확인: `aiModel/Dockerfile.xxx`
- 빌드 컨텍스트 확인: `aiModel`
- Cloud Build 로그 확인

### 배포 성공했지만 서비스 오류
- 로그 탭에서 에러 확인
- 환경변수 `PORT` 설정 확인
- 메모리 2GiB → 4GiB로 증가

### 모델 로딩 실패
- 타임아웃 300 → 600으로 증가
- 메모리 4GiB로 증가
- CPU 4로 증가

---

## ✅ 최종 체크리스트

배포 완료 후:
- [ ] Segmentation API 헬스체크 성공
- [ ] Inpainting API 헬스체크 성공
- [ ] Try-on API 헬스체크 성공
- [ ] Railway 환경변수 설정
- [ ] Spring Boot 재배포
- [ ] 전체 시스템 테스트
