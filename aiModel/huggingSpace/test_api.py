"""
Test script for Background Removal API

Usage:
    python test_api.py <image_path> [api_url]

Examples:
    # Test local Gradio server
    python test_api.py test_image.png http://localhost:7860

    # Test deployed Hugging Face Space
    python test_api.py test_image.png https://YOUR-USERNAME-background-removal.hf.space
"""

import sys
import requests
import base64
from PIL import Image
import io
import time

def test_api(image_path, api_url="http://localhost:7860"):
    """
    Test the background removal API

    Args:
        image_path: Path to test image
        api_url: API URL (default: localhost)
    """
    print(f"🧪 Testing Background Removal API")
    print(f"   Image: {image_path}")
    print(f"   API URL: {api_url}")
    print()

    try:
        # Read image
        with open(image_path, "rb") as f:
            image_bytes = f.read()

        print(f"📤 Sending request...")
        start_time = time.time()

        # Call API (Gradio 4.x uses /run/predict)
        response = requests.post(
            f"{api_url}/run/predict",
            files={"data": ("image.png", image_bytes, "image/png")},
            timeout=120
        )

        response.raise_for_status()
        elapsed_time = time.time() - start_time

        print(f"✅ Request completed in {elapsed_time:.2f}s")
        print()

        # Parse response
        result = response.json()

        if "data" in result and len(result["data"]) > 0:
            data_item = result["data"][0]

            # URL로 반환된 경우 (Gradio 4.x)
            if isinstance(data_item, dict) and "url" in data_item:
                image_url = data_item["url"]
                # 상대 URL을 절대 URL로 변환
                if image_url.startswith("/"):
                    image_url = f"{api_url}{image_url}"

                print(f"📥 Downloading image from: {image_url}")
                img_response = requests.get(image_url, timeout=30)
                img_response.raise_for_status()
                image_data = img_response.content

            # Base64로 반환된 경우
            elif isinstance(data_item, str):
                if data_item.startswith("data:image"):
                    # data:image/png;base64,... format
                    base64_data = data_item.split(",")[1]
                    image_data = base64.b64decode(base64_data)
                else:
                    # Direct base64 data
                    image_data = base64.b64decode(data_item)
            else:
                print(f"❌ Unexpected response format: {type(data_item)}")
                print(f"Response: {result}")
                sys.exit(1)

            # Save result
            output_image = Image.open(io.BytesIO(image_data))
            output_path = image_path.rsplit(".", 1)[0] + "_removed_bg.png"
            output_image.save(output_path)

            print(f"💾 Result saved: {output_path}")
            print(f"   Size: {output_image.size}")
            print(f"   Mode: {output_image.mode}")
            print()
            print("✅ Test passed!")

        else:
            print("❌ Invalid response format")
            print(f"Response: {result}")
            sys.exit(1)

    except requests.exceptions.RequestException as e:
        print(f"❌ API request failed: {e}")
        sys.exit(1)
    except Exception as e:
        print(f"❌ Error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python test_api.py <image_path> [api_url]")
        print()
        print("Examples:")
        print("  python test_api.py test.png")
        print("  python test_api.py test.png http://localhost:7860")
        print("  python test_api.py test.png https://YOUR-USERNAME-background-removal.hf.space")
        sys.exit(1)

    image_path = sys.argv[1]
    api_url = sys.argv[2] if len(sys.argv) > 2 else "http://localhost:7860"

    test_api(image_path, api_url)
