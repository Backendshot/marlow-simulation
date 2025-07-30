package com.marlow.registrationSystem.queries

class UserQuery {
    companion object {
        const val INSERT_INFORMATION    = "CALL insert_information(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        const val INSERT_CREDENTIALS    = "CALL insert_credentials(?, ?, ?, ?, ?, ?, ?)"
        const val CHECK_USERNAME_EXISTS = "SELECT COUNT(*) AS count FROM tbl_information WHERE username = ?"
        const val GET_USER              = "SELECT id, email FROM tbl_information WHERE username = ?"
        const val INSERT_EMAIL_SENDING  = "CALL insert_email_sending(?, ?, ?, ?, ?, ?, ?, ?, ?)"
    }
}