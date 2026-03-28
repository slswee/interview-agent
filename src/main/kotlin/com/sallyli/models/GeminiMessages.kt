package com.sallyli.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class GeminiSetupMessage(
    val setup: SetupConfig
)

@Serializable
data class SetupConfig(
    val model: String,
    @SerialName("generation_config")
    val generationConfig: GenerationConfig? = null,
    @SerialName("system_instruction")
    val systemInstruction: SystemInstruction? = null
)

@Serializable
data class GenerationConfig(
    @SerialName("response_modalities")
    val responseModalities: List<String>
)

@Serializable
data class SystemInstruction(
    val parts: List<TextPart>
)

@Serializable
data class TextPart(
    val text: String
)

@Serializable
data class RealtimeInputMessage(
    @SerialName("realtime_input")
    val realtimeInput: RealtimeInput
)

@Serializable
data class RealtimeInput(
    @SerialName("media_chunks")
    val mediaChunks: List<MediaChunk>
)

@Serializable
data class MediaChunk(
    @SerialName("mime_type")
    val mimeType: String,
    val data: String // Base64 encoded
)

@Serializable
data class ServerContentMessage(
    @SerialName("serverContent")
    val serverContent: ServerContent? = null,
    @SerialName("setupComplete")
    val setupComplete: JsonElement? = null
)

@Serializable
data class ServerContent(
    @SerialName("modelTurn")
    val modelTurn: ModelTurn? = null,
    val interrupted: Boolean? = null,
    val turnComplete: Boolean? = null
)

@Serializable
data class ModelTurn(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@Serializable
data class InlineData(
    val mimeType: String,
    val data: String // Base64 encoded
)
