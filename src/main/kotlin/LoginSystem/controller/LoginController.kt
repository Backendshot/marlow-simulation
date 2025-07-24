package com.marlow.LoginSystem.controller

import com.marlow.configuration.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LoginController {
    val connection = Config().connect()

    suspend fun login(username: String, password: String): Boolean {
        return withContext(Dispatchers.IO) {
            val query = "SELECT * FROM users WHERE username = ? AND password = ?"
            connection.prepareStatement(query).use { stmt ->
                stmt.setString(1, username)
                stmt.setString(2, password)
                val resultSet = stmt.executeQuery()
                resultSet.next() 
            }
        }
    }

}