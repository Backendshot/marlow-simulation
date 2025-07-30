package com.marlow.plugin

import com.marlow.global.GlobalResponse
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.CannotTransformContentToTypeException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import kotlinx.serialization.SerializationException
import io.ktor.server.response.respond
import java.net.InetAddress
import java.time.ZoneOffset

fun Application.installGlobalErrorHandling(ds: HikariDataSource) {
    install(StatusPages) {
        exception<SerializationException> { call, cause ->
            logError(ds, call, cause, HttpStatusCode.BadRequest, 400, "Invalid JSON: ${cause.localizedMessage}")
        }
        exception<CannotTransformContentToTypeException> { call, cause ->
            logError(ds, call, cause, HttpStatusCode.BadRequest, 400, "Wrong input: ${cause.localizedMessage}")
        }
        exception<NumberFormatException> { call, cause ->
            logError(ds, call, cause, HttpStatusCode.BadRequest, 400, "Invalid number format: ${cause.localizedMessage}")
        }
        exception<BadRequestException> { call, cause ->
            logError(ds, call, cause, HttpStatusCode.BadRequest, 400, cause.message ?: "Bad request")
        }
        exception<Throwable> { call, cause ->
            logError(ds, call, cause, HttpStatusCode.InternalServerError, 500, "Server error: ${cause.localizedMessage}")
        }
    }
}

private suspend fun logError(ds: HikariDataSource, call: ApplicationCall, cause: Throwable, status: HttpStatusCode, errorCode: Int, errorMessage: String) {
    val ERROR_LOG_QUERY = "INSERT INTO error_logs (error_code, error_message, timestamp, api_occurred, system_occurred) VALUES (?, ?, ?, ?, ?)"

    // Log error to database
    val timestamp = java.sql.Timestamp(java.time.LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli())
    val apiOccurred = call.request.path()
    val systemOccurred = InetAddress.getLocalHost().hostName

    // Call a function to log the error to the database
    ds.connection.use { conn ->
        conn.prepareStatement(ERROR_LOG_QUERY).use { stmt ->
            stmt.setInt(1, errorCode)
            stmt.setString(2, errorMessage)
            stmt.setTimestamp(3, timestamp)
            stmt.setString(4, apiOccurred)
            stmt.setString(5, systemOccurred)

            stmt.executeUpdate()
        }
    }

    // Convert exception to HTTP response call
    call.respond(status, GlobalResponse(errorCode, false, errorMessage))
}