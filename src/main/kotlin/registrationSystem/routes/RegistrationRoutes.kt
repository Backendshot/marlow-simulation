package com.marlow.registrationSystem.routes

import io.ktor.server.response.respondText
import io.ktor.server.routing.*

fun Route.registrationRouting() {
    route("/test") {
        get {
            call.respondText("this is a test")
        }
    }
}
