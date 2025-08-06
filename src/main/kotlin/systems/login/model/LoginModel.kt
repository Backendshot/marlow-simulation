package com.marlow.systems.login.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginModel(
    val userId: Int,
    val username: String,
    val password: String,
    val jwtToken: String? = null,
    val activeSession: String? = null,
    val activeSessionDeleted: Boolean? = null,
)

@Serializable
data class AuditModel(
    val id: Int,
    val userId: Int,
    val timestamp: String,
    val browser: String,
)

@Serializable
data class LoginRequest(
    val userId: Int? = null, 
    val username: String, 
    val password: String
)

@Serializable
data class LogoutRequest(
    val userId: Int
)

class Validator {
    fun <T> validateLoginInput(data: T): List<String> {
        val errors = mutableListOf<String>()
        if (data is LoginModel) {
            if (data.username.isBlank()) {
                errors.add("Username cannot be empty")
            }
            if (data.password.isBlank()) {
                errors.add("Password cannot be empty")
            }
        }
        return errors
    }

    inline fun <reified T> sanitizeInput(data: T): T {
        return when (data) {
            is LoginModel -> data.copy(username = data.username.trim()) as T
            is LoginRequest -> data.copy(username = data.username.trim()) as T
            else -> data
        }
    }
}