# 관찰성 및 모니터링 가이드

> ClosetConnect 시스템 관찰성(Observability) 표준 문서 (MCP Reference)

**버전**: v1.0
**최종 수정**: 2025-12-31
**목적**: 로깅, 메트릭, 트레이싱 표준 정의 및 장애 대응 Runbook 제공

---

## 📋 목차

1. [로그 표준](#로그-표준)
2. [구조화 로그 필드](#구조화-로그-필드)
3. [메트릭 및 SLI/SLO](#메트릭-및-slislo)
4. [분산 추적(Tracing)](#분산-추적tracing)
5. [알람 규칙](#알람-규칙)
6. [장애 대응 Runbook](#장애-대응-runbook)

---

## 로그 표준

### 로그 레벨 정의

| 레벨 | 용도 | 예시 | 운영 환경 출력 |
|-----|------|------|--------------|
| **TRACE** | 세부 실행 흐름 | 함수 진입/종료 | ❌ |
| **DEBUG** | 디버깅 정보 | 변수 값, SQL 쿼리 | ❌ |
| **INFO** | 일반 정보 | 요청 처리 시작/완료 | ✅ |
| **WARN** | 경고 (처리 계속) | Deprecated API 사용 | ✅ |
| **ERROR** | 에러 (처리 실패) | 예외 발생, 외부 API 실패 | ✅ |
| **FATAL** | 치명적 오류 (시스템 중단) | DB 연결 불가 | ✅ |

### 로그 포맷

#### JSON 구조화 로그 (권장)

```json
{
  "timestamp": "2025-12-31T10:30:00.123Z",
  "level": "INFO",
  "logger": "com.tigger.closetconnectproject.Closet.Service.ClothService",
  "thread": "http-nio-8080-exec-5",
  "message": "옷 업로드 처리 시작",
  "context": {
    "traceId": "a1b2c3d4e5f6",
    "spanId": "1234567890ab",
    "userId": "***456",
    "clothId": 123,
    "operation": "upload",
    "duration": 1523,
    "status": "SUCCESS"
  },
  "tags": {
    "environment": "production",
    "service": "backend",
    "version": "1.0.0"
  }
}
```

#### 기존 텍스트 로그 (현재)

```
2025-12-31 10:30:00.123 INFO  [http-nio-8080-exec-5] c.t.c.Closet.Service.ClothService : [a1b2c3d4e5f6] 옷 업로드 처리 시작 userId=***456, clothId=123
```

---

## 구조화 로그 필드

### 필수 필드

| 필드 | 타입 | 설명 | 예시 |
|-----|------|------|------|
| `timestamp` | ISO8601 | 로그 발생 시각 (UTC) | `2025-12-31T10:30:00.123Z` |
| `level` | String | 로그 레벨 | `INFO`, `ERROR` |
| `logger` | String | 로거 이름 (클래스명) | `com.tigger...ClothService` |
| `message` | String | 로그 메시지 | `옷 업로드 처리 시작` |
| `traceId` | String | 분산 추적 ID | `a1b2c3d4e5f6` |

### 컨텍스트 필드

| 필드 | 타입 | 설명 | 마스킹 | 예시 |
|-----|------|------|--------|------|
| `userId` | Long | 사용자 ID | ⭐ 필수 | `***456` (뒤 3자리만) |
| `email` | String | 사용자 이메일 | ⭐ 필수 | `u***@example.com` |
| `clothId` | Long | 옷 ID | ❌ | `123` |
| `orderId` | String | 주문 ID | ❌ | `ORDER_123` |
| `paymentKey` | String | 결제 키 | ⭐ 필수 | `toss***123` |
| `ipAddress` | String | 클라이언트 IP | ❌ | `192.168.1.1` |
| `userAgent` | String | User-Agent | ❌ | `Mozilla/5.0...` |

### 성능 필드

| 필드 | 타입 | 설명 | 예시 |
|-----|------|------|------|
| `duration` | Integer | 처리 시간 (ms) | `1523` |
| `dbQueryTime` | Integer | DB 쿼리 시간 (ms) | `245` |
| `externalApiTime` | Integer | 외부 API 호출 시간 (ms) | `3500` |

### 에러 필드

| 필드 | 타입 | 설명 | 예시 |
|-----|------|------|------|
| `errorCode` | String | 애플리케이션 에러 코드 | `CLOTH_NOT_FOUND` |
| `errorMessage` | String | 에러 메시지 | `옷을 찾을 수 없습니다.` |
| `stackTrace` | String | 스택 트레이스 | `java.lang.NullPointerException...` |
| `causedBy` | String | 근본 원인 | `java.net.SocketTimeoutException` |

### 마스킹 규칙

#### 사용자 정보

```java
// userId: 123456 → ***456 (뒤 3자리만)
String maskedUserId = "***" + userId.toString().substring(Math.max(0, userId.toString().length() - 3));

// email: user@example.com → u***@example.com
String maskedEmail = email.charAt(0) + "***@" + email.split("@")[1];

// phone: 010-1234-5678 → 010-****-5678
String maskedPhone = phone.substring(0, 4) + "****" + phone.substring(8);
```

#### 결제 정보

```java
// paymentKey: toss_payment_key_123456 → toss***456
String maskedKey = key.substring(0, 4) + "***" + key.substring(Math.max(0, key.length() - 3));

// cardNumber: 1234-5678-9012-3456 → 1234-****-****-3456
String maskedCard = card.substring(0, 5) + "****-****" + card.substring(15);
```

### 로그 예시

#### 정상 처리

```json
{
  "timestamp": "2025-12-31T10:30:00.123Z",
  "level": "INFO",
  "logger": "com.tigger.closetconnectproject.Closet.Service.ClothService",
  "thread": "http-nio-8080-exec-5",
  "message": "옷 업로드 처리 완료",
  "context": {
    "traceId": "a1b2c3d4e5f6",
    "spanId": "1234567890ab",
    "userId": "***456",
    "clothId": 123,
    "operation": "upload",
    "imageType": "SINGLE_ITEM",
    "duration": 1523,
    "status": "SUCCESS"
  }
}
```

#### 에러 발생

```json
{
  "timestamp": "2025-12-31T10:30:05.456Z",
  "level": "ERROR",
  "logger": "com.tigger.closetconnectproject.Closet.Service.ClothService",
  "thread": "http-nio-8080-exec-5",
  "message": "AI 서버 호출 실패",
  "context": {
    "traceId": "a1b2c3d4e5f6",
    "spanId": "1234567890ab",
    "userId": "***456",
    "clothId": 123,
    "operation": "ai_segmentation",
    "duration": 5234,
    "status": "FAILED"
  },
  "error": {
    "errorCode": "AI_SERVICE_UNAVAILABLE",
    "errorMessage": "AI 서버에 연결할 수 없습니다.",
    "causedBy": "java.net.SocketTimeoutException: Read timed out",
    "stackTrace": "com.tigger.closetconnectproject.Closet.Client.AIClient.callSegmentation(AIClient.java:45)..."
  }
}
```

---

## 메트릭 및 SLI/SLO

### 메트릭 네이밍 규칙

```
{application}_{component}_{metric}_{unit}

예시:
- closet_api_requests_total (Counter)
- closet_ai_processing_duration_seconds (Histogram)
- closet_db_connections_active (Gauge)
```

### RED 메트릭 (Request-Driven Services)

#### Rate (요청률)

| 메트릭 | 타입 | 설명 | 예시 |
|-------|------|------|------|
| `closet_api_requests_total` | Counter | 총 요청 수 (by endpoint, status) | `{endpoint="/api/v1/cloth", status="200"}` |
| `closet_ai_requests_total` | Counter | AI 요청 수 (by model) | `{model="segformer"}` |

#### Errors (에러율)

| 메트릭 | 타입 | 설명 | 예시 |
|-------|------|------|------|
| `closet_api_errors_total` | Counter | API 에러 수 (by endpoint, code) | `{endpoint="/api/v1/cloth", code="500"}` |
| `closet_ai_errors_total` | Counter | AI 처리 에러 수 | `{model="segformer", error="timeout"}` |

#### Duration (지연시간)

| 메트릭 | 타입 | 설명 | 예시 |
|-------|------|------|------|
| `closet_api_duration_seconds` | Histogram | API 응답 시간 (by endpoint) | Buckets: 0.1, 0.5, 1, 5, 10 |
| `closet_ai_duration_seconds` | Histogram | AI 처리 시간 (by model) | Buckets: 10, 30, 60, 120 |

### USE 메트릭 (Resource Services)

#### Utilization (사용률)

| 메트릭 | 타입 | 설명 | 임계값 |
|-------|------|------|--------|
| `closet_cpu_usage_percent` | Gauge | CPU 사용률 | > 80% 경고 |
| `closet_memory_usage_percent` | Gauge | 메모리 사용률 | > 85% 경고 |
| `closet_db_connections_active` | Gauge | 활성 DB 커넥션 수 | > 80% 경고 |

#### Saturation (포화도)

| 메트릭 | 타입 | 설명 | 임계값 |
|-------|------|------|--------|
| `closet_db_pool_queue_length` | Gauge | DB 커넥션 대기 큐 길이 | > 10 경고 |
| `closet_thread_pool_queue_length` | Gauge | 스레드 풀 대기 큐 길이 | > 100 경고 |

#### Errors

| 메트릭 | 타입 | 설명 | 임계값 |
|-------|------|------|--------|
| `closet_db_errors_total` | Counter | DB 에러 수 | > 5/min 경고 |
| `closet_oom_errors_total` | Counter | Out of Memory 발생 수 | > 0 즉시 알람 |

### 비즈니스 메트릭

| 메트릭 | 타입 | 설명 | 예시 |
|-------|------|------|------|
| `closet_uploads_total` | Counter | 총 업로드 수 (by imageType) | `{imageType="SINGLE_ITEM"}` |
| `closet_ai_success_rate` | Gauge | AI 처리 성공률 (%) | `95.5` |
| `closet_payment_amount_total` | Counter | 총 결제 금액 (원) | `1500000` |
| `closet_users_active_daily` | Gauge | 일일 활성 사용자 (DAU) | `1234` |

### SLI (Service Level Indicator)

#### API 가용성

```
SLI: (성공한 요청 수 / 전체 요청 수) * 100

PromQL:
sum(rate(closet_api_requests_total{status=~"2.."}[5m]))
/
sum(rate(closet_api_requests_total[5m]))
* 100
```

**목표**: 99.5% (월간)

#### API 응답 시간 (P99)

```
SLI: 99th percentile 응답 시간

PromQL:
histogram_quantile(0.99,
  sum(rate(closet_api_duration_seconds_bucket[5m])) by (le, endpoint)
)
```

**목표**: < 1초 (일반 API), < 10초 (AI 시작)

#### AI 처리 성공률

```
SLI: (성공한 AI 처리 / 전체 AI 처리) * 100

PromQL:
sum(rate(closet_ai_requests_total{status="SUCCESS"}[5m]))
/
sum(rate(closet_ai_requests_total[5m]))
* 100
```

**목표**: 95% (월간)

### SLO (Service Level Objective)

| 서비스 | SLI | SLO | Error Budget (월간) |
|--------|-----|-----|-------------------|
| API 가용성 | 성공률 | 99.5% | 0.5% (21.6분) |
| API 응답 시간 | P99 Latency | < 1s | - |
| AI 처리 성공률 | 성공률 | 95% | 5% |
| DB 쿼리 시간 | P95 Latency | < 100ms | - |

### Error Budget 계산

```
Error Budget = (1 - SLO) * Total Requests

예시: 월간 100만 요청, SLO 99.5%
Error Budget = (1 - 0.995) * 1,000,000 = 5,000 실패 허용
```

**Error Budget 소진 시 대응**:
- 50% 소진: 경고, 새 기능 배포 신중
- 75% 소진: 새 기능 배포 중단, 안정성 작업 우선
- 100% 소진: 긴급 대응, 원인 분석 및 개선

---

## 분산 추적(Tracing)

### Trace Context 전파

#### HTTP 헤더

```
X-Trace-Id: a1b2c3d4e5f6      (16자리 hex)
X-Span-Id: 1234567890ab       (12자리 hex)
X-Parent-Span-Id: abcdef123456 (12자리 hex, optional)
```

#### 생성 규칙

```java
String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
String spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
```

### Span 구조

```json
{
  "traceId": "a1b2c3d4e5f6",
  "spanId": "1234567890ab",
  "parentSpanId": null,
  "operationName": "POST /api/v1/cloth/upload",
  "startTime": 1735635000000,
  "duration": 5234,
  "tags": {
    "http.method": "POST",
    "http.url": "/api/v1/cloth/upload",
    "http.status_code": 201,
    "user.id": "456",
    "cloth.id": "123"
  },
  "logs": [
    {
      "timestamp": 1735635001000,
      "fields": {
        "event": "ai_request_start",
        "model": "segformer"
      }
    },
    {
      "timestamp": 1735635004500,
      "fields": {
        "event": "ai_request_complete",
        "status": "success"
      }
    }
  ]
}
```

### 트레이스 플로우 예시

```
Client Request (traceId: a1b2c3d4e5f6)
  │
  ├─ [Span 1] API Gateway (spanId: 1234567890ab)
  │   │ Duration: 5234ms
  │   │
  │   ├─ [Span 2] ClothService.upload (spanId: 2345678901bc, parent: 1234567890ab)
  │   │   │ Duration: 150ms
  │   │   │
  │   │   └─ [Span 3] DB INSERT (spanId: 3456789012cd, parent: 2345678901bc)
  │   │       Duration: 45ms
  │   │
  │   ├─ [Span 4] RabbitMQ Publish (spanId: 4567890123de, parent: 1234567890ab)
  │   │   Duration: 23ms
  │   │
  │   └─ [Span 5] AI Server Request (spanId: 5678901234ef, parent: 1234567890ab)
  │       │ Duration: 4850ms
  │       │
  │       ├─ [Span 6] Segformer Inference (spanId: 6789012345f0, parent: 5678901234ef)
  │       │   Duration: 3200ms
  │       │
  │       └─ [Span 7] Image Processing (spanId: 789012345601, parent: 5678901234ef)
  │           Duration: 1500ms
  │
  └─ Response
```

---

## 알람 규칙

### 알람 등급

| 등급 | 설명 | 대응 시간 | 예시 |
|-----|------|----------|------|
| **P1 (Critical)** | 서비스 중단 | 즉시 (5분 이내) | API 가용성 < 95% |
| **P2 (High)** | 주요 기능 장애 | 30분 이내 | AI 처리 실패율 > 20% |
| **P3 (Medium)** | 성능 저하 | 2시간 이내 | P99 지연시간 > 5s |
| **P4 (Low)** | 경미한 문제 | 24시간 이내 | 메모리 사용률 > 80% |

### 알람 정책

#### API 가용성 (P1)

```yaml
alert: APIAvailabilityLow
expr: |
  (sum(rate(closet_api_requests_total{status=~"2.."}[5m]))
  /
  sum(rate(closet_api_requests_total[5m]))
  * 100) < 95
for: 5m
labels:
  severity: critical
  priority: P1
annotations:
  summary: "API 가용성이 95% 미만입니다."
  description: "현재 가용성: {{ $value }}%"
  runbook: "https://wiki.closetconnect.com/runbook/api-availability"
```

#### AI 처리 실패율 (P2)

```yaml
alert: AIProcessingFailureHigh
expr: |
  (sum(rate(closet_ai_requests_total{status="FAILED"}[5m]))
  /
  sum(rate(closet_ai_requests_total[5m]))
  * 100) > 20
for: 10m
labels:
  severity: high
  priority: P2
annotations:
  summary: "AI 처리 실패율이 20%를 초과했습니다."
  description: "현재 실패율: {{ $value }}%"
```

#### 응답 시간 지연 (P3)

```yaml
alert: APILatencyHigh
expr: |
  histogram_quantile(0.99,
    sum(rate(closet_api_duration_seconds_bucket[5m])) by (le, endpoint)
  ) > 5
for: 15m
labels:
  severity: medium
  priority: P3
annotations:
  summary: "P99 응답 시간이 5초를 초과했습니다."
  description: "엔드포인트 {{ $labels.endpoint }}: {{ $value }}s"
```

#### 메모리 사용률 (P4)

```yaml
alert: MemoryUsageHigh
expr: closet_memory_usage_percent > 80
for: 30m
labels:
  severity: low
  priority: P4
annotations:
  summary: "메모리 사용률이 80%를 초과했습니다."
  description: "현재 사용률: {{ $value }}%"
```

### 알람 채널

| 우선순위 | Slack | PagerDuty | Email | 비고 |
|---------|-------|-----------|-------|------|
| P1 | ✅ #alerts-critical | ✅ On-Call | ✅ | 즉시 알림 |
| P2 | ✅ #alerts-high | ✅ | ✅ | 30분 이내 |
| P3 | ✅ #alerts-medium | ❌ | ✅ | 업무 시간 |
| P4 | ✅ #alerts-low | ❌ | ❌ | 주간 요약 |

---

## 장애 대응 Runbook

### Runbook 템플릿

```markdown
# [장애명]

## 증상
- 무엇이 관찰되는가? (메트릭, 로그, 사용자 신고 등)

## 원인
- 가능한 근본 원인 목록

## 진단
- 원인을 확인하는 단계별 절차

## 조치
- 문제를 해결하는 단계별 절차

## 검증
- 해결 확인 방법

## 사후 조치
- 재발 방지책
```

### Runbook 1: API 가용성 저하

#### 증상
```
알람: APIAvailabilityLow
메트릭: API 가용성 < 95%
로그: 대량의 5xx 에러
사용자: "서비스가 느려요", "에러가 나요"
```

#### 원인
1. **DB 커넥션 풀 고갈**: DB 응답 지연 → 커넥션 부족
2. **AI 서버 다운**: AI 처리 타임아웃 → 요청 실패
3. **메모리 부족**: GC Pause → 응답 지연
4. **외부 API 장애**: Toss Payments 타임아웃

#### 진단

**1단계: 메트릭 확인**
```bash
# Grafana 대시보드 확인
http://grafana.closetconnect.com/d/api-overview

# 주요 메트릭:
- closet_api_requests_total (급증?)
- closet_api_errors_total (증가?)
- closet_api_duration_seconds (P99 지연?)
```

**2단계: 로그 확인**
```bash
# Kibana에서 최근 5분간 에러 로그
level:ERROR AND timestamp:[now-5m TO now]

# 패턴 분석:
- SocketTimeoutException → 외부 API 문제
- Connection pool exhausted → DB 문제
- OutOfMemoryError → 메모리 부족
```

**3단계: 리소스 확인**
```bash
# CPU, 메모리, 디스크
kubectl top nodes
kubectl top pods -n production

# DB 상태
mysql> SHOW PROCESSLIST;
mysql> SHOW STATUS LIKE 'Threads_connected';
```

#### 조치

**Case 1: DB 커넥션 풀 고갈**
```bash
# 1. 즉시 조치: 커넥션 풀 크기 증가
kubectl set env deployment/backend \
  SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=50

# 2. 재시작 (롤링 업데이트)
kubectl rollout restart deployment/backend

# 3. 느린 쿼리 확인
mysql> SELECT * FROM information_schema.processlist WHERE Time > 5;

# 4. 느린 쿼리 Kill
mysql> KILL <PROCESS_ID>;
```

**Case 2: AI 서버 다운**
```bash
# 1. AI 서버 상태 확인
kubectl get pods -n ai-server

# 2. 로그 확인
kubectl logs -n ai-server <pod-name> --tail=100

# 3. 재시작
kubectl rollout restart deployment/ai-server

# 4. Circuit Breaker 확인 (자동 차단되었는지)
curl http://backend:8080/actuator/health
```

**Case 3: 메모리 부족**
```bash
# 1. 힙 덤프 생성 (분석용)
kubectl exec -it <pod-name> -- jmap -dump:format=b,file=/tmp/heap.hprof <PID>

# 2. 즉시 조치: Pod 재시작
kubectl delete pod <pod-name>

# 3. 메모리 한도 증가
kubectl set resources deployment/backend \
  --limits=memory=4Gi \
  --requests=memory=2Gi
```

#### 검증
```bash
# 1. 가용성 메트릭 확인 (95% 이상 회복?)
watch -n 5 'curl -s http://prometheus:9090/api/v1/query?query=closet_api_availability'

# 2. 에러율 감소 확인
watch -n 5 'kubectl logs -n production deployment/backend --tail=10 | grep ERROR'

# 3. 사용자 요청 정상 처리 확인
curl -H "Authorization: Bearer $TOKEN" http://api.closetconnect.com/api/v1/cloth
```

#### 사후 조치
1. **포스트모템 작성**: 원인, 영향, 타임라인, 재발 방지책
2. **DB 쿼리 최적화**: 인덱스 추가, N+1 문제 해결
3. **AI 서버 헬스체크 강화**: Liveness/Readiness Probe 튜닝
4. **메모리 프로파일링**: 메모리 누수 분석 및 수정

---

### Runbook 2: AI 처리 실패율 급증

#### 증상
```
알람: AIProcessingFailureHigh
메트릭: AI 실패율 > 20%
로그: AI_SERVICE_UNAVAILABLE 대량 발생
사용자: "옷 등록이 계속 실패해요"
```

#### 원인
1. **AI 서버 과부하**: 동시 요청 증가
2. **모델 로딩 실패**: GPU 메모리 부족
3. **RabbitMQ 메시지 적체**: Worker 처리 지연
4. **Google AI API 장애**: Gemini/Imagen 응답 없음

#### 진단

**1단계: AI 서버 상태**
```bash
# Pod 상태 확인
kubectl get pods -n ai-server

# 로그 확인 (최근 100줄)
kubectl logs -n ai-server deployment/ai-server --tail=100

# 리소스 사용률
kubectl top pods -n ai-server
```

**2단계: RabbitMQ 상태**
```bash
# 큐 길이 확인
curl -u guest:guest http://rabbitmq:15672/api/queues/%2F/cloth.processing.queue

# 메시지 적체 여부
{
  "messages": 1234,  # 적체됨 (> 100이면 문제)
  "messages_ready": 1200,
  "consumers": 0  # Worker 중단?
}
```

**3단계: 외부 API 상태**
```bash
# Google AI API 헬스체크
curl https://generativelanguage.googleapis.com/v1beta/models?key=$GOOGLE_API_KEY

# 응답 시간 확인
time curl -X POST https://generativelanguage.googleapis.com/...
```

#### 조치

**Case 1: AI 서버 과부하**
```bash
# 1. 즉시 조치: Replicas 증가
kubectl scale deployment/ai-server --replicas=5

# 2. HPA (Horizontal Pod Autoscaler) 설정
kubectl autoscale deployment/ai-server \
  --cpu-percent=70 \
  --min=3 --max=10

# 3. 부하 분산 확인
kubectl get svc ai-server
```

**Case 2: RabbitMQ 적체**
```bash
# 1. Worker 수 증가
kubectl scale deployment/cloth-worker --replicas=5

# 2. 우선순위 큐 처리 (긴급 요청 먼저)
# (애플리케이션 레벨 수정 필요)

# 3. 메시지 수동 처리 (비상)
kubectl exec -it deployment/cloth-worker -- python process_backlog.py
```

**Case 3: Google AI API 장애**
```bash
# 1. Circuit Breaker 확인 (자동 차단)
# 로그에서 "Circuit is OPEN" 확인

# 2. Fallback 전략 활성화 (설정 변경)
# - Imagen 대신 로컬 모델 사용
# - 또는 처리 대기 큐에 저장

# 3. Google Cloud Status 확인
curl https://status.cloud.google.com/
```

#### 검증
```bash
# 1. 실패율 감소 확인
watch -n 10 'curl -s "http://prometheus:9090/api/v1/query?query=closet_ai_failure_rate"'

# 2. RabbitMQ 큐 길이 감소
watch -n 5 'curl -u guest:guest http://rabbitmq:15672/api/queues/%2F/cloth.processing.queue | jq .messages'

# 3. 사용자 요청 정상 처리
# (테스트 이미지 업로드 → 처리 완료 확인)
```

---

### Runbook 3: DB 연결 불가

#### 증상
```
알람: DatabaseConnectionFailed
로그: Unable to acquire JDBC Connection
사용자: "서비스를 이용할 수 없습니다"
```

#### 원인
1. **DB 서버 다운**: MariaDB 프로세스 종료
2. **네트워크 단절**: DB 호스트 연결 불가
3. **인증 실패**: 비밀번호 변경, 권한 부족
4. **Max Connections 초과**: DB 커넥션 한도 도달

#### 진단
```bash
# 1. DB 서버 Ping
ping db.closetconnect.com

# 2. DB 포트 확인
telnet db.closetconnect.com 3306

# 3. DB 프로세스 확인
kubectl get pods -n database

# 4. DB 로그 확인
kubectl logs -n database deployment/mariadb --tail=100

# 5. 커넥션 수 확인
mysql> SHOW STATUS LIKE 'Threads_connected';
mysql> SHOW STATUS LIKE 'Max_used_connections';
mysql> SHOW VARIABLES LIKE 'max_connections';
```

#### 조치
```bash
# Case 1: DB 서버 재시작 필요
kubectl rollout restart deployment/mariadb

# Case 2: Max Connections 증가
mysql> SET GLOBAL max_connections = 500;

# Case 3: 비정상 커넥션 Kill
mysql> SELECT * FROM information_schema.processlist WHERE Command = 'Sleep' AND Time > 300;
mysql> KILL <PROCESS_ID>;

# Case 4: 백엔드 커넥션 풀 축소 (임시)
kubectl set env deployment/backend \
  SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=20
```

---

## MCP 질의 예시

### Q1: "API 응답 시간이 느릴 때 어떻게 확인하나요?"

**A**:
1. Grafana 대시보드에서 `closet_api_duration_seconds` P99 메트릭 확인
2. 로그에서 `duration > 1000ms` 검색
3. Trace ID로 분산 추적에서 병목 지점 확인 (DB? AI? 외부 API?)
4. Runbook: API 가용성 저하 참고

### Q2: "로그에서 사용자 ID는 어떻게 마스킹하나요?"

**A**:
```java
String maskedUserId = "***" + userId.toString().substring(Math.max(0, userId.toString().length() - 3));
```
예시: `123456` → `***456`

### Q3: "AI 처리 실패율 SLO는?"

**A**: 95% (월간). Error Budget은 5%. 현재 실패율이 20%를 초과하면 P2 알람 발생.

---

**작성일**: 2025-12-31
**관리자**: DevOps Team
**문서 상태**: Draft
