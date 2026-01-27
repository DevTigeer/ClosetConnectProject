# ClosetConnect 프로젝트 면접 질문 리스트

> 이 문서는 ClosetConnect 프로젝트에서 나올 수 있는 기술 면접 질문들을 정리한 것입니다.

---

## 목차
1. [Spring Boot / Java 기초](#1-spring-boot--java-기초)
2. [Spring Security / JWT 인증](#2-spring-security--jwt-인증)
3. [JPA / Hibernate / 데이터베이스](#3-jpa--hibernate--데이터베이스)
4. [REST API 설계](#4-rest-api-설계)
5. [비동기 처리 / 메시징 (RabbitMQ)](#5-비동기-처리--메시징-rabbitmq)
6. [WebSocket / 실시간 통신](#6-websocket--실시간-통신)
7. [이미지 처리 / AI 파이프라인](#7-이미지-처리--ai-파이프라인)
8. [결제 시스템 (Toss Payments)](#8-결제-시스템-toss-payments)
9. [DevOps / 배포](#9-devops--배포)
10. [테스트](#10-테스트)
11. [디자인 패턴 / 아키텍처](#11-디자인-패턴--아키텍처)
12. [트러블슈팅 / 문제 해결](#12-트러블슈팅--문제-해결)
13. [프론트엔드 (React)](#13-프론트엔드-react)

---

## 1. Spring Boot / Java 기초

### Q1-1. Spring Boot 3.x를 선택한 이유는 무엇인가요?
**예상 답변 키워드:**
- Java 17 LTS 지원 (최소 요구사항)
- Jakarta EE 9+ 마이그레이션 (javax → jakarta 패키지)
- GraalVM 네이티브 이미지 지원 개선
- 향상된 관찰성(Observability) - Micrometer, Tracing
- Spring Security 6.x의 새로운 보안 기능

### Q1-2. `@RequiredArgsConstructor`를 사용한 이유와 장점은 무엇인가요?
**예상 답변 키워드:**
- Lombok을 통한 생성자 주입 자동화
- final 필드에 대한 생성자 자동 생성
- 불변성(Immutability) 보장
- 테스트 용이성 (Mock 주입 가능)
- `@Autowired` 대비 순환 참조 방지

### Q1-3. `BaseTimeEntity`를 구현한 이유와 JPA Auditing이란?
**예상 답변 키워드:**
```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {
    @CreatedDate
    private LocalDateTime createdDate;

    @LastModifiedDate
    private LocalDateTime modifiedDate;
}
```
- 생성일/수정일 자동 관리
- `@EnableJpaAuditing` 설정 필요
- 코드 중복 제거 및 일관성 유지

### Q1-4. Spring Boot에서 프로파일(Profile)을 어떻게 관리하셨나요?
**예상 답변 키워드:**
- `application.properties` (기본)
- `application-prod.properties` (운영)
- `spring.profiles.active` 환경변수
- 환경별 설정 분리 (DB URL, API Key 등)

### Q1-5. 환경변수를 어떻게 관리하셨나요?
**예상 답변 키워드:**
- `.env.example` 파일로 템플릿 제공
- `${ENV_VAR:default_value}` 문법
- Railway/Vercel 환경변수 설정
- 민감 정보 보호 (JWT Secret, API Key)

---

## 2. Spring Security / JWT 인증

### Q2-1. JWT를 사용한 인증 방식의 장단점은 무엇인가요?
**장점:**
- Stateless - 서버 확장 용이
- 마이크로서비스 간 인증 공유 가능
- 세션 저장소 불필요

**단점:**
- 토큰 탈취 시 무효화 어려움
- 토큰 크기가 상대적으로 큼
- Refresh Token 전략 필요

### Q2-2. `JwtTokenProvider` 클래스의 역할과 구현 방식을 설명해주세요.
**예상 답변 키워드:**
- HS256 알고리즘 사용
- Secret Key 최소 32자 이상 (256bit)
- Claims: subject(email), role, userId
- 토큰 생성/검증/파싱 메서드
- `io.jsonwebtoken.jjwt` 라이브러리

### Q2-3. `JwtAuthenticationFilter`의 동작 방식을 설명해주세요.
**예상 답변 키워드:**
```java
@Override
protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain filterChain) {
    // 1. Authorization 헤더에서 토큰 추출
    // 2. 토큰 유효성 검증
    // 3. Authentication 객체 생성
    // 4. SecurityContextHolder에 저장
    // 5. 다음 필터로 전달
}
```

### Q2-4. BCrypt 패스워드 인코딩의 특징은 무엇인가요?
**예상 답변 키워드:**
- 단방향 해시 함수
- Salt 자동 생성
- Work Factor (Cost) 조절 가능
- 동일 평문 → 다른 해시값
- Rainbow Table 공격 방어

### Q2-5. CORS 설정을 어떻게 구성하셨나요?
**예상 답변 키워드:**
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    // localhost:3000, 5173 (개발)
    // *.vercel.app (프론트엔드)
    // *.railway.app (백엔드)
}
```
- Preflight 요청 처리
- 허용 Origin, Method, Header 설정
- Credentials 허용 여부

### Q2-6. 역할 기반 접근 제어(RBAC)를 어떻게 구현하셨나요?
**예상 답변 키워드:**
- `UserRole.ROLE_USER`, `UserRole.ROLE_ADMIN`
- `@PreAuthorize("hasRole('ADMIN')")`
- SecurityConfig에서 URL 패턴별 권한 설정
- 관리자 전용 엔드포인트 분리

---

## 3. JPA / Hibernate / 데이터베이스

### Q3-1. 엔티티 연관관계를 어떻게 설계하셨나요?
**예상 답변 키워드:**
```
Users ←(1:N)→ Cloth
Users ←(1:N)→ Order (buyer/seller)
Users ←(1:N)→ Ootd
MarketProduct ←(1:N)→ Order
```
- `@ManyToOne`, `@OneToMany`
- 양방향 vs 단방향 관계
- mappedBy 사용

### Q3-2. N+1 문제가 무엇이고 어떻게 해결하셨나요?
**예상 답변 키워드:**
- 문제: 1개 쿼리 + N개 추가 쿼리 발생
- 해결 방법:
  - `@EntityGraph` 사용
  - `fetch join` (JPQL)
  - `@BatchSize` 설정
  - `FetchType.LAZY` + 필요시 명시적 로딩

### Q3-3. `FetchType.LAZY`와 `FetchType.EAGER`의 차이점은?
**예상 답변 키워드:**
- LAZY: 지연 로딩, 필요시 쿼리
- EAGER: 즉시 로딩, JOIN으로 함께 조회
- `@ManyToOne` 기본값: EAGER
- `@OneToMany` 기본값: LAZY
- 성능 최적화를 위해 LAZY 권장

### Q3-4. `ddl-auto=update` 설정의 위험성은?
**예상 답변 키워드:**
- 개발 환경에서만 사용 권장
- 운영 환경: `validate` 또는 `none`
- 스키마 변경 자동 적용 → 데이터 손실 가능
- Flyway/Liquibase로 마이그레이션 관리 권장

### Q3-5. Soft Delete를 구현한 이유와 방법은?
**예상 답변 키워드:**
```java
@Enumerated(EnumType.STRING)
private UserStatus status; // NORMAL, SUSPENDED, DELETED
```
- 데이터 복구 가능
- 참조 무결성 유지
- `@Where(clause = "status != 'DELETED'")`
- 또는 서비스 레이어에서 필터링

### Q3-6. 트랜잭션 관리를 어떻게 하셨나요?
**예상 답변 키워드:**
- `@Transactional` 어노테이션
- 읽기 전용: `@Transactional(readOnly = true)`
- 전파(Propagation) 속성
- 롤백 조건 설정
- 서비스 레이어에서 트랜잭션 경계 설정

---

## 4. REST API 설계

### Q4-1. API 버저닝 전략은 무엇인가요?
**예상 답변 키워드:**
- URL Path 버저닝: `/api/v1/...`
- 하위 호환성 유지
- 메이저 버전 변경 시 새 경로
- Swagger 문서에 버전 명시

### Q4-2. RESTful API 설계 원칙을 어떻게 적용하셨나요?
**예상 답변 키워드:**
- 명사형 리소스 이름 (`/users`, `/cloth`)
- HTTP 메서드 의미 부여 (GET, POST, PUT, DELETE)
- 상태 코드 적절히 사용 (200, 201, 400, 401, 404)
- HATEOAS 고려 (링크 제공)

### Q4-3. DTO와 Entity를 분리한 이유는?
**예상 답변 키워드:**
- 계층 간 책임 분리
- API 스펙과 DB 스키마 독립
- 민감 정보 노출 방지 (password 등)
- API 변경에 유연하게 대응
- `@JsonIgnore` 남발 방지

### Q4-4. Swagger/OpenAPI 문서화는 어떻게 구성하셨나요?
**예상 답변 키워드:**
```java
@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info().title("ClosetConnect API").version("v1.0.0"))
            .addSecurityItem(...)
            .components(...);
    }
}
```
- Springdoc OpenAPI 2.7.0
- JWT Bearer 토큰 설정
- 태그별 API 그룹화

### Q4-5. 에러 응답 형식은 어떻게 통일하셨나요?
**예상 답변 키워드:**
```java
@ExceptionHandler
public ResponseEntity<ErrorResponse> handleException(...) {
    // 일관된 에러 응답 형식
}
```
- Global Exception Handler
- HTTP 상태 코드 매핑
- 에러 메시지 구조화
- 커스텀 예외 클래스

---

## 5. 비동기 처리 / 메시징 (RabbitMQ)

### Q5-1. RabbitMQ를 사용한 이유와 장점은?
**예상 답변 키워드:**
- 비동기 처리로 응답 시간 단축
- 시스템 간 결합도 감소
- 작업 큐로 부하 분산
- 재시도 및 Dead Letter Queue
- 이미지 처리 같은 시간 소요 작업에 적합

### Q5-2. 메시지 큐 구조를 설명해주세요.
**예상 답변 키워드:**
```
Exchange: cloth.exchange (Direct)
    ├── cloth.processing.queue (Spring → Python)
    ├── cloth.result.queue (Python → Spring)
    └── cloth.progress.queue (진행상황)

Routing Key: cloth.processing, cloth.result, cloth.progress
```

### Q5-3. 메시지 발행/구독 패턴을 어떻게 구현하셨나요?
**예상 답변 키워드:**
```java
// Producer
@Component
public class ClothMessageProducer {
    private final RabbitTemplate rabbitTemplate;

    public void sendProcessingMessage(ClothProcessingMessage message) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }
}

// Consumer
@Component
public class ClothResultConsumer {
    @RabbitListener(queues = "cloth.result.queue")
    public void handleResult(ClothResultMessage message) {
        // 처리 결과 반영
    }
}
```

### Q5-4. 메시지 처리 실패 시 어떻게 대응하나요?
**예상 답변 키워드:**
- 재시도 정책 (Retry)
- Dead Letter Queue (DLQ)
- 에러 로깅 및 알림
- 수동 재처리 메커니즘
- `ProcessingStatus.FAILED` 상태 관리

### Q5-5. 이벤트 기반 아키텍처의 장단점은?
**장점:**
- 서비스 간 느슨한 결합
- 확장성 및 유연성
- 비동기 처리 가능

**단점:**
- 디버깅 복잡도 증가
- 이벤트 순서 보장 어려움
- 트랜잭션 관리 복잡

---

## 6. WebSocket / 실시간 통신

### Q6-1. WebSocket을 사용한 기능은 무엇인가요?
**예상 답변 키워드:**
- 마켓 1:1 채팅
- 이미지 처리 진행상황 실시간 알림
- 주문 상태 변경 알림

### Q6-2. STOMP 프로토콜을 사용한 이유는?
**예상 답변 키워드:**
- 메시지 형식 표준화
- Subscribe/Publish 패턴
- Spring과의 통합 용이
- 클라이언트 라이브러리 풍부 (SockJS)

### Q6-3. WebSocket 설정을 설명해주세요.
**예상 답변 키워드:**
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
```

### Q6-4. WebSocket 보안은 어떻게 처리하셨나요?
**예상 답변 키워드:**
- JWT 토큰 검증
- 연결 시 인증 처리
- 구독 권한 검증
- Origin 검증

---

## 7. 이미지 처리 / AI 파이프라인

### Q7-1. 이미지 처리 파이프라인의 흐름을 설명해주세요.
**예상 답변 키워드:**
```
1. 원본 이미지 업로드
      ↓
2. rembg (배경 제거)
      ↓
3. SegFormer (옷 세그멘테이션)
      ↓
4. Stable Diffusion (Inpainting)
      ↓
5. 최종 이미지 저장
```

### Q7-2. 외부 AI 서비스 호출은 어떻게 구현하셨나요?
**예상 답변 키워드:**
```java
// WebClient 사용 (비동기)
@Configuration
public class WebClientConfig {
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
            .baseUrl(aiServerUrl)
            .build();
    }
}
```
- WebClient (Spring WebFlux)
- 비동기 처리
- 타임아웃 설정
- 에러 핸들링

### Q7-3. 이미지 처리 상태 관리는 어떻게 하셨나요?
**예상 답변 키워드:**
```java
public enum ProcessingStatus {
    PENDING,    // 대기 중
    PROCESSING, // 처리 중
    COMPLETED,  // 완료
    FAILED      // 실패
}

// 진행률 표시
private Integer progressPercentage; // 0-100
```

### Q7-4. 파일 업로드 제한은 어떻게 설정하셨나요?
**예상 답변 키워드:**
```properties
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=12MB
```
- MultipartFile 검증
- 파일 타입 검증 (이미지만 허용)
- 파일 크기 제한

### Q7-5. Google AI (Gemini, Imagen) 연동은 어떻게 하셨나요?
**예상 답변 키워드:**
- Gemini: 가상 피팅 (Try-On)
- Imagen: 이미지 확장
- API Key 환경변수 관리
- Python FastAPI 서버에서 처리

---

## 8. 결제 시스템 (Toss Payments)

### Q8-1. 결제 흐름을 설명해주세요.
**예상 답변 키워드:**
```
1. 주문 생성 (PAYMENT_PENDING)
      ↓
2. Toss 결제창 호출 (클라이언트)
      ↓
3. 결제 승인 요청 (/payments/confirm)
      ↓
4. Toss API 결제 승인
      ↓
5. 주문 상태 업데이트 (PAYMENT_PAID)
```

### Q8-2. 결제 승인 API 구현은 어떻게 하셨나요?
**예상 답변 키워드:**
```java
@PostMapping("/payments/confirm")
public ResponseEntity<?> confirmPayment(
    @RequestBody PaymentConfirmRequest request) {
    // 1. Toss API 호출 (paymentKey, orderId, amount)
    // 2. 응답 검증
    // 3. 주문 상태 업데이트
    // 4. 결과 반환
}
```

### Q8-3. 주문 상태 관리는 어떻게 하셨나요?
**예상 답변 키워드:**
```java
public enum OrderStatus {
    PAYMENT_PENDING,     // 결제 대기
    PAYMENT_PAID,        // 결제 완료
    SHIPPED,             // 배송 중
    DELIVERED,           // 배송 완료
    CONFIRMED,           // 구매 확정
    CANCELLED,           // 취소
    REFUNDED,            // 환불
    SETTLEMENT_RELEASED  // 정산 완료
}
```

### Q8-4. 결제 보안은 어떻게 처리하셨나요?
**예상 답변 키워드:**
- Secret Key 서버 사이드에서만 사용
- 환경변수로 키 관리
- 금액 위변조 검증
- 주문 ID 유니크 검증

### Q8-5. 결제 실패 시 처리는 어떻게 하나요?
**예상 답변 키워드:**
- 에러 코드별 분기 처리
- 사용자에게 적절한 메시지 전달
- 재시도 안내
- 로깅 및 모니터링

---

## 9. DevOps / 배포

### Q9-1. Docker 멀티스테이지 빌드를 사용한 이유는?
**예상 답변 키워드:**
```dockerfile
# Stage 1: Build
FROM gradle:8.5-jdk17-alpine AS builder
...

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
COPY --from=builder /app/build/libs/*.jar app.jar
```
- 이미지 크기 최소화
- 빌드 도구 제외
- 보안 강화

### Q9-2. Railway 배포 환경 설정을 설명해주세요.
**예상 답변 키워드:**
- 환경변수 설정 (DB, JWT, API Keys)
- 512MB 메모리 플랜
- JVM 메모리 최적화 (`-Xmx320m`)
- Health Check 설정
- 자동 배포 (GitHub 연동)

### Q9-3. 프론트엔드/백엔드 분리 배포의 장점은?
**예상 답변 키워드:**
- Frontend: Vercel (정적 호스팅)
- Backend: Railway (컨테이너)
- 독립적 스케일링
- 독립적 배포
- CDN 활용

### Q9-4. CORS 문제는 어떻게 해결하셨나요?
**예상 답변 키워드:**
- 백엔드 CORS 설정
- 허용 도메인 목록 관리
- Swagger 상대 URL 사용
- Preflight 요청 처리

### Q9-5. 로깅 및 모니터링은 어떻게 구성하셨나요?
**예상 답변 키워드:**
- SLF4J + Logback
- Hibernate SQL 로깅
- Railway 대시보드
- 에러 로깅 및 추적

---

## 10. 테스트

### Q10-1. 어떤 테스트를 작성하셨나요?
**예상 답변 키워드:**
- 단위 테스트 (JUnit 5)
- 통합 테스트 (Spring Boot Test)
- Mock 테스트 (Mockito)
- 컨트롤러 테스트 (MockMvc)

### Q10-2. Mock 테스트는 어떻게 작성하셨나요?
**예상 답변 키워드:**
```java
@ExtendWith(MockitoExtension.class)
class ClothServiceTest {
    @Mock
    private ClothRepository clothRepository;

    @InjectMocks
    private ClothService clothService;

    @Test
    void testCreateCloth() {
        when(clothRepository.save(any())).thenReturn(mockCloth);
        // ...
    }
}
```

### Q10-3. 통합 테스트 환경은 어떻게 구성하셨나요?
**예상 답변 키워드:**
```java
@SpringBootTest
@AutoConfigureMockMvc
class UsersControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void testLogin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());
    }
}
```

### Q10-4. 테스트 데이터는 어떻게 관리하셨나요?
**예상 답변 키워드:**
- `@BeforeEach`로 테스트 데이터 준비
- `@Transactional`로 롤백
- H2 인메모리 DB 사용 (테스트용)
- Test Fixtures

---

## 11. 디자인 패턴 / 아키텍처

### Q11-1. 계층형 아키텍처를 설명해주세요.
**예상 답변 키워드:**
```
Controller (Presentation Layer)
    ↓
Service (Business Layer)
    ↓
Repository (Data Access Layer)
    ↓
Entity (Domain Model)
```
- 관심사 분리
- 테스트 용이성
- 유지보수성

### Q11-2. 의존성 주입(DI)의 장점은 무엇인가요?
**예상 답변 키워드:**
- 결합도 감소
- 테스트 용이 (Mock 주입)
- 유연한 구현체 교체
- IoC (제어의 역전)

### Q11-3. Repository 패턴을 사용한 이유는?
**예상 답변 키워드:**
- 데이터 접근 로직 캡슐화
- 비즈니스 로직과 데이터 로직 분리
- 테스트 용이성
- Spring Data JPA 활용

### Q11-4. 이벤트 기반 설계를 어디에 적용하셨나요?
**예상 답변 키워드:**
```java
// 이벤트 발행
applicationEventPublisher.publishEvent(new ClothUploadedEvent(cloth));

// 이벤트 리스너
@EventListener
public void handleClothUploaded(ClothUploadedEvent event) {
    // 이미지 처리 시작
}
```
- 옷 업로드 → 이미지 처리 시작
- 결제 완료 → 주문 상태 변경
- 느슨한 결합 달성

### Q11-5. 마이크로서비스 아키텍처의 장단점은?
**장점:**
- 독립적 배포/확장
- 기술 스택 다양화 (Java, Python)
- 장애 격리

**단점:**
- 네트워크 복잡도 증가
- 분산 트랜잭션 관리
- 운영 복잡도 증가

---

## 12. 트러블슈팅 / 문제 해결

### Q12-1. 개발 중 겪었던 어려운 문제와 해결 과정은?
**예시 답변 포인트:**
- CORS 에러: Swagger 상대 URL 사용
- JWT 토큰 검증 실패: Secret Key 길이 문제
- N+1 쿼리 문제: Fetch Join 적용
- 메모리 부족: JVM 옵션 최적화

### Q12-2. 성능 이슈를 어떻게 발견하고 해결하셨나요?
**예상 답변 키워드:**
- SQL 로깅으로 쿼리 분석
- 프로파일링 도구 사용
- 인덱스 최적화
- 캐싱 적용 고려

### Q12-3. 외부 API 장애 대응은 어떻게 하셨나요?
**예상 답변 키워드:**
- 타임아웃 설정
- 재시도 로직
- Circuit Breaker 패턴 고려
- 폴백(Fallback) 전략

### Q12-4. 동시성 이슈는 어떻게 처리하셨나요?
**예상 답변 키워드:**
- `@Transactional` 격리 수준
- 비관적 락 (`@Lock`)
- 낙관적 락 (`@Version`)
- 분산 락 (Redis) 고려

---

## 13. 프론트엔드 (React)

### Q13-1. React 프로젝트 구조를 설명해주세요.
**예상 답변 키워드:**
```
src/
├── components/     # 재사용 컴포넌트
├── pages/          # 페이지 컴포넌트
├── hooks/          # 커스텀 훅
├── services/       # API 호출
├── store/          # 상태 관리
└── utils/          # 유틸리티
```

### Q13-2. API 호출은 어떻게 관리하셨나요?
**예상 답변 키워드:**
- Axios 인스턴스 생성
- 인터셉터로 토큰 자동 첨부
- 에러 핸들링 공통화
- 타입스크립트 타입 정의

### Q13-3. 인증 상태 관리는 어떻게 하셨나요?
**예상 답변 키워드:**
- JWT 토큰 저장 (localStorage/sessionStorage)
- Context API 또는 상태 관리 라이브러리
- Protected Route 구현
- 토큰 만료 처리

### Q13-4. WebSocket 클라이언트 구현은?
**예상 답변 키워드:**
- SockJS + STOMP 클라이언트
- 연결/구독/발행 로직
- 재연결 처리
- 훅으로 추상화

### Q13-5. TypeScript를 사용한 이유는?
**예상 답변 키워드:**
- 타입 안정성
- IDE 자동완성 지원
- 리팩토링 용이
- 문서화 효과
- 런타임 에러 방지

---

## 추가 심화 질문

### 확장성 관련
- 사용자가 10배 증가하면 어떻게 대응하시겠습니까?
- 이미지 처리 서버 부하가 증가하면 어떻게 스케일링하시겠습니까?

### 보안 관련
- SQL Injection을 어떻게 방지하셨나요?
- XSS 공격 방어는 어떻게 하셨나요?
- OWASP Top 10 중 고려한 항목은?

### 설계 관련
- 현재 아키텍처의 개선점이 있다면?
- 처음부터 다시 설계한다면 어떻게 하시겠습니까?
- MSA로 완전히 분리한다면 어떤 서비스로 나누시겠습니까?

---

## 면접 팁

1. **코드 예시 준비**: 실제 프로젝트 코드를 기반으로 설명
2. **Why 강조**: 기술 선택 이유와 트레이드오프 설명
3. **문제 해결 경험**: 구체적인 트러블슈팅 사례 준비
4. **성장 마인드**: 개선점과 학습한 내용 언급

---

*이 문서는 ClosetConnect 프로젝트 기반으로 작성되었습니다.*
