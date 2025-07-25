package com.marlow.LoginSystem.query

class LoginQuery {
    companion object {
        const val LOGIN_QUERY = "SELECT * FROM tbl_credentials WHERE username = ? AND password = ?"
        const val INSERT_JWT_QUERY = "UPDATE tbl_credentials SET jwt_token = ? WHERE username = ? AND password = ?"
        const val UPDATE_SESSION_QUERY = "UPDATE tbl_credentials SET active_session = ?, jwt_token = ? WHERE id = ?"
        const val LOGOUT_SESSION_QUERY = "UPDATE tbl_credentials SET active_session_deleted = TRUE WHERE id = ?"
        const val INSERT_AUDIT_QUERY = "INSERT INTO audit_trail (user_id, timestamp, browser) VALUES (?, ?, ?) RETURNING id, user_id, timestamp, browser"
        //const val GET_AUDIT_QUERY = "SELECT * FROM audit_trail ORDER BY timestamp DESC"
        const val GET_AUDIT_BY_ID_QUERY = "SELECT * FROM audit_trail WHERE user_id = ? ORDER BY timestamp DESC"
    }
}