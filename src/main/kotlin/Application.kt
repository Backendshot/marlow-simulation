package com.marlow

import com.marlow.todo.plugin.configureHTTP
import com.marlow.todo.plugin.configureRouting
import com.marlow.todo.plugin.configureSecurity
import com.marlow.todo.plugin.configureSerialization
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSerialization()
    configureSecurity()
    configureHTTP()
//    configureDatabases()
    configureRouting()
}
