"""
RabbitMQ Cloth Processing Worker
- RabbitMQ에서 옷 처리 요청을 받아 전체 파이프라인 실행
- rembg → segmentation → imagen expansion → inpainting
- 결과를 RabbitMQ로 다시 전송
"""

import pika
import json
import base64
import os
import sys
import traceback
from pathlib import Path
from PIL import Image
import io
import torch
import numpy as np
from rembg import remove
from datetime import datetime
from dotenv import load_dotenv

# .env 파일에서 환경변수 로드
# 현재 디렉토리부터 상위로 자동 검색 (프로젝트 루트의 .env 찾음)
load_dotenv()

# Google AI Imagen 서비스 import
try:
    from ..services.imagen_service import GoogleAIImagenService
    IMAGEN_AVAILABLE = True
except ImportError:
    IMAGEN_AVAILABLE = False
    print("⚠️  Google AI Imagen 서비스를 사용할 수 없습니다.")
    print("   설치: pip install google-cloud-aiplatform")

# 기존 segmentation 모델 import
try:
    from ..api.cloth_segmentation_api import ClothSegmentationModel
    USE_EXISTING_MODEL = True
except ImportError:
    from transformers import AutoImageProcessor, AutoModelForSemanticSegmentation
    USE_EXISTING_MODEL = False

# U2NET 모델 import (단일 옷 이미지용)
try:
    from ..utils.u2net_process import load_seg_model, get_palette, generate_mask
    U2NET_AVAILABLE = True
except ImportError as e:
    print(f"⚠️  U2NET 모델 import 실패: {e}")
    U2NET_AVAILABLE = False

# 프로젝트 루트 디렉토리
PROJECT_ROOT = Path(__file__).parent.parent
OUTPUTS_DIR = PROJECT_ROOT / "outputs"
SEGMENTED_DIR = OUTPUTS_DIR / "segmented_clothes"
REMOVED_BG_DIR = OUTPUTS_DIR / "removed_bg"
EXPANDED_DIR = OUTPUTS_DIR / "expanded"  # Nano Banana 확장만 된 이미지
INPAINTED_DIR = OUTPUTS_DIR / "inpainted"

# 디렉토리 생성
SEGMENTED_DIR.mkdir(parents=True, exist_ok=True)
REMOVED_BG_DIR.mkdir(parents=True, exist_ok=True)
EXPANDED_DIR.mkdir(parents=True, exist_ok=True)
INPAINTED_DIR.mkdir(parents=True, exist_ok=True)

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

# 의상 카테고리 정의
CLOTH_LABELS = {
    0: "background",
    1: "hat",
    2: "hair",
    3: "sunglasses",
    4: "upper-clothes",
    5: "skirt",
    6: "pants",
    7: "dress",
    8: "belt",
    9: "shoes",  # left-shoe + right-shoe 통합
    10: "right-shoe",  # 사용되지 않음 (9로 통합됨)
    11: "face",
    12: "left-leg",
    13: "right-leg",
    14: "left-arm",
    15: "right-arm",
    16: "bag",
    17: "scarf"
}

# 카테고리 매핑 (AI 라벨 → 앱 카테고리)
# Spring Category enum: ACC, TOP, BOTTOM, SHOES
CATEGORY_MAPPING = {
    "upper-clothes": "TOP",
    "dress": "TOP",  # 원피스도 상의로 분류
    "pants": "BOTTOM",
    "skirt": "BOTTOM",
    "hat": "ACC",
    "bag": "ACC",
    "scarf": "ACC",
    "shoes": "SHOES"  # left-shoe + right-shoe 통합
}


class ClothProcessingPipeline:
    """옷 이미지 처리 파이프라인"""

    def __init__(self):
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        print(f"🚀 Initializing pipeline on device: {self.device}")

        # Segmentation 모델 로드
        if USE_EXISTING_MODEL:
            print("  Using existing ClothSegmentationModel")
            self.seg_model = ClothSegmentationModel()
        else:
            print("  Using direct transformers model")
            model_name = "mattmdjaga/segformer_b2_clothes"
            from transformers import AutoImageProcessor, AutoModelForSemanticSegmentation
            self.processor = AutoImageProcessor.from_pretrained(model_name)
            self.model = AutoModelForSemanticSegmentation.from_pretrained(model_name)
            self.model.to(self.device)
            self.model.eval()

        # U2NET 모델 로드 (단일 옷 이미지용)
        self.u2net_available = False
        if U2NET_AVAILABLE:
            try:
                checkpoint_path = Path(__file__).parent.parent / "model" / "cloth_segm.pth"
                if checkpoint_path.exists():
                    self.u2net_model = load_seg_model(str(checkpoint_path), device=str(self.device))
                    self.u2net_palette = get_palette(4)
                    self.u2net_available = True
                    print("  ✅ U2NET 모델 로드 성공 (단일 옷 이미지용)")
                else:
                    print(f"  ⚠️  U2NET 체크포인트 없음: {checkpoint_path}")
            except Exception as e:
                print(f"  ⚠️  U2NET 모델 로드 실패: {e}")

        # Google AI Imagen 서비스 초기화 (선택적)
        if IMAGEN_AVAILABLE:
            try:
                self.imagen_service = GoogleAIImagenService()
                self.use_imagen = True
                print("  ✅ Google AI Imagen 서비스 활성화")
            except Exception as e:
                print(f"  ⚠️  Google AI Imagen 초기화 실패: {e}")
                print("     이미지 확장 기능이 비활성화됩니다.")
                self.use_imagen = False
        else:
            self.use_imagen = False

        print("✅ Pipeline initialized successfully")

    def remove_background(self, image_bytes):
        """배경 제거 (rembg)"""
        print("  Step 1/3: Removing background...")
        output = remove(image_bytes)
        image = Image.open(io.BytesIO(output)).convert("RGBA")
        print("  ✅ Background removed")
        return image

    def segment_clothing(self, image):
        """의상 세그멘테이션 (여러 의류 감지)"""
        print("  Step 2/3: Segmenting clothing with Segformer (전신 사진 모드)...")
        print("  🤖 모델: Hugging Face SegFormer (Multi-class segmentation)")

        # RGB로 변환 (segmentation 모델은 RGB 사용)
        if image.mode == "RGBA":
            rgb_image = Image.new("RGB", image.size, (255, 255, 255))
            rgb_image.paste(image, mask=image.split()[3])
            image = rgb_image
        elif image.mode != "RGB":
            image = image.convert("RGB")

        if USE_EXISTING_MODEL:
            # 기존 모델 사용
            pred_seg = self.seg_model.segment_image(image)
        else:
            # 직접 모델 사용
            inputs = self.processor(
                images=image,
                return_tensors="pt",
                do_rescale=True,
                do_normalize=True
            )
            inputs = {k: v.to(self.device) for k, v in inputs.items()}

            with torch.no_grad():
                outputs = self.model(**inputs)

            logits = outputs.logits
            upsampled_logits = torch.nn.functional.interpolate(
                logits,
                size=image.size[::-1],
                mode="bilinear",
                align_corners=False
            )

            pred_seg = upsampled_logits.argmax(dim=1)[0].cpu().numpy()

        # 의상 아이템 추출 (모든 의류)
        detected_items = self._extract_cloth_items(image, pred_seg)

        if not detected_items:
            raise Exception("No clothing items detected in image")

        # 모든 감지된 의류 출력
        print(f"  📋 Detected {len(detected_items)} clothing item(s):")
        for item in detected_items:
            print(f"     - {item['label']}: {item['area_pixels']} pixels")

        # 우선순위에 따라 메인 아이템 선택
        # 우선순위: dress > upper-clothes > pants > skirt > shoes > bag > hat
        PRIORITY_ORDER = ["dress", "upper-clothes", "pants", "skirt", "left-shoe", "right-shoe", "bag", "hat", "scarf"]

        primary_item = None
        for priority_label in PRIORITY_ORDER:
            for item in detected_items:
                if item["label"] == priority_label:
                    primary_item = item
                    break
            if primary_item:
                break

        # 우선순위에 없으면 가장 큰 것 선택
        if not primary_item:
            primary_item = max(detected_items, key=lambda x: x["area_pixels"])

        print(f"  ✅ Primary item selected: {primary_item['label']} ({primary_item['area_pixels']} pixels)")

        # 모든 감지된 아이템도 함께 반환
        return primary_item, detected_items

    def segment_clothing_u2net(self, image):
        """U2NET 모델로 단일 옷 이미지 세그멘테이션 - 배경 제거, 옷만 추출"""
        print("  Step 2/3: Segmenting clothing with U2NET (단일 옷 모드)...")
        print("  🤖 모델: U2NET (Single item segmentation + Auto category detection)")

        # RGB로 변환
        if image.mode == "RGBA":
            rgb_image = Image.new("RGB", image.size, (255, 255, 255))
            rgb_image.paste(image, mask=image.split()[3])
            image = rgb_image
        elif image.mode != "RGB":
            image = image.convert("RGB")

        # U2NET으로 마스크 생성
        cloth_mask = generate_mask(image, self.u2net_model, self.u2net_palette, device=str(self.device))

        # 마스크를 numpy 배열로 변환
        mask_np = np.array(cloth_mask)
        image_np = np.array(image)

        # U2NET 카테고리 매핑 (자동 감지)
        U2NET_LABEL_MAP = {
            1: "upper-clothes",  # 상의
            2: "pants",          # 하의
            3: "dress"           # 원피스
        }

        # 각 클래스별 픽셀 수 계산
        unique_labels = np.unique(mask_np)
        class_pixels = {}
        for label_id in unique_labels:
            if label_id == 0:  # 배경 제외
                continue
            pixel_count = np.sum(mask_np == label_id)
            class_pixels[label_id] = pixel_count

        # 배경 제외하고 가장 많은 픽셀을 차지하는 클래스 찾기 (카테고리 자동 감지)
        if not class_pixels:
            raise Exception("No clothing detected in single item image")

        dominant_class = max(class_pixels, key=class_pixels.get)
        detected_label = U2NET_LABEL_MAP.get(dominant_class, "upper-clothes")
        print(f"     🎯 자동 감지된 카테고리: {detected_label} (U2NET class {dominant_class})")

        # 배경이 아닌 모든 영역을 옷으로 간주 (모든 옷 클래스 통합)
        cloth_mask_binary = (mask_np > 0).astype(np.uint8)
        total_cloth_pixels = np.sum(cloth_mask_binary)

        # 1. 원본 크기 유지 버전 생성 (배경만 투명)
        alpha_channel_full = (cloth_mask_binary * 255).astype(np.uint8)
        image_rgba_full = np.dstack([image_np, alpha_channel_full])
        fullsize_pil = Image.fromarray(image_rgba_full, mode='RGBA')

        print(f"     🖼️  원본 크기 유지: {image.size} (W×H), 옷 영역: {total_cloth_pixels:,} pixels")

        # 2. 타이트 크롭 버전 생성 (옷 영역만)
        rows = np.any(cloth_mask_binary, axis=1)
        cols = np.any(cloth_mask_binary, axis=0)

        if not rows.any() or not cols.any():
            raise Exception("No clothing detected in single item image")

        rmin, rmax = np.where(rows)[0][[0, -1]]
        cmin, cmax = np.where(cols)[0][[0, -1]]

        # Adaptive padding
        bbox_width = cmax - cmin
        bbox_height = rmax - rmin
        adaptive_padding = max(5, min(20, int(max(bbox_width, bbox_height) * 0.02)))

        img_height, img_width = image_np.shape[:2]
        rmin_padded = max(0, rmin - adaptive_padding)
        rmax_padded = min(img_height - 1, rmax + adaptive_padding)
        cmin_padded = max(0, cmin - adaptive_padding)
        cmax_padded = min(img_width - 1, cmax + adaptive_padding)

        # 크롭
        mask_cropped = cloth_mask_binary[rmin_padded:rmax_padded+1, cmin_padded:cmax_padded+1]
        image_cropped = image_np[rmin_padded:rmax_padded+1, cmin_padded:cmax_padded+1]

        # RGBA 이미지 생성 (크롭 버전)
        alpha_channel_cropped = (mask_cropped * 255).astype(np.uint8)
        image_rgba_cropped = np.dstack([image_cropped, alpha_channel_cropped])
        cropped_pil = Image.fromarray(image_rgba_cropped, mode='RGBA')

        # 품질 확인
        opaque_ratio = np.sum(alpha_channel_cropped > 0) / alpha_channel_cropped.size
        print(f"     ✂️  크롭 크기: {image_rgba_cropped.shape[1]}×{image_rgba_cropped.shape[0]}, 불투명 비율: {opaque_ratio*100:.1f}%")

        if opaque_ratio < 0.3:
            print(f"     ⚠️  경고: 투명 영역이 {(1-opaque_ratio)*100:.1f}%로 과도합니다.")
        elif opaque_ratio > 0.8:
            print(f"     ✅ 양호: 크롭이 잘 되었습니다.")

        # Primary 아이템 결과 (크롭 버전 사용)
        primary_item = {
            "label": detected_label,  # U2NET으로 자동 감지된 카테고리
            "label_id": 4,  # Spring Boot에서 사용하는 ID (매핑 필요)
            "area_pixels": int(total_cloth_pixels),
            "bbox": [int(cmin_padded), int(rmin_padded), int(cmax_padded), int(rmax_padded)],
            "cropped_image": cropped_pil,  # 크롭 버전
            "fullsize_image": fullsize_pil  # 원본 크기 유지 버전 (새로 추가)
        }

        print(f"  ✅ Single item detected: {detected_label} ({total_cloth_pixels:,} pixels)")
        print(f"     - 크롭 버전: {image_rgba_cropped.shape[1]}×{image_rgba_cropped.shape[0]}")
        print(f"     - Fullsize 버전: {image.size}")

        return primary_item, [primary_item]  # 단일 아이템이므로 리스트에도 같은 아이템

    def _extract_cloth_items(self, image, segmentation_map):
        """세그멘테이션 맵에서 의상 아이템 추출 (마스크 적용, 투명 배경)"""
        # 신발 통합: left-shoe (9) + right-shoe (10) → shoes (9)
        # segmentation_map을 복사하여 수정 (원본 보존)
        segmentation_map = segmentation_map.copy()

        left_shoe_exists = 9 in segmentation_map
        right_shoe_exists = 10 in segmentation_map

        if left_shoe_exists or right_shoe_exists:
            print(f"     👟 신발 감지: left={left_shoe_exists}, right={right_shoe_exists}")
            # left-shoe와 right-shoe를 모두 label 9 (shoes)로 통합
            segmentation_map[segmentation_map == 10] = 9
            print(f"     👟 신발 통합 완료: left-shoe + right-shoe → shoes")

        unique_labels = np.unique(segmentation_map)
        results = []
        image_np = np.array(image)
        img_height, img_width = image_np.shape[:2]

        # 제외할 라벨: 머리카락, 얼굴, 팔/다리
        EXCLUDE_LABELS = {2, 11, 12, 13, 14, 15}  # hair, face, left-leg, right-leg, left-arm, right-arm

        # Padding 설정 (bounding box 여유 공간)
        PADDING = 5  # 픽셀 단위 (너무 크면 투명 영역이 과도하게 포함됨)

        for label_id in unique_labels:
            if label_id == 0:  # background 제외
                continue

            # 머리카락, 얼굴, 팔/다리 명시적으로 제외
            if label_id in EXCLUDE_LABELS:
                continue

            label_name = CLOTH_LABELS.get(label_id, "unknown")

            # 옷 관련 라벨만 처리
            if label_name not in CATEGORY_MAPPING:
                continue

            # 마스크 생성
            mask = (segmentation_map == label_id).astype(np.uint8)
            area_pixels = np.sum(mask)

            # 너무 작은 영역 제외 (노이즈)
            if area_pixels < 1000:
                print(f"     ⚠️  {label_name} 영역이 너무 작아 제외됨 ({area_pixels} pixels)")
                continue

            # Bounding box 계산
            rows = np.any(mask, axis=1)
            cols = np.any(mask, axis=0)

            if not rows.any() or not cols.any():
                print(f"     ⚠️  {label_name} bounding box 계산 실패")
                continue

            rmin, rmax = np.where(rows)[0][[0, -1]]
            cmin, cmax = np.where(cols)[0][[0, -1]]

            # Adaptive Padding (옷 크기에 비례)
            bbox_width = cmax - cmin
            bbox_height = rmax - rmin

            # 옷 크기의 2%를 padding으로 사용 (최소 5, 최대 20)
            adaptive_padding = max(5, min(20, int(max(bbox_width, bbox_height) * 0.02)))

            # Padding 추가 (이미지 경계 내에서)
            rmin_padded = max(0, rmin - adaptive_padding)
            rmax_padded = min(img_height - 1, rmax + adaptive_padding)
            cmin_padded = max(0, cmin - adaptive_padding)
            cmax_padded = min(img_width - 1, cmax + adaptive_padding)

            print(f"     📐 {label_name} bbox: ({cmin}, {rmin}) → ({cmax}, {rmax}) [크기: {bbox_width}×{bbox_height}]")
            print(f"        → padding: {adaptive_padding}px, 최종: ({cmin_padded}, {rmin_padded}) → ({cmax_padded}, {rmax_padded})")

            # ========== 마스크 적용 크롭 (투명 배경) ==========
            # Padding이 적용된 영역으로 마스크 및 이미지 크롭
            mask_cropped = mask[rmin_padded:rmax_padded+1, cmin_padded:cmax_padded+1]
            image_cropped = image_np[rmin_padded:rmax_padded+1, cmin_padded:cmax_padded+1]

            # RGB로 변환 (필요시)
            if len(image_cropped.shape) == 2:  # Grayscale
                image_cropped = np.stack([image_cropped] * 3, axis=-1)
            elif image_cropped.shape[2] == 4:  # RGBA → RGB
                # 흰색 배경에 합성
                rgb_image = np.ones((image_cropped.shape[0], image_cropped.shape[1], 3), dtype=np.uint8) * 255
                alpha = image_cropped[:, :, 3:4] / 255.0
                rgb_image = (image_cropped[:, :, :3] * alpha + rgb_image * (1 - alpha)).astype(np.uint8)
                image_cropped = rgb_image

            # RGBA 이미지 생성 (마스크 영역만 불투명, 나머지는 투명)
            alpha_channel = (mask_cropped * 255).astype(np.uint8)
            image_rgba = np.dstack([image_cropped, alpha_channel])

            # 디버그: 크롭된 이미지 크기 및 품질 확인
            opaque_ratio = np.sum(alpha_channel > 0) / alpha_channel.size
            print(f"     🖼️  {label_name} 크롭 결과: {image_rgba.shape} (H×W×C), 불투명 비율: {opaque_ratio*100:.1f}%")

            if opaque_ratio < 0.3:
                print(f"     ⚠️  경고: 투명 영역이 {(1-opaque_ratio)*100:.1f}%로 과도합니다. 크롭이 너무 넓을 수 있습니다.")
            elif opaque_ratio > 0.8:
                print(f"     ✅ 양호: 크롭이 잘 되었습니다.")

            cropped_pil = Image.fromarray(image_rgba, mode='RGBA')
            # ==================================================

            results.append({
                "label": label_name,
                "label_id": int(label_id),
                "area_pixels": int(area_pixels),
                "bbox": [int(cmin_padded), int(rmin_padded), int(cmax_padded), int(rmax_padded)],
                "cropped_image": cropped_pil
            })

        return results

    def inpaint_image(self, image):
        """이미지 복원 (간단한 처리 - Stable Diffusion은 너무 무거우므로 생략 가능)"""
        print("  Step 3/3: Inpainting image...")
        # 여기서는 간단히 원본 이미지를 반환 (실제로는 Stable Diffusion 사용)
        # 프로덕션에서는 clothing_restorer.py의 로직 사용
        print("  ⚠️  Inpainting skipped (use original cropped image)")
        return image

    def process(self, cloth_id, user_id, image_bytes, image_type, worker=None):
        """전체 파이프라인 실행"""
        print(f"\n🔄 Processing clothId: {cloth_id}, userId: {user_id}, imageType: {image_type}")

        try:
            # 모델 선택
            use_u2net = (image_type == "SINGLE_ITEM" and self.u2net_available)

            print(f"\n{'='*60}")
            if use_u2net:
                print("  🎯 모델 선택: U2NET")
                print("  📸 이미지 타입: SINGLE_ITEM (단일 옷 이미지)")
                print("  🔍 처리 방식: 단일 아이템 감지 + 자동 카테고리 분류")
            else:
                print("  🎯 모델 선택: Segformer (HuggingFace)")
                print(f"  📸 이미지 타입: {image_type} (전신 사진)")
                print("  🔍 처리 방식: 다중 의류 아이템 감지 (상의/하의/신발/가방 등)")
            print(f"{'='*60}\n")

            # Step 1: Background Removal (0% → 33%)
            if worker:
                worker.send_progress(cloth_id, user_id, "PROCESSING", "배경 제거 중...", 10)
            print(f"  [10%] 배경 제거 중...")
            removed_bg_image = self.remove_background(image_bytes)
            removed_bg_path = REMOVED_BG_DIR / f"{cloth_id}.png"
            removed_bg_image.save(removed_bg_path)
            if worker:
                worker.send_progress(cloth_id, user_id, "PROCESSING", "배경 제거 완료", 33)
            print(f"  [33%] 배경 제거 완료")

            # Step 2: Cloth Segmentation (33% → 66%)
            if worker:
                worker.send_progress(cloth_id, user_id, "PROCESSING", "옷 영역 분석 중...", 40)
            print(f"  [40%] 옷 영역 분석 중...")

            if use_u2net:
                # U2NET 모델 사용 (단일 옷 이미지)
                primary_item, all_detected_items = self.segment_clothing_u2net(removed_bg_image)

                # U2NET: fullsize 버전도 저장 (배경만 투명, 원본 크기 유지)
                if "fullsize_image" in primary_item:
                    fullsize_image = primary_item["fullsize_image"]
                    fullsize_path = SEGMENTED_DIR / f"{cloth_id}_fullsize.png"
                    fullsize_image.save(fullsize_path)
                    print(f"  💾 Fullsize 이미지 저장: {fullsize_path}")
            else:
                # Segformer 모델 사용 (전신 사진)
                primary_item, all_detected_items = self.segment_clothing(removed_bg_image)

            # 모든 감지된 아이템 저장 (크롭 버전) - 픽셀 크기순 정렬
            all_detected_items_sorted = sorted(all_detected_items, key=lambda x: x["area_pixels"], reverse=True)

            # Primary 아이템은 가장 큰 것
            primary_item = all_detected_items_sorted[0]
            cropped_image = primary_item["cropped_image"]
            segmented_path = SEGMENTED_DIR / f"{cloth_id}.png"
            cropped_image.save(segmented_path)
            print(f"  💾 메인 크롭 이미지 저장: {segmented_path} ({primary_item['label']}, {primary_item['area_pixels']} pixels)")

            # 모든 아이템의 크롭 버전 저장 (추가 아이템들)
            all_segmented_items = []
            all_segmented_items.append({
                "label": primary_item["label"],
                "segmentedPath": str(segmented_path.absolute()),
                "areaPixels": primary_item["area_pixels"]
            })

            additional_count = len(all_detected_items_sorted) - 1
            for idx, item in enumerate(all_detected_items_sorted[1:], 1):  # primary 제외
                item_segmented_path = SEGMENTED_DIR / f"{cloth_id}_{item['label']}.png"
                item["cropped_image"].save(item_segmented_path)
                print(f"  💾 추가 크롭 이미지 저장: {item_segmented_path} ({item['label']}, {item['area_pixels']} pixels)")

                all_segmented_items.append({
                    "label": item["label"],
                    "segmentedPath": str(item_segmented_path.absolute()),
                    "areaPixels": item["area_pixels"]
                })

            print(f"  💾 Found {len(all_detected_items)} clothing item(s) (1 primary + {additional_count} additional)")

            if worker:
                worker.send_progress(cloth_id, user_id, "PROCESSING", "옷 영역 분석 완료", 66)
            print(f"  [66%] 옷 영역 분석 완료")

            # Step 3: Google AI Imagen 확장 - Primary item (66% → 70%)
            if self.use_imagen:
                if worker:
                    worker.send_progress(cloth_id, user_id, "PROCESSING", "메인 아이템 확장 중...", 68)
                print(f"  [68%] 메인 아이템 Nano Banana 확장 중...")
                cropped_image = self.imagen_service.expand_image(cropped_image)

                # Nano Banana 확장 결과 저장
                expanded_path = EXPANDED_DIR / f"{cloth_id}.png"
                cropped_image.save(expanded_path)
                print(f"  [69%] 메인 아이템 Nano Banana 확장 이미지 저장: {expanded_path}")
                print(f"  [70%] 메인 아이템 Nano Banana 확장 완료")
            else:
                print(f"  [68%] Nano Banana 비활성화 - 메인 아이템 확장 건너뜀")

            # Step 4: Inpainting - Primary item (70% → 75%)
            if worker:
                worker.send_progress(cloth_id, user_id, "PROCESSING", "메인 아이템 복원 중...", 72)
            print(f"  [72%] 메인 아이템 인페인팅 중...")
            inpainted_image = self.inpaint_image(cropped_image)
            inpainted_path = INPAINTED_DIR / f"{cloth_id}.png"
            inpainted_image.save(inpainted_path)
            print(f"  [75%] 메인 아이템 복원 완료")

            # Step 4: Gemini 확장 - Additional items (75% → 95%)
            progress = 75
            progress_step = 20 / max(len(all_detected_items_sorted), 1)  # 남은 20%를 모든 아이템들로 분배

            all_expanded_items = []

            # Primary item도 expanded_items에 추가
            all_expanded_items.append({
                "label": primary_item["label"],
                "expandedPath": str(expanded_path.absolute()) if self.use_imagen else str(segmented_path.absolute()),
                "areaPixels": primary_item["area_pixels"]
            })

            # 추가 아이템들 처리 (크기순으로 정렬된 순서대로)
            for idx, item in enumerate(all_detected_items_sorted[1:], 1):  # primary 제외
                item_image = item["cropped_image"]

                # Google AI Imagen 확장 (무조건 처리)
                if self.use_imagen:
                    if worker:
                        worker.send_progress(
                            cloth_id, user_id, "PROCESSING",
                            f"추가 아이템 확장 중... ({idx}/{additional_count})",
                            int(progress)
                        )
                    print(f"  [{int(progress)}%] 추가 아이템 Nano Banana 확장 중... ({idx}/{additional_count}): {item['label']}")
                    item_image = self.imagen_service.expand_image(item_image)

                    # Nano Banana 확장 결과 저장
                    expanded_item_path = EXPANDED_DIR / f"{cloth_id}_{item['label']}.png"
                    item_image.save(expanded_item_path)
                    print(f"  [{int(progress)}%] 추가 아이템 Nano Banana 확장 이미지 저장: {expanded_item_path}")

                    all_expanded_items.append({
                        "label": item["label"],
                        "expandedPath": str(expanded_item_path.absolute()),
                        "areaPixels": item["area_pixels"]
                    })
                else:
                    # Gemini 비활성화 시 크롭 이미지 경로 사용
                    item_segmented_path = SEGMENTED_DIR / f"{cloth_id}_{item['label']}.png"
                    all_expanded_items.append({
                        "label": item["label"],
                        "expandedPath": str(item_segmented_path.absolute()),
                        "areaPixels": item["area_pixels"]
                    })
                    print(f"  [{int(progress)}%] Nano Banana 비활성화 - 추가 아이템 확장 건너뜀: {item['label']}")

                # 인페인팅 (현재는 그냥 확장된 이미지 반환)
                # inpainted = expanded이므로 별도 저장 불필요

                progress += progress_step

            if worker:
                worker.send_progress(cloth_id, user_id, "PROCESSING", "모든 아이템 확장 완료", 95)
            print(f"  [95%] 모든 아이템 확장 완료 (총 {len(all_expanded_items)}개)")

            # 카테고리 매핑
            suggested_category = CATEGORY_MAPPING.get(primary_item["label"], "ACC")  # 기본값: ACC

            result = {
                "clothId": cloth_id,
                "success": True,
                "errorMessage": None,
                "removedBgImagePath": str(removed_bg_path.absolute()),
                "segmentedImagePath": str(segmented_path.absolute()),  # primary만 (하위 호환)
                "inpaintedImagePath": str(inpainted_path.absolute()),  # primary만 (하위 호환)
                "suggestedCategory": suggested_category,
                "segmentationLabel": primary_item["label"],
                "areaPixels": primary_item["area_pixels"],
                # 새로운 필드: 모든 아이템 (크기순 정렬)
                "allSegmentedItems": all_segmented_items,  # 모든 크롭 이미지
                "allExpandedItems": all_expanded_items,    # 모든 Gemini 확장 이미지
                # 하위 호환용 (deprecated)
                "additionalClothingItems": [
                    {"label": item["label"], "path": item["expandedPath"], "area_pixels": item["areaPixels"]}
                    for item in all_expanded_items[1:]  # primary 제외
                ]
            }

            print(f"\n{'='*60}")
            print(f"✅ Processing completed: {suggested_category}")
            print(f"   🤖 사용된 모델: {'U2NET' if use_u2net else 'Segformer'}")
            print(f"   📸 이미지 타입: {image_type}")
            print(f"   📦 감지된 아이템: {len(all_segmented_items)}개 (Segmented)")
            print(f"   🎨 확장된 아이템: {len(all_expanded_items)}개 (Imagen)")
            print(f"   🏷️  Primary 카테고리: {suggested_category} ({primary_item['label']})")
            print(f"{'='*60}\n")
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
    """RabbitMQ Worker"""

    def __init__(self):
        self.pipeline = ClothProcessingPipeline()
        self.connection = None
        self.channel = None
        self.user_id = None  # 현재 처리 중인 사용자 ID

    def connect(self):
        """RabbitMQ 연결"""
        credentials = pika.PlainCredentials(RABBITMQ_USER, RABBITMQ_PASS)
        parameters = pika.ConnectionParameters(
            host=RABBITMQ_HOST,
            port=RABBITMQ_PORT,
            credentials=credentials,
            heartbeat=600,
            blocked_connection_timeout=300
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

        # QoS 설정 (한 번에 1개만 처리)
        self.channel.basic_qos(prefetch_count=1)

        print(f"✅ Connected to RabbitMQ at {RABBITMQ_HOST}:{RABBITMQ_PORT}")

    def on_message(self, ch, method, properties, body):
        """메시지 수신 콜백"""
        try:
            # 메시지 파싱
            message = json.loads(body)
            cloth_id = message["clothId"]
            user_id = message.get("userId")  # userId 추출
            image_bytes_data = message["imageBytes"]
            original_filename = message["originalFilename"]
            image_type = message.get("imageType", "FULL_BODY")  # imageType 추출 (기본값: FULL_BODY)

            print(f"\n📨 Received message: clothId={cloth_id}, userId={user_id}, filename={original_filename}, imageType={image_type}")

            # userId 저장 (진행도 전송에 필요)
            self.user_id = user_id

            # imageBytes 처리 (Jackson이 byte[]를 숫자 배열로 직렬화함)
            if isinstance(image_bytes_data, str):
                # Base64 문자열인 경우
                image_bytes = base64.b64decode(image_bytes_data)
            elif isinstance(image_bytes_data, list):
                # 숫자 배열인 경우 (Jackson 기본 직렬화)
                image_bytes = bytes(image_bytes_data)
            else:
                raise ValueError(f"Unsupported imageBytes format: {type(image_bytes_data)}")

            # 파이프라인 실행 (worker 인스턴스 전달하여 진행도 전송 가능하게)
            result = self.pipeline.process(cloth_id, user_id, image_bytes, image_type, self)

            # 결과 전송
            self.send_result(result)

            # ACK
            ch.basic_ack(delivery_tag=method.delivery_tag)
            print(f"✅ Message processed and acknowledged\n")

        except Exception as e:
            print(f"❌ Error processing message: {str(e)}")
            traceback.print_exc()

            # NACK (재시도)
            ch.basic_nack(delivery_tag=method.delivery_tag, requeue=True)

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
                delivery_mode=2,  # persistent
                content_type='application/json'
            )
        )

        print(f"📤 Result sent to {RESULT_QUEUE}")

    def start(self):
        """Worker 시작"""
        self.connect()

        print(f"🎯 Listening on queue: {REQUEST_QUEUE}")
        print("Waiting for messages. To exit press CTRL+C\n")

        self.channel.basic_consume(
            queue=REQUEST_QUEUE,
            on_message_callback=self.on_message
        )

        try:
            self.channel.start_consuming()
        except KeyboardInterrupt:
            print("\n⛔ Stopping worker...")
            self.channel.stop_consuming()
        finally:
            if self.connection:
                self.connection.close()
            print("👋 Worker stopped")


if __name__ == "__main__":
    worker = ClothProcessingWorker()
    worker.start()
