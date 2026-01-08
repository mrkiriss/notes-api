package ru.miet.kvosk.notes

import io.ktor.server.application.Application
import io.ktor.server.http.content.staticResources
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.configureHTTP() {
    routing {
        get("/openapi") {
            call.respondRedirect("/apidoc/v1/index.yaml", permanent = true)
        }
    }
    routing {
        staticResources("/swagger", "swagger")
    }
    routing {
        staticResources("/apidoc/v1", "docs/apidoc/v1")
    }
    routing {
        staticResources("/docs", "stoplight")
    }
}
