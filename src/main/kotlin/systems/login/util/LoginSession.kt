package com.marlow.systems.login.util

import java.security.SecureRandom
import java.util.Base64

object LoginSession {
    fun generatedSessionId(): String {
        val random = SecureRandom()
        val sessionId = ByteArray(16)
        random.nextBytes(sessionId)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(sessionId)
    }
}