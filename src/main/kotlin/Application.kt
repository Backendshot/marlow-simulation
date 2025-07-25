package com.marlow

import com.marlow.LoginSystem.util.LoginAudit
import com.marlow.LoginSystem.util.LoginDecryption
import com.marlow.LoginSystem.util.LoginDecryption.aesDecrypt
import com.marlow.LoginSystem.util.LoginDecryption.aesEncrypt
import com.marlow.LoginSystem.util.LoginDecryption.generateAESKey
import com.marlow.configuration.Config
import com.marlow.todo.plugin.configureHTTP
import com.marlow.todo.plugin.configureRouting
import com.marlow.todo.plugin.configureSecurity
import com.marlow.todo.plugin.configureSerialization
import com.marlow.todo.query.TodoQuery
import io.ktor.client.HttpClient
import io.ktor.server.application.*
import io.ktor.client.engine.cio.CIO
import kotlinx.serialization.json.Json
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.bearer
import kotlin.test.assertEquals

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
}

fun Application.module() {
    val connection = Config().connect()
    val bearerTokenQuery = connection.prepareCall(TodoQuery.GET_BEARER_TOKEN)
    val resultSet = bearerTokenQuery.executeQuery()
    val bearerToken = if (resultSet.next()) {
        resultSet.getString("bearer_token")
    } else {
        null
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
    configureRouting()
}
