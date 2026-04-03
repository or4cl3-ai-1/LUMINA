package ai.or4cl3.lumina.core.session

import java.util.UUID

// ─────────────────────────────────────────────
// Child Profile
// ─────────────────────────────────────────────

data class ChildProfile(
    val id: String = UUID.randomUUID().toString(),
    val nickname: String? = null, // Child-chosen only — never real name
    val ageGroup: AgeGroup,
    val languageCode: String,
    val assessedLiteracyLevel: LiteracyLevel = LiteracyLevel.UNKNOWN,
    val assessedNumeracyLevel: NumeracyLevel = NumeracyLevel.UNKNOWN,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSeenAt: Long = System.currentTimeMillis()
)

enum class AgeGroup(val minAge: Int, val maxAge: Int, val displayName: String) {
    SEEDLINGS(5, 7, "Seedlings"),
    SPROUTS(8, 10, "Sprouts"),
    GROWERS(11, 13, "Growers"),
    BRIDGES(14, 17, "Bridges")
}

enum class LiteracyLevel {
    UNKNOWN, PRE_LITERACY, EMERGENT, EARLY, DEVELOPING, FLUENT
}

enum class NumeracyLevel {
    UNKNOWN, PRE_NUMERACY, EMERGENT, EARLY, DEVELOPING, PROFICIENT
}

// ─────────────────────────────────────────────
// Emotional State Vector
// ─────────────────────────────────────────────

/**
 * HAVEN maintains this continuously throughout each session.
 * ARIA and SENTINEL consume it to modulate their behaviour.
 */
data class EmotionalStateVector(
    val valence: Float = 0.5f,       // -1.0 (distressed) → 1.0 (positive)
    val arousal: Float = 0.3f,       //  0.0 (calm)        → 1.0 (activated)
    val engagement: Float = 0.5f,   //  0.0 (withdrawn)   → 1.0 (engaged)
    val safetyFlag: Boolean = false, // true when SENTINEL criteria approaching
    val learningReady: Boolean = true // derived gate signal consumed by ARIA
) {
    companion object {
        val NEUTRAL      = EmotionalStateVector()
        val DISTRESSED   = EmotionalStateVector(valence = -0.7f, arousal = 0.8f,  engagement = 0.2f, learningReady = false)
        val SAFE_READY   = EmotionalStateVector(valence =  0.7f, arousal = 0.4f,  engagement = 0.8f, learningReady = true)
    }
}

// ─────────────────────────────────────────────
// Session State  (shared across all agents)
// ─────────────────────────────────────────────

data class SessionState(
    val sessionId: String = UUID.randomUUID().toString(),
    val childProfile: ChildProfile,
    val emotionalState: EmotionalStateVector = EmotionalStateVector.NEUTRAL,
    val learningState: LearningState = LearningState(),
    val sessionHistory: List<Interaction> = emptyList(),
    val safetyFlags: List<SafetyFlag> = emptyList(),
    val activeAgent: AgentType = AgentType.HAVEN, // HAVEN *always* opens
    val sessionStartTime: Long = System.currentTimeMillis(),
    val sessionPhase: SessionPhase = SessionPhase.CHECK_IN
)

data class LearningState(
    val currentDomain: CurriculumDomain? = null,
    val currentActivity: String? = null,
    val sessionsCompleted: Int = 0,
    val totalEngagementMinutes: Int = 0,
    val recentMilestones: List<String> = emptyList()
)

data class Interaction(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val agentType: AgentType,
    val userInput: String?,
    val agentResponse: String,
    val emotionalSnapshot: EmotionalStateVector
)

data class SafetyFlag(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val category: SafetyCategory,
    val protocolLevel: ProtocolLevel,
    val triggerContext: String, // AES-256 encrypted before storage
    val resolved: Boolean = false
)

data class SessionSummary(
    val sessionId: String,
    val durationMinutes: Int,
    val interactionCount: Int,
    val finalEmotionalState: EmotionalStateVector,
    val learningMilestones: List<String>,
    val safetyFlagsRaised: Int,
    val dominantPhase: SessionPhase
)

// ─────────────────────────────────────────────
// Enumerations
// ─────────────────────────────────────────────

enum class AgentType { ARIA, HAVEN, SENTINEL }

enum class SessionPhase {
    CHECK_IN,   // HAVEN establishes emotional safety
    LEARNING,   // ARIA active
    SUPPORT,    // HAVEN support mode
    DECOMPRESS, // Session wind-down
    CRISIS      // SENTINEL protocol active
}

enum class CurriculumDomain {
    LITERACY, NUMERACY, LIFE_SKILLS, CREATIVE_EXPRESSION
}

enum class SafetyCategory {
    SELF_HARM_IDEATION,
    ABUSE_DISCLOSURE,
    ACUTE_CRISIS,
    EXPLOITATION_INDICATORS,
    GENERAL_DISTRESS
}

enum class ProtocolLevel { MONITOR, HIGH, CRITICAL }
