package com.marlow.globals

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.sql.Timestamp
import java.time.Instant

@Serializable
data class ErrorLog(
    val errorCode: Int,
    val errorMessage: String,
    @Serializable(with = TimestampIsoSerializer::class) val timestamp: Timestamp,
    val apiOccurred: String,
    val systemOccurred: String
) {
    companion object {
        const val ERROR_LOG_QUERY =
            "INSERT INTO error_logs (error_code, error_message, timestamp, api_occurred, system_occurred) VALUES (?, ?, ?, ?, ?)"
    }
}

object TimestampIsoSerializer : KSerializer<Timestamp> {
    override val descriptor = PrimitiveSerialDescriptor(
        "java.sql.Timestamp", PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: Timestamp) {
        // Use Instant to get ISO format
        encoder.encodeString(value.toInstant().toString())
    }

    override fun deserialize(decoder: Decoder): Timestamp {
        val iso = decoder.decodeString()
        return Timestamp.from(Instant.parse(iso))
    }
}