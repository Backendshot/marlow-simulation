package com.marlow.LoginSystem.controller

import com.marlow.LoginSystem.model.LoginModel
import com.marlow.LoginSystem.model.Validator
import com.marlow.LoginSystem.query.LoginQuery
import com.marlow.LoginSystem.util.LoginJWT
import com.marlow.LoginSystem.util.LoginSession
import com.marlow.configuration.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LoginController {
    val connection = Config().connect()

    // for tommorrow create a function to get Brower info
    // and pass it to the audit trail

    suspend fun login(login: LoginModel): LoginModel? = withContext(Dispatchers.IO) {
        val validator = Validator()
        val sanitizeLogin = validator.sanitizeInput(login) 
        val validateLogin = validator.validateLoginInput(sanitizeLogin)

        if (validateLogin.isNotEmpty()) {
            throw IllegalArgumentException("Validation Errors: ${validateLogin.joinToString(", ")}")
        }
        println("Sanitized and Validated Login Input: $sanitizeLogin")

        connection.prepareStatement(LoginQuery.LOGIN_QUERY).use { stmt ->
            stmt.setString(1, login.username)
            stmt.setString(2, login.password)
            val resultSet = stmt.executeQuery()
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

                connection.prepareStatement(LoginQuery.INSERT_AUDIT_QUERY).use { auditStmt ->
                    auditStmt.setInt(1, userId)
                    auditStmt.setString(2, System.currentTimeMillis().toString())
                    auditStmt.setString(3, "Browser Info") 
                    auditStmt.executeUpdate()
                }
                
                return@withContext LoginModel(
                    id = userId,
                    username = login.username,
                    password = login.password,
                    jwt_token = jwtToken,
                    active_session = sessionId,
                    active_session_deleted = false
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