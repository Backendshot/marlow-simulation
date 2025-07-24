package com.marlow.LoginSystem.query

class LoginQuery {
    companion object {
        const val LOGIN_QUERY = "SELECT * FROM tbl_credentials WHERE username = ? AND password = ?"
    }
}