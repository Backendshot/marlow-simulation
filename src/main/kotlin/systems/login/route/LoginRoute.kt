package com.marlow.systems.login.route

import com.marlow.globals.ErrorHandler
import com.marlow.globals.GlobalMethods
import com.marlow.systems.login.controller.LoginController
import com.marlow.systems.login.model.LoginRequest
import com.marlow.systems.login.util.LoginAudit
import com.marlow.systems.login.model.LogoutRequest
import com.marlow.globals.GlobalResponse
import com.marlow.systems.login.model.LoginModel
import com.marlow.systems.login.model.Validator
import com.marlow.systems.login.util.LoginJWT
import com.marlow.systems.login.util.LoginSession
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.LoginRoute(ds: HikariDataSource) {

    val loginController = LoginController(ds)

    route("/user") {
        post("/login") {
            try {
                val loginData = call.receive<LoginRequest>()
                val browserInfo = LoginAudit().parseBrowser(call.request.headers["User-Agent"] ?: "Unknown")
                // Validate input
                val validator = Validator()
                val sanitizedLogin = validator.sanitizeInput(loginData)
                val errors = validator.validateLoginInput(sanitizedLogin)

                if (errors.isNotEmpty()) {
                    return@post call.respond(HttpStatusCode.BadRequest, GlobalResponse(400, false, "Validation Errors: ${errors.joinToString(", ")}"))
                }
                // Get userId and hash
                val userIdAndHash = loginController.getUserIdAndHash(sanitizedLogin.username)
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, GlobalResponse(401, false, "Invalid username or password."))

                val (userId, storedHash) = userIdAndHash

                // Check password
                val argon2 = de.mkammerer.argon2.Argon2Factory.create()
                if (!argon2.verify(storedHash, sanitizedLogin.password.toCharArray())) {
                    return@post call.respond(HttpStatusCode.Unauthorized, GlobalResponse(401, false, "Invalid username or password."))
                }
                // Check email status
                if (!loginController.checkEmailStatus(userId)) {
                    return@post call.respond(HttpStatusCode.Forbidden, GlobalResponse(403, false, "Email not verified. Please check your email to verify."))
                }
                // Generate JWT and session
                val jwtToken = LoginJWT.generateJWT(userId)
                val sessionId = LoginSession.generatedSessionId()
                val sessionDeleted = false
                // Update session
                loginController.updateSession(userId, sessionId, jwtToken, sessionDeleted)
                // Insert audit
                loginController.insertAudit(userId, browserInfo)
                // Create Response
                val response = loginController.loginResponse(userId, sanitizedLogin.username, sanitizedLogin.password, jwtToken, sessionId, sessionDeleted)
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Throwable) {
                ErrorHandler.handle(call, e)

            }
        }

        post("/logout") {
            try {
                val logoutData = call.receive<LogoutRequest>()
                val result = loginController.logout(logoutData.user_id)
                if (!result) {
                    return@post call.respond(HttpStatusCode.NotFound, GlobalResponse(404, false, "User session not found"))
                }
                call.respond(HttpStatusCode.OK, GlobalResponse(200, true, "Logout successful"))
            } catch (e: Throwable) {
                ErrorHandler.handle(call, e)
            }
        }

        get("/audit/{userId}") {
            try {
                val userId = call.parameters["userId"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid User ID")
                val auditData = loginController.viewAllAuditById(userId)
                call.respond(HttpStatusCode.OK, auditData)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, GlobalResponse(400, false, e.message ?: "Invalid request"))
            } catch (e: Throwable) {
                ErrorHandler.handle(call, e)
            }
        }
    }
}