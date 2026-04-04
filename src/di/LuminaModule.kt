package ai.or4cl3.lumina.di

import ai.or4cl3.lumina.agents.aria.AriaAgent
import ai.or4cl3.lumina.agents.haven.HavenAgent
import ai.or4cl3.lumina.agents.sentinel.SentinelAgent
import ai.or4cl3.lumina.core.curriculum.CurriculumLoader
import ai.or4cl3.lumina.core.model.GemmaInferenceEngine
import ai.or4cl3.lumina.core.offline.*
import ai.or4cl3.lumina.core.offline.dao.*
import ai.or4cl3.lumina.core.security.LuminaEncryption
import ai.or4cl3.lumina.core.session.AgentOrchestrator
import ai.or4cl3.lumina.core.sync.NgoSyncManager
import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LuminaModule {

    // ── Database ──────────────────────────────────────────────────────────
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): LuminaDatabase =
        Room.databaseBuilder(ctx, LuminaDatabase::class.java, LuminaDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration().build()

    @Provides @Singleton fun provideProfileDao(db: LuminaDatabase)     = db.childProfileDao()
    @Provides @Singleton fun provideSessionDao(db: LuminaDatabase)     = db.sessionDao()
    @Provides @Singleton fun provideInteractionDao(db: LuminaDatabase) = db.interactionDao()
    @Provides @Singleton fun provideSafetyFlagDao(db: LuminaDatabase)  = db.safetyFlagDao()

    // ── Security ──────────────────────────────────────────────────────────
    @Provides @Singleton
    fun provideEncryption(): LuminaEncryption = LuminaEncryption()

    // ── Inference engine ──────────────────────────────────────────────────
    @Provides @Singleton
    fun provideGemmaEngine(@ApplicationContext ctx: Context): GemmaInferenceEngine =
        GemmaInferenceEngine(ctx)

    // ── Curriculum ─────────────────────────────────────────────────────────
    @Provides @Singleton
    fun provideCurriculumLoader(@ApplicationContext ctx: Context): CurriculumLoader =
        CurriculumLoader(ctx)

    // ── Agents ─────────────────────────────────────────────────────────────
    @Provides @Singleton
    fun provideAriaAgent(
        engine: GemmaInferenceEngine,
        curriculum: CurriculumLoader
    ): AriaAgent = AriaAgent(engine, curriculum)

    @Provides @Singleton
    fun provideHavenAgent(engine: GemmaInferenceEngine): HavenAgent = HavenAgent(engine)

    @Provides @Singleton
    fun provideSentinelAgent(
        engine: GemmaInferenceEngine,
        encryption: LuminaEncryption
    ): SentinelAgent = SentinelAgent(engine, encryption)

    // ── Orchestrator ────────────────────────────────────────────────────────
    @Provides @Singleton
    fun provideOrchestrator(
        aria: AriaAgent, haven: HavenAgent, sentinel: SentinelAgent
    ): AgentOrchestrator = AgentOrchestrator(aria, haven, sentinel)

    // ── Sync ─────────────────────────────────────────────────────────────────
    @Provides @Singleton
    fun provideNgoSyncManager(@ApplicationContext ctx: Context): NgoSyncManager =
        NgoSyncManager(ctx)
}
