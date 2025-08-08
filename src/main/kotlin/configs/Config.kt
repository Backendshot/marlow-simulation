package com.marlow.configs

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.cdimascio.dotenv.dotenv
import java.io.PrintWriter
import java.sql.Connection
import java.sql.DriverManager
import java.util.*


class Config {
    fun connect(): Connection {
        val dotenv   = dotenv()
        val database = dotenv["DB_DATABASE"]
        val hostname = dotenv["DB_HOSTNAME"]
        val port     = dotenv["DB_PORT"]
        val dbName   = dotenv["DB_NAME"]
        val url      = "jdbc:$database://$hostname:$port/$dbName"
        val username = dotenv["DB_USERNAME"]
        val password = dotenv["DB_PASSWORD"]

        return DriverManager.getConnection(url, username, password)
    }

    fun getConnection(): HikariDataSource {
        val dotenv   = dotenv()
        val database = dotenv["DB_DATABASE"]
        val hostname = dotenv["DB_HOSTNAME"]
        val port     = dotenv["DB_PORT"] //"5432" //pgBouncer's port address
        val dbName   = dotenv["DB_NAME"]
        val username = dotenv["DB_USERNAME"]
        val password = dotenv["DB_PASSWORD"]
        val url      = dotenv["DB_URL"]
        HikariDataSource().also {
            // set the hikari basic config
            it.jdbcUrl = url
            // set the username
            it.username = username
            // set the password
            it.password = password

            it.poolName = "Marlow Simulation"

            it.maximumPoolSize = 15

            it.minimumIdle = 1

            it.idleTimeout = 60000 //formerly 1000, but it is now a value that is >= 10s, such as 1 minute

            it.maxLifetime = 3500000 //formerly 1000, now >= 30s, few seconds (3500 seconds) below PgBouncer max lifetime (3600s)

            it.leakDetectionThreshold = 60_000 //register leak after 60 seconds has passed and the connection hasn't been closed

            it.connectionTimeout = 30000 //formerly 1000, now waits 30s to acquire connection

            it.connectionTestQuery = "SELECT 1"

            it.leakDetectionThreshold = 2000

            it.logWriter = PrintWriter(System.out)

            return it
        }
    }

    fun createDataSource(): HikariDataSource {
        val dotenv   = dotenv()
        val hostname = dotenv["DB_HOSTNAME"]
        val port     = dotenv["DB_PORT"] //"5432" //pgBouncer's port address
        val dbName   = dotenv["DB_NAME"]
        val username = dotenv["DB_USERNAME"]
        val password = dotenv["DB_PASSWORD"]

        val props = Properties()
        props.setProperty("dataSourceClassName", "org.postgresql.ds.PGSimpleDataSource") //this or jdbcUrl
        props.setProperty("dataSource.user", username)
        props.setProperty("dataSource.password", password)
        props.setProperty("dataSource.serverName", hostname)
        props.setProperty("dataSource.portNumber", port)
        props.setProperty("dataSource.databaseName", dbName)
        //Tune to match PgBouncer capacity
        props["dataSource.logWriter"] = PrintWriter(System.out)

        val config = HikariConfig(props)
        val ds = HikariDataSource(config)
        return ds
    }
}