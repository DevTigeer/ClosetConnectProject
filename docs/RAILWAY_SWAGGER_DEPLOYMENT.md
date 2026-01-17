# Railway에서 Swagger 사용하기

## 📋 목차
1. [설정 완료 항목](#설정-완료-항목)
2. [Railway 환경변수 설정](#railway-환경변수-설정)
3. [배포하기](#배포하기)
4. [Swagger 접속하기](#swagger-접속하기)
5. [문제 해결](#문제-해결)

---

## 설정 완료 항목

### ✅ 로컬 코드 설정 완료

다음 항목들이 자동으로 설정되었습니다:

1. **build.gradle** - SpringDoc OpenAPI 의존성 추가
   ```gradle
   implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'
   ```

2. **SwaggerConfig.java** - Swagger 설정 클래스 생성
   - JWT 인증 지원
   - 서버 URL 환경변수로 설정 가능
   - 위치: `src/main/java/com/tigger/closetconnectproject/Common/Config/SwaggerConfig.java`

3. **application.properties** - Swagger 설정 추가
   - Swagger UI 경로: `/swagger-ui/index.html`
   - OpenAPI JSON: `/v3/api-docs`

4. **application-prod.properties** - 프로덕션 환경 설정
   - Railway URL 환경변수로 주입

5. **SecurityConfig.java** - Swagger 경로 허용 (이미 설정되어 있었음)
   ```java
   .requestMatchers("/api/v1/auth/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
   ```

---

## Railway 환경변수 설정

Railway 대시보드에서 다음 환경변수를 설정하세요:

### 1. Railway 프로젝트 접속
```
https://railway.app
```

### 2. 환경변수 추가

프로젝트 → Variables 탭에서 다음 변수들을 추가:

#### 필수 환경변수

```bash
# Spring Boot 프로파일 (프로덕션 환경)
SPRING_PROFILES_ACTIVE=prod

# Swagger 서버 URL (Railway 배포 URL로 변경)
SWAGGER_SERVER_URL=https://your-app-name.up.railway.app

# Swagger 서버 설명
SWAGGER_SERVER_DESCRIPTION=Railway Production Server
```

#### Railway 자동 제공 환경변수

Railway는 다음 환경변수를 자동으로 제공합니다:
- `PORT` - 애플리케이션 포트 (자동 할당)
- `RAILWAY_PUBLIC_DOMAIN` - 공개 도메인 (예: your-app.up.railway.app)

### 3. Railway 배포 URL 확인

Railway 대시보드에서:
1. **Settings** → **Domains** 섹션 확인
2. 생성된 Railway 도메인 복사 (예: `https://closetconnect-production.up.railway.app`)
3. `SWAGGER_SERVER_URL` 환경변수에 이 값 입력

### 4. 환경변수 설정 예시

```bash
# 데이터베이스
DB_URL=jdbc:mariadb://your-db-host:3306/closetconnect
DB_USERNAME=your_db_user
DB_PASSWORD=your_db_password

# JWT
JWT_SECRET=your-very-long-secret-key-min-256-bits
JWT_TOKEN_VALIDITY=3600

# Swagger
SWAGGER_SERVER_URL=https://closetconnect-production.up.railway.app
SWAGGER_SERVER_DESCRIPTION=Railway Production Server

# RabbitMQ
RABBITMQ_ENABLED=true
RABBITMQ_HOST=your-rabbitmq-host
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=your_rabbitmq_user
RABBITMQ_PASSWORD=your_rabbitmq_password

# Spring Profile
SPRING_PROFILES_ACTIVE=prod
```

---

## 배포하기

### 방법 1: Git Push를 통한 자동 배포 (권장)

Railway는 GitHub 저장소와 연동하여 자동 배포합니다:

```bash
# 변경사항 커밋
git add .
git commit -m "Add Swagger configuration"

# 메인 브랜치로 푸시
git push origin main
```

Railway가 자동으로:
1. 코드 변경 감지
2. 빌드 시작
3. 배포 완료

### 방법 2: Railway CLI를 통한 배포

```bash
# Railway CLI 설치 (처음 한 번만)
npm install -g @railway/cli

# Railway 로그인
railway login

# 프로젝트 연결
railway link

# 배포
railway up
```

### 배포 확인

Railway 대시보드에서:
1. **Deployments** 탭 확인
2. 빌드 로그 확인
3. 배포 상태가 "Success"인지 확인

---

## Swagger 접속하기

### 1. Swagger UI 접속

배포가 완료되면 다음 URL로 접속:

```
https://your-app-name.up.railway.app/swagger-ui/index.html
```

예시:
```
https://closetconnect-production.up.railway.app/swagger-ui/index.html
```

### 2. OpenAPI JSON 스펙 확인

```
https://your-app-name.up.railway.app/v3/api-docs
```

### 3. JWT 인증하기

Swagger UI에서 API 테스트하려면:

1. **로그인 API 실행**
   - `POST /api/v1/auth/login` 엔드포인트 찾기
   - Request Body 입력:
     ```json
     {
       "email": "test@test.com",
       "password": "password123"
     }
     ```
   - "Execute" 버튼 클릭
   - Response에서 `accessToken` 값 복사

2. **Authorize 버튼 클릭**
   - Swagger UI 우측 상단 "Authorize" 🔓 버튼 클릭
   - Value 필드에 토큰만 붙여넣기 (Bearer 접두사 제외)
   - "Authorize" 버튼 클릭
   - 창 닫기

3. **인증 필요한 API 테스트**
   - 이제 모든 API 요청에 자동으로 JWT 토큰이 포함됨
   - 원하는 API 엔드포인트 선택하여 테스트

### 4. Swagger UI 주요 기능

#### 서버 선택
- Swagger UI 상단에서 서버 선택 가능
- Production Server (Railway)와 Local Development Server 전환 가능

#### API 그룹
- **Auth**: 회원가입/로그인
- **Cloth**: 옷장 관리
- **Outfit**: 코디 관리
- **Ootd**: OOTD 공유
- **Community**: 게시판
- **Post**: 게시글/댓글
- **Market**: 마켓 (중고 거래)
- **Payment**: 결제 (토스페이먼츠)
- **Chat**: 채팅
- **Weather**: 날씨 기반 추천

#### 요청/응답 확인
- 각 API 엔드포인트를 펼치면:
  - Parameters: 요청 파라미터
  - Request body: 요청 본문 (POST/PUT)
  - Responses: 응답 코드별 예시
  - "Try it out" 버튼으로 직접 테스트 가능

---

## 문제 해결

### Swagger UI가 404 에러

**원인**: Swagger 경로가 SecurityConfig에서 허용되지 않음

**해결**:
1. SecurityConfig.java 확인:
   ```java
   .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
   ```
2. 이미 설정되어 있으므로 Railway 재배포 시도

### Swagger UI는 보이지만 API 호출이 401 에러

**원인**: JWT 토큰이 설정되지 않음

**해결**:
1. 로그인 API로 토큰 발급
2. "Authorize" 버튼으로 토큰 입력

### 서버 URL이 localhost로 표시됨

**원인**: `SWAGGER_SERVER_URL` 환경변수가 설정되지 않음

**해결**:
1. Railway 대시보드 → Variables
2. `SWAGGER_SERVER_URL` 환경변수 추가
3. 값: `https://your-app-name.up.railway.app`
4. 재배포

### CORS 에러

**원인**: CORS 설정 문제

**확인**:
- SecurityConfig.java의 `corsConfigurationSource()` 확인
- Railway 도메인이 허용 목록에 있는지 확인

**해결**:
```java
// SecurityConfig.java
config.addAllowedOriginPattern("https://*.railway.app");
```

### 배포 후 Swagger가 안 보임

**체크리스트**:
1. ✅ build.gradle에 의존성 추가되었는지 확인
2. ✅ SwaggerConfig.java 파일 존재하는지 확인
3. ✅ application-prod.properties에 Swagger 설정 있는지 확인
4. ✅ Railway 환경변수 `SPRING_PROFILES_ACTIVE=prod` 설정되었는지 확인
5. ✅ Railway 배포 로그에서 에러 확인

---

## 추가 설정 (선택사항)

### API 문서화 개선

컨트롤러에 Swagger 어노테이션 추가:

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Tag(name = "Cloth", description = "옷장 관리 API")
@RestController
@RequestMapping("/api/v1/cloth")
public class ClothController {

    @Operation(
        summary = "옷 목록 조회",
        description = "사용자의 옷 목록을 페이징하여 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    public Page<ClothResponse> list(...) {
        // ...
    }
}
```

상세한 문서화 가이드는 `SWAGGER_SETUP_GUIDE.md` 참조

### Swagger UI 커스터마이징

application-prod.properties에 추가:

```properties
# Swagger UI 커스터마이징
springdoc.swagger-ui.doc-expansion=none
springdoc.swagger-ui.default-models-expand-depth=-1
springdoc.swagger-ui.try-it-out-enabled=true
```

---

## 참고 자료

- [SpringDoc OpenAPI 공식 문서](https://springdoc.org/)
- [Railway 배포 가이드](https://docs.railway.app/)
- [프로젝트 Swagger 설정 가이드](./SWAGGER_SETUP_GUIDE.md)

---

## 요약

### Railway에서 Swagger 사용 순서

1. ✅ **로컬 설정 완료** (이미 완료됨)
   - build.gradle 의존성 추가
   - SwaggerConfig.java 생성
   - application.properties 설정

2. **Railway 환경변수 설정**
   ```bash
   SWAGGER_SERVER_URL=https://your-app.up.railway.app
   SPRING_PROFILES_ACTIVE=prod
   ```

3. **Git Push로 배포**
   ```bash
   git add .
   git commit -m "Add Swagger"
   git push origin main
   ```

4. **Swagger UI 접속**
   ```
   https://your-app.up.railway.app/swagger-ui/index.html
   ```

5. **JWT 인증 후 API 테스트**
   - 로그인 → 토큰 복사 → Authorize 버튼 → 테스트

---

## 작성일
2026-01-17
