package com.marlow.registrationSystem.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegistrationRequest(
    val username: String,
    val firstName: String,
    val middleName: String? = null,
    val lastName: String? = null,
    val email: String,
    val password: String,
    val birthday: String? = null
)
