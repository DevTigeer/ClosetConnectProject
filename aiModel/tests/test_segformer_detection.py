"""
Segformer 다중 의류 감지 + 크롭 테스트 스크립트
- 이미지에서 감지되는 의류 아이템 개수와 종류 확인
- 각 의류 아이템을 실제로 세그멘테이션하고 크롭하여 저장
"""

import sys
from pathlib import Path
from PIL import Image
import numpy as np
from datetime import datetime

# 프로젝트 경로 추가
sys.path.append(str(Path(__file__).parent))

try:
    from cloth_segmentation_api import ClothSegmentationModel
    print("✅ ClothSegmentationModel import 성공")
except ImportError:
    print("❌ ClothSegmentationModel import 실패")
    print("   transformers 사용으로 대체")
    from transformers import AutoImageProcessor, AutoModelForSemanticSegmentation
    import torch

# 의류 라벨 정의 (Segformer 18 classes)
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

# 의류 아이템만 (신체 부위 제외)
CLOTHING_ITEMS = {1, 4, 5, 6, 7, 8, 9, 16, 17}  # hat, upper-clothes, skirt, pants, dress, belt, shoes, bag, scarf


def extract_and_crop_item(image, pred_seg, label_id, label_name, output_dir):
    """
    특정 라벨의 의류 아이템을 추출하고 크롭

    Args:
        image: 원본 PIL Image (RGB)
        pred_seg: 세그멘테이션 결과 (numpy array)
        label_id: 추출할 라벨 ID
        label_name: 라벨 이름
        output_dir: 저장할 디렉토리

    Returns:
        cropped_image: 크롭된 PIL Image (RGBA)
    """
    # 1. 해당 라벨의 마스크 생성
    mask = (pred_seg == label_id).astype(np.uint8)

    # 픽셀 수 확인
    pixel_count = np.sum(mask)
    if pixel_count == 0:
        print(f"  ⚠️  {label_name}: 픽셀이 없습니다.")
        return None

    # 2. 원본 이미지를 numpy array로 변환
    image_np = np.array(image)

    # 3. 바운딩 박스 계산
    rows = np.any(mask, axis=1)
    cols = np.any(mask, axis=0)
    y_min, y_max = np.where(rows)[0][[0, -1]]
    x_min, x_max = np.where(cols)[0][[0, -1]]

    # 4. 패딩 추가 (2% adaptive padding)
    height, width = mask.shape
    bbox_width = x_max - x_min
    bbox_height = y_max - y_min
    padding = max(5, min(20, int(0.02 * max(bbox_width, bbox_height))))

    y_min = max(0, y_min - padding)
    y_max = min(height, y_max + padding)
    x_min = max(0, x_min - padding)
    x_max = min(width, x_max + padding)

    # 5. 크롭된 영역 추출
    cropped_image_np = image_np[y_min:y_max, x_min:x_max]
    cropped_mask = mask[y_min:y_max, x_min:x_max]

    # 6. 알파 채널 생성 (마스크를 알파로 사용)
    alpha_channel = (cropped_mask * 255).astype(np.uint8)

    # 7. RGBA 이미지 생성
    image_rgba = np.dstack([cropped_image_np, alpha_channel])
    result_image = Image.fromarray(image_rgba, mode='RGBA')

    # 8. 저장
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    filename = f"{label_name}_{timestamp}.png"
    save_path = output_dir / filename
    result_image.save(save_path)

    print(f"  ✅ {label_name}: {result_image.size} → {save_path.name}")

    return result_image


def test_segformer_detection(image_path):
    """
    Segformer 모델로 이미지에서 의류 아이템 감지 테스트

    Args:
        image_path: 테스트할 이미지 경로
    """
    print(f"\n{'='*70}")
    print(f"🔬 Segformer 다중 의류 감지 테스트")
    print(f"{'='*70}\n")

    # 1. 이미지 로드
    print(f"📷 이미지 로드 중: {image_path}")
    image_path = Path(image_path)

    if not image_path.exists():
        print(f"❌ 이미지를 찾을 수 없습니다: {image_path}")
        return

    try:
        image = Image.open(image_path).convert("RGB")
        print(f"✅ 이미지 로드 완료: {image.size} (W×H)\n")
    except Exception as e:
        print(f"❌ 이미지 로드 실패: {e}")
        return

    # 2. 모델 로드
    print("📦 Segformer 모델 로딩 중...")
    try:
        # ClothSegmentationModel 사용 시도
        model = ClothSegmentationModel()
        print("✅ ClothSegmentationModel 로드 완료\n")
        use_custom_model = True
    except:
        # transformers 직접 사용
        print("   ClothSegmentationModel 사용 불가, transformers 직접 사용")
        model_name = "mattmdjaga/segformer_b2_clothes"
        processor = AutoImageProcessor.from_pretrained(model_name)
        model = AutoModelForSemanticSegmentation.from_pretrained(model_name)
        device = torch.device("cpu")
        model.to(device)
        model.eval()
        print("✅ Transformers 모델 로드 완료\n")
        use_custom_model = False

    # 3. 세그멘테이션 수행
    print("🎯 세그멘테이션 수행 중...")
    try:
        if use_custom_model:
            pred_seg = model.segment_image(image)
        else:
            # transformers 직접 사용
            inputs = processor(images=image, return_tensors="pt")
            with torch.no_grad():
                outputs = model(**inputs)
            logits = outputs.logits
            pred_seg = torch.argmax(logits, dim=1).squeeze().cpu().numpy()

        print("✅ 세그멘테이션 완료\n")
    except Exception as e:
        print(f"❌ 세그멘테이션 실패: {e}")
        import traceback
        traceback.print_exc()
        return

    # 3.5. 신발 통합 (left-shoe + right-shoe → shoes)
    left_shoe_exists = 9 in pred_seg
    right_shoe_exists = 10 in pred_seg

    if left_shoe_exists or right_shoe_exists:
        print(f"👟 신발 통합: left={left_shoe_exists}, right={right_shoe_exists}")
        pred_seg[pred_seg == 10] = 9  # right-shoe를 left-shoe로 통합
        print(f"👟 신발 통합 완료: left-shoe + right-shoe → shoes\n")

    # 4. 결과 분석
    print(f"📊 세그멘테이션 결과 분석:")
    print(f"{'='*70}\n")

    # 감지된 모든 클래스
    unique_labels = np.unique(pred_seg)
    print(f"📋 감지된 전체 클래스 ({len(unique_labels)}개):")
    print(f"{'-'*70}")

    all_items = []
    for label_id in unique_labels:
        pixel_count = np.sum(pred_seg == label_id)
        percentage = (pixel_count / pred_seg.size) * 100
        label_name = CLOTH_LABELS.get(label_id, f"Unknown ({label_id})")

        is_clothing = label_id in CLOTHING_ITEMS
        marker = "👔" if is_clothing else "  "

        all_items.append({
            'id': label_id,
            'name': label_name,
            'pixels': pixel_count,
            'percentage': percentage,
            'is_clothing': is_clothing
        })

        print(f"{marker} [{label_id:2d}] {label_name:20s}: {pixel_count:8,} pixels ({percentage:5.2f}%)")

    # 의류 아이템만 필터링
    clothing_items = [item for item in all_items if item['is_clothing']]

    print(f"\n{'='*70}")
    print(f"👔 의류 아이템만 ({len(clothing_items)}개):")
    print(f"{'-'*70}")

    if clothing_items:
        # 픽셀 수로 정렬 (많은 순)
        clothing_items_sorted = sorted(clothing_items, key=lambda x: x['pixels'], reverse=True)

        for item in clothing_items_sorted:
            print(f"   • {item['name']:20s}: {item['pixels']:8,} pixels ({item['percentage']:5.2f}%)")

        # 메인 아이템 (가장 큰 것)
        main_item = clothing_items_sorted[0]
        print(f"\n🎯 메인 아이템: {main_item['name']} ({main_item['pixels']:,} pixels, {main_item['percentage']:.2f}%)")

        if len(clothing_items_sorted) > 1:
            print(f"\n✨ 추가 아이템 ({len(clothing_items_sorted) - 1}개):")
            for item in clothing_items_sorted[1:]:
                print(f"   • {item['name']:20s}: {item['pixels']:8,} pixels ({item['percentage']:5.2f}%)")
    else:
        print("   ⚠️  의류 아이템이 감지되지 않았습니다.")

    print(f"\n{'='*70}")
    print(f"✅ 감지 테스트 완료!")
    print(f"{'='*70}\n")

    return image, pred_seg, clothing_items


if __name__ == "__main__":
    # 테스트 이미지 경로
    image_path = "/Users/grail/Desktop/스크린샷 2025-12-27 11.09.25.png"

    print("\n" + "="*70)
    print("Segformer 다중 의류 감지 + 크롭 테스트 시작")
    print("="*70)

    # 1. 감지 테스트
    result = test_segformer_detection(image_path)

    if result:
        image, pred_seg, clothing_items = result

        print(f"\n최종 결과: {len(clothing_items)}개의 의류 아이템 감지됨")
        print("\n감지된 아이템 목록:")
        for i, item in enumerate(clothing_items, 1):
            print(f"  {i}. {item['name']}")

        # 2. 각 아이템 크롭 및 저장
        print(f"\n{'='*70}")
        print("📦 각 아이템 크롭 및 저장 시작")
        print(f"{'='*70}\n")

        # 출력 디렉토리 생성
        output_dir = Path(__file__).parent / "test_output_segmented"
        output_dir.mkdir(exist_ok=True)
        print(f"📁 출력 디렉토리: {output_dir}\n")

        cropped_results = []
        for item in clothing_items:
            label_id = item['id']
            label_name = item['name']
            print(f"🔧 {label_name} 크롭 중...")

            cropped = extract_and_crop_item(
                image=image,
                pred_seg=pred_seg,
                label_id=label_id,
                label_name=label_name,
                output_dir=output_dir
            )

            if cropped:
                cropped_results.append({
                    'name': label_name,
                    'image': cropped,
                    'pixels': item['pixels']
                })

        print(f"\n{'='*70}")
        print(f"✅ 크롭 완료! {len(cropped_results)}개 아이템 저장됨")
        print(f"{'='*70}\n")

        print(f"💾 저장된 파일:")
        for item in cropped_results:
            print(f"  • {item['name']}: {item['image'].size} ({item['pixels']:,} pixels)")

        print(f"\n📂 저장 위치: {output_dir}")
    else:
        print("\n❌ 감지된 의류 아이템이 없습니다.")
