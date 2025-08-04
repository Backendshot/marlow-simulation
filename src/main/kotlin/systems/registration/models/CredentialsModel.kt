package com.marlow.systems.registration.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class CredentialsModel(
    val id: Int? = null,
    val userId: Int,
    val username: String,
    val password: String,
    val jwtToken: String? = null,
    val activeSession: Boolean = true,
    val activeSessionDeleted: Boolean = false,
    @Contextual val createdAt: LocalDate,
    @Contextual val updatedAt: LocalDate
) {
    init {
        require(username.isNotBlank()) { "Username cannot be blank" }
        require(password.isNotBlank()) { "Password cannot be blank" }
    }
    fun sanitized(): CredentialsModel = this.copy(
        username = username.sanitizeInput(),
        password = password,
        jwtToken = jwtToken?.sanitizeInput()
    )
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        if (username.isBlank()) errors.add("Username is required.")
        if (password.length < 8) {
            errors.add("Password must be at least 8 characters long.")
        } else {
            if (!password.matches(Regex(".*[A-Z].*"))) {
                errors.add("Password must contain at least one uppercase letter.")
            }
            if (!password.matches(Regex(".*[a-z].*"))) {
                errors.add("Password must contain at least one lowercase letter.")
            }
            if (!password.matches(Regex(".*\\d.*"))) {
                errors.add("Password must contain at least one number.")
            }
            if (!password.matches(Regex(".*[!@#\$%^&*(),.?\":{}|<>].*"))) {
                errors.add("Password must contain at least one special character.")
            }
        }
        if (!username.matches(Regex("^[a-zA-Z0-9._-]{3,}$"))) {
            errors.add("Username must be at least 3 characters and contain only letters, numbers, dots, hyphens, or underscores.")
        }
        return errors
    }
}