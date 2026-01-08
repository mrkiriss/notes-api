package ru.miet.kvosk.notes.api

import ru.miet.kvosk.notes.db.NoteSearchFilter
import ru.miet.kvosk.notes.db.NoteSort
import ru.miet.kvosk.notes.db.PageRequest
import ru.miet.kvosk.notes.db.TagSearchFilter
import ru.miet.kvosk.notes.db.TagSort
import java.util.UUID

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
