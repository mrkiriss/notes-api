package ru.miet.kvosk.notes.db

import java.time.Instant
import java.util.UUID

data class NoteRecord(
    val id: UUID,
    val title: String,
    val content: String,
    val isArchived: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class TagRecord(
    val id: UUID,
    val name: String,
)

data class PageRequest(
    val number: Int,
    val size: Int,
)

data class PageResult<T>(
    val items: List<T>,
    val total: Long,
    val page: Int,
    val size: Int,
)

data class NoteSearchFilter(
    val query: String? = null,
    val isArchived: Boolean? = null,
    val tagIds: List<UUID>? = null,
)

enum class SortDirection {
    ASC,
    DESC,
}

enum class NoteSortField {
    CREATED_AT,
    UPDATED_AT,
    TITLE,
}

data class NoteSort(
    val field: NoteSortField,
    val direction: SortDirection,
)

data class TagSearchFilter(
    val query: String? = null,
)

enum class TagSortField {
    NAME,
}

data class TagSort(
    val field: TagSortField,
    val direction: SortDirection,
)
