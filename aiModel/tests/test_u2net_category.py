"""
U2NET 카테고리 자동 감지 테스트
"""

import sys
from pathlib import Path
from PIL import Image
import numpy as np

sys.path.append(str(Path(__file__).parent / "huggingface-cloth-segmentation"))

from process import load_seg_model, get_palette, generate_mask

# 카테고리 매핑
U2NET_LABEL_MAP = {
    0: "배경 (Background)",
    1: "상의 (Upper body)",
    2: "하의 (Lower body)",
    3: "원피스 (Full body)"
}

# Spring Boot Category enum 매핑
CATEGORY_MAPPING = {
    1: "TOP",        # 상의 → TOP
    2: "BOTTOM",     # 하의 → BOTTOM
    3: "TOP",        # 원피스 → TOP (원피스도 상의로 분류)
}

def detect_category(image_path):
    """U2NET으로 카테고리 자동 감지"""

    print(f"\n{'='*60}")
    print(f"🔍 U2NET 카테고리 자동 감지 테스트")
    print(f"{'='*60}\n")

    # 모델 로드
    checkpoint_path = Path(__file__).parent.parent / "model" / "cloth_segm.pth"
    model = load_seg_model(str(checkpoint_path), device="cpu")
    palette = get_palette(4)

    # 이미지 로드
    image = Image.open(image_path).convert("RGB")
    print(f"📷 이미지: {image_path}")
    print(f"   크기: {image.size} (W×H)\n")

    # 세그멘테이션
    cloth_mask = generate_mask(image, model, palette, device="cpu")
    mask_np = np.array(cloth_mask)

    # 각 클래스별 픽셀 수 분석
    print("📊 감지된 클래스 분석:")
    unique_labels = np.unique(mask_np)

    class_pixels = {}
    for label_id in unique_labels:
        pixel_count = np.sum(mask_np == label_id)
        percentage = (pixel_count / mask_np.size) * 100
        class_pixels[label_id] = pixel_count

        label_name = U2NET_LABEL_MAP.get(label_id, f"Unknown ({label_id})")
        print(f"   {label_name}: {pixel_count:,} pixels ({percentage:.2f}%)")

    # 배경 제외하고 가장 많은 픽셀을 차지하는 클래스 찾기
    cloth_pixels = {k: v for k, v in class_pixels.items() if k != 0}

    if not cloth_pixels:
        print("\n❌ 옷 영역을 찾을 수 없습니다!")
        return None

    # 가장 많은 픽셀을 가진 클래스
    dominant_class = max(cloth_pixels, key=cloth_pixels.get)
    dominant_label = U2NET_LABEL_MAP.get(dominant_class)
    dominant_pixels = cloth_pixels[dominant_class]
    dominant_percentage = (dominant_pixels / mask_np.size) * 100

    # Spring Boot 카테고리 매핑
    spring_category = CATEGORY_MAPPING.get(dominant_class, "TOP")

    print(f"\n🎯 자동 감지 결과:")
    print(f"   지배적 클래스: {dominant_label}")
    print(f"   픽셀 수: {dominant_pixels:,} ({dominant_percentage:.2f}%)")
    print(f"   → Spring Category: {spring_category}")

    return {
        "u2net_class_id": dominant_class,
        "u2net_label": dominant_label,
        "spring_category": spring_category,
        "pixel_count": dominant_pixels,
        "percentage": dominant_percentage,
        "all_detected": cloth_pixels
    }


if __name__ == "__main__":
    # 테스트 이미지들
    test_images = [
        "/Users/grail/Documents/ClosetConnectProject/aiModel/huggingface-cloth-segmentation/input/03615_00.jpg",
        "/Users/grail/Documents/ClosetConnectProject/aiModel/test/스크린샷 2023-12-19 18.18.54.png",
    ]

    for img_path in test_images:
        if Path(img_path).exists():
            result = detect_category(img_path)
            print(f"\n{'='*60}\n")
