package com.marlow.global

import kotlinx.serialization.Serializable

@Serializable
data class GlobalResponse(
    val code: Int, val status: Boolean, val message: String
)