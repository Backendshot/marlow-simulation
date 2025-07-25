package com.marlow.LoginSystem.util

import eu.bitwalker.useragentutils.Browser
import eu.bitwalker.useragentutils.UserAgent


class LoginAudit {
   fun parseBrowser(userAgent: String): String = when {
        userAgent.contains("Edg", ignoreCase = true) -> "Edge"
        userAgent.contains("Chrome", ignoreCase = true) && !userAgent.contains("Edg", ignoreCase = true) -> "Chrome"
        userAgent.contains("Firefox", ignoreCase = true) -> "Firefox"
        userAgent.contains("Safari", ignoreCase = true) && !userAgent.contains("Chrome", ignoreCase = true) -> "Safari"
        userAgent.contains("Opera", ignoreCase = true) || userAgent.contains("OPR", ignoreCase = true) -> "Opera"
        else -> "Unknown"
    }
}





