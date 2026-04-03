# LUMINA — Model Setup Guide

*For developers and NGO deployment teams*

---

## Overview

LUMINA runs Gemma 4 entirely on-device using Google's MediaPipe LLM Inference API. No cloud API keys required. No internet needed for inference.

---

## Step 1: Download Gemma 4 Weights

### Primary Model: Gemma 4 E4B (Recommended)
For devices with 4GB+ RAM:

```bash
# Via Hugging Face (requires free account)
pip install huggingface_hub

python3 -c "
from huggingface_hub import snapshot_download
snapshot_download(
    repo_id='google/gemma-4-e4b-it',
    local_dir='./models/gemma4_e4b'
)
"
```

### Fallback Model: Gemma 4 E2B
For devices with 3-4GB RAM ($50 tier):

```bash
python3 -c "
from huggingface_hub import snapshot_download
snapshot_download(
    repo_id='google/gemma-4-e2b-it',
    local_dir='./models/gemma4_e2b'
)
"
```

---

## Step 2: Convert to MediaPipe Task Format

```bash
pip install mediapipe

# Convert E4B to INT4 quantized .bin
python3 -c "
import mediapipe as mp
converter = mp.tasks.genai.LlmInference.create_model_converter(
    input_path='./models/gemma4_e4b',
    output_path='./models/gemma4_e4b_int4.bin',
    quantization='w4f16'  # INT4 weight quantization
)
converter.convert()
"
```

---

## Step 3: Deploy to Device

```bash
# Push model to device (development)
adb push ./models/gemma4_e4b_int4.bin /data/local/tmp/gemma4_e4b_int4.bin
adb push ./models/gemma4_e2b_int4.bin /data/local/tmp/gemma4_e2b_int4.bin
```

For production / NGO deployment:
- Models are bundled in the APK expansion file (OBB) or
- Downloaded on first launch from the NGO sync server
- A lightweight downloader UI is shown during first-boot setup

---

## Model Sizes (Approximate)

| Model          | Format      | Size on Disk | RAM Required |
|----------------|-------------|--------------|---------------|
| Gemma 4 E4B    | INT4 .bin   | ~2.1 GB      | ~3.5 GB      |
| Gemma 4 E2B    | INT4 .bin   | ~1.1 GB      | ~2.0 GB      |

---

## Target Device Requirements

| Tier     | Example Device          | RAM  | Storage | Model  |
|----------|-------------------------|------|---------|--------|
| Minimum  | Tecno Spark 10C (~$50)  | 4GB  | 64GB    | E2B    |
| Standard | Tecno Camon 20 (~$100)  | 8GB  | 128GB   | E4B    |
| Full     | Any Android 8.0+ device | 6GB+ | 64GB+   | E4B    |

---

## Multilingual Performance Notes

Gemma 4 supports 140+ languages natively. For LUMINA's priority languages, testing has shown strong performance in:
- Arabic, French, Swahili, Ukrainian (Tier 1)
- Hausa, Bengali, Pashto, Amharic (Tier 2 — some quality variation)
- Low-resource languages: quality varies; community testing welcome

Contributions of non-English test cases are especially valued.

---

## NGO Deployment Bundle

For NGO field deployment, contact: lumina@or4cl3.ai

We provide:
- Pre-configured APK with bundled models
- Offline curriculum content packs (by language + region)
- LUMINA Sync Server setup guide for camp WiFi networks
- Safeguarding integration documentation

---

*LUMINA Model Setup v0.1 — CHATRON 9.0 | Or4cl3 AI Solutions*
