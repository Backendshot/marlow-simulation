package com.marlow.LoginSystem.route

import com.marlow.LoginSystem.controller.LoginController
import com.marlow.LoginSystem.model.LoginModel
import com.marlow.LoginSystem.model.Validator
import com.marlow.LoginSystem.util.LoginAudit
import com.marlow.global.GlobalResponse
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.http.HttpHeaders
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerializationException

fun Route.LoginRouting(ds: HikariDataSource) {

    route("/user") {
        post ("/login"){
            try {
                val loginData = call.receive<LoginModel>()
                val browserInfo = LoginAudit().parseBrowser(call.request.headers["User-Agent"] ?: "Unknown")

                val response = LoginController(ds).login(loginData, browserInfo)
                if (response != null) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.Unauthorized, GlobalResponse(401, false, "Invalid username or password"))
                }
            } catch (e: SerializationException) {
                call.respond(HttpStatusCode.BadRequest, GlobalResponse(400, false, "Invalid JSON format"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalResponse(500, false, e.localizedMessage))
            }
        }

        post("/logout") {
            try {
                val loginData = call.receive<LoginModel>()
                val userId = loginData.id
                val result = LoginController(ds).logout(userId)
                if (result) {
                    call.respond(HttpStatusCode.OK, GlobalResponse(200, true, "Logout successful"))
                } else {
                    call.respond(HttpStatusCode.NotFound, GlobalResponse(404, false, "User session not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalResponse(500, false, e.localizedMessage))
            }
        }

        get ("/audit/{userId}") {
            try {
                val userId = call.parameters["userId"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid User ID")
                val auditData = LoginController(ds).viewAllAuditById(userId)
                call.respond(HttpStatusCode.OK, auditData)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, GlobalResponse(400, false, e.message ?: "Invalid request"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalResponse(500, false, e.localizedMessage))
            }
        }
    }
}