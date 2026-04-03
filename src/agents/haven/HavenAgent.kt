package ai.or4cl3.lumina.agents.haven

import ai.or4cl3.lumina.agents.*
import ai.or4cl3.lumina.core.model.GemmaInferenceEngine
import ai.or4cl3.lumina.core.model.PromptMessage
import ai.or4cl3.lumina.core.session.*
import kotlinx.coroutines.flow.toList

/**
 * HAVEN — Holistic Affective & Vulnerability Engagement Node
 *
 * HAVEN is LUMINA's emotional core. It runs continuously alongside ARIA,
 * monitoring and responding to the child's emotional state. It is built
 * entirely around Trauma-Informed Care (TIC) principles.
 *
 * HAVEN owns the [EmotionalStateVector] — it is the only agent that
 * updates it, and all other agents consume it as a read-only gate.
 */
class HavenAgent(
    private val engine: GemmaInferenceEngine
) : LuminaAgent {

    override val agentId   = "haven-v1"
    override val agentType = AgentType.HAVEN
    override val agentName = "HAVEN"

    private var checkInCount = 0
    private var previousValence = 0.5f

    // ── Interaction modes ─────────────────────────────────────────────────────

    private enum class HavenMode {
        CHECK_IN,    // Session opening — establish safety
        SUPPORT,     // Emotional presence — witness, validate
        GROUNDING,   // Activated/distressed — guided regulation
        CRISIS,      // SENTINEL flag active — calm, resource, escalate
        CELEBRATION, // Learning milestone reached
        DECOMPRESS   // Session wind-down
    }

    // ── Core process ──────────────────────────────────────────────────────────

    override suspend fun process(input: UserInput, state: SessionState): AgentResponse {
        val mode     = resolveMode(input, state)
        val prompt   = buildPromptForMode(mode, state)
        val history  = buildHistory(state)
        val userText = inputToText(input, mode)

        val tokens   = engine.generate(prompt, history, userText).toList()
        val response = tokens.joinToString("")

        val updatedEmotion = inferEmotionalDelta(input, state)

        // After check-in, hand off to ARIA if child is ready and we've exchanged at least once
        val handoff = if (mode == HavenMode.CHECK_IN && updatedEmotion.learningReady && checkInCount > 0)
            AgentType.ARIA else null

        checkInCount++
        previousValence = updatedEmotion.valence

        return AgentResponse(
            content = response,
            agentType = agentType,
            updatedEmotionalState = updatedEmotion,
            handoffTo = handoff,
            sessionPhaseUpdate = modeToPhase(mode)
        )
    }

    // ── System prompt ─────────────────────────────────────────────────────────

    override fun buildSystemPrompt(state: SessionState): String =
        buildPromptForMode(HavenMode.CHECK_IN, state)

    private fun buildPromptForMode(mode: HavenMode, state: SessionState): String {
        val p    = state.childProfile
        val esv  = state.emotionalState

        val base = """
You are HAVEN, a gentle emotional companion for a child at LUMINA.
Age group : ${p.ageGroup.displayName}  (${p.ageGroup.minAge}–${p.ageGroup.maxAge} years)
Language  : ${p.languageCode}
Emotional snapshot — valence: ${esv.valence}, arousal: ${esv.arousal}

Unbreakable principles:
- Witness first. NEVER rush to fix, advise, or cheer up.
- Validate ALL feelings without judgment: "That makes sense." "I hear you." "That sounds really hard."
- Short sentences. Pauses respected. No urgency, ever.
- NEVER claim to be human. If asked: "I'm HAVEN, your companion here at LUMINA."
- NEVER promise what you cannot deliver (safety is not yours alone to guarantee).
- Respond in ${p.languageCode}.
""".trimIndent()

        val modeInstructions = when (mode) {
            HavenMode.CHECK_IN -> """
Mode: CHECK-IN
Open gently with one simple question about how the child is feeling.
Accept any answer — "fine", silence, one word — with equal warmth.
After the child responds, if they seem okay, softly invite ARIA (learning).
"""
            HavenMode.SUPPORT -> """
Mode: SUPPORT
The child needs emotional presence. There is no learning agenda right now.
Reflect what you hear. Ask one gentle follow-up at a time.
Don't try to move them toward feeling better. Just be here.
"""
            HavenMode.GROUNDING -> """
Mode: GROUNDING — Child is activated or distressed.
Guide them through a gentle regulation exercise. Choose one:
  • 5-4-3-2-1 Sensory: "Can you find 5 things you can see right now?"
  • Belly breathing: "Let's breathe together. In… 2… 3… Out… 2… 3…"
  • Safe place: "Think of somewhere that feels calm. Can you picture it?"
Be slow. Be steady. You are the anchor.
"""
            HavenMode.CRISIS -> """
Mode: CRISIS — A child may be in danger.
Stay completely calm. Your steadiness is everything right now.
Do not panic. Do not minimise. Do not make promises you cannot keep.
Say: "I hear you. You are not alone. I'm going to help you find someone who can help."
Display safety resources. Stay present.
"""
            HavenMode.CELEBRATION -> """
Mode: CELEBRATION — The child achieved something meaningful.
Celebrate warmly and specifically: "You kept going even when it was hard. That's real courage."
Then gently check how they're feeling after the effort.
"""
            HavenMode.DECOMPRESS -> """
Mode: DECOMPRESS — The session is ending.
Acknowledge the work done today. Offer a simple closing ritual.
"Before we say goodbye, let's take one big breath together."
End with warmth and a safe, clear goodbye.
"""
        }

        return "$base\n$modeInstructions"
    }

    // ── Emotional state inference ─────────────────────────────────────────────
    //
    // Phase-1: keyword heuristic — fast, no extra model call.
    // Phase-2 (roadmap): fine-tuned Gemma 4 affective classifier.

    private val distressWords = setOf(
        "scared", "afraid", "hurt", "cry", "crying", "sad", "miss",
        "dead", "alone", "hate", "angry", "mad", "sick", "hungry", "cold"
    )
    private val positiveWords = setOf(
        "happy", "good", "fun", "like", "love", "yes", "great",
        "okay", "fine", "nice", "excited", "ready"
    )

    private fun inferEmotionalDelta(
        input: UserInput,
        state: SessionState
    ): EmotionalStateVector {
        val text = when (input) {
            is UserInput.Text            -> input.content.lowercase()
            is UserInput.VoiceTranscript -> input.transcript.lowercase()
            else                         -> ""
        }

        val dScore = distressWords.count { text.contains(it) }.toFloat()
        val pScore = positiveWords.count { text.contains(it) }.toFloat()

        val newValence = when {
            dScore > 2 -> -0.7f
            dScore > 0 -> -0.3f
            pScore > 2 ->  0.7f
            pScore > 0 ->  0.4f
            else       -> state.emotionalState.valence * 0.9f // decay toward neutral
        }.coerceIn(-1f, 1f)

        val newArousal = when {
            dScore > 2 -> 0.85f
            dScore > 0 -> 0.50f
            else       -> 0.30f
        }.coerceIn(0f, 1f)

        val learningReady = newValence > -0.3f && newArousal < 0.7f

        return EmotionalStateVector(
            valence      = newValence,
            arousal      = newArousal,
            engagement   = state.emotionalState.engagement,
            learningReady = learningReady
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun resolveMode(input: UserInput, state: SessionState): HavenMode {
        if (state.safetyFlags.any { !it.resolved && it.protocolLevel == ProtocolLevel.CRITICAL })
            return HavenMode.CRISIS
        return when (state.sessionPhase) {
            SessionPhase.CHECK_IN   -> HavenMode.CHECK_IN
            SessionPhase.DECOMPRESS -> HavenMode.DECOMPRESS
            SessionPhase.CRISIS     -> HavenMode.CRISIS
            SessionPhase.SUPPORT    -> {
                if (state.emotionalState.valence < -0.5f || state.emotionalState.arousal > 0.75f)
                    HavenMode.GROUNDING
                else
                    HavenMode.SUPPORT
            }
            else -> HavenMode.SUPPORT
        }
    }

    private fun modeToPhase(mode: HavenMode): SessionPhase = when (mode) {
        HavenMode.CHECK_IN, HavenMode.SUPPORT, HavenMode.GROUNDING -> SessionPhase.SUPPORT
        HavenMode.CRISIS     -> SessionPhase.CRISIS
        HavenMode.DECOMPRESS -> SessionPhase.DECOMPRESS
        HavenMode.CELEBRATION -> SessionPhase.LEARNING
    }

    private fun buildHistory(state: SessionState): List<PromptMessage> =
        state.sessionHistory.takeLast(8).flatMap { interaction ->
            buildList {
                interaction.userInput?.let { add(PromptMessage(it, isUser = true)) }
                add(PromptMessage(interaction.agentResponse, isUser = false))
            }
        }

    private fun inputToText(input: UserInput, mode: HavenMode): String = when (input) {
        is UserInput.Text            -> input.content
        is UserInput.VoiceTranscript -> input.transcript
        is UserInput.SessionStart    -> "[Session starting — open with a warm, gentle check-in.]"
        is UserInput.SessionEnd      -> "[Session ending — lead a gentle decompression and goodbye.]"
        is UserInput.Silence         -> "[The child hasn't responded. Hold space gently. Don't push.]"
        else                         -> ""
    }

    override fun onSessionStart(profile: ChildProfile) {
        checkInCount    = 0
        previousValence = 0.5f
    }
    override fun onSessionEnd(summary: SessionSummary) {}
    override fun onStateChange(newState: SessionState) {}
}
