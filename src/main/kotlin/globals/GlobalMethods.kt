package com.marlow.globals

import com.marlow.systems.registration.queries.UserQuery
import de.mkammerer.argon2.Argon2Factory
import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import jakarta.mail.Message
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.sql.Connection
import java.util.*

class GlobalMethods {
    fun getUserByUsername(connection: Connection, username: String): GlobalUserInfo? {
        val stmt = connection.prepareCall(UserQuery.GET_USER)
        stmt.setString(1, username)
        val result = stmt.executeQuery()

        return if (result.next()) {
            val id = result.getInt("id")
            val email = result.getString("email")
            connection.close()
            GlobalUserInfo(id, email)
        } else {
            connection.close()
            null
        }
    }

    fun hashPassword(password: String): String {
        val hasher = Argon2Factory.create()

        return hasher.hash(2, 65536, 1, password.toCharArray())
    }

    fun saveImage(part: PartData.FileItem): String {
        val allowedExtensions = listOf("jpg", "jpeg", "png", "webp")
        val originalName      = part.originalFileName ?: ""
        val extension         = File(originalName).extension.lowercase()

        require(extension in allowedExtensions) { "Invalid image type: .$extension is not allowed." }

        val fileName = "${UUID.randomUUID()}.$extension"
        val filePath = "image_uploads/$fileName"

        val file = File(filePath)
        file.parentFile.mkdirs()

        // Copy the stream directly once
        file.outputStream().use { output ->
            part.streamProvider().use { input ->
                input.copyTo(output)
            }
        }

        return fileName
    }

    suspend fun getAccessToken(): String {
        val dotEnv       = dotenv()
        val clientId     = dotEnv["GMAIL_CLIENT_ID"] ?: return "Missing GMAIL_CLIENT_ID env variable."
        val clientSecret = dotEnv["GMAIL_CLIENT_SECRET"] ?: return "Missing GMAIL_CLIENT_SECRET env variable."
        val refreshToken = dotEnv["GMAIL_REFRESH_TOKEN"] ?: return "Missing GMAIL_REFRESH_TOKEN env variable."
        val client       = HttpClient(CIO)

        val response = client.submitForm(
            url = "https://oauth2.googleapis.com/token",
            formParameters = Parameters.build {
                append("client_id", clientId)
                append("client_secret", clientSecret)
                append("refresh_token", refreshToken)
                append("grant_type", "refresh_token")
            }
        )

        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        return json["access_token"]?.jsonPrimitive?.content ?: throw IllegalStateException("Failed to get access token")
    }

    fun sendEmail(
        recipient: String, subject: String?, body: String, accessToken: String
    ) {
        val dotEnv    = dotenv()
        val userEmail = dotEnv["GMAIL_EMAIL"]
        val props     = Properties().apply {
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

object ErrorHandler {
    suspend fun handle(call: ApplicationCall, e: Throwable) {
        when (e) {
            is IllegalArgumentException -> call.respond(HttpStatusCode.BadRequest, GlobalResponse(400, false, e.localizedMessage ?: "Invalid request"))
            is IllegalStateException -> call.respond(HttpStatusCode.Forbidden, GlobalResponse(403, false, e.localizedMessage ?: "Forbidden"))
            else -> call.respond(HttpStatusCode.InternalServerError, GlobalResponse(500, false, e.localizedMessage ?: "Internal server error"))
        }
    }
}