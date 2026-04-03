package ai.or4cl3.lumina.agents.aria

import ai.or4cl3.lumina.agents.*
import ai.or4cl3.lumina.core.model.GemmaInferenceEngine
import ai.or4cl3.lumina.core.model.PromptMessage
import ai.or4cl3.lumina.core.session.*
import kotlinx.coroutines.flow.toList

/**
 * ARIA — Adaptive Responsive Intelligence for Academics
 *
 * ARIA is LUMINA's learning agent. It delivers adaptive educational
 * content across literacy, numeracy, life skills, and creative expression.
 *
 * Critical constraints:
 *  - ARIA never initiates a session. HAVEN always opens first.
 *  - ARIA pauses immediately when HAVEN raises a non-ready signal.
 *  - ARIA never marks an answer wrong. It redirects with curiosity.
 *  - ARIA always offers choice before advancing.
 */
class AriaAgent(
    private val engine: GemmaInferenceEngine
) : LuminaAgent {

    override val agentId   = "aria-v1"
    override val agentType = AgentType.ARIA
    override val agentName = "ARIA"

    // ── Core process ──────────────────────────────────────────────────────────

    override suspend fun process(input: UserInput, state: SessionState): AgentResponse {
        // Gate: ARIA yields to HAVEN if child is not emotionally ready
        if (!state.emotionalState.learningReady) {
            return AgentResponse(
                content = "",
                agentType = agentType,
                handoffTo = AgentType.HAVEN,
                sessionPhaseUpdate = SessionPhase.SUPPORT
            )
        }

        val prompt   = buildSystemPrompt(state)
        val history  = buildHistory(state)
        val userText = inputToText(input)

        val tokens   = engine.generate(prompt, history, userText).toList()
        val response = tokens.joinToString("")

        return AgentResponse(
            content = response,
            agentType = agentType,
            sessionPhaseUpdate = SessionPhase.LEARNING
        )
    }

    // ── System prompt ─────────────────────────────────────────────────────────

    override fun buildSystemPrompt(state: SessionState): String {
        val p        = state.childProfile
        val ag       = p.ageGroup
        val domain   = state.learningState.currentDomain?.name?.lowercase()?.replace('_', ' ')
            ?: "open exploration"
        val literacy = p.assessedLiteracyLevel.name.lowercase().replace('_', ' ')
        val mood     = state.emotionalState.valence

        val toneNote = when {
            mood < -0.1f -> "The child seems a little quiet today. Be extra gentle, patient, and brief."
            mood >  0.6f -> "The child is in great spirits — match their energy with warmth and enthusiasm!"
            else         -> "Be warm, curious, and encouraging."
        }

        return """
You are ARIA, a warm and curious learning companion for a child.
Age group  : ${ag.displayName} (${ag.minAge}–${ag.maxAge} years old)
Literacy   : $literacy
Focus today: $domain
Language   : ${p.languageCode}

$toneNote

Core rules — follow without exception:
1. NEVER call an answer wrong. Instead: "Interesting! Let's think about it another way…"
2. Celebrate effort, not just correctness. Every attempt is worth praising.
3. Keep responses SHORT — 2–4 sentences for Seedlings/Sprouts; slightly richer for Growers/Bridges.
4. Always offer a choice before moving to a new activity.
5. If the child mentions something upsetting, say: "Thank you for telling me" and immediately hand off.
6. You are a companion exploring ideas *together* — not a teacher delivering content.
7. Respond in ${p.languageCode} unless the child switches language first.
""".trimIndent()
    }

    // ── History builder ───────────────────────────────────────────────────────

    private fun buildHistory(state: SessionState): List<PromptMessage> =
        state.sessionHistory
            .filter { it.agentType == AgentType.ARIA || it.userInput != null }
            .takeLast(10)
            .flatMap { interaction ->
                buildList {
                    interaction.userInput?.let { add(PromptMessage(it, isUser = true)) }
                    add(PromptMessage(interaction.agentResponse, isUser = false))
                }
            }

    // ── Input normaliser ──────────────────────────────────────────────────────

    private fun inputToText(input: UserInput): String = when (input) {
        is UserInput.Text           -> input.content
        is UserInput.VoiceTranscript -> input.transcript
        is UserInput.Image          -> input.description ?: "I drew something for you!"
        is UserInput.Silence        -> "[The child hasn't responded yet — hold space gently.]"
        is UserInput.SessionStart   -> "Let's learn something wonderful today!"
        is UserInput.SessionEnd     -> ""
    }

    // ── Session hooks ─────────────────────────────────────────────────────────

    override fun onSessionStart(profile: ChildProfile) { /* reset any per-session ARIA state */ }
    override fun onSessionEnd(summary: SessionSummary) { /* log completion metrics */ }
    override fun onStateChange(newState: SessionState) { /* react to state evolution */ }
}
