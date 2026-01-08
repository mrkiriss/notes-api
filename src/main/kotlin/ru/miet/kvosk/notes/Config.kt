package ru.miet.kvosk.notes

import io.github.cdimascio.dotenv.Dotenv
import io.ktor.server.application.Application
import java.nio.file.Files
import java.nio.file.Paths

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
    return AppConfig(
        env = readEnv("APP_ENV"),
        db = loadDatabaseConfig(),
    )
}

private fun loadDatabaseConfig(): DatabaseConfig {
    return DatabaseConfig(
        host = readEnv("DB_HOST"),
        port = readEnv("DB_PORT").toInt(),
        name = readEnv("DB_NAME"),
        user = readEnv("DB_USER"),
        password = readEnv("DB_PASSWORD"),
    )
}

private val dotenv: Dotenv by lazy {
    val localPath = Paths.get(".env.local")
    val defaultPath = Paths.get(".env")
    val fileName = when {
        Files.exists(localPath) -> ".env.local"
        Files.exists(defaultPath) -> ".env"
        else -> null
    }
    if (fileName == null) {
        io.github.cdimascio.dotenv.dotenv { ignoreIfMissing = true }
    } else {
        io.github.cdimascio.dotenv.dotenv {
            filename = fileName
            ignoreIfMissing = true
        }
    }
}

fun readEnv(name: String): String {
    return System.getenv(name) ?: dotenv[name]
    ?: throw IllegalStateException("Required environment variable $name is not set")
}
