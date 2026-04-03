package ai.or4cl3.lumina

import ai.or4cl3.lumina.core.offline.LuminaRepository
import ai.or4cl3.lumina.ui.screens.onboarding.OnboardingScreen
import ai.or4cl3.lumina.ui.screens.session.SessionScreen
import ai.or4cl3.lumina.ui.theme.LuminaTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * LUMINA MainActivity
 *
 * Single-activity architecture with Compose Navigation.
 *
 * Route logic:
 *   - If a child profile exists  → go directly to session
 *   - If no profile exists       → go through onboarding first
 *
 * No login. No account. Just LUMINA.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var repository: LuminaRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check for existing profile (fast local DB read)
        val hasProfile = runBlocking { repository.getMostRecentProfile() != null }

        setContent {
            LuminaTheme {
                LuminaNavGraph(startWithOnboarding = !hasProfile)
            }
        }
    }
}

// ─── Navigation graph ─────────────────────────────────────────────────────────

private object Routes {
    const val ONBOARDING = "onboarding"
    const val SESSION    = "session"
}

@Composable
private fun LuminaNavGraph(startWithOnboarding: Boolean) {
    val navController = rememberNavController()
    val start = if (startWithOnboarding) Routes.ONBOARDING else Routes.SESSION

    NavHost(navController = navController, startDestination = start) {

        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onComplete = { profile ->
                    // Profile is saved inside SessionViewModel.initialise()
                    navController.navigate(Routes.SESSION) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.SESSION) {
            SessionScreen(
                onSessionEnd = {
                    // Return to session home (re-enter session for same child)
                    navController.navigate(Routes.SESSION) {
                        popUpTo(Routes.SESSION) { inclusive = true }
                    }
                }
            )
        }
    }
}
