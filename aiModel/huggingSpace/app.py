"""
Background Removal API using rembg
Optimized for Hugging Face Spaces with GPU support
Simple Gradio-only implementation
"""

import gradio as gr
from rembg import remove, new_session
from PIL import Image
import io
import time

def create_rembg_session():
    """Create rembg session with GPU fallback to CPU if CUDA libraries are missing."""
    print("🔥 Loading rembg model...")
    start_time = time.time()
    try:
        session = new_session(model_name="u2net")
        load_time = time.time() - start_time
        print(f"✅ rembg model loaded in {load_time:.2f}s (GPU)")
        return session
    except Exception as e:
        print(f"⚠️  GPU session failed, falling back to CPU: {e}")
        try:
            session = new_session(model_name="u2net", providers=["CPUExecutionProvider"])
            load_time = time.time() - start_time
            print(f"✅ rembg model loaded in {load_time:.2f}s (CPU)")
            return session
        except Exception as e2:
            print(f"❌ Failed to load rembg: {e2}")
            raise

# Global session for model reuse
rembg_session = create_rembg_session()

def remove_background(image):
    """
    Remove background from image using rembg

    Args:
        image: PIL Image

    Returns:
        PIL Image with transparent background
    """
    if image is None:
        return None

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
        raise gr.Error(f"Background removal failed: {str(e)}")

# Create Gradio Interface
demo = gr.Interface(
    fn=remove_background,
    inputs=gr.Image(type="pil", label="Upload Image"),
    outputs=gr.Image(type="pil", label="Background Removed"),
    title="🎨 Background Removal API",
    description="""
    Fast background removal using rembg (u2net model).

    **Features:**
    - GPU-accelerated processing (when available)
    - Session reuse for faster processing
    - API access via gradio_client

    **Python API Usage:**
    ```python
    from gradio_client import Client

    client = Client("https://tigger13-background-removal.hf.space")
    result = client.predict("/path/to/image.png")
    ```
    """,
    examples=[],
    allow_flagging="never"
)

if __name__ == "__main__":
    demo.launch(
        server_name="0.0.0.0",
        server_port=7860,
        show_error=True  # Enable error reporting
    )
