package com.marlow.globals

import com.marlow.systems.registration.queries.UserQuery
import de.mkammerer.argon2.Argon2Factory
import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.content.PartData
import io.ktor.http.content.streamProvider
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import jakarta.mail.Message
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.sql.Connection
import java.util.Properties
import java.util.UUID

class GlobalMethods(private val dispatcher: CoroutineDispatcher = Dispatchers.IO) {
    suspend fun getUserByUsername(connection: Connection, username: String): GlobalUserInfo? = withContext(dispatcher) {
        val stmt = connection.prepareCall(UserQuery.GET_USER)
        stmt.setString(1, username)
        val result = stmt.executeQuery()

        return@withContext if (result.next()) {
            val id = result.getInt("id")
            val email = result.getString("email")
            connection.close()
            GlobalUserInfo(id, email)
        } else {
            connection.close()
            null
        }
    }

    suspend fun hashPassword(password: String): String = withContext(dispatcher) {
        val hasher = Argon2Factory.create()

        return@withContext hasher.hash(2, 65536, 1, password.toCharArray())
    }

    suspend fun saveImage(part: PartData.FileItem): String = withContext(dispatcher) {
        val allowedExtensions = listOf("jpg", "jpeg", "png", "webp")
        val originalName = part.originalFileName ?: ""
        val extension = File(originalName).extension.lowercase()

        require(extension in allowedExtensions) { "Invalid image type: .$extension is not allowed." }

        val inputStream    = part.streamProvider()
        val byteArray      = inputStream.readBytes()
        val maxSizeInBytes = 16 * 1024 * 1024

        require(byteArray.size <= maxSizeInBytes) { "File size exceeds 16MB limit."}

        val fileName = UUID.randomUUID().toString() + "." + extension
        val filePath = "image_uploads/$fileName"

        File(filePath).apply {
            parentFile.mkdirs()
            outputStream().use { part.streamProvider().copyTo(it) }
        }

        return@withContext fileName
    }

    suspend fun getAccessToken(): String = withContext(dispatcher) {
        val dotEnv = dotenv()
        val clientId = dotEnv["GMAIL_CLIENT_ID"] ?: return@withContext "Missing GMAIL_CLIENT_ID env variable."
        val clientSecret =
            dotEnv["GMAIL_CLIENT_SECRET"] ?: return@withContext "Missing GMAIL_CLIENT_SECRET env variable."
        val refreshToken =
            dotEnv["GMAIL_REFRESH_TOKEN"] ?: return@withContext "Missing GMAIL_REFRESH_TOKEN env variable."
        val client = HttpClient(CIO)

        val response = client.submitForm(
            url = "https://oauth2.googleapis.com/token", formParameters = Parameters.build {
                append("client_id", clientId)
                append("client_secret", clientSecret)
                append("refresh_token", refreshToken)
                append("grant_type", "refresh_token")
            })

        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        return@withContext json["access_token"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("Failed to get access token")
    }

    suspend fun sendEmail(
        recipient: String, subject: String?, body: String, accessToken: String
    ) = withContext(dispatcher) {
        val dotEnv = dotenv()
        val userEmail = dotEnv["GMAIL_EMAIL"]
        val props = Properties().apply {
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.auth.mechanisms", "XOAUTH2")
            put("mail.smtp.auth", "true")
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.port", "587")
        }

        val session = jakarta.mail.Session.getInstance(props)
        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(userEmail))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient))
            setSubject(subject)
            setText(body)
        }

        val transport = session.getTransport("smtp")
        transport.connect("smtp.gmail.com", userEmail, accessToken)
        transport.sendMessage(message, message.allRecipients)
        transport.close()
    }
}

class ErrorHandler(private val dispatcher: CoroutineDispatcher = Dispatchers.IO) {
    suspend fun handle(call: ApplicationCall, e: Throwable) = withContext(dispatcher) {
        when (e) {
            is IllegalArgumentException -> call.respond(
                HttpStatusCode.BadRequest, GlobalResponse(400, false, e.localizedMessage ?: "Invalid request")
            )

            is IllegalStateException -> call.respond(
                HttpStatusCode.Forbidden, GlobalResponse(403, false, e.localizedMessage ?: "Forbidden")
            )

            else -> call.respond(
                HttpStatusCode.InternalServerError,
                GlobalResponse(500, false, e.localizedMessage ?: "Internal server error")
            )
        }
    }
}