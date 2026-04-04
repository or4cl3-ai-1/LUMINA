package ai.or4cl3.lumina.core.offline.dao

import ai.or4cl3.lumina.core.offline.entities.*
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChildProfileDao {
    @Query("SELECT * FROM child_profiles ORDER BY last_seen_at DESC")
    fun observeAll(): Flow<List<ChildProfileEntity>>
    @Query("SELECT * FROM child_profiles WHERE id = :id")
    suspend fun getById(id: String): ChildProfileEntity?
    @Query("SELECT * FROM child_profiles ORDER BY last_seen_at DESC LIMIT 1")
    suspend fun getMostRecent(): ChildProfileEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: ChildProfileEntity)
    @Query("UPDATE child_profiles SET last_seen_at = :timestamp WHERE id = :id")
    suspend fun updateLastSeen(id: String, timestamp: Long)
    @Query("UPDATE child_profiles SET literacy_level = :level WHERE id = :id")
    suspend fun updateLiteracyLevel(id: String, level: String)
    @Query("DELETE FROM child_profiles WHERE id = :id")
    suspend fun delete(id: String)
    @Query("DELETE FROM child_profiles")
    suspend fun deleteAll()
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE child_id = :childId ORDER BY started_at DESC")
    fun observeForChild(childId: String): Flow<List<SessionEntity>>
    @Query("SELECT * FROM sessions WHERE child_id = :childId ORDER BY started_at DESC LIMIT :limit")
    suspend fun getRecentForChild(childId: String, limit: Int = 10): List<SessionEntity>
    @Query("SELECT COUNT(*) FROM sessions WHERE child_id = :childId")
    suspend fun countForChild(childId: String): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity)
    @Query("UPDATE sessions SET ended_at = :endedAt, duration_minutes = :duration, interaction_count = :interactions, final_valence = :valence, safety_flags_raised = :flags, milestones_json = :milestones WHERE id = :id")
    suspend fun finalise(id: String, endedAt: Long, duration: Int, interactions: Int, valence: Float, flags: Int, milestones: String)
}

@Dao
interface InteractionDao {
    @Query("SELECT * FROM interactions WHERE session_id = :sessionId ORDER BY timestamp ASC")
    suspend fun getForSession(sessionId: String): List<InteractionEntity>
    @Query("SELECT * FROM interactions WHERE session_id = :sessionId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentForSession(sessionId: String, limit: Int = 20): List<InteractionEntity>
    @Insert
    suspend fun insert(interaction: InteractionEntity)
    @Insert
    suspend fun insertAll(interactions: List<InteractionEntity>)
    @Query("DELETE FROM interactions WHERE session_id = :sessionId")
    suspend fun deleteForSession(sessionId: String)
}

@Dao
interface SafetyFlagDao {
    @Query("SELECT * FROM safety_flags WHERE child_id = :childId AND resolved = 0 ORDER BY timestamp DESC")
    fun observeUnresolvedForChild(childId: String): Flow<List<SafetyFlagEntity>>
    @Query("SELECT * FROM safety_flags WHERE session_id = :sessionId ORDER BY timestamp ASC")
    suspend fun getForSession(sessionId: String): List<SafetyFlagEntity>
    @Insert
    suspend fun insert(flag: SafetyFlagEntity)
    @Query("UPDATE safety_flags SET resolved = 1 WHERE id = :id")
    suspend fun markResolved(id: String)
    @Query("SELECT COUNT(*) FROM safety_flags WHERE child_id = :childId AND protocol_level = 'CRITICAL' AND resolved = 0")
    suspend fun countUnresolvedCriticalForChild(childId: String): Int
}
