package ai.or4cl3.lumina.agents

import ai.or4cl3.lumina.agents.aria.AriaAgent
import ai.or4cl3.lumina.core.model.GemmaInferenceEngine
import ai.or4cl3.lumina.core.session.*
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for AriaAgent.
 *
 * ARIA's most critical behaviour: it must NEVER activate when
 * HAVEN signals the child is not emotionally ready.
 */
class AriaAgentTest {

    private lateinit var engine: GemmaInferenceEngine
    private lateinit var aria: AriaAgent

    private val testProfile = ChildProfile(
        id           = "test-child-01",
        ageGroup     = AgeGroup.SPROUTS,
        languageCode = "en"
    )

    @Before
    fun setUp() {
        engine = mockk(relaxed = true)
        every { engine.generate(any(), any(), any()) } returns flowOf("Great question! Let's explore that together.")
        aria = AriaAgent(engine)
    }

    // ─── Emotional gate ───────────────────────────────────────────────────────

    @Test
    fun `ARIA yields to HAVEN when child is not emotionally ready`() = runTest {
        val distressedState = baseState().copy(
            emotionalState = EmotionalStateVector.DISTRESSED
        )
        val response = aria.process(UserInput.Text("Hello"), distressedState)

        assertEquals(AgentType.HAVEN, response.handoffTo)
        assertEquals(SessionPhase.SUPPORT, response.sessionPhaseUpdate)
        assertTrue(response.content.isEmpty())
        verify(exactly = 0) { engine.generate(any(), any(), any()) }
    }

    @Test
    fun `ARIA processes input when child is emotionally ready`() = runTest {
        val readyState = baseState().copy(
            emotionalState = EmotionalStateVector.SAFE_READY
        )
        val response = aria.process(UserInput.Text("What is 2 + 2?"), readyState)

        assertEquals(AgentType.ARIA, response.agentType)
        assertEquals(SessionPhase.LEARNING, response.sessionPhaseUpdate)
        assertFalse(response.content.isEmpty())
    }

    // ─── System prompt ────────────────────────────────────────────────────────

    @Test
    fun `system prompt includes language code`() {
        val state = baseState().copy(
            childProfile = testProfile.copy(languageCode = "sw") // Swahili
        )
        val prompt = aria.buildSystemPrompt(state)
        assertTrue(prompt.contains("sw"))
    }

    @Test
    fun `system prompt adapts tone for distressed child`() {
        val state = baseState().copy(
            emotionalState = EmotionalStateVector(
                valence = -0.3f, arousal = 0.5f, engagement = 0.3f, learningReady = true
            )
        )
        val prompt = aria.buildSystemPrompt(state)
        assertTrue(prompt.contains("gentle") || prompt.contains("quiet"))
    }

    @Test
    fun `system prompt adapts tone for happy child`() {
        val state = baseState().copy(
            emotionalState = EmotionalStateVector(
                valence = 0.9f, arousal = 0.5f, engagement = 0.9f, learningReady = true
            )
        )
        val prompt = aria.buildSystemPrompt(state)
        assertTrue(prompt.contains("enthusiasm") || prompt.contains("spirits"))
    }

    // ─── Input normalisation ──────────────────────────────────────────────────

    @Test
    fun `ARIA handles Silence input gracefully`() = runTest {
        val state = baseState().copy(emotionalState = EmotionalStateVector.SAFE_READY)
        val response = aria.process(UserInput.Silence, state)
        // Should not crash and should attempt a gentle response
        assertNotNull(response)
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun baseState() = SessionState(
        childProfile   = testProfile,
        emotionalState = EmotionalStateVector.NEUTRAL,
        activeAgent    = AgentType.ARIA
    )
}
