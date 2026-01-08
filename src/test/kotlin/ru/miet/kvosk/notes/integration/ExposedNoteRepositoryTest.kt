package ru.miet.kvosk.notes.integration

import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import ru.miet.kvosk.notes.TestPostgresHelper
import ru.miet.kvosk.notes.db.ExposedNoteRepository
import ru.miet.kvosk.notes.db.ExposedTagRepository
import ru.miet.kvosk.notes.db.NoteSearchFilter
import ru.miet.kvosk.notes.db.NoteSort
import ru.miet.kvosk.notes.db.NoteSortField
import ru.miet.kvosk.notes.db.NoteTagsTable
import ru.miet.kvosk.notes.db.NotesTable
import ru.miet.kvosk.notes.db.PageRequest
import ru.miet.kvosk.notes.db.SortDirection
import ru.miet.kvosk.notes.db.TagsTable
import java.time.Instant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExposedNoteRepositoryTest {
    private val noteRepository = ExposedNoteRepository()
    private val tagRepository = ExposedTagRepository()

    init {
        val db = TestPostgresHelper.startOrSkip()
        Database.connect(
            url = db.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = db.username,
            password = db.password,
        )
        Flyway.configure()
            .dataSource(db.jdbcUrl, db.username, db.password)
            .locations("classpath:db/migration")
            .load()
            .migrate()
    }

    @AfterAll
    fun stopContainer() {
        TestPostgresHelper.stop()
    }

    @Test
    fun `search by query matches title and content`() {
        transaction {
            NoteTagsTable.deleteAll()
            NotesTable.deleteAll()
            TagsTable.deleteAll()
        }

        val note1 = noteRepository.create("Buy milk", "eggs", false, Instant.parse("2026-01-01T00:00:00Z"))
        noteRepository.create("Other", "something else", false, Instant.parse("2026-01-02T00:00:00Z"))

        val result =
            noteRepository.search(
                NoteSearchFilter(query = "milk"),
                emptyList(),
                PageRequest(1, 20),
            )

        assertEquals(1, result.total)
        assertEquals(note1.id, result.items.first().id)
    }

    @Test
    fun `search by tag ids filters notes`() {
        transaction {
            NoteTagsTable.deleteAll()
            NotesTable.deleteAll()
            TagsTable.deleteAll()
        }

        val note1 = noteRepository.create("Tagged", "content", false, Instant.parse("2026-01-01T00:00:00Z"))
        noteRepository.create("Untagged", "content", false, Instant.parse("2026-01-02T00:00:00Z"))

        val tag = tagRepository.create("home")
        noteRepository.setTags(note1.id, listOf(tag.id))

        val result =
            noteRepository.search(
                NoteSearchFilter(tagIds = listOf(tag.id)),
                emptyList(),
                PageRequest(1, 20),
            )

        assertEquals(1, result.total)
        assertEquals(note1.id, result.items.first().id)
    }

    @Test
    fun `pagination returns total and pages`() {
        transaction {
            NoteTagsTable.deleteAll()
            NotesTable.deleteAll()
            TagsTable.deleteAll()
        }

        noteRepository.create("Note1", "content", false, Instant.parse("2026-01-01T00:00:00Z"))
        noteRepository.create("Note2", "content", false, Instant.parse("2026-01-02T00:00:00Z"))

        val page1 = noteRepository.search(NoteSearchFilter(), emptyList(), PageRequest(1, 1))
        val page2 = noteRepository.search(NoteSearchFilter(), emptyList(), PageRequest(2, 1))

        assertEquals(2, page1.total)
        assertEquals(1, page1.items.size)
        assertEquals(1, page2.items.size)
        assertNotEquals(page1.items.first().id, page2.items.first().id)
    }

    @Test
    fun `sort by created_at desc`() {
        transaction {
            NoteTagsTable.deleteAll()
            NotesTable.deleteAll()
            TagsTable.deleteAll()
        }

        val note1 = noteRepository.create("Old", "content", false, Instant.parse("2026-01-01T00:00:00Z"))
        val note2 = noteRepository.create("New", "content", false, Instant.parse("2026-02-01T00:00:00Z"))

        val result =
            noteRepository.search(
                NoteSearchFilter(),
                listOf(NoteSort(NoteSortField.CREATED_AT, SortDirection.DESC)),
                PageRequest(1, 20),
            )

        assertEquals(2, result.total)
        assertEquals(note2.id, result.items.first().id)
        assertEquals(note1.id, result.items.last().id)
    }
}
