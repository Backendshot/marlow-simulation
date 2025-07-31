package com.marlow.systems.login.controller

import com.marlow.systems.login.model.AuditModel
import com.marlow.systems.login.model.LoginModel
import com.marlow.systems.login.model.LoginRequest
import com.marlow.systems.login.model.Validator
import com.marlow.systems.login.query.LoginQuery
import com.marlow.systems.login.util.LoginJWT
import com.marlow.systems.login.util.LoginSession
import com.zaxxer.hikari.HikariDataSource
import de.mkammerer.argon2.Argon2Factory
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoginController(ds: HikariDataSource) {
    val connection = ds.connection
    val argon2 = Argon2Factory.create()

    suspend fun login(login: LoginRequest, browserInfo: String): LoginModel? =
            withContext(Dispatchers.IO) {
                val validator = Validator()
                val sanitizedLogin = validator.sanitizeInput(login)
                val errors = validator.validateLoginInput(sanitizedLogin)

                if (errors.isNotEmpty()) {
                    throw IllegalArgumentException(
                            "Validation Errors: ${errors.joinToString(", ")}"
                    )
                }

                val (userId, storedHash) =
                        connection.prepareStatement(LoginQuery.GET_USER_PASS_BY_USERNAME).use { stmt
                            ->
                            stmt.setString(1, sanitizedLogin.username)
                            stmt.executeQuery().use { rs ->
                                if (rs.next()) {
                                    val id = rs.getInt("id")
                                    val hash = rs.getString("password")
                                    id to hash
                                } else {
                                    throw IllegalArgumentException("Invalid username or password.")
                                }
                            }
                        }

                val isPasswordValid =
                        argon2.verify(storedHash, sanitizedLogin.password.toCharArray())
                if (!isPasswordValid) {
                    throw IllegalArgumentException("Invalid username or password.")
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
                    stmt.execute()
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
