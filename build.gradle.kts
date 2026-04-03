// LUMINA — Root build configuration
// Android app: offline-first AI companion for crisis-affected children
// Inference: Gemma 4 via MediaPipe LLM Inference API

plugins {
    alias(libs.plugins.android.application)    apply false
    alias(libs.plugins.kotlin.android)         apply false
    alias(libs.plugins.kotlin.compose)         apply false
    alias(libs.plugins.hilt.android)           apply false
    alias(libs.plugins.ksp)                    apply false
    alias(libs.plugins.kotlin.serialization)   apply false
}
