"""
Google AI Studio - Gemini 2.5 Flash Image (Nano Banana) 이미지 확장 서비스
잘린 이미지의 경계를 자연스럽게 확장하여 완전한 옷 이미지로 복원
"""

import os
import io
import time
from PIL import Image, ImageDraw

try:
    import google.generativeai as genai
    GENAI_AVAILABLE = True
except ImportError:
    GENAI_AVAILABLE = False
    print("⚠️  Google Generative AI 라이브러리가 설치되지 않았습니다.")
    print("   설치: pip install google-generativeai")


class GoogleAIImagenService:
    """
    Google AI Studio - Gemini 2.5 Flash Image를 사용한 이미지 확장 서비스

    기능:
    - 잘린 이미지의 경계를 자연스럽게 확장
    - 프롬프트 기반 이미지 편집
    - 투명 배경 유지 시도
    - Nano Banana (gemini-2.5-flash-image) 사용
    """

    def __init__(self, api_key=None):
        """
        Google AI Studio 서비스 초기화

        Args:
            api_key: Google AI Studio API 키 (None이면 환경변수에서 로드)
        """
        if not GENAI_AVAILABLE:
            raise ImportError(
                "Google Generative AI 라이브러리가 필요합니다.\n"
                "설치: pip install google-generativeai"
            )

        self.api_key = api_key or os.getenv("GOOGLE_API_KEY")

        if not self.api_key:
            raise ValueError(
                "Google AI Studio API 키가 필요합니다.\n"
                "환경변수 GOOGLE_API_KEY를 설정하거나 api_key 파라미터를 전달하세요.\n"
                "API 키 발급: https://aistudio.google.com/apikey"
            )

        # Google Generative AI 설정
        # gRPC 사용 (고속 연결)
        genai.configure(api_key=self.api_key)

        # Gemini 3 Pro Image 모델 초기화 (이미지 생성/편집 지원)
        self.model = genai.GenerativeModel('gemini-3-pro-image-preview')

        print(f"✅ Google AI Studio (Gemini 3 Pro Image) 서비스 초기화 완료")
        print(f"   모델: gemini-3-pro-image-preview")

    def expand_image(self,
                     image_pil,
                     expand_pixels=50,
                     prompt=None,
                     retry_on_rate_limit=True,
                     api_delay=None):
        """
        이미지를 자연스럽게 확장

        Args:
            image_pil: PIL Image (RGBA 또는 RGB)
            expand_pixels: 각 방향으로 확장할 픽셀 수
            prompt: 사용자 정의 프롬프트 (None이면 기본 프롬프트 사용)
            retry_on_rate_limit: Rate limit 에러 시 자동 재시도 여부
            api_delay: API 호출 전 대기 시간 (초, 무료 티어 Rate limit 방지)

        Returns:
            PIL Image (확장된 이미지)
        """
        if prompt is None:
            prompt = self._get_default_prompt()

        # API 지연 시간 설정 (환경변수 또는 기본값)
        if api_delay is None:
            api_delay = float(os.getenv("IMAGEN_API_DELAY", "2.0"))

        # Rate limit 방지를 위한 지연
        if api_delay > 0:
            print(f"     Rate limit 방지 대기: {api_delay}초...")
            time.sleep(api_delay)

        try:
            print(f"  🎨 Gemini 2.5 Flash Image (Nano Banana)로 이미지 확장 중... ({expand_pixels}px)")

            # 1. 확장된 캔버스 생성 (원본을 중앙에 배치하고 주변은 흰색)
            canvas = self._create_expanded_canvas(image_pil, expand_pixels)
            print(f"     캔버스 생성 완료: {canvas.size}")

            # 2. PIL Image를 임시 파일로 저장
            canvas_path = "/tmp/imagen_canvas.png"
            canvas.save(canvas_path, "PNG")
            print(f"     캔버스 저장 완료: {canvas_path}")

            # 3. 이미지 업로드
            print(f"     이미지 업로드 중...")
            uploaded_image = genai.upload_file(canvas_path)
            print(f"     이미지 업로드 완료: {uploaded_image.name}")

            # 4. Gemini 2.5 Flash Image로 이미지 편집
            # 확장된 영역을 자연스럽게 채우도록 요청
            full_prompt = (
                f"{prompt}\n\n"
                f"The image has white borders around it. "
                f"Fill in the white areas by naturally extending the clothing item. "
                f"Keep the center clothing item exactly as is, only extend the edges seamlessly."
            )
            print(f"     프롬프트: {full_prompt[:100]}...")

            print(f"     Gemini API 호출 중...")
            response = self.model.generate_content([
                full_prompt,
                uploaded_image
            ])
            print(f"     Gemini API 응답 받음")

            # 디버깅: 응답 구조 출력
            print(f"     응답 타입: {type(response)}")
            print(f"     응답 parts 존재: {hasattr(response, 'parts')}")
            if hasattr(response, 'parts'):
                print(f"     응답 parts 개수: {len(response.parts) if response.parts else 0}")
                if response.parts:
                    for idx, part in enumerate(response.parts):
                        print(f"       Part {idx}: {type(part)}, has inline_data: {hasattr(part, 'inline_data')}")
                        if hasattr(part, 'text'):
                            print(f"       Part {idx} text: {part.text[:200]}")

            # 5. 결과 이미지 추출
            if response.parts:
                for part in response.parts:
                    if hasattr(part, 'inline_data') and part.inline_data:
                        # 이미지 데이터를 PIL Image로 변환
                        image_data = part.inline_data.data
                        result = Image.open(io.BytesIO(image_data))

                        # RGBA로 변환 (투명 배경 유지 시도)
                        if result.mode != 'RGBA':
                            result = result.convert('RGBA')

                        print(f"  ✅ Nano Banana 이미지 확장 완료")

                        # 임시 파일 삭제
                        try:
                            os.remove(canvas_path)
                            genai.delete_file(uploaded_image.name)
                        except:
                            pass

                        return result

            # 응답에 이미지가 없으면 원본 반환
            print(f"  ⚠️  응답에 이미지가 없습니다. 원본을 반환합니다.")
            print(f"     응답 전체: {response}")
            return image_pil

        except Exception as e:
            import traceback
            error_str = str(e)
            error_type = type(e).__name__

            print(f"  ❌ Nano Banana 이미지 확장 실패: {e}")
            print(f"     에러 타입: {error_type}")

            # Rate limit 에러 확인
            if "429" in error_str or "ResourceExhausted" in error_type:
                print(f"     ⚠️  Rate limit 감지!")

                if retry_on_rate_limit:
                    # retry_delay 추출 시도
                    import re
                    retry_match = re.search(r'retry in ([\d.]+)s', error_str)
                    if retry_match:
                        retry_seconds = float(retry_match.group(1))
                        print(f"     ⏳ {retry_seconds}초 대기 후 재시도...")
                        time.sleep(retry_seconds + 1)  # 1초 여유

                        # 재시도 (재귀 호출, retry_on_rate_limit=False로 무한 루프 방지)
                        print(f"     🔄 재시도 중...")
                        return self.expand_image(
                            image_pil,
                            expand_pixels,
                            prompt,
                            retry_on_rate_limit=False,  # 재시도는 1번만
                            api_delay=0  # 이미 대기했으므로 추가 지연 불필요
                        )

            print(f"     에러 상세:")
            traceback.print_exc()
            print(f"     원본 이미지를 반환합니다.")
            return image_pil

    def should_expand(self, image_pil, min_size=300):
        """
        이미지가 확장이 필요한지 판단

        Args:
            image_pil: PIL Image
            min_size: 최소 크기 기준 (이보다 작으면 확장 권장)

        Returns:
            bool: True면 확장 권장
        """
        width, height = image_pil.size

        # 이미지가 너무 작으면 확장 권장
        if width < min_size or height < min_size:
            return True

        return False

    def _create_expanded_canvas(self, image_pil, expand_pixels):
        """
        확장된 캔버스 생성 (원본을 중앙에 배치)

        Args:
            image_pil: 원본 PIL Image
            expand_pixels: 확장할 픽셀 수

        Returns:
            canvas: 확장된 캔버스 (PIL Image, RGB)
        """
        width, height = image_pil.size
        new_width = width + expand_pixels * 2
        new_height = height + expand_pixels * 2

        # 흰색 배경의 캔버스 생성
        canvas = Image.new('RGB', (new_width, new_height), (255, 255, 255))

        # 원본 이미지를 RGB로 변환 후 중앙에 배치
        if image_pil.mode == 'RGBA':
            # RGBA인 경우 흰색 배경과 합성
            temp_canvas = Image.new('RGB', image_pil.size, (255, 255, 255))
            temp_canvas.paste(image_pil, (0, 0), image_pil)
            canvas.paste(temp_canvas, (expand_pixels, expand_pixels))
        else:
            canvas.paste(image_pil.convert('RGB'), (expand_pixels, expand_pixels))

        return canvas

    def _get_default_prompt(self):
        """
        기본 확장 프롬프트
        환경변수 IMAGEN_EXPAND_PROMPT가 설정되어 있으면 우선 사용
        """
        # 환경변수에서 커스텀 프롬프트 확인
        custom_prompt = os.getenv("IMAGEN_EXPAND_PROMPT", "").strip()
        if custom_prompt:
            return custom_prompt

        # 기본 프롬프트 반환
        return (
            "Naturally extend and complete the clothing item in the image. "
            "Maintain the exact original style, color, pattern, and texture. "
            "Create seamless edges with no visible borders. "
            "Professional product photo quality. "
            "Clean white background."
        )


# 편의 함수
def create_imagen_service(api_key=None):
    """
    Google AI Studio Imagen 서비스 인스턴스 생성

    Args:
        api_key: Google AI Studio API 키 (None이면 환경변수 사용)

    Returns:
        GoogleAIImagenService 인스턴스
    """
    return GoogleAIImagenService(api_key=api_key)
