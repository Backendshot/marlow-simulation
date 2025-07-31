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
    return this.trim().lowercase()
}

fun String.sanitizeRole(): String {
    val sanitized    = this.trim().uppercase()
    val allowedRoles = setOf("USER", "ADMIN")

    require(sanitized in allowedRoles) {"Invalid role: $sanitized. Only 'USER' and 'ADMIN' are allowed."}

    return sanitized
}