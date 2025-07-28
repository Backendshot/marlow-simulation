package com.marlow.registrationSystem.controllers

import com.marlow.configuration.Config
import com.marlow.global.GlobalMethods
import com.marlow.global.RegistrationResult
import com.marlow.registrationSystem.dto.RegistrationRequest
import com.marlow.registrationSystem.models.CredentialsModel
import com.marlow.registrationSystem.models.InformationModel
import com.marlow.registrationSystem.queries.UserQuery
import com.marlow.todo.query.TodoQuery
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.*
import io.ktor.server.request.*
import java.sql.Connection
import java.time.LocalDate
import de.mkammerer.argon2.Argon2Factory
import java.io.File
import java.util.UUID

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
                        if (part.name == "image") {
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
                birthday   = formFields["birthday"],
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
                birthday   = input.birthday?.let { LocalDate.parse(it) },
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

            val userId = methods.getUserIdByUsername(ds.connection, information.username)
                ?: return RegistrationResult.Failure("Failed to retrieve new user ID.")

            val credentials = CredentialsModel(
                userId    = userId,
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

            val accessToken = methods.getAccessToken()
            methods.sendEmail(
                recipient = information.email,
                subject   = "Welcome to our app, ${information.firstName}!",
                body      = """
                     Hello ${information.firstName},
                        Your registration was successful. Welcome aboard!
                        Regards,  
                        The Team
                """.trimIndent(),
                accessToken = accessToken
            )

            // TODO: Test the email sending
            // TODO: Add saving inside the tbl_email_sending table

            RegistrationResult.Success("User registered successfully.")
        } catch (e: Exception) {
            RegistrationResult.Failure("Internal server error: ${e.message}")
        }
    }
}
