package ru.miet.kvosk.notes.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.miet.kvosk.notes.db.NoteRecord
import ru.miet.kvosk.notes.db.NoteRepository
import ru.miet.kvosk.notes.db.NoteSearchFilter
import ru.miet.kvosk.notes.db.NoteSort
import ru.miet.kvosk.notes.db.PageRequest
import ru.miet.kvosk.notes.db.PageResult
import ru.miet.kvosk.notes.db.TagRecord
import ru.miet.kvosk.notes.db.TagRepository
import java.time.Instant
import java.util.UUID

class NoteServiceTest {
    private val noteRepository = FakeNoteRepository()
    private val tagRepository = FakeTagRepository()
    private val service = NoteService(noteRepository, tagRepository)

    @Test
    fun `create rejects blank title`() {
        val result = service.create(" ", "content", emptyList())
        assertTrue(result is ServiceResult.Error)
        val error = (result as ServiceResult.Error).error
        assertTrue(error is ServiceError.Validation)
        assertEquals("title", (error as ServiceError.Validation).field)
    }

    @Test
    fun `create rejects unknown tag`() {
        val tagId = UUID.randomUUID()
        val result = service.create("Title", "content", listOf(tagId))
        assertTrue(result is ServiceResult.Error)
        val error = (result as ServiceResult.Error).error
        assertTrue(error is ServiceError.Validation)
    }

    @Test
    fun `update returns not found for missing note`() {
        val result =
            service.update(
                id = UUID.randomUUID(),
                title = "Title",
                content = "content",
                isArchived = false,
                tagIds = emptyList(),
            )
        assertTrue(result is ServiceResult.Error)
        val error = (result as ServiceResult.Error).error
        assertTrue(error is ServiceError.NotFound)
    }

    @Test
    fun `search rejects invalid page`() {
        val result =
            service.search(
                filter = NoteSearchFilter(),
                sort = emptyList(),
                page = PageRequest(number = 0, size = 10),
                includeTags = false,
            )
        assertTrue(result is ServiceResult.Error)
        val error = (result as ServiceResult.Error).error
        assertTrue(error is ServiceError.Validation)
        assertEquals("page.number", (error as ServiceError.Validation).field)
    }

    private class FakeNoteRepository : NoteRepository {
        override fun create(
            title: String,
            content: String,
            isArchived: Boolean,
            now: Instant,
        ): NoteRecord = NoteRecord(UUID.randomUUID(), title, content, isArchived, now, now)

        override fun findById(id: UUID): NoteRecord? = null

        override fun update(
            id: UUID,
            title: String,
            content: String,
            isArchived: Boolean,
            now: Instant,
        ): NoteRecord? = null

        override fun updatePartial(
            id: UUID,
            title: String?,
            content: String?,
            isArchived: Boolean?,
            now: Instant,
        ): NoteRecord? = null

        override fun delete(id: UUID): Boolean = false

        override fun setTags(
            noteId: UUID,
            tagIds: List<UUID>,
        ) = Unit

        override fun getTags(noteId: UUID): List<TagRecord> = emptyList()

        override fun search(
            filter: NoteSearchFilter,
            sort: List<NoteSort>,
            page: PageRequest,
        ): PageResult<NoteRecord> = PageResult(emptyList(), 0, page.number, page.size)
    }

    private class FakeTagRepository : TagRepository {
        override fun create(name: String): TagRecord = TagRecord(UUID.randomUUID(), name)

        override fun findById(id: UUID): TagRecord? = null

        override fun findByIds(ids: List<UUID>): List<TagRecord> = emptyList()

        override fun findByName(name: String): TagRecord? = null

        override fun update(
            id: UUID,
            name: String,
        ): TagRecord? = null

        override fun delete(id: UUID): Boolean = false

        override fun search(
            filter: ru.miet.kvosk.notes.db.TagSearchFilter,
            sort: List<ru.miet.kvosk.notes.db.TagSort>,
            page: PageRequest,
        ): PageResult<TagRecord> = PageResult(emptyList(), 0, page.number, page.size)
    }
}
