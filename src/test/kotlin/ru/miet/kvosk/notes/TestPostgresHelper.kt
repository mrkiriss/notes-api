package ru.miet.kvosk.notes

import org.junit.jupiter.api.Assumptions.assumeTrue

data class TestDbConfig(
    val jdbcUrl: String,
    val username: String,
    val password: String,
    val host: String,
    val port: Int,
    val database: String,
)

object TestPostgresHelper {
    fun startOrSkip(): TestDbConfig {
        val enabled = System.getenv("RUN_INTEGRATION_TESTS") == "true"
        assumeTrue(enabled, "Set RUN_INTEGRATION_TESTS=true to enable integration tests")
        return externalConfig()
    }

    fun stop() {
        // no-op for external database
    }

    private fun externalConfig(): TestDbConfig {
        val host = System.getenv("IT_DB_HOST") ?: error("IT_DB_HOST is required")
        val port = System.getenv("IT_DB_PORT")?.toIntOrNull() ?: 5432
        val database = System.getenv("IT_DB_NAME") ?: "notes"
        val username = System.getenv("IT_DB_USER") ?: "notes"
        val password = System.getenv("IT_DB_PASSWORD") ?: "notes"
        val jdbcUrl = "jdbc:postgresql://$host:$port/$database"
        return TestDbConfig(
            jdbcUrl = jdbcUrl,
            username = username,
            password = password,
            host = host,
            port = port,
            database = database,
        )
    }
}
