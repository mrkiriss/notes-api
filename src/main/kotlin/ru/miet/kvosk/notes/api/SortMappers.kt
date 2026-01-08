package ru.miet.kvosk.notes.api

import ru.miet.kvosk.notes.db.NoteSortField
import ru.miet.kvosk.notes.db.SortDirection
import ru.miet.kvosk.notes.db.TagSortField

internal fun NoteSortFieldDto.toDomain(): NoteSortField {
    return when (this) {
        NoteSortFieldDto.CREATED_AT -> NoteSortField.CREATED_AT
        NoteSortFieldDto.UPDATED_AT -> NoteSortField.UPDATED_AT
        NoteSortFieldDto.TITLE -> NoteSortField.TITLE
    }
}

internal fun TagSortFieldDto.toDomain(): TagSortField {
    return when (this) {
        TagSortFieldDto.NAME -> TagSortField.NAME
    }
}

internal fun SortDirectionDto.toDomain(): SortDirection {
    return when (this) {
        SortDirectionDto.ASC -> SortDirection.ASC
        SortDirectionDto.DESC -> SortDirection.DESC
    }
}
