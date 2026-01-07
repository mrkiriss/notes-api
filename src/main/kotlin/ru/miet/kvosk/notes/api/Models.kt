package ru.miet.kvosk.notes.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Envelope<T>(
    val data: T? = null,
    val meta: Meta? = null,
)

@Serializable
data class ErrorEnvelope(
    val errors: List<ErrorItem>,
)

@Serializable
data class ErrorItem(
    val message: String,
    val field: String? = null,
)

@Serializable
data class Meta(
    val pagination: PaginationMeta? = null,
)

@Serializable
data class PaginationMeta(
    val page: Int,
    val size: Int,
    val total: Long,
)

@Serializable
data class NoteResponse(
    val id: String,
    val title: String,
    val content: String,
    @SerialName("is_archived")
    val isArchived: Boolean,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
    val tags: List<TagResponse>? = null,
)

@Serializable
data class TagResponse(
    val id: String,
    val name: String,
)

@Serializable
data class CreateNoteRequest(
    val title: String,
    val content: String = "",
    @SerialName("tag_ids")
    val tagIds: List<String> = emptyList(),
)

@Serializable
data class UpdateNoteRequest(
    val title: String,
    val content: String = "",
    @SerialName("is_archived")
    val isArchived: Boolean,
    @SerialName("tag_ids")
    val tagIds: List<String> = emptyList(),
)

@Serializable
data class PatchNoteRequest(
    val title: String? = null,
    val content: String? = null,
    @SerialName("is_archived")
    val isArchived: Boolean? = null,
    @SerialName("tag_ids")
    val tagIds: List<String>? = null,
)

@Serializable
data class CreateTagRequest(
    val name: String,
)

@Serializable
data class PageRequestDto(
    val number: Int? = null,
    val size: Int? = null,
)

@Serializable
data class NoteSearchRequest(
    val filter: NoteSearchFilterDto? = null,
    val sort: List<NoteSortDto> = emptyList(),
    val page: PageRequestDto? = null,
)

@Serializable
data class NoteSearchFilterDto(
    val query: String? = null,
    val title: String? = null,
    @SerialName("is_archived")
    val isArchived: Boolean? = null,
    @SerialName("tag_ids")
    val tagIds: List<String>? = null,
)

@Serializable
data class NoteSortDto(
    val field: NoteSortFieldDto,
    val direction: SortDirectionDto,
)

@Serializable
enum class NoteSortFieldDto {
    @SerialName("created_at")
    CREATED_AT,
    @SerialName("updated_at")
    UPDATED_AT,
    @SerialName("title")
    TITLE,
}

@Serializable
enum class SortDirectionDto {
    @SerialName("asc")
    ASC,
    @SerialName("desc")
    DESC,
}

@Serializable
data class TagSearchRequest(
    val filter: TagSearchFilterDto? = null,
    val sort: List<TagSortDto> = emptyList(),
    val page: PageRequestDto? = null,
)

@Serializable
data class TagSearchFilterDto(
    val query: String? = null,
)

@Serializable
data class TagSortDto(
    val field: TagSortFieldDto,
    val direction: SortDirectionDto,
)

@Serializable
enum class TagSortFieldDto {
    @SerialName("name")
    NAME,
}
