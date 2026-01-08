package ru.miet.kvosk.notes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import ru.miet.kvosk.notes.api.ErrorEnvelope
import ru.miet.kvosk.notes.api.ErrorItem
import ru.miet.kvosk.notes.api.NoteSearchRequest
import ru.miet.kvosk.notes.api.toFilter
import ru.miet.kvosk.notes.db.NoteSearchFilter
import ru.miet.kvosk.notes.service.ServiceError
import java.util.UUID

internal fun ApplicationCall.includes(name: String): Boolean {
    val include = request.queryParameters["include"] ?: return false
    return include.split(",").map { it.trim() }.contains(name)
}

internal suspend fun ApplicationCall.parseUuidParam(name: String): UUID? {
    val raw =
        parameters[name] ?: run {
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

internal suspend fun ApplicationCall.parseUuidList(
    values: List<String>,
    field: String,
): List<UUID>? {
    return try {
        values.map { UUID.fromString(it) }
    } catch (_: IllegalArgumentException) {
        respondBadRequest("Invalid UUID", field)
        null
    }
}

internal suspend fun ApplicationCall.parseNoteFilter(request: NoteSearchRequest): NoteSearchFilter? {
    return try {
        request.toFilter()
    } catch (_: IllegalArgumentException) {
        respondBadRequest("Invalid UUID", "tag_ids")
        null
    }
}

internal suspend fun ApplicationCall.parsePatchTagIds(tagIds: List<String>?): List<UUID>? {
    return if (tagIds == null) {
        null
    } else {
        parseUuidList(tagIds, "tag_ids")
    }
}

internal suspend fun ApplicationCall.respondBadRequest(
    message: String,
    field: String? = null,
) {
    respond(HttpStatusCode.BadRequest, ErrorEnvelope(listOf(ErrorItem(message = message, field = field))))
}

internal suspend fun ApplicationCall.respondServiceError(error: ServiceError) {
    when (error) {
        is ServiceError.NotFound ->
            respond(
                HttpStatusCode.NotFound,
                ErrorEnvelope(listOf(ErrorItem(message = "${error.entity} not found", field = "id"))),
            )
        is ServiceError.Validation ->
            respond(
                HttpStatusCode.BadRequest,
                ErrorEnvelope(listOf(ErrorItem(message = error.message, field = error.field))),
            )
        is ServiceError.Conflict ->
            respond(
                HttpStatusCode.Conflict,
                ErrorEnvelope(listOf(ErrorItem(message = error.message, field = error.field))),
            )
    }
}
