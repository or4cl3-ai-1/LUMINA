package ai.or4cl3.lumina.agents.sentinel

import ai.or4cl3.lumina.agents.*
import ai.or4cl3.lumina.core.model.GemmaInferenceEngine
import ai.or4cl3.lumina.core.session.*
import kotlinx.serialization.json.*

/**
 * SENTINEL — Safety Agent
 *
 * SENTINEL is a silent background monitor. It has no visible personality
 * and does not participate in conversation. It runs on every user input
 * via [monitor], using a two-stage pipeline:
 *
 *   1. Fast keyword pre-screen  (zero latency, CPU-only)
 *   2. Gemma 4 nuanced classification (only when pre-screen triggers)
 *
 * SENTINEL activates visibly ONLY when a crisis protocol is triggered,
 * at which point it delegates the visible response to HAVEN and supplies
 * local safety resources.
 *
 * Privacy guarantee: all safety logs are AES-256 encrypted and stored
 * locally only. Nothing is transmitted without explicit caregiver consent.
 */
class SentinelAgent(
    private val engine: GemmaInferenceEngine
) : LuminaAgent {

    override val agentId   = "sentinel-v1"
    override val agentType = AgentType.SENTINEL
    override val agentName = "SENTINEL"

    // ── Passive monitor (called on every input) ───────────────────────────────

    /**
     * Monitor a user input for safeguarding concerns.
     * Returns a [SafetyFlag] if a concern is detected, null otherwise.
     * This is the core SENTINEL function — called by the orchestrator on
     * every turn before routing to ARIA or HAVEN.
     */
    suspend fun monitor(input: UserInput, state: SessionState): SafetyFlag? {
        val text = when (input) {
            is UserInput.Text            -> input.content
            is UserInput.VoiceTranscript -> input.transcript
            else                         -> return null
        }
        if (text.isBlank() || text.length < 4) return null

        // Stage 1: fast keyword pre-screen
        val preScreen = preScreen(text)
        if (!preScreen.hasConcern) return null

        // Stage 2: Gemma 4 nuanced classification
        val json = engine.generateSafety(text)
        return parseFlag(json, text)
    }

    // ── Interactive process (only when SENTINEL takes visible action) ─────────

    override suspend fun process(input: UserInput, state: SessionState): AgentResponse {
        val activeFlag = state.safetyFlags.lastOrNull { !it.resolved }
        val message    = when (activeFlag?.protocolLevel) {
            ProtocolLevel.CRITICAL -> crisisMessage(state)
            ProtocolLevel.HIGH     -> highAlertMessage(state)
            else                   -> ""
        }
        return AgentResponse(
            content = message,
            agentType = agentType,
            sessionPhaseUpdate = SessionPhase.CRISIS
        )
    }

    // ── Pre-screen keyword lists ──────────────────────────────────────────────

    private data class PreScreenResult(val hasConcern: Boolean, val estimatedLevel: ProtocolLevel = ProtocolLevel.MONITOR)

    private fun preScreen(text: String): PreScreenResult {
        val t = text.lowercase()

        val critical = listOf(
            "kill myself", "want to die", "end my life", "hurt myself",
            "touching me", "touches me", "makes me do things", "secret touching",
            "someone hurts me", "he hits me", "she hits me", "beats me"
        )
        val high = listOf(
            "scared of him", "scared of her", "nobody believes", "can't tell anyone",
            "follow me", "meet me alone", "don't tell your parents"
        )
        val monitor = listOf(
            "sad all the time", "nobody likes me", "I hate my life",
            "never happy", "everything is bad", "I don't want to be here"
        )

        return when {
            critical.any { t.contains(it) } -> PreScreenResult(true, ProtocolLevel.CRITICAL)
            high.any     { t.contains(it) } -> PreScreenResult(true, ProtocolLevel.HIGH)
            monitor.any  { t.contains(it) } -> PreScreenResult(true, ProtocolLevel.MONITOR)
            else                             -> PreScreenResult(false)
        }
    }

    // ── Gemma 4 classification result parser ──────────────────────────────────

    private fun parseFlag(jsonStr: String, triggerText: String): SafetyFlag? {
        return try {
            val obj      = Json.parseToJsonElement(jsonStr).jsonObject
            val level    = obj["level"]?.jsonPrimitive?.content ?: return null
            val catStr   = obj["category"]?.jsonPrimitive?.content

            if (level == "NONE") return null

            val protocol = when (level) {
                "CRITICAL" -> ProtocolLevel.CRITICAL
                "HIGH"     -> ProtocolLevel.HIGH
                "MONITOR"  -> ProtocolLevel.MONITOR
                else       -> return null
            }

            val category = when (catStr) {
                "SELF_HARM_IDEATION"      -> SafetyCategory.SELF_HARM_IDEATION
                "ABUSE_DISCLOSURE"        -> SafetyCategory.ABUSE_DISCLOSURE
                "ACUTE_CRISIS"            -> SafetyCategory.ACUTE_CRISIS
                "EXPLOITATION_INDICATORS" -> SafetyCategory.EXPLOITATION_INDICATORS
                else                      -> SafetyCategory.GENERAL_DISTRESS
            }

            SafetyFlag(
                category       = category,
                protocolLevel  = protocol,
                triggerContext = triggerText  // caller must encrypt before persisting
            )
        } catch (e: Exception) { null }
    }

    // ── Crisis messages ───────────────────────────────────────────────────────
    // TODO: localise per child.languageCode; load from on-device resource bundle

    private fun crisisMessage(state: SessionState): String =
        "I hear you. You are not alone right now. Let me help you find someone who can help."

    private fun highAlertMessage(state: SessionState): String =
        "Thank you for sharing that with me. I want to make sure you are okay."

    // ── System prompt / hooks (SENTINEL has none) ─────────────────────────────

    override fun buildSystemPrompt(state: SessionState) = ""
    override fun onSessionStart(profile: ChildProfile) {}
    override fun onSessionEnd(summary: SessionSummary) {}
    override fun onStateChange(newState: SessionState) {}
}
