package com.marlow.global

import com.marlow.registrationSystem.queries.UserQuery
import de.mkammerer.argon2.Argon2Factory
import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.content.PartData
import io.ktor.http.content.streamProvider
import jakarta.mail.Message
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import java.io.File
import java.sql.Connection
import java.util.Properties
import java.util.UUID

class GlobalMethods {
     fun getUserIdByUsername(connection: Connection, username: String): Int? {
        val userId = connection.prepareCall(UserQuery.GET_USER_ID)
        userId.setString(1, username)
        val result = userId.executeQuery()
        return if (result.next()) result.getInt("id") else null
    }

     fun hashPassword(password: String): String {
        val hasher = Argon2Factory.create()

        return hasher.hash(2, 65536, 1, password.toCharArray())
    }

     fun saveImage(part: PartData.FileItem): String {
        val allowedExtensions = listOf("jpg", "jpeg", "png", "webp")
        val originalName      = part.originalFileName ?: ""
        val extension         = File(originalName).extension.lowercase()

        if (extension !in allowedExtensions) {
            throw IllegalArgumentException("Invalid image type: .$extension is not allowed.")
        }

        val inputStream    = part.streamProvider()
        val byteArray      = inputStream.readBytes()
        val maxSizeInBytes = 2 * 1024 * 1024

        if (byteArray.size > maxSizeInBytes) {
            throw IllegalArgumentException("File size exceeds 2MB limit.")
        }

        val fileName = UUID.randomUUID().toString() + "." + extension
        val filePath = "image_uploads/$fileName"

        File(filePath).apply {
            parentFile.mkdirs()
            outputStream().use { part.streamProvider().copyTo(it) }
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
        return json["access_token"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("Failed to get access token")
    }

    fun sendEmail(
        recipient: String,
        subject: String,
        body: String,
        accessToken: String
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