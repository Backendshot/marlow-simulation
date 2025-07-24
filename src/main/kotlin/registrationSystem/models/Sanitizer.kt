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