package com.vaishali.aichat.data

import com.vaishali.aichat.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GeminiRepository {

    private val model = GenerativeModel(
        modelName = "gemini-3-flash-preview",
        apiKey = BuildConfig.API_KEY
    )

    // Single response
    suspend fun sendMessage(prompt: String): String {
        val response = model.generateContent(prompt)
        return response.text ?: "No response"
    }

    // Streaming response (word by word)
    fun sendMessageStream(prompt: String): Flow<String> {
        return model.generateContentStream(prompt)
            .map { it.text ?: "" }
    }
}
