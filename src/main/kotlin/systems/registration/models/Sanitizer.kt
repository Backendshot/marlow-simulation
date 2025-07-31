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

    //Originally, the condition was if (!cleaned.contains("@") || !cleaned.contains(".")) to throw an IllegalArgumentException.
    //However, SonarQube recommends using require() instead, which throws an IllegalArgumentException with a given string if the arguments evaluate to false.
    //Shifting the code from an if condition to require() necessitated the inverse of (!cleaned.contains("@") || !cleaned.contains("."))
    //So, by De Morgan's rule, the inverse of the above expression is (cleaned.contains("@") && cleaned.contains("."))
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