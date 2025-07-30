package com.marlow.registrationSystem.controllers

import com.marlow.global.GlobalMethods
import com.marlow.global.RegistrationResult
import com.marlow.registrationSystem.dto.RegistrationRequest
import com.marlow.registrationSystem.models.CredentialsModel
import com.marlow.registrationSystem.models.EmailSendingModel
import com.marlow.registrationSystem.models.InformationModel
import com.marlow.registrationSystem.queries.UserQuery
import com.marlow.todo.query.TodoQuery
import com.zaxxer.hikari.HikariDataSource
import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.*
import io.ktor.server.request.*
import java.time.LocalDate

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

            val insertInfo = ds.connection.prepareCall(UserQuery.INSERT_INFORMATION)
            insertInfo.setString(1, information.username)
            insertInfo.setString(2, information.firstName)
            insertInfo.setString(3, information.middleName)
            insertInfo.setString(4, information.lastName)
            insertInfo.setString(5, information.email)
            insertInfo.setObject(6, information.birthday)
            insertInfo.setObject(7, information.createdAt)
            insertInfo.setObject(8, information.updatedAt)
            insertInfo.setString(9, information.roleType)
            insertInfo.setString(10, information.image)
            insertInfo.execute()

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

            val insertCred = ds.connection.prepareCall(UserQuery.INSERT_CREDENTIALS)
            insertCred.setInt(1, credentials.userId)
            insertCred.setString(2, credentials.username)
            insertCred.setString(3, credentials.password)
            insertCred.setBoolean(4, credentials.activeSession)
            insertCred.setBoolean(5, credentials.activeSessionDeleted)
            insertCred.setObject(6, credentials.createdAt)
            insertCred.setObject(7, credentials.updatedAt)
            insertCred.execute()

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
            print("Preparing to send email...")
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
}
