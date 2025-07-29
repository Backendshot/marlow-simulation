package com.marlow.plugin

import com.marlow.global.GlobalResponse
import io.ktor.server.application.Application
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.CannotTransformContentToTypeException
import io.ktor.server.plugins.statuspages.StatusPages
import kotlinx.serialization.SerializationException
import io.ktor.server.response.respond

fun Application.installGlobalErrorHandling() {
    install(StatusPages) {
        exception<SerializationException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest, GlobalResponse(400, false, "Invalid JSON: ${cause.localizedMessage}")
            )
        }
        exception<CannotTransformContentToTypeException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest, GlobalResponse(400, false, "Wrong input: ${cause.localizedMessage}")
            )
        }
        exception<NumberFormatException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                GlobalResponse(400, false, "Invalid number format: ${cause.localizedMessage}")
            )
        }
        exception<BadRequestException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest, GlobalResponse(
                    code = 400, status = false, message = cause.message ?: "Bad request"
                )
            )
        }
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                GlobalResponse(500, false, "Server error: ${cause.localizedMessage}")
            )
        }
    }
}