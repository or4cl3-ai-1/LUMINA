package ai.or4cl3.lumina.ui.session

import ai.or4cl3.lumina.agents.UserInput
import ai.or4cl3.lumina.core.model.GemmaInferenceEngine
import ai.or4cl3.lumina.core.offline.LuminaRepository
import ai.or4cl3.lumina.core.session.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SessionViewModel
 *
 * Bridges the AgentOrchestrator to the Compose UI layer.
 * Owns session lifecycle: init → running → ended.
 * Persists all interactions and safety flags via LuminaRepository.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val orchestrator: AgentOrchestrator,
    private val repository: LuminaRepository,
    private val engine: GemmaInferenceEngine
) : ViewModel() {

    // ── UI state ────────────────────────────────────────────────────────────

    private val _uiState = MutableStateFlow<SessionUiState>(SessionUiState.Initialising)
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    val sessionState: StateFlow<SessionState?> = orchestrator.sessionState

    // Derived: messages visible in the chat UI
    val messages: StateFlow<List<ChatMessage>> = orchestrator.sessionState
        .filterNotNull()
        .map { state -> state.sessionHistory.map { it.toChatMessage() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Derived: which agent is currently active (drives ambient colour)
    val activeAgent: StateFlow<AgentType> = orchestrator.sessionState
        .filterNotNull()
        .map { it.activeAgent }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AgentType.HAVEN)

    // Latest agent response (drives streaming text)
    val latestResponse: StateFlow<String> = orchestrator.lastResponse
        .filterNotNull()
        .map { it.content }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    // ── Session lifecycle ───────────────────────────────────────────────────

    fun initialise(profile: ChildProfile) {
        viewModelScope.launch {
            _uiState.value = SessionUiState.Initialising

            // Boot inference engine
            val engineResult = engine.initialize()
            if (engineResult.isFailure) {
                _uiState.value = SessionUiState.Error("Could not load AI model. Please check available storage.")
                return@launch
            }

            // Save / touch profile
            repository.saveProfile(profile)

            // Start session
            orchestrator.startSession(profile)
            repository.openSession(orchestrator.sessionState.value!!)

            _uiState.value = SessionUiState.Active(
                profile    = profile,
                engineMode = if (engine.runningOnFallback) EngineMode.FALLBACK else EngineMode.PRIMARY
            )
        }
    }

    fun sendText(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _uiState.value = (_uiState.value as? SessionUiState.Active)?.copy(isThinking = true)
                ?: _uiState.value

            orchestrator.processInput(UserInput.Text(text))
            persistLatestInteraction()

            _uiState.value = (_uiState.value as? SessionUiState.Active)?.copy(isThinking = false)
                ?: _uiState.value
        }
    }

    fun endSession() {
        viewModelScope.launch {
            orchestrator.endSession()
            _uiState.value = SessionUiState.Ended
        }
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private suspend fun persistLatestInteraction() {
        val state = orchestrator.sessionState.value ?: return
        val latest = state.sessionHistory.lastOrNull() ?: return
        repository.appendInteraction(state.sessionId, latest)

        // Persist any new safety flags
        state.safetyFlags.lastOrNull()?.let { flag ->
            repository.saveSafetyFlag(
                sessionId        = state.sessionId,
                childId          = state.childProfile.id,
                flag             = flag,
                encryptedContext = flag.triggerContext.toByteArray() // TODO: Tink AES-256 encrypt
            )
        }
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private fun Interaction.toChatMessage() = ChatMessage(
        id        = id,
        isUser    = userInput != null,
        text      = if (userInput != null) userInput else agentResponse,
        agentType = agentType,
        timestamp = timestamp
    )

    override fun onCleared() {
        super.onCleared()
        engine.release()
    }
}

// ── UI state models ──────────────────────────────────────────────────────────

sealed class SessionUiState {
    object Initialising : SessionUiState()
    data class Active(
        val profile    : ChildProfile,
        val engineMode : EngineMode,
        val isThinking : Boolean = false
    ) : SessionUiState()
    object Ended : SessionUiState()
    data class Error(val message: String) : SessionUiState()
}

enum class EngineMode { PRIMARY, FALLBACK }

data class ChatMessage(
    val id        : String,
    val isUser    : Boolean,
    val text      : String,
    val agentType : AgentType,
    val timestamp : Long
)
