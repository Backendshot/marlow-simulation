package com.marlow.systems.login.query

class LoginQuery {
    companion object {
        const val LOGIN_QUERY = "SELECT * FROM tbl_credentials WHERE username = ? AND password = ?"
        const val GET_USER_PASS_BY_USERNAME = "SELECT id, password FROM tbl_credentials WHERE username = ?"
        const val INSERT_JWT_QUERY = "UPDATE tbl_credentials SET jwt_token = ? WHERE username = ? AND password = ?"
        const val UPDATE_SESSION_QUERY = "UPDATE tbl_credentials SET active_session = ?, jwt_token = ?, active_session_deleted = ? WHERE id = ?"
        const val LOGOUT_SESSION_QUERY = "UPDATE tbl_credentials SET active_session_deleted = TRUE WHERE id = ?"
        const val INSERT_AUDIT_QUERY = "INSERT INTO audit_trail (user_id, timestamp, browser) VALUES (?, ?, ?) RETURNING id, user_id, timestamp, browser"
        const val GET_AUDIT_BY_ID_QUERY = "SELECT * FROM audit_trail WHERE user_id = ? ORDER BY timestamp DESC"
        const val CHECK_EMAIL_STATUS_QUERY = "SELECT status FROM tbl_email_sending WHERE user_id = ?"
        const val GET_BEARER_TOKEN = "SELECT jwt_token FROM tbl_credentials WHERE id = ?"
    }
}