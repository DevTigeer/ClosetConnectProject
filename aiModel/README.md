# AI Model Server

ClosetConnect의 AI 기능을 제공하는 Python 백엔드 서버입니다.

## 주요 기능

- **의류 세그멘테이션**: Hugging Face 모델을 사용한 의류 영역 분리 및 카테고리 분류
- **이미지 인페인팅**: Stable Diffusion 기반 배경 복원
- **이미지 확장**: Google Imagen API를 활용한 옷 영역 확장
- **가상 피팅**: Gemini API를 활용한 AI 가상 착용
- **비동기 처리**: RabbitMQ 워커를 통한 백그라운드 작업

## 디렉토리 구조

```
aiModel/
├── src/
│   ├── api/              # FastAPI 엔드포인트
│   │   ├── cloth_segmentation_api.py
│   │   └── inpainting_api.py
│   ├── services/         # 비즈니스 로직
│   │   ├── imagen_service.py
│   │   └── tryon/
│   ├── worker/           # RabbitMQ 워커
│   │   └── cloth_processing_worker.py
│   ├── models/           # AI 모델 설정
│   └── utils/            # 유틸리티
│       └── clothing_restorer.py
├── tests/                # 테스트 코드
├── scripts/              # 유틸리티 스크립트
├── data/                 # 데이터 및 모델 파일
└── requirements.txt      # Python 의존성
```

## 설치 방법

### 1. Python 가상환경 생성

```bash
python3 -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate
```

### 2. 의존성 설치

```bash
pip install -r requirements.txt
```

### 3. 환경변수 설정

**중요**: AI 서버는 프로젝트 루트의 `.env` 파일을 자동으로 찾습니다.

```bash
# 프로젝트 루트로 이동
cd ..

# .env 파일이 없으면 생성
cp .env.example .env

# .env 파일 편집하여 API 키 설정
# GOOGLE_API_KEY=your-google-ai-api-key
```

또는 직접 환경변수로 설정:
```bash
export GOOGLE_API_KEY=your-google-ai-api-key
```

## 서버 실행

### 🚀 모든 서버 한 번에 실행 (권장)

**가장 간단한 방법**: 아래 명령어 하나로 4개의 서버를 모두 실행합니다.

```bash
# aiModel 디렉토리에서 실행
python start_all_servers.py
```

**특징**:
- ✅ 4개 서버 동시 실행 (Segmentation, Inpainting, Try-On, Worker)
- ✅ 실시간 컬러 로그 출력 (각 서버별 색상 구분)
- ✅ 로그 파일 자동 저장 (`logs/` 디렉토리)
- ✅ Ctrl+C로 모든 서버 한 번에 종료
- ✅ 프로세스 상태 자동 모니터링

**실행 예시**:
```
🚀 ClosetConnect AI Model Servers Launcher
======================================================================
✅ SEGMENTATION 시작됨 (PID: 12345)
✅ INPAINTING 시작됨 (PID: 12346)
✅ TRYON 시작됨 (PID: 12347)
✅ WORKER 시작됨 (PID: 12348)
======================================================================

[10:30:15] [SEGMENTATION] * Running on http://0.0.0.0:8002
[10:30:16] [INPAINTING] * Running on http://0.0.0.0:8003
[10:30:17] [TRYON] * Running on http://0.0.0.0:5001
[10:30:18] [WORKER] Connected to RabbitMQ
```

---

### 개별 서버 실행 (고급)

필요한 서버만 실행하거나 디버깅이 필요한 경우 아래 방법을 사용하세요.

#### Cloth Segmentation API (Port 8002)

의류 세그멘테이션 서비스

```bash
# aiModel 디렉토리에서 실행
python -m src.api.cloth_segmentation_api

# 또는 직접 실행
cd src/api && python cloth_segmentation_api.py
```

### Inpainting API (Port 8003)

이미지 인페인팅 (배경 복원) 서비스

```bash
# aiModel 디렉토리에서 실행
python -m src.api.inpainting_api

# 또는 직접 실행
cd src/api && python inpainting_api.py
```

### Outfit Try-On API (Port 5001)

가상 착용 시뮬레이션 서비스

```bash
# aiModel 디렉토리에서 실행
python -m src.api.outfit_tryon_api

# 또는 직접 실행
cd src/api && python outfit_tryon_api.py
```

### RabbitMQ Worker

비동기 의류 처리 워커

```bash
# RabbitMQ가 실행 중이어야 함
# aiModel 디렉토리에서 실행
python -m src.worker.cloth_processing_worker

# 또는 직접 실행
cd src/worker && python cloth_processing_worker.py
```

## API 엔드포인트

### Cloth Segmentation API

- `POST /segment` - 의류 세그멘테이션 실행
- `GET /health` - 헬스 체크

### Inpainting API

- `POST /inpaint` - 이미지 인페인팅 실행
- `GET /health` - 헬스 체크

## 모델 정보

### 의류 세그멘테이션

- **모델**: Hugging Face Cloth Segmentation
- **입력**: 의류 이미지
- **출력**: 세그멘테이션 마스크 + 카테고리 분류

### 이미지 인페인팅

- **모델**: Stable Diffusion Inpainting
- **입력**: 원본 이미지 + 마스크
- **출력**: 배경이 복원된 이미지

### 이미지 확장

- **API**: Google Imagen (Nano Banana)
- **기능**: 옷 영역을 자연스럽게 확장
- **제한**: 무료 티어 하루 1,500 요청

### 가상 피팅

- **API**: Google Gemini 2.5 Flash
- **기능**: AI 기반 가상 착용 시뮬레이션

## 테스트

```bash
# 전체 테스트 실행
pytest tests/

# 특정 테스트 파일
pytest tests/test_segmentation.py
```

## 트러블슈팅

### CUDA 관련 오류

```bash
# CPU 전용 PyTorch 설치
pip install torch torchvision --index-url https://download.pytorch.org/whl/cpu
```

### Google API 키 오류

- Google AI Studio에서 API 키 발급: https://aistudio.google.com/apikey
- .env 파일에 GOOGLE_API_KEY 설정 확인

### RabbitMQ 연결 오류

```bash
# macOS
brew services start rabbitmq

# Linux
sudo systemctl start rabbitmq-server

# Docker
docker run -d -p 5672:5672 -p 15672:15672 rabbitmq:management
```

## 성능 최적화

- GPU 사용 시 CUDA 설치 권장
- 배치 처리로 다중 이미지 동시 처리
- Redis 캐싱으로 반복 요청 최적화 (선택사항)

## 다른 환경으로 이전하기

프로젝트를 다른 PC로 옮길 때 필요한 작업:

### 1. Git 클론 또는 파일 복사

```bash
git clone <repository-url>
cd ClosetConnectProject
```

### 2. 환경변수 설정

```bash
# .env 파일 생성
cp .env.example .env

# .env 파일을 편집하여 API 키 입력
# - GOOGLE_API_KEY: Google AI Studio에서 발급
# - TOSS_CLIENT_KEY, TOSS_SECRET_KEY: 토스페이먼츠에서 발급
# - DB_PASSWORD: MariaDB 비밀번호
```

### 3. Python 가상환경 및 의존성 설치

```bash
cd aiModel

# 가상환경 생성
python3 -m venv venv

# 가상환경 활성화
source venv/bin/activate  # Windows: venv\Scripts\activate

# 의존성 설치
pip install -r requirements.txt
```

### 4. RabbitMQ 설치 및 실행

```bash
# macOS
brew install rabbitmq
brew services start rabbitmq

# Ubuntu/Debian
sudo apt-get install rabbitmq-server
sudo systemctl start rabbitmq-server

# Windows (Chocolatey)
choco install rabbitmq
```

### 5. MariaDB 설정

```bash
# 데이터베이스 생성
mysql -u root -p
CREATE DATABASE closetConnectProject;
```

### 6. 서버 실행

```bash
# AI 모델 서버 (aiModel 디렉토리에서)
python3 start_all_servers.py

# Spring Boot 백엔드 (프로젝트 루트에서)
./gradlew bootRun
```

### 주의사항

- `.env` 파일은 Git에 포함되지 않으므로 **반드시 새로 생성**해야 합니다
- API 키는 각 서비스에서 **새로 발급**받거나 기존 키를 복사해야 합니다
- `venv/` 디렉토리도 Git에 포함되지 않으므로 **재생성** 필요
- 대용량 모델 파일은 자동으로 다운로드되거나 별도로 복사 필요

## 라이선스

MIT License
