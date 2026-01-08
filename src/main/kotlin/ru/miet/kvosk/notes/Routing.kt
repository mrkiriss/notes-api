package ru.miet.kvosk.notes

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import ru.miet.kvosk.notes.api.ErrorEnvelope
import ru.miet.kvosk.notes.api.ErrorItem
import ru.miet.kvosk.notes.db.ExposedNoteRepository
import ru.miet.kvosk.notes.db.ExposedTagRepository
import ru.miet.kvosk.notes.service.NoteService
import ru.miet.kvosk.notes.service.NoteServiceContract
import ru.miet.kvosk.notes.service.TagService
import ru.miet.kvosk.notes.service.TagServiceContract

fun Application.configureRouting() {
    val noteService = NoteService(ExposedNoteRepository(), ExposedTagRepository())
    val tagService = TagService(ExposedTagRepository())
    configureRouting(noteService, tagService)
}

fun Application.configureRouting(
    noteService: NoteServiceContract,
    tagService: TagServiceContract,
) {
    installStatusPages()

    routing {
        route("/api/v1") {
            notesRoutes(noteService)
            tagsRoutes(tagService)
        }
    }
}

private fun Application.installStatusPages() {
    install(StatusPages) {
        exception<BadRequestException> { call, cause ->
            call.application.log.warn("Bad request", cause)
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorEnvelope(listOf(ErrorItem(message = "Invalid request body"))),
            )
        }
        exception<JsonConvertException> { call, cause ->
            call.application.log.warn("Invalid JSON", cause)
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorEnvelope(listOf(ErrorItem(message = "Invalid JSON"))),
            )
        }
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorEnvelope(listOf(ErrorItem(message = "Internal server error"))),
            )
        }
    }
}
