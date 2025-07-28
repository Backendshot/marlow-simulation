package com.marlow.LoginSystem.util

class LoginAudit {
    fun parseBrowser(userAgent: String): String {
        val ua = userAgent.lowercase()
        return when {
            ua.contains("edg") -> "Edge"
            ua.contains("opr") || ua.contains("opera") -> "Opera"
            ua.contains("chrome") && !ua.contains("edg") -> "Chrome"
            ua.contains("firefox") -> "Firefox"
            ua.contains("safari") && !ua.contains("chrome") -> "Safari"
            ua.contains("brave") -> "Brave"
            ua.contains("msie") || ua.contains("trident") -> "Internet Explorer"
            else -> "Unknown"
        }
    }
}





