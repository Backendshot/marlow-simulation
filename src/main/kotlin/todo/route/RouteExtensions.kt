package com.marlow.todo.route

import com.marlow.global.GlobalResponse
import com.marlow.global.GlobalResponseData
import com.marlow.todo.model.Todo
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

// 1. Parameter extractor
fun ApplicationCall.requireIntParam(name: String): Int =
    parameters[name]?.toIntOrNull()
        ?: throw BadRequestException("Parameter `$name` is missing or not a number")