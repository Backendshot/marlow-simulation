package com.marlow.configuration

import com.marlow.LoginSystem.route.LoginRouting
import com.marlow.todo.route.todoRouting
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.*

fun Application.configureRouting(ds: HikariDataSource) {
    routing {
        swaggerUI("/swagger","src/main/kotlin/todo/documentation/documentation.yaml")
        authenticate("auth-bearer") {
            todoRouting(ds)
        }
        LoginRouting(ds)
    }
}
