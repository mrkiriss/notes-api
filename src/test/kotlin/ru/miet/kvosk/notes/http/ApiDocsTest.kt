package ru.miet.kvosk.notes.http

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.miet.kvosk.notes.configureHTTP

class ApiDocsTest {
    @Test
    fun `openapi redirects to versioned spec`() {
        testApplication {
            application {
                configureHTTP()
            }
            val client =
                createClient {
                    followRedirects = false
                }
            val response = client.get("/openapi")
            assertEquals(HttpStatusCode.MovedPermanently, response.status)
        }
    }

    @Test
    fun `spec and docs are served`() {
        testApplication {
            application {
                configureHTTP()
            }
            val spec = client.get("/apidoc/v1/index.yaml")
            assertEquals(HttpStatusCode.OK, spec.status)

            val docs = client.get("/docs/index.html")
            assertEquals(HttpStatusCode.OK, docs.status)

            val swagger = client.get("/swagger/index.html")
            assertEquals(HttpStatusCode.OK, swagger.status)
        }
    }
}
