package ai.or4cl3.lumina.di

import ai.or4cl3.lumina.agents.aria.AriaAgent
import ai.or4cl3.lumina.agents.haven.HavenAgent
import ai.or4cl3.lumina.agents.sentinel.SentinelAgent
import ai.or4cl3.lumina.core.model.GemmaInferenceEngine
import ai.or4cl3.lumina.core.offline.*
import ai.or4cl3.lumina.core.offline.dao.*
import ai.or4cl3.lumina.core.session.AgentOrchestrator
import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DI module — wires the entire LUMINA dependency graph.
 *
 * Dependency order:
 *   Context
 *     └─ LuminaDatabase ─────────────────────────── DAOs ──── LuminaRepository
 *     └─ GemmaInferenceEngine ─────── AriaAgent
 *                            └─────── HavenAgent
 *                            └─────── SentinelAgent
 *                                         └─ AgentOrchestrator
 */
@Module
@InstallIn(SingletonComponent::class)
object LuminaModule {

    // ── Database ──────────────────────────────────────────────────────────

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LuminaDatabase =
        Room.databaseBuilder(context, LuminaDatabase::class.java, LuminaDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides @Singleton
    fun provideChildProfileDao(db: LuminaDatabase): ChildProfileDao = db.childProfileDao()

    @Provides @Singleton
    fun provideSessionDao(db: LuminaDatabase): SessionDao = db.sessionDao()

    @Provides @Singleton
    fun provideInteractionDao(db: LuminaDatabase): InteractionDao = db.interactionDao()

    @Provides @Singleton
    fun provideSafetyFlagDao(db: LuminaDatabase): SafetyFlagDao = db.safetyFlagDao()

    // ── Inference engine ──────────────────────────────────────────────────

    @Provides @Singleton
    fun provideGemmaEngine(@ApplicationContext context: Context): GemmaInferenceEngine =
        GemmaInferenceEngine(context)

    // ── Agents ─────────────────────────────────────────────────────────────

    @Provides @Singleton
    fun provideAriaAgent(engine: GemmaInferenceEngine): AriaAgent = AriaAgent(engine)

    @Provides @Singleton
    fun provideHavenAgent(engine: GemmaInferenceEngine): HavenAgent = HavenAgent(engine)

    @Provides @Singleton
    fun provideSentinelAgent(engine: GemmaInferenceEngine): SentinelAgent = SentinelAgent(engine)

    // ── Orchestrator ────────────────────────────────────────────────────────

    @Provides @Singleton
    fun provideOrchestrator(
        aria: AriaAgent, haven: HavenAgent, sentinel: SentinelAgent
    ): AgentOrchestrator = AgentOrchestrator(aria, haven, sentinel)
}
