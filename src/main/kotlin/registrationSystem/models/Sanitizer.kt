package com.marlow.registrationSystem.models

fun String.sanitizeInput(): String {
    return this
        .trim()
        .replace(Regex("<[^>]*>"), "")
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#x27;")
        .replace("/", "&#x2F;")
}

fun String.sanitizeEmail(): String {
    val cleaned = this.trim().lowercase()

    if (!cleaned.contains("@") || !cleaned.contains(".")) {
        throw IllegalArgumentException("Invalid email format.")
    }

    val emailRegex = Regex("^[\\w._%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")
    if (!emailRegex.matches(cleaned)) {
        throw IllegalArgumentException("Invalid email format.")
    }

    return cleaned
}

fun String.sanitizeRole(): String {
    val sanitized    = this.trim().uppercase()
    val allowedRoles = setOf("USER", "ADMIN")

    if (sanitized !in allowedRoles) {
        throw IllegalArgumentException("Invalid role: $sanitized. Only 'USER' and 'ADMIN' are allowed.")
    }

    return sanitized
}

fun String.sanitizeStatus(): String {
    val sanitized       = this.trim().uppercase()
    val allowedStatuses = setOf("PENDING", "SENT", "VERIFIED")

    if (sanitized !in allowedStatuses) {
        throw IllegalArgumentException("Invalid Status: $sanitized. Only 'PENDING' and 'VERIFIED' are allowed.")
    }

    return sanitized
}