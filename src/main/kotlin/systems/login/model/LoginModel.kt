package com.marlow.systems.login.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginModel(
    val user_id: Int,
    val username: String,
    val jwt_token: String? = null,
    val active_session: String? = null,
    val active_session_deleted: Boolean? = null
)

@Serializable
data class UserProfileImage(
    val userImg: String
)

@Serializable
data class ImageUploadResponse(
    val success: Boolean,
    val message: String,
    val filename: String
)

@Serializable
data class AuditModel(
    val id: Int,
    val user_id: Int,
    val timestamp: String,
    val browser: String,
)

@Serializable
data class LoginRequest(
    val user_id: Int? = null, 
    val username: String, 
    val password: String
)

@Serializable
data class LogoutRequest(
    val user_id: Int
)

class Validator {
    fun <T> validateLoginInput(data: T): List<String> {
        val errors = mutableListOf<String>()
        if (data is LoginModel) {
            if (data.username.isBlank()) {
                errors.add("Username cannot be empty")
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