"""
Gemini를 사용한 Virtual Try-On 서비스 구현
Google Gemini Vision 모델을 활용하여 의류 조합 이미지 생성
"""

import os
import io
import time
from typing import Dict, List, Optional
from PIL import Image

try:
    import google.generativeai as genai
    GENAI_AVAILABLE = True
except ImportError:
    GENAI_AVAILABLE = False
    print("⚠️  Google Generative AI 라이브러리가 설치되지 않았습니다.")

from .base_tryon_service import BaseTryonService


class GeminiTryonService(BaseTryonService):
    """
    Google Gemini를 사용한 Virtual Try-On 서비스

    특징:
    - Gemini Vision 모델을 사용하여 여러 의류 아이템 조합
    - 프롬프트 기반 이미지 생성
    - 상의, 하의, 신발, 악세서리 조합 지원
    """

    def __init__(self, api_key: Optional[str] = None, model_name: str = "gemini-2.0-flash-exp"):
        """
        Gemini Try-On 서비스 초기화

        Args:
            api_key: Google AI Studio API 키 (None이면 환경변수에서 로드)
            model_name: 사용할 Gemini 모델 이름
        """
        self.available = False
        self.model = None
        self.imagen_model = None  # Imagen 모델
        self.model_name = model_name

        if not GENAI_AVAILABLE:
            print("  ⚠️  Google Generative AI 라이브러리가 필요합니다.")
            return

        self.api_key = api_key or os.getenv("GOOGLE_API_KEY")

        if not self.api_key:
            print("  ⚠️  GOOGLE_API_KEY 환경변수가 설정되지 않았습니다.")
            return

        try:
            # Google Generative AI 설정
            # transport='rest'를 추가하여 gRPC 대신 REST API 사용 (DNS 문제 회피)
            genai.configure(api_key=self.api_key, transport='rest')

            # Gemini 이미지 편집 모델 초기화 (cloth_processing_worker와 동일)
            # imagen_service.py와 동일한 모델 사용
            self.model = genai.GenerativeModel('gemini-3-pro-image-preview')

            self.available = True
            print(f"✅ GoogleAI Imagen (Nano Banana) Try-On 서비스 초기화 완료")
            print(f"   모델: gemini-3-pro-image-preview")
            print(f"   방식: cloth_processing_worker와 동일한 Imagen 방식")

        except Exception as e:
            print(f"  ❌ Gemini Try-On 서비스 초기화 실패: {e}")

    def generate_outfit_tryon(
        self,
        upper_clothes: Optional[Image.Image] = None,
        lower_clothes: Optional[Image.Image] = None,
        shoes: Optional[Image.Image] = None,
        accessories: Optional[List[Image.Image]] = None,
        model_image: Optional[Image.Image] = None,
        prompt: Optional[str] = None
    ) -> Image.Image:
        """
        선택된 의류 아이템들을 조합하여 가상 착용 이미지 생성

        Args:
            upper_clothes: 상의 이미지
            lower_clothes: 하의 이미지
            shoes: 신발 이미지
            accessories: 악세서리 이미지 리스트
            model_image: 모델 이미지 (선택사항)
            prompt: 사용자 정의 프롬프트

        Returns:
            PIL Image: 조합된 try-on 결과 이미지
        """
        if not self.is_available():
            raise RuntimeError("Gemini Try-On 서비스를 사용할 수 없습니다.")

        # 동적 프롬프트 생성 (선택된 아이템에 맞게)
        if prompt is None:
            prompt = self._generate_dynamic_prompt(
                upper_clothes, lower_clothes, shoes, accessories, model_image
            )

        # API 호출 전 지연 (Rate limit 방지)
        api_delay = float(os.getenv("IMAGEN_API_DELAY", "2.0"))
        if api_delay > 0:
            time.sleep(api_delay)

        try:
            print(f"  🎨 Gemini로 outfit try-on 생성 중...")

            # 이미지 파일들을 임시로 저장하고 업로드
            uploaded_files = []
            content_parts = [prompt]

            # 각 아이템 업로드
            if upper_clothes:
                file_info = self._upload_image(upper_clothes, "upper_clothes")
                uploaded_files.append(file_info)
                content_parts.append(file_info)
                print(f"     상의 업로드 완료")

            if lower_clothes:
                file_info = self._upload_image(lower_clothes, "lower_clothes")
                uploaded_files.append(file_info)
                content_parts.append(file_info)
                print(f"     하의 업로드 완료")

            if shoes:
                file_info = self._upload_image(shoes, "shoes")
                uploaded_files.append(file_info)
                content_parts.append(file_info)
                print(f"     신발 업로드 완료")

            if accessories:
                for idx, accessory in enumerate(accessories):
                    file_info = self._upload_image(accessory, f"accessory_{idx}")
                    uploaded_files.append(file_info)
                    content_parts.append(file_info)
                print(f"     악세서리 {len(accessories)}개 업로드 완료")

            if model_image:
                file_info = self._upload_image(model_image, "model")
                uploaded_files.append(file_info)
                content_parts.append(file_info)
                print(f"     모델 이미지 업로드 완료")

            # Gemini API 호출 (이미지 생성 설정)
            print(f"     GoogleAI Imagen 방식으로 이미지 생성 중... (총 {len(uploaded_files)}개 의류)")

            # 이미지 생성 설정
            generation_config = {
                'temperature': 0.4,  # 창의성 조절
                'top_p': 0.8,
                'top_k': 32,
                'max_output_tokens': 2048,
            }

            response = self.model.generate_content(
                content_parts,
                generation_config=generation_config
            )

            print(f"     API 응답 받음")

            # 디버깅: 응답 내용 출력
            print(f"     응답 타입: {type(response)}")
            try:
                if hasattr(response, 'text'):
                    print(f"     응답 텍스트: {response.text[:300] if len(response.text) > 300 else response.text}")
            except ValueError:
                # 이미지 데이터가 포함된 경우 텍스트 변환 불가 (정상 동작)
                print(f"     ✅ 응답에 이미지 데이터 포함 (텍스트 변환 불가 - 이미지 생성 성공)")
            if hasattr(response, 'parts') and response.parts:
                print(f"     응답 parts: {len(response.parts)}개")
                for idx, part in enumerate(response.parts):
                    if hasattr(part, 'inline_data') and part.inline_data:
                        print(f"       Part {idx}: 이미지 데이터 발견! (크기: {len(part.inline_data.data)} bytes)")
                    elif hasattr(part, 'text'):
                        print(f"       Part {idx}: 텍스트 - {part.text[:100]}")

            # 응답에서 이미지 추출
            result_image = self._extract_image_from_response(response)

            # 임시 파일 정리
            for file_info in uploaded_files:
                try:
                    genai.delete_file(file_info.name)
                except:
                    pass

            if result_image:
                print(f"  ✅ GoogleAI Imagen 방식 try-on 생성 완료")
                return result_image
            else:
                # 이미지 생성 실패 시 기본 이미지 반환
                print(f"  ⚠️  이미지 생성 실패")
                print(f"     원인: Gemini가 이미지 대신 텍스트만 반환했습니다.")
                print(f"     참고: Gemini 2.0은 이미지 생성을 지원하지 않을 수 있습니다.")
                return self._create_placeholder_image()

        except Exception as e:
            print(f"  ❌ Gemini outfit try-on 생성 실패: {e}")
            import traceback
            traceback.print_exc()
            return self._create_placeholder_image()

    def is_available(self) -> bool:
        """서비스 사용 가능 여부"""
        return self.available

    def get_service_name(self) -> str:
        """서비스 이름 반환"""
        return "Gemini"

    def _upload_image(self, image_pil: Image.Image, name_prefix: str):
        """
        PIL Image를 Gemini에 업로드

        Args:
            image_pil: PIL Image
            name_prefix: 파일명 접두사

        Returns:
            업로드된 파일 정보
        """
        # 임시 파일로 저장
        temp_path = f"/tmp/{name_prefix}_{int(time.time())}.png"

        # RGBA를 RGB로 변환 (Gemini는 RGB 선호)
        if image_pil.mode == 'RGBA':
            # 흰색 배경과 합성
            background = Image.new('RGB', image_pil.size, (255, 255, 255))
            background.paste(image_pil, mask=image_pil.split()[3])
            image_pil = background
        elif image_pil.mode != 'RGB':
            image_pil = image_pil.convert('RGB')

        image_pil.save(temp_path, "PNG")

        # Gemini에 업로드
        uploaded_file = genai.upload_file(temp_path)

        # 임시 파일 삭제
        try:
            os.remove(temp_path)
        except:
            pass

        return uploaded_file

    def _extract_image_from_response(self, response) -> Optional[Image.Image]:
        """
        Gemini 응답에서 이미지 추출

        Args:
            response: Gemini API 응답

        Returns:
            PIL Image 또는 None
        """
        if not hasattr(response, 'parts') or not response.parts:
            # 텍스트 응답만 있을 경우
            if hasattr(response, 'text'):
                print(f"     Gemini 응답 (텍스트): {response.text[:200]}")
            return None

        # parts에서 이미지 찾기
        for part in response.parts:
            if hasattr(part, 'inline_data') and part.inline_data:
                try:
                    image_data = part.inline_data.data
                    result = Image.open(io.BytesIO(image_data))

                    # RGBA로 변환
                    if result.mode != 'RGBA':
                        result = result.convert('RGBA')

                    return result
                except Exception as e:
                    print(f"     이미지 추출 실패: {e}")

        return None

    def _generate_dynamic_prompt(
        self,
        upper_clothes: Optional[Image.Image],
        lower_clothes: Optional[Image.Image],
        shoes: Optional[Image.Image],
        accessories: Optional[List[Image.Image]],
        model_image: Optional[Image.Image]
    ) -> str:
        """
        선택된 아이템에 맞게 동적으로 프롬프트 생성

        Args:
            upper_clothes: 상의 이미지
            lower_clothes: 하의 이미지
            shoes: 신발 이미지
            accessories: 액세서리 이미지 리스트
            model_image: 모델 이미지

        Returns:
            동적 생성된 프롬프트 문자열
        """
        # 환경변수에서 커스텀 프롬프트 확인
        custom_prompt = os.getenv("TRYON_PROMPT", "").strip()
        if custom_prompt:
            return custom_prompt

        # 이미지 순서 카운터
        image_index = 1
        wearing_items = []

        # 각 아이템에 대해 순서대로 설명 생성
        if upper_clothes:
            wearing_items.append(f"the upper garment (top/shirt) from image {image_index}")
            image_index += 1

        if lower_clothes:
            wearing_items.append(f"the lower garment (pants/skirt) from image {image_index}")
            image_index += 1

        if shoes:
            wearing_items.append(f"the shoes from image {image_index}")
            image_index += 1

        if accessories and len(accessories) > 0:
            if len(accessories) == 1:
                wearing_items.append(f"the accessory from image {image_index}")
            else:
                acc_indices = [str(image_index + i) for i in range(len(accessories))]
                wearing_items.append(f"the accessories from images {', '.join(acc_indices)}")
            image_index += len(accessories)

        # 아이템 리스트를 문장으로 변환
        if len(wearing_items) == 1:
            wearing_description = wearing_items[0]
        elif len(wearing_items) == 2:
            wearing_description = f"{wearing_items[0]} and {wearing_items[1]}"
        else:
            wearing_description = ", ".join(wearing_items[:-1]) + f", and {wearing_items[-1]}"

        # 최종 프롬프트 생성
        prompt = (
            f"Generate a high-quality fashion lookbook image. "
            f"Create a full-body photo of a professional fashion model wearing {wearing_description}. "
            f"Style: Professional fashion photography, clean white background, full body shot, natural lighting. "
            f"The outfit should look perfectly fitted and styled. "
            f"High resolution, photorealistic, fashion magazine quality."
        )

        print(f"  📝 생성된 프롬프트: {prompt}")
        return prompt

    def _create_placeholder_image(self) -> Image.Image:
        """
        에러 발생 시 반환할 플레이스홀더 이미지 생성

        Returns:
            PIL Image (흰색 배경에 텍스트)
        """
        from PIL import ImageDraw, ImageFont

        width, height = 512, 512
        image = Image.new('RGB', (width, height), (255, 255, 255))
        draw = ImageDraw.Draw(image)

        # 중앙에 텍스트 그리기
        text = "Try-on generation failed"
        bbox = draw.textbbox((0, 0), text)
        text_width = bbox[2] - bbox[0]
        text_height = bbox[3] - bbox[1]

        x = (width - text_width) // 2
        y = (height - text_height) // 2

        draw.text((x, y), text, fill=(128, 128, 128))

        return image


# 편의 함수
def create_gemini_tryon_service(api_key: Optional[str] = None) -> GeminiTryonService:
    """
    Gemini Try-On 서비스 인스턴스 생성

    Args:
        api_key: Google AI Studio API 키 (None이면 환경변수 사용)

    Returns:
        GeminiTryonService 인스턴스
    """
    return GeminiTryonService(api_key=api_key)
