package ru.miet.kvosk.notes.integration

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import ru.miet.kvosk.notes.TestDbConfig
import ru.miet.kvosk.notes.TestPostgresHelper
import ru.miet.kvosk.notes.configureHTTP
import ru.miet.kvosk.notes.configureRouting
import ru.miet.kvosk.notes.configureSerialization
import ru.miet.kvosk.notes.db.DatabaseFactory
import ru.miet.kvosk.notes.loadConfig

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiIntegrationTest {
    private var dbConfig: TestDbConfig? = null

    @AfterAll
    fun cleanup() {
        TestPostgresHelper.stop()
    }

    @Test
    fun `create tag and note and fetch by id`() {
        startTestAppWithDb { client ->
            val tagName = "home-${java.util.UUID.randomUUID()}"
            val tagResponse =
                client.post("/api/v1/tags") {
                    contentType(ContentType.Application.Json)
                    setBody("{\"name\":\"$tagName\"}")
                }
            val tagBody = tagResponse.bodyAsText()
            assertEquals(HttpStatusCode.Created, tagResponse.status, "tag response: $tagBody")
            val tagId = extractId(tagBody)
            assertNotNull(tagId)

            val noteResponse =
                client.post("/api/v1/notes") {
                    contentType(ContentType.Application.Json)
                    setBody("{\"title\":\"Buy\",\"content\":\"Milk\",\"tag_ids\":[\"$tagId\"]}")
                }
            val noteBody = noteResponse.bodyAsText()
            assertEquals(HttpStatusCode.Created, noteResponse.status, "note response: $noteBody")
            val noteId = extractId(noteBody)
            assertNotNull(noteId)

            val getResponse = client.get("/api/v1/notes/$noteId?include=tags")
            assertEquals(HttpStatusCode.OK, getResponse.status)
        }
    }

    @Test
    fun `search returns created note`() {
        startTestAppWithDb { client ->
            val noteResponse =
                client.post("/api/v1/notes") {
                    contentType(ContentType.Application.Json)
                    setBody("{\"title\":\"Eggs\",\"content\":\"Eggs and milk\",\"tag_ids\":[]}")
                }
            assertEquals(HttpStatusCode.Created, noteResponse.status)

            val searchResponse =
                client.post("/api/v1/notes:search") {
                    contentType(ContentType.Application.Json)
                    setBody("{\"filter\":{\"query\":\"Eggs\"},\"page\":{\"number\":1,\"size\":10}}")
                }
            assertEquals(HttpStatusCode.OK, searchResponse.status)
        }
    }

    private fun startTestAppWithDb(block: suspend (HttpClient) -> Unit) {
        dbConfig = TestPostgresHelper.startOrSkip()
        val db = dbConfig ?: return
        System.setProperty("APP_ENV", "test")
        System.setProperty("DB_HOST", db.host)
        System.setProperty("DB_PORT", db.port.toString())
        System.setProperty("DB_NAME", db.database)
        System.setProperty("DB_USER", db.username)
        System.setProperty("DB_PASSWORD", db.password)

        testApplication {
            application {
                DatabaseFactory.init(loadConfig().db)
                configureSerialization()
                configureHTTP()
                configureRouting()
            }
            val httpClient = client
            block(httpClient)
        }
    }

    private fun extractId(body: String): String? {
        val json = Json.parseToJsonElement(body).jsonObject
        val data = json["data"]?.jsonObject ?: return null
        return data["id"]?.jsonPrimitive?.content
    }
}
