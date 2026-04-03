package ai.or4cl3.lumina.agents

import ai.or4cl3.lumina.agents.sentinel.SentinelAgent
import ai.or4cl3.lumina.core.model.GemmaInferenceEngine
import ai.or4cl3.lumina.core.session.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SentinelAgent.
 *
 * SENTINEL's correctness is non-negotiable — false negatives could
 * leave a child in danger. These tests validate the pre-screening
 * pipeline and classification routing.
 *
 * NOTE: The Gemma 4 model is mocked. Production integration tests
 * with real model output will be added in sprint-3.
 */
class SentinelAgentTest {

    private lateinit var engine: GemmaInferenceEngine
    private lateinit var sentinel: SentinelAgent

    private val testProfile = ChildProfile(
        id = "test-child-01",
        ageGroup = AgeGroup.GROWERS,
        languageCode = "en"
    )

    @Before
    fun setUp() {
        engine = mockk(relaxed = true)
        // Default: Gemma 4 returns NONE (clear)
        coEvery { engine.generateSafety(any()) } returns
            """{"level":"NONE","category":null}"""
        sentinel = SentinelAgent(engine)
    }

    private fun baseState() = SessionState(
        childProfile = testProfile,
        emotionalState = EmotionalStateVector.NEUTRAL
    )

    // ─── Keyword pre-screen: CRITICAL ─────────────────────────────────────────

    @Test
    fun `CRITICAL keyword triggers flag with CRITICAL protocol level`() = runTest {
        coEvery { engine.generateSafety(any()) } returns
            """{"level":"CRITICAL","category":"SELF_HARM_IDEATION"}"""

        val flag = sentinel.monitor(
            UserInput.Text("I want to kill myself"),
            baseState()
        )
        assertNotNull(flag)
        assertEquals(ProtocolLevel.CRITICAL, flag!!.protocolLevel)
        assertEquals(SafetyCategory.SELF_HARM_IDEATION, flag.category)
    }

    @Test
    fun `abuse disclosure triggers CRITICAL flag`() = runTest {
        coEvery { engine.generateSafety(any()) } returns
            """{"level":"CRITICAL","category":"ABUSE_DISCLOSURE"}"""

        val flag = sentinel.monitor(
            UserInput.Text("Someone touches me in a secret way"),
            baseState()
        )
        assertNotNull(flag)
        assertEquals(ProtocolLevel.CRITICAL, flag!!.protocolLevel)
    }

    // ─── Keyword pre-screen: HIGH ─────────────────────────────────────────────

    @Test
    fun `high-risk keyword triggers HIGH protocol level`() = runTest {
        coEvery { engine.generateSafety(any()) } returns
            """{"level":"HIGH","category":"EXPLOITATION_INDICATORS"}"""

        val flag = sentinel.monitor(
            UserInput.Text("He told me don't tell your parents"),
            baseState()
        )
        assertNotNull(flag)
        assertEquals(ProtocolLevel.HIGH, flag!!.protocolLevel)
    }

    // ─── Pre-screen gate: no model call for safe input ────────────────────────

    @Test
    fun `safe everyday input does not trigger model call`() = runTest {
        val flag = sentinel.monitor(
            UserInput.Text("I like drawing pictures of animals"),
            baseState()
        )
        assertNull(flag)
        coVerify(exactly = 0) { engine.generateSafety(any()) }
    }

    @Test
    fun `blank input returns null immediately`() = runTest {
        val flag = sentinel.monitor(UserInput.Text(""), baseState())
        assertNull(flag)
        coVerify(exactly = 0) { engine.generateSafety(any()) }
    }

    @Test
    fun `very short input returns null immediately`() = runTest {
        val flag = sentinel.monitor(UserInput.Text("ok"), baseState())
        assertNull(flag)
    }

    // ─── Non-text inputs ──────────────────────────────────────────────────────

    @Test
    fun `SessionStart input returns null`() = runTest {
        val flag = sentinel.monitor(UserInput.SessionStart, baseState())
        assertNull(flag)
    }

    @Test
    fun `Silence input returns null`() = runTest {
        val flag = sentinel.monitor(UserInput.Silence, baseState())
        assertNull(flag)
    }

    // ─── Model parse resilience ───────────────────────────────────────────────

    @Test
    fun `malformed model JSON returns null gracefully`() = runTest {
        coEvery { engine.generateSafety(any()) } returns "this is not json at all"

        // Pre-screen will trigger (keyword present), model returns garbage
        val flag = sentinel.monitor(
            UserInput.Text("I want to kill myself"),
            baseState()
        )
        // Should return null rather than crash
        assertNull(flag)
    }
}
