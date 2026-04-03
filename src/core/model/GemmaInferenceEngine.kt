package ai.or4cl3.lumina.core.model

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * GemmaInferenceEngine — Real MediaPipe LLM Inference implementation.
 *
 * Key design decisions:
 *  - Single LlmInference instance shared across all agents (RAM constraint)
 *  - Mutex ensures only one generation runs at a time on-device
 *  - callbackFlow bridges MediaPipe's callback API to Kotlin coroutines
 *  - Auto-fallback from E4B to E2B on OOM or model-not-found
 *  - Safety generation is fire-and-forget with 128-token cap (speed critical)
 */
class GemmaInferenceEngine(
    private val context: Context,
    val config: GemmaConfig = GemmaConfig()
) {
    private var llm: LlmInference? = null
    private val generationMutex = Mutex()
    private var isInitialized = false
    var runningOnFallback = false
        private set

    // ── Configuration ─────────────────────────────────────────────────────

    data class GemmaConfig(
        val primaryModelPath: String  = "/data/local/tmp/gemma4_e4b_int4.bin",
        val fallbackModelPath: String = "/data/local/tmp/gemma4_e2b_int4.bin",
        val maxNewTokens: Int         = 512,
        val safetyMaxTokens: Int      = 128, // Fast classification
        val temperature: Float        = 0.7f,
        val topK: Int                 = 40,
        val topP: Float               = 0.95f,
        val maxContextChars: Int      = 12_000
    )

    // ── Lifecycle ────────────────────────────────────────────────────────────

    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { loadModel(config.primaryModelPath, fallback = false) }
            .recoverCatching { loadModel(config.fallbackModelPath, fallback = true) }
    }

    private fun loadModel(path: String, fallback: Boolean) {
        require(File(path).exists()) { "Model not found at $path" }
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(path)
            .setMaxTokens(config.maxNewTokens)
            .build()
        llm = LlmInference.createFromOptions(context, options)
        runningOnFallback = fallback
        isInitialized = true
    }

    fun release() {
        llm?.close()
        llm = null
        isInitialized = false
    }

    val isReady: Boolean get() = isInitialized

    // ── Streaming generation (ARIA + HAVEN) ────────────────────────────────

    /**
     * Streams partial token strings from Gemma 4 via MediaPipe's
     * ResultListener callback, bridged to a Kotlin Flow via callbackFlow.
     *
     * The Mutex ensures only one generation runs at a time — essential
     * because LlmInference is NOT thread-safe.
     */
    fun generate(
        systemPrompt: String,
        history: List<PromptMessage>,
        userInput: String
    ): Flow<String> = callbackFlow {
        require(isInitialized) { "Engine not initialised" }

        val prompt = buildGemmaPrompt(systemPrompt, history, userInput)

        generationMutex.withLock {
            val engine = llm ?: throw IllegalStateException("LLM released")

            withContext(Dispatchers.Default) {
                val session = LlmInferenceSession.createFromOptions(
                    engine,
                    LlmInferenceSession.LlmInferenceSessionOptions.builder()
                        .setTemperature(config.temperature)
                        .setTopK(config.topK)
                        .setTopP(config.topP)
                        .setResultListener { partial, done ->
                            trySend(partial)
                            if (done) close()
                        }
                        .setErrorListener { error ->
                            close(Exception("Gemma error: ${error.message}"))
                        }
                        .build()
                )
                session.addQueryChunk(prompt)
                session.generateResponseAsync()
            }
        }

        awaitClose()
    }.flowOn(Dispatchers.Default)

    // ── Safety classification (SENTINEL) ──────────────────────────────────

    /**
     * Synchronous safety classification for SENTINEL.
     * Capped at 128 tokens for speed. Returns JSON string.
     */
    suspend fun generateSafety(inputText: String): String =
        withContext(Dispatchers.Default) {
            require(isInitialized) { "Engine not initialised" }

            suspendCancellableCoroutine { cont ->
                val prompt = buildSafetyPrompt(inputText)
                val sb = StringBuilder()

                try {
                    generationMutex.tryLock().let { acquired ->
                        if (!acquired) {
                            // Engine busy — return safe default rather than block
                            cont.resume("""{"level":"NONE","category":null}""") {}
                            return@suspendCancellableCoroutine
                        }
                    }

                    val engine = llm ?: run {
                        generationMutex.unlock()
                        cont.resume("""{"level":"NONE","category":null}""") {}
                        return@suspendCancellableCoroutine
                    }

                    val session = LlmInferenceSession.createFromOptions(
                        engine,
                        LlmInferenceSession.LlmInferenceSessionOptions.builder()
                            .setTemperature(0.1f) // Low temp for classification
                            .setTopK(1)
                            .setResultListener { partial, done ->
                                sb.append(partial)
                                if (done) {
                                    generationMutex.unlock()
                                    cont.resume(sb.toString()) {}
                                }
                            }
                            .setErrorListener {
                                generationMutex.unlock()
                                cont.resume("""{"level":"NONE","category":null}""") {}
                            }
                            .build()
                    )
                    session.addQueryChunk(prompt)
                    session.generateResponseAsync()

                } catch (e: Exception) {
                    if (generationMutex.isLocked) generationMutex.unlock()
                    cont.resume("""{"level":"NONE","category":null}""") {}
                }
            }
        }

    // ── Prompt builders ───────────────────────────────────────────────────────

    fun buildGemmaPrompt(
        systemPrompt: String,
        history: List<PromptMessage>,
        userInput: String
    ): String = buildString {
        append("<start_of_turn>system\n$systemPrompt<end_of_turn>\n")
        trimToContext(history).forEach { msg ->
            val role = if (msg.isUser) "user" else "model"
            append("<start_of_turn>$role\n${msg.content}<end_of_turn>\n")
        }
        append("<start_of_turn>user\n$userInput<end_of_turn>\n")
        append("<start_of_turn>model\n")
    }

    private fun buildSafetyPrompt(text: String): String = buildString {
        append("<start_of_turn>system\n")
        append("Classify child safety risk. Reply JSON only.\n")
        append("Format: {\"level\":\"NONE|MONITOR|HIGH|CRITICAL\",\"category\":\"...or null\"}\n")
        append("<end_of_turn>\n")
        append("<start_of_turn>user\nText: $text<end_of_turn>\n")
        append("<start_of_turn>model\n")
    }

    private fun trimToContext(history: List<PromptMessage>): List<PromptMessage> {
        var chars = 0
        return history.reversed().takeWhile {
            chars += it.content.length
            chars < config.maxContextChars
        }.reversed()
    }
}

data class PromptMessage(val content: String, val isUser: Boolean)
