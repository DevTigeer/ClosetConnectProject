"""
rembg Background Removal API Server
Python FastAPI 기반 이미지 배경 제거 서버

설치 방법:
pip install fastapi uvicorn rembg pillow python-multipart

실행 방법:
python rembg_server.py

또는:
uvicorn rembg_server:app --host 0.0.0.0 --port 8001 --reload
"""

from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.responses import StreamingResponse
from fastapi.middleware.cors import CORSMiddleware
from rembg import remove
from PIL import Image
import io
import logging

# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# FastAPI 앱 생성
app = FastAPI(
    title="rembg Background Removal API",
    description="이미지 배경 제거 서비스 (ClosetConnect 전용)",
    version="1.0.0"
)

# CORS 설정 (Spring Boot 서버에서 접근 허용)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8080"],  # Spring Boot 서버
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/")
async def root():
    """
    루트 엔드포인트 - API 정보 반환
    """
    return {
        "message": "rembg Background Removal API",
        "version": "1.0.0",
        "endpoints": {
            "remove_bg": "POST /remove-bg",
            "health": "GET /health"
        }
    }


@app.get("/health")
async def health_check():
    """
    헬스 체크 엔드포인트
    """
    return {"status": "healthy"}


@app.post("/remove-bg")
async def remove_background(file: UploadFile = File(...)):
    """
    이미지 배경 제거 엔드포인트

    Parameters:
    - file: UploadFile - 업로드된 이미지 파일

    Returns:
    - StreamingResponse - 배경이 제거된 PNG 이미지

    Raises:
    - HTTPException(400) - 잘못된 파일 형식
    - HTTPException(500) - 처리 중 에러 발생
    """
    logger.info(f"Received background removal request: {file.filename}")

    # 파일 형식 검증
    if not file.content_type.startswith("image/"):
        logger.error(f"Invalid file type: {file.content_type}")
        raise HTTPException(
            status_code=400,
            detail=f"이미지 파일만 업로드 가능합니다. (현재: {file.content_type})"
        )

    try:
        # 1. 업로드된 파일 읽기
        input_image_bytes = await file.read()
        logger.info(f"Read image file: {len(input_image_bytes)} bytes")

        # 2. rembg로 배경 제거
        logger.info("Starting background removal...")
        output_image_bytes = remove(input_image_bytes)
        logger.info(f"Background removal completed: {len(output_image_bytes)} bytes")

        # 3. PNG로 변환하여 반환
        return StreamingResponse(
            io.BytesIO(output_image_bytes),
            media_type="image/png",
            headers={
                "Content-Disposition": "attachment; filename=removed_bg.png",
                "X-Original-Filename": file.filename or "unknown"
            }
        )

    except Exception as e:
        logger.error(f"Failed to remove background: {str(e)}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"배경 제거 실패: {str(e)}"
        )


if __name__ == "__main__":
    import uvicorn

    logger.info("Starting rembg Background Removal API Server...")
    logger.info("Server will be available at: http://localhost:8001")
    logger.info("API documentation: http://localhost:8001/docs")

    uvicorn.run(
        app,
        host="0.0.0.0",
        port=8001,
        log_level="info"
    )
