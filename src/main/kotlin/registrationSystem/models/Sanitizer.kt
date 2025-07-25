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
    return this.trim().lowercase()
}

fun String.sanitizeRole(): String {
    val sanitized    = this.trim().uppercase()
    val allowedRoles = setOf("USER", "ADMIN")

    if (sanitized !in allowedRoles) {
        throw IllegalArgumentException("Invalid role: $sanitized. Only 'USER' and 'ADMIN' are allowed.")
    }

    return sanitized
}