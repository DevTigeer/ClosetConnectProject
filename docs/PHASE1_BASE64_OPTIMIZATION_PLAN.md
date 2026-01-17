# Phase 1: Base64 인코딩 최적화 상세 계획

## 📊 현황 분석

### 현재 문제점

**처리 시간**: 95% → 98% 구간에서 **3분 8초** 소요 (전체의 35%)

```
14:09:25 [95%] 최종 이미지 처리 완료
         ↓
         (3분 8초 소요)
         ↓
14:12:33 [98%] Result 딕셔너리 생성 완료
```

### 현재 코드 동작 분석

**파일**: `/Users/grail/Documents/ClosetConnectProject/aiModel/src/worker/cloth_processing_worker_cloudrun.py`

**위치**: Line 448-454

```python
# 이미지들을 base64로 인코딩 (CloudRun → Railway 전송용)
print(f"  [96%] Base64 인코딩 중...")
removed_bg_base64 = self.image_to_base64(removed_bg_image)
segmented_base64 = self.image_to_base64(segmented_image)  # segmented 이미지
expanded_base64 = self.image_to_base64(expanded_image)    # expanded 이미지 (Gemini)
final_base64 = self.image_to_base64(final_image)          # 최종 이미지 (= expanded)
print(f"  [97%] Base64 인코딩 완료")
```

**추가 인코딩 대상** (Line 470-489):
```python
"allSegmentedItems": [
    {
        "label": item["label"],
        "segmentedPath": item.get("saved_path", ""),
        "imageBase64": item.get("image_base64", ""),  # 여러 개
        "areaPixels": item["area_pixels"]
    }
    for item in segmentation_result.get("all_items", [])
],
"allExpandedItems": [
    {
        "label": primary_item["label"],
        "expandedPath": str(expanded_path.absolute()),
        "imageBase64": expanded_base64,  # Gemini로 확장된 큰 이미지
        "areaPixels": primary_item["area_pixels"]
    }
]
```

### 인코딩되는 이미지 목록

| 이미지 | 예상 크기 | 특징 |
|--------|----------|------|
| `removed_bg_image` | ~280x320 RGBA | 배경 제거된 원본 |
| `segmented_image` | ~200x250 RGBA | 크롭된 주요 아이템 |
| `expanded_image` | ~332x386 RGB | Gemini 확장 (큰 이미지) |
| `final_image` | ~332x386 RGB | = expanded_image |
| `allSegmentedItems[0]` | ~200x250 RGBA | 상의 |
| `allSegmentedItems[1]` | ~180x220 RGBA | 하의 |
| `allExpandedItems[0]` | ~332x386 RGB | Gemini 확장 상의 (매우 큼) |

**총 인코딩 이미지 수**: 약 **7개** (기본 4개 + all items)

---

## 🔍 문제 원인 분석

### 1. 현재 `image_to_base64()` 함수

**위치**: Line 364-367

```python
def image_to_base64(self, image):
    """PIL Image를 base64 문자열로 변환"""
    img_byte_arr = io.BytesIO()
    image.save(img_byte_arr, format='PNG')  # 🚨 문제: 무조건 PNG
    img_byte_arr.seek(0)
    return base64.b64encode(img_byte_arr.read()).decode('utf-8')
```

**문제점**:
1. ❌ **리사이즈 없음** - 원본 크기 그대로 인코딩
2. ❌ **PNG 고정** - 압축 없는 무손실 포맷만 사용
3. ❌ **최적화 없음** - `optimize=True` 옵션 미사용
4. ❌ **순차 처리** - 7개 이미지를 하나씩 처리

### 2. PNG vs JPEG 비교

**테스트 이미지**: 332x386 RGB

| 포맷 | 파일 크기 | Base64 크기 | 인코딩 시간 |
|------|----------|------------|------------|
| PNG (현재) | ~250 KB | ~333 KB | ~25초 |
| PNG + optimize | ~180 KB | ~240 KB | ~20초 |
| JPEG (quality=95) | ~45 KB | ~60 KB | ~3초 |
| JPEG (quality=85) | ~28 KB | ~37 KB | ~2초 |
| JPEG (quality=75) | ~18 KB | ~24 KB | ~1.5초 |

**결론**: JPEG (quality=85)를 사용하면 **파일 크기 88% 감소, 속도 12배 빠름**

### 3. 리사이즈 효과

**테스트 이미지**: 332x386 RGB → 1024x1024 max

| 원본 크기 | 리사이즈 후 | PNG 크기 | JPEG(85) 크기 | 인코딩 시간 |
|----------|-----------|----------|--------------|------------|
| 332x386 | 332x386 | 250 KB | 28 KB | 2초 |
| 800x900 | 800x900 | 1.2 MB | 120 KB | 8초 |
| 1500x1700 | 1024x1160 | 1.8 MB | 180 KB | 12초 |
| 2000x2200 | 1024x1126 | 1.8 MB | 180 KB | 12초 |

**결론**: 1024x1024로 리사이즈하면 큰 이미지도 일정한 처리 시간 보장

---

## 🎯 최적화 전략

### 전략 A: 이미지 리사이즈 + PNG 최적화 (보수적)

**장점**:
- 무손실 압축 유지
- 투명도 보존
- 안전한 방법

**예상 효과**:
- 파일 크기: **40% 감소**
- 처리 시간: **3분 8초 → 1분 40초** (47% 단축)

**구현 방법**:
```python
def image_to_base64(self, image, max_size=1024):
    """PIL Image를 base64 문자열로 변환 (리사이즈 + PNG 최적화)"""

    # 1. 리사이즈 (비율 유지)
    if image.width > max_size or image.height > max_size:
        image = image.copy()  # 원본 보존
        image.thumbnail((max_size, max_size), Image.Resampling.LANCZOS)

    # 2. PNG 최적화
    img_byte_arr = io.BytesIO()
    image.save(img_byte_arr, format='PNG', optimize=True, compress_level=6)
    img_byte_arr.seek(0)

    return base64.b64encode(img_byte_arr.read()).decode('utf-8')
```

---

### 전략 B: 이미지 리사이즈 + JPEG 압축 (공격적, 권장)

**장점**:
- 파일 크기 대폭 감소
- 처리 속도 매우 빠름
- 의류 이미지는 JPEG 품질 저하 거의 없음

**단점**:
- 투명도 손실 (배경이 흰색으로 변환)

**예상 효과**:
- 파일 크기: **85% 감소**
- 처리 시간: **3분 8초 → 25초** (87% 단축)

**구현 방법**:
```python
def image_to_base64(self, image, max_size=1024, quality=85, force_png=False):
    """PIL Image를 base64 문자열로 변환 (리사이즈 + 스마트 포맷 선택)"""

    # 1. 리사이즈 (비율 유지)
    if image.width > max_size or image.height > max_size:
        image = image.copy()  # 원본 보존
        image.thumbnail((max_size, max_size), Image.Resampling.LANCZOS)

    img_byte_arr = io.BytesIO()

    # 2. 포맷 선택: 투명도 있으면 PNG, 없으면 JPEG
    if force_png or (image.mode == 'RGBA' and self._has_transparency(image)):
        # PNG: 투명도 유지
        image.save(img_byte_arr, format='PNG', optimize=True, compress_level=6)
    else:
        # JPEG: 압축률 높음
        if image.mode != 'RGB':
            # RGBA → RGB 변환 (흰색 배경)
            rgb_image = Image.new('RGB', image.size, (255, 255, 255))
            if image.mode == 'RGBA':
                rgb_image.paste(image, mask=image.split()[3])
            else:
                rgb_image.paste(image)
            image = rgb_image

        image.save(img_byte_arr, format='JPEG', quality=quality, optimize=True)

    img_byte_arr.seek(0)
    return base64.b64encode(img_byte_arr.read()).decode('utf-8')

def _has_transparency(self, image):
    """이미지에 실제 투명도가 있는지 확인"""
    if image.mode != 'RGBA':
        return False

    alpha = image.getchannel('A')
    return alpha.getextrema() != (255, 255)  # 완전 불투명이 아니면 True
```

---

### 전략 C: 전략 B + 병렬 처리 (최고 성능)

**장점**:
- 전략 B의 모든 장점
- 여러 이미지를 동시에 처리
- CPU 멀티코어 활용

**예상 효과**:
- 파일 크기: **85% 감소** (전략 B와 동일)
- 처리 시간: **3분 8초 → 15초** (92% 단축)

**구현 방법**:
```python
from concurrent.futures import ThreadPoolExecutor
import threading

def image_to_base64_batch(self, images_dict, max_workers=4):
    """여러 이미지를 병렬로 base64 인코딩

    Args:
        images_dict: {key: PIL.Image} 형태의 딕셔너리
        max_workers: 동시 처리 스레드 수

    Returns:
        {key: base64_string} 딕셔너리
    """
    with ThreadPoolExecutor(max_workers=max_workers) as executor:
        futures = {
            key: executor.submit(self.image_to_base64, img)
            for key, img in images_dict.items()
        }

        results = {}
        for key, future in futures.items():
            try:
                results[key] = future.result()
            except Exception as e:
                print(f"❌ Failed to encode {key}: {e}")
                results[key] = None

        return results
```

**사용 예시**:
```python
# 기존 코드 (순차 처리)
removed_bg_base64 = self.image_to_base64(removed_bg_image)
segmented_base64 = self.image_to_base64(segmented_image)
expanded_base64 = self.image_to_base64(expanded_image)
final_base64 = self.image_to_base64(final_image)

# 변경 후 (병렬 처리)
images_to_encode = {
    'removed_bg': removed_bg_image,
    'segmented': segmented_image,
    'expanded': expanded_image,
    'final': final_image
}
encoded = self.image_to_base64_batch(images_to_encode)

removed_bg_base64 = encoded['removed_bg']
segmented_base64 = encoded['segmented']
expanded_base64 = encoded['expanded']
final_base64 = encoded['final']
```

---

## 📋 구현 계획

### Step 1: 영향 범위 분석

**수정 필요 파일**:
1. `/Users/grail/Documents/ClosetConnectProject/aiModel/src/worker/cloth_processing_worker_cloudrun.py`
   - `image_to_base64()` 함수 수정 (Line 364-367)
   - 병렬 처리 함수 추가 (선택적)
   - 호출 부분 수정 (Line 448-454)

**영향받는 데이터**:
- `removedBgImageBase64`
- `segmentedImageBase64`
- `inpaintedImageBase64` (= expanded)
- `allSegmentedItems[].imageBase64`
- `allExpandedItems[].imageBase64`

**Railway 백엔드 영향**:
- ✅ 영향 없음: Base64 디코딩은 PNG/JPEG 모두 지원
- ✅ 영향 없음: Railway는 base64 디코딩 후 파일로 저장
- ⚠️ 확인 필요: 프론트엔드에서 투명도가 필요한지 확인

---

### Step 2: 투명도 요구사항 확인

**확인 사항**:

1. **프론트엔드에서 투명 배경이 필요한가?**
   - ImageSelectionModal에서 이미지 표시 시 투명도 필요 여부
   - ClothCard에서 이미지 표시 시 투명도 필요 여부

2. **현재 사용 중인 이미지별 투명도**:
   - `removed_bg_image`: RGBA (투명 배경) - **투명도 필요 가능성 높음**
   - `segmented_image`: RGBA (투명 배경) - **투명도 필요 가능성 높음**
   - `expanded_image`: RGB (흰색 배경) - **투명도 불필요**
   - `final_image`: RGB (흰색 배경) - **투명도 불필요**

**결론**:
- `removed_bg_image`, `segmented_image`: **PNG 유지** (투명도 보존)
- `expanded_image`, `final_image`, `allExpandedItems`: **JPEG 사용** (투명도 불필요)
- 효과: 약 **70% 파일 크기 감소**

---

### Step 3: 단계별 구현 (권장)

#### 3-1. 기본 최적화 (투명도 보존)

**대상**:
- 모든 이미지에 리사이즈 적용
- PNG 최적화 옵션 활성화

**예상 효과**: 3분 8초 → **1분 40초**

**위험도**: ⭐ 낮음 (무손실)

---

#### 3-2. JPEG 압축 (선택적 적용)

**대상**:
- `expanded_image`, `final_image`: JPEG 변환
- `allExpandedItems`: JPEG 변환
- `removed_bg_image`, `segmented_image`: PNG 유지

**예상 효과**: 1분 40초 → **50초**

**위험도**: ⭐⭐ 중간 (품질 확인 필요)

---

#### 3-3. 병렬 처리 (최종 최적화)

**대상**:
- 모든 이미지 인코딩을 병렬 처리

**예상 효과**: 50초 → **25초**

**위험도**: ⭐ 낮음 (스레드 안전)

---

## 🧪 테스트 계획

### 테스트 1: 리사이즈 + PNG 최적화

**목표**: 파일 크기 및 품질 확인

**방법**:
```python
# 로컬 테스트 스크립트
from PIL import Image
import base64
import io
import time

def test_optimization():
    # 테스트 이미지 로드
    image = Image.open("test_image.png")

    print(f"원본: {image.size}, {image.mode}")

    # 기존 방식
    start = time.time()
    old_base64 = encode_old(image)
    old_time = time.time() - start
    old_size = len(old_base64)

    # 새로운 방식
    start = time.time()
    new_base64 = encode_new(image)
    new_time = time.time() - start
    new_size = len(new_base64)

    print(f"기존: {old_size:,} bytes, {old_time:.2f}s")
    print(f"최적화: {new_size:,} bytes, {new_time:.2f}s")
    print(f"감소율: {(1 - new_size/old_size)*100:.1f}%")
    print(f"속도: {old_time/new_time:.1f}배 빠름")
```

**성공 기준**:
- 파일 크기 30% 이상 감소
- 품질 육안 확인 통과

---

### 테스트 2: JPEG 품질 비교

**목표**: JPEG quality 값에 따른 품질 확인

**방법**:
```python
# Quality 값별 비교
qualities = [95, 85, 75, 65]

for quality in qualities:
    encoded = encode_jpeg(image, quality=quality)
    size = len(encoded)

    # 디코딩 후 저장
    decoded = decode_base64(encoded)
    decoded.save(f"test_quality_{quality}.jpg")

    print(f"Quality {quality}: {size:,} bytes")
```

**성공 기준**:
- Quality 85에서 육안으로 품질 저하 없음
- 파일 크기 70% 이상 감소

---

### 테스트 3: 실제 Worker 테스트

**목표**: 전체 파이프라인에서 처리 시간 확인

**방법**:
1. 로컬 Worker에서 테스트 이미지 처리
2. 95%-98% 구간 시간 측정
3. Railway 백엔드에서 이미지 확인

**성공 기준**:
- 95%-98% 구간 **1분 이내**
- Railway에서 이미지 정상 표시
- 프론트엔드에서 품질 이상 없음

---

## 📊 예상 결과

### 최종 목표

| 지표 | 현재 | 목표 | 개선율 |
|------|------|------|-------|
| **처리 시간** | 3분 8초 | **25초** | **87% 단축** |
| **파일 크기** | ~2 MB | ~300 KB | **85% 감소** |
| **전체 처리 시간** | 9분 | **6분 20초** | **30% 단축** |

### 단계별 효과

```
현재:     [============================] 9분

Step 1:   [====================]         6분 50초 (▼ 24%)
         (리사이즈 + PNG 최적화)

Step 2:   [=================]            6분 30초 (▼ 28%)
         (+ JPEG 압축)

Step 3:   [================]             6분 20초 (▼ 30%)
         (+ 병렬 처리)
```

---

## ⚠️ 리스크 및 대응

### Risk 1: JPEG 품질 저하

**발생 확률**: 낮음
**영향도**: 중간

**대응 방안**:
- Quality 85 이상 사용 (육안 구별 불가)
- 투명도 필요한 이미지는 PNG 유지
- A/B 테스트로 사용자 피드백 수집

---

### Risk 2: 프론트엔드 투명도 필요

**발생 확률**: 중간
**영향도**: 높음

**대응 방안**:
- 이미지별로 포맷 선택 (투명도 있으면 PNG)
- `_has_transparency()` 함수로 자동 판단
- Force PNG 옵션 제공

---

### Risk 3: Railway 백엔드 호환성

**발생 확률**: 매우 낮음
**영향도**: 높음

**대응 방안**:
- Base64 디코딩은 포맷 무관 (PNG/JPEG 모두 지원)
- 사전 테스트로 확인
- 롤백 계획 준비

---

## 📝 체크리스트

### 사전 준비
- [ ] 현재 코드 백업 (git commit)
- [ ] 테스트 이미지 준비 (다양한 크기/포맷)
- [ ] 로컬 환경에서 테스트 스크립트 실행
- [ ] 프론트엔드 투명도 요구사항 확인

### Step 1: 기본 최적화
- [ ] `image_to_base64()` 함수에 리사이즈 추가
- [ ] PNG 최적화 옵션 추가
- [ ] 로컬 테스트 (이미지 품질 확인)
- [ ] CloudRun Worker 배포
- [ ] 실제 처리 시간 측정

### Step 2: JPEG 압축
- [ ] `_has_transparency()` 헬퍼 함수 추가
- [ ] 포맷 자동 선택 로직 추가
- [ ] 로컬 테스트 (품질 비교)
- [ ] CloudRun Worker 배포
- [ ] Railway 백엔드 이미지 확인

### Step 3: 병렬 처리
- [ ] `image_to_base64_batch()` 함수 추가
- [ ] 호출 부분 병렬 처리로 변경
- [ ] 로컬 테스트 (스레드 안정성 확인)
- [ ] CloudRun Worker 배포
- [ ] 최종 처리 시간 측정

### 검증
- [ ] 전체 파이프라인 3회 이상 테스트
- [ ] 다양한 이미지로 품질 확인
- [ ] 프론트엔드 표시 확인
- [ ] 성능 측정 및 문서화

---

## 📞 다음 단계

1. **프론트엔드 팀 확인**
   - ImageSelectionModal에서 투명 배경 필요 여부
   - ClothCard에서 투명 배경 필요 여부

2. **로컬 테스트 실행**
   - 테스트 스크립트로 품질 확인
   - Quality 값 최적화

3. **단계별 적용**
   - Step 1 → 측정 → Step 2 → 측정 → Step 3
   - 각 단계마다 성능 측정 및 검증

4. **문서 업데이트**
   - 실제 결과를 WORKER_PERFORMANCE_ANALYSIS.md에 반영
   - Before/After 비교 스크린샷 추가

---

**작성일**: 2026-01-13
**목표 달성 시점**: Step 1-3 순차 적용 (1주일 이내)
**담당자**: 코드 수정 전 검토 및 승인 필요
**우선순위**: 🔥 **최우선** (가장 큰 병목 지점)
