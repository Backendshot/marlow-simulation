package com.marlow.registrationSystem.queries

class UserQuery {
    companion object {
        const val INSERT_INFORMATION    = "CALL insert_information(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        const val INSERT_CREDENTIALS    = "CALL insert_credentials(?, ?, ?, ?, ?, ?, ?)"
        const val CHECK_USERNAME_EXISTS = "SELECT COUNT(*) AS count FROM tbl_information WHERE username = ?"
        const val GET_USER_ID           = "SELECT id FROM tbl_information WHERE username = ?"
    }
}