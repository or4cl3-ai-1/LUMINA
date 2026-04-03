package ai.or4cl3.lumina.core.model

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * GemmaInferenceEngine
 *
 * Single-instance wrapper around Gemma 4 on-device inference.
 * Shared across ARIA, HAVEN, and SENTINEL to keep only one copy
 * of model weights resident in device RAM.
 *
 * Primary model : Gemma 4 E4B  INT4 (~2 GB RAM, $75+ devices)
 * Fallback model: Gemma 4 E2B  INT4 (~1 GB RAM, $50  devices)
 *
 * Integration target: MediaPipe LLM Inference API
 * Fallback target   : llama.cpp GGUF via JNI bridge
 */
class GemmaInferenceEngine(
    private val context: Context,
    val config: GemmaConfig = GemmaConfig()
) {
    private var isInitialized = false
    private var usingFallback = false

    // ── Configuration ────────────────────────────────────────────────────────

    data class GemmaConfig(
        val primaryModelPath: String  = "/data/local/tmp/gemma4_e4b_int4.bin",
        val fallbackModelPath: String = "/data/local/tmp/gemma4_e2b_int4.bin",
        val maxNewTokens: Int         = 512,
        val temperature: Float        = 0.7f,
        val topK: Int                 = 40,
        val topP: Float               = 0.95f,
        val maxContextChars: Int      = 12_000  // ~4K tokens — conservative for low-end RAM
    )

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        // MediaPipe LLM Inference API initialisation
        // In production: replace stub with real MediaPipe builder
        runCatching {
            initMediaPipe(config.primaryModelPath)
            isInitialized = true
            usingFallback = false
        }.recoverCatching {
            // Primary failed — try E2B fallback
            initMediaPipe(config.fallbackModelPath)
            isInitialized = true
            usingFallback = true
        }
    }

    private fun initMediaPipe(modelPath: String) {
        // TODO: real MediaPipe LlmInference.createFromOptions(context, options)
        // Stubbed for compilation — real impl in sprint-2
    }

    fun release() {
        isInitialized = false
    }

    val isReady: Boolean get() = isInitialized
    val runningOnFallback: Boolean get() = usingFallback

    // ── Generation ────────────────────────────────────────────────────────────

    /**
     * Streaming generation for interactive ARIA / HAVEN turns.
     * Emits partial token strings as they are produced by Gemma 4.
     */
    fun generate(
        systemPrompt: String,
        history: List<PromptMessage>,
        userInput: String
    ): Flow<String> = flow {
        require(isInitialized) { "GemmaInferenceEngine not initialised" }

        val prompt = buildGemmaPrompt(systemPrompt, history, userInput)

        withContext(Dispatchers.Default) {
            // TODO: wire to MediaPipe streaming callback
            // Stub emits placeholder — replaced in sprint-2 with real inference
            emit("[LUMINA] Inference engine ready — model response will stream here.")
        }
    }

    /**
     * Synchronous, low-token generation for SENTINEL safety classification.
     * Returns a JSON string: {"level":"NONE|MONITOR|HIGH|CRITICAL","category":"..."}
     */
    suspend fun generateSafety(inputText: String): String =
        withContext(Dispatchers.Default) {
            require(isInitialized) { "GemmaInferenceEngine not initialised" }
            val prompt = buildSafetyPrompt(inputText)
            // TODO: wire to MediaPipe synchronous call
            // Stub — returns NONE so SENTINEL stays silent until real model lands
            """{"level":"NONE","category":null}"""
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
        append("You are a child safeguarding classifier. Analyse the text for safety concerns.\n")
        append("Reply with JSON only — no prose: {\"level\": \"NONE|MONITOR|HIGH|CRITICAL\", \"category\": \"category_or_null\"}\n")
        append("<end_of_turn>\n")
        append("<start_of_turn>user\nText: $text<end_of_turn>\n")
        append("<start_of_turn>model\n")
    }

    /** Trim history to fit within context window (approximate char count). */
    private fun trimToContext(history: List<PromptMessage>): List<PromptMessage> {
        var chars = 0
        return history.reversed().takeWhile {
            chars += it.content.length
            chars < config.maxContextChars
        }.reversed()
    }
}

// ─────────────────────────────────────────────
// PromptMessage  (history entry)
// ─────────────────────────────────────────────

data class PromptMessage(
    val content: String,
    val isUser: Boolean
)
