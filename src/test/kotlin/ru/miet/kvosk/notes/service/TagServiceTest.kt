package ru.miet.kvosk.notes.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.miet.kvosk.notes.db.PageRequest
import ru.miet.kvosk.notes.db.PageResult
import ru.miet.kvosk.notes.db.TagRecord
import ru.miet.kvosk.notes.db.TagRepository
import ru.miet.kvosk.notes.db.TagSearchFilter
import ru.miet.kvosk.notes.db.TagSort
import java.util.UUID

class TagServiceTest {
    private val tagRepository = FakeTagRepository()
    private val service = TagService(tagRepository)

    @Test
    fun `create rejects blank name`() {
        val result = service.create(" ")
        assertTrue(result is ServiceResult.Error)
        val error = (result as ServiceResult.Error).error
        assertTrue(error is ServiceError.Validation)
        assertEquals("name", (error as ServiceError.Validation).field)
    }

    @Test
    fun `create rejects duplicate name`() {
        val result = service.create("home")
        assertTrue(result is ServiceResult.Error)
        val error = (result as ServiceResult.Error).error
        assertTrue(error is ServiceError.Conflict)
    }

    @Test
    fun `getById returns not found`() {
        val result = service.getById(UUID.randomUUID())
        assertTrue(result is ServiceResult.Error)
        val error = (result as ServiceResult.Error).error
        assertTrue(error is ServiceError.NotFound)
    }

    @Test
    fun `search rejects invalid page`() {
        val result = service.search(TagSearchFilter(), emptyList(), PageRequest(number = 0, size = 10))
        assertTrue(result is ServiceResult.Error)
        val error = (result as ServiceResult.Error).error
        assertTrue(error is ServiceError.Validation)
        assertEquals("page.number", (error as ServiceError.Validation).field)
    }

    private class FakeTagRepository : TagRepository {
        override fun create(name: String): TagRecord = TagRecord(UUID.randomUUID(), name)

        override fun findById(id: UUID): TagRecord? = null

        override fun findByIds(ids: List<UUID>): List<TagRecord> = emptyList()

        override fun findByName(name: String): TagRecord? =
            if (name == "home") {
                TagRecord(UUID.randomUUID(), name)
            } else {
                null
            }

        override fun update(
            id: UUID,
            name: String,
        ): TagRecord? = null

        override fun delete(id: UUID): Boolean = false

        override fun search(
            filter: TagSearchFilter,
            sort: List<TagSort>,
            page: PageRequest,
        ): PageResult<TagRecord> = PageResult(emptyList(), 0, page.number, page.size)
    }
}
