package com.marlow.todo.plugin

import com.api.route.todoRouting
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        swaggerUI("/swagger","src/main/kotlin/documentation/documentation.yaml")
        authenticate("auth-bearer") {
            todoRouting()
        }
    }
}
