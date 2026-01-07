package ru.miet.kvosk.notes

import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain
import ru.miet.kvosk.notes.db.DatabaseFactory

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val config = loadConfig()
    DatabaseFactory.init(config.db)
    configureSerialization()
    configureMonitoring()
    configureHTTP()
    configureRouting()
}
