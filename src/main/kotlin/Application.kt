package com.marlow

import com.marlow.configuration.*
import com.marlow.plugin.installGlobalErrorHandling
import com.marlow.todo.query.TodoQuery
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.netty.*
import kotlinx.serialization.json.Json

val client = HttpClient(CIO) {
    install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
        json(
            Json {
                prettyPrint       = true
                ignoreUnknownKeys = false
                isLenient         = false
                coerceInputValues = false
            },
        )
    }
}

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val ds = Config().getConnection()
    val bearerToken = ds.connection.use { conn ->
        conn.prepareCall(TodoQuery.GET_BEARER_TOKEN)
            .executeQuery()
            .takeIf { it.next() }
            ?.getString("bearer_token")
    }

    install(Authentication) {
        bearer("auth-bearer") {
            realm = "Access to the '/' path"
            authenticate { tokenCredential ->
                if (tokenCredential.token == bearerToken) {
                    UserIdPrincipal("marlow")
                } else {
                    null
                }
            }
        }
    }

    configureSerialization()
    configureSecurity()
    configureHTTP()
    installGlobalErrorHandling()
    configureRouting(ds)
}
