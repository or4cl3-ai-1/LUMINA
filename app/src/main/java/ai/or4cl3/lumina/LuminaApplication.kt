package ai.or4cl3.lumina

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * LUMINA Application
 *
 * Hilt entry point. Minimal — all heavy lifting is in the DI module.
 * No analytics, no crash reporting SDK, no third-party data collection.
 */
@HiltAndroidApp
class LuminaApplication : Application()
