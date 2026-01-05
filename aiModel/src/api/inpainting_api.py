"""
Image Inpainting API using Stable Diffusion
- clothing_restorer.py를 FastAPI로 래핑
- 크롭된 옷 이미지의 잘린 부분을 AI로 복원
"""

from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.responses import StreamingResponse
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image
import io
import logging
from pathlib import Path

# Import ClothingRestorer from utils
try:
    from ..utils.clothing_restorer import ClothingRestorer
except ImportError as e:
    # 대체 구현 (Stable Diffusion 없을 경우)
    print(f"⚠️  ClothingRestorer import 실패: {e}")
    ClothingRestorer = None

# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="Clothing Inpainting API",
    description="AI-powered clothing image restoration using Stable Diffusion",
    version="1.0.0"
)

# CORS 설정 (Spring Boot 서버 허용)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8080"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 글로벌 모델 인스턴스
restorer = None


@app.on_event("startup")
async def startup_event():
    """API 시작 시 모델 로드"""
    global restorer

    if ClothingRestorer is None:
        logger.warning("ClothingRestorer not available - API will return error responses")
        return

    try:
        logger.info("Loading Stable Diffusion Inpainting model...")
        restorer = ClothingRestorer()
        logger.info("Model loaded successfully!")
    except Exception as e:
        logger.error(f"Failed to load model: {e}")
        logger.warning("API will run but inpainting will fail")


@app.get("/")
async def root():
    """루트 엔드포인트"""
    return {
        "message": "Clothing Inpainting API",
        "version": "1.0.0",
        "model_loaded": restorer is not None,
        "endpoints": {
            "/inpaint": "POST - Upload cropped image for inpainting",
            "/health": "GET - Check API health"
        }
    }


@app.get("/health")
async def health_check():
    """헬스 체크 엔드포인트"""
    return {
        "status": "healthy" if restorer is not None else "model_not_loaded",
        "model_loaded": restorer is not None,
        "device": str(restorer.device) if restorer else "not loaded"
    }


@app.post("/inpaint")
async def inpaint_image(
    file: UploadFile = File(...),
    extend_ratio: float = 0.5
):
    """
    크롭된 옷 이미지를 복원 (잘린 부분을 AI로 생성)

    Args:
        file: 크롭된 옷 이미지 파일
        extend_ratio: 캔버스 확장 비율 (기본: 0.5 = 50%)

    Returns:
        복원된 이미지 (PNG 포맷)
    """
    if restorer is None:
        raise HTTPException(
            status_code=503,
            detail="Model not loaded. Please check server logs."
        )

    try:
        logger.info(f"Inpainting request received: {file.filename}")

        # 이미지 읽기
        contents = await file.read()
        image = Image.open(io.BytesIO(contents)).convert("RGB")

        logger.info(f"Image loaded: {image.size}")

        # 임시 파일로 저장 (ClothingRestorer가 파일 경로를 요구함)
        temp_dir = Path("./outputs/temp_inpaint")
        temp_dir.mkdir(parents=True, exist_ok=True)

        temp_input = temp_dir / f"input_{file.filename}"
        image.save(temp_input)

        logger.info("Starting inpainting process...")

        # Inpainting 수행
        restored_image = restorer.restore(
            str(temp_input),
            extend_ratio=extend_ratio
        )

        logger.info("Inpainting completed successfully")

        # 임시 파일 삭제
        temp_input.unlink(missing_ok=True)

        # PNG로 변환 (메모리 버퍼)
        img_byte_arr = io.BytesIO()
        restored_image.save(img_byte_arr, format='PNG')
        img_byte_arr.seek(0)

        return StreamingResponse(
            img_byte_arr,
            media_type="image/png",
            headers={
                "Content-Disposition": f"attachment; filename=inpainted_{file.filename}"
            }
        )

    except Exception as e:
        logger.error(f"Inpainting failed: {e}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"Inpainting failed: {str(e)}"
        )


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        app,
        host="0.0.0.0",
        port=8003,
        log_level="info"
    )
