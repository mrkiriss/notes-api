package ru.miet.kvosk.notes.db

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import ru.miet.kvosk.notes.DatabaseConfig
import org.flywaydb.core.Flyway

object DatabaseFactory {
    fun init(config: DatabaseConfig) {
        Database.connect(
            url = "jdbc:postgresql://${config.host}:${config.port}/${config.name}",
            driver = "org.postgresql.Driver",
            user = config.user,
            password = config.password,
        )

        val flyway = Flyway.configure()
            .dataSource(
                "jdbc:postgresql://${config.host}:${config.port}/${config.name}",
                config.user,
                config.password,
            )
            .load()
        flyway.migrate()
    }
}

fun <T> dbQuery(block: () -> T): T = transaction { block() }
