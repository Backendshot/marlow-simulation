package com.marlow

import com.marlow.LoginSystem.util.LoginAudit
import com.marlow.LoginSystem.util.LoginDecryption
import com.marlow.LoginSystem.util.LoginDecryption.aesDecrypt
import com.marlow.LoginSystem.util.LoginDecryption.aesEncrypt
import com.marlow.LoginSystem.util.LoginDecryption.generateAESKey
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
<<<<<<< HEAD
import kotlin.test.assertEquals
=======
import io.ktor.server.netty.EngineMain
>>>>>>> 53d9edccfbf40cd1b94407e62e46439dbb976a79

val client = HttpClient(CIO) {
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
<<<<<<< HEAD
    io.ktor.server.netty.EngineMain.main(args)
//    val originalText = "Hello Kotlin AES Encryption!"
//    val secretKey = generateAESKey(256)
//
//    val encryptedData = aesEncrypt(originalText.toByteArray(), secretKey)
//
//    println("Encrypted Data: ${encryptedData.joinToString(", ")}")
//    val decryptedData = aesDecrypt(encryptedData, secretKey)
//    val decryptedText = String(decryptedData)
//
//    println("Decrypted Text: $decryptedText")
=======
    EngineMain.main(args)
>>>>>>> 53d9edccfbf40cd1b94407e62e46439dbb976a79
}

fun Application.module() {
//    val connection = Config().connect()
    val ds = Config().getConnection()//.createDataSource()
//    val todo = TodoController(ds)
    val bearerToken = ds.connection.use { conn ->
        conn.prepareCall(TodoQuery.GET_BEARER_TOKEN) //bearerTokenQuery
            .executeQuery() //resultSet
            .takeIf { it -> it.next() } //take the value if resultSet.next() is true
            ?.getString("bearer_token") //use the value to get the bearer_token
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
//    configureDatabases()
    configureRouting(ds)
}
