package com.ssbmax.pdfparser.insights.gemma

data class GemmaEngineConfig(
    val modelPath: String = "",
    val maxTokens: Int = 512,
    val temperature: Float = 0.2f,
    val topK: Int = 40,
)
