# 환경별 설정 가이드

## 환경 변수 파일 구조

```
.env.local          # 로컬 개발용 (gitignore에 포함)
.env.production     # AWS 운영용 (gitignore에 포함)
.env.example        # 예시 파일 (git에 커밋)
```

## 1. 로컬 개발 환경

### 설정
`.env.local` 파일이 자동으로 사용됩니다:
```env
VITE_API_BASE=http://localhost:8080
```

### 실행 명령어
```bash
# 기본 개발 서버 (자동으로 .env.local 사용)
npm run dev

# 또는 명시적으로 local 모드 지정
npm run dev:local
```

### 접속
- http://localhost:5173

---

## 2. AWS 운영 환경

### 설정
`.env.production` 파일:
```env
VITE_API_BASE=http://3.237.47.9
```

### 빌드 명령어
```bash
# 운영 환경용 빌드
npm run build:production
```

### 빌드 결과물
- `dist/` 디렉토리에 생성됨
- 이 파일들을 AWS S3, CloudFront, EC2 등에 배포

---

## 3. 환경 변수 우선순위 (Vite)

Vite는 다음 순서로 환경 변수를 로드합니다:

1. `.env.[mode].local` (최우선, gitignore)
2. `.env.[mode]` (환경별)
3. `.env.local` (모든 환경, gitignore)
4. `.env` (기본값)

### 예시
- `npm run dev` → `.env.local` → `.env` 순서로 로드
- `npm run build:production` → `.env.production.local` → `.env.production` → `.env` 순서로 로드

---

## 4. AWS 배포 시나리오

### Option 1: S3 + CloudFront (정적 호스팅)
```bash
# 1. 운영 환경 빌드
npm run build:production

# 2. S3에 업로드
aws s3 sync dist/ s3://your-bucket-name --delete

# 3. CloudFront 캐시 무효화 (선택사항)
aws cloudfront create-invalidation --distribution-id YOUR_DIST_ID --paths "/*"
```

### Option 2: EC2 + Nginx
```bash
# 1. 운영 환경 빌드
npm run build:production

# 2. EC2에 배포
scp -r dist/* ec2-user@your-ec2-ip:/var/www/html/

# 3. Nginx 설정 (React Router용)
# location / {
#   try_files $uri /index.html;
# }
```

### Option 3: Amplify (자동 배포)
- GitHub 연결 시 자동으로 빌드 및 배포
- Build settings에서 `npm run build:production` 지정

---

## 5. 팀원들을 위한 초기 설정

새로운 팀원이 프로젝트를 클론했을 때:

```bash
# 1. 의존성 설치
npm install

# 2. 로컬 환경 변수 파일 생성
cp .env.example .env.local

# 3. .env.local 파일 수정 (필요시)
# VITE_API_BASE를 로컬 백엔드 주소로 설정

# 4. 개발 서버 실행
npm run dev
```

---

## 6. 보안 주의사항

⚠️ **중요**: 다음 파일들은 절대 git에 커밋하지 마세요:
- `.env.local` (이미 gitignore에 포함)
- `.env.production` (이미 gitignore에 포함)

✅ **git에 커밋해야 하는 파일**:
- `.env.example` (실제 값 없이 키만 포함)

---

## 7. 환경 변수 사용법 (코드에서)

```javascript
// src/services/api.js 에서의 사용 예시
const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080';

console.log(API_BASE);
// 로컬: http://localhost:8080
// 운영: http://3.237.47.9
```

⚠️ **주의**: Vite에서는 환경 변수가 빌드 시점에 번들에 포함됩니다.
- 민감한 정보(API 키, 비밀번호)는 **절대** 프론트엔드 환경 변수에 넣지 마세요
- 백엔드에서 처리하세요

---

## 8. 문제 해결

### 환경 변수가 적용되지 않을 때
```bash
# 개발 서버 재시작 필요
# Ctrl+C로 종료 후
npm run dev
```

### 빌드 시 환경 변수 확인
```bash
# 빌드 후 dist/assets/*.js 파일에서 확인 가능
grep -r "VITE_API_BASE" dist/
```

### 다른 AWS IP로 변경
```bash
# .env.production 파일 수정
VITE_API_BASE=http://NEW_IP

# 다시 빌드
npm run build:production
```
