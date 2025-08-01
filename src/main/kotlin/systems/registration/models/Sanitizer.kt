package com.marlow.systems.registration.models

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
    require(cleaned.contains("@") && cleaned.contains(".")) { "Invalid email format." }

    val emailRegex = Regex("^[\\w._%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")

    require(emailRegex.matches(cleaned)) { "Invalid email format." }
    return cleaned
}

fun String.sanitizeRole(): String {
    val sanitized    = this.trim().uppercase()
    val allowedRoles = setOf("USER", "ADMIN")

    require(sanitized in allowedRoles) { "Invalid role: $sanitized. Only 'USER' and 'ADMIN' are allowed." }

    return sanitized
}

fun String.sanitizeStatus(): String {
    val sanitized       = this.trim().uppercase()
    val allowedStatuses = setOf("PENDING", "SENT", "VERIFIED")

    require(sanitized in allowedStatuses) { "Invalid Status: $sanitized. Only 'PENDING' and 'VERIFIED' are allowed." }

    return sanitized
}