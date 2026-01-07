package ru.miet.kvosk.notes.db

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import ru.miet.kvosk.notes.DatabaseConfig

object DatabaseFactory {
    fun init(config: DatabaseConfig) {
        Database.connect(
            url = "jdbc:postgresql://${config.host}:${config.port}/${config.name}",
            driver = "org.postgresql.Driver",
            user = config.user,
            password = config.password,
        )
    }
}

fun <T> dbQuery(block: () -> T): T = transaction { block() }
