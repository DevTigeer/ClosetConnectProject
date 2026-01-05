"""
Outfit Try-On REST API
의류 아이템들을 조합하여 가상 착용 이미지 생성하는 Flask API
"""

import os
import io
import base64
from flask import Flask, request, jsonify
from flask_cors import CORS
from PIL import Image
from dotenv import load_dotenv

# 환경변수 로드
load_dotenv()

# Try-On 서비스 import
from ..services.tryon import GeminiTryonService

# Flask 앱 초기화
app = Flask(__name__)
CORS(app)

# Try-On 서비스 초기화
tryon_service = None

try:
    tryon_service = GeminiTryonService()
    if tryon_service.is_available():
        print(f"✅ {tryon_service.get_service_name()} Try-On 서비스 준비 완료")
    else:
        print(f"⚠️  Try-On 서비스 초기화 실패")
except Exception as e:
    print(f"❌ Try-On 서비스 초기화 오류: {e}")


def decode_base64_image(base64_str: str) -> Image.Image:
    """
    Base64 문자열을 PIL Image로 디코딩

    Args:
        base64_str: Base64 인코딩된 이미지 문자열

    Returns:
        PIL Image
    """
    # data:image/png;base64, 접두사 제거
    if ',' in base64_str:
        base64_str = base64_str.split(',', 1)[1]

    image_data = base64.b64decode(base64_str)
    return Image.open(io.BytesIO(image_data))


def encode_image_to_base64(image_pil: Image.Image) -> str:
    """
    PIL Image를 Base64 문자열로 인코딩

    Args:
        image_pil: PIL Image

    Returns:
        Base64 인코딩된 이미지 문자열
    """
    buffer = io.BytesIO()
    image_pil.save(buffer, format="PNG")
    buffer.seek(0)
    return base64.b64encode(buffer.read()).decode('utf-8')


@app.route('/health', methods=['GET'])
def health_check():
    """헬스 체크 엔드포인트"""
    return jsonify({
        'status': 'ok',
        'service': 'Outfit Try-On API',
        'tryon_available': tryon_service is not None and tryon_service.is_available(),
        'engine': tryon_service.get_service_name() if tryon_service else None
    })


@app.route('/tryon', methods=['POST'])
def generate_tryon():
    """
    Outfit Try-On 생성 엔드포인트

    Request Body (JSON):
    {
        "upperClothes": "base64 encoded image (optional)",
        "lowerClothes": "base64 encoded image (optional)",
        "shoes": "base64 encoded image (optional)",
        "accessories": ["base64 encoded image", ...] (optional),
        "modelImage": "base64 encoded image (optional)",
        "prompt": "custom prompt (optional)"
    }

    Response (JSON):
    {
        "success": true,
        "image": "base64 encoded result image",
        "engine": "Gemini"
    }
    """
    if not tryon_service or not tryon_service.is_available():
        return jsonify({
            'success': False,
            'error': 'Try-On 서비스를 사용할 수 없습니다.'
        }), 503

    try:
        data = request.get_json()

        # 요청에서 이미지 추출
        upper_clothes = None
        lower_clothes = None
        shoes = None
        accessories = []
        model_image = None
        prompt = data.get('prompt')

        # 상의
        if 'upperClothes' in data and data['upperClothes']:
            upper_clothes = decode_base64_image(data['upperClothes'])
            print(f"  상의 이미지 수신: {upper_clothes.size}")

        # 하의
        if 'lowerClothes' in data and data['lowerClothes']:
            lower_clothes = decode_base64_image(data['lowerClothes'])
            print(f"  하의 이미지 수신: {lower_clothes.size}")

        # 신발
        if 'shoes' in data and data['shoes']:
            shoes = decode_base64_image(data['shoes'])
            print(f"  신발 이미지 수신: {shoes.size}")

        # 악세서리
        if 'accessories' in data and data['accessories']:
            for acc_base64 in data['accessories']:
                if acc_base64:
                    acc_image = decode_base64_image(acc_base64)
                    accessories.append(acc_image)
            print(f"  악세서리 {len(accessories)}개 수신")

        # 모델 이미지
        if 'modelImage' in data and data['modelImage']:
            model_image = decode_base64_image(data['modelImage'])
            print(f"  모델 이미지 수신: {model_image.size}")

        # 최소 1개 이상의 의류 아이템 필요
        if not any([upper_clothes, lower_clothes, shoes, accessories]):
            return jsonify({
                'success': False,
                'error': '최소 1개 이상의 의류 아이템이 필요합니다.'
            }), 400

        # Try-On 생성
        result_image = tryon_service.generate_outfit_tryon(
            upper_clothes=upper_clothes,
            lower_clothes=lower_clothes,
            shoes=shoes,
            accessories=accessories if accessories else None,
            model_image=model_image,
            prompt=prompt
        )

        # 결과를 Base64로 인코딩
        result_base64 = encode_image_to_base64(result_image)

        return jsonify({
            'success': True,
            'image': f'data:image/png;base64,{result_base64}',
            'engine': tryon_service.get_service_name()
        })

    except Exception as e:
        import traceback
        traceback.print_exc()
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500


@app.route('/tryon/url', methods=['POST'])
def generate_tryon_from_urls():
    """
    URL에서 이미지를 가져와 Try-On 생성

    Request Body (JSON):
    {
        "upperClothesUrl": "image URL (optional)",
        "lowerClothesUrl": "image URL (optional)",
        "shoesUrl": "image URL (optional)",
        "accessoriesUrls": ["image URL", ...] (optional),
        "modelImageUrl": "image URL (optional)",
        "prompt": "custom prompt (optional)"
    }
    """
    if not tryon_service or not tryon_service.is_available():
        return jsonify({
            'success': False,
            'error': 'Try-On 서비스를 사용할 수 없습니다.'
        }), 503

    try:
        import requests
        from io import BytesIO

        data = request.get_json()

        def load_image_from_url(url):
            """URL에서 이미지 로드"""
            response = requests.get(url, timeout=10)
            response.raise_for_status()
            return Image.open(BytesIO(response.content))

        # 요청에서 이미지 URL 추출 및 로드
        upper_clothes = None
        lower_clothes = None
        shoes = None
        accessories = []
        model_image = None
        prompt = data.get('prompt')

        if 'upperClothesUrl' in data and data['upperClothesUrl']:
            upper_clothes = load_image_from_url(data['upperClothesUrl'])
            print(f"  상의 이미지 로드: {upper_clothes.size}")

        if 'lowerClothesUrl' in data and data['lowerClothesUrl']:
            lower_clothes = load_image_from_url(data['lowerClothesUrl'])
            print(f"  하의 이미지 로드: {lower_clothes.size}")

        if 'shoesUrl' in data and data['shoesUrl']:
            shoes = load_image_from_url(data['shoesUrl'])
            print(f"  신발 이미지 로드: {shoes.size}")

        if 'accessoriesUrls' in data and data['accessoriesUrls']:
            for acc_url in data['accessoriesUrls']:
                if acc_url:
                    acc_image = load_image_from_url(acc_url)
                    accessories.append(acc_image)
            print(f"  악세서리 {len(accessories)}개 로드")

        if 'modelImageUrl' in data and data['modelImageUrl']:
            model_image = load_image_from_url(data['modelImageUrl'])
            print(f"  모델 이미지 로드: {model_image.size}")

        # 최소 1개 이상의 의류 아이템 필요
        if not any([upper_clothes, lower_clothes, shoes, accessories]):
            return jsonify({
                'success': False,
                'error': '최소 1개 이상의 의류 아이템이 필요합니다.'
            }), 400

        # Try-On 생성
        result_image = tryon_service.generate_outfit_tryon(
            upper_clothes=upper_clothes,
            lower_clothes=lower_clothes,
            shoes=shoes,
            accessories=accessories if accessories else None,
            model_image=model_image,
            prompt=prompt
        )

        # 결과를 Base64로 인코딩
        result_base64 = encode_image_to_base64(result_image)

        return jsonify({
            'success': True,
            'image': f'data:image/png;base64,{result_base64}',
            'engine': tryon_service.get_service_name()
        })

    except Exception as e:
        import traceback
        traceback.print_exc()
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500


if __name__ == '__main__':
    port = int(os.getenv('TRYON_API_PORT', '5001'))
    print(f"\n🚀 Outfit Try-On API 서버 시작")
    print(f"   포트: {port}")
    print(f"   엔진: {tryon_service.get_service_name() if tryon_service else 'None'}")
    print(f"\n사용 가능한 엔드포인트:")
    print(f"   GET  /health - 헬스 체크")
    print(f"   POST /tryon - Try-On 생성 (Base64)")
    print(f"   POST /tryon/url - Try-On 생성 (URL)")
    print()

    # debug=False로 설정하여 단일 프로세스로 실행
    # 개발 시 디버깅이 필요하면 debug=True로 변경
    app.run(host='0.0.0.0', port=port, debug=False)
