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
    fun create(name: String): ServiceResult<TagRecord> {
        val nameError = validateName(name)
        if (nameError != null) return ServiceResult.Error(nameError)

        val existing = tagRepository.findByName(name)
        if (existing != null) {
            return ServiceResult.Error(
                ServiceError.Conflict(
                    field = "name",
                    message = "Tag name already exists",
                ),
            )
        }
        val tag = tagRepository.create(name)
        return ServiceResult.Success(tag)
    }

    fun getById(id: UUID): ServiceResult<TagRecord> {
        val tag = tagRepository.findById(id)
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
        if (name.isBlank()) {
            return ServiceError.Validation("name", "Name must not be blank")
        }
        if (name.length > 50) {
            return ServiceError.Validation("name", "Name must be at most 50 characters")
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
}
