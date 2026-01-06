# 환경 설정 Quick Start

## 빠른 사용법

### 로컬 개발
```bash
npm run dev
```
자동으로 `.env.local` 파일 사용 → `http://localhost:8080` 연결

### AWS 운영 빌드
```bash
npm run build:production
```
자동으로 `.env.production` 파일 사용 → `http://3.237.47.9` 연결

---

## 현재 설정된 환경 파일

| 파일 | 용도 | API 주소 | Git 관리 |
|------|------|----------|----------|
| `.env.local` | 로컬 개발 | `http://localhost:8080` | ❌ gitignore |
| `.env.production` | AWS 운영 | `http://3.237.47.9` | ❌ gitignore |
| `.env.example` | 예시/템플릿 | `http://localhost:8080` | ✅ 커밋됨 |

---

## 처음 시작하는 개발자

```bash
# 1. 의존성 설치
npm install

# 2. 환경 변수 파일 복사
cp .env.example .env.local

# 3. 개발 서버 실행
npm run dev
```

---

## AWS 배포 시

```bash
# 1. 빌드
npm run build:production

# 2. dist/ 폴더를 AWS에 업로드
# - S3 + CloudFront
# - EC2 + Nginx
# - Amplify
```

자세한 내용은 `ENVIRONMENT_SETUP.md` 참고
