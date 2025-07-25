package com.marlow.LoginSystem.controller

import com.marlow.LoginSystem.model.LoginModel
import com.marlow.LoginSystem.model.Validator
import com.marlow.LoginSystem.model.LoginAuditResponse
import com.marlow.LoginSystem.query.LoginQuery
import com.marlow.LoginSystem.util.LoginJWT
import com.marlow.LoginSystem.util.LoginSession
import com.marlow.configuration.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object LoginController {
    val connection = Config().connect()

    suspend fun login(login: LoginModel, browserInfo: String): LoginModel? = withContext(Dispatchers.IO) {
        val validator = Validator()
        val sanitizeLogin = validator.sanitizeInput(login)
        val validateLogin = validator.validateLoginInput(sanitizeLogin)

        if (validateLogin.isNotEmpty()) {
            throw IllegalArgumentException("Validation Errors: ${validateLogin.joinToString(", ")}")
        }

        connection.prepareStatement(LoginQuery.LOGIN_QUERY).use { statement ->
            statement.setString(1, login.username)
            statement.setString(2, login.password)
            val resultSet = statement.executeQuery()
            if (resultSet.next()) {
                val userId = resultSet.getInt(login.id)
                val jwtToken = LoginJWT.generateJWT(userId)
                val sessionId = LoginSession.generatedSessionId()

                connection.prepareStatement(LoginQuery.UPDATE_SESSION_QUERY).use { updateStmt ->
                    updateStmt.setString(1, sessionId)
                    updateStmt.setString(2, jwtToken)
                    updateStmt.setInt(3, userId)
                    updateStmt.executeUpdate()
                }

                val currentDateTime = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("-MM-dd-yyyy HH:mm:ss")
                val formattedDateTime = currentDateTime.format(formatter)

                val auditModel = connection.prepareStatement(LoginQuery.INSERT_AUDIT_QUERY).use { auditStmt ->
                    auditStmt.setInt(1, userId)
                    auditStmt.setString(2, formattedDateTime)
                    auditStmt.setString(3, browserInfo)
                    auditStmt.executeQuery()
                }

                val loginModel = LoginModel(
                    id = userId,
                    username = login.username,
                    password = login.password,
                    jwt_token = jwtToken,
                    active_session = sessionId,
                    active_session_deleted = false
                )

                val loginAuditResponse = LoginAuditResponse(
                    login = loginModel,
                    audit = auditModel
                )
            }
        }
        return@withContext null
    }

    suspend fun logout(userId: Int): Boolean = withContext(Dispatchers.IO) {
        connection.prepareStatement(LoginQuery.LOGOUT_SESSION_QUERY).use { stmt ->
            stmt.setInt(1, userId)
            val rowsUpdated = stmt.executeUpdate()
            return@withContext rowsUpdated > 0
        }
    }

}