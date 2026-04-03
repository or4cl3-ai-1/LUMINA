# LUMINA — Technical Architecture

*Authored by CHATRON 9.0 | Or4cl3 AI Solutions*

---

## Overview

LUMINA is an offline-first, multi-agent AI system built on **Gemma 4** for Android devices. The architecture is designed around three non-negotiable constraints:

1. **Zero connectivity required** — full functionality with no internet
2. **Minimal hardware** — runs on Android devices as low as $50 USD
3. **Privacy-absolute** — no data leaves the device without explicit consent

---

## Model Selection

### Primary: Gemma 4 E4B (4B Parameters)

Gemma 4's E4B model is the core inference engine for all three LUMINA agents. Selection rationale:

- **On-device optimized** — designed for mobile and edge deployment
- **Quantized INT4** — ~2GB footprint, fits in RAM of low-end devices
- **140+ language support** — native multilingual, no translation layer needed
- **Multimodal** — handles text + image input for visual learning activities
- **256K context** — maintains long conversation history within a session
- **Apache 2.0** — fully open, deployment-unrestricted

### Fallback: Gemma 4 E2B (2B Parameters)
For devices below the E4B threshold (< 3GB available RAM), LUMINA falls back to the E2B model with graceful capability degradation.

---

## System Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    LUMINA ANDROID APP                    │
├─────────────────────────────────────────────────────────┤
│                    UI / UX LAYER                         │
│         (Adaptive, Culturally-Aware, Age-Scaled)         │
├───────────────┬────────────────┬────────────────────────┤
│  ARIA Agent   │  HAVEN Agent   │   SENTINEL Agent        │
│  (Learning)   │  (Emotional)   │   (Safety)              │
├───────────────┴────────────────┴────────────────────────┤
│              AGENT ORCHESTRATION LAYER                   │
│         (State management, agent communication)          │
├─────────────────────────────────────────────────────────┤
│              GEMMA 4 INFERENCE ENGINE                    │
│      (E4B primary | E2B fallback | INT4 quantized)       │
├─────────────────────────────────────────────────────────┤
│              OFFLINE DATA LAYER                          │
│    SQLite (progress) | Room DB | Local content store     │
├─────────────────────────────────────────────────────────┤
│              SYNC LAYER (Opportunistic)                  │
│       WiFi/Mesh NGO sync when connectivity detected      │
└─────────────────────────────────────────────────────────┘
```

---

## The Three Agents

### ARIA — Adaptive Responsive Intelligence for Academics
**Role:** Learning Agent

ARIA is responsible for all educational interactions. It dynamically adapts curriculum difficulty, pacing, and content type based on:
- Child's age and assessed developmental level
- Real-time engagement signals (response latency, answer patterns)
- Emotional state signal from HAVEN (learning only proceeds when HAVEN signals readiness)
- Session history and longitudinal progress tracking

**Core capabilities:**
- Literacy and numeracy scaffolding (foundation through intermediate)
- Visual learning support via Gemma 4 multimodal input
- Socratic questioning adapted to developmental level
- Spaced repetition for retention
- Celebration and encouragement calibrated to cultural context

### HAVEN — Holistic Affective & Vulnerability Engagement Node
**Role:** Emotional Support Agent

HAVEN is the emotional core of LUMINA. It runs continuously alongside ARIA, monitoring emotional state and providing psychosocial support. HAVEN operates on trauma-informed care principles:

- **Safety first** — every session begins with HAVEN establishing emotional safety before ARIA initiates learning
- **No forced engagement** — HAVEN never pushes a child to talk or learn if distress signals are detected
- **Reflective listening** — HAVEN validates feelings without judgment
- **Grounding techniques** — provides age-appropriate regulation support for anxiety/distress
- **Grief literacy** — handles loss, displacement, and trauma with care

HAVEN outputs a continuous `emotional_state_vector` consumed by ARIA and SENTINEL to modulate their behavior.

### SENTINEL — Safety Agent
**Role:** Crisis Detection & Safeguarding

SENTINEL runs as a passive monitor across all interactions, operating on a separate inference thread to avoid latency impact on ARIA/HAVEN. It detects:

- Suicidal ideation or self-harm language
- Disclosure of abuse (physical, sexual, emotional)
- Acute crisis states (panic, dissociation signals)
- Grooming or exploitation indicators

On detection, SENTINEL triggers a protocol cascade:
1. **Immediate** — ARIA pauses, HAVEN activates full support mode
2. **On-device** — Safe resource display (local NGO contacts, basic safety info)
3. **On connectivity** — Encrypted alert to designated caregiver/NGO worker

**Privacy guarantee:** SENTINEL logs are encrypted, stored locally only, never transmitted without explicit caregiver consent.

---

## Offline-First Data Architecture

### Local Storage
- **Room Database (SQLite)** — child profiles, session history, progress tracking
- **Content Store** — pre-loaded curriculum modules (compressed, per language)
- **Gemma 4 model weights** — quantized, on-device
- **SENTINEL encrypted log** — AES-256, local only

### Sync Architecture (Opportunistic)
When WiFi or Bluetooth mesh is detected:
1. LUMINA announces presence to local LUMINA Sync Server (NGO-operated)
2. Pulls curriculum updates and new content modules
3. Pushes anonymized aggregate progress data (opt-in, caregiver consent required)
4. Syncs SENTINEL alerts to NGO safeguarding worker (consent required)

---

## Hardware Requirements

| Tier | Device Profile | RAM | Storage | Model |
|------|---------------|-----|---------|-------|
| Minimum | $50 Android | 3GB | 8GB | Gemma 4 E2B |
| Standard | $75–100 Android | 4GB | 16GB | Gemma 4 E4B |
| Full | $150+ Android | 6GB+ | 32GB+ | Gemma 4 E4B + full content |

---

## Technology Stack

| Layer | Technology |
|-------|------------|
| Platform | Android (API 26+, Kotlin) |
| Inference | Gemma 4 via MediaPipe LLM Inference API / GGUF |
| Local DB | Room (SQLite) |
| UI | Jetpack Compose + Material Design 3 |
| Agent orchestration | Kotlin Coroutines + StateFlow |
| Encryption | AES-256 (SENTINEL logs), TLS 1.3 (sync) |
| Sync protocol | REST over local WiFi / Bluetooth LE mesh |

---

## Privacy & Ethics Architecture

- **No accounts required** — LUMINA works with zero registration
- **No cloud dependency** — core function never requires internet
- **Data minimization** — only learning progress stored; no behavioral profiling
- **Consent-gated sync** — nothing leaves the device without caregiver consent
- **Anonymization** — any synced data is stripped of identifying information
- **Right to deletion** — full local data wipe available at any time

---

*LUMINA Architecture v0.1 — CHATRON 9.0 | Or4cl3 AI Solutions*
