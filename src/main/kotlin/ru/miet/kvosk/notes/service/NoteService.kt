package ru.miet.kvosk.notes.service

import ru.miet.kvosk.notes.db.NoteRecord
import ru.miet.kvosk.notes.db.NoteRepository
import ru.miet.kvosk.notes.db.NoteSearchFilter
import ru.miet.kvosk.notes.db.NoteSort
import ru.miet.kvosk.notes.db.PageRequest
import ru.miet.kvosk.notes.db.PageResult
import ru.miet.kvosk.notes.db.TagRecord
import ru.miet.kvosk.notes.db.TagRepository
import java.util.UUID

data class NoteDetails(
    val note: NoteRecord,
    val tags: List<TagRecord> = emptyList(),
)

private const val NOTE_TITLE_MAX_LENGTH = 200

interface NoteServiceContract {
    fun create(
        title: String,
        content: String,
        tagIds: List<UUID>,
    ): ServiceResult<NoteDetails>

    fun getById(
        id: UUID,
        includeTags: Boolean,
    ): ServiceResult<NoteDetails>

    fun update(
        id: UUID,
        title: String,
        content: String,
        isArchived: Boolean,
        tagIds: List<UUID>,
    ): ServiceResult<NoteDetails>

    fun updatePartial(
        id: UUID,
        title: String?,
        content: String?,
        isArchived: Boolean?,
        tagIds: List<UUID>?,
    ): ServiceResult<NoteDetails>

    fun delete(id: UUID): ServiceResult<Boolean>

    fun search(
        filter: NoteSearchFilter,
        sort: List<NoteSort>,
        page: PageRequest,
        includeTags: Boolean,
    ): ServiceResult<PageResult<NoteDetails>>
}

class NoteService(
    private val noteRepository: NoteRepository,
    private val tagRepository: TagRepository,
) : NoteServiceContract {
    override fun create(
        title: String,
        content: String,
        tagIds: List<UUID>,
    ): ServiceResult<NoteDetails> {
        val titleError = validateTitle(title)
        val missingTags = findMissingTags(tagIds)
        return when {
            titleError != null -> ServiceResult.Error(titleError)
            missingTags.isNotEmpty() ->
                ServiceResult.Error(
                    ServiceError.Validation(
                        field = "tag_ids",
                        message = "Unknown tag ids: ${missingTags.joinToString()}",
                    ),
                )
            else -> {
                val note = noteRepository.create(title, content, isArchived = false)
                val distinctTags = tagIds.distinct()
                if (distinctTags.isNotEmpty()) {
                    noteRepository.setTags(note.id, distinctTags)
                }
                val tags = if (distinctTags.isEmpty()) emptyList() else noteRepository.getTags(note.id)
                ServiceResult.Success(NoteDetails(note, tags))
            }
        }
    }

    override fun getById(
        id: UUID,
        includeTags: Boolean,
    ): ServiceResult<NoteDetails> {
        val note =
            noteRepository.findById(id)
                ?: return ServiceResult.Error(ServiceError.NotFound("note", id.toString()))
        val tags = if (includeTags) noteRepository.getTags(id) else emptyList()
        return ServiceResult.Success(NoteDetails(note, tags))
    }

    override fun update(
        id: UUID,
        title: String,
        content: String,
        isArchived: Boolean,
        tagIds: List<UUID>,
    ): ServiceResult<NoteDetails> {
        val titleError = validateTitle(title)
        val missingTags = findMissingTags(tagIds)
        return when {
            titleError != null -> ServiceResult.Error(titleError)
            missingTags.isNotEmpty() ->
                ServiceResult.Error(
                    ServiceError.Validation(
                        field = "tag_ids",
                        message = "Unknown tag ids: ${missingTags.joinToString()}",
                    ),
                )
            else -> {
                val updated = noteRepository.update(id, title, content, isArchived)
                if (updated == null) {
                    ServiceResult.Error(ServiceError.NotFound("note", id.toString()))
                } else {
                    val distinctTags = tagIds.distinct()
                    noteRepository.setTags(id, distinctTags)
                    val tags = if (distinctTags.isEmpty()) emptyList() else noteRepository.getTags(id)
                    ServiceResult.Success(NoteDetails(updated, tags))
                }
            }
        }
    }

    override fun updatePartial(
        id: UUID,
        title: String?,
        content: String?,
        isArchived: Boolean?,
        tagIds: List<UUID>?,
    ): ServiceResult<NoteDetails> {
        val titleError = if (title == null) null else validateTitle(title)
        val missingTags = if (tagIds == null) emptyList() else findMissingTags(tagIds)

        return when {
            titleError != null -> ServiceResult.Error(titleError)
            missingTags.isNotEmpty() ->
                ServiceResult.Error(
                    ServiceError.Validation(
                        field = "tag_ids",
                        message = "Unknown tag ids: ${missingTags.joinToString()}",
                    ),
                )
            else -> {
                val updated = noteRepository.updatePartial(id, title, content, isArchived)
                if (updated == null) {
                    ServiceResult.Error(ServiceError.NotFound("note", id.toString()))
                } else {
                    val tags = resolveTagsAfterPatch(id, tagIds)
                    ServiceResult.Success(NoteDetails(updated, tags))
                }
            }
        }
    }

    private fun resolveTagsAfterPatch(
        noteId: UUID,
        tagIds: List<UUID>?,
    ): List<TagRecord> {
        if (tagIds == null) return emptyList()
        val distinctTags = tagIds.distinct()
        noteRepository.setTags(noteId, distinctTags)
        return if (distinctTags.isEmpty()) emptyList() else noteRepository.getTags(noteId)
    }

    override fun delete(id: UUID): ServiceResult<Boolean> {
        val deleted = noteRepository.delete(id)
        return ServiceResult.Success(deleted)
    }

    override fun search(
        filter: NoteSearchFilter,
        sort: List<NoteSort>,
        page: PageRequest,
        includeTags: Boolean,
    ): ServiceResult<PageResult<NoteDetails>> {
        val pageError = validatePage(page)
        return if (pageError != null) {
            ServiceResult.Error(pageError)
        } else {
            val result = noteRepository.search(filter, sort, page)
            val items =
                if (!includeTags) {
                    result.items.map { NoteDetails(it) }
                } else {
                    result.items.map { note ->
                        val tags = noteRepository.getTags(note.id)
                        NoteDetails(note, tags)
                    }
                }
            ServiceResult.Success(PageResult(items, result.total, result.page, result.size))
        }
    }

    private fun validateTitle(title: String): ServiceError.Validation? {
        return when {
            title.isBlank() -> ServiceError.Validation("title", "Title must not be blank")
            title.length > NOTE_TITLE_MAX_LENGTH ->
                ServiceError.Validation("title", "Title must be at most $NOTE_TITLE_MAX_LENGTH characters")
            else -> null
        }
    }

    private fun validatePage(page: PageRequest): ServiceError.Validation? {
        return when {
            page.number < 1 -> ServiceError.Validation("page.number", "Page number must be >= 1")
            page.size < 1 -> ServiceError.Validation("page.size", "Page size must be >= 1")
            else -> null
        }
    }

    private fun findMissingTags(tagIds: List<UUID>): List<UUID> {
        if (tagIds.isEmpty()) return emptyList()
        val existing = tagRepository.findByIds(tagIds).map { it.id }.toSet()
        return tagIds.distinct().filterNot { existing.contains(it) }
    }
}
