package com.marlow

import com.marlow.configuration.Config
import com.marlow.configuration.configureHTTP
import com.marlow.configuration.configureRouting
import com.marlow.configuration.configureSecurity
import com.marlow.configuration.configureSerialization
import com.marlow.todo.query.TodoQuery
import io.ktor.client.HttpClient
import io.ktor.server.application.*
import io.ktor.client.engine.cio.CIO
import kotlinx.serialization.json.Json
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.bearer
//import io.ktor.server.auth.form
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
    val bearerToken = ds.connection.use { conn ->
        conn.prepareCall(TodoQuery.GET_BEARER_TOKEN)
            .executeQuery()
            .takeIf { it -> it.next() }
            ?.getString("bearer_token")
    }

// Create a default Argon2 instance
//    val argon2: Argon2 = Argon2Factory.create()
//
//// Hash (costs: iterations, memoryKiB, parallelism)
//    val hash: String = argon2.hash(2, 65536, 1, "mySecretPassword".toCharArray())
//
//// Verify
//    val matches: Boolean = argon2.verify(hash, "attemptedPassword".toCharArray())
//
//    if (matches) {
//        println("✅ Password OK")
//    } else {
//        println("❌ Bad credentials")
//    }

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
//        form("login") {
//            userParamName = "username"
//            passwordParamName = "password"
//            validate { credentials ->
//                val storedHash = userRepo.getHashFor(credentials.name)          // fetch from DB
//        if (argon2.verify(storedHash, credentials.password.toCharArray())) {
//            UserIdPrincipal(credentials.name)
//        } else null
//            }
//        }
    }

    install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = false
            ignoreUnknownKeys = false
            coerceInputValues = false
        })
    }

    configureSerialization()
    configureSecurity()
    configureHTTP()
    configureRouting(ds)
}
