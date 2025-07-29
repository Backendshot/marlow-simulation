package com.marlow.configuration

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*

fun Application.configureHTTP() {
    install(CORS) {
        anyHost()
        allowCredentials = true
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader("X-Requested-With")
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
    }

//    routing { openAPI(path = "openapi") }
//    routing { swaggerUI(path = "openapi") }
}
