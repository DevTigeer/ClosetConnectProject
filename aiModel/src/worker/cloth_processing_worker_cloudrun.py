"""
RabbitMQ Cloth Processing Worker (CloudRun API Version)
- CloudRun에 배포된 AI API들을 호출하여 처리
- rembg (Hugging Face Space) → Segmentation APIs → Google AI Imagen (Expand)
- ImageType 분기: FULL_BODY (Segformer API) / SINGLE_ITEM (U2NET API)
- 모든 AI 모델은 별도 API로 분리되어 있음 (초경량 Worker)
- HTTP 헬스체크 서버 포함 (Cloud Run 배포용)
"""

import pika
import json
import base64
import os
import sys
import traceback
import requests
import time
from pathlib import Path
from PIL import Image
import io
from urllib.parse import urlparse
from datetime import datetime
from dotenv import load_dotenv
import threading
from flask import Flask, jsonify

# .env 파일에서 환경변수 로드
load_dotenv()

# Google AI Imagen 서비스 import (선택적)
try:
    from ..services.imagen_service import GoogleAIImagenService
    IMAGEN_AVAILABLE = True
except ImportError:
    IMAGEN_AVAILABLE = False
    print("⚠️  Google AI Imagen 서비스를 사용할 수 없습니다.")

# 프로젝트 루트 디렉토리
# __file__ = /app/src/worker/cloth_processing_worker_cloudrun.py
# .parent.parent.parent = /app (Dockerfile WORKDIR)
PROJECT_ROOT = Path(__file__).parent.parent.parent
OUTPUTS_DIR = PROJECT_ROOT / "outputs"
SEGMENTED_DIR = OUTPUTS_DIR / "segmented_clothes"
REMOVED_BG_DIR = OUTPUTS_DIR / "removed_bg"
EXPANDED_DIR = OUTPUTS_DIR / "expanded"
# INPAINTED_DIR = OUTPUTS_DIR / "inpainted"  # Stable Diffusion - 미사용

# 디렉토리 생성
SEGMENTED_DIR.mkdir(parents=True, exist_ok=True)
REMOVED_BG_DIR.mkdir(parents=True, exist_ok=True)
EXPANDED_DIR.mkdir(parents=True, exist_ok=True)
# INPAINTED_DIR.mkdir(parents=True, exist_ok=True)  # Stable Diffusion - 미사용

# RabbitMQ 설정
RABBITMQ_HOST = os.getenv("RABBITMQ_HOST", "localhost")
RABBITMQ_PORT = int(os.getenv("RABBITMQ_PORT", "5672"))
RABBITMQ_USER = os.getenv("RABBITMQ_USERNAME", "guest")
RABBITMQ_PASS = os.getenv("RABBITMQ_PASSWORD", "guest")

REQUEST_QUEUE = "cloth.processing.queue"
RESULT_QUEUE = "cloth.result.queue"
PROGRESS_QUEUE = "cloth.progress.queue"
EXCHANGE = "cloth.exchange"
REQUEST_ROUTING_KEY = "cloth.processing"
RESULT_ROUTING_KEY = "cloth.result"
PROGRESS_ROUTING_KEY = "cloth.progress"

# CloudRun API URLs
SEGMENTATION_API_URL = os.getenv("SEGMENTATION_API_URL", "http://localhost:8002")  # Segformer (FULL_BODY)
U2NET_API_URL = os.getenv("U2NET_API_URL", "http://localhost:8004")  # U2NET (SINGLE_ITEM)
# INPAINTING_API_URL = os.getenv("INPAINTING_API_URL", "http://localhost:8003")  # Stable Diffusion - 미사용
REMBG_API_URL = os.getenv("REMBG_API_URL", None)  # Hugging Face Space URL

# 카테고리 매핑 (AI 라벨 → Spring Category enum)
CATEGORY_MAPPING = {
    "upper-clothes": "TOP",
    "dress": "TOP",
    "pants": "BOTTOM",
    "skirt": "BOTTOM",
    "hat": "ACC",
    "bag": "ACC",
    "scarf": "ACC",
    "shoes": "SHOES",
    "left-shoe": "SHOES",
    "right-shoe": "SHOES"
}


class ClothProcessingPipelineCloudRun:
    """CloudRun API를 호출하는 옷 이미지 처리 파이프라인"""

    @staticmethod
    def normalize_rembg_api_url(rembg_api_url: str) -> str:
        """Normalize Hugging Face Space URL to use the hf.space runtime domain."""
        if not rembg_api_url:
            return rembg_api_url

        rembg_api_url = rembg_api_url.rstrip("/")
        parsed = urlparse(rembg_api_url)
        if "huggingface.co" not in parsed.netloc:
            if parsed.path.rstrip("/").endswith("/remove-bg"):
                return rembg_api_url[: -len("/remove-bg")]
            return rembg_api_url

        path_parts = parsed.path.strip("/").split("/")
        if len(path_parts) >= 3 and path_parts[0] == "spaces":
            owner = path_parts[1]
            space = path_parts[2]
            return f"https://{owner}-{space}.hf.space"

        return rembg_api_url

    def __init__(self):
        print(f"🚀 Initializing CloudRun API Pipeline (Lightweight Worker)")
        print(f"   Segformer API (FULL_BODY): {SEGMENTATION_API_URL}")
        print(f"   U2NET API (SINGLE_ITEM): {U2NET_API_URL}")
        # print(f"   Inpainting API: {INPAINTING_API_URL}")  # Stable Diffusion - 미사용

        # Background Removal 설정 (API 또는 로컬 rembg)
        self.rembg_api_url = self.normalize_rembg_api_url(REMBG_API_URL)
        self.rembg_session = None

        if self.rembg_api_url:
            print(f"   Background Removal: Hugging Face API ({self.rembg_api_url})")
        else:
            print(f"   Background Removal: Local rembg")
            # 로컬 rembg 사용 (로컬 환경에서만)
            try:
                from rembg import remove, new_session
                print("  🔥 Warming up local rembg model...")
                warmup_start = time.time()
                self.rembg_session = new_session()

                # Warmup
                dummy_img = Image.new('RGB', (100, 100), color='white')
                dummy_bytes = io.BytesIO()
                dummy_img.save(dummy_bytes, format='PNG')
                dummy_bytes.seek(0)
                remove(dummy_bytes.getvalue(), session=self.rembg_session)

                warmup_time = time.time() - warmup_start
                print(f"  ✅ Local rembg model loaded and ready ({warmup_time:.2f}s)")
            except ImportError:
                print(f"  ⚠️  rembg not available. Please install rembg or set REMBG_API_URL")
                raise Exception("Background removal not available: rembg not installed and REMBG_API_URL not set")
            except Exception as e:
                print(f"  ⚠️  rembg warmup failed: {e}")

        # Google AI Imagen 서비스 초기화 (선택적)
        if IMAGEN_AVAILABLE:
            try:
                self.imagen_service = GoogleAIImagenService()
                self.use_imagen = True
                print("  ✅ Google AI Imagen 서비스 활성화")
            except Exception as e:
                print(f"  ⚠️  Google AI Imagen 초기화 실패: {e}")
                self.use_imagen = False
        else:
            self.use_imagen = False

        print("✅ Pipeline initialized successfully")

    def remove_background(self, image_bytes):
        """배경 제거 (Hugging Face Gradio API 또는 로컬 rembg)"""
        if self.rembg_api_url:
            # Hugging Face Space Gradio API 사용
            print("  Step 1/4: Removing background with Hugging Face Gradio API...")
            try:
                from gradio_client import Client
                import tempfile

                api_base_url = self.rembg_api_url.rstrip("/")

                # 임시 파일로 저장
                with tempfile.NamedTemporaryFile(suffix=".png", delete=False) as temp_file:
                    temp_file.write(image_bytes)
                    temp_path = temp_file.name

                try:
                    # Gradio Client로 호출
                    from gradio_client import handle_file

                    print(f"  🔗 Connecting to {self.rembg_api_url}...")
                    client = Client(self.rembg_api_url)

                    print(f"  📤 Sending image...")
                    result = client.predict(
                        handle_file(temp_path)
                    )

                    print(f"  📥 Received result: {type(result)}")
                    print(f"  📝 Result value: {result}")

                    # result의 타입별 상세 정보
                    if isinstance(result, str):
                        print(f"  📄 String result length: {len(result)}")
                        print(f"  📄 String preview: {result[:200]}")
                    elif hasattr(result, '__dict__'):
                        print(f"  📦 Object attributes: {dir(result)}")
                    else:
                        print(f"  ❓ Unknown result type")

                    # 결과 처리
                    if isinstance(result, str):
                        # 파일 경로로 반환된 경우
                        print(f"  📄 Result is file path: {result}")

                        # gradio_client는 파일을 로컬에 다운로드하고 경로를 반환함
                        # 로컬 파일 시스템에서 직접 읽기
                        if os.path.exists(result):
                            print(f"  📂 Reading from local file: {result}")
                            with open(result, 'rb') as f:
                                image_data = f.read()
                            print(f"  📦 Read {len(image_data)} bytes from local file")
                        else:
                            # URL인 경우 (거의 없음)
                            print(f"  🌐 File not found locally, trying as URL...")
                            if result.startswith("http"):
                                file_url = result
                            else:
                                raise Exception(f"Invalid file path: {result} (does not exist)")

                            response = requests.get(file_url, timeout=30)
                            response.raise_for_status()
                            image_data = response.content
                            print(f"  📦 Downloaded {len(image_data)} bytes")

                    elif hasattr(result, 'save'):
                        # PIL Image로 반환된 경우
                        print(f"  🖼️  Result is PIL Image: {result.size}, {result.mode}")
                        img_byte_arr = io.BytesIO()
                        result.save(img_byte_arr, format='PNG')
                        img_byte_arr.seek(0)  # 중요: seek to beginning
                        image_data = img_byte_arr.getvalue()
                        print(f"  📦 Converted to {len(image_data)} bytes")

                    else:
                        raise Exception(f"Unexpected result type: {type(result)}, value: {result}")

                    # 이미지 데이터 검증
                    if not image_data or len(image_data) < 100:
                        raise Exception(f"Invalid image data: {len(image_data) if image_data else 0} bytes")

                finally:
                    # 임시 파일 삭제
                    try:
                        os.remove(temp_path)
                    except OSError:
                        pass

                # BytesIO로 변환 시 seek(0) 필수
                image_io = io.BytesIO(image_data)
                image_io.seek(0)
                image = Image.open(image_io).convert("RGBA")
                print(f"  ✅ Background removed (Hugging Face Gradio API): {image.size}, {image.mode}")
                return image

            except Exception as e:
                print(f"  ❌ Hugging Face API failed: {e}")
                import traceback
                traceback.print_exc()
                raise Exception(f"Background removal API 호출 실패: {e}")
        else:
            # 로컬 rembg 사용
            print("  Step 1/4: Removing background with local rembg...")
            from rembg import remove
            output = remove(image_bytes, session=self.rembg_session)
            image = Image.open(io.BytesIO(output)).convert("RGBA")
            print("  ✅ Background removed (local rembg)")
            return image

    def segment_clothing_api(self, image):
        """CloudRun Segmentation API 호출"""
        print("  Step 2/4: Calling CloudRun Segmentation API...")

        # RGB로 변환
        if image.mode == "RGBA":
            rgb_image = Image.new("RGB", image.size, (255, 255, 255))
            rgb_image.paste(image, mask=image.split()[3])
            image = rgb_image
        elif image.mode != "RGB":
            image = image.convert("RGB")

        # 이미지를 바이트로 변환
        img_byte_arr = io.BytesIO()
        image.save(img_byte_arr, format='PNG')
        img_byte_arr.seek(0)

        # CloudRun API 호출
        try:
            response = requests.post(
                f"{SEGMENTATION_API_URL}/segment",
                files={"file": ("image.png", img_byte_arr, "image/png")},
                timeout=60
            )
            response.raise_for_status()
            result = response.json()

            if result["status"] != "success":
                raise Exception(f"Segmentation failed: {result.get('message', 'Unknown error')}")

            detected_items = result["detected_items"]
            print(f"  ✅ Segmentation API returned {len(detected_items)} items")

            # 결과 파싱 (가장 큰 아이템 선택)
            if not detected_items:
                raise Exception("No clothing items detected")

            # 픽셀 크기순으로 정렬
            detected_items_sorted = sorted(detected_items, key=lambda x: x["area_pixels"], reverse=True)

            # 모든 아이템에 cropped_image 변환 추가
            all_items_with_images = []
            for item in detected_items_sorted:
                if "image_base64" in item:
                    image_data = base64.b64decode(item["image_base64"])
                    item_cropped_image = Image.open(io.BytesIO(image_data)).convert("RGBA")
                else:
                    item_cropped_image = image.copy()

                all_items_with_images.append({
                    "label": item["label"],
                    "area_pixels": item["area_pixels"],
                    "cropped_image": item_cropped_image,
                    "image_base64": item.get("image_base64", "")
                })

            primary_item = all_items_with_images[0]
            print(f"  ✅ Loaded {len(all_items_with_images)} items with cropped images")

            return {
                "label": primary_item["label"],
                "area_pixels": primary_item["area_pixels"],
                "cropped_image": primary_item["cropped_image"],
                "all_items": all_items_with_images
            }

        except requests.exceptions.RequestException as e:
            print(f"  ❌ Segmentation API call failed: {e}")
            raise Exception(f"Segmentation API 호출 실패: {e}")

    def segment_clothing_u2net_api(self, image):
        """U2NET API로 단일 옷 이미지 세그멘테이션"""
        print("  Step 2/4: Calling U2NET API (SINGLE_ITEM mode)...")

        # RGB로 변환
        if image.mode == "RGBA":
            rgb_image = Image.new("RGB", image.size, (255, 255, 255))
            rgb_image.paste(image, mask=image.split()[3])
            image = rgb_image
        elif image.mode != "RGB":
            image = image.convert("RGB")

        # 이미지를 바이트로 변환
        img_byte_arr = io.BytesIO()
        image.save(img_byte_arr, format='PNG')
        img_byte_arr.seek(0)

        # U2NET API 호출
        try:
            # Cold Start를 고려한 재시도 로직
            max_retries = 3
            retry_delay = 10  # 초

            for attempt in range(max_retries):
                try:
                    print(f"  🔄 U2NET API 호출 시도 {attempt + 1}/{max_retries}...")
                    response = requests.post(
                        f"{U2NET_API_URL}/segment",
                        files={"file": ("image.png", img_byte_arr, "image/png")},
                        timeout=120  # Cold Start 고려하여 2분으로 증가
                    )
                    response.raise_for_status()
                    break  # 성공하면 루프 종료

                except requests.exceptions.HTTPError as e:
                    if e.response.status_code == 503 and attempt < max_retries - 1:
                        # 503 에러이고 재시도 가능하면 대기 후 재시도
                        print(f"  ⚠️  503 Service Unavailable (Cold Start 중?). {retry_delay}초 후 재시도...")
                        time.sleep(retry_delay)
                        img_byte_arr.seek(0)  # 파일 포인터 리셋
                        continue
                    else:
                        raise
                except requests.exceptions.Timeout as e:
                    if attempt < max_retries - 1:
                        print(f"  ⚠️  타임아웃. {retry_delay}초 후 재시도...")
                        time.sleep(retry_delay)
                        img_byte_arr.seek(0)
                        continue
                    else:
                        raise
            response.raise_for_status()
            result = response.json()

            if result["status"] != "success":
                raise Exception(f"U2NET API failed: {result.get('message', 'Unknown error')}")

            detected_item = result["detected_item"]
            print(f"  ✅ U2NET API returned: {detected_item['label']} ({detected_item['area_pixels']} pixels)")

            # base64 이미지 데이터에서 이미지 로드
            cropped_image_data = base64.b64decode(detected_item["image_base64"])
            cropped_image = Image.open(io.BytesIO(cropped_image_data)).convert("RGBA")

            fullsize_image_data = base64.b64decode(detected_item["fullsize_base64"])
            fullsize_image = Image.open(io.BytesIO(fullsize_image_data)).convert("RGBA")

            # Primary 아이템 결과
            primary_item = {
                "label": detected_item["label"],
                "area_pixels": detected_item["area_pixels"],
                "cropped_image": cropped_image,
                "fullsize_image": fullsize_image
            }

            # 모든 감지된 아이템 (U2NET은 단일 아이템만)
            detected_items = [primary_item]

            return primary_item, detected_items

        except requests.exceptions.RequestException as e:
            print(f"  ❌ U2NET API call failed: {e}")
            raise Exception(f"U2NET API 호출 실패: {e}")

    # ============================================
    # Stable Diffusion Inpainting (미사용)
    # ============================================
    # def inpaint_image_api(self, image):
    #     """CloudRun Inpainting API 호출 (Stable Diffusion - 미사용)"""
    #     print("  Step 4/4: Calling CloudRun Inpainting API...")
    #
    #     # 이미지를 바이트로 변환
    #     img_byte_arr = io.BytesIO()
    #     image.save(img_byte_arr, format='PNG')
    #     img_byte_arr.seek(0)
    #
    #     # CloudRun API 호출
    #     try:
    #         response = requests.post(
    #             f"{INPAINTING_API_URL}/inpaint",
    #             files={"file": ("image.png", img_byte_arr, "image/png")},
    #             data={"extend_ratio": 0.5},
    #             timeout=300  # Inpainting은 시간이 오래 걸릴 수 있음
    #         )
    #         response.raise_for_status()
    #
    #         # 응답은 이미지 바이트
    #         inpainted_image = Image.open(io.BytesIO(response.content)).convert("RGBA")
    #         print("  ✅ Inpainting completed")
    #         return inpainted_image
    #
    #     except requests.exceptions.RequestException as e:
    #         print(f"  ⚠️  Inpainting API call failed: {e}")
    #         print("  Using original image as fallback")
    #         return image  # 실패 시 원본 반환

    def _has_transparency(self, image):
        """이미지에 실제 투명도가 있는지 확인"""
        if image.mode != 'RGBA':
            return False

        # 알파 채널 확인
        alpha = image.getchannel('A')
        extrema = alpha.getextrema()

        # 알파 채널이 모두 255(완전 불투명)가 아니면 투명도 있음
        return extrema != (255, 255)

    def image_to_base64(self, image, max_size=1024, quality=85):
        """PIL Image를 base64 문자열로 변환 (최적화: 리사이즈 + JPEG)

        Args:
            image: PIL Image 객체
            max_size: 최대 이미지 크기 (너비 또는 높이)
            quality: JPEG 품질 (0-100, 85 권장)

        Returns:
            str: base64 인코딩된 문자열
        """
        # 1. 리사이즈 (비율 유지)
        if image.width > max_size or image.height > max_size:
            # 원본 보존을 위해 복사
            image = image.copy()
            image.thumbnail((max_size, max_size), Image.Resampling.LANCZOS)

        # 2. 포맷 선택: 투명도 있으면 PNG, 없으면 JPEG
        img_byte_arr = io.BytesIO()

        if image.mode == 'RGBA' and self._has_transparency(image):
            # 투명도가 있는 경우: PNG 사용 (투명도 보존)
            image.save(img_byte_arr, format='PNG', optimize=True, compress_level=6)
        else:
            # 투명도가 없는 경우: JPEG 사용 (파일 크기 대폭 감소)
            if image.mode != 'RGB':
                # RGBA 또는 다른 모드를 RGB로 변환 (흰색 배경)
                rgb_image = Image.new('RGB', image.size, (255, 255, 255))
                if image.mode == 'RGBA':
                    # 알파 채널을 마스크로 사용하여 합성
                    rgb_image.paste(image, mask=image.split()[3])
                else:
                    rgb_image.paste(image)
                image = rgb_image

            # JPEG로 저장 (품질 85, 최적화 활성화)
            image.save(img_byte_arr, format='JPEG', quality=quality, optimize=True)

        img_byte_arr.seek(0)
        return base64.b64encode(img_byte_arr.read()).decode('utf-8')

    def process(self, cloth_id, user_id, image_bytes, image_type, worker=None):
        """전체 파이프라인 실행 (CloudRun API 사용)"""
        print(f"\n🔄 Processing clothId: {cloth_id}, userId: {user_id}, imageType: {image_type}")

        try:
            # 모델 선택 로직
            use_u2net_api = (image_type == "SINGLE_ITEM")

            print(f"\n{'='*60}")
            if use_u2net_api:
                print("  🎯 모델 선택: U2NET API")
                print("  📸 이미지 타입: SINGLE_ITEM (단일 옷 이미지)")
                print("  🔍 처리 방식: 단일 아이템 감지 + 자동 카테고리 분류")
            else:
                print("  🎯 모델 선택: Segformer API")
                print(f"  📸 이미지 타입: {image_type} (전신 사진)")
                print("  🔍 처리 방식: 다중 의류 아이템 감지 (상의/하의/신발/가방 등)")
            print(f"{'='*60}\n")

            # Step 1: Background Removal (0% → 25%)
            if worker:
                worker.send_progress(cloth_id, user_id, "PROCESSING", "배경 제거 중...", 10)
            print(f"  [10%] 배경 제거 중...")
            removed_bg_image = self.remove_background(image_bytes)
            removed_bg_path = REMOVED_BG_DIR / f"{cloth_id}.png"
            removed_bg_image.save(removed_bg_path)
            if worker:
                worker.send_progress(cloth_id, user_id, "PROCESSING", "배경 제거 완료", 25)
            print(f"  [25%] 배경 제거 완료")

            # Step 2: Cloth Segmentation (25% → 50%)
            if worker:
                worker.send_progress(cloth_id, user_id, "PROCESSING", "옷 영역 분석 중...", 30)
            print(f"  [30%] 옷 영역 분석 중...")

            if use_u2net_api:
                # U2NET API 사용 (단일 옷 이미지)
                primary_item, all_detected_items = self.segment_clothing_u2net_api(removed_bg_image)
                segmented_image = primary_item["cropped_image"]

                # U2NET: fullsize 버전도 저장 (배경만 투명, 원본 크기 유지)
                if "fullsize_image" in primary_item:
                    fullsize_image = primary_item["fullsize_image"]
                    fullsize_path = SEGMENTED_DIR / f"{cloth_id}_fullsize.png"
                    fullsize_image.save(fullsize_path)
                    print(f"  💾 Fullsize 이미지 저장: {fullsize_path}")
            else:
                # Segformer API 사용 (전신 사진)
                segmentation_result = self.segment_clothing_api(removed_bg_image)
                primary_item = segmentation_result
                all_detected_items = segmentation_result.get("all_items", [primary_item])
                segmented_image = primary_item["cropped_image"]

            segmented_path = SEGMENTED_DIR / f"{cloth_id}.png"
            segmented_image.save(segmented_path)
            print(f"  💾 Segmented 이미지 저장: {segmented_path}")

            if worker:
                worker.send_progress(cloth_id, user_id, "PROCESSING", "옷 영역 분석 완료", 50)
            print(f"  [50%] 옷 영역 분석 완료")

            # Step 3: Google AI Imagen 확장 (50% → 70%)
            all_expanded_items = []

            if image_type == "FULL_BODY":
                # FULL_BODY: 모든 감지된 아이템을 Gemini로 확장
                print(f"  [50%] 전신 사진 - 모든 아이템 확장 시작 (총 {len(all_detected_items)}개)")
                progress = 50
                progress_step = 20 / max(len(all_detected_items), 1)

                for idx, item in enumerate(all_detected_items):
                    item_image = item["cropped_image"]

                    if self.use_imagen:
                        if worker:
                            worker.send_progress(
                                cloth_id, user_id, "PROCESSING",
                                f"아이템 확장 중... ({idx+1}/{len(all_detected_items)})",
                                int(progress)
                            )
                        print(f"  [{int(progress)}%] Gemini로 아이템 확장 중... ({idx+1}/{len(all_detected_items)}): {item['label']}")

                        # Gemini 확장
                        expanded_item_image = self.imagen_service.expand_image(item_image)

                        # 저장 경로 생성
                        if idx == 0:  # primary item
                            item_expanded_path = EXPANDED_DIR / f"{cloth_id}.png"
                        else:
                            item_expanded_path = EXPANDED_DIR / f"{cloth_id}_{item['label']}_{idx}.png"

                        expanded_item_image.save(item_expanded_path)
                        print(f"  [{int(progress)}%] 확장 이미지 저장: {item_expanded_path}")

                        # base64 인코딩
                        expanded_base64 = self.image_to_base64(expanded_item_image)

                        all_expanded_items.append({
                            "label": item["label"],
                            "expandedPath": str(item_expanded_path.absolute()),
                            "imageBase64": expanded_base64,
                            "areaPixels": item["area_pixels"]
                        })
                    else:
                        # Imagen 비활성화 시 segmented 이미지 사용
                        print(f"  [{int(progress)}%] Imagen 비활성화 - 확장 건너뜀: {item['label']}")
                        all_expanded_items.append({
                            "label": item["label"],
                            "expandedPath": str(segmented_path.absolute()),
                            "imageBase64": item.get("image_base64", ""),
                            "areaPixels": item["area_pixels"]
                        })

                    progress += progress_step

                # primary item의 expanded_image 설정 (기존 코드 호환)
                if all_expanded_items:
                    expanded_path = Path(all_expanded_items[0]["expandedPath"])
                    # expanded_image는 primary item의 이미지 (final_image 생성용)
                    expanded_image = Image.open(expanded_path)
                else:
                    expanded_image = segmented_image
                    expanded_path = segmented_path

                print(f"  [70%] 전신 사진 - 모든 아이템 확장 완료 (총 {len(all_expanded_items)}개)")

            else:
                # SINGLE_ITEM: primary item만 확장 (기존 로직 유지)
                print(f"  [50%] 단일 아이템 - primary item만 확장")
                expanded_image = segmented_image
                expanded_path = segmented_path

                if self.use_imagen:
                    if worker:
                        worker.send_progress(cloth_id, user_id, "PROCESSING", "이미지 확장 중...", 55)
                    print(f"  [55%] Gemini로 이미지 확장 중...")
                    expanded_image = self.imagen_service.expand_image(segmented_image)
                    expanded_path = EXPANDED_DIR / f"{cloth_id}.png"
                    expanded_image.save(expanded_path)
                    print(f"  [70%] 이미지 확장 완료")

                    # base64 인코딩
                    expanded_base64 = self.image_to_base64(expanded_image)

                    all_expanded_items.append({
                        "label": primary_item["label"],
                        "expandedPath": str(expanded_path.absolute()),
                        "imageBase64": expanded_base64,
                        "areaPixels": primary_item["area_pixels"]
                    })
                else:
                    print(f"  [55%] Imagen 비활성화 - 확장 건너뜀")

            if worker:
                worker.send_progress(cloth_id, user_id, "PROCESSING", "이미지 확장 완료", 70)

            # ============================================
            # Step 4: Stable Diffusion Inpainting (미사용)
            # ============================================
            # if worker:
            #     worker.send_progress(cloth_id, user_id, "PROCESSING", "이미지 복원 중...", 75)
            # print(f"  [75%] CloudRun Inpainting API 호출...")
            #
            # inpainted_image = self.inpaint_image_api(expanded_image)
            # inpainted_path = INPAINTED_DIR / f"{cloth_id}.png"
            # inpainted_image.save(inpainted_path)
            #
            # if worker:
            #     worker.send_progress(cloth_id, user_id, "PROCESSING", "이미지 복원 완료", 95)
            # print(f"  [95%] 이미지 복원 완료")

            # 최종 이미지는 expanded_image 사용
            final_image = expanded_image
            final_path = expanded_path

            if worker:
                worker.send_progress(cloth_id, user_id, "PROCESSING", "처리 완료", 95)
            print(f"  [95%] 최종 이미지 처리 완료")

            # 카테고리 매핑
            suggested_category = CATEGORY_MAPPING.get(primary_item["label"], "ACC")

            # 이미지들을 base64로 인코딩 (CloudRun → Railway 전송용)
            print(f"  [96%] Base64 인코딩 중...")
            removed_bg_base64 = self.image_to_base64(removed_bg_image)
            segmented_base64 = self.image_to_base64(segmented_image)  # segmented 이미지
            expanded_base64 = self.image_to_base64(expanded_image)    # expanded 이미지 (Gemini)
            final_base64 = self.image_to_base64(final_image)          # 최종 이미지 (= expanded)
            print(f"  [97%] Base64 인코딩 완료")

            result = {
                "clothId": cloth_id,
                "success": True,
                "errorMessage": None,
                # 파일 경로 (참고용, Railway에서는 사용 안 함)
                "removedBgImagePath": str(removed_bg_path.absolute()),
                "segmentedImagePath": str(segmented_path.absolute()),
                "inpaintedImagePath": str(final_path.absolute()),  # 최종 이미지 경로 (expanded)
                # base64 이미지 데이터 (Railway에서 사용)
                "removedBgImageBase64": removed_bg_base64,
                "segmentedImageBase64": segmented_base64,
                "inpaintedImageBase64": final_base64,  # 최종 이미지 base64 (= expanded)
                "suggestedCategory": suggested_category,
                "segmentationLabel": primary_item["label"],
                "areaPixels": primary_item["area_pixels"],
                # 추가 아이템 (있는 경우)
                "allSegmentedItems": [
                    {
                        "label": item["label"],
                        "segmentedPath": item.get("saved_path", ""),
                        "imageBase64": item.get("image_base64", ""),  # Segmentation API에서 받은 base64
                        "areaPixels": item["area_pixels"]
                    }
                    for item in all_detected_items
                ],
                # ✅ 수정: allExpandedItems에 모든 확장된 아이템 추가
                "allExpandedItems": all_expanded_items
            }

            print(f"  [98%] Result 딕셔너리 생성 완료")
            print(f"\n{'='*60}")
            print(f"✅ Processing completed: {suggested_category}")
            print(f"   🤖 사용된 모델: {'U2NET API' if use_u2net_api else 'Segformer API'}")
            print(f"   📸 이미지 타입: {image_type}")
            print(f"   🏷️  Category: {suggested_category} ({primary_item['label']})")
            print(f"   📦 감지된 아이템: {len(all_detected_items)}개")
            print(f"   🎨 Gemini 확장된 아이템: {len(all_expanded_items)}개")
            print(f"{'='*60}\n")
            print(f"  [99%] Returning result...")
            return result

        except Exception as e:
            print(f"❌ Processing failed: {str(e)}")
            traceback.print_exc()

            return {
                "clothId": cloth_id,
                "success": False,
                "errorMessage": str(e),
                "removedBgImagePath": None,
                "segmentedImagePath": None,
                "inpaintedImagePath": None,
                "suggestedCategory": None,
                "segmentationLabel": None,
                "areaPixels": None
            }


class ClothProcessingWorker:
    """RabbitMQ Worker (CloudRun API 버전)"""

    def __init__(self):
        self.pipeline = ClothProcessingPipelineCloudRun()
        self.connection = None
        self.channel = None
        self.user_id = None

    def connect(self):
        """RabbitMQ 연결"""
        credentials = pika.PlainCredentials(RABBITMQ_USER, RABBITMQ_PASS)
        parameters = pika.ConnectionParameters(
            host=RABBITMQ_HOST,
            port=RABBITMQ_PORT,
            credentials=credentials,
            heartbeat=600,  # 10분 heartbeat
            blocked_connection_timeout=300,  # 5분 블록 타임아웃
            socket_timeout=600  # 10분 소켓 타임아웃 (rembg 첫 실행 대응)
        )

        self.connection = pika.BlockingConnection(parameters)
        self.channel = self.connection.channel()

        # Exchange 선언
        self.channel.exchange_declare(
            exchange=EXCHANGE,
            exchange_type='direct',
            durable=True
        )

        # 큐 선언
        self.channel.queue_declare(queue=REQUEST_QUEUE, durable=True)
        self.channel.queue_declare(queue=RESULT_QUEUE, durable=True)
        self.channel.queue_declare(queue=PROGRESS_QUEUE, durable=True)

        # 바인딩
        self.channel.queue_bind(
            exchange=EXCHANGE,
            queue=REQUEST_QUEUE,
            routing_key=REQUEST_ROUTING_KEY
        )
        self.channel.queue_bind(
            exchange=EXCHANGE,
            queue=RESULT_QUEUE,
            routing_key=RESULT_ROUTING_KEY
        )
        self.channel.queue_bind(
            exchange=EXCHANGE,
            queue=PROGRESS_QUEUE,
            routing_key=PROGRESS_ROUTING_KEY
        )

        # QoS 설정
        self.channel.basic_qos(prefetch_count=1)

        print(f"✅ Connected to RabbitMQ at {RABBITMQ_HOST}:{RABBITMQ_PORT}")

    def on_message(self, ch, method, properties, body):
        """메시지 수신 콜백"""
        cloth_id = None
        user_id = None
        retry_count = 0

        try:
            # 메시지 파싱
            message = json.loads(body)
            cloth_id = message["clothId"]
            user_id = message.get("userId")
            image_bytes_data = message["imageBytes"]
            original_filename = message["originalFilename"]
            image_type = message.get("imageType", "FULL_BODY")
            retry_count = message.get("retryCount", 0)
            message_timestamp = message.get("timestamp", None)

            print(f"\n📨 Received message: clothId={cloth_id}, userId={user_id}, imageType={image_type}, retryCount={retry_count}")

            # 오래된 메시지 체크 (10분 이상 지난 메시지는 폐기)
            MAX_MESSAGE_AGE_SECONDS = 600  # 10분
            if message_timestamp:
                current_time = datetime.now().timestamp() * 1000  # milliseconds
                message_age_ms = current_time - message_timestamp
                message_age_sec = message_age_ms / 1000

                if message_age_sec > MAX_MESSAGE_AGE_SECONDS:
                    print(f"⏰ Message too old ({message_age_sec:.0f}s > {MAX_MESSAGE_AGE_SECONDS}s). Discarding.")

                    # 오래된 메시지는 실패 처리하고 ACK
                    if cloth_id and user_id:
                        failed_result = {
                            "clothId": cloth_id,
                            "success": False,
                            "errorMessage": f"메시지가 너무 오래되었습니다 ({message_age_sec:.0f}초). 다시 업로드해주세요.",
                            "removedBgImagePath": None,
                            "segmentedImagePath": None,
                            "inpaintedImagePath": None,
                            "suggestedCategory": None,
                            "segmentationLabel": None,
                            "areaPixels": None
                        }
                        self.send_result(failed_result)

                    ch.basic_ack(delivery_tag=method.delivery_tag)
                    print(f"✅ Old message discarded\n")
                    return

            # userId 저장
            self.user_id = user_id

            # imageBytes 처리
            if isinstance(image_bytes_data, str):
                image_bytes = base64.b64decode(image_bytes_data)
            elif isinstance(image_bytes_data, list):
                image_bytes = bytes(image_bytes_data)
            else:
                raise ValueError(f"Unsupported imageBytes format: {type(image_bytes_data)}")

            # 파이프라인 실행
            result = self.pipeline.process(cloth_id, user_id, image_bytes, image_type, self)
            print(f"  [100%] Pipeline completed, sending result to Railway...")

            # 결과 전송
            self.send_result(result)
            print(f"  [100%] Result sent successfully")

            # ACK
            ch.basic_ack(delivery_tag=method.delivery_tag)
            print(f"✅ Message processed and acknowledged\n")

        except Exception as e:
            print(f"❌ Error processing message: {str(e)}")
            traceback.print_exc()

            # 최대 재시도 횟수 체크 (5회)
            MAX_RETRIES = 5

            if retry_count >= MAX_RETRIES:
                # 최대 재시도 초과 - 실패 처리하고 ACK (큐에서 제거)
                print(f"⚠️  Max retries ({MAX_RETRIES}) exceeded for clothId={cloth_id}. Sending FAILED status.")

                # 실패 결과 전송
                if cloth_id and user_id:
                    failed_result = {
                        "clothId": cloth_id,
                        "success": False,
                        "errorMessage": f"AI 처리 실패: 최대 재시도 횟수({MAX_RETRIES}회) 초과. {str(e)}",
                        "removedBgImagePath": None,
                        "segmentedImagePath": None,
                        "inpaintedImagePath": None,
                        "suggestedCategory": None,
                        "segmentationLabel": None,
                        "areaPixels": None
                    }
                    self.send_result(failed_result)

                # ACK - 메시지 제거 (더 이상 재시도하지 않음)
                ch.basic_ack(delivery_tag=method.delivery_tag)
                print(f"✅ Message acknowledged as FAILED (max retries exceeded)\n")
            else:
                # 재시도 가능 - retryCount 증가시켜서 다시 발행
                print(f"🔄 Retry {retry_count + 1}/{MAX_RETRIES} for clothId={cloth_id}")

                # 원본 메시지 ACK (큐에서 제거)
                ch.basic_ack(delivery_tag=method.delivery_tag)

                # retryCount 증가시킨 새 메시지 발행
                if cloth_id and user_id:
                    retry_message = json.loads(body)  # 원본 메시지 복사
                    retry_message["retryCount"] = retry_count + 1  # retryCount 증가
                    # timestamp는 유지 (원본 메시지의 생성 시간 유지)

                    # 같은 큐에 다시 발행
                    self.channel.basic_publish(
                        exchange=EXCHANGE,
                        routing_key=REQUEST_ROUTING_KEY,
                        body=json.dumps(retry_message),
                        properties=pika.BasicProperties(
                            delivery_mode=2,  # persistent
                            content_type='application/json'
                        )
                    )
                    print(f"📤 Re-queued message with retryCount={retry_count + 1}")
                else:
                    # cloth_id나 user_id가 없으면 그냥 버림
                    print(f"⚠️  Cannot retry - missing clothId or userId")

    def send_progress(self, cloth_id, user_id, status, current_step, progress_percentage):
        """진행도 메시지 전송"""
        progress_message = {
            "clothId": cloth_id,
            "userId": user_id,
            "status": status,
            "currentStep": current_step,
            "progressPercentage": progress_percentage,
            "timestamp": int(datetime.now().timestamp() * 1000)
        }

        progress_json = json.dumps(progress_message)

        self.channel.basic_publish(
            exchange=EXCHANGE,
            routing_key=PROGRESS_ROUTING_KEY,
            body=progress_json,
            properties=pika.BasicProperties(
                delivery_mode=2,
                content_type='application/json'
            )
        )

        print(f"📊 Progress sent: {progress_percentage}% - {current_step}")

    def send_result(self, result):
        """결과 메시지 전송"""
        result_json = json.dumps(result)

        self.channel.basic_publish(
            exchange=EXCHANGE,
            routing_key=RESULT_ROUTING_KEY,
            body=result_json,
            properties=pika.BasicProperties(
                delivery_mode=2,
                content_type='application/json'
            )
        )

        print(f"📤 Result sent to {RESULT_QUEUE}")

    def start(self):
        """Worker 시작 (자동 재연결 포함)"""
        while True:
            try:
                self.connect()

                print(f"🎯 Listening on queue: {REQUEST_QUEUE}")
                print(f"🌐 Using CloudRun APIs:")
                print(f"   - Segmentation: {SEGMENTATION_API_URL}")
                # print(f"   - Inpainting: {INPAINTING_API_URL}")  # Stable Diffusion - 미사용
                print("Waiting for messages. To exit press CTRL+C\n")

                self.channel.basic_consume(
                    queue=REQUEST_QUEUE,
                    on_message_callback=self.on_message
                )

                self.channel.start_consuming()

            except KeyboardInterrupt:
                print("\n⛔ Stopping worker...")
                if self.channel and self.channel.is_open:
                    self.channel.stop_consuming()
                break

            except (pika.exceptions.ConnectionClosedByBroker,
                    pika.exceptions.AMQPConnectionError,
                    pika.exceptions.StreamLostError) as e:
                print(f"❌ RabbitMQ connection lost: {e}")
                print("🔄 Reconnecting in 5 seconds...")
                time.sleep(5)
                continue

            except Exception as e:
                print(f"❌ Unexpected error: {e}")
                traceback.print_exc()
                print("🔄 Reconnecting in 5 seconds...")
                time.sleep(5)
                continue

            finally:
                # 연결이 열려있을 때만 닫기
                if self.connection and self.connection.is_open:
                    try:
                        self.connection.close()
                    except Exception as e:
                        print(f"⚠️  Error closing connection: {e}")

        print("👋 Worker stopped")


# Flask HTTP 서버 (Cloud Run 헬스체크용)
app = Flask(__name__)

@app.route('/health', methods=['GET'])
def health_check():
    """Cloud Run 헬스체크 엔드포인트"""
    return jsonify({
        "status": "healthy",
        "service": "closetconnect-worker",
        "mode": "cloudrun-api",
        "rabbitmq_host": RABBITMQ_HOST,
        "segmentation_api": SEGMENTATION_API_URL,
        # "inpainting_api": INPAINTING_API_URL  # Stable Diffusion - 미사용
    }), 200

@app.route('/', methods=['GET'])
def index():
    """루트 엔드포인트"""
    return jsonify({
        "service": "ClosetConnect CloudRun Worker",
        "status": "running",
        "description": "RabbitMQ worker that processes cloth images using CloudRun APIs"
    }), 200


def run_worker():
    """별도 스레드에서 RabbitMQ Worker 실행"""
    worker = ClothProcessingWorker()
    worker.start()


if __name__ == "__main__":
    # Cloud Run에서 요구하는 PORT 환경변수
    port = int(os.getenv("PORT", "8080"))

    # RabbitMQ Worker를 별도 스레드로 시작
    worker_thread = threading.Thread(target=run_worker, daemon=True)
    worker_thread.start()
    print(f"🔄 RabbitMQ Worker thread started")

    # Flask HTTP 서버 시작 (Cloud Run 헬스체크용)
    print(f"🌐 Starting HTTP health check server on port {port}")
    app.run(host='0.0.0.0', port=port, debug=False)