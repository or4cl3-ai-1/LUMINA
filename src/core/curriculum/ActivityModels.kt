package ai.or4cl3.lumina.core.curriculum

import ai.or4cl3.lumina.core.session.AgeGroup
import ai.or4cl3.lumina.core.session.CurriculumDomain
import kotlinx.serialization.Serializable

/**
 * Curriculum data models.
 *
 * Activities are the atomic unit of LUMINA's curriculum.
 * ARIA selects and sequences them based on the child's
 * assessed level, emotional state, and session history.
 *
 * All content is loaded from on-device JSON asset packs.
 * Packs are updated via opportunistic NGO sync.
 */

// ── Activity (atomic learning unit) ────────────────────────────────────────────

@Serializable
data class Activity(
    val id: String,
    val type: ActivityType,
    val domain: String,         // CurriculumDomain.name
    val ageGroup: String,       // AgeGroup.name
    val title: String,
    val ariaPrompt: String,     // ARIA's opening message to the child
    val followUps: List<String> = emptyList(), // Suggested follow-up prompts for ARIA
    val vocabularyLevel: Int    = 1,           // 1 (very simple) to 5 (complex)
    val estimatedMinutes: Int   = 5,
    val tags: List<String>      = emptyList()
)

@Serializable
enum class ActivityType {
    STORY,          // Narrative / story completion
    QUESTION,       // Open question / discussion
    GAME,           // Interactive learning game
    CREATIVE,       // Drawing prompt / imaginative play
    LIFE_SKILL,     // Practical real-world skill
    CELEBRATION     // Milestone celebration activity
}

// ── Activity Pack (collection of activities for a language + domain + age group) ─────

@Serializable
data class ActivityPack(
    val packId: String,
    val language: String,
    val domain: String,
    val ageGroup: String,
    val version: Int,
    val activities: List<Activity>
)

// ── Activity selection result ──────────────────────────────────────────────────

data class SelectedActivity(
    val activity: Activity,
    val contextNote: String = "" // Why ARIA chose this (internal, not shown to child)
)
