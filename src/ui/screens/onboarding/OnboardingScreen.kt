package ai.or4cl3.lumina.ui.screens.onboarding

import ai.or4cl3.lumina.core.session.AgeGroup
import ai.or4cl3.lumina.core.session.ChildProfile
import ai.or4cl3.lumina.ui.theme.*
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import java.util.UUID

/**
 * OnboardingScreen
 *
 * First-run experience. Three steps:
 *   1. Welcome — LUMINA introduces itself
 *   2. Language — child (or helper) picks their language
 *   3. Age group + nickname — personalisation
 *
 * Design principles:
 *  - No account creation, no email, no passwords
 *  - Large touch targets (≥48dp)
 *  - Icon-led — minimal literacy required to navigate
 *  - Warm, safe visual tone from the first screen
 */
@Composable
fun OnboardingScreen(
    onComplete: (ChildProfile) -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var selectedLanguage by remember { mutableStateOf("en") }
    var selectedAgeGroup by remember { mutableStateOf(AgeGroup.SPROUTS) }
    var nickname by remember { mutableStateOf("") }

    LuminaTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color    = LuminaColors.WarmBackground
        ) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    slideInHorizontally { it } + fadeIn() togetherWith
                    slideOutHorizontally { -it } + fadeOut()
                },
                label = "onboarding_step"
            ) { currentStep ->
                when (currentStep) {
                    0 -> WelcomeStep(
                        onNext = { step = 1 }
                    )
                    1 -> LanguageStep(
                        selected = selectedLanguage,
                        onSelect = { selectedLanguage = it },
                        onNext   = { step = 2 }
                    )
                    2 -> ProfileStep(
                        ageGroup      = selectedAgeGroup,
                        nickname      = nickname,
                        onAgeChange   = { selectedAgeGroup = it },
                        onNameChange  = { nickname = it },
                        onComplete    = {
                            onComplete(
                                ChildProfile(
                                    id           = UUID.randomUUID().toString(),
                                    nickname     = nickname.ifBlank { null },
                                    ageGroup     = selectedAgeGroup,
                                    languageCode = selectedLanguage
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

// ── Step 1: Welcome ────────────────────────────────────────────────────

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🌟", fontSize = 72.sp)

        Spacer(Modifier.height(24.dp))

        Text(
            text      = "LUMINA",
            style     = MaterialTheme.typography.headlineMedium,
            color     = LuminaColors.TextPrimary
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text      = "Your learning companion",
            style     = MaterialTheme.typography.bodyLarge,
            color     = LuminaColors.TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text      = "Works without internet • Your privacy is safe",
            style     = MaterialTheme.typography.bodySmall,
            color     = LuminaColors.TextHint,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(48.dp))

        LuminaPrimaryButton(text = "Let's begin →", onClick = onNext)
    }
}

// ── Step 2: Language ───────────────────────────────────────────────────

private val TOP_LANGUAGES = listOf(
    Pair("en", "English"),      Pair("ar", "العربية"),
    Pair("fr", "Français"),      Pair("sw", "Kiswahili"),
    Pair("uk", "Українська"),    Pair("bn", "বাংলা"),
    Pair("ha", "Hausa"),         Pair("ps", "پښتو"),
    Pair("am", "አማርኛ"),       Pair("es", "Español"),
    Pair("so", "Soomaali"),      Pair("my", "မြန်မာ"),
)

@Composable
private fun LanguageStep(
    selected: String,
    onSelect: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Text(
            "Choose your language",
            style = MaterialTheme.typography.titleLarge,
            color = LuminaColors.TextPrimary
        )

        Spacer(Modifier.height(24.dp))

        LazyLanguageGrid(
            languages = TOP_LANGUAGES,
            selected  = selected,
            onSelect  = onSelect
        )

        Spacer(Modifier.weight(1f))

        LuminaPrimaryButton(text = "Continue →", onClick = onNext)
    }
}

@Composable
private fun LazyLanguageGrid(
    languages: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        languages.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (code, label) ->
                    val isSelected = code == selected
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(code) },
                        color  = if (isSelected) LuminaColors.HavenLavenderLight else LuminaColors.CardSurface,
                        border = if (isSelected) BorderStroke(2.dp, LuminaColors.HavenLavender) else null,
                        shape  = RoundedCornerShape(12.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text  = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) LuminaColors.HavenLavenderDark else LuminaColors.TextPrimary
                            )
                        }
                    }
                }
                if (row.size < 3) repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// ── Step 3: Profile ───────────────────────────────────────────────────

@Composable
private fun ProfileStep(
    ageGroup: AgeGroup,
    nickname: String,
    onAgeChange: (AgeGroup) -> Unit,
    onNameChange: (String) -> Unit,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Text(
            "A little about you",
            style = MaterialTheme.typography.titleLarge,
            color = LuminaColors.TextPrimary
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "You don't need to use your real name.",
            style = MaterialTheme.typography.bodySmall,
            color = LuminaColors.TextHint
        )

        Spacer(Modifier.height(28.dp))

        Text("How old are you?", style = MaterialTheme.typography.titleMedium)

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AgeGroup.entries.forEach { group ->
                AgeGroupChip(
                    group    = group,
                    selected = group == ageGroup,
                    onClick  = { onAgeChange(group) }
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Text("What should we call you? (optional)", style = MaterialTheme.typography.titleMedium)

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value         = nickname,
            onValueChange = { if (it.length <= 20) onNameChange(it) },
            placeholder   = { Text("A name or a star ★") },
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(16.dp)
        )

        Spacer(Modifier.weight(1f))

        LuminaPrimaryButton(text = "Meet LUMINA 🌟", onClick = onComplete)
    }
}

@Composable
private fun AgeGroupChip(group: AgeGroup, selected: Boolean, onClick: () -> Unit) {
    val emoji = when (group) {
        AgeGroup.SEEDLINGS -> "🌱" // 5-7
        AgeGroup.SPROUTS   -> "🌿" // 8-10
        AgeGroup.GROWERS   -> "🌾" // 11-13
        AgeGroup.BRIDGES   -> "🌈" // 14-17
    }
    Surface(
        modifier = Modifier
            .weight(1f).height(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color  = if (selected) LuminaColors.AriaAmberLight else LuminaColors.CardSurface,
        border = if (selected) BorderStroke(2.dp, LuminaColors.AriaAmber) else null,
        shape  = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier            = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 20.sp)
            Text(
                text  = "${group.minAge}–${group.maxAge}",
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) LuminaColors.AriaAmberDark else LuminaColors.TextSecondary
            )
        }
    }
}

// ── Shared components ───────────────────────────────────────────────────────

@Composable
fun LuminaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick  = onClick,
        modifier = modifier.fillMaxWidth().height(56.dp),
        enabled  = enabled,
        shape    = RoundedCornerShape(16.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor = LuminaColors.NeutralTeal,
            contentColor   = Color.White
        )
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
