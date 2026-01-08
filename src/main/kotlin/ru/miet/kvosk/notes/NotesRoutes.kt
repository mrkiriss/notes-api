package ru.miet.kvosk.notes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import ru.miet.kvosk.notes.api.CreateNoteRequest
import ru.miet.kvosk.notes.api.Envelope
import ru.miet.kvosk.notes.api.Meta
import ru.miet.kvosk.notes.api.NoteSearchRequest
import ru.miet.kvosk.notes.api.PaginationMeta
import ru.miet.kvosk.notes.api.PatchNoteRequest
import ru.miet.kvosk.notes.api.UpdateNoteRequest
import ru.miet.kvosk.notes.api.toPage
import ru.miet.kvosk.notes.api.toResponse
import ru.miet.kvosk.notes.api.toSort
import ru.miet.kvosk.notes.service.NoteService
import ru.miet.kvosk.notes.service.ServiceError
import ru.miet.kvosk.notes.service.ServiceResult

fun Route.notesRoutes(noteService: NoteService) {
    noteCrudRoutes(noteService)
    noteSearchRoutes(noteService)
}

private fun Route.noteCrudRoutes(noteService: NoteService) {
    route("/notes") {
        post { createNote(noteService) }
    }
    get("/notes/{id}") { getNote(noteService) }
    put("/notes/{id}") { replaceNote(noteService) }
    patch("/notes/{id}") { patchNote(noteService) }
    delete("/notes/{id}") { deleteNote(noteService) }
}

private fun Route.noteSearchRoutes(noteService: NoteService) {
    post("/notes:search") { searchNotes(noteService) }
    post("/notes:search-one") { searchOneNote(noteService) }
}

private suspend fun RoutingContext.createNote(noteService: NoteService) {
    val request = call.receive<CreateNoteRequest>()
    val tagIds = call.parseUuidList(request.tagIds, "tag_ids") ?: return
    when (val result = noteService.create(request.title, request.content, tagIds)) {
        is ServiceResult.Success ->
            call.respond(
                HttpStatusCode.Created,
                Envelope(data = result.value.toResponse(includeTags = false)),
            )
        is ServiceResult.Error -> call.respondServiceError(result.error)
    }
}

private suspend fun RoutingContext.getNote(noteService: NoteService) {
    val id = call.parseUuidParam("id") ?: return
    val includeTags = call.includes("tags")
    when (val result = noteService.getById(id, includeTags)) {
        is ServiceResult.Success ->
            call.respond(
                HttpStatusCode.OK,
                Envelope(data = result.value.toResponse(includeTags)),
            )
        is ServiceResult.Error -> call.respondServiceError(result.error)
    }
}

private suspend fun RoutingContext.replaceNote(noteService: NoteService) {
    val id = call.parseUuidParam("id") ?: return
    val request = call.receive<UpdateNoteRequest>()
    val tagIds = call.parseUuidList(request.tagIds, "tag_ids") ?: return
    when (
        val result =
            noteService.update(
                id = id,
                title = request.title,
                content = request.content,
                isArchived = request.isArchived,
                tagIds = tagIds,
            )
    ) {
        is ServiceResult.Success ->
            call.respond(
                HttpStatusCode.OK,
                Envelope(data = result.value.toResponse(includeTags = false)),
            )
        is ServiceResult.Error -> call.respondServiceError(result.error)
    }
}

private suspend fun RoutingContext.patchNote(noteService: NoteService) {
    val id = call.parseUuidParam("id") ?: return
    val request = call.receive<PatchNoteRequest>()
    val tagIds = call.parsePatchTagIds(request.tagIds) ?: return
    when (
        val result =
            noteService.updatePartial(
                id = id,
                title = request.title,
                content = request.content,
                isArchived = request.isArchived,
                tagIds = tagIds,
            )
    ) {
        is ServiceResult.Success ->
            call.respond(
                HttpStatusCode.OK,
                Envelope(data = result.value.toResponse(includeTags = false)),
            )
        is ServiceResult.Error -> call.respondServiceError(result.error)
    }
}

private suspend fun RoutingContext.deleteNote(noteService: NoteService) {
    val id = call.parseUuidParam("id") ?: return
    noteService.delete(id)
    call.respond(HttpStatusCode.OK, Envelope<JsonElement>(data = JsonNull))
}

private suspend fun RoutingContext.searchNotes(noteService: NoteService) {
    val request = call.receive<NoteSearchRequest>()
    val includeTags = call.includes("tags")
    val filter = call.parseNoteFilter(request) ?: return
    val page = request.toPage()
    val sort = request.toSort()
    when (val result = noteService.search(filter, sort, page, includeTags)) {
        is ServiceResult.Success -> {
            val meta =
                Meta(
                    pagination =
                        PaginationMeta(
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

private suspend fun RoutingContext.searchOneNote(noteService: NoteService) {
    val request = call.receive<NoteSearchRequest>()
    val includeTags = call.includes("tags")
    val filter = call.parseNoteFilter(request) ?: return
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
