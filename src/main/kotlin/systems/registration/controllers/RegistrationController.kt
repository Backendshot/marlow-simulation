package com.marlow.systems.registration.controllers

import com.marlow.globals.GlobalMethods
import com.marlow.globals.RegistrationResult
import com.marlow.globals.VerificationResult
import com.marlow.systems.registration.dto.RegistrationRequest
import com.marlow.systems.registration.models.CredentialsModel
import com.marlow.systems.registration.models.EmailSendingModel
import com.marlow.systems.registration.models.InformationModel
import com.marlow.systems.registration.queries.UserQuery
import com.zaxxer.hikari.HikariDataSource
import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import java.time.LocalDate

class RegistrationController(private val ds: HikariDataSource) {
    val minimumPasswordLength = 8;
    val userNotExist = 0;

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
                return RegistrationResult.ValidationError(infoErrors.joinToString(separator = "\n"))
            }

            if (input.password.isBlank() || input.password.length < minimumPasswordLength) {
                return RegistrationResult.ValidationError("Password must be at least 8 characters.")
            }

            ds.connection.use { conn ->
                conn.prepareStatement(UserQuery.CHECK_USERNAME_EXISTS).use { stmt ->
                    stmt.setString(1, information.username)
                    val result = stmt.executeQuery()
                    if (result.next() && result.getInt("count") > userNotExist) {
                        return RegistrationResult.Conflict("Username already exists.")
                    }
                }
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

            RegistrationResult.Success("User registered successfully.", user.id)
        } catch (e: Exception) {
            e.printStackTrace()
            RegistrationResult.Failure("Internal server error: ${e.message}")
        }
    }

    fun verifyEmail(call: ApplicationCall): VerificationResult {
        val userIdParam = call.request.queryParameters["userId"]
        if (userIdParam == null) {
            return VerificationResult.Failure("User Id was not passed Successfully")
        }

        val userId = userIdParam.toIntOrNull()
        if (userId == null) {
            return VerificationResult.Failure("User Id was not passed Successfully")
        }

        val dateNow = LocalDate.now()
        val status  = "VERIFIED"

        try {
            ds.connection.use { conn ->
                conn.prepareStatement(UserQuery.UPDATE_EMAIL_VERIFIED).use { stmt ->
                    stmt.setString(1, status)
                    stmt.setObject(2, dateNow)
                    stmt.setInt(3, userId)

                    val rows = stmt.executeUpdate()
                    return if (rows > 0) {
                        VerificationResult.Success("User Email Verification Successful", userId = userId)
                    } else {
                        VerificationResult.NotFound("User Email Verification NotFound")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return VerificationResult.Error("Unknown Error Occurred")
        }
    }
}