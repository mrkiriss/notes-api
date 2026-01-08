package ru.miet.kvosk.notes.http

import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.miet.kvosk.notes.configureRouting
import ru.miet.kvosk.notes.configureSerialization
import ru.miet.kvosk.notes.db.NoteRecord
import ru.miet.kvosk.notes.db.NoteSearchFilter
import ru.miet.kvosk.notes.db.NoteSort
import ru.miet.kvosk.notes.db.PageRequest
import ru.miet.kvosk.notes.db.PageResult
import ru.miet.kvosk.notes.db.TagRecord
import ru.miet.kvosk.notes.service.NoteDetails
import ru.miet.kvosk.notes.service.NoteServiceContract
import ru.miet.kvosk.notes.service.ServiceError
import ru.miet.kvosk.notes.service.ServiceResult
import java.time.Instant
import java.util.UUID

class NoteRoutesTest {
    private val noteId = UUID.fromString("11111111-1111-1111-1111-111111111111")
    private val tagId = UUID.fromString("22222222-2222-2222-2222-222222222222")

    private val note =
        NoteRecord(
            id = noteId,
            title = "Buy groceries",
            content = "Milk, eggs",
            isArchived = false,
            createdAt = Instant.parse("2026-01-08T08:59:04Z"),
            updatedAt = Instant.parse("2026-01-08T08:59:04Z"),
        )

    private val tag = TagRecord(tagId, "home")

    @Test
    fun `create note returns 201`() {
        testApplication {
            application {
                configureSerialization()
                configureRouting(FakeNoteService(), FakeTagService())
            }
            val response =
                client.post("/api/v1/notes") {
                    contentType(ContentType.Application.Json)
                    setBody("{\"title\":\"Buy groceries\",\"content\":\"Milk\",\"tag_ids\":[\"$tagId\"]}")
                }
            assertEquals(HttpStatusCode.Created, response.status)
        }
    }

    @Test
    fun `get note returns 200`() {
        testApplication {
            application {
                configureSerialization()
                configureRouting(FakeNoteService(), FakeTagService())
            }
            val response = client.get("/api/v1/notes/$noteId?include=tags")
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    @Test
    fun `replace note returns 200`() {
        testApplication {
            application {
                configureSerialization()
                configureRouting(FakeNoteService(), FakeTagService())
            }
            val response =
                client.put("/api/v1/notes/$noteId") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        "{\"title\":\"Buy groceries today\",\"content\":\"Milk\"," +
                            "\"is_archived\":false,\"tag_ids\":[\"$tagId\"]}",
                    )
                }
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    @Test
    fun `patch note returns 200`() {
        testApplication {
            application {
                configureSerialization()
                configureRouting(FakeNoteService(), FakeTagService())
            }
            val response =
                client.patch("/api/v1/notes/$noteId") {
                    contentType(ContentType.Application.Json)
                    setBody("{\"content\":\"Updated\"}")
                }
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    @Test
    fun `delete note returns 200`() {
        testApplication {
            application {
                configureSerialization()
                configureRouting(FakeNoteService(), FakeTagService())
            }
            val response = client.delete("/api/v1/notes/$noteId")
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    @Test
    fun `search notes returns 200`() {
        testApplication {
            application {
                configureSerialization()
                configureRouting(FakeNoteService(), FakeTagService())
            }
            val response =
                client.post("/api/v1/notes:search") {
                    contentType(ContentType.Application.Json)
                    setBody("{\"filter\":{\"query\":\"Buy\"},\"page\":{\"number\":1,\"size\":20}}")
                }
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    @Test
    fun `search one note returns 200`() {
        testApplication {
            application {
                configureSerialization()
                configureRouting(FakeNoteService(), FakeTagService())
            }
            val response =
                client.post("/api/v1/notes:search-one") {
                    contentType(ContentType.Application.Json)
                    setBody("{\"filter\":{\"title\":\"Buy groceries\"}}")
                }
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    private inner class FakeNoteService : NoteServiceContract {
        override fun create(
            title: String,
            content: String,
            tagIds: List<UUID>,
        ): ServiceResult<NoteDetails> {
            return ServiceResult.Success(NoteDetails(note, listOf(tag)))
        }

        override fun getById(
            id: UUID,
            includeTags: Boolean,
        ): ServiceResult<NoteDetails> {
            val tags = if (includeTags) listOf(tag) else emptyList()
            return ServiceResult.Success(NoteDetails(note, tags))
        }

        override fun update(
            id: UUID,
            title: String,
            content: String,
            isArchived: Boolean,
            tagIds: List<UUID>,
        ): ServiceResult<NoteDetails> {
            return ServiceResult.Success(NoteDetails(note, listOf(tag)))
        }

        override fun updatePartial(
            id: UUID,
            title: String?,
            content: String?,
            isArchived: Boolean?,
            tagIds: List<UUID>?,
        ): ServiceResult<NoteDetails> {
            return ServiceResult.Success(NoteDetails(note, listOf(tag)))
        }

        override fun delete(id: UUID): ServiceResult<Boolean> {
            return ServiceResult.Success(true)
        }

        override fun search(
            filter: NoteSearchFilter,
            sort: List<NoteSort>,
            page: PageRequest,
            includeTags: Boolean,
        ): ServiceResult<PageResult<NoteDetails>> {
            val items = listOf(NoteDetails(note, if (includeTags) listOf(tag) else emptyList()))
            return ServiceResult.Success(PageResult(items, 1, page.number, page.size))
        }
    }

    private class FakeTagService : ru.miet.kvosk.notes.service.TagServiceContract {
        override fun create(name: String): ServiceResult<TagRecord> {
            return ServiceResult.Error(ServiceError.Validation("name", "stub"))
        }

        override fun getById(id: UUID): ServiceResult<TagRecord> {
            return ServiceResult.Error(ServiceError.NotFound("tag", id.toString()))
        }

        override fun search(
            filter: ru.miet.kvosk.notes.db.TagSearchFilter,
            sort: List<ru.miet.kvosk.notes.db.TagSort>,
            page: PageRequest,
        ): ServiceResult<PageResult<TagRecord>> {
            return ServiceResult.Success(PageResult(emptyList(), 0, page.number, page.size))
        }
    }
}
