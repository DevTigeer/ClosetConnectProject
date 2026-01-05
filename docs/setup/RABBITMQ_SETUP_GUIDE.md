# RabbitMQ 기반 옷 처리 파이프라인 설정 가이드

## 개요

Spring Boot 애플리케이션과 Python AI 워커 간의 완전한 비동기 메시지 기반 통신을 위한 RabbitMQ 파이프라인입니다.

## 아키텍처

```
┌──────────────┐     요청 큐          ┌───────────────┐
│   Spring     │ ──────────────────> │  Python       │
│   Backend    │   cloth.processing  │  AI Worker    │
│              │        .queue        │               │
│              │                      │ - rembg       │
│              │                      │ - segmentation│
│              │                      │ - inpainting  │
│              │     응답 큐          │               │
│              │ <────────────────── │               │
└──────────────┘   cloth.result      └───────────────┘
                      .queue
```

### 메시지 흐름

1. **요청 (Spring → Python)**
   - 큐: `cloth.processing.queue`
   - Exchange: `cloth.exchange` (Direct)
   - Routing Key: `cloth.processing`
   - 메시지: `ClothProcessingMessage` (clothId, imageBytes, filename)

2. **응답 (Python → Spring)**
   - 큐: `cloth.result.queue`
   - Exchange: `cloth.exchange` (Direct)
   - Routing Key: `cloth.result`
   - 메시지: `ClothResultMessage` (success, imagePaths, category, etc.)

## 설치 및 실행

### 1. RabbitMQ 설치 및 실행

#### Docker 사용 (권장)
```bash
docker run -d --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:3-management

# Management UI 접속: http://localhost:15672
# 기본 계정: guest / guest
```

#### macOS (Homebrew)
```bash
brew install rabbitmq
brew services start rabbitmq
```

### 2. Python 워커 설정

```bash
cd aiModel

# 가상환경 생성 (권장)
python3 -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# 의존성 설치
pip install -r requirements.txt

# 워커 실행
python cloth_processing_worker.py
```

**출력 예시:**
```
🚀 Initializing pipeline on device: cuda
✅ Pipeline initialized successfully
✅ Connected to RabbitMQ at localhost:5672
🎯 Listening on queue: cloth.processing.queue
Waiting for messages. To exit press CTRL+C
```

### 3. Spring Boot 애플리케이션 실행

```bash
./gradlew bootRun
```

## 환경 변수 설정

### Spring (application.properties)

이미 설정되어 있습니다:
```properties
# RabbitMQ 연결
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

# 큐 설정
rabbitmq.queue.cloth-processing=cloth.processing.queue
rabbitmq.queue.cloth-result=cloth.result.queue
rabbitmq.exchange.cloth=cloth.exchange
rabbitmq.routing-key.cloth-processing=cloth.processing
rabbitmq.routing-key.cloth-result=cloth.result
```

### Python Worker

환경 변수로 오버라이드 가능:
```bash
export RABBITMQ_HOST=localhost
export RABBITMQ_PORT=5672
export RABBITMQ_USERNAME=guest
export RABBITMQ_PASSWORD=guest

python cloth_processing_worker.py
```

## 테스트

### 1. 옷 이미지 업로드 (API 요청)

```bash
curl -X POST http://localhost:8080/api/v1/cloth/upload \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "imageFile=@/path/to/image.jpg" \
  -F "name=테스트 옷"
```

**응답:**
```json
{
  "id": 123,
  "name": "테스트 옷",
  "processingStatus": "PROCESSING",
  "category": null,
  "suggestedCategory": null
}
```

### 2. 처리 상태 확인

```bash
curl -X GET http://localhost:8080/api/v1/cloth/123/status \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**처리 중:**
```json
{
  "clothId": 123,
  "processingStatus": "PROCESSING",
  "currentStep": "옷 영역 분석 중",
  "progressPercentage": 40,
  "suggestedCategory": null
}
```

**처리 완료:**
```json
{
  "clothId": 123,
  "processingStatus": "READY_FOR_REVIEW",
  "currentStep": "처리 완료",
  "progressPercentage": 100,
  "suggestedCategory": "TOP",
  "segmentationLabel": "upper-clothes",
  "segmentedImageUrl": "/uploads/segmented/123.png",
  "inpaintedImageUrl": "/uploads/inpainted/123.png"
}
```

### 3. RabbitMQ Management UI 확인

http://localhost:15672 접속 (guest/guest)

- **Queues 탭**에서 큐 상태 확인
- `cloth.processing.queue`: 요청 메시지 대기
- `cloth.result.queue`: 응답 메시지 대기
- Message rates, consumers 수 확인

## 로그 확인

### Spring Boot
```bash
# 메시지 발행
[ClothUploadedEventListener] Publishing RabbitMQ message after transaction commit

# 결과 수신
[ResultConsumer][123] Received cloth processing result (success: true)
[ResultConsumer][123] Saved segmented image: /uploads/segmented/123.png
[ResultConsumer][123] ✅ Processing completed successfully
```

### Python Worker
```bash
📨 Received message: clothId=123, filename=test.jpg

🔄 Processing clothId: 123
  Step 1/3: Removing background...
  ✅ Background removed
  Step 2/3: Segmenting clothing...
  ✅ Segmented: upper-clothes (125430 pixels)
  Step 3/3: Inpainting image...
  ⚠️  Inpainting skipped (use original cropped image)
✅ Processing completed: TOP

📤 Result sent to cloth.result.queue
✅ Message processed and acknowledged
```

## 트러블슈팅

### RabbitMQ 연결 실패
```
pika.exceptions.AMQPConnectionError
```

**해결:**
1. RabbitMQ가 실행 중인지 확인: `docker ps` 또는 `brew services list`
2. 포트 확인: `netstat -an | grep 5672`
3. 방화벽 확인

### Python 워커가 메시지를 받지 못함
1. 큐 이름 확인 (Spring과 Python이 일치해야 함)
2. Exchange와 Routing Key 확인
3. RabbitMQ Management UI에서 바인딩 확인

### 처리 시간이 너무 오래 걸림
- GPU 사용 확인: `torch.cuda.is_available()`
- Stable Diffusion inpainting은 무거우므로 현재 스킵됨
- 필요시 `cloth_processing_worker.py`의 `inpaint_image()` 구현

### 이미지 파일을 찾을 수 없음
```
Image file not found: /path/to/image.png
```

**해결:**
- Python과 Spring이 같은 파일 시스템 접근 필요
- Docker 사용 시 볼륨 마운트 확인
- 절대 경로 확인

## 성능 튜닝

### Python Worker 병렬 실행

여러 워커를 동시에 실행하여 처리량 증가:
```bash
# Terminal 1
python cloth_processing_worker.py

# Terminal 2
python cloth_processing_worker.py

# Terminal 3
python cloth_processing_worker.py
```

RabbitMQ가 자동으로 메시지를 분산합니다.

### Spring Boot Consumer 동시성

`application.properties`:
```properties
spring.rabbitmq.listener.simple.concurrency=5
spring.rabbitmq.listener.simple.max-concurrency=10
```

## 다음 단계

- [ ] Stable Diffusion Inpainting 통합
- [ ] 에러 핸들링 강화 (DLQ - Dead Letter Queue)
- [ ] 모니터링 및 메트릭 추가
- [ ] 프로덕션 환경 설정 (RabbitMQ 클러스터)
- [ ] 이미지 파일 저장소 통합 (S3 등)
