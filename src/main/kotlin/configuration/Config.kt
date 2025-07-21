package com.marlow.configuration

import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.application.*
import java.sql.Connection
import java.sql.DriverManager

class Config {
    //TODO: HikariCP and pgBouncer
    fun connect(): Connection {
        val dotenv = dotenv()
        val database = dotenv["DB_DATABASE"]
        val hostname = dotenv["DB_HOSTNAME"]
        val port = dotenv["DB_PORT"]
        val dbName = dotenv["DB_NAME"]
        val url = "jdbc:$database://$hostname:$port/$dbName"
//
        val username = dotenv["DB_USERNAME"]
        val password = dotenv["DB_PASSWORD"]

        println("Connecting to postgres database at $url, with username $username and password $password")
        return DriverManager.getConnection(url, username, password)
    }
}