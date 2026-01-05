#!/usr/bin/env python3
"""
Google AI Studio API 키 및 모델 테스트 스크립트
"""

import os
import sys
from pathlib import Path

# Add parent directory to path for imports
sys.path.insert(0, str(Path(__file__).parent))

try:
    import google.generativeai as genai
    GENAI_AVAILABLE = True
except ImportError:
    GENAI_AVAILABLE = False
    print("❌ google-generativeai 라이브러리가 설치되지 않았습니다.")
    print("   설치: pip install google-generativeai")
    sys.exit(1)


def test_api_key_and_quota():
    """API 키 유효성 및 사용 가능한 모델 테스트"""

    # .env 파일에서 API 키 로드 시도
    env_path = Path(__file__).parent.parent / ".env"
    if env_path.exists():
        print(f"✅ .env 파일 발견: {env_path}")
        with open(env_path) as f:
            for line in f:
                if line.startswith("GOOGLE_API_KEY="):
                    api_key = line.split("=", 1)[1].strip()
                    os.environ["GOOGLE_API_KEY"] = api_key
                    break

    api_key = os.getenv("GOOGLE_API_KEY")

    if not api_key:
        print("❌ GOOGLE_API_KEY 환경변수가 설정되지 않았습니다.")
        print("   .env 파일에 GOOGLE_API_KEY를 설정하거나")
        print("   export GOOGLE_API_KEY='your-key-here'를 실행하세요.")
        return False

    # API 키 일부만 표시 (보안)
    masked_key = api_key[:10] + "..." + api_key[-4:] if len(api_key) > 14 else "***"
    print(f"✅ API 키 발견: {masked_key}")

    try:
        # Google AI 설정
        genai.configure(api_key=api_key)
        print("✅ Google AI Studio 설정 완료")

        # 사용 가능한 모델 목록 조회
        print("\n📋 사용 가능한 모델 목록:")
        print("-" * 80)

        models = genai.list_models()
        model_count = 0
        image_gen_models = []

        for model in models:
            model_count += 1
            print(f"\n{model_count}. {model.name}")
            print(f"   Display Name: {model.display_name}")
            print(f"   Description: {model.description[:100]}..." if len(model.description) > 100 else f"   Description: {model.description}")

            # 지원 메서드 확인
            if hasattr(model, 'supported_generation_methods'):
                methods = model.supported_generation_methods
                print(f"   Supported Methods: {methods}")

                # 이미지 생성 관련 메서드 확인
                if any(m in methods for m in ['generateContent', 'generateImage']):
                    image_gen_models.append(model.name)

        print("\n" + "=" * 80)
        print(f"총 {model_count}개의 모델 발견")

        if image_gen_models:
            print(f"\n🎨 이미지 생성/편집 가능 모델 ({len(image_gen_models)}개):")
            for m in image_gen_models:
                print(f"   - {m}")

        # 현재 imagen_service.py에서 사용 중인 모델 확인
        target_model = "models/gemini-3-pro-image-preview"
        print(f"\n🔍 imagen_service.py에서 사용 중인 모델: {target_model}")

        model_exists = any(m.name == target_model for m in models)
        if model_exists:
            print(f"   ✅ 모델이 존재하고 사용 가능합니다.")
        else:
            print(f"   ❌ 경고: 해당 모델을 찾을 수 없습니다!")
            print(f"   사용 가능한 이미지 모델로 교체가 필요할 수 있습니다.")

        # 간단한 텍스트 생성 테스트 (quota 확인용)
        print(f"\n🧪 API 호출 테스트 (quota 확인)...")
        try:
            # Gemini 2.5 Flash로 테스트 (가장 안정적)
            test_model = genai.GenerativeModel('gemini-pro')
            response = test_model.generate_content("Hello, respond with just 'Hi'")
            print(f"   ✅ API 호출 성공!")
            print(f"   응답: {response.text[:100]}")
        except Exception as e:
            print(f"   ❌ API 호출 실패: {e}")
            if "429" in str(e) or "quota" in str(e).lower():
                print(f"   ⚠️  Quota 초과 또는 Rate limit 감지!")
            return False

        print("\n" + "=" * 80)
        print("✅ 모든 테스트 통과!")
        return True

    except Exception as e:
        print(f"\n❌ 테스트 실패: {e}")
        import traceback
        traceback.print_exc()
        return False


if __name__ == "__main__":
    print("=" * 80)
    print("Google AI Studio API 키 및 모델 테스트")
    print("=" * 80)
    print()

    success = test_api_key_and_quota()

    if success:
        print("\n✅ 테스트 완료: Google AI Studio API를 사용할 준비가 되었습니다.")
        sys.exit(0)
    else:
        print("\n❌ 테스트 실패: 위의 오류를 확인하고 수정하세요.")
        sys.exit(1)
