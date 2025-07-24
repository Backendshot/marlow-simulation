package com.marlow.LoginSystem.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginModel(
    val id: String,
    val username: String,
    val password: String,
    val jwt_token: String? = null,
    val active_session: Boolean = false,
    val active_session_deleted: Boolean = true,
)


@Serializable
data class AuditModel(
    val id: String,
    val user_id: String,
    val timestamp: String,
    val browser: String,
)


