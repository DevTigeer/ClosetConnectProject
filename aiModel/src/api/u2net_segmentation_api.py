"""
U2NET Segmentation API for SINGLE_ITEM images
- 단일 옷 이미지 세그멘테이션
- 자동 카테고리 분류 (상의/하의/원피스)
- FastAPI 서버
"""

from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image
import torch
import numpy as np
import io
import base64
import os
from datetime import datetime
from pathlib import Path
import traceback

# U2NET 모델 import
import sys
sys.path.append(str(Path(__file__).parent.parent))
from utils.u2net_process import load_seg_model, get_palette, generate_mask

app = FastAPI(title="U2NET Segmentation API (SINGLE_ITEM)")

# CORS 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:8080",
        "https://*.railway.app",
        "https://*.vercel.app"
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# U2NET 카테고리 매핑
U2NET_LABEL_MAP = {
    1: "upper-clothes",  # 상의
    2: "pants",          # 하의
    3: "dress"           # 원피스
}


class U2NetSegmentationModel:
    def __init__(self):
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        print(f"🚀 Initializing U2NET Segmentation API")
        print(f"   Device: {self.device}")

        # U2NET 모델 로드
        checkpoint_path = Path(__file__).parent.parent.parent / "model" / "cloth_segm.pth"
        print(f"   Checkpoint path: {checkpoint_path}")

        if not checkpoint_path.exists():
            raise FileNotFoundError(f"U2NET checkpoint not found: {checkpoint_path}")

        print("   Loading U2NET model...")
        self.model = load_seg_model(str(checkpoint_path), device=str(self.device))
        self.palette = get_palette(4)
        print("   ✅ U2NET model loaded successfully")

        # 출력 디렉토리 생성
        base_dir = Path(__file__).parent.parent.parent
        self.output_dir = base_dir / "outputs" / "u2net_segmented"
        self.output_dir.mkdir(parents=True, exist_ok=True)

    def segment_single_item(self, image: Image.Image):
        """
        단일 옷 이미지 세그멘테이션

        Returns:
            dict: {
                "label": str,           # 감지된 카테고리 (upper-clothes, pants, dress)
                "area_pixels": int,     # 옷 영역 픽셀 수
                "cropped_image": PIL.Image,  # 크롭된 이미지 (RGBA)
                "fullsize_image": PIL.Image  # 배경만 투명한 원본 크기 이미지
            }
        """
        # RGB로 변환
        if image.mode == "RGBA":
            rgb_image = Image.new("RGB", image.size, (255, 255, 255))
            rgb_image.paste(image, mask=image.split()[3])
            image = rgb_image
        elif image.mode != "RGB":
            image = image.convert("RGB")

        # U2NET으로 마스크 생성
        cloth_mask = generate_mask(image, self.model, self.palette, device=str(self.device))

        # 마스크를 numpy 배열로 변환
        mask_np = np.array(cloth_mask)
        image_np = np.array(image)

        # 각 클래스의 픽셀 수 계산 (배경 제외)
        class_pixels = {}
        for class_id in [1, 2, 3]:
            pixel_count = np.sum(mask_np == class_id)
            if pixel_count > 0:
                class_pixels[class_id] = pixel_count

        # 가장 많은 픽셀을 차지하는 클래스 선택
        if not class_pixels:
            raise ValueError("No clothing detected in image")

        dominant_class = max(class_pixels, key=class_pixels.get)
        detected_label = U2NET_LABEL_MAP.get(dominant_class, "upper-clothes")

        # 배경이 아닌 모든 영역을 옷으로 간주
        cloth_mask_binary = (mask_np > 0).astype(np.uint8)
        total_cloth_pixels = np.sum(cloth_mask_binary)

        # 바운딩 박스 계산 (패딩 포함)
        rows = np.any(cloth_mask_binary, axis=1)
        cols = np.any(cloth_mask_binary, axis=0)

        if not np.any(rows) or not np.any(cols):
            raise ValueError("No valid bounding box found for clothing")

        rmin, rmax = np.where(rows)[0][[0, -1]]
        cmin, cmax = np.where(cols)[0][[0, -1]]

        # Adaptive padding (2%)
        height, width = cloth_mask_binary.shape
        bbox_width = cmax - cmin
        bbox_height = rmax - rmin
        padding = max(5, min(20, int(0.02 * max(bbox_width, bbox_height))))

        rmin_padded = max(0, rmin - padding)
        rmax_padded = min(height, rmax + padding)
        cmin_padded = max(0, cmin - padding)
        cmax_padded = min(width, cmax + padding)

        # 크롭된 이미지 생성 (RGBA)
        cropped_image_np = image_np[rmin_padded:rmax_padded, cmin_padded:cmax_padded]
        cropped_mask = cloth_mask_binary[rmin_padded:rmax_padded, cmin_padded:cmax_padded]
        alpha_channel = (cropped_mask * 255).astype(np.uint8)
        image_rgba = np.dstack([cropped_image_np, alpha_channel])
        cropped_image = Image.fromarray(image_rgba, mode='RGBA')

        # Fullsize 이미지 생성 (배경만 투명, 크기 유지)
        alpha_full = (cloth_mask_binary * 255).astype(np.uint8)
        fullsize_rgba = np.dstack([image_np, alpha_full])
        fullsize_image = Image.fromarray(fullsize_rgba, mode='RGBA')

        return {
            "label": detected_label,
            "area_pixels": int(total_cloth_pixels),
            "cropped_image": cropped_image,
            "fullsize_image": fullsize_image,
            "class_pixels": {U2NET_LABEL_MAP[k]: int(v) for k, v in class_pixels.items()}
        }


# 전역 모델 인스턴스
model = None


@app.on_event("startup")
async def startup_event():
    """서버 시작 시 모델 로드"""
    global model
    try:
        model = U2NetSegmentationModel()
        print("✅ U2NET API server ready")
    except Exception as e:
        print(f"❌ Failed to load U2NET model: {e}")
        traceback.print_exc()
        raise


@app.get("/")
async def root():
    """루트 엔드포인트"""
    return {
        "service": "U2NET Segmentation API",
        "version": "1.0",
        "model": "U2NET",
        "purpose": "Single clothing item segmentation (SINGLE_ITEM)",
        "device": str(model.device) if model else "not loaded"
    }


@app.get("/health")
async def health_check():
    """헬스체크 엔드포인트"""
    if model is None:
        raise HTTPException(status_code=503, detail="Model not loaded")

    return {
        "status": "healthy",
        "model": "U2NET",
        "device": str(model.device)
    }


@app.post("/segment")
async def segment_cloth(file: UploadFile = File(...)):
    """
    단일 옷 이미지 세그멘테이션

    Args:
        file: 이미지 파일 (JPEG, PNG)

    Returns:
        JSON: {
            "status": "success",
            "detected_item": {
                "label": str,
                "area_pixels": int,
                "image_base64": str,      # 크롭된 이미지 (base64)
                "fullsize_base64": str    # Fullsize 이미지 (base64)
            }
        }
    """
    if model is None:
        raise HTTPException(status_code=503, detail="Model not loaded")

    try:
        # 이미지 로드
        image_bytes = await file.read()
        image = Image.open(io.BytesIO(image_bytes)).convert("RGB")

        # 세그멘테이션 수행
        result = model.segment_single_item(image)

        # 크롭된 이미지를 base64로 인코딩
        cropped_buffer = io.BytesIO()
        result["cropped_image"].save(cropped_buffer, format='PNG')
        cropped_buffer.seek(0)
        cropped_base64 = base64.b64encode(cropped_buffer.read()).decode('utf-8')

        # Fullsize 이미지를 base64로 인코딩
        fullsize_buffer = io.BytesIO()
        result["fullsize_image"].save(fullsize_buffer, format='PNG')
        fullsize_buffer.seek(0)
        fullsize_base64 = base64.b64encode(fullsize_buffer.read()).decode('utf-8')

        return JSONResponse({
            "status": "success",
            "detected_item": {
                "label": result["label"],
                "area_pixels": result["area_pixels"],
                "class_pixels": result["class_pixels"],
                "image_base64": cropped_base64,
                "fullsize_base64": fullsize_base64
            }
        })

    except ValueError as e:
        # 의류 감지 실패
        raise HTTPException(status_code=400, detail=str(e))

    except Exception as e:
        # 기타 에러
        print(f"❌ Segmentation error: {e}")
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Segmentation failed: {str(e)}")


if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("PORT", "8004"))
    uvicorn.run(app, host="0.0.0.0", port=port)
