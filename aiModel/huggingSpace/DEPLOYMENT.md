# 🚀 Hugging Face Spaces 배포 가이드

## 사전 준비

1. **Hugging Face 계정 생성**
   - https://huggingface.co/ 에서 회원가입
   - 무료 계정으로도 GPU 사용 가능 (제한적)

2. **Git 설정**
   ```bash
   git config --global user.email "your@email.com"
   git config --global user.name "Your Name"
   ```

## 배포 방법

### 방법 1: Web UI로 배포 (추천 - 가장 쉬움)

1. **새 Space 생성**
   - https://huggingface.co/new-space 접속
   - Space name: `background-removal` (또는 원하는 이름)
   - License: MIT
   - SDK: **Gradio**
   - Hardware: **CPU Basic** (나중에 GPU로 변경 가능)
   - Click "Create Space"

2. **파일 업로드**
   - Files 탭에서 "Upload files" 클릭
   - 다음 파일들을 드래그 앤 드롭:
     - `app.py`
     - `requirements.txt`
     - `README.md`
   - "Commit changes to main" 클릭

3. **GPU 활성화** (선택 - 성능 향상)
   - Settings → Hardware
   - **GPU T4** 선택 (무료 계정은 제한 있음)
   - "Confirm" 클릭

4. **배포 완료!**
   - 빌드가 완료되면 자동으로 실행됨
   - URL: `https://YOUR-USERNAME-background-removal.hf.space`

### 방법 2: Git으로 배포

1. **Git 저장소 클론**
   ```bash
   cd /Users/grail/Documents/ClosetConnectProject/aiModel/huggingSpace
   git init
   git remote add space https://huggingface.co/spaces/YOUR-USERNAME/background-removal
   ```

2. **파일 커밋 및 푸시**
   ```bash
   git add .
   git commit -m "Initial deployment"
   git push --force space main
   ```

3. **빌드 확인**
   - Hugging Face Space 페이지에서 빌드 로그 확인
   - 5-10분 소요 (모델 다운로드 포함)

## CloudRun Worker 연동

배포가 완료되면 CloudRun Worker에 환경변수를 설정하세요:

```bash
# CloudRun 환경변수 설정
REMBG_API_URL=https://YOUR-USERNAME-background-removal.hf.space
```

### Google Cloud Console에서 설정

1. Cloud Run 서비스 선택
2. "Edit & Deploy New Revision" 클릭
3. "Variables & Secrets" 탭
4. "Add Variable" 클릭:
   - Name: `REMBG_API_URL`
   - Value: `https://YOUR-USERNAME-background-removal.hf.space`
5. "Deploy" 클릭

## 테스트

### 1. Web UI 테스트

- Space URL 접속
- 이미지 업로드
- 배경 제거 결과 확인

### 2. API 테스트

```python
import requests
from PIL import Image
import io
import base64

API_URL = "https://YOUR-USERNAME-background-removal.hf.space/api/predict"

# 테스트 이미지 업로드
with open("test_image.png", "rb") as f:
    response = requests.post(
        API_URL,
        files={"data": f},
        timeout=60
    )

# 결과 확인
result = response.json()
print(result)
```

### 3. Worker 통합 테스트

```bash
# CloudRun Worker 로그 확인
gcloud logging read "resource.type=cloud_run_revision" --limit 50

# "Background removed (Hugging Face API)" 메시지 확인
```

## 모니터링

### Hugging Face Spaces Dashboard

- 사용량 확인: Settings → Usage
- 로그 확인: Logs 탭
- 성능 모니터링: 응답 시간, 에러율

### 예상 성능

| 하드웨어 | 첫 요청 | 이후 요청 |
|---------|--------|----------|
| CPU Basic | 20-30초 | 10-15초 |
| GPU T4 | 5-10초 | 1-3초 |

## 비용

- **CPU Basic**: 무료 (제한 있음)
- **GPU T4**: 무료 계정 - 주당 일정 시간 무료
- **Pro 계정** ($9/월): 무제한 GPU 사용

## 문제 해결

### 빌드 실패

- Logs 탭에서 에러 확인
- requirements.txt 버전 확인
- Python 버전 호환성 확인

### API 응답 느림

- GPU로 업그레이드 (Settings → Hardware)
- Cold start 방지: Settings → "Always on" 활성화 (Pro 계정 필요)

### 연결 실패

- Space URL 확인 (https로 시작)
- Space가 "Running" 상태인지 확인
- 네트워크 방화벽 확인

## 참고 자료

- [Hugging Face Spaces 공식 문서](https://huggingface.co/docs/hub/spaces)
- [Gradio 공식 문서](https://www.gradio.app/docs/)
- [rembg GitHub](https://github.com/danielgatis/rembg)
