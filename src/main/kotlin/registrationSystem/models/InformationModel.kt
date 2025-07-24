package com.marlow.registrationSystem.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class Information(
    val id: Int? = null,
    val username: String,
    val firstName: String,
    val middleName: String? = null,
    val lastName: String? = null,
    val email: String,
    val emailVerified: Boolean = false,

    @Contextual val emailVerifiedAt: LocalDate? = null,
    @Contextual val birthday: LocalDate? = null,
    @Contextual val createdAt: LocalDate,
    @Contextual val updatedAt: LocalDate,

    val image: String? = null,
    val roleType: String = "USER"
) {
    init {
        require(username.isNotBlank()) { "Username must not be blank" }
        require(firstName.isNotBlank()) { "First name must not be blank" }
        require(email.isNotBlank()) { "Email must not be blank" }
        require(roleType.isNotBlank()) { "Role type must not be blank" }
    }

    fun sanitized(): Information = this.copy(
        username   = username.trim().sanitizeInput(),
        firstName  = firstName.trim().sanitizeInput(),
        middleName = middleName?.trim()?.sanitizeInput(),
        lastName   = lastName?.trim()?.sanitizeInput(),
        email      = email.trim().sanitizeEmail(),
        image      = image?.trim()?.sanitizeInput(),
        roleType   = roleType.trim().uppercase()
    )
}