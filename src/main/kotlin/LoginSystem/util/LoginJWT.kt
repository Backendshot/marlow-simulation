package com.marlow.LoginSystem.util

import java.security.SecureRandom
import java.util.Base64
import io.github.cdimascio.dotenv.dotenv
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object LoginJWT {
    val dotenv   = dotenv()
    private val SECRET = dotenv["SECRET_TOKEN"]

    fun generateJWT(userId: Int): String {
        val header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}"
        val payload = "{\"userId\":$userId,\"iat\":${System.currentTimeMillis() / 1000}}"
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val encodedHeader = encoder.encodeToString(header.toByteArray())
        val encodedPayload = encoder.encodeToString(payload.toByteArray())
        val signature = hmacSha256("$encodedHeader.$encodedPayload", SECRET)
        val signatureEncoded = encoder.encodeToString(signature)
        return "$encodedHeader.$encodedPayload.$signatureEncoded"
    }

    fun verifyAndExtractUserId(token: String): Int {
        val parts = token.split(".")
        if (parts.size != 3) throw IllegalArgumentException("Invalid token format")

        val (header, payload, signature) = parts

        val expectedSig = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(hmacSha256("$header.$payload", SECRET))

        if (expectedSig != signature) {
            throw IllegalArgumentException("Invalid token signature")
        }

        val jsonPayload = String(Base64.getUrlDecoder().decode(payload))
        val userIdRegex = Regex("\"userId\"\\s*:\\s*(\\d+)")
        val match = userIdRegex.find(jsonPayload) ?: throw IllegalArgumentException("userId not found in token")
        return match.groupValues[1].toInt()
    }

    private fun hmacSha256(data: String, secret: String): ByteArray {
        val hmac = Mac.getInstance("HmacSHA256")
        val keySpec = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
        hmac.init(keySpec)
        return hmac.doFinal(data.toByteArray())
    }
}
