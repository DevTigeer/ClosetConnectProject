"""
U2NET 단일 옷 이미지 세그멘테이션 테스트 스크립트
- huggingface-cloth-segmentation의 U2NET 모델 사용
- 단일 옷 이미지 입력하여 세그멘테이션 결과 확인
"""

import sys
from pathlib import Path
from PIL import Image
import numpy as np
import torch

# huggingface-cloth-segmentation 경로 추가
sys.path.append(str(Path(__file__).parent / "huggingface-cloth-segmentation"))

try:
    from process import load_seg_model, get_palette, generate_mask
    print("✅ U2NET 모듈 import 성공")
except ImportError as e:
    print(f"❌ U2NET 모듈 import 실패: {e}")
    sys.exit(1)


def test_u2net_segmentation(image_path, output_dir="test_outputs"):
    """
    U2NET 모델로 단일 옷 이미지 세그멘테이션 테스트

    Args:
        image_path: 입력 이미지 경로
        output_dir: 출력 디렉토리
    """
    print(f"\n{'='*60}")
    print(f"🔬 U2NET 세그멘테이션 테스트")
    print(f"{'='*60}\n")

    # 1. 디바이스 설정
    device = "cuda" if torch.cuda.is_available() else "cpu"
    print(f"📱 디바이스: {device}")

    # 2. 모델 로드
    print("\n📦 U2NET 모델 로딩 중...")
    checkpoint_path = Path(__file__).parent.parent / "model" / "cloth_segm.pth"

    if not checkpoint_path.exists():
        print(f"❌ 모델 체크포인트를 찾을 수 없습니다: {checkpoint_path}")
        print(f"   다운로드 필요: https://github.com/wildoctopus/huggingface-cloth-segmentation")
        return

    try:
        model = load_seg_model(str(checkpoint_path), device=device)
        palette = get_palette(4)  # 4 classes
        print("✅ 모델 로드 완료")
    except Exception as e:
        print(f"❌ 모델 로드 실패: {e}")
        return

    # 3. 이미지 로드
    print(f"\n📷 이미지 로드 중: {image_path}")
    image_path = Path(image_path)

    if not image_path.exists():
        print(f"❌ 이미지를 찾을 수 없습니다: {image_path}")
        return

    try:
        image = Image.open(image_path).convert("RGB")
        print(f"✅ 이미지 로드 완료: {image.size} (W×H)")
    except Exception as e:
        print(f"❌ 이미지 로드 실패: {e}")
        return

    # 4. 세그멘테이션 수행
    print(f"\n🎯 세그멘테이션 수행 중...")
    try:
        cloth_mask = generate_mask(image, model, palette, device=device)
        print("✅ 세그멘테이션 완료")
    except Exception as e:
        print(f"❌ 세그멘테이션 실패: {e}")
        import traceback
        traceback.print_exc()
        return

    # 5. 결과 분석
    print(f"\n📊 세그멘테이션 결과 분석:")
    mask_np = np.array(cloth_mask)
    unique_labels = np.unique(mask_np)

    label_names = {
        0: "배경 (Background)",
        1: "상의 (Upper body)",
        2: "하의 (Lower body)",
        3: "원피스 (Full body)"
    }

    print(f"   감지된 클래스:")
    for label_id in unique_labels:
        pixel_count = np.sum(mask_np == label_id)
        percentage = (pixel_count / mask_np.size) * 100
        label_name = label_names.get(label_id, f"Unknown ({label_id})")
        print(f"     - {label_name}: {pixel_count:,} pixels ({percentage:.2f}%)")

    # 6. 결과 저장
    output_path = Path(output_dir)
    output_path.mkdir(parents=True, exist_ok=True)

    # 원본 이미지 저장
    original_output = output_path / f"{image_path.stem}_original.png"
    image.save(original_output)
    print(f"\n💾 원본 이미지 저장: {original_output}")

    # 세그멘테이션 마스크 저장
    mask_output = output_path / f"{image_path.stem}_mask.png"
    cloth_mask.save(mask_output)
    print(f"💾 세그멘테이션 마스크 저장: {mask_output}")

    # 각 클래스별 알파 이미지 생성 및 저장
    print(f"\n🖼️  클래스별 크롭 이미지 생성:")
    image_np = np.array(image)

    for label_id in unique_labels:
        if label_id == 0:  # 배경 제외
            continue

        # 해당 클래스 마스크
        class_mask = (mask_np == label_id).astype(np.uint8)
        pixel_count = np.sum(class_mask)

        if pixel_count < 1000:  # 너무 작은 영역 제외
            print(f"   ⚠️  {label_names.get(label_id, f'Label {label_id}')}: 영역이 너무 작아 건너뜀 ({pixel_count} pixels)")
            continue

        # Bounding box 계산
        rows = np.any(class_mask, axis=1)
        cols = np.any(class_mask, axis=0)

        if not rows.any() or not cols.any():
            continue

        rmin, rmax = np.where(rows)[0][[0, -1]]
        cmin, cmax = np.where(cols)[0][[0, -1]]

        # Adaptive padding (2% of bbox size)
        bbox_width = cmax - cmin
        bbox_height = rmax - rmin
        padding = max(5, min(20, int(max(bbox_width, bbox_height) * 0.02)))

        img_height, img_width = image_np.shape[:2]
        rmin_padded = max(0, rmin - padding)
        rmax_padded = min(img_height - 1, rmax + padding)
        cmin_padded = max(0, cmin - padding)
        cmax_padded = min(img_width - 1, cmax + padding)

        # 크롭
        mask_cropped = class_mask[rmin_padded:rmax_padded+1, cmin_padded:cmax_padded+1]
        image_cropped = image_np[rmin_padded:rmax_padded+1, cmin_padded:cmax_padded+1]

        # RGBA 이미지 생성
        alpha_channel = (mask_cropped * 255).astype(np.uint8)
        image_rgba = np.dstack([image_cropped, alpha_channel])

        # 품질 확인
        opaque_ratio = np.sum(alpha_channel > 0) / alpha_channel.size

        # 저장
        class_name = label_names.get(label_id, f"label_{label_id}").split(" (")[0]
        class_output = output_path / f"{image_path.stem}_{class_name}.png"
        Image.fromarray(image_rgba, mode='RGBA').save(class_output)

        status = "✅" if opaque_ratio > 0.6 else "⚠️"
        print(f"   {status} {label_names.get(label_id, f'Label {label_id}')}: {class_output.name}")
        print(f"       크기: {image_rgba.shape[1]}×{image_rgba.shape[0]}, 불투명도: {opaque_ratio*100:.1f}%")

    # 7. 마스크된 부분만 원본에서 추출 (전체 이미지 크기 유지, 투명 배경)
    print(f"\n🎨 마스크된 부분만 원본에서 추출 (전체 크기 유지):")

    for label_id in unique_labels:
        if label_id == 0:  # 배경 제외
            continue

        # 해당 클래스 마스크
        class_mask = (mask_np == label_id).astype(np.uint8)
        pixel_count = np.sum(class_mask)

        if pixel_count < 1000:  # 너무 작은 영역 제외
            continue

        # 원본 이미지 크기 그대로 유지하면서 마스크 적용
        # RGB 이미지와 알파 채널(마스크) 결합
        alpha_channel_full = (class_mask * 255).astype(np.uint8)
        image_rgba_full = np.dstack([image_np, alpha_channel_full])

        # 저장
        class_name = label_names.get(label_id, f"label_{label_id}").split(" (")[0]
        full_output = output_path / f"{image_path.stem}_{class_name}_fullsize.png"
        Image.fromarray(image_rgba_full, mode='RGBA').save(full_output)

        print(f"   ✅ {label_names.get(label_id, f'Label {label_id}')}: {full_output.name}")
        print(f"       원본 크기 유지: {image_rgba_full.shape[1]}×{image_rgba_full.shape[0]}, 마스크 영역: {pixel_count:,} pixels")

    # 8. 모든 옷 클래스 합쳐서 추출 (배경만 제거)
    print(f"\n👔 모든 옷 영역 통합 추출 (배경 제거):")

    # 배경 제외한 모든 클래스 합치기
    all_cloth_mask = (mask_np > 0).astype(np.uint8)  # label 0(배경) 제외
    cloth_pixel_count = np.sum(all_cloth_mask)

    if cloth_pixel_count > 0:
        # 전체 크기 버전 (배경만 투명)
        alpha_channel_all = (all_cloth_mask * 255).astype(np.uint8)
        image_rgba_all = np.dstack([image_np, alpha_channel_all])

        all_output = output_path / f"{image_path.stem}_all_clothes_fullsize.png"
        Image.fromarray(image_rgba_all, mode='RGBA').save(all_output)
        print(f"   ✅ 모든 옷 영역: {all_output.name}")
        print(f"       원본 크기 유지: {image_rgba_all.shape[1]}×{image_rgba_all.shape[0]}")

        # 타이트 크롭 버전
        rows = np.any(all_cloth_mask, axis=1)
        cols = np.any(all_cloth_mask, axis=0)

        if rows.any() and cols.any():
            rmin, rmax = np.where(rows)[0][[0, -1]]
            cmin, cmax = np.where(cols)[0][[0, -1]]

            # Adaptive padding
            bbox_width = cmax - cmin
            bbox_height = rmax - rmin
            padding = max(5, min(20, int(max(bbox_width, bbox_height) * 0.02)))

            img_height, img_width = image_np.shape[:2]
            rmin_padded = max(0, rmin - padding)
            rmax_padded = min(img_height - 1, rmax + padding)
            cmin_padded = max(0, cmin - padding)
            cmax_padded = min(img_width - 1, cmax + padding)

            # 크롭
            mask_cropped_all = all_cloth_mask[rmin_padded:rmax_padded+1, cmin_padded:cmax_padded+1]
            image_cropped_all = image_np[rmin_padded:rmax_padded+1, cmin_padded:cmax_padded+1]

            # RGBA
            alpha_cropped_all = (mask_cropped_all * 255).astype(np.uint8)
            image_rgba_cropped_all = np.dstack([image_cropped_all, alpha_cropped_all])

            all_cropped_output = output_path / f"{image_path.stem}_all_clothes_cropped.png"
            Image.fromarray(image_rgba_cropped_all, mode='RGBA').save(all_cropped_output)
            print(f"   ✅ 모든 옷 영역 (크롭): {all_cropped_output.name}")
            print(f"       크롭 크기: {image_rgba_cropped_all.shape[1]}×{image_rgba_cropped_all.shape[0]}")

    print(f"\n{'='*60}")
    print(f"✅ 테스트 완료! 결과는 '{output_dir}' 폴더에 저장되었습니다.")
    print(f"{'='*60}\n")


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="U2NET 단일 옷 이미지 세그멘테이션 테스트")
    parser.add_argument("--image", type=str, required=True, help="입력 이미지 경로")
    parser.add_argument("--output", type=str, default="test_outputs", help="출력 디렉토리 (기본값: test_outputs)")

    image="/Users/grail/Documents/ClosetConnectProject/aiModel/test/스크린샷 2023-12-19 18.18.54.png"
    output="/Users/grail/Documents/ClosetConnectProject/aiModel/test/"
    test_u2net_segmentation(image, output)
