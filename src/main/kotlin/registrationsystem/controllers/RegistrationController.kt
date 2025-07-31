package com.marlow.registrationsystem.controllers

import com.marlow.global.GlobalMethods
import com.marlow.global.RegistrationResult
import com.marlow.registrationsystem.dto.RegistrationRequest
import com.marlow.registrationsystem.models.CredentialsModel
import com.marlow.registrationsystem.models.EmailSendingModel
import com.marlow.registrationsystem.models.InformationModel
import com.marlow.registrationsystem.queries.UserQuery
import com.zaxxer.hikari.HikariDataSource
import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import java.time.LocalDate
import java.time.LocalDateTime

class RegistrationController (private val ds: HikariDataSource) {
    suspend fun register(call: ApplicationCall): RegistrationResult {
        return try {
            val methods    = GlobalMethods()
            val multipart  = call.receiveMultipart()
            val formFields = mutableMapOf<String, String>()
            val now        = LocalDate.now()

            var imageFileName: String? = null
            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        formFields[part.name.orEmpty()] = part.value
                    }
                    is PartData.FileItem -> {
                        if (part.name == "image" && !part.originalFileName.isNullOrBlank()) {
                            imageFileName = methods.saveImage(part)
                        }
                    }
                    else -> {}
                }
                part.dispose()
            }

            val input = RegistrationRequest(
                username   = formFields["username"] ?: "",
                firstName  = formFields["firstName"] ?: "",
                middleName = formFields["middleName"],
                lastName   = formFields["lastName"] ?: "",
                roleType   = formFields["roleType"] ?: "",
                email      = formFields["email"] ?: "",
                birthday   = formFields["birthday"] ?: "",
                password   = formFields["password"] ?: "",
                image      = imageFileName
            )

            val information = InformationModel(
                username   = input.username,
                firstName  = input.firstName,
                middleName = input.middleName,
                lastName   = input.lastName,
                roleType   = input.roleType,
                email      = input.email,
                birthday   = input.birthday?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) },
                createdAt  = now,
                updatedAt  = now,
                image      = imageFileName
            ).sanitized()

            val infoErrors = information.validate()
            if (infoErrors.isNotEmpty()) {
                return RegistrationResult.ValidationError("Information validation failed.")
            }

            if (input.password.isBlank() || input.password.length < 8) {
                return RegistrationResult.ValidationError("Password must be at least 8 characters.")
            }

            val checkStmt = ds.connection.prepareStatement(UserQuery.CHECK_USERNAME_EXISTS)
            checkStmt.setString(1, information.username)
            val result = checkStmt.executeQuery()
            if (result.next() && result.getInt("count") > 0) {
                return RegistrationResult.Conflict("Username already exists.")
            }

            ds.connection.use { conn ->
                conn.prepareCall(UserQuery.INSERT_INFORMATION).use { stmt ->
                    stmt.setString(1, information.username)
                    stmt.setString(2, information.firstName)
                    stmt.setString(3, information.middleName)
                    stmt.setString(4, information.lastName)
                    stmt.setString(5, information.email)
                    stmt.setObject(6, information.birthday)
                    stmt.setObject(7, information.createdAt)
                    stmt.setObject(8, information.updatedAt)
                    stmt.setString(9, information.roleType)
                    stmt.setString(10, information.image)
                    stmt.execute()
                }
            }

            val user = methods.getUserByUsername(ds.connection, information.username)
                ?: return RegistrationResult.Failure("Failed to retrieve new user ID.")

            val credentials = CredentialsModel(
                userId    = user.id,
                username  = information.username,
                password  = methods.hashPassword(input.password),
                createdAt = now,
                updatedAt = now
            ).sanitized()

            val credErrors = credentials.validate()
            if (credErrors.isNotEmpty()) {
                return RegistrationResult.ValidationError("Credentials validation failed.")
            }

            ds.connection.use { conn ->
                conn.prepareCall(UserQuery.INSERT_CREDENTIALS).use { stmt ->
                    stmt.setInt(1, credentials.userId)
                    stmt.setString(2, credentials.username)
                    stmt.setString(3, credentials.password)
                    stmt.setBoolean(4, credentials.activeSession)
                    stmt.setBoolean(5, credentials.activeSessionDeleted)
                    stmt.setObject(6, credentials.createdAt)
                    stmt.setObject(7, credentials.updatedAt)
                    stmt.execute()
                }
            }

            val verificationLink = "http://localhost:8080/api/user/email/verify?userId=${user.id}"
            val dotEnv           = dotenv()

            val emailLogs = EmailSendingModel(
                userId        = user.id,
                fromSystem    = "REGISTRATION",
                senderEmail   = dotEnv["GMAIL_EMAIL"],
                receiverEmail = user.email,
                status        = "PENDING",
                subject       = "Welcome to our app, ${input.firstName}!",
                body          = """
                                    Hello ${input.firstName},
                                    
                                    Your registration was successful!
                            
                                    Please click the link below to verify your email:
                                    $verificationLink
                            
                                    Regards,
                                    The Team
                                """.trimIndent(),
                requestedAt   = now,
                verifiedAt    = null,
            ).sanitized()

            val emailLogsError = emailLogs.validate()
            if (emailLogsError.isNotEmpty()) {
                return RegistrationResult.ValidationError("Email Logs validation failed.")
            }

            ds.connection.use { conn ->
                conn.prepareCall(UserQuery.INSERT_EMAIL_SENDING).use { stmt ->
                    stmt.setInt(1, emailLogs.userId)
                    stmt.setString(2, emailLogs.fromSystem)
                    stmt.setString(3, emailLogs.senderEmail)
                    stmt.setString(4, emailLogs.receiverEmail)
                    stmt.setString(5, emailLogs.status)
                    stmt.setString(6, emailLogs.subject)
                    stmt.setString(7, emailLogs.body)
                    stmt.setObject(8, emailLogs.requestedAt)
                    stmt.setObject(9, emailLogs.verifiedAt)
                    stmt.execute()
                }
            }

            val accessToken = methods.getAccessToken()
            methods.sendEmail(
                recipient   = information.email,
                subject     = emailLogs.subject,
                body        = emailLogs.body,
                accessToken = accessToken
            )

            RegistrationResult.Success("User registered successfully.")
        } catch (e: Exception) {
            e.printStackTrace()
            RegistrationResult.Failure("Internal server error: ${e.message}")
        }
    }

    fun verifyEmail(call: ApplicationCall): RegistrationResult {
        val userIdParam = call.request.queryParameters["userId"] ?: return RegistrationResult.ValidationError("test")

        val userId = userIdParam.toIntOrNull()
        if (userId == null) {
            return RegistrationResult.Failure("test")
        }

        val dateNow = LocalDate.now()

        return try {
            ds.connection.use { conn ->
                ds.connection.use { conn ->
                    conn.prepareCall(UserQuery.UPDATE_EMAIL_VERIFIED).use { stmt ->
                        stmt.setInt(1, userId)
                        stmt.setObject(2, dateNow)


                        val rows = stmt.executeUpdate()
                        if (rows > 0) {
                            RegistrationResult.Success("Email verification successful!")
                        } else {
                            RegistrationResult.Failure("User email log not found.")
                        }
                    }
                }
            }
            RegistrationResult.Success("Email Verification Success!")
        } catch (e: Exception) {
            e.printStackTrace()
            RegistrationResult.Failure("Internal server error: ${e.message}")
        }
    }
}
