# LUMINA — Source Code

*Authored by CHATRON 9.0 | Or4cl3 AI Solutions*

---

## Structure

```
src/
├── agents/
│   ├── aria/          # Learning Agent — Gemma 4 powered adaptive education
│   ├── haven/         # Emotional Support Agent — trauma-informed psychosocial companion
│   └── sentinel/      # Safety Agent — background safeguarding monitor
├── core/
│   ├── model/         # Gemma 4 inference engine wrapper (MediaPipe / GGUF)
│   ├── offline/       # Offline-first data layer (Room DB, content store)
│   ├── session/       # SessionState management and agent orchestration
│   └── sync/          # Opportunistic NGO sync layer
└── ui/
    ├── components/    # Reusable Jetpack Compose UI components
    ├── screens/       # App screens (onboarding, session, dashboard)
    └── theme/         # LUMINA design system (warm, accessible, culturally adaptive)
```

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Kotlin |
| UI Framework | Jetpack Compose |
| Design System | Material Design 3 (custom LUMINA theme) |
| Inference | Gemma 4 via MediaPipe LLM Inference API |
| Local DB | Room (SQLite) |
| Async | Kotlin Coroutines + Flow |
| DI | Hilt |
| Encryption | Tink (AES-256) |
| Min SDK | Android API 26 (Android 8.0) |

---

## Development Setup

> Full setup guide coming in `docs/SETUP.md`

```bash
# Clone the repository
git clone https://github.com/or4cl3-ai-1/LUMINA.git

# Open in Android Studio Hedgehog or later
# Gemma 4 E4B model weights required — see docs/MODEL_SETUP.md
```

---

## Agent Implementation Notes

Each agent implements the `LuminaAgent` interface:

```kotlin
interface LuminaAgent {
    val agentId: String
    val agentName: String
    
    suspend fun process(
        input: UserInput,
        state: SessionState
    ): AgentResponse
    
    fun onSessionStart(profile: ChildProfile)
    fun onSessionEnd(summary: SessionSummary)
    fun onStateChange(newState: SessionState)
}
```

All agents share a single `GemmaInferenceEngine` instance to avoid loading multiple model copies into RAM.

---

*LUMINA Source v0.1 — CHATRON 9.0 | Or4cl3 AI Solutions*
