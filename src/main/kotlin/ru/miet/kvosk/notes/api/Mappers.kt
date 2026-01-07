package ru.miet.kvosk.notes.api

import ru.miet.kvosk.notes.db.NoteRecord
import ru.miet.kvosk.notes.db.NoteSearchFilter
import ru.miet.kvosk.notes.db.NoteSort
import ru.miet.kvosk.notes.db.NoteSortField
import ru.miet.kvosk.notes.db.PageRequest
import ru.miet.kvosk.notes.db.SortDirection
import ru.miet.kvosk.notes.db.TagRecord
import ru.miet.kvosk.notes.db.TagSearchFilter
import ru.miet.kvosk.notes.db.TagSort
import ru.miet.kvosk.notes.db.TagSortField
import ru.miet.kvosk.notes.service.NoteDetails
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

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

fun NoteSearchRequest.toFilter(): NoteSearchFilter {
    val filter = filter
    return NoteSearchFilter(
        query = filter?.query,
        title = filter?.title,
        isArchived = filter?.isArchived,
        tagIds = filter?.tagIds?.map { UUID.fromString(it) },
    )
}

fun NoteSearchRequest.toSort(): List<NoteSort> {
    return sort.map {
        NoteSort(
            field = it.field.toDomain(),
            direction = it.direction.toDomain(),
        )
    }
}

fun NoteSearchRequest.toPage(defaultSize: Int = 20): PageRequest {
    val page = page
    return PageRequest(
        number = page?.number ?: 1,
        size = page?.size ?: defaultSize,
    )
}

fun TagSearchRequest.toFilter(): TagSearchFilter {
    return TagSearchFilter(query = filter?.query)
}

fun TagSearchRequest.toSort(): List<TagSort> {
    return sort.map {
        TagSort(
            field = it.field.toDomain(),
            direction = it.direction.toDomain(),
        )
    }
}

fun TagSearchRequest.toPage(defaultSize: Int = 50): PageRequest {
    val page = page
    return PageRequest(
        number = page?.number ?: 1,
        size = page?.size ?: defaultSize,
    )
}

private fun NoteSortFieldDto.toDomain(): NoteSortField {
    return when (this) {
        NoteSortFieldDto.CREATED_AT -> NoteSortField.CREATED_AT
        NoteSortFieldDto.UPDATED_AT -> NoteSortField.UPDATED_AT
        NoteSortFieldDto.TITLE -> NoteSortField.TITLE
    }
}

private fun TagSortFieldDto.toDomain(): TagSortField {
    return when (this) {
        TagSortFieldDto.NAME -> TagSortField.NAME
    }
}

private fun SortDirectionDto.toDomain(): SortDirection {
    return when (this) {
        SortDirectionDto.ASC -> SortDirection.ASC
        SortDirectionDto.DESC -> SortDirection.DESC
    }
}

private fun formatInstant(value: Instant): String = instantFormatter.format(value)
