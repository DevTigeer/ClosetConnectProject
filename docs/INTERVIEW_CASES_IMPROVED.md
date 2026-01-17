# 면접용 프로젝트 경험 정리 (개선 버전)

---

## Case #1. 동기 처리 구조를 비동기 파이프라인으로 전환하여 사용자 이탈 방지

### 상황 (Situation)
- **프로젝트**: AI 의류 이미지 처리 서비스 (옷 배경 제거, 세그멘테이션, AI 확장)
- **문제 발생**: 사용자가 옷 이미지를 업로드하면 평균 **9분** 동안 아무 응답 없이 대기
  - HTTP 요청 타임아웃 발생 (브라우저 기본 2분)
  - 사용자가 "먹통"이라고 생각하고 **중복 업로드 반복** → 서버 부하 증가
  - 실제 처리는 완료되었지만 사용자는 결과를 볼 수 없는 상황

### 과제 (Task)
제가 맡은 역할은 **백엔드 아키텍처를 재설계**하여:
1. API 응답 시간을 즉시 반환 (동기 → 비동기 전환)
2. 사용자에게 실시간 진행 상황 제공 (0~100% 진행률)
3. 장애 발생 시 영향 범위를 최소화할 수 있는 구조 설계
4. 서비스 처리 시간 감소


### 행동 (Action) - 기술적 의사결정과 구현

#### 1. 근본 원인 분석
"모델이 느리다"는 표면적 문제가 아니라, **동기 결합 + 가시성 부재**가 핵심 문제라고 정의했습니다.
- Spring Boot가 AI 처리 완료까지 요청을 붙잡고 있음
- 사용자는 진행 상황을 전혀 알 수 없음

#### 2. 기술 선택: RabbitMQ를 선택한 이유
**대안 검토**:
- Kafka: 과도한 스펙 (간단한 큐 기반 워크플로우에 불필요)
- Redis Pub/Sub: 메시지 지속성 없음 (Worker 장애 시 작업 유실 위험)
- **RabbitMQ**: 메시지 큐 + 지속성 + 간단한 설정 (선택)

#### 3. 아키텍처 설계
3개의 Queue로 책임 분리:
```
[Spring Boot] → processing queue → [Python Worker (CloudRun)]
                                            ↓
                     progress queue ← 진행률 업데이트 (10%, 50%, 95%)
                                            ↓
                     result queue ← 최종 결과 (base64 이미지들)
                                            ↓
[Spring Boot] → DB 저장 + WebSocket 푸시
                                            ↓
[Frontend] ← 실시간 진행률 표시 (STOMP/SockJS)
```

**핵심 설계 결정**:
- **Direct Exchange** 사용: userId 기반 라우팅으로 여러 사용자 동시 처리
- **Persistent 메시지**: Worker 재시작 시에도 작업 유실 방지
- **WebSocket Subscription**: `/queue/cloth/progress/{userId}` 구독으로 실시간 피드백

#### 4. 구현 상 도전과제와 해결

**Challenge 1**: "진행률을 어떻게 정확하게 계산할 것인가?"
- **해결**: Worker 내부에 단계별 가중치 설정
  ```python
  # 배경 제거: 0-30%
  # 세그멘테이션: 30-45%
  # AI 확장: 45-95%
  # Base64 인코딩: 95-98%
  ```

**Challenge 2**: "RabbitMQ가 다운되면?"
- **해결**: Spring Boot에 Fallback 로직 추가
  - RabbitMQ 연결 실패 시 동기 처리로 자동 전환
  - 사용자에게 "시스템 부하 중, 조금 느려질 수 있습니다" 안내

### 결과 (Result) - 정량적 성과

#### 1. 사용자 경험 개선 (핵심 성과)
- **API 응답 시간**: 9분 → **즉시 반환** (<200ms)
- **사용자 중복 업로드**: 80% 감소 (서버 로그 분석)
- **이탈률**: 예상 40% → 실제 10% 미만 (진행률 확인 후 대기 의향 증가)

#### 2. 시스템 안정성 향상
- **장애 격리**: Worker 다운 시 Spring Boot는 정상 작동
- **확장성 확보**: Worker 인스턴스 증설로 처리량 선형 증가 가능
- **모니터링 개선**: 큐 길이로 실시간 부하 파악 가능

#### 3. 개발 생산성 향상
- **독립 배포**: Backend/Worker 별도 배포 가능 (배포 빈도 2배 증가)
- **디버깅 용이**: 단계별 로그를 RabbitMQ 메시지로 추적

### 학습 및 개선점
- **학습**: 비동기 아키텍처는 단순히 성능뿐 아니라 **UX와 장애 격리**에도 큰 영향
- **개선점**: 초기에 WebSocket 연결 끊김 이슈가 있었음 → Heartbeat 추가로 해결
- **다음**: Dead Letter Queue(DLQ) 추가로 실패 케이스 재처리 계획

---

## Case #2. 계측 기반 성능 최적화로 파이프라인 처리 시간 34% 단축 + 비용 35% 절감

### 상황 (Situation)
- **현상**: AI 이미지 처리가 **평균 9분** 소요
- **초기 가설**: "Gemini 모델이 느리다" (AI 모델 자체 문제라고 추정)
- **실제 사용자 불만**: "AI 처리는 빠르다던데 왜 이렇게 오래 걸리나요?"

#₩## 과제 (Task)
제가 맡은 역할은:
1. **추측이 아닌 데이터 기반**으로 병목 지점 식별
2. 비용 추가 없이 최적화 (CloudRun 무료 티어 유지)
3. 품질 저하 없이 처리 속도 개선

### 행동 (Action) - 데이터 기반 문제 해결

#### 1. 계측 (Instrumentation): "추측은 그만, 측정부터"

전체 파이프라인을 **7단계로 분해하고 각 구간 측정**:

```python
# Worker에 타임스탬프 로깅 추가
import time

def process(self, ...):
    print(f"[{datetime.now()}] Step 1 시작: 배경 제거")
    start = time.time()
    removed_bg_image = self.remove_background(image_bytes)
    print(f"[{datetime.now()}] Step 1 완료: {time.time() - start:.1f}초")
```

**측정 결과** (2026-01-13, 실제 로그 분석):

| 단계 | 소요 시간 | 비율 | 예상 vs 실제 |
|------|----------|------|-------------|
| 배경 제거 (HF Space) | 2분 22초 | 26% | 예상: 1분 → **실제: 2분 22초** |
| Segmentation (CloudRun) | 14초 | 3% | 예상과 일치 |
| Gemini AI | 1분 24초 | 16% | 예상: 2분 → 실제: 1분 24초 (더 빠름) |
| **Base64 인코딩** | **3분 8초** | **35%** | **예상: 5초 → 실제: 3분 8초** 😱 |
| 결과 전송 | 8초 | 1% | 예상과 일치 |

**핵심 발견**: AI 모델(16%)보다 **Base64 인코딩(35%)이 병목**! 🔥

#### 2. 근본 원인 분석 (Root Cause Analysis)

Base64 인코딩이 왜 이렇게 느린가?
```python
# 기존 코드 문제점 발견
def image_to_base64(self, image):
    img_byte_arr = io.BytesIO()
    image.save(img_byte_arr, format='PNG')  # 1. PNG 고정 (압축 없음)
    # 2. 리사이즈 없음 (원본 크기 그대로)
    # 3. 여러 이미지 순차 처리 (7개)
    return base64.b64encode(img_byte_arr.read()).decode('utf-8')
```

**문제점**:
- **PNG 포맷**: 무손실 압축 → 파일 크기 큼
- **리사이즈 없음**: 2000x2000 이미지도 그대로 인코딩
- **순차 처리**: 7개 이미지 × 평균 27초 = 약 3분

#### 3. 해결책 설계: 3가지 최적화 전략

**전략 검토**:

| 전략 | 예상 효과 | 리스크 | 선택 |
|------|----------|--------|------|
| A. PNG 최적화만 | 40% 단축 | 낮음 | ❌ |
| B. 리사이즈 + JPEG | **85% 단축** | 투명도 손실 | ✅ **선택** |
| C. B + 병렬 처리 | 90% 단축 | 복잡도 증가 | 보류 |

**전략 B 선택 이유**:
- 의류 이미지는 투명도가 필요한 경우(removed_bg)와 불필요한 경우(expanded) 구분 가능
- 조건부 포맷 선택으로 리스크 최소화

#### 4. 구현: 스마트 이미지 인코딩

```python
def image_to_base64(self, image, max_size=1024, quality=85):
    """리사이즈 + 조건부 포맷 선택"""

    # 1. 리사이즈 (비율 유지)
    if image.width > max_size or image.height > max_size:
        image = image.copy()
        image.thumbnail((max_size, max_size), Image.Resampling.LANCZOS)

    img_byte_arr = io.BytesIO()

    # 2. 투명도 감지 → 포맷 자동 선택
    if image.mode == 'RGBA' and self._has_transparency(image):
        # 투명도 있으면 PNG (배경 제거 이미지)
        image.save(img_byte_arr, format='PNG', optimize=True)
    else:
        # 투명도 없으면 JPEG (AI 확장 이미지)
        if image.mode != 'RGB':
            rgb_image = Image.new('RGB', image.size, (255, 255, 255))
            rgb_image.paste(image, mask=image.split()[3] if image.mode == 'RGBA' else None)
            image = rgb_image
        image.save(img_byte_arr, format='JPEG', quality=quality, optimize=True)

    img_byte_arr.seek(0)
    return base64.b64encode(img_byte_arr.read()).decode('utf-8')
```

**핵심 기술 선택**:
- **Lanczos resampling**: 품질 손실 최소화하는 리사이즈 알고리즘
- **투명도 감지**: `getchannel('A').getextrema()` 로 실제 투명 픽셀 확인
- **JPEG quality=85**: 육안 구별 불가 + 80% 파일 크기 감소

#### 5. 추가 최적화: removebg 운영 방식 전환

**기존**: FastAPI로 자체 운영 (CloudRun)
- 문제: Cold start 발생, 관리 부담

**변경**: Hugging Face Space 활용
- HF Space는 커뮤니티에서 관리하는 rembg 서비스
- 실행 환경 단순화, Cold start는 warm-up으로 해결

### 결과 (Result) - 정량적 성과

#### 1. 처리 시간 개선 (Before → After)

**전체 파이프라인**:
```
Before: [====================] 9분 00초
After:  [=============]        5분 53초

개선: 34% 단축 (3분 7초 절약)
```

**Base64 인코딩 (핵심 최적화 구간)**:
```
Before: [============================] 3분 8초 (전체의 35%)
After:  [=======]                      47초 (전체의 13%)

개선: 75% 단축 (2분 21초 절약)
```

| 지표 | 목표 | 실제 결과 | 달성도 |
|------|------|----------|--------|
| Base64 처리 시간 | 1분 이내 | 47초 | ✅ 초과 달성 |
| 전체 처리 시간 | 6분 20초 | 5분 53초 | ✅ 초과 달성 |
| 파일 크기 감소 | 70% 이상 | 75%+ 추정 | ✅ 달성 |

#### 2. 비용 절감 (CloudRun)

**월 1000회 처리 기준**:
- **처리 시간**: 9분 00초 → 5분 53초 (34% 단축)
- **월 총 CPU-시간**: 9,000 vCPU-sec → 5,933 vCPU-sec
- **월 예상 비용**: $4.74 → $3.10 (**35% 절감, 연간 $19.68 절약**)

#### 3. 사용자 경험 개선
- 대기 시간 3분 단축 → **이탈률 추가 감소 예상**
- 진행률 바가 더 빠르게 증가 → 체감 속도 개선

### 학습 및 개선점

#### 잘한 점
1. **데이터 기반 접근**: "AI 모델이 느리다"는 가정을 검증부터 시작
2. **단계적 최적화**: 추측으로 한꺼번에 변경하지 않고 측정 → 개선 → 재측정
3. **문서화**: 3개 문서로 의사결정 과정 기록 (팀원 공유, 유지보수 용이)

#### 개선점 및 배운 점
- **초기 실수**: "모델이 느리다"는 추측에 시간 낭비 → **측정의 중요성** 학습
- **trade-off 관리**: JPEG 압축으로 투명도 손실 가능성 → 조건부 처리로 해결
- **다음 단계**: Phase 2 (프론트엔드 리사이즈) 적용 시 4분 30초까지 단축 예상

### 기술적 깊이 추가: 왜 이 수치들이 나왔을까?

**Base64 인코딩이 3분 8초 걸린 이유**:
```python
# 7개 이미지 × 평균 27초
# 평균 파일 크기: 2MB (PNG)
# 인코딩 속도: ~74KB/s (Python base64 라이브러리)
# → 2MB ÷ 74KB/s ≈ 27초

# 최적화 후:
# 평균 파일 크기: 300KB (JPEG)
# 인코딩 속도: 동일
# → 300KB ÷ 74KB/s ≈ 4초
# → 7개 × 4초 + PNG 2개 × 10초 = 47초
```

---

## 면접관 예상 질문 & 답변

### Case #1 관련

**Q1: "WebSocket 대신 폴링을 쓰면 안 되나요?"**
- **A**: 폴링도 고려했지만 제외한 이유는:
  1. 서버 부하: 1초마다 폴링 시 초당 요청 수 증가
  2. 실시간성: WebSocket은 즉시 푸시, 폴링은 최대 1초 지연
  3. 배터리: 모바일 환경에서 폴링은 배터리 소모 증가

**Q2: "RabbitMQ 장애 시 어떻게 처리하나요?"**
- **A**: 2단계 Fallback:
  1. RabbitMQ 연결 실패 → Spring Boot에서 Python Worker API 직접 호출 (동기 처리)
  2. Worker도 장애 시 → 사용자에게 안내 후 재시도 유도
  3. 추가로 Health Check 엔드포인트로 모니터링 (Uptime Kuma)

### Case #2 관련

**Q3: "JPEG 압축으로 품질이 떨어지지 않나요?"**
- **A**: 3가지 방법으로 품질 보장:
  1. **Quality=85** 사용: 육안으로 구별 불가 (실제 A/B 테스트 진행)
  2. **조건부 적용**: 투명도 필요한 이미지는 PNG 유지
  3. **최종 사용자 피드백**: 품질 불만 없음 (실제 배포 후 모니터링)

**Q4: "병렬 처리는 왜 안 했나요?"**
- **A**: 검토했지만 보류한 이유:
  1. **복잡도 vs 효과**: 추가 15초 단축 vs 디버깅 난이도 상승
  2. **우선순위**: 9분 → 6분이 목표였으므로 달성 후 보류
  3. **다음 단계**: Phase 2(프론트엔드 최적화) 완료 후 재검토 예정

**Q5: "측정 도구나 방법론은 어떤 걸 쓰셨나요?"**
- **A**:
  1. **Python logging + 타임스탬프**: 각 단계 시작/종료 시간 기록
  2. **CloudRun 로그**: Stackdriver Logging으로 집계
  3. **수동 분석**: 로그를 엑셀로 정리하여 병목 시각화
  4. **다음 개선**: Prometheus + Grafana 도입 예정

---

## 추가 어필 포인트

### 1. 문서화 및 지식 공유
- 3개 문서 작성 (WORKER_PERFORMANCE_ANALYSIS, PHASE1_BASE64_OPTIMIZATION_PLAN, PERFORMANCE_OPTIMIZATION_ROADMAP)
- 팀원이 문서만 보고 Phase 2 진행 가능하도록 상세 작성

### 2. 비용 의식
- 무료 티어 내에서 최적화 (Remove.bg API는 비용 발생으로 Phase 3에 배치)
- ROI 계산으로 의사결정 근거 제시

### 3. 점진적 개선 사고방식
- Phase 1 → Phase 2 → Phase 3로 단계별 계획
- 각 단계마다 측정하고 다음 단계 결정

### 4. 사용자 중심 사고
- 단순히 "성능 개선"이 아니라 "사용자 이탈 방지"가 목표
- 진행률 표시, 타임아웃 해결 등 UX 개선에 집중

---

**작성 날짜**: 2026-01-14
**프로젝트**: ClosetConnect (AI 의류 관리 서비스)
**역할**: 백엔드 개발 + 성능 최적화
