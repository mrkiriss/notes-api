package ru.miet.kvosk.notes.api

import ru.miet.kvosk.notes.db.TagRecord
import ru.miet.kvosk.notes.service.NoteDetails
import java.time.Instant
import java.time.format.DateTimeFormatter

private val instantFormatter = DateTimeFormatter.ISO_INSTANT

fun NoteDetails.toResponse(includeTags: Boolean): NoteResponse {
    return NoteResponse(
        id = note.id.toString(),
        title = note.title,
        content = note.content,
        isArchived = note.isArchived,
        createdAt = formatInstant(note.createdAt),
        updatedAt = formatInstant(note.updatedAt),
        tags = if (includeTags) tags.map { it.toResponse() } else null,
    )
}

fun TagRecord.toResponse(): TagResponse {
    return TagResponse(
        id = id.toString(),
        name = name,
    )
}

private fun formatInstant(value: Instant): String = instantFormatter.format(value)
