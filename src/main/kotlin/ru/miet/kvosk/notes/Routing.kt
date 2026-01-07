package ru.miet.kvosk.notes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import ru.miet.kvosk.notes.api.*
import ru.miet.kvosk.notes.db.ExposedNoteRepository
import ru.miet.kvosk.notes.db.ExposedTagRepository
import ru.miet.kvosk.notes.db.NoteSearchFilter
import ru.miet.kvosk.notes.service.NoteService
import ru.miet.kvosk.notes.service.ServiceError
import ru.miet.kvosk.notes.service.ServiceResult
import ru.miet.kvosk.notes.service.TagService
import java.util.*

fun Application.configureRouting() {
    val noteService = NoteService(ExposedNoteRepository(), ExposedTagRepository())
    val tagService = TagService(ExposedTagRepository())

    install(StatusPages) {
        exception<Throwable> { call, _ ->
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorEnvelope(listOf(ErrorItem(message = "Internal server error"))),
            )
        }
    }

    routing {
        route("/api/v1") {
            post("/notes") {
                val request = call.receive<CreateNoteRequest>()
                val tagIds = call.parseUuidList(request.tagIds, "tag_ids") ?: return@post
                when (val result = noteService.create(request.title, request.content, tagIds)) {
                    is ServiceResult.Success -> call.respond(
                        HttpStatusCode.Created,
                        Envelope(data = result.value.toResponse(includeTags = false)),
                    )
                    is ServiceResult.Error -> call.respondServiceError(result.error)
                }
            }

            get("/notes/{id}") {
                val id = call.parseUuidParam("id") ?: return@get
                val includeTags = call.includes("tags")
                when (val result = noteService.getById(id, includeTags)) {
                    is ServiceResult.Success -> call.respond(
                        HttpStatusCode.OK,
                        Envelope(data = result.value.toResponse(includeTags)),
                    )
                    is ServiceResult.Error -> call.respondServiceError(result.error)
                }
            }

            put("/notes/{id}") {
                val id = call.parseUuidParam("id") ?: return@put
                val request = call.receive<UpdateNoteRequest>()
                val tagIds = call.parseUuidList(request.tagIds, "tag_ids") ?: return@put
                when (val result = noteService.update(
                    id = id,
                    title = request.title,
                    content = request.content,
                    isArchived = request.isArchived,
                    tagIds = tagIds,
                )) {
                    is ServiceResult.Success -> call.respond(
                        HttpStatusCode.OK,
                        Envelope(data = result.value.toResponse(includeTags = false)),
                    )
                    is ServiceResult.Error -> call.respondServiceError(result.error)
                }
            }

            patch("/notes/{id}") {
                val id = call.parseUuidParam("id") ?: return@patch
                val request = call.receive<PatchNoteRequest>()
                val tagIds = if (request.tagIds == null) {
                    null
                } else {
                    call.parseUuidList(request.tagIds, "tag_ids") ?: return@patch
                }
                when (val result = noteService.updatePartial(
                    id = id,
                    title = request.title,
                    content = request.content,
                    isArchived = request.isArchived,
                    tagIds = tagIds,
                )) {
                    is ServiceResult.Success -> call.respond(
                        HttpStatusCode.OK,
                        Envelope(data = result.value.toResponse(includeTags = false)),
                    )
                    is ServiceResult.Error -> call.respondServiceError(result.error)
                }
            }

            delete("/notes/{id}") {
                val id = call.parseUuidParam("id") ?: return@delete
                noteService.delete(id)
                call.respond(HttpStatusCode.OK, Envelope<JsonElement>(data = JsonNull))
            }

            post("/notes:search") {
                val request = call.receive<NoteSearchRequest>()
                val includeTags = call.includes("tags")
                val filter = call.parseNoteFilter(request) ?: return@post
                val page = request.toPage()
                val sort = request.toSort()
                when (val result = noteService.search(filter, sort, page, includeTags)) {
                    is ServiceResult.Success -> {
                        val meta = Meta(
                            pagination = PaginationMeta(
                                page = result.value.page,
                                size = result.value.size,
                                total = result.value.total,
                            ),
                        )
                        val items = result.value.items.map { it.toResponse(includeTags) }
                        call.respond(HttpStatusCode.OK, Envelope(data = items, meta = meta))
                    }
                    is ServiceResult.Error -> call.respondServiceError(result.error)
                }
            }

            post("/notes:search-one") {
                val request = call.receive<NoteSearchRequest>()
                val includeTags = call.includes("tags")
                val filter = call.parseNoteFilter(request) ?: return@post
                val page = request.toPage(defaultSize = 1)
                val sort = request.toSort()
                when (val result = noteService.search(filter, sort, page, includeTags)) {
                    is ServiceResult.Success -> {
                        val first = result.value.items.firstOrNull()
                        if (first == null) {
                            call.respondServiceError(ServiceError.NotFound("note", "search-one"))
                        } else {
                            call.respond(HttpStatusCode.OK, Envelope(data = first.toResponse(includeTags)))
                        }
                    }
                    is ServiceResult.Error -> call.respondServiceError(result.error)
                }
            }

            post("/tags") {
                val request = call.receive<CreateTagRequest>()
                when (val result = tagService.create(request.name)) {
                    is ServiceResult.Success -> call.respond(
                        HttpStatusCode.Created,
                        Envelope(data = result.value.toResponse()),
                    )
                    is ServiceResult.Error -> call.respondServiceError(result.error)
                }
            }

            get("/tags/{id}") {
                val id = call.parseUuidParam("id") ?: return@get
                when (val result = tagService.getById(id)) {
                    is ServiceResult.Success -> call.respond(
                        HttpStatusCode.OK,
                        Envelope(data = result.value.toResponse()),
                    )
                    is ServiceResult.Error -> call.respondServiceError(result.error)
                }
            }

            post("/tags:search") {
                val request = call.receive<TagSearchRequest>()
                val page = request.toPage()
                val sort = request.toSort()
                val filter = request.toFilter()
                when (val result = tagService.search(filter, sort, page)) {
                    is ServiceResult.Success -> {
                        val meta = Meta(
                            pagination = PaginationMeta(
                                page = result.value.page,
                                size = result.value.size,
                                total = result.value.total,
                            ),
                        )
                        val items = result.value.items.map { it.toResponse() }
                        call.respond(HttpStatusCode.OK, Envelope(data = items, meta = meta))
                    }
                    is ServiceResult.Error -> call.respondServiceError(result.error)
                }
            }
        }
    }
}

private fun ApplicationCall.includes(name: String): Boolean {
    val include = request.queryParameters["include"] ?: return false
    return include.split(",").map { it.trim() }.contains(name)
}

private suspend fun ApplicationCall.parseUuidParam(name: String): UUID? {
    val raw = parameters[name] ?: run {
        respondBadRequest("$name is required", name)
        return null
    }
    return try {
        UUID.fromString(raw)
    } catch (_: IllegalArgumentException) {
        respondBadRequest("Invalid UUID", name)
        null
    }
}

private suspend fun ApplicationCall.parseUuidList(values: List<String>, field: String): List<UUID>? {
    return try {
        values.map { UUID.fromString(it) }
    } catch (_: IllegalArgumentException) {
        respondBadRequest("Invalid UUID", field)
        null
    }
}

private suspend fun ApplicationCall.respondBadRequest(message: String, field: String? = null) {
    respond(HttpStatusCode.BadRequest, ErrorEnvelope(listOf(ErrorItem(message = message, field = field))))
}

private suspend fun ApplicationCall.respondServiceError(error: ServiceError) {
    when (error) {
        is ServiceError.NotFound -> respond(
            HttpStatusCode.NotFound,
            ErrorEnvelope(listOf(ErrorItem(message = "${error.entity} not found", field = "id"))),
        )
        is ServiceError.Validation -> respond(
            HttpStatusCode.BadRequest,
            ErrorEnvelope(listOf(ErrorItem(message = error.message, field = error.field))),
        )
        is ServiceError.Conflict -> respond(
            HttpStatusCode.Conflict,
            ErrorEnvelope(listOf(ErrorItem(message = error.message, field = error.field))),
        )
    }
}

private suspend fun ApplicationCall.parseNoteFilter(request: NoteSearchRequest): NoteSearchFilter? {
    return try {
        request.toFilter()
    } catch (_: IllegalArgumentException) {
        respondBadRequest("Invalid UUID", "tag_ids")
        null
    }
}
