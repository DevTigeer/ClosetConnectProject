"""
Virtual Try-On 서비스 추상 인터페이스
다양한 Try-On 엔진 (Gemini, ComfyUI 등)을 교체 가능하도록 설계
"""

from abc import ABC, abstractmethod
from typing import Dict, List, Optional
from PIL import Image


class BaseTryonService(ABC):
    """
    Virtual Try-On 서비스 추상 기본 클래스

    구현체: GeminiTryonService, ComfyUITryonService 등
    """

    @abstractmethod
    def generate_outfit_tryon(
        self,
        upper_clothes: Optional[Image.Image],
        lower_clothes: Optional[Image.Image],
        shoes: Optional[Image.Image],
        accessories: Optional[List[Image.Image]],
        model_image: Optional[Image.Image] = None,
        prompt: Optional[str] = None
    ) -> Image.Image:
        """
        선택된 의류 아이템들을 조합하여 가상 착용 이미지 생성

        Args:
            upper_clothes: 상의 이미지 (PIL Image, RGBA 또는 RGB)
            lower_clothes: 하의 이미지 (PIL Image, RGBA 또는 RGB)
            shoes: 신발 이미지 (PIL Image, RGBA 또는 RGB)
            accessories: 악세서리 이미지 리스트 (PIL Image, RGBA 또는 RGB)
            model_image: 모델 이미지 (선택사항, 없으면 기본 모델 사용)
            prompt: 사용자 정의 프롬프트 (None이면 기본 프롬프트 사용)

        Returns:
            PIL Image: 조합된 try-on 결과 이미지
        """
        pass

    @abstractmethod
    def is_available(self) -> bool:
        """
        서비스가 사용 가능한지 확인

        Returns:
            bool: True면 서비스 사용 가능
        """
        pass

    @abstractmethod
    def get_service_name(self) -> str:
        """
        서비스 이름 반환

        Returns:
            str: 서비스 이름 (예: "Gemini", "ComfyUI")
        """
        pass
