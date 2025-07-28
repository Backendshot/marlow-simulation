package com.marlow.global

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class GlobalResponse(
    val code: Int,
    val status: Boolean,
    val message: String,
    val data: JsonElement? = null
)
