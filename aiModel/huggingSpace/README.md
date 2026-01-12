---
title: ClosetConnect Background Removal API
emoji: 🎨
colorFrom: blue
colorTo: purple
sdk: gradio
sdk_version: 6.3.0
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
from PIL import Image
import io

# Your Hugging Face Space URL
API_URL = "https://YOUR-USERNAME-background-removal.hf.space/remove-bg"

# Read image
with open("your_image.png", "rb") as f:
    image_bytes = f.read()

# Call API
response = requests.post(
    API_URL,
    files={"file": ("image.png", image_bytes, "image/png")},
    timeout=60
)

# Save result (response is PNG image)
output_image = Image.open(io.BytesIO(response.content))
output_image.save("output.png")
```

Or use curl:
```bash
curl -X POST "https://YOUR-USERNAME-background-removal.hf.space/remove-bg" \
     -F "file=@your_image.png" \
     -o output.png
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
