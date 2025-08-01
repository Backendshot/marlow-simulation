package com.marlow.systems.login.route

import com.marlow.systems.login.controller.LoginController
import com.marlow.systems.login.model.LoginRequest
import com.marlow.systems.login.util.LoginAudit
import com.marlow.systems.login.model.LogoutRequest
import com.marlow.globals.GlobalResponse
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import kotlinx.serialization.SerializationException

fun Route.LoginRouting(ds: HikariDataSource) {

    route("/user") {
        post("/login") {
            try {
                val loginData = call.receive<LoginRequest>()
                val browserInfo =
                    LoginAudit().parseBrowser(call.request.headers["User-Agent"] ?: "Unknown")

                val response =
                    LoginController(ds).login(loginData, browserInfo)
                        ?: call.respond(
                            HttpStatusCode.Unauthorized,
                            GlobalResponse(401, false, "Invalid username or password")
                        )
                call.respond(HttpStatusCode.OK, response)
            } catch (e: SerializationException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    GlobalResponse(400, false, "Invalid JSON format")
                )
            } catch (e: IllegalStateException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    GlobalResponse(403, false, e.localizedMessage)
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    GlobalResponse(500, false, e.localizedMessage)
                )
            }
        }

        post("/logout") {
            try {
                val logoutData = call.receive<LogoutRequest>()
                val result = LoginController(ds).logout(logoutData.user_id)

                if (result) {
                    call.respond(HttpStatusCode.OK, GlobalResponse(200, true, "Logout successful"))
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        GlobalResponse(404, false, "User session not found")
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    GlobalResponse(500, false, e.localizedMessage)
                )
            }
        }

        get("/audit/{userId}") {
            try {
                val userId =
                    call.parameters["userId"]?.toIntOrNull()
                        ?: throw IllegalArgumentException("Invalid User ID")
                val auditData = LoginController(ds).viewAllAuditById(userId)
                call.respond(HttpStatusCode.OK, auditData)
            } catch (e: IllegalArgumentException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    GlobalResponse(400, false, e.message ?: "Invalid request")
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    GlobalResponse(500, false, e.localizedMessage)
                )
            }
        }
    }
}
