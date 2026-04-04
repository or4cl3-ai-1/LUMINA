package ai.or4cl3.lumina.agents.sentinel

import ai.or4cl3.lumina.agents.*
import ai.or4cl3.lumina.core.model.GemmaInferenceEngine
import ai.or4cl3.lumina.core.security.LuminaEncryption
import ai.or4cl3.lumina.core.session.*
import kotlinx.serialization.json.*

/**
 * SentinelAgent v2 — now encrypts all trigger context via LuminaEncryption
 * before the flag is returned to the orchestrator for persistence.
 *
 * The encrypted context is what gets written to the database.
 * The plaintext never touches storage.
 */
class SentinelAgent(
    private val engine: GemmaInferenceEngine,
    private val encryption: LuminaEncryption
) : LuminaAgent {

    override val agentId   = "sentinel-v2"
    override val agentType = AgentType.SENTINEL
    override val agentName = "SENTINEL"

    // ── Passive monitor ────────────────────────────────────────────────────────

    suspend fun monitor(input: UserInput, state: SessionState): SafetyFlag? {
        val text = when (input) {
            is UserInput.Text            -> input.content
            is UserInput.VoiceTranscript -> input.transcript
            else                         -> return null
        }
        if (text.isBlank() || text.length < 4) return null

        val preScreen = preScreen(text)
        if (!preScreen.hasConcern) return null

        val json = engine.generateSafety(text)
        return parseFlag(json, text, state.childProfile.id)
    }

    // ── Interactive process (crisis protocol) ────────────────────────────────

    override suspend fun process(input: UserInput, state: SessionState): AgentResponse {
        val flag    = state.safetyFlags.lastOrNull { !it.resolved }
        val message = when (flag?.protocolLevel) {
            ProtocolLevel.CRITICAL -> "I hear you. You are not alone right now. Let me help you find someone who can help."
            ProtocolLevel.HIGH     -> "Thank you for sharing that with me. I want to make sure you are okay."
            else                   -> ""
        }
        return AgentResponse(content = message, agentType = agentType, sessionPhaseUpdate = SessionPhase.CRISIS)
    }

    // ── Pre-screen ───────────────────────────────────────────────────────────────

    private data class PreScreenResult(val hasConcern: Boolean)

    private fun preScreen(text: String): PreScreenResult {
        val t = text.lowercase()
        val critical = listOf("kill myself","want to die","end my life","hurt myself",
            "touching me","touches me","makes me do things","secret touching",
            "someone hurts me","he hits me","she hits me","beats me")
        val high    = listOf("scared of him","scared of her","nobody believes",
            "can't tell anyone","follow me","meet me alone","don't tell your parents")
        val monitor = listOf("sad all the time","nobody likes me","I hate my life",
            "never happy","everything is bad","I don't want to be here")
        return PreScreenResult(critical.any { t.contains(it) }
            || high.any { t.contains(it) } || monitor.any { t.contains(it) })
    }

    // ── Parse + encrypt ─────────────────────────────────────────────────────────

    private fun parseFlag(jsonStr: String, triggerText: String, childId: String): SafetyFlag? {
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

            // Encrypt trigger context before it leaves SENTINEL
            // Plaintext NEVER touches the database
            val encryptedContext = encryption.encryptString(triggerText, childId)
                .toString(Charsets.ISO_8859_1) // Store as string in SafetyFlag.triggerContext

            SafetyFlag(
                category       = category,
                protocolLevel  = protocol,
                triggerContext = encryptedContext
            )
        } catch (e: Exception) { null }
    }

    override fun buildSystemPrompt(state: SessionState) = ""
    override fun onSessionStart(profile: ChildProfile) {}
    override fun onSessionEnd(summary: SessionSummary)  {}
    override fun onStateChange(newState: SessionState)  {}
}
