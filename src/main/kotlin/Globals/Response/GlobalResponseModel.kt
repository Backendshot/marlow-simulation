package com.marlow.Globals.Response

import kotlinx.serialization.Serializable

@Serializable
data class GlobalResponseModel(
    val code: Int,
    val status: String,
    val message: String
)