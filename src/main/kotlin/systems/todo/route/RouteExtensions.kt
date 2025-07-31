package com.marlow.systems.todo.route

import io.ktor.server.application.*
import io.ktor.server.plugins.*

// 1. Parameter extractor
fun ApplicationCall.requireIntParam(name: String): Int =
    parameters[name]?.toIntOrNull()
        ?: throw BadRequestException("Parameter `$name` is missing or not a number")