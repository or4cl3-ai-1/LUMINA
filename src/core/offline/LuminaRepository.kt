package ai.or4cl3.lumina.core.offline

import ai.or4cl3.lumina.core.offline.dao.*
import ai.or4cl3.lumina.core.offline.entities.*
import ai.or4cl3.lumina.core.session.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LuminaRepository
 *
 * Single source of truth for all persisted LUMINA data.
 * Translates between domain models (SessionState, ChildProfile, etc.)
 * and Room entities. All writes are suspend functions; all reads
 * that benefit from live updates return Flow.
 */
@Singleton
class LuminaRepository @Inject constructor(
    private val profileDao: ChildProfileDao,
    private val sessionDao: SessionDao,
    private val interactionDao: InteractionDao,
    private val safetyFlagDao: SafetyFlagDao
) {
    // ── Child Profiles ──────────────────────────────────────────────────────

    fun observeProfiles(): Flow<List<ChildProfile>> =
        profileDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getMostRecentProfile(): ChildProfile? =
        profileDao.getMostRecent()?.toDomain()

    suspend fun getProfile(id: String): ChildProfile? =
        profileDao.getById(id)?.toDomain()

    suspend fun saveProfile(profile: ChildProfile) {
        profileDao.upsert(profile.toEntity())
    }

    suspend fun touchProfile(id: String) {
        profileDao.updateLastSeen(id, System.currentTimeMillis())
    }

    suspend fun updateLiteracyLevel(id: String, level: LiteracyLevel) {
        profileDao.updateLiteracyLevel(id, level.name)
    }

    /** Wipe all data for a child — right to deletion. */
    suspend fun deleteProfile(id: String) {
        profileDao.delete(id)
    }

    // ── Sessions ─────────────────────────────────────────────────────────────

    suspend fun openSession(state: SessionState) {
        sessionDao.insert(
            SessionEntity(
                id                 = state.sessionId,
                childId            = state.childProfile.id,
                startedAt          = state.sessionStartTime,
                endedAt            = null,
                durationMinutes    = 0,
                interactionCount   = 0,
                finalValence       = 0.5f,
                safetyFlagsRaised  = 0,
                milestonesJson     = "[]"
            )
        )
    }

    suspend fun closeSession(summary: SessionSummary) {
        sessionDao.finalise(
            id           = summary.sessionId,
            endedAt      = System.currentTimeMillis(),
            duration     = summary.durationMinutes,
            interactions = summary.interactionCount,
            valence      = summary.finalEmotionalState.valence,
            flags        = summary.safetyFlagsRaised,
            milestones   = Json.encodeToString(summary.learningMilestones)
        )
    }

    suspend fun appendInteraction(sessionId: String, interaction: Interaction) {
        interactionDao.insert(interaction.toEntity(sessionId))
    }

    suspend fun saveSafetyFlag(sessionId: String, childId: String, flag: SafetyFlag, encryptedContext: ByteArray) {
        safetyFlagDao.insert(
            SafetyFlagEntity(
                id               = flag.id,
                sessionId        = sessionId,
                childId          = childId,
                timestamp        = flag.timestamp,
                category         = flag.category.name,
                protocolLevel    = flag.protocolLevel.name,
                encryptedContext = encryptedContext,
                resolved         = false
            )
        )
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private fun ChildProfileEntity.toDomain() = ChildProfile(
        id                    = id,
        nickname              = nickname,
        ageGroup              = AgeGroup.valueOf(ageGroup),
        languageCode          = languageCode,
        assessedLiteracyLevel = LiteracyLevel.valueOf(literacyLevel),
        assessedNumeracyLevel = NumeracyLevel.valueOf(numeracyLevel),
        createdAt             = createdAt,
        lastSeenAt            = lastSeenAt
    )

    private fun ChildProfile.toEntity() = ChildProfileEntity(
        id            = id,
        nickname      = nickname,
        ageGroup      = ageGroup.name,
        languageCode  = languageCode,
        literacyLevel = assessedLiteracyLevel.name,
        numeracyLevel = assessedNumeracyLevel.name,
        createdAt     = createdAt,
        lastSeenAt    = lastSeenAt
    )

    private fun Interaction.toEntity(sessionId: String) = InteractionEntity(
        id               = id,
        sessionId        = sessionId,
        timestamp        = timestamp,
        agentType        = agentType.name,
        userInput        = userInput,
        agentResponse    = agentResponse,
        emotionalValence = emotionalSnapshot.valence,
        emotionalArousal = emotionalSnapshot.arousal
    )
}
