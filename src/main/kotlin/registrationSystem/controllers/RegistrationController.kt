package com.marlow.registrationSystem.controllers

import com.marlow.configuration.Config
import com.marlow.global.RegistrationResult
import com.marlow.registrationSystem.dto.RegistrationRequest
import com.marlow.registrationSystem.models.CredentialsModel
import com.marlow.registrationSystem.models.InformationModel
import com.marlow.registrationSystem.queries.UserQuery
import io.ktor.server.application.*
import io.ktor.server.request.*
import java.sql.Connection
import java.time.LocalDate
import org.mindrot.jbcrypt.BCrypt

class RegistrationController {
    val connection = Config().connect()
    suspend fun register(call: ApplicationCall): RegistrationResult {
        return try {
            val input = call.receive<RegistrationRequest>()
            val now   = LocalDate.now()

            val information = InformationModel(
                username   = input.username,
                firstName  = input.firstName,
                middleName = input.middleName,
                lastName   = input.lastName,
                roleType   = input.roleType,
                email      = input.email,
                birthday   = input.birthday?.let { LocalDate.parse(it) },
                createdAt  = now,
                updatedAt  = now
            ).sanitized()

            // Validate information
            val infoErrors = information.validate()
            if (infoErrors.isNotEmpty()) {
                return RegistrationResult.ValidationError("Information validation failed.")
            }

            if (input.password.isBlank() || input.password.length < 8) {
                return RegistrationResult.ValidationError("Password must be at least 8 characters.")
            }

            // Check if username exists
            val checkStmt = connection.prepareStatement(UserQuery.CHECK_USERNAME_EXISTS)
            checkStmt.setString(1, information.username)
            val result = checkStmt.executeQuery()
            if (result.next() && result.getInt("count") > 0) {
                return RegistrationResult.Conflict("Username already exists.")
            }

            // Insert information
            val insertInfo = connection.prepareCall(UserQuery.INSERT_INFORMATION)
            insertInfo.setString(1, information.username)
            insertInfo.setString(2, information.firstName)
            insertInfo.setString(3, information.middleName)
            insertInfo.setString(4, information.lastName)
            insertInfo.setString(5, information.email)
            insertInfo.setObject(6, information.birthday)
            insertInfo.setObject(7, information.createdAt)
            insertInfo.setObject(8, information.updatedAt)
            insertInfo.setString(9, information.roleType)
            insertInfo.execute()

            val userId = getUserIdByUsername(connection, information.username)
                ?: return RegistrationResult.Failure("Failed to retrieve new user ID.")

            val credentials = CredentialsModel(
                userId = userId,
                username = information.username,
                password = hashPassword(input.password),
                createdAt = now,
                updatedAt = now
            ).sanitized()

            val credErrors = credentials.validate()
            if (credErrors.isNotEmpty()) {
                return RegistrationResult.ValidationError("Credentials validation failed.")
            }

            val insertCred = connection.prepareCall(UserQuery.INSERT_CREDENTIALS)
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
        val stmt = connection.prepareStatement("SELECT id FROM tbl_information WHERE username = ?")
        stmt.setString(1, username)
        val result = stmt.executeQuery()
        return if (result.next()) result.getInt("id") else null
    }

    private fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }
}
