package ru.miet.kvosk.notes.service

import ru.miet.kvosk.notes.db.PageRequest
import ru.miet.kvosk.notes.db.PageResult
import ru.miet.kvosk.notes.db.TagRecord
import ru.miet.kvosk.notes.db.TagRepository
import ru.miet.kvosk.notes.db.TagSearchFilter
import ru.miet.kvosk.notes.db.TagSort
import java.util.UUID

class TagService(
    private val tagRepository: TagRepository,
) {
    private companion object {
        const val TAG_NAME_MAX_LENGTH = 50
    }

    fun create(name: String): ServiceResult<TagRecord> {
        val nameError = validateName(name)
        val existing = tagRepository.findByName(name)
        return when {
            nameError != null -> ServiceResult.Error(nameError)
            existing != null ->
                ServiceResult.Error(
                    ServiceError.Conflict(
                        field = "name",
                        message = "Tag name already exists",
                    ),
                )
            else -> {
                val tag = tagRepository.create(name)
                ServiceResult.Success(tag)
            }
        }
    }

    fun getById(id: UUID): ServiceResult<TagRecord> {
        val tag =
            tagRepository.findById(id)
                ?: return ServiceResult.Error(ServiceError.NotFound("tag", id.toString()))
        return ServiceResult.Success(tag)
    }

    fun search(
        filter: TagSearchFilter,
        sort: List<TagSort>,
        page: PageRequest,
    ): ServiceResult<PageResult<TagRecord>> {
        val pageError = validatePage(page)
        if (pageError != null) return ServiceResult.Error(pageError)
        val result = tagRepository.search(filter, sort, page)
        return ServiceResult.Success(result)
    }

    private fun validateName(name: String): ServiceError.Validation? {
        return when {
            name.isBlank() -> ServiceError.Validation("name", "Name must not be blank")
            name.length > TAG_NAME_MAX_LENGTH ->
                ServiceError.Validation("name", "Name must be at most $TAG_NAME_MAX_LENGTH characters")
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
}
