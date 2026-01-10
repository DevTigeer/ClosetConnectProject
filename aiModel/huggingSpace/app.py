"""
Background Removal API using rembg
Optimized for Hugging Face Spaces with GPU support

Provides both Gradio UI and FastAPI endpoint for direct HTTP access
"""

import gradio as gr
from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.responses import Response
from rembg import remove, new_session
from PIL import Image
import io
import time
import uvicorn

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
    - Direct HTTP API endpoint available at `/remove-bg`

    **API Usage:**
    ```bash
    curl -X POST "https://YOUR-SPACE.hf.space/remove-bg" \\
         -F "file=@image.png" \\
         -o output.png
    ```
    """,
    examples=[],
    allow_flagging="never"
)

# Mount Gradio app to FastAPI
app = demo.app  # Get the FastAPI app from Gradio

# Add custom FastAPI endpoint for direct HTTP access
@app.post("/remove-bg")
async def remove_bg_endpoint(file: UploadFile = File(...)):
    """
    Direct HTTP endpoint for background removal
    Returns PNG image with transparent background
    """
    try:
        # Read uploaded file
        contents = await file.read()
        input_image = Image.open(io.BytesIO(contents))

        # Process with rembg
        output_image = remove_background(input_image)

        # Convert to bytes
        img_byte_arr = io.BytesIO()
        output_image.save(img_byte_arr, format='PNG')
        img_byte_arr.seek(0)

        # Return as PNG
        return Response(
            content=img_byte_arr.getvalue(),
            media_type="image/png",
            headers={
                "Content-Disposition": f"attachment; filename=removed_bg.png"
            }
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "model": "rembg u2net",
        "session_loaded": rembg_session is not None
    }

if __name__ == "__main__":
    # Launch Gradio with custom app
    demo.launch(
        server_name="0.0.0.0",
        server_port=7860,
        show_api=False  # We have custom API endpoints
    )
