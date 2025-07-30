package com.marlow.LoginSystem.controller

import com.marlow.LoginSystem.model.AuditModel
import com.marlow.LoginSystem.model.LoginModel
import com.marlow.LoginSystem.model.LoginRequest
import com.marlow.LoginSystem.model.Validator
import com.marlow.LoginSystem.query.LoginQuery
import com.marlow.LoginSystem.util.LoginJWT
import com.marlow.LoginSystem.util.LoginSession
import com.zaxxer.hikari.HikariDataSource
import java.sql.Timestamp
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat

class LoginController(ds: HikariDataSource) {
    val connection = ds.connection

    suspend fun login(login: LoginRequest, browserInfo: String): LoginModel? =
            withContext(Dispatchers.IO) {
                val validator = Validator()
                val sanitizedLogin: LoginRequest = validator.sanitizeInput(login)
                val errors = validator.validateLoginInput(sanitizedLogin)

                if (errors.isNotEmpty()) {
                    throw IllegalArgumentException(
                            "Validation Errors: ${errors.joinToString(", ")}"
                    )
                }

                val userId =
                        connection.prepareStatement(LoginQuery.LOGIN_QUERY).use { stmt ->
                            stmt.setString(1, sanitizedLogin.username)
                            stmt.setString(2, sanitizedLogin.password)
                            stmt.executeQuery().use { rs ->
                                if (rs.next()) rs.getInt("id") else return@withContext null
                            }
                        }

                connection.prepareStatement(LoginQuery.CHECK_EMAIL_STATUS_QUERY).use { stmt ->
                    stmt.setInt(1, userId)
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) {
                            val status = rs.getString("status")
                            if (status.equals("PENDING", ignoreCase = true)) {
                                throw IllegalStateException(
                                        "Email not verified. Please check your email to verify."
                                )
                            }
                            true
                        } else {
                            throw IllegalStateException(
                                    "No email verification record found for this user."
                            )
                        }
                    }
                }

                val jwtToken = LoginJWT.generateJWT(userId)
                val sessionId = LoginSession.generatedSessionId()
                val sessionDeleted = false

                connection.prepareStatement(LoginQuery.UPDATE_SESSION_QUERY).use { stmt ->
                    stmt.setString(1, sessionId)
                    stmt.setString(2, jwtToken)
                    stmt.setBoolean(3, sessionDeleted)
                    stmt.setInt(4, userId)
                    stmt.executeUpdate()
                }

                connection.prepareStatement(LoginQuery.INSERT_AUDIT_QUERY).use { stmt ->
                    stmt.setInt(1, userId)
                    stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()))
                    stmt.setString(3, browserInfo)

                    stmt.executeQuery().use { rs ->
                        if (!rs.next()) {
                            throw Exception("Audit insert failed; no result returned.")
                        }
                    }
                }

                return@withContext LoginModel(
                        id = userId,
                        username = sanitizedLogin.username,
                        password = sanitizedLogin.password,
                        jwt_token = jwtToken,
                        active_session = sessionId,
                        active_session_deleted = sessionDeleted
                )
            }

    suspend fun viewAllAuditById(user_id: Int): MutableList<AuditModel> =
            withContext(Dispatchers.IO) {
                val auditList = mutableListOf<AuditModel>()
                val query = connection.prepareStatement(LoginQuery.GET_AUDIT_BY_ID_QUERY)
                query.setInt(1, user_id)
                val result = query.executeQuery()

                val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm")

                while (result.next()) {
                    val id = result.getInt("id")
                    val userId = result.getInt("user_id")
                    val timestampRaw = result.getTimestamp("timestamp")
                    val timestamp = formatter.format(timestampRaw)
                    val browser = result.getString("browser")
                    auditList.add(AuditModel(id, userId, timestamp, browser))
                }

                return@withContext auditList
            }

    suspend fun logout(id: Int): Boolean =
            withContext(Dispatchers.IO) {
                connection.prepareStatement(LoginQuery.LOGOUT_SESSION_QUERY).use { stmt ->
                    stmt.setInt(1, id)
                    val rowsUpdated = stmt.executeUpdate()
                    return@withContext rowsUpdated > 0
                }
            }
}
