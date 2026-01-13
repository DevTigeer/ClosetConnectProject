# clothing_restorer.py (미사용 - Stable Diffusion Inpainting) 🎨
# 현재는 Google AI Imagen을 사용하므로 이 파일은 미사용

from diffusers import StableDiffusionInpaintPipeline
from PIL import Image, ImageDraw
import torch
import numpy as np
from pathlib import Path

class ClothingRestorer:
    """Stable Diffusion Inpainting을 사용한 의상 복원"""
    
    def __init__(self, model_id="runwayml/stable-diffusion-inpainting"):
        """
        초기화
        
        Args:
            model_id: Hugging Face 모델 ID
        """
        print("🔄 모델 로딩 중...")
        
        # 디바이스 설정
        if torch.cuda.is_available():
            self.device = "cuda"
            self.dtype = torch.float16
        elif torch.backends.mps.is_available():
            self.device = "mps"
            self.dtype = torch.float32  # MPS는 float32 사용
        else:
            self.device = "cpu"
            self.dtype = torch.float32
        
        print(f"📱 Device: {self.device}")
        
        # 파이프라인 로드
        self.pipe = StableDiffusionInpaintPipeline.from_pretrained(
            model_id,
            torch_dtype=self.dtype,
            safety_checker=None  # 안전성 체커 비활성화 (속도 향상)
        )
        self.pipe = self.pipe.to(self.device)
        
        # 메모리 최적화 (선택)
        if self.device == "cuda":
            self.pipe.enable_attention_slicing()
        
        print("✅ 모델 로드 완료!")
    
    def prepare_image_and_mask(self, image_path, extend_ratio=0.5):
        """
        이미지와 마스크 준비
        
        Args:
            image_path: 입력 이미지 경로
            extend_ratio: 확장 비율 (0.5 = 50% 확장)
        
        Returns:
            extended_image: 확장된 이미지
            mask: 채워야 할 영역 마스크
        """
        print(f"📂 이미지 로드: {image_path}")
        
        # 1. 원본 이미지 로드
        original = Image.open(image_path).convert("RGB")
        orig_width, orig_height = original.size
        
        print(f"📐 원본 크기: {orig_width}x{orig_height}")
        
        # 2. 확장할 크기 계산
        extend_pixels_height = int(orig_height * extend_ratio)
        extend_pixels_width = int(orig_width * 0.2)  # 좌우는 20%만
        
        new_width = orig_width + (extend_pixels_width * 2)
        new_height = orig_height + (extend_pixels_height * 2)
        
        # 512의 배수로 조정 (Stable Diffusion 최적화)
        new_width = ((new_width + 63) // 64) * 64
        new_height = ((new_height + 63) // 64) * 64
        
        print(f"📐 확장 크기: {new_width}x{new_height}")
        
        # 3. 흰색 캔버스 생성
        extended_image = Image.new("RGB", (new_width, new_height), (255, 255, 255))
        
        # 4. 원본 이미지 중앙 배치
        paste_x = (new_width - orig_width) // 2
        paste_y = (new_height - orig_height) // 2
        extended_image.paste(original, (paste_x, paste_y))
        
        # 5. 마스크 생성 (채워야 할 영역 = 흰색)
        mask = Image.new("L", (new_width, new_height), 255)  # 전체 흰색
        mask_draw = ImageDraw.Draw(mask)
        
        # 원본 이미지 영역은 검은색 (변경하지 않음)
        mask_draw.rectangle(
            [paste_x, paste_y, paste_x + orig_width, paste_y + orig_height],
            fill=0
        )
        
        return extended_image, mask
    
    def detect_clothing_type(self, image_path):
        """
        의상 타입 자동 감지
        
        Returns:
            str: 'top', 'bottom', 'dress'
        """
        image = Image.open(image_path)
        width, height = image.size
        ratio = height / width
        
        if ratio > 1.5:
            return "bottom"  # 바지/치마 (세로로 김)
        elif ratio < 0.8:
            return "top"     # 상의 (가로로 넓음)
        else:
            return "dress"   # 원피스
    
    def get_prompt_for_clothing(self, clothing_type, filename=""):
        """
        의상 타입에 맞는 프롬프트 생성
        
        Args:
            clothing_type: 의상 타입
            filename: 파일명 (추가 힌트)
        
        Returns:
            tuple: (prompt, negative_prompt)
        """
        # 파일명에서 힌트 추출
        filename_lower = filename.lower()
        
        base_prompt = "professional product photography, high quality, detailed, "
        negative_prompt = "blurry, low quality, distorted, deformed, "
        
        if "skirt" in filename_lower or clothing_type == "bottom":
            prompt = base_prompt + "complete full-length skirt, flowing fabric, elegant design, white background"
            negative = negative_prompt + "incomplete, cropped, person, body parts"
        
        elif "pants" in filename_lower or "바지" in filename:
            prompt = base_prompt + "complete full-length pants, trousers, smooth fabric, white background"
            negative = negative_prompt + "incomplete, cropped, person, legs"
        
        elif "dress" in filename_lower or "원피스" in filename:
            prompt = base_prompt + "complete full-length dress, flowing fabric, elegant, white background"
            negative = negative_prompt + "incomplete, cropped, person, body"
        
        elif "상의" in filename or clothing_type == "top":
            prompt = base_prompt + "complete shirt, blouse, top garment, smooth fabric, white background"
            negative = negative_prompt + "incomplete, cropped, person, arms"
        
        else:
            prompt = base_prompt + "complete clothing item, professional product photo, white background"
            negative = negative_prompt + "incomplete, cropped, person, body parts"
        
        return prompt, negative
    
    def restore(
        self,
        image_path,
        output_path=None,
        extend_ratio=0.5,
        num_inference_steps=50,
        guidance_scale=7.5,
        strength=0.8
    ):
        """
        이미지 복원 수행
        
        Args:
            image_path: 입력 이미지 경로
            output_path: 출력 경로 (None이면 자동 생성)
            extend_ratio: 확장 비율
            num_inference_steps: 추론 스텝 수 (높을수록 품질↑, 시간↑)
            guidance_scale: 프롬프트 가이드 강도 (7-15 권장)
            strength: 변경 강도 (0.0-1.0, 높을수록 많이 변경)
        
        Returns:
            str: 저장된 파일 경로
        """
        # 1. 이미지와 마스크 준비
        extended_image, mask = self.prepare_image_and_mask(image_path, extend_ratio)
        
        # 2. 의상 타입 감지
        clothing_type = self.detect_clothing_type(image_path)
        print(f"👕 감지된 의상 타입: {clothing_type}")
        
        # 3. 프롬프트 생성
        prompt, negative_prompt = self.get_prompt_for_clothing(
            clothing_type,
            Path(image_path).name
        )
        
        print(f"📝 Prompt: {prompt}")
        print(f"🚫 Negative: {negative_prompt}")
        
        # 4. Inpainting 수행
        print(f"🎨 복원 중... (Steps: {num_inference_steps})")
        
        result = self.pipe(
            prompt=prompt,
            negative_prompt=negative_prompt,
            image=extended_image,
            mask_image=mask,
            num_inference_steps=num_inference_steps,
            guidance_scale=guidance_scale,
            strength=strength,
        ).images[0]
        
        # 5. 저장
        if output_path is None:
            input_path = Path(image_path)
            output_path = input_path.parent / f"{input_path.stem}_restored.png"
        
        result.save(output_path)
        print(f"✅ 저장 완료: {output_path}")
        
        return str(output_path)
    
    def batch_restore(self, input_dir, output_dir=None, **kwargs):
        """
        폴더 내 모든 이미지 배치 복원
        
        Args:
            input_dir: 입력 폴더
            output_dir: 출력 폴더 (None이면 입력 폴더에 저장)
            **kwargs: restore() 함수의 추가 인자
        """
        input_path = Path(input_dir)
        
        if output_dir is None:
            output_path = input_path / "restored"
        else:
            output_path = Path(output_dir)
        
        output_path.mkdir(parents=True, exist_ok=True)
        
        # 이미지 파일 찾기
        image_files = list(input_path.glob("*.png")) + \
                     list(input_path.glob("*.jpg")) + \
                     list(input_path.glob("*.jpeg"))
        
        print(f"📁 발견된 이미지: {len(image_files)}개")
        
        for i, image_file in enumerate(image_files, 1):
            print(f"\n[{i}/{len(image_files)}] 처리 중: {image_file.name}")
            
            output_file = output_path / f"{image_file.stem}_restored.png"
            
            try:
                self.restore(
                    str(image_file),
                    str(output_file),
                    **kwargs
                )
            except Exception as e:
                print(f"❌ 에러: {e}")
                continue
        
        print(f"\n🎉 완료! 결과 폴더: {output_path}")


# ============================================
# 메인 실행 함수
# ============================================

def main():
    """메인 실행 함수"""
    
    print("=" * 50)
    print("🎨 Clothing Restorer")
    print("=" * 50)
    
    # 1. Restorer 초기화
    restorer = ClothingRestorer()
    
    # 2. 단일 이미지 복원
    print("\n단일 이미지 복원 시작...")
    restorer.restore(
        image_path="/Users/grail/Documents/ClosetConnectProject/aiModel/하의_skirt_20251223_154654.png",
        output_path="restored_skirt.png",
        extend_ratio=0.5,          # 50% 확장
        num_inference_steps=50,    # 스텝 수 (높을수록 품질↑)
        guidance_scale=7.5,        # 프롬프트 가이드 강도
        strength=0.8               # 변경 강도
    )
    
    print("\n✅ 모든 작업 완료!")


if __name__ == "__main__":
    main()