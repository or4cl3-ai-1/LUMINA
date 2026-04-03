package ai.or4cl3.lumina.agents

import ai.or4cl3.lumina.agents.haven.HavenAgent
import ai.or4cl3.lumina.core.model.GemmaInferenceEngine
import ai.or4cl3.lumina.core.session.*
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for HavenAgent.
 *
 * HAVEN's most critical invariants:
 *  1. Every session opens with HAVEN
 *  2. Distress signals are detected and reflected in EmotionalStateVector
 *  3. Positive signals correctly set learningReady = true
 *  4. HAVEN hands off to ARIA only when child is ready AND check-in has occurred
 */
class HavenAgentTest {

    private lateinit var engine: GemmaInferenceEngine
    private lateinit var haven: HavenAgent

    private val testProfile = ChildProfile(
        id           = "test-child-01",
        ageGroup     = AgeGroup.SEEDLINGS,
        languageCode = "en"
    )

    @Before
    fun setUp() {
        engine = mockk(relaxed = true)
        every { engine.generate(any(), any(), any()) } returns
            flowOf("I hear you. How are you feeling today?")
        haven = HavenAgent(engine)
        haven.onSessionStart(testProfile)
    }

    // ─── Emotional state inference ────────────────────────────────────────────

    @Test
    fun `distress keywords lower valence and block learning`() = runTest {
        val state = baseState()
        val response = haven.process(
            UserInput.Text("I am scared and sad and I miss my home"),
            state
        )
        val emotion = response.updatedEmotionalState
        assertNotNull(emotion)
        assertTrue(emotion!!.valence < 0f)
        assertFalse(emotion.learningReady)
    }

    @Test
    fun `positive keywords raise valence and enable learning`() = runTest {
        val state = baseState()
        val response = haven.process(
            UserInput.Text("I feel happy and good today, I love learning!"),
            state
        )
        val emotion = response.updatedEmotionalState
        assertNotNull(emotion)
        assertTrue(emotion!!.valence > 0f)
        assertTrue(emotion.learningReady)
    }

    @Test
    fun `neutral input causes valence to decay toward neutral`() = runTest {
        // Start with slightly positive valence
        val state = baseState().copy(
            emotionalState = EmotionalStateVector(valence = 0.6f, arousal = 0.3f, engagement = 0.5f)
        )
        val response = haven.process(UserInput.Text("Okay."), state)
        val emotion = response.updatedEmotionalState
        assertNotNull(emotion)
        // Should decay toward neutral (0.5f * 0.9f = 0.45f)
        assertTrue(emotion!!.valence < 0.6f)
    }

    // ─── ARIA handoff logic ───────────────────────────────────────────────────

    @Test
    fun `HAVEN does NOT hand off to ARIA on first check-in`() = runTest {
        // Fresh session — checkInCount is 0
        val state = baseState().copy(sessionPhase = SessionPhase.CHECK_IN)
        val response = haven.process(
            UserInput.Text("I feel good."),
            state
        )
        // First check-in should NOT hand off even if ready
        assertNotEquals(AgentType.ARIA, response.handoffTo)
    }

    @Test
    fun `HAVEN hands off to ARIA after check-in when child is ready`() = runTest {
        val state = baseState().copy(
            sessionPhase   = SessionPhase.CHECK_IN,
            emotionalState = EmotionalStateVector.SAFE_READY
        )
        // First call increments checkInCount to 1
        haven.process(UserInput.Text("I feel okay."), state)
        // Second call — checkInCount is now 1, child is ready
        val response2 = haven.process(UserInput.Text("Yes I want to learn!"), state.copy(
            emotionalState = EmotionalStateVector.SAFE_READY
        ))
        assertEquals(AgentType.ARIA, response2.handoffTo)
    }

    // ─── Session reset ────────────────────────────────────────────────────────

    @Test
    fun `onSessionStart resets check-in counter`() = runTest {
        val state = baseState().copy(
            sessionPhase   = SessionPhase.CHECK_IN,
            emotionalState = EmotionalStateVector.SAFE_READY
        )
        // Run two interactions to increment counter
        haven.process(UserInput.Text("good"), state)
        haven.process(UserInput.Text("great"), state)
        // Reset
        haven.onSessionStart(testProfile)
        // After reset, first check-in should NOT hand off
        val response = haven.process(UserInput.Text("I feel good."), state)
        assertNotEquals(AgentType.ARIA, response.handoffTo)
    }

    private fun baseState() = SessionState(
        childProfile = testProfile,
        emotionalState = EmotionalStateVector.NEUTRAL,
        sessionPhase = SessionPhase.CHECK_IN
    )
}
