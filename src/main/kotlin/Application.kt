package com.marlow

import com.marlow.LoginSystem.query.LoginQuery
import com.marlow.LoginSystem.util.LoginJWT
import com.marlow.configuration.Config
import com.marlow.configuration.configureHTTP
import com.marlow.configuration.configureRouting
import com.marlow.configuration.configureSecurity
import com.marlow.configuration.configureSerialization
import com.marlow.todo.query.TodoQuery
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.bearer
import io.ktor.server.netty.EngineMain
import kotlinx.serialization.json.Json

val client =
        HttpClient(CIO) {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(
                        Json {
                            prettyPrint = true
                            ignoreUnknownKeys = false
                            isLenient = false
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
                                    val rs = stmt.executeQuery()
                                    var valid = false
                                    while (rs.next()) {
                                        val storedToken = rs.getString("jwt_token")
                                        if (storedToken == tokenCredential.token) {
                                            valid = true
                                            break
                                        }
                                    }
                                    valid
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
    configureRouting(ds)
}
