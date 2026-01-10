# 로컬 테스트 가이드

Hugging Face Space에 배포하기 전에 로컬에서 테스트하는 방법입니다.

## 1. 환경 설정

### Python 환경 생성 (권장)

```bash
cd /Users/grail/Documents/ClosetConnectProject/aiModel/huggingSpace

# Python 가상환경 생성
python3 -m venv venv

# 가상환경 활성화
source venv/bin/activate  # Mac/Linux
# 또는
venv\Scripts\activate  # Windows
```

### 의존성 설치

```bash
pip install -r requirements.txt
```

## 2. 로컬 서버 실행

```bash
python app.py
```

출력:
```
🔥 Loading rembg model...
✅ rembg model loaded in 3.45s
Running on local URL:  http://127.0.0.1:7860

To create a public link, set `share=True` in `launch()`.
```

## 3. 테스트 방법

### 방법 1: Web UI

브라우저에서 접속:
```
http://localhost:7860
```

이미지를 업로드하고 결과 확인

### 방법 2: API 테스트 스크립트

```bash
# 테스트 이미지 준비
# (예: test_image.png)

# API 테스트 실행
python test_api.py test_image.png http://localhost:7860
```

출력:
```
🧪 Testing Background Removal API
   Image: test_image.png
   API URL: http://localhost:7860

📤 Sending request...
✅ Request completed in 2.34s

💾 Result saved: test_image_removed_bg.png
   Size: (512, 512)
   Mode: RGBA

✅ Test passed!
```

### 방법 3: Python 코드로 직접 테스트

```python
import requests
from PIL import Image
import io

API_URL = "http://localhost:7860/remove-bg"

# 이미지 읽기
with open("test_image.png", "rb") as f:
    image_bytes = f.read()

# API 호출
response = requests.post(
    API_URL,
    files={"file": ("image.png", image_bytes, "image/png")},
    timeout=60
)

# 결과 저장 (응답은 PNG 이미지)
output_image = Image.open(io.BytesIO(response.content))
output_image.save("output.png")
print("✅ Saved to output.png")
```

## 4. 성능 확인

### 첫 요청
- 모델 로딩: 3-5초
- 처리 시간: 5-10초 (CPU)
- 총 시간: ~10-15초

### 이후 요청 (세션 재사용)
- 처리 시간: 3-5초 (CPU)
- GPU 사용 시: 1-2초

## 5. CloudRun Worker와 통합 테스트

### Worker 설정

로컬에서 Worker를 실행하고 로컬 Gradio 서버를 사용:

```bash
# .env 파일 또는 환경변수 설정
export REMBG_API_URL=http://localhost:7860
```

### Worker 실행

```bash
cd /Users/grail/Documents/ClosetConnectProject/aiModel

# Worker 실행
python -m src.worker.cloth_processing_worker_cloudrun
```

로그 확인:
```
🚀 Initializing CloudRun API Pipeline
   Segmentation API: http://localhost:8002
   Inpainting API: http://localhost:8003
   Background Removal: Hugging Face API (http://localhost:7860)
✅ Pipeline initialized successfully
```

## 6. 문제 해결

### rembg 모델 다운로드 실패

```bash
# 수동으로 모델 다운로드
python -c "from rembg import remove; import io; from PIL import Image; img = Image.new('RGB', (100,100)); buf = io.BytesIO(); img.save(buf, format='PNG'); remove(buf.getvalue())"
```

### GPU 사용 확인 (NVIDIA GPU가 있는 경우)

```python
import onnxruntime as ort
print(ort.get_available_providers())
# ['CUDAExecutionProvider', 'CPUExecutionProvider'] 이면 GPU 사용 가능
```

GPU 사용을 위해 requirements.txt 수정:
```
onnxruntime-gpu>=1.16.0  # GPU 사용
```

### 포트 충돌

다른 포트로 실행:
```python
# app.py 수정
demo.launch(
    server_port=7861,  # 다른 포트
    ...
)
```

## 7. 다음 단계

로컬 테스트가 성공하면:

1. `DEPLOYMENT.md` 참고하여 Hugging Face Space에 배포
2. CloudRun Worker 환경변수 설정
3. 프로덕션 테스트 진행

## 참고

- GPU 없이 CPU만 사용하는 경우 처리 시간이 느릴 수 있습니다
- 프로덕션에서는 Hugging Face Space의 GPU를 사용하는 것을 권장합니다
- 로컬 테스트는 기능 확인용이며, 성능 측정은 실제 배포 환경에서 진행하세요
