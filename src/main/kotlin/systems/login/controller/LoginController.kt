package com.marlow.systems.login.controller

import com.marlow.systems.login.model.*
import com.marlow.systems.login.query.*
import com.zaxxer.hikari.HikariDataSource
import java.sql.Timestamp
import java.time.LocalDateTime

class LoginController(private val ds: HikariDataSource) {

    fun getUserIdAndHash(usernameParam: String): Pair<Int, String>? {
        ds.connection.use { con ->
            con.prepareStatement(LoginQuery.GET_USER_PASS_BY_USERNAME).use { stmt ->
                stmt.setString(1, usernameParam)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) {
                        rs.getInt("user_id") to rs.getString("password")
                    } else null
                }
            }
        }
    }

    fun checkEmailStatus(userIdParam: Int): Boolean {
        ds.connection.use { con ->
            con.prepareStatement(LoginQuery.CHECK_EMAIL_STATUS_QUERY).use { stmt ->
                stmt.setInt(1, userIdParam)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        val status = rs.getString("status")
                        return !status.equals("PENDING", ignoreCase = true)
                    }
                }
            }
        }
        return false
    }

    fun updateSession(userIdParam: Int, sessionIdParam: String, jwtTokenParam: String, sessionDeletedParam: Boolean) {
        ds.connection.use { con ->
            con.prepareStatement(LoginQuery.UPDATE_SESSION_QUERY).use { stmt ->
                stmt.setString(1, sessionIdParam)
                stmt.setString(2, jwtTokenParam)
                stmt.setBoolean(3, sessionDeletedParam)
                stmt.setInt(4, userIdParam)
                stmt.executeUpdate()
            }
        }
    }

    fun loginResponse(
        userIdParam: Int,
        usernameParam: String,
        passwordParam: String,
        jwtParam: String,
        a_sessionParam: String,
        a_session_deletedParam: Boolean,
        ): LoginModel{
        return LoginModel(
            user_id = userIdParam,
            username = usernameParam,
            password = passwordParam,
            jwt_token = jwtParam,
            active_session = a_sessionParam,
            active_session_deleted = a_session_deletedParam
        )
    }

    fun insertAudit(userId: Int, browserInfo: String) {
        ds.connection.use { con ->
            con.prepareStatement(LoginQuery.INSERT_AUDIT_QUERY).use { stmt ->
                stmt.setInt(1, userId)
                stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()))
                stmt.setString(3, browserInfo)
                stmt.execute()
            }
        }
    }

    fun viewAllAuditById(userIdParam: Int): List<AuditModel> {
        ds.connection.use { con ->
            con.prepareStatement(LoginQuery.GET_AUDIT_BY_ID_QUERY).use { stmt ->
                stmt.setInt(1, userIdParam)
                stmt.executeQuery().use { data ->
                    val auditList = mutableListOf<AuditModel>()
                    while (data.next()) {
                        val id = data.getInt("id")
                        val userId = data.getInt("user_id")
                        val timestamp = data.getString("timestamp")
                        val browser = data.getString("browser")
                        auditList.add(AuditModel(id, userId, timestamp, browser))
                    }
                    return auditList
                }
            }
        }
    }

    fun logout(userIdParam: Int): Boolean {
        ds.connection.use { con ->
            con.prepareStatement(LoginQuery.LOGOUT_SESSION_QUERY).use { stmt ->
                stmt.setInt(1, userIdParam)
                val rowsUpdated = stmt.executeUpdate()
                return rowsUpdated > 0
            }
        }
    }
}