package com.marlow.systems.registration.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class EmailSendingModel (
    val id: Int? = null,
    val userId: Int,
    val fromSystem: String,
    val senderEmail: String,
    val receiverEmail: String,
    val status: String,
    val subject: String? = null,
    val body: String,

    @Contextual val requestedAt: LocalDate,
    @Contextual val verifiedAt: LocalDate? = null
) {
    init {
        require(fromSystem.isNotBlank()) {"System cannot be blank"}
        require(senderEmail.isNotBlank()) {"Sender Email cannot be bank"}
        require(receiverEmail.isNotBlank()) {"Receiver Email cannot be blank"}
        require(status.isNotBlank()) {"Status cannot be blank"}
        require(body.isNotBlank()) {"Body cannot be blank"}
    }

    fun sanitized(): EmailSendingModel = this.copy (
        status        = status.sanitizeStatus(),
        senderEmail   = senderEmail.sanitizeEmail(),
        receiverEmail = receiverEmail.sanitizeEmail()
    )

    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        if (fromSystem.isBlank()) errors.add("System is required.")
        if (senderEmail.isBlank()) errors.add("Sender Email is required")
        if (receiverEmail.isBlank()) errors.add("Receiver Email is required")
        if (status.isBlank()) errors.add("Status is required")
        if (body.isBlank()) errors.add("Body is required")

        return errors
    }
}