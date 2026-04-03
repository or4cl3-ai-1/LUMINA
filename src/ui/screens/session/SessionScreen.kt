package ai.or4cl3.lumina.ui.screens.session

import ai.or4cl3.lumina.core.session.AgentType
import ai.or4cl3.lumina.ui.session.*
import ai.or4cl3.lumina.ui.theme.*
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * SessionScreen
 *
 * The main interaction screen — this is what a child sees.
 *
 * Visual design:
 *  - Ambient background tints to agent colour (HAVEN=lavender, ARIA=amber)
 *  - Agent name + emoji shown above bubble stream
 *  - No scores, no timers, no progress bars that could cause anxiety
 *  - Large send button, generous input field
 *  - Typing indicator while model is generating
 */
@Composable
fun SessionScreen(
    viewModel: SessionViewModel = hiltViewModel(),
    onSessionEnd: () -> Unit = {}
) {
    val uiState   by viewModel.uiState.collectAsState()
    val messages  by viewModel.messages.collectAsState()
    val agent     by viewModel.activeAgent.collectAsState()

    val ambientColor = remember(agent) {
        when (agent) {
            AgentType.HAVEN    -> LuminaColors.HavenLavenderLight
            AgentType.ARIA     -> LuminaColors.AriaAmberLight
            AgentType.SENTINEL -> LuminaColors.CardSurface
        }
    }

    LuminaTheme {
        AnimatedContent(
            targetState = uiState,
            label       = "session_state"
        ) { state ->
            when (state) {
                is SessionUiState.Initialising -> InitialisingView()
                is SessionUiState.Active       -> ActiveSessionView(
                    messages     = messages,
                    activeAgent  = agent,
                    ambientColor = ambientColor,
                    isThinking   = state.isThinking,
                    onSend       = viewModel::sendText,
                    onEnd        = {
                        viewModel.endSession()
                        onSessionEnd()
                    }
                )
                is SessionUiState.Ended        -> SessionEndedView(onSessionEnd)
                is SessionUiState.Error        -> ErrorView(state.message)
            }
        }
    }
}

// ── Active session ───────────────────────────────────────────────────────

@Composable
private fun ActiveSessionView(
    messages: List<ChatMessage>,
    activeAgent: AgentType,
    ambientColor: Color,
    isThinking: Boolean,
    onSend: (String) -> Unit,
    onEnd: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState  = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        containerColor = ambientColor,
        topBar         = { SessionTopBar(activeAgent = activeAgent, onEnd = onEnd) },
        bottomBar      = {
            MessageInputBar(
                value     = inputText,
                onChange  = { inputText = it },
                onSend    = {
                    onSend(inputText)
                    inputText = ""
                },
                enabled   = !isThinking
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                state           = listState,
                modifier        = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding  = PaddingValues(vertical = 12.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(message = message, activeAgent = activeAgent)
                }

                if (isThinking) {
                    item { ThinkingBubble(agentType = activeAgent) }
                }
            }
        }
    }
}

// ── Top bar ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionTopBar(activeAgent: AgentType, onEnd: () -> Unit) {
    val (emoji, name) = when (activeAgent) {
        AgentType.HAVEN    -> "💙" to "HAVEN"
        AgentType.ARIA     -> "🌟" to "ARIA"
        AgentType.SENTINEL -> "🛡️" to "LUMINA"
    }
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 22.sp)
                Spacer(Modifier.width(8.dp))
                Text(name, style = MaterialTheme.typography.titleMedium)
            }
        },
        actions = {
            TextButton(onClick = onEnd) {
                Text("End session", color = LuminaColors.TextSecondary)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

// ── Message bubbles ──────────────────────────────────────────────────────

@Composable
private fun MessageBubble(message: ChatMessage, activeAgent: AgentType) {
    val isUser = message.isUser
    Row(
        modifier            = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            AgentAvatar(agentType = message.agentType)
            Spacer(Modifier.width(8.dp))
        }
        Surface(
            shape = RoundedCornerShape(
                topStart    = if (isUser) 18.dp else 4.dp,
                topEnd      = if (isUser) 4.dp  else 18.dp,
                bottomStart = 18.dp,
                bottomEnd   = 18.dp
            ),
            color = if (isUser) LuminaColors.NeutralTeal else LuminaColors.SurfaceWhite,
            tonalElevation = if (isUser) 0.dp else 1.dp
        ) {
            Text(
                text     = message.text,
                style    = MaterialTheme.typography.bodyLarge,
                color    = if (isUser) Color.White else LuminaColors.TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
        if (isUser) Spacer(Modifier.width(8.dp))
    }
}

@Composable
private fun AgentAvatar(agentType: AgentType) {
    val (emoji, color) = when (agentType) {
        AgentType.HAVEN    -> "💙" to LuminaColors.HavenLavenderLight
        AgentType.ARIA     -> "🌟" to LuminaColors.AriaAmberLight
        AgentType.SENTINEL -> "🛡️" to LuminaColors.CardSurface
    }
    Box(
        modifier          = Modifier.size(36.dp).clip(CircleShape).background(color),
        contentAlignment  = Alignment.Center
    ) {
        Text(emoji, fontSize = 18.sp)
    }
}

@Composable
private fun ThinkingBubble(agentType: AgentType) {
    val emoji = if (agentType == AgentType.HAVEN) "💙" else "🌟"
    Row(verticalAlignment = Alignment.CenterVertically) {
        AgentAvatar(agentType)
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
            color = LuminaColors.SurfaceWhite,
            tonalElevation = 1.dp
        ) {
            Text(
                text     = "•••",
                style    = MaterialTheme.typography.bodyLarge,
                color    = LuminaColors.TextHint,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
            )
        }
    }
}

// ── Input bar ─────────────────────────────────────────────────────────────

@Composable
private fun MessageInputBar(
    value: String,
    onChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean
) {
    Surface(
        tonalElevation = 4.dp,
        color          = LuminaColors.SurfaceWhite
    ) {
        Row(
            modifier            = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value         = value,
                onValueChange = onChange,
                enabled       = enabled,
                modifier      = Modifier.weight(1f),
                placeholder   = { Text("Say something…") },
                shape         = RoundedCornerShape(24.dp),
                maxLines      = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (value.isNotBlank()) onSend() })
            )
            FilledIconButton(
                onClick   = onSend,
                enabled   = enabled && value.isNotBlank(),
                modifier  = Modifier.size(48.dp),
                colors    = IconButtonDefaults.filledIconButtonColors(
                    containerColor = LuminaColors.NeutralTeal
                )
            ) {
                Icon(
                    imageVector  = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint         = Color.White
                )
            }
        }
    }
}

// ── Supporting views ───────────────────────────────────────────────────────

@Composable
private fun InitialisingView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = LuminaColors.HavenLavender)
            Spacer(Modifier.height(16.dp))
            Text(
                "LUMINA is waking up…",
                style = MaterialTheme.typography.bodyLarge,
                color = LuminaColors.TextSecondary
            )
        }
    }
}

@Composable
private fun SessionEndedView(onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(LuminaColors.WarmBackground), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(32.dp)
        ) {
            Text("🌟", fontSize = 56.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "Great session today.",
                style     = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                color     = LuminaColors.TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "See you next time.",
                style = MaterialTheme.typography.bodyLarge,
                color = LuminaColors.TextSecondary
            )
            Spacer(Modifier.height(40.dp))
            OutlinedButton(onClick = onBack) { Text("Back to home") }
        }
    }
}

@Composable
private fun ErrorView(message: String) {
    Box(Modifier.fillMaxSize().background(LuminaColors.WarmBackground), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text("⚠️", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        }
    }
}
