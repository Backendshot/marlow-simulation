package com.marlow

import com.marlow.plugin.installGlobalErrorHandling
import com.marlow.LoginSystem.query.LoginQuery
import com.marlow.LoginSystem.util.LoginJWT
import com.marlow.configuration.Config
import com.marlow.configuration.configureHTTP
import com.marlow.configuration.configureMonitoring
import com.marlow.configuration.configureRouting
import com.marlow.configuration.configureSecurity
import com.marlow.configuration.configureSerialization
import com.marlow.todo.query.TodoQuery
import io.ktor.server.application.*
import kotlinx.serialization.json.Json
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.bearer

import io.ktor.server.netty.EngineMain

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

    monitor.subscribe(ApplicationStarted) { application ->
        application.environment.log.info("Server is started")
    }

    monitor.subscribe(ApplicationStopped) { application ->
        application.environment.log.info("Server is stopped")
        // Release resources and unsubscribe from events
        monitor.unsubscribe(ApplicationStarted) {}
        monitor.unsubscribe(ApplicationStopped) {}
    }

    install(Authentication) {
        bearer("auth-bearer") {
            realm = "Access to the '/' path"
            authenticate { tokenCredential ->
                try {
                    val userId = LoginJWT.verifyAndExtractUserId(tokenCredential.token)

                    val isValidToken =
                        ds.connection.use { conn ->
                            conn.prepareStatement(LoginQuery.GET_BEARER_TOKEN).use { stmt ->
                                stmt.setInt(1, userId)
                                //since this is the last statement, this will return any values resulting from the function call below (a boolean)
                                stmt.executeQuery().use { rs ->
                                    //lazily (don't retrieve until needed) get the jwt_token
                                    generateSequence { if (rs.next()) rs.getString("jwt_token") else null }
                                        .any { it == tokenCredential.token } //if the token retrieved doesn't match with the tokenCredential
                                }
                            }
                        }
                    if (isValidToken) {
                        UserIdPrincipal(userId.toString())
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    println("JWT validation failed: ${e.message}")
                    null
                }
            }
        }
    }

    install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
        json(
                Json {
                    prettyPrint = true
                    isLenient = false
                    ignoreUnknownKeys = false
                    coerceInputValues = false
                }
        )
    }

    configureSerialization()
    configureSecurity()
    configureHTTP()
    configureMonitoring()
    installGlobalErrorHandling(ds)
    configureRouting(ds)
}
