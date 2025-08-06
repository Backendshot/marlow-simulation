package com.marlow.systems.registration.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class InformationModel(
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
    val roleType: String
) {
    init {
        require(username.isNotBlank()) { "Username must not be blank" }
        require(firstName.isNotBlank()) { "First name must not be blank" }
        require(email.isNotBlank()) { "Email must not be blank" }
        require(roleType.isNotBlank()) { "Role type must not be blank" }
    }
    fun sanitized(): InformationModel = this.copy(
        username   = username.trim().sanitizeInput(),
        firstName  = firstName.trim().sanitizeInput(),
        middleName = middleName?.trim()?.sanitizeInput(),
        lastName   = lastName?.trim()?.sanitizeInput(),
        email      = email.trim().sanitizeEmail(),
        image      = image?.trim()?.sanitizeInput(),
        roleType   = roleType.trim().sanitizeRole()
    )
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        val nameRegexPattern = "^[a-zA-Z\\s'-]+$"
        if (username.isBlank()) errors.add("Username is required.")
        if (!username.matches(Regex("^[a-zA-Z0-9._-]{3,}$"))) {
            errors.add("Username must be at least 3 characters and contain only letters, numbers, dots, hyphens, or underscores.")
        }
        if (firstName.isBlank()) {
            errors.add("First Name is required.")
        } else if (!firstName.matches(Regex(nameRegexPattern))) {
            errors.add("First Name must only contain letters, spaces, hyphens, or apostrophes.")
        }
        middleName?.matches(Regex(nameRegexPattern))?.let {
            if (!it) {
                errors.add("Middle Name must only contain letters, spaces, hyphens, or apostrophes.")
            }
        }
        lastName?.matches(Regex(nameRegexPattern))?.let {
            if (!it) {
                errors.add("Last Name must only contain letters, spaces, hyphens, or apostrophes.")
            }
        }
        if (email.isBlank()) errors.add("Email is required.")
        if (roleType.isBlank()) errors.add("Role is required.")
        return errors
    }
}