from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image
import torch
import numpy as np
from transformers import AutoImageProcessor, AutoModelForSemanticSegmentation
import io
import base64
from datetime import datetime
import os
from pathlib import Path

app = FastAPI(title="Cloth Segmentation API")

# CORS 설정 (Spring Boot 서버와 Vercel 프론트엔드 허용)
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

# 의상 카테고리 정의``
CLOTH_LABELS = {
    0: "background",
    1: "hat",
    2: "hair",
    3: "sunglasses",
    4: "upper-clothes",  # 상의
    5: "skirt",
    6: "pants",  # 하의
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

# 상의/하의 카테고리 매핑
CATEGORY_MAPPING = {
    "upper-clothes": "상의",
    "dress": "원피스",
    "pants": "하의",
    "skirt": "하의",
    "hat": "모자",
    "shoes": "신발",
    "bag": "가방",
    "scarf": "스카프"
}

class ClothSegmentationModel:
    def __init__(self):
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        print(f"Using device: {self.device}")
        
        # Hugging Face 모델 로드 (mattmdjaga/segformer_b2_clothes)
        model_name = "mattmdjaga/segformer_b2_clothes"
        self.processor = AutoImageProcessor.from_pretrained(model_name)
        self.model = AutoModelForSemanticSegmentation.from_pretrained(model_name)
        self.model.to(self.device)
        self.model.eval()
        
        # 결과 저장 디렉토리 생성
        # 절대 경로 사용 (Java 서버에서 접근 가능하도록)
        base_dir = Path(__file__).parent.parent  # ClosetConnectProject 디렉토리
        self.output_dir = base_dir / "outputs" / "segmented_clothes"
        self.output_dir.mkdir(parents=True, exist_ok=True)
    
    def segment_image(self, image: Image.Image):
        """이미지에서 의상 세그멘테이션 수행"""
        # 전처리
        inputs = self.processor(images=image, return_tensors="pt")
        inputs = {k: v.to(self.device) for k, v in inputs.items()}
        
        # 추론
        with torch.no_grad():
            outputs = self.model(**inputs)
        
        # 후처리
        logits = outputs.logits
        upsampled_logits = torch.nn.functional.interpolate(
            logits,
            size=image.size[::-1],  # (height, width)
            mode="bilinear",
            align_corners=False
        )
        
        pred_seg = upsampled_logits.argmax(dim=1)[0].cpu().numpy()
        return pred_seg
    
    def extract_cloth_items(self, image: Image.Image, segmentation_map):
        """세그멘테이션 맵에서 의상 아이템 추출"""
        unique_labels = np.unique(segmentation_map)
        
        results = []
        image_np = np.array(image)
        
        for label_id in unique_labels:
            if label_id == 0:  # background 제외
                continue
            
            label_name = CLOTH_LABELS.get(label_id, "unknown")
            
            # 상의/하의 등 주요 의상만 처리
            if label_name not in ["upper-clothes", "pants", "skirt", "dress"]:
                continue
            
            # 해당 라벨의 마스크 생성
            mask = (segmentation_map == label_id).astype(np.uint8) * 255
            
            # 바운딩 박스 계산
            coords = np.column_stack(np.where(mask > 0))
            if len(coords) == 0:
                continue
            
            y_min, x_min = coords.min(axis=0)
            y_max, x_max = coords.max(axis=0)
            
            # 해당 영역 크롭
            cropped_image = image_np[y_min:y_max, x_min:x_max]
            cropped_mask = mask[y_min:y_max, x_min:x_max]
            
            # 마스크 적용 (배경을 흰색으로)
            masked_image = cropped_image.copy()
            mask_3d = np.stack([cropped_mask] * 3, axis=-1) > 0
            masked_image[~mask_3d] = 255
            
            # 이미지 저장 (로컬 파일)
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            category_kr = CATEGORY_MAPPING.get(label_name, label_name)
            filename = f"{category_kr}_{label_name}_{timestamp}.png"
            filepath = self.output_dir / filename

            pil_image = Image.fromarray(masked_image)
            pil_image.save(filepath)

            # 이미지를 base64로 인코딩 (CloudRun Worker에서 사용)
            img_byte_arr = io.BytesIO()
            pil_image.save(img_byte_arr, format='PNG')
            img_byte_arr.seek(0)
            image_base64 = base64.b64encode(img_byte_arr.read()).decode('utf-8')

            results.append({
                "label": label_name,
                "category_kr": category_kr,
                "bbox": {
                    "x_min": int(x_min),
                    "y_min": int(y_min),
                    "x_max": int(x_max),
                    "y_max": int(y_max)
                },
                "saved_path": str(filepath),  # 로컬 저장용 (참고용)
                "image_base64": image_base64,  # CloudRun Worker에서 사용할 이미지 데이터
                "area_pixels": int(np.sum(mask > 0))
            })
        
        return results

# 글로벌 모델 인스턴스
model = None

@app.on_event("startup")
async def startup_event():
    """API 시작 시 모델 로드"""
    global model
    print("Loading cloth segmentation model...")
    model = ClothSegmentationModel()
    print("Model loaded successfully!")

@app.get("/")
async def root():
    return {
        "message": "Cloth Segmentation API",
        "endpoints": {
            "/segment": "POST - Upload image for cloth segmentation",
            "/health": "GET - Check API health"
        }
    }

@app.get("/health")
async def health_check():
    return {
        "status": "healthy",
        "model_loaded": model is not None,
        "device": str(model.device) if model else "not loaded"
    }

@app.post("/segment")
async def segment_clothes(file: UploadFile = File(...)):
    """
    이미지를 업로드하여 의상 세그멘테이션 수행
    
    Returns:
        - detected_items: 검출된 의상 아이템 목록
        - saved_images: 저장된 이미지 경로
    """
    if model is None:
        raise HTTPException(status_code=503, detail="Model not loaded")
    
    try:
        # 이미지 읽기
        contents = await file.read()
        image = Image.open(io.BytesIO(contents)).convert("RGB")
        
        # 세그멘테이션 수행
        segmentation_map = model.segment_image(image)
        
        # 의상 아이템 추출 및 저장
        results = model.extract_cloth_items(image, segmentation_map)
        
        if not results:
            return {
                "status": "no_clothes_detected",
                "message": "이미지에서 의상을 찾을 수 없습니다.",
                "detected_items": []
            }
        
        return {
            "status": "success",
            "message": f"{len(results)}개의 의상 아이템이 검출되었습니다.",
            "detected_items": results,
            "summary": {
                "total_items": len(results),
                "categories": list(set(item["category_kr"] for item in results))
            }
        }
    
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error processing image: {str(e)}")

if __name__ == "__main__":
    import uvicorn
    import os

    port = int(os.getenv("PORT", "8002"))
    uvicorn.run(app, host="0.0.0.0", port=port)
