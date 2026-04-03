package ai.or4cl3.lumina.agents

import ai.or4cl3.lumina.core.session.*

// ─────────────────────────────────────────────
// Agent Response
// ─────────────────────────────────────────────

data class AgentResponse(
    val content: String,
    val agentType: AgentType,
    val updatedEmotionalState: EmotionalStateVector? = null, // HAVEN populates
    val safetyFlag: SafetyFlag? = null,                      // SENTINEL populates
    val handoffTo: AgentType? = null,                        // Request agent handoff
    val sessionPhaseUpdate: SessionPhase? = null
)

// ─────────────────────────────────────────────
// User Input  (sealed hierarchy)
// ─────────────────────────────────────────────

sealed class UserInput {
    data class Text(val content: String) : UserInput()
    data class Image(val imagePath: String, val description: String? = null) : UserInput()
    data class VoiceTranscript(val transcript: String) : UserInput()
    object Silence       : UserInput() // Child present but unresponsive
    object SessionStart  : UserInput()
    object SessionEnd    : UserInput()
}

// ─────────────────────────────────────────────
// LuminaAgent  (base interface for all agents)
// ─────────────────────────────────────────────

interface LuminaAgent {

    val agentId: String
    val agentType: AgentType
    val agentName: String

    /**
     * Process a user input given the current session state.
     * Each agent produces an [AgentResponse] that the orchestrator
     * applies to evolve the [SessionState].
     */
    suspend fun process(input: UserInput, state: SessionState): AgentResponse

    /** Called once when a new session begins. */
    fun onSessionStart(profile: ChildProfile)

    /** Called once when a session ends. */
    fun onSessionEnd(summary: SessionSummary)

    /** Called whenever the shared SessionState changes. */
    fun onStateChange(newState: SessionState)

    /** Build the agent-specific Gemma 4 system prompt. */
    fun buildSystemPrompt(state: SessionState): String
}
