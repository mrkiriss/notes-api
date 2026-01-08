package ru.miet.kvosk.notes

import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test

class ApplicationTest {
    @Test
    fun testRoot() {
        testApplication {
        }
    }
}
