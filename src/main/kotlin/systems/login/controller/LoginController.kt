package com.marlow.systems.login.controller

import com.marlow.systems.login.model.AuditModel
import com.marlow.systems.login.model.LoginModel
import com.marlow.systems.login.query.LoginQuery
import com.zaxxer.hikari.HikariDataSource
import java.sql.Timestamp
import java.text.SimpleDateFormat
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
        ds.connection.use { conn ->
            conn.prepareStatement(LoginQuery.CHECK_EMAIL_STATUS_QUERY).use { stmt ->
                stmt.setInt(1, userIdParam)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        val status = rs.getString("status")
                        println(status)
                        return !status.equals(
                            "PENDING", ignoreCase = true
                        ) //return true if the status is Verified and false if the status is Pending
                    }
                    println("if not executed")
                }
            }
        }
        return false
    }

    fun updateSession(
        userIdParam: Int, sessionIdParam: String, jwtTokenParam: String, sessionDeletedParam: Boolean
    ) {
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
        jwtTokenParam: String,
        activeSessionParam: String,
        activeSessionDeletedParam: Boolean,
    ): LoginModel {
        return LoginModel(
            userId = userIdParam,
            username = usernameParam,
            password = passwordParam,
            jwtToken = jwtTokenParam,
            activeSession = activeSessionParam,
            activeSessionDeleted = activeSessionDeletedParam
        )
    }

    fun insertAudit(userIdParam: Int, browserInfoParam: String) {
        ds.connection.use { conn ->
            conn.prepareStatement(LoginQuery.INSERT_AUDIT_QUERY).use { stmt ->
                stmt.setInt(1, userIdParam)
                stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()))
                stmt.setString(3, browserInfoParam)
                stmt.execute()
            }
        }
    }

    fun viewAllAuditById(userIdParam: Int): MutableList<AuditModel> {
        val auditList = mutableListOf<AuditModel>()
        ds.connection.use { conn ->
            conn.prepareStatement(LoginQuery.GET_AUDIT_BY_ID_QUERY).use { stmt ->
                stmt.setInt(1, userIdParam)
                val result = stmt.executeQuery()
                val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm")
                while (result.next()) {
                    val id = result.getInt("id")
                    val userId = result.getInt("user_id")
                    val timestampRaw = result.getTimestamp("timestamp")
                    val timestamp = formatter.format(timestampRaw)
                    val browser = result.getString("browser")
                    auditList.add(AuditModel(id, userId, timestamp, browser))
                }
            }
        }
        return auditList
    }

    fun logout(userIdParam: Int): Boolean {
        ds.connection.use { conn ->
            conn.prepareStatement(LoginQuery.LOGOUT_SESSION_QUERY).use { stmt ->
                stmt.setInt(1, userIdParam)
                val rowsUpdated = stmt.executeUpdate()
                return rowsUpdated > 0
            }
        }
    }
}
