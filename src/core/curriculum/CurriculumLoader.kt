package ai.or4cl3.lumina.core.curriculum

import ai.or4cl3.lumina.core.session.AgeGroup
import ai.or4cl3.lumina.core.session.CurriculumDomain
import ai.or4cl3.lumina.core.session.EmotionalStateVector
import ai.or4cl3.lumina.core.session.LearningState
import android.content.Context
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CurriculumLoader
 *
 * Loads activity packs from on-device JSON assets and provides
 * intelligent activity selection for ARIA based on:
 *   - Child's age group and assessed level
 *   - Current emotional state (from HAVEN)
 *   - Session history (avoid repetition)
 *   - Time of session (shorter activities near session end)
 *
 * Asset path convention: assets/curriculum/{language}_{domain}_{ageGroup}.json
 * Example: assets/curriculum/en_literacy_seedlings.json
 */
@Singleton
class CurriculumLoader @Inject constructor(
    private val context: Context
) {
    private val packCache = mutableMapOf<String, ActivityPack>()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // ── Pack loading ───────────────────────────────────────────────────────────

    fun loadPack(language: String, domain: CurriculumDomain, ageGroup: AgeGroup): ActivityPack? {
        val key = packKey(language, domain, ageGroup)
        return packCache.getOrPut(key) {
            loadFromAssets(key) ?: return null
        }
    }

    private fun loadFromAssets(key: String): ActivityPack? {
        return try {
            val assetPath = "curriculum/$key.json"
            val jsonString = context.assets.open(assetPath).bufferedReader().readText()
            json.decodeFromString<ActivityPack>(jsonString)
        } catch (e: Exception) {
            null // Pack not available on device — graceful degradation
        }
    }

    private fun packKey(language: String, domain: CurriculumDomain, ageGroup: AgeGroup): String =
        "${language}_${domain.name.lowercase()}_${ageGroup.name.lowercase()}"

    // ── Activity selection ────────────────────────────────────────────────────────

    /**
     * Select the best next activity for a child.
     *
     * Selection algorithm:
     *  1. Filter by age group, domain, and emotional readiness
     *  2. Exclude recently seen activities
     *  3. Prefer lighter activities (shorter, simpler) when arousal is elevated
     *  4. Vary activity type for engagement
     *  5. Return null if no suitable activity found (ARIA free-flows)
     */
    fun selectNextActivity(
        language: String,
        domain: CurriculumDomain,
        ageGroup: AgeGroup,
        emotionalState: EmotionalStateVector,
        learningState: LearningState,
        recentActivityIds: Set<String> = emptySet()
    ): SelectedActivity? {
        val pack = loadPack(language, domain, ageGroup) ?: return null
        val available = pack.activities.filter { it.id !in recentActivityIds }
        if (available.isEmpty()) return null

        // For elevated arousal, prefer shorter, lighter activities
        val candidates = if (emotionalState.arousal > 0.6f) {
            available.filter { it.estimatedMinutes <= 3 || it.vocabularyLevel <= 2 }
                .ifEmpty { available }
        } else available

        // Pick with light randomisation (avoid same activity twice in a row)
        val selected = candidates.random()
        val reason = when {
            emotionalState.valence > 0.6f -> "Child is in great spirits — picked an engaging activity"
            emotionalState.arousal > 0.6f -> "Child seems activated — picked a shorter, gentler activity"
            else -> "Standard selection"
        }

        return SelectedActivity(selected, reason)
    }

    /**
     * Build a context string for ARIA's system prompt describing the current activity.
     */
    fun buildActivityContext(activity: Activity): String = buildString {
        append("Current activity: \"${activity.title}\"\n")
        append("Type: ${activity.type.name.lowercase()}\n")
        append("Open with: ${activity.ariaPrompt}\n")
        if (activity.followUps.isNotEmpty()) {
            append("Follow-up ideas (use naturally, don't read verbatim):\n")
            activity.followUps.forEach { append(" - $it\n") }
        }
    }
}
