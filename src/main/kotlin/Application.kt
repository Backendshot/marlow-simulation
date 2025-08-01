package com.marlow

import com.marlow.systems.login.query.LoginQuery
import com.marlow.configs.Config
import com.marlow.configs.configureHTTP
import com.marlow.configs.configureMonitoring
import com.marlow.configs.configureRouting
import com.marlow.configs.configureSecurity
import com.marlow.configs.configureSerialization
import com.marlow.systems.login.util.LoginJWT
import com.marlow.plugins.installGlobalErrorHandling
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

    configureSerialization()
    configureSecurity()
    configureHTTP()
    configureMonitoring()
    installGlobalErrorHandling(ds)
    configureRouting(ds)
}
