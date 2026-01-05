# ClosetConnect

> AI 기반 스마트 옷장 관리 및 패션 커뮤니티 플랫폼

## 📌 프로젝트 소개

ClosetConnect는 사용자의 옷장을 디지털화하고, AI 기술을 활용하여 스타일링 추천, 가상 피팅, 날씨 기반 코디 제안 등을 제공하는 종합 패션 플랫폼입니다.

### 주요 기능

- 🎨 **AI 의류 세그멘테이션**: 사진에서 자동으로 옷 영역 분리 및 카테고리 분류
- 👔 **가상 피팅**: Gemini API를 활용한 AI 가상 착용 시뮬레이션
- 🌤️ **날씨 기반 코디 추천**: 실시간 날씨 정보를 반영한 맞춤형 스타일링 제안
- 🛍️ **마켓플레이스**: 중고 의류 거래 플랫폼 (Toss Payments 연동)
- 💬 **커뮤니티**: 패션 관련 게시판 및 소셜 기능
- 📱 **OOTD 공유**: 오늘의 코디 업로드 및 공유

## 🏗️ 아키텍처

```
ClosetConnectProject/
├── backend/          # Spring Boot REST API
├── frontend/         # React 프론트엔드
├── aiModel/          # Python AI 서버 (FastAPI)
├── docs/             # 프로젝트 문서
└── examples/         # 사용 예제
```

### 기술 스택

**Backend (Java/Spring)**
- Spring Boot 3.5.7
- Spring Security + JWT
- Spring Data JPA
- RabbitMQ (비동기 메시징)
- MariaDB

**Frontend (React)**
- React 18
- React Router
- Axios

**AI Model Server (Python)**
- FastAPI / Flask
- Google AI Studio (Gemini, Imagen)
- Hugging Face Transformers
- OpenCV, PIL
- RabbitMQ (워커)

**External APIs**
- Google Gemini API (가상 피팅)
- Google Imagen API (이미지 확장)
- 기상청 API (날씨 정보)
- Toss Payments (결제)

## 🚀 빠른 시작

### 사전 요구사항

- Java 17+
- Node.js 16+
- Python 3.9+
- MariaDB 10.6+
- RabbitMQ 3.9+

### 1. 환경 설정

```bash
# 환경변수 파일 생성
cp .env.example .env

# 필수 API 키 설정 (.env 파일 편집)
# - GOOGLE_AI_API_KEY
# - TOSS_CLIENT_KEY
# - TOSS_SECRET_KEY
```

### 2. 백엔드 실행

```bash
# 데이터베이스 설정 (MariaDB)
mysql -u root -p < db/schema.sql

# RabbitMQ 시작
brew services start rabbitmq  # macOS
# 또는
sudo systemctl start rabbitmq-server  # Linux

# Spring Boot 실행
./gradlew bootRun
```

백엔드 서버: http://localhost:8080

### 3. AI 모델 서버 실행

```bash
cd aiModel

# Python 가상환경 생성 및 활성화
python3 -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# 의존성 설치
pip install -r requirements.txt

# 🚀 모든 서버 한 번에 실행 (권장)
python start_all_servers.py
```

**실시간 컬러 로그**로 4개 서버의 상태를 한눈에 확인할 수 있습니다.
- Cloth Segmentation API (Port 8002)
- Inpainting API (Port 8003)
- Outfit Try-On API (Port 5001)
- RabbitMQ Worker

개별 서버 실행 방법은 [aiModel/README.md](aiModel/README.md)를 참고하세요.

### 4. 프론트엔드 실행

```bash
cd frontend/ClosetConnectProject

# 의존성 설치
npm install

# 개발 서버 실행
npm start
```

프론트엔드 서버: http://localhost:3000

## 📚 문서

- [설정 가이드](docs/setup/)
  - [Google AI Studio 설정](docs/setup/GOOGLE_AI_STUDIO_SETUP.md)
  - [RabbitMQ 설정](docs/setup/RABBITMQ_SETUP_GUIDE.md)
  - [Rembg 통합 가이드](docs/setup/REMBG_INTEGRATION_GUIDE.md)
  - [Toss Payments 설정](docs/setup/TOSS_PAYMENT_SETUP.md)

- [사용 가이드](docs/guides/)
  - [의류 추천 시스템](docs/guides/CLOTHING_RECOMMENDATION_README.md)
  - [마켓플레이스 빠른 시작](docs/guides/MARKET_QUICK_START.md)
  - [마켓플레이스 사용 가이드](docs/guides/MARKET_USAGE_GUIDE.md)
  - [날씨 기능 가이드](docs/guides/WEATHER_README.md)
  - [프론트엔드 개발 가이드](docs/guides/FRONTEND_PROGRESS_GUIDE.md)

- [개발 문서](docs/development/)
  - [프로젝트 가이드라인 (Claude)](docs/development/CLAUDE.md)

## 🔧 개발 환경 설정

### 테스트 실행

```bash
# 전체 테스트 (139개)
./gradlew test

# 특정 테스트 클래스
./gradlew test --tests "com.tigger.closetconnectproject.Auth.AuthControllerTest"

# 테스트 리포트
open build/reports/tests/test/index.html
```

### 코드 스타일

- Java: Spring Boot 표준 컨벤션
- Python: PEP 8
- JavaScript/React: ESLint + Prettier

## 🗂️ 프로젝트 구조

### Backend (Spring Boot)

```
src/main/java/com/tigger/closetconnectproject/
├── Closet/        # 옷장 관리 도메인
├── User/          # 사용자 관리
├── Post/          # 게시글 시스템
├── Community/     # 커뮤니티 게시판
├── Market/        # 마켓플레이스
├── Weather/       # 날씨 기능
├── Common/        # 공통 기능 (Auth, Config, Exception)
└── Security/      # Spring Security 설정
```

### AI Model Server (Python)

```
aiModel/
├── src/
│   ├── api/       # FastAPI 엔드포인트
│   ├── services/  # 비즈니스 로직
│   ├── worker/    # RabbitMQ 워커
│   └── models/    # AI 모델 설정
└── tests/         # 테스트 코드
```

## 🔐 보안

- JWT 기반 인증/인가
- BCrypt 패스워드 해싱
- CORS 정책 적용
- 환경변수로 민감 정보 관리
- SQL Injection 방지 (JPA/Hibernate)

## 📊 API 문서

Spring Boot 서버 실행 후:
- Swagger UI: http://localhost:8080/swagger-ui.html
- API Docs: http://localhost:8080/v3/api-docs

## 🤝 기여 방법

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 라이선스

이 프로젝트는 MIT 라이선스를 따릅니다.

## 👥 팀

- Backend: Spring Boot
- Frontend: React
- AI Model: Python/FastAPI
- DevOps: Docker, RabbitMQ

## 📞 문의

프로젝트 관련 문의사항이나 버그 리포트는 GitHub Issues를 이용해주세요.

---

⭐ 이 프로젝트가 도움이 되셨다면 Star를 눌러주세요!
