# rembg 이미지 배경 제거 연동 가이드

## 개요

ClosetConnect 프로젝트의 옷장(Closet) 기능에 Python rembg 서버를 연동하여 업로드된 옷 이미지의 배경을 자동으로 제거하는 기능입니다.

## 아키텍처

```
사용자 → Spring Boot → Python FastAPI (rembg) → 배경 제거 이미지 반환
                ↓
         원본 + 누끼 이미지 저장
                ↓
             DB 저장
```

## 설치 및 실행

### 1. Python rembg 서버 설치

```bash
# Python 가상환경 생성
python3 -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# 필요한 패키지 설치
pip install fastapi uvicorn rembg pillow python-multipart
```

### 2. Python FastAPI 서버 실행

프로젝트 루트에 `rembg_server.py` 파일을 생성하고 다음 내용을 작성합니다:

```python
from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.responses import StreamingResponse
from rembg import remove
from PIL import Image
import io

app = FastAPI(title="rembg Background Removal API")

@app.get("/")
async def root():
    return {"message": "rembg Background Removal API", "version": "1.0"}

@app.post("/remove-bg")
async def remove_background(file: UploadFile = File(...)):
    """
    이미지 배경 제거 엔드포인트

    - 요청: multipart/form-data, 필드명: file
    - 응답: image/png (배경이 투명한 PNG)
    """
    try:
        # 업로드된 파일 읽기
        input_image = await file.read()

        # rembg로 배경 제거
        output_image = remove(input_image)

        # PNG로 변환하여 반환
        return StreamingResponse(
            io.BytesIO(output_image),
            media_type="image/png",
            headers={
                "Content-Disposition": f"attachment; filename=removed_bg.png"
            }
        )

    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"배경 제거 실패: {str(e)}"
        )

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)
```

서버 실행:
```bash
python rembg_server.py
```

또는:
```bash
uvicorn rembg_server:app --host 0.0.0.0 --port 8001 --reload
```

### 3. Spring Boot 서버 실행

```bash
./gradlew bootRun
```

## API 사용법

### 1. 이미지 업로드 API

**엔드포인트**: `POST /api/v1/closet/upload`

**요청**:
- Content-Type: `multipart/form-data`
- 인증: JWT 토큰 필요 (Authorization: Bearer {token})

**파라미터**:
- `image`: 이미지 파일 (최대 10MB)
- `name`: 옷 이름
- `category`: 카테고리 (TOP, BOTTOM, SHOES, ACC)

**응답 예시**:
```json
{
  "id": 123,
  "name": "나이키 운동화",
  "category": "SHOES",
  "imageUrl": "/uploads/removed-bg/123.png",
  "originalImageUrl": "/uploads/original/123.jpg",
  "removedBgImageUrl": "/uploads/removed-bg/123.png"
}
```

### 2. cURL 예제

```bash
curl -X POST http://localhost:8080/api/v1/closet/upload \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "image=@/path/to/image.jpg" \
  -F "name=나이키 운동화" \
  -F "category=SHOES"
```

### 3. Postman 사용

1. Method: POST
2. URL: `http://localhost:8080/api/v1/closet/upload`
3. Headers:
   - `Authorization: Bearer YOUR_JWT_TOKEN`
4. Body:
   - Type: form-data
   - Key: `image`, Type: File, Value: (파일 선택)
   - Key: `name`, Type: Text, Value: "나이키 운동화"
   - Key: `category`, Type: Text, Value: "SHOES"

## 저장 구조

```
uploads/
├── original/
│   ├── 123.jpg      # 원본 이미지
│   ├── 124.png
│   └── ...
└── removed-bg/
    ├── 123.png      # 배경 제거 이미지
    ├── 124.png
    └── ...
```

## 데이터베이스 스키마

```sql
ALTER TABLE cloth ADD COLUMN original_image_url VARCHAR(512);
ALTER TABLE cloth ADD COLUMN removed_bg_image_url VARCHAR(512);
```

## 에러 처리

### 1. rembg 서버 연결 실패
- **HTTP 502 Bad Gateway**
- 원인: Python rembg 서버가 실행되지 않음
- 해결: `python rembg_server.py` 실행

### 2. 파일 크기 초과
- **HTTP 400 Bad Request**
- 원인: 10MB 초과 파일 업로드
- 해결: 이미지 크기 줄이기

### 3. 잘못된 파일 형식
- **HTTP 400 Bad Request**
- 원인: 이미지 파일이 아님
- 해결: JPG, PNG 등 이미지 파일 업로드

## 설정 변경

### application.properties

```properties
# rembg 서버 URL 변경
rembg.server.url=http://localhost:8001

# 타임아웃 변경 (기본: 10초)
rembg.timeout.seconds=20

# 업로드 디렉토리 변경
upload.dir=./uploads

# 파일 크기 제한 변경
spring.servlet.multipart.max-file-size=20MB
spring.servlet.multipart.max-request-size=25MB
```

## 성능 최적화

### 1. rembg 서버 성능 향상
- GPU 사용 (CUDA 설치)
- 모델 크기 조정 (`u2net`, `u2netp` 등)

### 2. 비동기 처리 (선택)
현재는 동기 처리이지만, 필요시 Spring의 `@Async`를 사용하여 비동기로 변경 가능합니다.

## 테스트

### 1. rembg 서버 테스트
```bash
curl -X POST http://localhost:8001/remove-bg \
  -F "file=@test_image.jpg" \
  -o output.png
```

### 2. 통합 테스트
Spring Boot 애플리케이션에서 실제 이미지 업로드 테스트를 수행합니다.

## 트러블슈팅

### Q: rembg 서버가 느림
A: GPU를 사용하거나 더 작은 모델(`u2netp`)을 사용하세요.

### Q: 파일이 저장되지 않음
A: `./uploads` 디렉토리에 쓰기 권한이 있는지 확인하세요.

### Q: JWT 토큰이 없어서 401 에러
A: `/api/v1/auth/login`으로 로그인하여 토큰을 먼저 받으세요.

## 참고 자료

- [rembg 공식 문서](https://github.com/danielgatis/rembg)
- [FastAPI 공식 문서](https://fastapi.tiangolo.com/)
- [Spring WebClient 문서](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-client)
