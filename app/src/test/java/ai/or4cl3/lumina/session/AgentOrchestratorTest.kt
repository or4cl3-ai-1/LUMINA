package ai.or4cl3.lumina.session

import ai.or4cl3.lumina.agents.*
import ai.or4cl3.lumina.agents.aria.AriaAgent
import ai.or4cl3.lumina.agents.haven.HavenAgent
import ai.or4cl3.lumina.agents.sentinel.SentinelAgent
import ai.or4cl3.lumina.core.model.GemmaInferenceEngine
import ai.or4cl3.lumina.core.session.*
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for AgentOrchestrator.
 *
 * Validates session lifecycle, routing logic, and
 * SENTINEL integration into the processing pipeline.
 */
class AgentOrchestratorTest {

    private lateinit var engine: GemmaInferenceEngine
    private lateinit var aria: AriaAgent
    private lateinit var haven: HavenAgent
    private lateinit var sentinel: SentinelAgent
    private lateinit var orchestrator: AgentOrchestrator

    private val testProfile = ChildProfile(
        id           = "test-child-01",
        ageGroup     = AgeGroup.SPROUTS,
        languageCode = "en"
    )

    @Before
    fun setUp() {
        engine    = mockk(relaxed = true)
        aria      = mockk(relaxed = true)
        haven     = mockk(relaxed = true)
        sentinel  = mockk(relaxed = true)

        every { engine.generate(any(), any(), any()) } returns flowOf("Test response")
        coEvery { engine.generateSafety(any()) } returns """{"level":"NONE","category":null}"""

        // HAVEN opens with a check-in response
        coEvery { haven.process(any(), any()) } returns AgentResponse(
            content             = "How are you feeling today?",
            agentType           = AgentType.HAVEN,
            updatedEmotionalState = EmotionalStateVector.SAFE_READY,
            sessionPhaseUpdate  = SessionPhase.CHECK_IN
        )

        // SENTINEL is clear by default
        coEvery { sentinel.monitor(any(), any()) } returns null

        orchestrator = AgentOrchestrator(aria, haven, sentinel)
    }

    // ─── Session start ────────────────────────────────────────────────────────

    @Test
    fun `startSession initialises state with HAVEN as active agent`() = runTest {
        orchestrator.startSession(testProfile)

        val state = orchestrator.sessionState.value
        assertNotNull(state)
        assertEquals(AgentType.HAVEN, state!!.activeAgent)
        assertEquals(testProfile.id, state.childProfile.id)
    }

    @Test
    fun `startSession calls HAVEN first`() = runTest {
        orchestrator.startSession(testProfile)
        coVerify(exactly = 1) { haven.process(UserInput.SessionStart, any()) }
        coVerify(exactly = 0) { aria.process(any(), any()) }
    }

    @Test
    fun `startSession notifies all agents`() = runTest {
        orchestrator.startSession(testProfile)
        verify(exactly = 1) { aria.onSessionStart(testProfile) }
        verify(exactly = 1) { haven.onSessionStart(testProfile) }
        verify(exactly = 1) { sentinel.onSessionStart(testProfile) }
    }

    // ─── SENTINEL integration ─────────────────────────────────────────────────

    @Test
    fun `SENTINEL monitors every input passively`() = runTest {
        orchestrator.startSession(testProfile)
        orchestrator.processInput(UserInput.Text("Hello there"))

        coVerify(atLeast = 1) { sentinel.monitor(UserInput.Text("Hello there"), any()) }
    }

    @Test
    fun `SENTINEL flag elevates session to CRISIS phase`() = runTest {
        val criticalFlag = SafetyFlag(
            category      = SafetyCategory.SELF_HARM_IDEATION,
            protocolLevel = ProtocolLevel.CRITICAL,
            triggerContext = "test trigger"
        )
        coEvery { sentinel.monitor(any(), any()) } returns criticalFlag
        coEvery { haven.process(any(), any()) } returns AgentResponse(
            content            = "I hear you. You are not alone.",
            agentType          = AgentType.HAVEN,
            sessionPhaseUpdate = SessionPhase.CRISIS
        )

        orchestrator.startSession(testProfile)
        orchestrator.processInput(UserInput.Text("I want to hurt myself"))

        val state = orchestrator.sessionState.value
        assertNotNull(state)
        assertEquals(1, state!!.safetyFlags.size)
        assertEquals(SessionPhase.CRISIS, state.sessionPhase)
    }

    // ─── Session end ──────────────────────────────────────────────────────────

    @Test
    fun `endSession clears state and notifies all agents`() = runTest {
        coEvery { haven.process(UserInput.SessionEnd, any()) } returns AgentResponse(
            content    = "Goodbye, see you soon!",
            agentType  = AgentType.HAVEN,
            sessionPhaseUpdate = SessionPhase.DECOMPRESS
        )

        orchestrator.startSession(testProfile)
        orchestrator.endSession()

        assertNull(orchestrator.sessionState.value)
        verify(exactly = 1) { aria.onSessionEnd(any()) }
        verify(exactly = 1) { haven.onSessionEnd(any()) }
        verify(exactly = 1) { sentinel.onSessionEnd(any()) }
    }
}
