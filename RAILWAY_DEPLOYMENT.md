# Railway 배포 가이드

## 1. Railway 프로젝트 설정

### 1.1 GitHub 연동
1. Railway 대시보드에서 "New Project" 클릭
2. "Deploy from GitHub repo" 선택
3. 저장소 선택: `ClosetConnectProject`

### 1.2 필수 서비스 추가

Railway 프로젝트에 다음 서비스들을 추가해야 합니다:

#### MySQL/MariaDB
1. 프로젝트 캔버스에서 "+ New" 클릭
2. "Database" → "Add MySQL" 선택
3. 자동으로 생성된 환경 변수 확인:
   - `MYSQLHOST`
   - `MYSQLPORT`
   - `MYSQLDATABASE`
   - `MYSQLUSER`
   - `MYSQLPASSWORD`

#### RabbitMQ
1. 프로젝트 캔버스에서 "+ New" 클릭
2. "Database" → "Add RabbitMQ" 선택
3. 생성된 환경 변수 확인

## 2. 환경 변수 설정

Spring Boot 서비스의 "Variables" 탭에서 다음 환경 변수를 추가하세요:

### 필수 환경 변수

```bash
# Spring Profile
SPRING_PROFILES_ACTIVE=prod

# Database (MySQL 서비스와 연결)
DB_URL=jdbc:mariadb://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}
DB_USERNAME=${{MySQL.MYSQLUSER}}
DB_PASSWORD=${{MySQL.MYSQLPASSWORD}}

# RabbitMQ (RabbitMQ 서비스와 연결)
RABBITMQ_HOST=${{RabbitMQ.RABBITMQ_HOST}}
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=${{RabbitMQ.RABBITMQ_DEFAULT_USER}}
RABBITMQ_PASSWORD=${{RabbitMQ.RABBITMQ_DEFAULT_PASS}}

# JWT Secret (반드시 변경하세요!)
JWT_SECRET=your-production-secret-key-minimum-256-bits-long-please-change-this
JWT_TOKEN_VALIDITY=3600

# Toss Payments (프로덕션 키로 변경)
TOSS_CLIENT_KEY=test_gck_docs_Ovk5rk1EwkEbP0W43n07xlzm
TOSS_SECRET_KEY=test_gsk_docs_OaPz8L5KdmQXkzRz3y47BMw6
```

### 선택적 환경 변수 (Python 서비스가 있는 경우)

```bash
# Python AI 서비스들 (별도 배포한 경우에만)
REMBG_SERVER_URL=https://your-rembg-service.railway.app
CLOTH_SEGMENTATION_SERVER_URL=https://your-segmentation-service.railway.app
INPAINTING_SERVER_URL=https://your-inpainting-service.railway.app
TRYON_API_URL=https://your-tryon-service.railway.app
```

## 3. 배포 설정

### 3.1 Dockerfile 확인
Railway는 자동으로 `Dockerfile`을 감지합니다. 빌드 로그에서 다음 메시지를 확인하세요:
```
Using detected Dockerfile!
```

### 3.2 헬스 체크 설정
Railway 서비스 설정에서:
1. "Settings" → "Health Check" 섹션
2. Health Check Path: `/actuator/health`
3. Timeout: 60초 (첫 시작은 시간이 걸릴 수 있음)

### 3.3 도메인 설정
1. "Settings" → "Networking"
2. "Generate Domain" 클릭하여 공개 URL 생성
3. 생성된 URL을 `TOSS_PAYMENTS_SUCCESS_URL`, `TOSS_PAYMENTS_FAIL_URL`에 업데이트

## 4. 배포 실행

### 4.1 자동 배포
- GitHub에 푸시하면 자동으로 배포됩니다
- 빌드 로그를 확인하여 진행 상황 모니터링

### 4.2 수동 배포
1. Railway 대시보드에서 서비스 선택
2. "Deployments" 탭
3. "Deploy" 버튼 클릭

## 5. 배포 후 확인

### 5.1 헬스 체크
```bash
curl https://your-app.railway.app/actuator/health
```

응답 예시:
```json
{
  "status": "UP"
}
```

### 5.2 로그 확인
Railway 대시보드의 "Logs" 탭에서 실시간 로그 확인

### 5.3 데이터베이스 연결 확인
로그에서 다음과 같은 메시지 확인:
```
HikariPool-1 - Start completed.
```

## 6. 문제 해결

### 빌드 실패
- **원인**: Gradle 테스트 실패
- **해결**: Dockerfile이 `-x test` 옵션으로 빌드하는지 확인

### 데이터베이스 연결 실패
- **원인**: 환경 변수 미설정 또는 잘못된 값
- **해결**:
  1. Railway MySQL 서비스가 실행 중인지 확인
  2. 환경 변수가 올바르게 참조되는지 확인
  3. Reference 형식: `${{서비스명.변수명}}`

### RabbitMQ 연결 실패
- **원인**: RabbitMQ 서비스 미설치
- **해결**: Railway에 RabbitMQ 서비스 추가

### Out of Memory
- **원인**: Railway 무료 플랜의 메모리 제한
- **해결**:
  1. Dockerfile의 `-XX:MaxRAMPercentage=75.0` 조정
  2. Railway Pro 플랜으로 업그레이드

### 파일 업로드 문제
- **원인**: Railway의 임시 파일 시스템
- **해결**:
  - S3, Cloudinary 등 외부 스토리지 사용 권장
  - 또는 Railway Volume 사용

## 7. 프로덕션 체크리스트

- [ ] MySQL/MariaDB 서비스 추가 및 연결
- [ ] RabbitMQ 서비스 추가 및 연결
- [ ] JWT_SECRET을 안전한 랜덤 값으로 변경
- [ ] TOSS_CLIENT_KEY, TOSS_SECRET_KEY를 프로덕션 키로 변경
- [ ] SPRING_PROFILES_ACTIVE=prod 설정
- [ ] 도메인 생성 및 HTTPS 확인
- [ ] 헬스 체크 설정 및 작동 확인
- [ ] 로그 모니터링 설정
- [ ] 데이터베이스 백업 설정 (Railway Pro)

## 8. 참고 링크

- [Railway 공식 문서](https://docs.railway.com/)
- [Railway Dockerfile 가이드](https://docs.railway.com/guides/dockerfiles)
- [Toss Payments API 문서](https://docs.tosspayments.com/)
