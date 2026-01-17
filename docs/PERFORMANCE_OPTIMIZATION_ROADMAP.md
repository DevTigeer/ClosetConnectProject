# Worker 성능 최적화 로드맵

## 📊 현재 상태 (Phase 1 완료 후)

**총 처리 시간**: **5분 53초**

| 단계 | 소요 시간 | 비율 | 상태 |
|------|----------|------|------|
| 1. 배경 제거 (HF Space) | 2분 17초 | 39% | 🔴 **최대 병목** |
| 2. Segmentation | 12초 | 3% | ✅ 양호 |
| 3. Imagen (Gemini) | 2분 31초 | 43% | 🔴 **최대 병목** |
| 4. Base64 인코딩 | 47초 | 13% | ✅ 최적화 완료 |
| 5. 결과 전송 | 0초 | 0% | ✅ 양호 |

**다음 목표**: 배경 제거와 Imagen 최적화 → **3분대 진입**

---

## 🎯 Phase 2: 프론트엔드 최적화 (무료, 중간 난이도)

### 목표: 5분 53초 → **4분 30초** (23% 단축)

### 2-1. 프론트엔드 이미지 리사이즈 ⭐⭐⭐

**예상 효과**: 배경 제거 2분 17초 → **1분 10초** (50% 단축)

**이유**:
- 현재 사용자가 고해상도 이미지 업로드 (예: 4000x3000)
- Worker가 큰 이미지를 그대로 처리
- HF Space 전송 시간과 처리 시간 모두 증가

**구현 방법**:

**파일**: `/Users/grail/Documents/ClosetConnectProject/frontend/ClosetConnectProject/src/components/AddClothModal.jsx`

```javascript
// 이미지 리사이즈 함수 추가
const resizeImage = (file, maxSize = 1200) => {
  return new Promise((resolve) => {
    const reader = new FileReader();

    reader.onload = (e) => {
      const img = new Image();

      img.onload = () => {
        const canvas = document.createElement('canvas');
        let width = img.width;
        let height = img.height;

        // 최대 크기 제한
        if (width > maxSize || height > maxSize) {
          if (width > height) {
            height = (height / width) * maxSize;
            width = maxSize;
          } else {
            width = (width / height) * maxSize;
            height = maxSize;
          }
        }

        canvas.width = width;
        canvas.height = height;

        const ctx = canvas.getContext('2d');
        ctx.drawImage(img, 0, 0, width, height);

        // JPEG로 변환 (quality 0.9)
        canvas.toBlob(
          (blob) => resolve(blob),
          'image/jpeg',
          0.9
        );
      };

      img.src = e.target.result;
    };

    reader.readAsDataURL(file);
  });
};

// 파일 선택 시 자동 리사이즈
const handleFileChange = async (e) => {
  const file = e.target.files[0];
  if (!file) return;

  try {
    // 리사이즈 진행 표시
    setIsOptimizing(true);

    // 이미지 최적화
    const optimizedFile = await resizeImage(file, 1200);

    // 최적화된 파일로 교체
    const optimizedFileObj = new File(
      [optimizedFile],
      file.name,
      { type: 'image/jpeg' }
    );

    setSelectedFile(optimizedFileObj);
    setIsOptimizing(false);

    console.log(`이미지 최적화: ${(file.size / 1024).toFixed(0)}KB → ${(optimizedFile.size / 1024).toFixed(0)}KB`);
  } catch (error) {
    console.error('이미지 최적화 실패:', error);
    setSelectedFile(file); // 실패 시 원본 사용
    setIsOptimizing(false);
  }
};
```

**UI 개선**:
```javascript
{isOptimizing && (
  <div className="text-sm text-gray-500 mt-2">
    🔄 이미지 최적화 중... (처리 속도가 빨라집니다)
  </div>
)}
```

**효과**:
- 업로드 파일 크기: 5MB → 300KB (95% 감소)
- HF Space 전송: 41초 → 5초
- rembg 처리: 42초 → 20초
- 배경 제거 전체: **2분 17초 → 1분 10초**

**비용**: 무료 (클라이언트에서 처리)

**난이도**: ⭐⭐ 중간

---

### 2-2. HF Space Warm-up 유지

**예상 효과**: Cold start 40초 → **5초** (35초 단축)

**방법 1: Railway Cron Job**

**파일**: `/Users/grail/Documents/ClosetConnectProject/src/main/java/com/tigger/closetconnectproject/Scheduler/HuggingFaceWarmUpScheduler.java`

```java
@Component
@Slf4j
public class HuggingFaceWarmUpScheduler {

    @Value("${huggingface.space.url:https://tigger13-background-removal.hf.space}")
    private String hfSpaceUrl;

    // 5분마다 실행
    @Scheduled(fixedRate = 300000)
    public void keepWarm() {
        try {
            log.info("Sending warm-up request to HF Space: {}", hfSpaceUrl);

            RestTemplate restTemplate = new RestTemplate();
            restTemplate.getForEntity(hfSpaceUrl, String.class);

            log.info("HF Space warm-up successful");
        } catch (Exception e) {
            log.warn("HF Space warm-up failed: {}", e.getMessage());
        }
    }
}
```

**application.yml 설정**:
```yaml
huggingface:
  space:
    url: https://tigger13-background-removal.hf.space
```

**방법 2: GitHub Actions (무료)**

**파일**: `.github/workflows/hf-warmup.yml`

```yaml
name: HuggingFace Space Warm-up

on:
  schedule:
    # 5분마다 실행
    - cron: '*/5 * * * *'

jobs:
  warmup:
    runs-on: ubuntu-latest
    steps:
      - name: Send warm-up request
        run: |
          curl -X GET https://tigger13-background-removal.hf.space/ || true
```

**효과**:
- Cold start 제거
- 배경 제거 2분 17초 → **1분 42초**

**비용**: 무료

**난이도**: ⭐ 쉬움

---

## 🚀 Phase 3: 서비스 업그레이드 (유료, 높은 효과)

### 목표: 4분 30초 → **2분 이내** (50% 이상 단축)

### 3-1. Remove.bg API 도입 ⭐⭐⭐⭐⭐

**예상 효과**: 배경 제거 1분 10초 → **10초** (85% 단축)

**비교**:

| 서비스 | 처리 시간 | 품질 | 비용 (1000회/월) |
|--------|----------|------|-----------------|
| **HF Space (현재)** | 2분 17초 | ⭐⭐⭐⭐ | 무료 |
| **Remove.bg** | 3-10초 | ⭐⭐⭐⭐⭐ | $200 |
| **Clipdrop** | 3-7초 | ⭐⭐⭐⭐⭐ | $150 |
| **Photoroom** | 5-8초 | ⭐⭐⭐⭐ | $99 |

**권장**: Remove.bg (가장 빠르고 품질 우수)

**구현 방법**:

**Worker 수정**: `/Users/grail/Documents/ClosetConnectProject/aiModel/src/worker/cloth_processing_worker_cloudrun.py`

```python
def remove_background(self, image_bytes):
    """배경 제거 (Remove.bg API 또는 HF Space)"""

    # 환경 변수로 선택
    use_removebg = os.getenv("USE_REMOVEBG_API", "false").lower() == "true"
    removebg_api_key = os.getenv("REMOVEBG_API_KEY")

    if use_removebg and removebg_api_key:
        return self._remove_bg_with_api(image_bytes, removebg_api_key)
    else:
        return self._remove_bg_with_huggingface(image_bytes)

def _remove_bg_with_api(self, image_bytes, api_key):
    """Remove.bg API 사용"""
    print("  Step 1/4: Removing background with Remove.bg API...")

    try:
        response = requests.post(
            'https://api.remove.bg/v1.0/removebg',
            files={'image_file': image_bytes},
            data={'size': 'auto'},
            headers={'X-Api-Key': api_key},
            timeout=30
        )
        response.raise_for_status()

        image = Image.open(io.BytesIO(response.content)).convert("RGBA")
        print(f"  ✅ Background removed (Remove.bg API): {image.size}, {image.mode}")
        return image

    except Exception as e:
        print(f"  ⚠️  Remove.bg API failed: {e}")
        print("  Falling back to HuggingFace...")
        return self._remove_bg_with_huggingface(image_bytes)

def _remove_bg_with_huggingface(self, image_bytes):
    """HuggingFace Space 사용 (기존 코드)"""
    # 기존 HF Space 코드...
```

**환경 변수 설정**:
```bash
USE_REMOVEBG_API=true
REMOVEBG_API_KEY=your_api_key_here
```

**비용**:
- Remove.bg: $0.20/image
- 월 1000회: $200
- 하지만 처리 속도 **30배 빠름**

**ROI 계산**:
```
시간 가치 계산:
- 사용자당 2분 절약 × 1000회 = 33시간 절약
- 사용자 이탈률 감소로 인한 수익 증가
- CloudRun 실행 시간 단축 → 비용 절감
```

**난이도**: ⭐⭐ 중간

---

### 3-2. Gemini 최적화 또는 대체

**예상 효과**: Imagen 2분 31초 → **30초** (80% 단축)

#### 옵션 A: Gemini를 선택적으로 사용

```python
def process(self, cloth_id, user_id, image_bytes, image_type, worker=None):
    # ... 기존 코드 ...

    # Gemini를 사용자 선택 또는 특정 조건에서만 실행
    use_imagen = os.getenv("USE_IMAGEN", "true").lower() == "true"

    if use_imagen and self._should_use_imagen(segmented_image):
        # Gemini 확장 실행
        expanded_image = self.imagen_service.expand_image(segmented_image)
    else:
        # Gemini 스킵
        expanded_image = segmented_image
        print(f"  [55%] Imagen 스킵 (이미지 크기 충분)")

def _should_use_imagen(self, image):
    """Gemini가 필요한지 판단"""
    width, height = image.size

    # 이미지가 충분히 크면 스킵
    if width >= 300 and height >= 300:
        return False

    # 이미지가 작으면 확장 필요
    return True
```

**효과**:
- 불필요한 Gemini 호출 제거
- 조건부 실행으로 **평균 50% 사용 감소**
- 비용: 무료

#### 옵션 B: DALL-E 3 Outpainting으로 대체

```python
def expand_image_with_dalle(self, image_pil):
    """DALL-E 3 Outpainting 사용"""

    # OpenAI API 호출
    response = openai.Image.create_variation(
        image=image_pil,
        n=1,
        size="1024x1024"
    )

    # 결과 반환
    result_url = response['data'][0]['url']
    result_image = Image.open(requests.get(result_url, stream=True).raw)
    return result_image
```

**비교**:

| 서비스 | 처리 시간 | 비용 | 품질 |
|--------|----------|------|------|
| **Gemini (현재)** | 2분 31초 | 무료 (quota) | ⭐⭐⭐⭐ |
| **DALL-E 3** | 10-20초 | $0.04/image | ⭐⭐⭐⭐⭐ |
| **Stable Diffusion (self-hosted)** | 30-60초 | CloudRun GPU 비용 | ⭐⭐⭐⭐ |
| **없음 (스킵)** | 0초 | 무료 | N/A |

**난이도**: ⭐⭐⭐ 높음

---

## 📈 Phase별 예상 결과

### Phase 2 적용 후

```
Phase 1 후:  [=============]        5분 53초
Phase 2 후:  [=========]            4분 30초 (23% 단축)

변화:
- 배경 제거: 2분 17초 → 1분 10초 (프론트엔드 리사이즈)
- HF Cold start: 40초 → 5초 (Warm-up)
- 기타: 동일
```

### Phase 3 적용 후

```
Phase 2 후:  [=========]            4분 30초
Phase 3 후:  [====]                 1분 50초 (59% 단축)

변화:
- 배경 제거: 1분 10초 → 10초 (Remove.bg)
- Imagen: 2분 31초 → 30초 (조건부 실행 + 최적화)
- 기타: 동일
```

### 최종 목표 달성

```
최초:       [====================] 9분 00초
Phase 1 후: [=============]        5분 53초 (34% ↓)
Phase 2 후: [=========]            4분 30초 (50% ↓)
Phase 3 후: [====]                 1분 50초 (80% ↓)
```

---

## 💰 비용 분석

### Phase 2 (무료)

| 항목 | 비용 | 효과 |
|------|------|------|
| 프론트엔드 리사이즈 | 무료 | 1분 7초 단축 |
| HF Warm-up (GitHub Actions) | 무료 | 35초 단축 |
| **총 비용** | **무료** | **1분 23초 단축** |

**ROI**: ♾️ (무한대) - 무료로 23% 개선

---

### Phase 3 (유료)

**월 1000회 기준**:

| 항목 | 비용 | 효과 |
|------|------|------|
| Remove.bg API | $200/월 | 1분 단축 |
| Gemini 최적화 (조건부) | 무료 | 2분 단축 |
| **총 비용** | **$200/월** | **3분 단축** |

**총 비용**:
- 기존 CloudRun: $3.10/월
- Phase 3 추가: $200/월
- **합계**: $203.10/월

**ROI 분석**:
```
사용자 경험 개선:
- 처리 시간: 6분 → 2분 (70% 단축)
- 이탈률 감소: 예상 30-50%
- 사용자 만족도: 대폭 증가

비용 대비 효과:
- $200/월로 3분 단축
- 분당 비용: $66
- 사용자 1명당: $0.20
```

**서비스 성장 단계별 권장**:
- **초기 (무료 사용자 많음)**: Phase 2까지만 (무료)
- **성장기 (유료 전환 목표)**: Phase 3 부분 적용 (Remove.bg만)
- **확장기 (수익 안정)**: Phase 3 전체 적용

---

## 🎯 우선순위 가이드

### 즉시 적용 (Phase 2)

**1순위: 프론트엔드 이미지 리사이즈** ⭐⭐⭐⭐⭐
- 비용: 무료
- 효과: 1분 7초 단축
- 난이도: 중간
- 추가 이점: 업로드 속도도 빨라짐

**2순위: HF Space Warm-up** ⭐⭐⭐
- 비용: 무료
- 효과: 35초 단축
- 난이도: 쉬움
- 구현: GitHub Actions 또는 Railway Scheduler

---

### 서비스 성장 후 검토 (Phase 3)

**3순위: Gemini 조건부 실행** ⭐⭐⭐⭐
- 비용: 무료
- 효과: 1분 이상 단축 (평균)
- 난이도: 낮음
- 사용자 경험 영향 최소

**4순위: Remove.bg API** ⭐⭐⭐⭐⭐
- 비용: $200/월
- 효과: 1분 단축
- 난이도: 중간
- 품질도 개선됨

---

## 📋 단계별 실행 계획

### Week 1-2: Phase 2-1 (프론트엔드 리사이즈)

**Day 1-2**: 개발
- [ ] AddClothModal.jsx에 리사이즈 함수 추가
- [ ] UI 피드백 추가 ("이미지 최적화 중...")
- [ ] 로컬 테스트

**Day 3-4**: 테스트
- [ ] 다양한 이미지 크기로 테스트
- [ ] 품질 확인 (육안 검사)
- [ ] 에러 처리 확인

**Day 5**: 배포
- [ ] 프론트엔드 배포
- [ ] 실제 사용자 테스트
- [ ] 처리 시간 측정

---

### Week 3: Phase 2-2 (HF Warm-up)

**Day 1**: 구현
- [ ] GitHub Actions workflow 작성
- [ ] 또는 Railway Scheduler 설정

**Day 2**: 모니터링
- [ ] Warm-up 작동 확인
- [ ] Cold start 발생 빈도 측정

---

### Week 4: 성과 측정 및 분석

- [ ] Phase 2 전체 효과 측정
- [ ] 사용자 피드백 수집
- [ ] Phase 3 진행 여부 결정

---

### 향후 (Phase 3): 수익성 확보 후

**조건**:
- 월간 활성 사용자 1000명 이상
- 또는 유료 전환율 목표 달성
- 또는 투자 유치 후

**우선 적용**:
1. Gemini 조건부 실행 (무료)
2. Remove.bg API 테스트 (소량)
3. ROI 확인 후 전면 적용

---

## 🔍 추가 최적화 아이디어

### 1. 이미지 캐싱

**아이디어**: 같은 이미지를 재처리하는 경우 캐시 사용

**구현**:
```python
import hashlib

def get_image_hash(image_bytes):
    return hashlib.md5(image_bytes).hexdigest()

def process_with_cache(self, cloth_id, user_id, image_bytes, ...):
    # 이미지 해시 계산
    image_hash = get_image_hash(image_bytes)

    # Redis에서 캐시 확인
    cached_result = redis_client.get(f"cloth:result:{image_hash}")
    if cached_result:
        print(f"  ✅ Cache hit! Returning cached result")
        return json.loads(cached_result)

    # 캐시 없으면 정상 처리
    result = self.process(cloth_id, user_id, image_bytes, ...)

    # 결과 캐싱 (24시간)
    redis_client.setex(
        f"cloth:result:{image_hash}",
        86400,
        json.dumps(result)
    )

    return result
```

**효과**: 재처리 시 **즉시 응답** (0초)

---

### 2. 병렬 처리 (고급)

**아이디어**: Segmentation과 Imagen을 병렬 실행

**주의**: 복잡하고 디버깅 어려움, Phase 3 이후 검토

---

### 3. CDN 사용

**아이디어**: 이미지 저장을 Railway 디스크 대신 S3/GCS + CloudFront

**효과**: 이미지 로딩 속도 개선 (Worker 성능과는 무관)

---

## 📊 성능 모니터링

### 지표 추적

**추가 로그**:
```python
# 각 단계 시작/종료 시간 기록
import time

def process(self, ...):
    metrics = {
        'cloth_id': cloth_id,
        'start_time': time.time(),
        'stages': {}
    }

    # Stage 1
    stage_start = time.time()
    removed_bg_image = self.remove_background(image_bytes)
    metrics['stages']['remove_bg'] = time.time() - stage_start

    # Stage 2
    stage_start = time.time()
    segmentation_result = self.segment_clothing_api(removed_bg_image)
    metrics['stages']['segmentation'] = time.time() - stage_start

    # ... 기타 단계들

    # 최종 메트릭 전송 (Railway로)
    self.send_metrics(metrics)
```

**Railway 백엔드에서 집계**:
- 평균 처리 시간
- 각 단계별 소요 시간
- 병목 지점 식별

---

## 🎓 학습 포인트

### 성능 최적화 원칙

1. **측정 없이 최적화 금지**: 항상 Before/After 측정
2. **병목 지점부터**: 가장 느린 부분부터 최적화
3. **ROI 고려**: 비용 대비 효과 분석
4. **점진적 적용**: Phase별로 나누어 적용
5. **사용자 영향 최소화**: 에러 처리 및 폴백 준비

---

## 📞 다음 단계

### 1. 즉시 시작 (Phase 2-1)
- [ ] 프론트엔드 리사이즈 구현
- [ ] 로컬 테스트
- [ ] 효과 측정

### 2. 1주일 내 (Phase 2-2)
- [ ] HF Warm-up 설정
- [ ] 모니터링

### 3. 1개월 내
- [ ] Phase 2 효과 분석
- [ ] Phase 3 진행 여부 결정
- [ ] ROI 계산

---

**작성일**: 2026-01-13
**현재 상태**: Phase 1 완료 (Base64 최적화)
**다음 목표**: Phase 2 적용 (프론트엔드 리사이즈 + HF Warm-up)
**장기 목표**: 2분 이내 처리 시간 달성
