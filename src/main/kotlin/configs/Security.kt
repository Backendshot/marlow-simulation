package com.marlow.configs

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import de.mkammerer.argon2.Argon2
import de.mkammerer.argon2.Argon2Factory
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable

fun Application.configureSecurity() {
    install(Sessions) {
        cookie<MySession>("MY_SESSION") {
            cookie.extensions["SameSite"] = "lax"
        }
    }
    // Please read the jwt property from the config file if you are using EngineMain
    val jwtAudience = "jwt-audience"
    val jwtDomain = "https://jwt-provider-domain/"
    val jwtRealm = "ktor sample app"
    val jwtSecret = "secret"
    authentication {
        jwt {
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtDomain)
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains(jwtAudience)) JWTPrincipal(credential.payload) else null
            }
        }
    }
    routing {
        get("/session/increment") {
            val session = call.sessions.get<MySession>() ?: MySession()
            call.sessions.set(session.copy(count = session.count + 1))
            call.respondText("Counter is ${session.count}. Refresh to increment.")
        }
    }
}

@Serializable
data class MySession(val count: Int = 0)

class PasswordHasher(
    private val iterations: Int = 3,
    private val memoryKb: Int = 1 shl 16, // 64 MB
    private val parallelism: Int = 1
) {
    // Create one Argon2 instance (thread‐safe entry point)
    private val argon2: Argon2 = Argon2Factory.create()

    /**
     * Hashes the given plain‐text password.
     * @return the encoded hash string, including parameters and salt.
     */
    fun hash(plain: CharArray): String {
        // The returned string contains parameters, salt, and hash.
        return argon2.hash(iterations, memoryKb, parallelism, plain)
    }

    fun verify(encoded: String, plain: CharArray): Boolean {
        return argon2.verify(encoded, plain)
    }
}
