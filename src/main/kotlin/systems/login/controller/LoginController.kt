package com.marlow.systems.login.controller

import com.marlow.systems.login.model.*
import com.marlow.systems.login.query.*
import com.zaxxer.hikari.HikariDataSource
import java.net.URLEncoder
import java.sql.SQLException
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.LocalDateTime

class LoginController(private val ds: HikariDataSource) {

    fun getUserIdAndHash(usernameParam: String): Pair<Int, String>? {
        ds.connection.use { con ->
            con.prepareStatement(LoginQuery.GET_USER_PASS_BY_USERNAME).use { stmt ->
                stmt.apply {setString(1, usernameParam)}
                    .executeQuery().use { rs ->
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
                stmt.apply {setInt(1, userIdParam)}
                    .executeQuery().use { rs ->
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
        ds.connection.use { conn ->
            conn.prepareStatement(LoginQuery.UPDATE_SESSION_QUERY).use { stmt ->
                stmt.apply {
                    setString(1, sessionIdParam)
                    setString(2, jwtTokenParam)
                    setBoolean(3, sessionDeletedParam)
                    setInt(4, userIdParam)
                    executeUpdate()
                }
            }
        }
    }

    fun loginResponse(
        userId: Int,
        username: String,
        jwtToken: String,
        activeSession: String,
        activeSessionDeleted: Boolean
    ): LoginModel = LoginModel(
        userId = userId,
        username = username,
        jwtToken = jwtToken,
        activeSession = activeSession,
        activeSessionDeleted = activeSessionDeleted
    )

    fun insertAudit(userId: Int, browserInfo: String) {
        ds.connection.use { conn ->
            conn.prepareStatement(LoginQuery.INSERT_AUDIT_QUERY).use { stmt ->
                stmt.apply {
                    setInt(1, userId)
                    setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()))
                    setString(3, browserInfo)
                    execute()
                }
            }
        }
    }

    fun getUserProfile(userId: Int): UserProfileImage {
        var fileName = ""
        ds.connection.use { con ->
            con.prepareStatement(LoginQuery.GET_IMAGE_QUERY).use { stmt ->
                stmt.setInt(1, userId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        fileName = rs.getString("image") ?: ""
                    }
                }
            }
        }
        if (fileName.isBlank()) fileName = "error"

        val fullUrl = "http://127.0.0.1:8080/image_uploads/${URLEncoder.encode(fileName, "UTF-8")}"
        return UserProfileImage(fullUrl)
    }

    fun patchUserProfile(userIdParam: Int, imgFileParam: String): Boolean {
        return try {
            ds.connection.use { con ->
                con.prepareStatement(LoginQuery.UPDATE_PROFILE_QUERY).use { stmt ->
                    stmt.setString(1, imgFileParam)
                    stmt.setInt(2, userIdParam)
                    stmt.executeUpdate() > 0
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            false
        }
    }
    fun getCurrentUserImage(userId: Int): String? {
        return try {
            ds.connection.use { con ->
                con.prepareStatement(LoginQuery.SELECT_CURRENT_USER_IMG).use { stmt ->
                    stmt.setInt(1, userId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) rs.getString("image") else null
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            null
        }
    }

    fun viewAllAuditById(userIdParam: Int): List<AuditModel> {
        ds.connection.use { conn ->
            conn.prepareStatement(LoginQuery.GET_AUDIT_BY_ID_QUERY).use { stmt ->
                stmt.apply {setInt(1, userIdParam)}
                    .executeQuery().use { data ->
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
        ds.connection.use { conn ->
            conn.prepareStatement(LoginQuery.LOGOUT_SESSION_QUERY).use { stmt ->
                stmt.setInt(1, userIdParam)
                val rowsUpdated = stmt.executeUpdate()
                return rowsUpdated > 0
            }
        }
    }
}
