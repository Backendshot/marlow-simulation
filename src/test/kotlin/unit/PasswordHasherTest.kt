package com.marlow.unit

// src/test/kotlin/unit/PasswordHasherTest.kt
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PasswordHasherTest {
    private val secretKey = "helloworld"
    private val hasher = PasswordHasher(secretKey)

    @Test
    fun `hashes and verifies correctly`() {
        val raw = "helloworld"
        val hashed = hasher.hash(raw)

        assertThat(hasher.verify(raw, hashed)).isTrue()
        assertThat(hasher.verify("wrong", hashed)).isFalse()
    }
}