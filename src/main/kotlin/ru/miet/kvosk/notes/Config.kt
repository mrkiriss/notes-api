package ru.miet.kvosk.notes

import io.ktor.server.application.Application
import io.ktor.server.config.ApplicationConfig

data class AppConfig(
    val env: String,
    val db: DatabaseConfig,
)

data class DatabaseConfig(
    val host: String,
    val port: Int,
    val name: String,
    val user: String,
    val password: String,
)

fun Application.loadConfig(): AppConfig {
    val config = environment.config
    return AppConfig(
        env = config.property("app.env").getString(),
        db = loadDatabaseConfig(config),
    )
}

private fun loadDatabaseConfig(config: ApplicationConfig): DatabaseConfig {
    return DatabaseConfig(
        host = config.property("db.host").getString(),
        port = config.property("db.port").getString().toInt(),
        name = config.property("db.name").getString(),
        user = config.property("db.user").getString(),
        password = config.property("db.password").getString(),
    )
}
