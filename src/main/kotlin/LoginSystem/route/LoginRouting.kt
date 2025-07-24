package com.marlow.LoginSystem.route

import com.marlow.LoginSystem.controller.LoginController
import com.marlow.LoginSystem.model.LoginModel
import com.marlow.LoginSystem.model.Validator
import com.marlow.global.GlobalResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.SerializationException

fun Route.LoginRouting() {

    route("/login") {
        post ("/"){
            try {
                val loginData = call.receive<LoginModel>()
                val errors = Validator().validateLoginInput(loginData)
                if (errors.isNotEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, GlobalResponse(400, false, errors.joinToString(", ")))
                    return@post
                }
                val sanitizedData = Validator().sanitizeInput(loginData)
                val response = LoginController.login(sanitizedData as LoginModel)
                if (response != null) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.Unauthorized, GlobalResponse(401, false, "Invalid Username or Password"))
                }
            } catch (e: SerializationException) {
                call.respond(HttpStatusCode.BadRequest, GlobalResponse(400, false, "Invalid JSON format"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalResponse(500, false, e.localizedMessage))
            }
        }

        post("logout") {
            try {
                val loginData = call.receive<LoginModel>()
                val userId = loginData.id ?: throw IllegalArgumentException("User ID is required for logout")
                val result = LoginController.logout(userId)
                if (result) {
                    call.respond(HttpStatusCode.OK, GlobalResponse(200, true, "Logout successful"))
                } else {
                    call.respond(HttpStatusCode.NotFound, GlobalResponse(404, false, "User session not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalResponse(500, false, e.localizedMessage))
            }
        }
    }
}