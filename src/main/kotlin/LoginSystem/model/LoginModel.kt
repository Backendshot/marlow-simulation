package com.marlow.LoginSystem.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class LoginModel(
    val id: Int,
    val username: String,
    val password: String,
    val jwt_token: String? = null,
    val active_session: String? = null,
    val active_session_deleted: Boolean? = null,
)


@Serializable
data class AuditModel(
    val id: Int,
    val user_id: String,
    val timestamp: String,
    val browser: String,
)


class Validator {
    fun <T> validateLoginInput(data: T): List<String>{
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

    fun sanitizeInput(data: Any): Any {
        return when (data) {
            is LoginModel -> data.copy(
                username = data.username.trim(),
                password = data.password.trim()
            )
            else -> data
        }
    }
}