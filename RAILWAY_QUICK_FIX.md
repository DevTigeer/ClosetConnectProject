# Railway 배포 에러 해결 가이드

## 현재 발생한 에러

```
Socket fail to connect to localhost. Connection refused
Unable to determine Dialect without JDBC metadata
```

**원인**: Railway에서 데이터베이스 환경 변수가 설정되지 않아 `localhost`에 연결 시도

## 즉시 해결 방법

### 1단계: Railway에서 MySQL 서비스 추가

1. **Railway 프로젝트 대시보드 열기**
   - https://railway.app

2. **MySQL 데이터베이스 추가**
   - 프로젝트 캔버스에서 `+ New` 클릭
   - `Database` → `Add MySQL` 선택
   - 자동으로 MySQL 서비스가 생성됩니다

3. **환경 변수 자동 생성 확인**
   MySQL 서비스를 추가하면 다음 변수들이 자동으로 생성됩니다:
   - `MYSQLHOST` (예: mysql.railway.internal)
   - `MYSQLPORT` (예: 3306)
   - `MYSQLDATABASE` (예: railway)
   - `MYSQLUSER` (예: root)
   - `MYSQLPASSWORD` (자동 생성된 비밀번호)

### 2단계: Spring Boot 서비스에 환경 변수 설정

1. **Spring Boot 서비스 선택**
   - 프로젝트 캔버스에서 Spring Boot 애플리케이션 서비스 클릭

2. **Variables 탭 이동**
   - 상단 메뉴에서 `Variables` 클릭

3. **필수 환경 변수 추가**

   **Raw Editor 사용 (권장):**
   - `RAW Editor` 버튼 클릭
   - 아래 내용을 복사해서 붙여넣기:

   ```bash
   # Spring Profile
   SPRING_PROFILES_ACTIVE=prod

   # Database Connection (MySQL 서비스 참조)
   DB_URL=jdbc:mariadb://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}
   DB_USERNAME=${{MySQL.MYSQLUSER}}
   DB_PASSWORD=${{MySQL.MYSQLPASSWORD}}

   # JWT Secret (반드시 변경!)
   JWT_SECRET=production-secret-key-change-this-to-very-long-random-string-at-least-256-bits

   # Toss Payments (나중에 실제 키로 변경)
   TOSS_CLIENT_KEY=test_gck_docs_Ovk5rk1EwkEbP0W43n07xlzm
   TOSS_SECRET_KEY=test_gsk_docs_OaPz8L5KdmQXkzRz3y47BMw6

   # RabbitMQ는 일단 비활성화 (선택사항)
   RABBITMQ_ENABLED=false
   ```

   **중요**:
   - `${{MySQL.MYSQLHOST}}` 형식으로 다른 서비스의 환경 변수를 참조합니다
   - `MySQL`은 Railway에서 생성한 MySQL 서비스의 이름입니다
   - 서비스 이름이 다르면 해당 이름으로 변경하세요

### 3단계: 재배포

환경 변수를 저장하면 자동으로 재배포가 시작됩니다.

**배포 로그 확인:**
1. `Deployments` 탭 클릭
2. 최신 배포 선택
3. `View Logs` 클릭
4. 다음 메시지가 나타나면 성공:
   ```
   HikariPool-1 - Start completed.
   Started ClosetConnectProjectApplication
   ```

### 4단계: 헬스 체크 확인

1. **공개 URL 확인**
   - `Settings` → `Networking`
   - `Generate Domain` 클릭 (아직 안 했다면)

2. **헬스 체크 테스트**
   ```bash
   curl https://your-app.railway.app/actuator/health
   ```

   응답:
   ```json
   {"status":"UP"}
   ```

## Railway 서비스 이름 확인 방법

MySQL 서비스 이름이 `MySQL`이 아닐 수 있습니다:

1. **서비스 이름 확인**
   - 프로젝트 캔버스에서 MySQL 서비스 클릭
   - 상단에 표시된 서비스 이름 확인 (예: `mysql`, `database` 등)

2. **환경 변수 참조 업데이트**
   - 서비스 이름이 `database`라면:
     ```
     DB_URL=jdbc:mariadb://${{database.MYSQLHOST}}:${{database.MYSQLPORT}}/${{database.MYSQLDATABASE}}
     ```

## RabbitMQ 추가 (선택사항)

옷 이미지 처리 기능을 사용하려면 RabbitMQ가 필요합니다:

1. **RabbitMQ 서비스 추가**
   - `+ New` → `Database` → `Add RabbitMQ`

2. **환경 변수 추가**
   ```bash
   RABBITMQ_ENABLED=true
   RABBITMQ_HOST=${{RabbitMQ.RABBITMQ_HOST}}
   RABBITMQ_PORT=5672
   RABBITMQ_USERNAME=${{RabbitMQ.RABBITMQ_DEFAULT_USER}}
   RABBITMQ_PASSWORD=${{RabbitMQ.RABBITMQ_DEFAULT_PASS}}
   ```

## 문제가 계속되면

### 1. 로그 확인
Railway 대시보드 → `Deployments` → 최신 배포 → `View Logs`

### 2. 환경 변수 확인
`Variables` 탭에서 모든 변수가 올바르게 설정되었는지 확인

### 3. 수동 재배포
`Deployments` → `Deploy` 버튼 클릭

### 4. 데이터베이스 연결 테스트
MySQL 서비스의 `Connect` 탭에서 연결 정보 확인

## 체크리스트

- [ ] MySQL 서비스 추가됨
- [ ] `SPRING_PROFILES_ACTIVE=prod` 설정됨
- [ ] `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` 설정됨
- [ ] `JWT_SECRET` 변경됨 (기본값 사용 금지!)
- [ ] 배포 성공 (로그 확인)
- [ ] 헬스 체크 통과
- [ ] 도메인 생성 완료

## 다음 단계

배포가 성공하면:
1. 데이터베이스 마이그레이션 확인
2. API 테스트
3. 프론트엔드 연동
4. Python AI 서비스 배포 (별도)
