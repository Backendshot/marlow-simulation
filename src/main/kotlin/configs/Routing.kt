package com.marlow.configs

import com.marlow.LoginSystem.route.LoginRouting
import com.marlow.systems.todo.route.todoRouting
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.http.content.files
import io.ktor.server.http.content.static
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.*
import registrationRouting

fun Application.configureRouting(ds: HikariDataSource) {
    routing {
        swaggerUI("/swagger", "src/main/kotlin/systems/todo/documentation/documentation.yaml")
        authenticate("auth-bearer") {
            todoRouting(ds)
        }
        LoginRouting(ds)
        registrationRouting(ds)
        static("/image-uploads") {
            files("image-uploads")
        }
    }
}
