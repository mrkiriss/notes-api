package ru.miet.kvosk.notes.http

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.miet.kvosk.notes.configureRouting
import ru.miet.kvosk.notes.configureSerialization

class ApiErrorHandlingTest {
    @Test
    fun `invalid json returns 400`() {
        testApplication {
            application {
                configureSerialization()
                configureRouting()
            }
            val response =
                client.post("/api/v1/tags") {
                    contentType(ContentType.Application.Json)
                    setBody("{name:home}")
                }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `invalid uuid returns 400`() {
        testApplication {
            application {
                configureSerialization()
                configureRouting()
            }
            val response = client.get("/api/v1/notes/not-a-uuid")
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `invalid tag ids in search returns 400`() {
        testApplication {
            application {
                configureSerialization()
                configureRouting()
            }
            val response =
                client.post("/api/v1/notes:search") {
                    contentType(ContentType.Application.Json)
                    setBody("{\"filter\":{\"tag_ids\":[\"not-a-uuid\"]}}")
                }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }
}
