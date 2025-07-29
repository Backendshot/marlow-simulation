package com.marlow.LoginSystem.controller

import com.marlow.LoginSystem.model.AuditModel
import com.marlow.LoginSystem.model.LoginModel
import com.marlow.LoginSystem.model.Validator
import com.marlow.LoginSystem.query.LoginQuery
import com.marlow.LoginSystem.util.LoginJWT
import com.marlow.LoginSystem.util.LoginSession
import com.marlow.configuration.Config
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.sql.Timestamp
import java.time.format.DateTimeFormatter

class LoginController(ds: HikariDataSource) {
    val connection = ds.connection

    suspend fun login(login: LoginModel, browserInfo: String): LoginModel? = withContext(Dispatchers.IO) {
        val validator = Validator()
        val sanitizeLogin = validator.sanitizeInput(login)
        val errorValidate = validator.validateLoginInput(sanitizeLogin)

        if (errorValidate.isNotEmpty()) {
            throw IllegalArgumentException("Validation Errors: ${errorValidate.joinToString(", ")}")
        }

        connection.prepareStatement(LoginQuery.LOGIN_QUERY).use { statement ->
            statement.setString(1, login.username)
            statement.setString(2, login.password)
            val resultSet = statement.executeQuery()
            if (resultSet.next()) {
                val userId = resultSet.getInt("id")
                connection.prepareStatement(LoginQuery.CHECK_EMAIL_STATUS_QUERY).use { statusStmt ->
                    statusStmt.setInt(1, userId)
                    statusStmt.executeQuery().use { statusRs ->
                        if (statusRs.next()) {
                            val status = statusRs.getString("status")
                            if (status.equals("PENDING", ignoreCase = true)) {
                                throw IllegalStateException("Email not verified. Please check your email to verify.")
                            }
                        } else {
                            throw IllegalStateException("No email verification record found for this user.")
                        }
                    }
                }
                val jwtToken = LoginJWT.generateJWT(userId)
                val sessionId = LoginSession.generatedSessionId()

                connection.prepareStatement(LoginQuery.UPDATE_SESSION_QUERY).use { updateStmt ->
                    updateStmt.setString(1, sessionId)
                    updateStmt.setString(2, jwtToken)
                    updateStmt.setInt(3, userId)
                    updateStmt.executeUpdate()
                }
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

                connection.prepareStatement(LoginQuery.INSERT_AUDIT_QUERY).use { auditStmt ->
                    auditStmt.setInt(1, userId)
                    auditStmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()))
                    auditStmt.setString(3, browserInfo)

                    auditStmt.executeQuery().use { rs ->
                        if (rs.next()) {
                            AuditModel(
                                id = rs.getInt("id"),
                                user_id = rs.getInt("user_id"),
                                timestamp = rs.getTimestamp("timestamp").toLocalDateTime().format(formatter),
                                browser = rs.getString("browser")
                            )
                        } else {
                            throw kotlin.Exception("Audit insert failed; no result returned.")
                        }
                    }
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

    suspend fun viewAllAuditById(user_id: Int): MutableList<AuditModel> = withContext(Dispatchers.IO) {
        val auditList = mutableListOf<AuditModel>()
        val query = connection.prepareStatement(LoginQuery.GET_AUDIT_BY_ID_QUERY)
        query.setInt(1, user_id)
        val result = query.executeQuery()
        while (result.next()) {
            val id = result.getInt("id")
            val userId = result.getInt("user_id")
            val timestamp = result.getDate("timestamp").toString()
            val browser = result.getString("browser")
            auditList.add(AuditModel(id, userId, timestamp, browser))
        }
        return@withContext auditList
    }

    suspend fun logout(userId: Int): Boolean = withContext(Dispatchers.IO) {
        connection.prepareStatement(LoginQuery.LOGOUT_SESSION_QUERY).use { stmt ->
            stmt.setInt(1, userId)
            val rowsUpdated = stmt.executeUpdate()
            return@withContext rowsUpdated > 0
        }
    }
}