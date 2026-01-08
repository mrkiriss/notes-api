package ru.miet.kvosk.notes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import ru.miet.kvosk.notes.api.CreateTagRequest
import ru.miet.kvosk.notes.api.Envelope
import ru.miet.kvosk.notes.api.Meta
import ru.miet.kvosk.notes.api.PaginationMeta
import ru.miet.kvosk.notes.api.TagSearchRequest
import ru.miet.kvosk.notes.api.toFilter
import ru.miet.kvosk.notes.api.toPage
import ru.miet.kvosk.notes.api.toResponse
import ru.miet.kvosk.notes.api.toSort
import ru.miet.kvosk.notes.service.ServiceResult
import ru.miet.kvosk.notes.service.TagServiceContract

fun Route.tagsRoutes(tagService: TagServiceContract) {
    post("/tags") {
        val request = call.receive<CreateTagRequest>()
        when (val result = tagService.create(request.name)) {
            is ServiceResult.Success ->
                call.respond(
                    HttpStatusCode.Created,
                    Envelope(data = result.value.toResponse()),
                )
            is ServiceResult.Error -> call.respondServiceError(result.error)
        }
    }

    get("/tags/{id}") {
        val id = call.parseUuidParam("id") ?: return@get
        when (val result = tagService.getById(id)) {
            is ServiceResult.Success ->
                call.respond(
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
                val meta =
                    Meta(
                        pagination =
                            PaginationMeta(
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
