package com.marlow.systems.registration.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegistrationRequest(
    val username: String,
    val firstName: String,
    val middleName: String? = null,
    val roleType: String,
    val lastName: String? = null,
    val email: String,
    val password: String,
    val birthday: String? = null,
    val image: String? = null
)
