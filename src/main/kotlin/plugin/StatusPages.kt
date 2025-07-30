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
    val ERROR_LOG_QUERY = "INSERT INTO error_logs (error_code, error_message, timestamp, api_occurred, system_occurred) VALUES (?, ?, ?, ?, ?)"

    install(StatusPages) {
        exception<SerializationException> { call, cause ->
            // Log error to database
            val errorCode = 400
            val errorMessage = "Invalid JSON: ${cause.localizedMessage}"
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
            call.respond(
                HttpStatusCode.BadRequest, GlobalResponse(400, false, "Invalid JSON: ${cause.localizedMessage}")
            )
        }
        exception<CannotTransformContentToTypeException> { call, cause ->
            // Log error to database
            val errorCode = 400
            val errorMessage = "Wrong input: ${cause.localizedMessage}"
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

            call.respond(
                HttpStatusCode.BadRequest, GlobalResponse(400, false, "Wrong input: ${cause.localizedMessage}")
            )
        }
        exception<NumberFormatException> { call, cause ->
            // Log error to database
            val errorCode = 400
            val errorMessage = "Invalid number format: ${cause.localizedMessage}"
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

            call.respond(
                HttpStatusCode.BadRequest,
                GlobalResponse(400, false, "Invalid number format: ${cause.localizedMessage}")
            )
        }
        exception<BadRequestException> { call, cause ->
            // Log error to database
            val errorCode = 400
            val errorMessage = cause.message ?: "Bad request"
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

            call.respond(
                HttpStatusCode.BadRequest, GlobalResponse(
                    code = 400, status = false, message = cause.message ?: "Bad request"
                )
            )
        }
        exception<Throwable> { call, cause ->
            // Log error to database
            val errorCode = 500
            val errorMessage = "Server error: ${cause.localizedMessage}"
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

            call.respond(
                HttpStatusCode.InternalServerError,
                GlobalResponse(500, false, "Server error: ${cause.localizedMessage}")
            )
        }
    }
}