package ai.or4cl3.lumina.core.offline

import ai.or4cl3.lumina.core.offline.dao.*
import ai.or4cl3.lumina.core.offline.entities.*
import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * LUMINA local Room database.
 *
 * All data is stored on-device only. No cloud sync occurs without
 * explicit caregiver consent via the NGO sync layer.
 *
 * Schema version history:
 *   v1 — Initial schema (child_profiles, sessions, interactions, safety_flags)
 */
@Database(
    entities = [
        ChildProfileEntity::class,
        SessionEntity::class,
        InteractionEntity::class,
        SafetyFlagEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LuminaDatabase : RoomDatabase() {
    abstract fun childProfileDao(): ChildProfileDao
    abstract fun sessionDao(): SessionDao
    abstract fun interactionDao(): InteractionDao
    abstract fun safetyFlagDao(): SafetyFlagDao

    companion object {
        const val DATABASE_NAME = "lumina_v1.db"
    }
}
