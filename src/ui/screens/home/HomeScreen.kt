package ai.or4cl3.lumina.ui.screens.home

import ai.or4cl3.lumina.ui.home.HomeUiState
import ai.or4cl3.lumina.ui.home.HomeViewModel
import ai.or4cl3.lumina.ui.screens.onboarding.LuminaPrimaryButton
import ai.or4cl3.lumina.ui.theme.*
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * HomeScreen
 *
 * The screen a returning child lands on.
 *
 * Design intent:
 *  - Warm, personal greeting using nickname
 *  - Growth story — narrative celebration of progress (NO scores or grades)
 *  - Streak visualisation — simple, warm (stars not numbers)
 *  - Two clear actions: Learn (ARIA) or Talk (HAVEN)
 *  - Never anxiety-inducing. Never shame-based.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onStartSession: () -> Unit,
    onTalkToHaven: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LuminaTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color    = LuminaColors.WarmBackground
        ) {
            AnimatedContent(targetState = uiState, label = "home_state") { state ->
                when (state) {
                    is HomeUiState.Loading   -> LoadingView()
                    is HomeUiState.NoProfile -> NoProfileView(onStartSession)
                    is HomeUiState.Ready     -> ReadyView(
                        state          = state,
                        onStartSession = onStartSession,
                        onTalkToHaven  = onTalkToHaven
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadyView(
    state: HomeUiState.Ready,
    onStartSession: () -> Unit,
    onTalkToHaven: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Greeting ───────────────────────────────────────────────────
        Text("🌟", fontSize = 56.sp)
        Spacer(Modifier.height(12.dp))
        val name = state.profile.nickname?.let { "Welcome back, $it!" } ?: "Welcome back!"
        Text(
            text      = name,
            style     = MaterialTheme.typography.headlineMedium,
            color     = LuminaColors.TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text      = "LUMINA is ready when you are.",
            style     = MaterialTheme.typography.bodyMedium,
            color     = LuminaColors.TextSecondary
        )

        Spacer(Modifier.height(32.dp))

        // ── Streak card ────────────────────────────────────────────────
        if (state.streakDays > 0) {
            StreakCard(days = state.streakDays)
            Spacer(Modifier.height(16.dp))
        }

        // ── Growth notes ────────────────────────────────────────────────
        if (state.growthNotes.isNotEmpty()) {
            GrowthStoryCard(notes = state.growthNotes)
            Spacer(Modifier.height(32.dp))
        }

        // ── Actions ────────────────────────────────────────────────────
        LuminaPrimaryButton(
            text    = "🌟 Start learning",
            onClick = onStartSession
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick  = onTalkToHaven,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape    = RoundedCornerShape(16.dp)
        ) {
            Text("💙 Just talk", style = MaterialTheme.typography.labelLarge)
        }
    }
}

// ── Streak card ────────────────────────────────────────────────────────────

@Composable
private fun StreakCard(days: Int) {
    val stars = "🌟".repeat(minOf(days, 7)) // Max 7 stars shown
    Surface(
        shape          = RoundedCornerShape(20.dp),
        color          = LuminaColors.AriaAmberLight,
        modifier       = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier            = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stars, fontSize = 24.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text  = if (days == 1) "Learning today!" else "$days days in a row!",
                style = MaterialTheme.typography.titleMedium,
                color = LuminaColors.AriaAmberDark
            )
            Text(
                text  = "Keep showing up. That's what matters.",
                style = MaterialTheme.typography.bodySmall,
                color = LuminaColors.TextSecondary
            )
        }
    }
}

// ── Growth story card ───────────────────────────────────────────────────────

@Composable
private fun GrowthStoryCard(notes: List<String>) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = LuminaColors.HavenLavenderLight,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Your story so far 📚",
                style = MaterialTheme.typography.titleMedium,
                color = LuminaColors.HavenLavenderDark
            )
            Spacer(Modifier.height(8.dp))
            notes.forEach { note ->
                Text(
                    text     = "• $note",
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = LuminaColors.TextPrimary,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

// ── Supporting views ────────────────────────────────────────────────────────

@Composable
private fun LoadingView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = LuminaColors.HavenLavender)
    }
}

@Composable
private fun NoProfileView(onStart: () -> Unit) {
    Box(Modifier.fillMaxSize().background(LuminaColors.WarmBackground), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("🌟", fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text("Let's get started!", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(32.dp))
            LuminaPrimaryButton(text = "Begin", onClick = onStart)
        }
    }
}
