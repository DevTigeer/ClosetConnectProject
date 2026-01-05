"""
간단한 Cloth Segmentation 테스트 스크립트
- 이미지 파일을 입력받아 세그멘테이션 수행
- 감지된 의류 아이템을 크롭하여 저장
- 결과를 터미널에 출력

사용법:
    python test_segmentation.py <이미지_경로>

예시:
    python test_segmentation.py test/sample.jpg
"""

import sys
import torch
import numpy as np
from PIL import Image
from pathlib import Path
from transformers import AutoImageProcessor, AutoModelForSemanticSegmentation

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
    9: "left-shoe",
    10: "right-shoe",
    11: "face",
    12: "left-leg",
    13: "right-leg",
    14: "left-arm",
    15: "right-arm",
    16: "bag",
    17: "scarf"
}

# 카테고리 매핑
CATEGORY_MAPPING = {
    "upper-clothes": "TOP",
    "dress": "TOP",
    "pants": "BOTTOM",
    "skirt": "BOTTOM",
    "hat": "ACC",
    "bag": "ACC",
    "scarf": "ACC",
    "left-shoe": "SHOES",
    "right-shoe": "SHOES"
}

# 제외할 라벨 (머리카락, 얼굴, 팔/다리)
EXCLUDE_LABELS = {2, 11, 12, 13, 14, 15}


class SimpleClothSegmentation:
    def __init__(self):
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        print(f"🚀 Device: {self.device}")

        # Hugging Face 모델 로드
        model_name = "mattmdjaga/segformer_b2_clothes"
        print(f"📦 Loading model: {model_name}...")

        self.processor = AutoImageProcessor.from_pretrained(model_name)
        self.model = AutoModelForSemanticSegmentation.from_pretrained(model_name)
        self.model.to(self.device)
        self.model.eval()

        print("✅ Model loaded successfully!\n")

    def segment_image(self, image_path):
        """이미지 세그멘테이션 수행"""
        # 이미지 로드
        image = Image.open(image_path).convert("RGB")
        print(f"📷 입력 이미지: {image.size} (W×H)")

        # 전처리
        inputs = self.processor(
            images=image,
            return_tensors="pt",
            do_rescale=True,
            do_normalize=True
        )
        inputs = {k: v.to(self.device) for k, v in inputs.items()}

        # 추론
        print("🔍 세그멘테이션 수행 중...")
        with torch.no_grad():
            outputs = self.model(**inputs)

        # 후처리
        logits = outputs.logits
        upsampled_logits = torch.nn.functional.interpolate(
            logits,
            size=image.size[::-1],
            mode="bilinear",
            align_corners=False
        )

        pred_seg = upsampled_logits.argmax(dim=1)[0].cpu().numpy()

        # 의류 아이템 추출
        detected_items = self._extract_cloth_items(image, pred_seg)

        return detected_items

    def _extract_cloth_items(self, image, segmentation_map):
        """세그멘테이션 맵에서 의류 아이템 추출"""
        unique_labels = np.unique(segmentation_map)
        results = []
        image_np = np.array(image)
        img_height, img_width = image_np.shape[:2]

        print(f"\n📊 감지된 라벨: {unique_labels}")
        print(f"{'='*60}")

        for label_id in unique_labels:
            if label_id == 0:  # background 제외
                continue

            # 머리카락, 얼굴, 팔/다리 제외
            if label_id in EXCLUDE_LABELS:
                continue

            label_name = CLOTH_LABELS.get(label_id, "unknown")

            # 옷 관련 라벨만 처리
            if label_name not in CATEGORY_MAPPING:
                print(f"⏭️  {label_name} (라벨 {label_id}): 의류 아님, 건너뜀")
                continue

            # 마스크 생성
            mask = (segmentation_map == label_id).astype(np.uint8)
            area_pixels = np.sum(mask)

            # 너무 작은 영역 제외
            if area_pixels < 1000:
                print(f"⚠️  {label_name} (라벨 {label_id}): 영역이 너무 작음 ({area_pixels} pixels)")
                continue

            # Bounding box 계산
            rows = np.any(mask, axis=1)
            cols = np.any(mask, axis=0)

            if not rows.any() or not cols.any():
                print(f"❌ {label_name}: bounding box 계산 실패")
                continue

            rmin, rmax = np.where(rows)[0][[0, -1]]
            cmin, cmax = np.where(cols)[0][[0, -1]]

            bbox_width = cmax - cmin
            bbox_height = rmax - rmin

            # Adaptive Padding
            adaptive_padding = max(5, min(20, int(max(bbox_width, bbox_height) * 0.02)))

            rmin_padded = max(0, rmin - adaptive_padding)
            rmax_padded = min(img_height - 1, rmax + adaptive_padding)
            cmin_padded = max(0, cmin - adaptive_padding)
            cmax_padded = min(img_width - 1, cmax + adaptive_padding)

            # 크롭
            mask_cropped = mask[rmin_padded:rmax_padded+1, cmin_padded:cmax_padded+1]
            image_cropped = image_np[rmin_padded:rmax_padded+1, cmin_padded:cmax_padded+1]

            # RGBA 이미지 생성
            alpha_channel = (mask_cropped * 255).astype(np.uint8)
            image_rgba = np.dstack([image_cropped, alpha_channel])

            cropped_pil = Image.fromarray(image_rgba, mode='RGBA')

            # 품질 확인
            opaque_ratio = np.sum(alpha_channel > 0) / alpha_channel.size

            print(f"\n✅ {label_name} (라벨 {label_id}):")
            print(f"   - 영역: {area_pixels:,} pixels")
            print(f"   - Bbox: ({cmin}, {rmin}) → ({cmax}, {rmax}) [크기: {bbox_width}×{bbox_height}]")
            print(f"   - Padding: {adaptive_padding}px")
            print(f"   - 크롭 결과: {image_rgba.shape[1]}×{image_rgba.shape[0]} (W×H)")
            print(f"   - 불투명 비율: {opaque_ratio*100:.1f}%", end="")

            if opaque_ratio < 0.3:
                print(" ⚠️ (투명 영역 과도)")
            elif opaque_ratio > 0.8:
                print(" 🎉 (양호)")
            else:
                print(" ✓")

            results.append({
                "label": label_name,
                "label_id": int(label_id),
                "category": CATEGORY_MAPPING.get(label_name, "ACC"),
                "area_pixels": int(area_pixels),
                "bbox": [int(cmin_padded), int(rmin_padded), int(cmax_padded), int(rmax_padded)],
                "cropped_image": cropped_pil,
                "opaque_ratio": float(opaque_ratio)
            })

        return results

    def save_results(self, items, output_dir="test_output", show_priority=True):
        """결과 저장"""
        output_path = Path(output_dir)
        output_path.mkdir(exist_ok=True)

        print(f"\n{'='*60}")
        print(f"💾 결과 저장 중: {output_path}/")

        # Primary item 선택 (우선순위)
        if show_priority and len(items) > 1:
            PRIORITY_ORDER = ["dress", "upper-clothes", "pants", "skirt", "left-shoe", "right-shoe", "bag", "hat"]

            primary_item = None
            for priority_label in PRIORITY_ORDER:
                for item in items:
                    if item["label"] == priority_label:
                        primary_item = item
                        break
                if primary_item:
                    break

            if not primary_item:
                primary_item = max(items, key=lambda x: x["area_pixels"])

            print(f"\n🎯 Primary Item: {primary_item['label']} (우선순위 선택)")
            print(f"   - 영역: {primary_item['area_pixels']:,} pixels")
            print(f"   - 카테고리: {primary_item['category']}")

            # Primary만 저장
            filename = f"primary_{primary_item['label']}.png"
            filepath = output_path / filename
            primary_item['cropped_image'].save(filepath)
            print(f"   ✓ {filename}")

            # Additional items
            print(f"\n📦 Additional Items ({len(items)-1}개):")
            for i, item in enumerate(items):
                if item == primary_item:
                    continue
                filename = f"additional_{i+1}_{item['label']}.png"
                filepath = output_path / filename
                item['cropped_image'].save(filepath)
                print(f"   ✓ {filename} ({item['category']})")
        else:
            # 전체 저장
            for i, item in enumerate(items):
                filename = f"{i+1}_{item['label']}.png"
                filepath = output_path / filename
                item['cropped_image'].save(filepath)
                print(f"   ✓ {filename} ({item['category']})")

        print(f"\n✅ 총 {len(items)}개 파일 저장 완료!")


def main():

    image_path = "/Users/grail/Documents/ClosetConnectProject/aiModel/test/스크린샷 2023-12-19 18.18.54.png"

    if not Path(image_path).exists():
        print(f"❌ 파일을 찾을 수 없습니다: {image_path}")
        sys.exit(1)

    print("="*60)
    print("🧥 Cloth Segmentation 테스트")
    print("="*60)
    print()

    # 세그멘테이션 수행
    segmentation = SimpleClothSegmentation()
    items = segmentation.segment_image(image_path)

    if not items:
        print("\n⚠️  의류 아이템을 찾을 수 없습니다.")
        return

    # 결과 저장
    segmentation.save_results(items)

    # 요약
    print(f"\n{'='*60}")
    print("📋 요약:")
    print(f"   - 입력: {image_path}")
    print(f"   - 감지된 아이템: {len(items)}개")
    for item in items:
        print(f"     • {item['label']} ({item['category']}): {item['opaque_ratio']*100:.1f}% 불투명")
    print("="*60)


if __name__ == "__main__":
    main()
