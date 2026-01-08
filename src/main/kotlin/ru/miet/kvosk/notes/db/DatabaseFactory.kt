package ru.miet.kvosk.notes.db

import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import ru.miet.kvosk.notes.DatabaseConfig

object DatabaseFactory {
    fun init(config: DatabaseConfig) {
        val jdbcUrl = "jdbc:postgresql://${config.host}:${config.port}/${config.name}"
        Database.connect(
            url = jdbcUrl,
            driver = "org.postgresql.Driver",
            user = config.user,
            password = config.password,
        )

        val flyway =
            Flyway.configure()
                .dataSource(jdbcUrl, config.user, config.password)
                .locations("classpath:db/migration")
                .validateMigrationNaming(true)
                .load()
        flyway.migrate()
    }
}

fun <T> dbQuery(block: () -> T): T = transaction { block() }
