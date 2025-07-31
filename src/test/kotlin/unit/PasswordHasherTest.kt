package com.marlow.unit

// src/test/kotlin/unit/PasswordHasherTest.kt
import org.assertj.core.api.Assertions.assertThat
//import org.junit.jupiter.api.Test
import com.marlow.configs.PasswordHasher
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class PasswordHasherTest {
    private val hasher = PasswordHasher()

    @Test
    fun `hashes and verifies correctly`() {
        val raw = "helloworld".toCharArray()
        val hashed = hasher.hash(raw)

        // must be non‐empty and start with the Argon2 prefix
        assertTrue(hashed.startsWith("\$argon2"), "Expected Argon2 hash format")

        // verify returns true for correct password
        assertTrue(hasher.verify(hashed, raw))

        // verify returns false for wrong password
        val wrong = "notSecret".toCharArray()
        assertFalse(hasher.verify(hashed, wrong))

        assertThat(hasher.verify(hashed, raw)).isTrue()
        assertThat(hasher.verify(hashed, "wrong".toCharArray())).isFalse()
    }
}