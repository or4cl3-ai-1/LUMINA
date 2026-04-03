package ai.or4cl3.lumina.core.offline.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/**
 * Room entities for LUMINA's local-only, privacy-absolute data layer.
 *
 * Design principles:
 *  - No real names stored — only child-chosen nicknames
 *  - No device identifiers
 *  - SafetyFlagEntity.encryptedContext stored as AES-256 ciphertext
 *  - All data stays on-device unless caregiver explicitly consents to sync
 */

// ─────────────────────────────────────────────
// Child Profile
// ─────────────────────────────────────────────

@Entity(tableName = "child_profiles")
data class ChildProfileEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "nickname")          val nickname: String?,
    @ColumnInfo(name = "age_group")         val ageGroup: String,          // AgeGroup.name
    @ColumnInfo(name = "language_code")     val languageCode: String,
    @ColumnInfo(name = "literacy_level")    val literacyLevel: String,     // LiteracyLevel.name
    @ColumnInfo(name = "numeracy_level")    val numeracyLevel: String,     // NumeracyLevel.name
    @ColumnInfo(name = "created_at")        val createdAt: Long,
    @ColumnInfo(name = "last_seen_at")      val lastSeenAt: Long
)

// ─────────────────────────────────────────────
// Session
// ─────────────────────────────────────────────

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "child_id")               val childId: String,
    @ColumnInfo(name = "started_at")             val startedAt: Long,
    @ColumnInfo(name = "ended_at")               val endedAt: Long?,
    @ColumnInfo(name = "duration_minutes")       val durationMinutes: Int,
    @ColumnInfo(name = "interaction_count")      val interactionCount: Int,
    @ColumnInfo(name = "final_valence")          val finalValence: Float,
    @ColumnInfo(name = "safety_flags_raised")    val safetyFlagsRaised: Int,
    @ColumnInfo(name = "milestones_json")        val milestonesJson: String  // JSON array of strings
)

// ─────────────────────────────────────────────
// Interaction  (individual turn within a session)
// ─────────────────────────────────────────────

@Entity(tableName = "interactions")
data class InteractionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "session_id")        val sessionId: String,
    @ColumnInfo(name = "timestamp")         val timestamp: Long,
    @ColumnInfo(name = "agent_type")        val agentType: String,          // AgentType.name
    @ColumnInfo(name = "user_input")        val userInput: String?,
    @ColumnInfo(name = "agent_response")    val agentResponse: String,
    @ColumnInfo(name = "emotional_valence") val emotionalValence: Float,
    @ColumnInfo(name = "emotional_arousal") val emotionalArousal: Float
)

// ─────────────────────────────────────────────
// Safety Flag  (AES-256 encrypted context)
// ─────────────────────────────────────────────

@Entity(tableName = "safety_flags")
data class SafetyFlagEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "session_id")          val sessionId: String,
    @ColumnInfo(name = "child_id")            val childId: String,
    @ColumnInfo(name = "timestamp")           val timestamp: Long,
    @ColumnInfo(name = "category")            val category: String,           // SafetyCategory.name
    @ColumnInfo(name = "protocol_level")      val protocolLevel: String,      // ProtocolLevel.name
    @ColumnInfo(name = "encrypted_context")   val encryptedContext: ByteArray, // AES-256 ciphertext
    @ColumnInfo(name = "resolved")            val resolved: Boolean
)
