"""
Background Removal API using rembg
Optimized for Hugging Face Spaces with GPU support

This Gradio app provides a simple API endpoint for background removal
that can be called from CloudRun Worker or other services.
"""

import gradio as gr
from rembg import remove, new_session
from PIL import Image
import io
import time

# Global session for model reuse (GPU optimized)
print("🔥 Loading rembg model...")
start_time = time.time()
rembg_session = new_session(model_name='u2net')
load_time = time.time() - start_time
print(f"✅ rembg model loaded in {load_time:.2f}s")

def remove_background(image):
    """
    Remove background from image using rembg

    Args:
        image: PIL Image

    Returns:
        PIL Image with transparent background
    """
    print(f"📥 Received image: {image.size}, mode: {image.mode}")

    try:
        # Convert PIL Image to bytes
        img_byte_arr = io.BytesIO()
        image.save(img_byte_arr, format='PNG')
        img_byte_arr.seek(0)
        image_bytes = img_byte_arr.getvalue()

        # Remove background using rembg with session reuse
        print("🔄 Processing with rembg...")
        start_time = time.time()
        output_bytes = remove(image_bytes, session=rembg_session)
        process_time = time.time() - start_time
        print(f"✅ Background removed in {process_time:.2f}s")

        # Convert back to PIL Image
        output_image = Image.open(io.BytesIO(output_bytes)).convert("RGBA")
        print(f"📤 Output image: {output_image.size}, mode: {output_image.mode}")

        return output_image

    except Exception as e:
        print(f"❌ Error: {e}")
        raise e

# Create Gradio Interface
demo = gr.Interface(
    fn=remove_background,
    inputs=gr.Image(type="pil", label="Upload Image"),
    outputs=gr.Image(type="pil", label="Background Removed"),
    title="🎨 Background Removal API",
    description="""
    Fast background removal using rembg (u2net model).

    **Features:**
    - GPU-accelerated processing (on HF Spaces)
    - Session reuse for faster processing
    - API endpoint available at `/api/predict`

    **Usage:**
    ```python
    import requests

    response = requests.post(
        "YOUR_SPACE_URL/api/predict",
        files={"data": open("image.png", "rb")}
    )
    result = response.json()
    ```
    """,
    examples=[
        # You can add example images here if you want
    ],
    api_name="predict",  # API endpoint name
    allow_flagging="never"
)

if __name__ == "__main__":
    # Launch with API mode enabled
    demo.launch(
        server_name="0.0.0.0",
        server_port=7860,
        show_api=True
    )
