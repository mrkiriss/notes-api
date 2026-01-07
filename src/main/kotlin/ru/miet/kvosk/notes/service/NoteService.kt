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

class NoteService(
    private val noteRepository: NoteRepository,
    private val tagRepository: TagRepository,
) {
    fun create(title: String, content: String, tagIds: List<UUID>): ServiceResult<NoteDetails> {
        val titleError = validateTitle(title)
        if (titleError != null) return ServiceResult.Error(titleError)

        val missingTags = findMissingTags(tagIds)
        if (missingTags.isNotEmpty()) {
            return ServiceResult.Error(
                ServiceError.Validation(
                    field = "tag_ids",
                    message = "Unknown tag ids: ${missingTags.joinToString()}",
                ),
            )
        }

        val note = noteRepository.create(title, content, isArchived = false)
        val distinctTags = tagIds.distinct()
        if (distinctTags.isNotEmpty()) {
            noteRepository.setTags(note.id, distinctTags)
        }
        val tags = if (distinctTags.isEmpty()) emptyList() else noteRepository.getTags(note.id)
        return ServiceResult.Success(NoteDetails(note, tags))
    }

    fun getById(id: UUID, includeTags: Boolean): ServiceResult<NoteDetails> {
        val note = noteRepository.findById(id)
            ?: return ServiceResult.Error(ServiceError.NotFound("note", id.toString()))
        val tags = if (includeTags) noteRepository.getTags(id) else emptyList()
        return ServiceResult.Success(NoteDetails(note, tags))
    }

    fun update(
        id: UUID,
        title: String,
        content: String,
        isArchived: Boolean,
        tagIds: List<UUID>,
    ): ServiceResult<NoteDetails> {
        val titleError = validateTitle(title)
        if (titleError != null) return ServiceResult.Error(titleError)

        val missingTags = findMissingTags(tagIds)
        if (missingTags.isNotEmpty()) {
            return ServiceResult.Error(
                ServiceError.Validation(
                    field = "tag_ids",
                    message = "Unknown tag ids: ${missingTags.joinToString()}",
                ),
            )
        }

        val updated = noteRepository.update(id, title, content, isArchived)
            ?: return ServiceResult.Error(ServiceError.NotFound("note", id.toString()))

        val distinctTags = tagIds.distinct()
        noteRepository.setTags(id, distinctTags)
        val tags = if (distinctTags.isEmpty()) emptyList() else noteRepository.getTags(id)
        return ServiceResult.Success(NoteDetails(updated, tags))
    }

    fun updatePartial(
        id: UUID,
        title: String?,
        content: String?,
        isArchived: Boolean?,
        tagIds: List<UUID>?,
    ): ServiceResult<NoteDetails> {
        if (title != null) {
            val titleError = validateTitle(title)
            if (titleError != null) return ServiceResult.Error(titleError)
        }

        if (tagIds != null) {
            val missingTags = findMissingTags(tagIds)
            if (missingTags.isNotEmpty()) {
                return ServiceResult.Error(
                    ServiceError.Validation(
                        field = "tag_ids",
                        message = "Unknown tag ids: ${missingTags.joinToString()}",
                    ),
                )
            }
        }

        val updated = noteRepository.updatePartial(id, title, content, isArchived)
            ?: return ServiceResult.Error(ServiceError.NotFound("note", id.toString()))

        val tags = if (tagIds == null) {
            emptyList()
        } else {
            val distinctTags = tagIds.distinct()
            noteRepository.setTags(id, distinctTags)
            if (distinctTags.isEmpty()) emptyList() else noteRepository.getTags(id)
        }

        return ServiceResult.Success(NoteDetails(updated, tags))
    }

    fun delete(id: UUID): ServiceResult<Boolean> {
        val deleted = noteRepository.delete(id)
        return ServiceResult.Success(deleted)
    }

    fun search(
        filter: NoteSearchFilter,
        sort: List<NoteSort>,
        page: PageRequest,
        includeTags: Boolean,
    ): ServiceResult<PageResult<NoteDetails>> {
        val pageError = validatePage(page)
        if (pageError != null) return ServiceResult.Error(pageError)

        val result = noteRepository.search(filter, sort, page)
        if (!includeTags) {
            val mapped = result.items.map { NoteDetails(it) }
            return ServiceResult.Success(PageResult(mapped, result.total, result.page, result.size))
        }

        val items = result.items.map { note ->
            val tags = noteRepository.getTags(note.id)
            NoteDetails(note, tags)
        }
        return ServiceResult.Success(PageResult(items, result.total, result.page, result.size))
    }

    private fun validateTitle(title: String): ServiceError.Validation? {
        if (title.isBlank()) {
            return ServiceError.Validation("title", "Title must not be blank")
        }
        if (title.length > 200) {
            return ServiceError.Validation("title", "Title must be at most 200 characters")
        }
        return null
    }

    private fun validatePage(page: PageRequest): ServiceError.Validation? {
        if (page.number < 1) {
            return ServiceError.Validation("page.number", "Page number must be >= 1")
        }
        if (page.size < 1) {
            return ServiceError.Validation("page.size", "Page size must be >= 1")
        }
        return null
    }

    private fun findMissingTags(tagIds: List<UUID>): List<UUID> {
        if (tagIds.isEmpty()) return emptyList()
        val existing = tagRepository.findByIds(tagIds).map { it.id }.toSet()
        return tagIds.distinct().filterNot { existing.contains(it) }
    }
}
