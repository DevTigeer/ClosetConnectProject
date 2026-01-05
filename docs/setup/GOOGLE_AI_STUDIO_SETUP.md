# Google AI Studio 설정 가이드 (Nano Banana 2.5)

ClosetConnect 프로젝트에서 **Google AI Studio**의 **Gemini 2.5 Flash Image (Nano Banana)** 를 사용하여 잘린 의류 이미지를 자연스럽게 확장하는 방법을 안내합니다.

## 📌 Google AI Studio란?

- Google의 무료 AI 개발 플랫폼
- API 키만으로 간단하게 사용 가능
- **Nano Banana (Gemini 2.5 Flash Image)**: 이미지 생성/편집 모델
- **무료 할당량**: 하루 1,500 요청

## 🔑 1단계: Google AI Studio API 키 발급

### 1.1. Google AI Studio 접속

**URL**: https://aistudio.google.com/apikey

### 1.2. API 키 생성

1. **Google 계정으로 로그인**
2. 왼쪽 메뉴에서 **"Get API key"** 클릭
3. **"Create API key"** 버튼 클릭
4. 두 가지 옵션 중 선택:
   - **"Create API key in new project"** (권장 - 처음 사용하는 경우)
   - **"Create API key in existing project"** (기존 GCP 프로젝트가 있는 경우)
5. API 키가 생성되면 **복사**하여 안전한 곳에 보관

### 1.3. API 키 형식

```
AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
```

⚠️ **주의**: API 키는 비밀 정보이므로 GitHub에 커밋하지 마세요!

## ⚙️ 2단계: 프로젝트 환경변수 설정

### 2.1. .env 파일 생성

```bash
cd /Users/grail/Documents/ClosetConnectProject
cp .env.example .env
```

### 2.2. .env 파일 편집

`.env` 파일을 열고 다음 값을 입력:

```bash
# ===================================
# GOOGLE AI STUDIO 설정
# ===================================
GOOGLE_API_KEY=AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
```

발급받은 API 키를 `GOOGLE_API_KEY`에 입력하세요.

## 🐍 3단계: Python 의존성 설치

### 3.1. aiModel 디렉토리로 이동

```bash
cd aiModel
```

### 3.2. 필요한 패키지 설치

```bash
pip install -r requirements.txt
```

주요 설치 항목:
- `google-generativeai>=0.8.0` - Google AI Studio SDK
- `pillow>=10.1.0` - 이미지 처리
- 기타 AI 모델 라이브러리

## 🧪 4단계: 테스트

### 4.1. Python 인터프리터로 테스트

```python
import os
from dotenv import load_dotenv
from services.imagen_service import GoogleAIImagenService
from PIL import Image

# 환경변수 로드
load_dotenv()

# Imagen 서비스 초기화
service = GoogleAIImagenService()
print("✅ 서비스 초기화 성공!")

# 테스트 이미지 로드 (예시)
# test_image = Image.open("path/to/test_image.png")
# expanded = service.expand_image(test_image, expand_pixels=50)
# expanded.save("expanded_result.png")
```

### 4.2. Worker 실행

```bash
python cloth_processing_worker.py
```

성공 메시지:
```
✅ Google AI Studio (Nano Banana) 서비스 초기화 완료
   모델: gemini-2.5-flash-image
```

## 📊 5단계: 전체 시스템 통합 확인

### 5.1. Spring Boot 애플리케이션 실행

```bash
cd /Users/grail/Documents/ClosetConnectProject
./gradlew bootRun
```

### 5.2. RabbitMQ 확인

Python Worker가 RabbitMQ에서 메시지를 받고 처리하는지 확인:

```bash
# Worker 로그 확인
tail -f aiModel/worker.log
```

### 5.3. 처리 파이프라인 동작 확인

1. Spring Boot API로 이미지 업로드
2. RabbitMQ → Python Worker 전달
3. **배경 제거** (0-33%)
4. **의류 분할** (33-66%)
5. **Nano Banana 이미지 확장** (66-70%) ⭐ NEW!
6. **인페인팅** (70-95%)
7. 결과 반환

## 💰 비용 및 제한사항

### 무료 할당량 (Google AI Studio)

- **하루 1,500 요청**
- **분당 15 요청**
- 개발/프로토타입에 적합

### 프로덕션 사용

더 많은 요청이 필요한 경우:

1. **Vertex AI로 마이그레이션** (GCP 프로젝트 필요)
2. **Google Cloud 결제 활성화**
3. 가격: 이미지당 약 $0.039 (1,290 토큰)

## 🔧 문제 해결

### Q1. "API key not valid" 오류

**해결방법**:
- API 키가 올바르게 `.env` 파일에 설정되었는지 확인
- API 키에 공백이나 줄바꿈이 없는지 확인
- https://aistudio.google.com/apikey 에서 API 키 재확인

### Q2. "Quota exceeded" 오류

**해결방법**:
- 하루 1,500 요청 제한 초과
- 24시간 후 다시 시도하거나
- Vertex AI로 업그레이드

### Q3. "Model not found" 오류

**해결방법**:
- `google-generativeai` 패키지 버전 확인: `pip show google-generativeai`
- 최소 버전 0.8.0 이상 필요: `pip install --upgrade google-generativeai`

### Q4. Imagen 서비스 비활성화 상태

Worker 로그에 다음 메시지가 표시되는 경우:
```
⚠️  Google AI Imagen 초기화 실패
```

**해결방법**:
1. `.env` 파일에 `GOOGLE_API_KEY` 설정 확인
2. `dotenv` 패키지 설치 확인: `pip install python-dotenv`
3. 환경변수 로드 확인: `load_dotenv()`가 호출되는지

## 📚 추가 리소스

- **Google AI Studio**: https://aistudio.google.com/
- **API 키 관리**: https://aistudio.google.com/apikey
- **Nano Banana 문서**: https://ai.google.dev/gemini-api/docs/image-generation
- **Gemini API 문서**: https://ai.google.dev/gemini-api/docs
- **가격 정보**: https://ai.google.dev/pricing

## ✅ 완료!

이제 ClosetConnect 프로젝트에서 **Nano Banana (Gemini 2.5 Flash Image)** 를 사용하여 의류 이미지를 자연스럽게 확장할 수 있습니다!

궁금한 점이 있으면 [Google AI Studio 커뮤니티](https://discuss.ai.google.dev/)에 문의하세요.
