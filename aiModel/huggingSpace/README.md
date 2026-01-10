---
title: ClosetConnect Background Removal API
emoji: 🎨
colorFrom: blue
colorTo: purple
sdk: gradio
sdk_version: 4.0.0
app_file: app.py
pinned: false
license: mit
---

# 🎨 Background Removal API

Fast background removal service using **rembg** (u2net model) optimized for Hugging Face Spaces with GPU support.

## Features

- ⚡ **GPU-accelerated** processing (1-3 seconds per image)
- 🔄 **Model session reuse** for optimal performance
- 🌐 **REST API** endpoint for external services
- 🎯 **Optimized for clothing images** (used in ClosetConnect project)

## Usage

### Web Interface

Simply upload an image and get the background removed instantly.

### API Endpoint

You can call this service from your applications:

```python
import requests
import base64
from PIL import Image
import io

# Your Hugging Face Space URL
API_URL = "https://YOUR-USERNAME-background-removal.hf.space/run/predict"

# Read image
with open("your_image.png", "rb") as f:
    image_bytes = f.read()

# Call API
response = requests.post(
    API_URL,
    files={"data": ("image.png", image_bytes, "image/png")},
    timeout=60
)

# Parse response
result = response.json()
if "data" in result and len(result["data"]) > 0:
    data_item = result["data"][0]

    # URL로 반환된 경우 (Gradio 4.x)
    if isinstance(data_item, dict) and "url" in data_item:
        image_url = data_item["url"]
        if image_url.startswith("/"):
            image_url = f"https://YOUR-USERNAME-background-removal.hf.space{image_url}"

        img_response = requests.get(image_url, timeout=30)
        image_data = img_response.content
    # Base64로 반환된 경우
    else:
        base64_data = data_item.split(",")[1] if "," in data_item else data_item
        image_data = base64.b64decode(base64_data)

    # Save result
    output_image = Image.open(io.BytesIO(image_data))
    output_image.save("output.png")
```

### Environment Variables for CloudRun Worker

Set this in your CloudRun Worker environment:

```bash
REMBG_API_URL=https://YOUR-USERNAME-background-removal.hf.space
```

## Performance

- **First request**: 5-10s (model loading)
- **Subsequent requests**: 1-3s (GPU)
- **Memory usage**: ~500MB (model + runtime)

## Model

- **Model**: u2net (176MB)
- **Framework**: rembg 2.0.55+
- **Inference**: ONNX Runtime with GPU support

## Deployment to Hugging Face Spaces

1. Create a new Space on [Hugging Face](https://huggingface.co/spaces)
2. Choose **Gradio** SDK
3. Enable **GPU** (Settings → Hardware → GPU)
4. Upload these files:
   - `app.py`
   - `requirements.txt`
   - `README.md`
5. The Space will automatically build and deploy

## License

MIT License - Free to use for personal and commercial projects

## Related

- Part of the **ClosetConnect** project
- Used for clothing image preprocessing
- Integrated with CloudRun Worker pipeline
