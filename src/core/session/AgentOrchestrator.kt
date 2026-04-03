package ai.or4cl3.lumina.core.session

import ai.or4cl3.lumina.agents.*
import ai.or4cl3.lumina.agents.aria.AriaAgent
import ai.or4cl3.lumina.agents.haven.HavenAgent
import ai.or4cl3.lumina.agents.sentinel.SentinelAgent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AgentOrchestrator
 *
 * The central coordinator of all LUMINA interactions. Routes between
 * ARIA, HAVEN, and SENTINEL; maintains the authoritative [SessionState];
 * and applies agent responses to evolve that state.
 *
 * Session lifecycle:
 *   startSession()  → HAVEN opens with CHECK_IN
 *   processInput()  → SENTINEL monitors silently; active agent responds
 *   endSession()    → HAVEN leads DECOMPRESS; all agents notified
 *
 * All state transitions are deterministic and observable via [sessionState].
 */
class AgentOrchestrator(
    private val aria     : AriaAgent,
    private val haven    : HavenAgent,
    private val sentinel : SentinelAgent
) {
    private val _sessionState   = MutableStateFlow<SessionState?>(null)
    val sessionState: StateFlow<SessionState?> = _sessionState.asStateFlow()

    private val _lastResponse   = MutableStateFlow<AgentResponse?>(null)
    val lastResponse: StateFlow<AgentResponse?> = _lastResponse.asStateFlow()

    // ── Session lifecycle ─────────────────────────────────────────────────────

    suspend fun startSession(profile: ChildProfile) {
        val initial = SessionState(
            childProfile  = profile,
            activeAgent   = AgentType.HAVEN,
            sessionPhase  = SessionPhase.CHECK_IN
        )
        _sessionState.value = initial

        aria.onSessionStart(profile)
        haven.onSessionStart(profile)
        sentinel.onSessionStart(profile)

        // HAVEN opens every session — no exceptions
        val opening = haven.process(UserInput.SessionStart, initial)
        applyResponse(opening, UserInput.SessionStart)
    }

    suspend fun processInput(input: UserInput) {
        val state = _sessionState.value ?: return

        // ── Stage 1: SENTINEL passive monitor (silent, every turn) ────────────
        val flag = sentinel.monitor(input, state)
        val monitoredState = if (flag != null) {
            val updated = state.copy(
                safetyFlags  = state.safetyFlags + flag,
                sessionPhase = when (flag.protocolLevel) {
                    ProtocolLevel.CRITICAL, ProtocolLevel.HIGH -> SessionPhase.CRISIS
                    ProtocolLevel.MONITOR                     -> state.sessionPhase
                }
            )
            _sessionState.value = updated
            updated
        } else state

        // ── Stage 2: Route to active agent ────────────────────────────────────
        val activeAgent = route(monitoredState)
        val response    = activeAgent.process(input, monitoredState)
        applyResponse(response, input)
    }

    suspend fun endSession() {
        val state = _sessionState.value ?: return
        val decomprState = state.copy(sessionPhase = SessionPhase.DECOMPRESS)
        _sessionState.value = decomprState

        val closing = haven.process(UserInput.SessionEnd, decomprState)
        _lastResponse.value = closing

        val summary = SessionSummary(
            sessionId          = state.sessionId,
            durationMinutes    = ((System.currentTimeMillis() - state.sessionStartTime) / 60_000).toInt(),
            interactionCount   = state.sessionHistory.size,
            finalEmotionalState = state.emotionalState,
            learningMilestones = state.learningState.recentMilestones,
            safetyFlagsRaised  = state.safetyFlags.size,
            dominantPhase      = state.sessionPhase
        )

        aria.onSessionEnd(summary)
        haven.onSessionEnd(summary)
        sentinel.onSessionEnd(summary)

        _sessionState.value = null
    }

    // ── Routing logic ─────────────────────────────────────────────────────────

    private fun route(state: SessionState): LuminaAgent = when {
        state.sessionPhase == SessionPhase.CRISIS       -> haven // HAVEN leads all crisis responses
        !state.emotionalState.learningReady             -> haven
        state.activeAgent == AgentType.ARIA             -> aria
        else                                            -> haven
    }

    // ── State evolution ───────────────────────────────────────────────────────

    private fun applyResponse(response: AgentResponse, input: UserInput) {
        val current = _sessionState.value ?: return

        val interaction = Interaction(
            agentType         = response.agentType,
            userInput         = when (input) {
                is UserInput.Text            -> input.content
                is UserInput.VoiceTranscript -> input.transcript
                else                         -> null
            },
            agentResponse     = response.content,
            emotionalSnapshot = response.updatedEmotionalState ?: current.emotionalState
        )

        val next = current.copy(
            emotionalState  = response.updatedEmotionalState ?: current.emotionalState,
            sessionHistory  = current.sessionHistory + interaction,
            activeAgent     = response.handoffTo ?: current.activeAgent,
            sessionPhase    = response.sessionPhaseUpdate ?: current.sessionPhase
        )

        _sessionState.value = next
        _lastResponse.value = response

        // Propagate to all agents
        aria.onStateChange(next)
        haven.onStateChange(next)
        sentinel.onStateChange(next)
    }
}
