package com.marlow.registrationSystem.controllers

import com.marlow.configuration.Config
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
            val multipart = call.receiveMultipart()
            val formFields = mutableMapOf<String, String>()
            var imageFileName: String? = null
            val now = LocalDate.now()

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        formFields[part.name.orEmpty()] = part.value
                    }
                    is PartData.FileItem -> {
                        if (part.name == "image") {
                            imageFileName = saveImage(part)
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

            val userId = getUserIdByUsername(ds.connection, information.username)
                ?: return RegistrationResult.Failure("Failed to retrieve new user ID.")

            val credentials = CredentialsModel(
                userId    = userId,
                username  = information.username,
                password  = hashPassword(input.password),
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

            RegistrationResult.Success("User registered successfully.")
        } catch (e: Exception) {
            RegistrationResult.Failure("Internal server error: ${e.message}")
        }
    }

    private fun getUserIdByUsername(connection: Connection, username: String): Int? {
        val userId = connection.prepareCall(UserQuery.GET_USER_ID)
        userId.setString(1, username)
        val result = userId.executeQuery()
        return if (result.next()) result.getInt("id") else null
    }

    private fun hashPassword(password: String): String {
        val hasher = Argon2Factory.create()

        return hasher.hash(2, 65536, 1, password.toCharArray())
    }

    private fun saveImage(part: PartData.FileItem): String {
        val allowedExtensions = listOf("jpg", "jpeg", "png", "webp")

        val originalName      = part.originalFileName ?: ""
        val extension         = File(originalName).extension.lowercase()

        if (extension !in allowedExtensions) {
            throw IllegalArgumentException("Invalid image type: .$extension is not allowed.")
        }

        val fileName = UUID.randomUUID().toString() + "." + extension
        val filePath = "image_uploads/$fileName"

        File(filePath).apply {
            parentFile.mkdirs()
            outputStream().use { part.streamProvider().copyTo(it) }
        }

        return fileName
    }
}
