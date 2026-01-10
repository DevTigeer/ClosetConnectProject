"""
Test script for Background Removal API

Usage:
    python test_api.py <image_path> [api_url]

Examples:
    # Test local server
    python test_api.py test_image.png http://localhost:7860

    # Test deployed Hugging Face Space
    python test_api.py test_image.png https://tigger13-background-removal.hf.space
"""

import sys
import requests
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

        # Call API (FastAPI endpoint)
        response = requests.post(
            f"{api_url}/remove-bg",
            files={"file": ("image.png", image_bytes, "image/png")},
            timeout=120
        )

        response.raise_for_status()
        elapsed_time = time.time() - start_time

        print(f"✅ Request completed in {elapsed_time:.2f}s")
        print()

        # Response is PNG image bytes
        image_data = response.content

        # Save result
        output_image = Image.open(io.BytesIO(image_data))
        output_path = image_path.rsplit(".", 1)[0] + "_removed_bg.png"
        output_image.save(output_path)

        print(f"💾 Result saved: {output_path}")
        print(f"   Size: {output_image.size}")
        print(f"   Mode: {output_image.mode}")
        print()
        print("✅ Test passed!")

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
        print("  python test_api.py test.png https://tigger13-background-removal.hf.space")
        sys.exit(1)

    image_path = sys.argv[1]
    api_url = sys.argv[2] if len(sys.argv) > 2 else "http://localhost:7860"

    test_api(image_path, api_url)
