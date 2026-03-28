package com.sallyli.services

import com.sallyli.config.InterviewPersona
import com.sallyli.models.*
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

class GeminiLiveClient(private val apiKey: String) {
    private val client = HttpClient {
        install(WebSockets)
    }

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun startSession(
        inputChannel: Channel<Frame>,
        outputChannel: Channel<Frame>
    ) {
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BiDiGenerateContent?key=$apiKey"

        client.webSocket(url) {
            // 1. Send Setup Message
            val setupMessage = GeminiSetupMessage(
                setup = SetupConfig(
                    model = "models/gemini-2.0-flash-exp",
                    generationConfig = GenerationConfig(
                        responseModalities = listOf("AUDIO")
                    ),
                    systemInstruction = SystemInstruction(
                        parts = listOf(TextPart(InterviewPersona.prompt))
                    )
                )
            )
            send(Frame.Text(json.encodeToString(setupMessage)))

            // 2. Handle Bidirectional Streaming
            val incomingJob = launch {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        try {
                            val serverMessage = json.decodeFromString<ServerContentMessage>(text)
                            val content = serverMessage.serverContent
                            if (content != null) {
                                // Forward audio and text to the client
                                val parts = content.modelTurn?.parts ?: emptyList()
                                for (part in parts) {
                                    if (part.inlineData != null) {
                                        val audioBytes = Base64.getDecoder().decode(part.inlineData.data)
                                        outputChannel.send(Frame.Binary(true, audioBytes))
                                    }
                                    if (part.text != null) {
                                        outputChannel.send(Frame.Text(part.text))
                                    }
                                }
                                if (content.interrupted == true) {
                                    outputChannel.send(Frame.Text("INTERRUPTED"))
                                }
                            }
                        } catch (e: Exception) {
                            println("Error decoding server message: ${e.message}")
                        }
                    }
                }
            }

            val outgoingJob = launch {
                for (frame in inputChannel) {
                    when (frame) {
                        is Frame.Binary -> {
                            val base64Data = Base64.getEncoder().encodeToString(frame.readBytes())
                            val inputMessage = RealtimeInputMessage(
                                realtimeInput = RealtimeInput(
                                    mediaChunks = listOf(
                                        MediaChunk(
                                            mimeType = "audio/pcm;rate=16000",
                                            data = base64Data
                                        )
                                    )
                                )
                            )
                            send(Frame.Text(json.encodeToString(inputMessage)))
                        }
                        is Frame.Text -> {
                            // Forward text input if needed (e.g., chat)
                        }
                        else -> {}
                    }
                }
            }

            joinAll(incomingJob, outgoingJob)
        }
    }
}
