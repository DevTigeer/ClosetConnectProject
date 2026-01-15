# ClosetConnect

> AI 기반 옷장 디지털화 및 소셜 플랫폼

## 프로젝트 개요

**ClosetConnect**는 의류 옷장 디지털화 및 소셜 플랫폼으로, 사용자가 자신의 옷을 디지털화하여 관리하고, AI 코디 추천을 받으며, 커뮤니티를 통해 패션 정보를 공유하고, 중고 의류 거래까지 할 수 있는 통합 패션 라이프 플랫폼입니다.

### 핵심 문제 해결

- **문제점**: 사람들은 자신이 가진 옷을 정확히 파악하지 못해 비슷한 옷을 중복 구매하거나, 코디에 어려움을 겪음
- **해결**: AI 기반 옷장 디지털화 및 자동 코디 추천으로 옷 활용도 증가 및 구매 최적화

### 기간
**2025.09 ~ 2026.01** (4개월)

---

## 핵심 성과

### 🚀 의류 업로드 파이프라인 최적화

| 지표 | Before | After | 개선율 |
|------|--------|-------|--------|
| **처리 시간** | 11분 00초 | 5분 53초 | **34% 단축** |
| **비용** | $4.74 | $3.10 | **35% 절감** |
| **Base64 인코딩** | 3분 08초 | 47초 | **75% 단축** |

---

## 기술 스택

### 1. Vercel (Frontend)
- **Framework**: React 18 + TypeScript

### 2. Railway (Backend)
- **Language**: Java 17
- **Framework**: Spring Boot 3.2.0
- **Database**: MariaDB (JDBC)
- **Message Queue**: RabbitMQ (AMQP)
- **Payment**: Toss Payments API

### 3. CloudRun (AI Services)
- **Language**: Python 3.11
- **AI Model**: Google Gemini 2.0 Flash
- **Segmentation**: Hugging Face - mattmdjaga/segformer_b2_clothes
- **Background Removal**: U2Net, Hugging Face cloth-segmentation

---

## 시스템 아키텍처

```
┌─────────────┐         ┌─────────────┐         ┌──────────────┐
│   Vercel    │◄────────┤   Railway   │◄────────┤  CloudRun    │
│  (React)    │  HTTPS  │ (Spring)    │  AMQP   │  (Python AI) │
│  Frontend   │         │  Backend    │         │   Worker     │
└─────────────┘         └─────────────┘         └──────────────┘
                              │
                              ▼
                        ┌─────────────┐
                        │  RabbitMQ   │
                        │ Message Bus │
                        └─────────────┘
                              │
                              ▼
                        ┌─────────────┐
                        │   MariaDB   │
                        │  Database   │
                        └─────────────┘
```

---

## Case Study #1: 동기 처리 → 비동기 파이프라인 전환

### 상황 (Situation)

- **프로젝트**: AI 의류 이미지 처리 서비스 (옷 배경 제거, 세그멘테이션, AI 확장)
- **문제 발생**: 사용자가 옷 이미지를 업로드하면 평균 **9분** 동안 아무 응답 없이 대기
  - HTTP 요청 타임아웃 발생
  - 사용자가 "먹통"이라고 생각하고 **중복 업로드 반복** → 서버 부하 증가
  - 실제 처리는 완료되었지만 사용자는 결과를 볼 수 없는 상황

### 과제 (Task)

1. API 응답 시간을 즉시 반환 (동기 → 비동기 전환)
2. 사용자에게 실시간 진행 상황 제공 (0~100% 진행률)

### 행동 (Action)

#### 비동기 아키텍처 설계

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

#### 핵심 설계 결정

- **Direct Exchange 사용**: userId 기반 라우팅으로 여러 사용자 동시 처리
- **Persistent 메시지**: Worker 재시작 시에도 작업 유실 방지
- **WebSocket Subscription**: `/queue/cloth/progress/{userId}` 구독으로 실시간 피드백

### 결과 (Result)

- **사용자 경험 개선**: 즉시 응답 반환, 실시간 진행률 표시
- **서버 부하 감소**: 중복 업로드 방지
- **안정성 향상**: 메시지 손실 방지, 재시도 메커니즘 구현

---

## Case Study #2: 계측 기반 성능 최적화

### 상황 (Situation)

- **현상**: AI 이미지 처리가 평균 **9분** 소요
- **요구사항**: 사용자 대기 시간 최소화 필요

### 과제 (Task)

1. 처리 시간 단축
2. 비용 추가 없이 최적화 (CloudRun 무료 티어 유지)

### 행동 (Action)

#### 1. 계측 (Instrumentation): "추측은 그만, 측정부터"

전체 파이프라인을 **7단계로 분해하고 각 구간 측정**:

```
[1] 이미지 다운로드        : 5초
[2] 배경 제거 (rembg)      : 45초
[3] 세그멘테이션           : 30초
[4] 바운딩 박스 추출       : 3초
[5] 이미지 확장            : 60초
[6] 인페인팅               : 40초
[7] Base64 인코딩 ★        : 188초 (3분 8초) ← 병목 구간!
```

**발견**: Base64 인코딩이 전체 시간의 35%를 차지!

#### 2. Base64 인코딩 최적화

**문제점**:
- PNG 포맷: 무손실 압축 → 파일 크기 큼
- 리사이즈 없음: 2000x2000 이미지도 그대로 인코딩
- 순차 처리: 7개 이미지 × 평균 27초 = 약 3분

**해결책**:

```python
# Before
base64.b64encode(png_bytes).decode('utf-8')

# After
# 1. 리사이즈 (최대 1024px)
# 2. JPEG 포맷 변환 (quality=85)
# 3. 병렬 처리 (ThreadPoolExecutor)

from concurrent.futures import ThreadPoolExecutor

def optimize_image(img):
    img.thumbnail((1024, 1024))
    buffer = io.BytesIO()
    img.save(buffer, format='JPEG', quality=85, optimize=True)
    return base64.b64encode(buffer.getvalue()).decode('utf-8')

with ThreadPoolExecutor(max_workers=4) as executor:
    base64_images = list(executor.map(optimize_image, images))
```

#### 3. removebg 운영 방식 전환

**기존**: FastAPI로 자체 운영 (CloudRun)
- 문제: Cold start 발생, 모델 다운로드 소요시간 발생

**변경**: Hugging Face Space 활용
- HF Space는 커뮤니티에서 관리하는 rembg 서비스
- 저비용으로 고성능 배포 및 Gradio로 성능 향상

**시간 절감**: **5분 절약**

#### 4. AI 파이프라인 CloudRun 배포 최적화

**기존 문제**:
- 전체 파이프라인을 하나의 API로 배포
- 첫 요청 시 모든 모델 로딩 필요 → 20분 소요 (CPU 모델)

**최적화 전략**:
- 파이프라인을 단계별로 독립 API로 분리 배포
- 각 API는 사전 로딩 (pre-warming)
- On-demand로 필요한 API만 호출

**시간 절감**: **20분 → 수 초**

#### 5. 다중 분류 모델 → 단일 이미지 최적화

**기존 문제**:
- 단일 이미지인데도 Gemini API가 다중 분류로 인식
- 4개 이상의 API 호출 발생

**해결책**:
- 단일 이미지 감지 로직 추가
- U2Net 모델 사용으로 단일 이미지 처리

**비용 절감**: **50%**

### 결과 (Result)

#### 처리 시간 개선

| 단계 | Before | After | 개선율 |
|------|--------|-------|--------|
| **전체 파이프라인** | 9분 00초 | 5분 53초 | **34% 단축** |
| **Base64 인코딩** | 3분 08초 | 47초 | **75% 단축** |

#### 비용 절감

| 항목 | Before | After | 절감율 |
|------|--------|-------|--------|
| **총 비용** | $4.74 | $3.10 | **35% 절감** |
| **Gemini API** | - | 50% 절감 | **50%** |

#### 핵심 성과

1. **데이터 기반 최적화**: 추측이 아닌 계측을 통한 병목 구간 발견
2. **다각도 개선**: 인코딩, 배포 방식, 모델 선택 등 종합적 최적화
3. **비용 효율성**: 성능 향상과 동시에 비용 절감 달성

---

## 주요 최적화 항목 요약

### 1. Base64 인코딩 최적화
- 이미지 리사이즈 (최대 1024px)
- PNG → JPEG 변환 (quality=85)
- 병렬 처리 (ThreadPoolExecutor)
- **결과**: 3분 8초 → 47초 (75% 단축)

### 2. AI 파이프라인 배포 최적화
- 파이프라인 단계별 독립 API 배포
- 사전 모델 로딩 (pre-warming)
- **결과**: 20분 → 수 초 (CPU 모델 기준)

### 3. removebg 운영 방식 전환
- 자체 FastAPI → Hugging Face Space
- Gradio API 활용
- **결과**: 5분 절약

### 4. 단일 이미지 처리 최적화
- 다중 분류 모델 → U2Net
- Gemini API 호출 최소화
- **결과**: 50% 비용 절감

---

## 기술적 챌린지와 해결

### 1. 비동기 메시지 처리
- **챌린지**: 동기 HTTP 요청의 타임아웃 문제
- **해결**: RabbitMQ + WebSocket 조합으로 비동기 파이프라인 구축

### 2. 성능 병목 해결
- **챌린지**: 9분의 긴 처리 시간
- **해결**: 계측을 통한 병목 구간 발견 및 최적화 (Base64 인코딩)

### 3. 비용 최적화
- **챌린지**: AI 모델 운영 비용 증가
- **해결**: Hugging Face Space 활용, 단일 이미지 모델 분리, API 호출 최소화

### 4. CloudRun 배포 최적화
- **챌린지**: Cold start와 모델 로딩 시간
- **해결**: 파이프라인 단계별 분리 배포, 사전 로딩

---

## 향후 개선 계획

### 1. 캐싱 전략
- Redis 도입으로 반복 요청 최적화
- 처리된 이미지 캐싱

### 2. 추가 성능 최적화
- 이미지 압축 알고리즘 개선
- 모델 경량화 (quantization)

### 3. 모니터링 강화
- Prometheus + Grafana 도입
- 실시간 성능 지표 추적

### 4. 비용 최적화
- Spot Instance 활용
- Auto-scaling 정책 세분화

---

**마지막 업데이트**: 2026-01-15
**작성자**: ClosetConnect Team
