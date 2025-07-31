package com.marlow.globals

import kotlinx.serialization.Serializable

@Serializable
data class GlobalResponseData<T>(
    val code: Int, val status: Boolean, val message: String, val data: T
)