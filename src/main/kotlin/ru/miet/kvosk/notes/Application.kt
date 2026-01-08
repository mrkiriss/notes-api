package ru.miet.kvosk.notes

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import ru.miet.kvosk.notes.db.DatabaseFactory

fun main(args: Array<String>) {
    val port = readEnv("APP_PORT").toInt()
    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    val config = loadConfig()
    DatabaseFactory.init(config.db)
    configureSerialization()
    configureMonitoring()
    configureHTTP()
    configureRouting()
}
