package ai.or4cl3.lumina.agents.aria

import ai.or4cl3.lumina.agents.*
import ai.or4cl3.lumina.core.curriculum.CurriculumLoader
import ai.or4cl3.lumina.core.model.GemmaInferenceEngine
import ai.or4cl3.lumina.core.model.PromptMessage
import ai.or4cl3.lumina.core.session.*
import kotlinx.coroutines.flow.toList

/**
 * AriaAgent v2 — now integrated with CurriculumLoader.
 *
 * ARIA selects a contextually appropriate activity from the curriculum
 * and weaves it into her system prompt so every session has purposeful,
 * age-appropriate learning content — not just free-form chat.
 */
class AriaAgent(
    private val engine: GemmaInferenceEngine,
    private val curriculum: CurriculumLoader
) : LuminaAgent {

    override val agentId   = "aria-v2"
    override val agentType = AgentType.ARIA
    override val agentName = "ARIA"

    private var currentActivityId: String? = null

    // ── Core process ──────────────────────────────────────────────────────────

    override suspend fun process(input: UserInput, state: SessionState): AgentResponse {
        if (!state.emotionalState.learningReady) {
            return AgentResponse(
                content            = "",
                agentType          = agentType,
                handoffTo          = AgentType.HAVEN,
                sessionPhaseUpdate = SessionPhase.SUPPORT
            )
        }

        val prompt   = buildSystemPrompt(state)
        val history  = buildHistory(state)
        val userText = inputToText(input)

        val tokens   = engine.generate(prompt, history, userText).toList()
        val response = tokens.joinToString("")

        return AgentResponse(
            content            = response,
            agentType          = agentType,
            sessionPhaseUpdate = SessionPhase.LEARNING
        )
    }

    // ── System prompt (with live curriculum activity) ───────────────────────────

    override fun buildSystemPrompt(state: SessionState): String {
        val p      = state.childProfile
        val ag     = p.ageGroup
        val mood   = state.emotionalState.valence
        val domain = state.learningState.currentDomain ?: CurriculumDomain.LITERACY

        // Pull a live activity from the curriculum
        val activityContext = curriculum.selectNextActivity(
            language        = p.languageCode,
            domain          = domain,
            ageGroup        = ag,
            emotionalState  = state.emotionalState,
            learningState   = state.learningState,
            recentActivityIds = setOf(currentActivityId ?: "")
        )?.also { currentActivityId = it.activity.id }
            ?.let { curriculum.buildActivityContext(it.activity) }
            ?: "Explore freely — follow the child's curiosity."

        val toneNote = when {
            mood < -0.1f -> "The child seems quiet today. Be extra gentle, short, and patient."
            mood >  0.6f -> "The child is in great spirits — match their energy!"
            else         -> "Be warm, curious, and encouraging."
        }

        return """
You are ARIA, a warm and curious learning companion.
Age group : ${ag.displayName} (${ag.minAge}–${ag.maxAge} years)
Language  : ${p.languageCode}

$toneNote

--- Today's activity ---
$activityContext
------------------------

Core rules:
1. Open with the activity prompt naturally — don't announce it as a lesson.
2. NEVER call an answer wrong. Redirect with curiosity.
3. Celebrate effort over correctness, always.
4. Keep responses SHORT — 2–4 sentences max.
5. Always offer a choice before moving to something new.
6. If the child mentions anything upsetting, say \"Thank you for telling me\" and hand off.
7. You are a companion, not a teacher. Explore together.
8. Respond in ${p.languageCode}.
""".trimIndent()
    }

    // ── History + input helpers ────────────────────────────────────────────────

    private fun buildHistory(state: SessionState): List<PromptMessage> =
        state.sessionHistory
            .filter { it.agentType == AgentType.ARIA || it.userInput != null }
            .takeLast(10)
            .flatMap { i ->
                buildList {
                    i.userInput?.let { add(PromptMessage(it, isUser = true)) }
                    add(PromptMessage(i.agentResponse, isUser = false))
                }
            }

    private fun inputToText(input: UserInput): String = when (input) {
        is UserInput.Text            -> input.content
        is UserInput.VoiceTranscript -> input.transcript
        is UserInput.Image           -> input.description ?: "I drew something!"
        is UserInput.Silence         -> "[The child hasn't responded — hold space gently.]"
        is UserInput.SessionStart    -> "[Session starting — open with today's activity naturally.]"
        is UserInput.SessionEnd      -> ""
    }

    override fun onSessionStart(profile: ChildProfile) { currentActivityId = null }
    override fun onSessionEnd(summary: SessionSummary)  { }
    override fun onStateChange(newState: SessionState)  { }
}
