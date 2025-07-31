package com.marlow.globals

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class GlobalResponse(
    val code: Int,
    val status: Boolean,
    val message: String,
    val data: JsonElement? = null,
    val errors: Map<String, List<String>>? = null
)
