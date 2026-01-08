package ru.miet.kvosk.notes.http

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.miet.kvosk.notes.configureRouting
import ru.miet.kvosk.notes.configureSerialization
import ru.miet.kvosk.notes.db.PageRequest
import ru.miet.kvosk.notes.db.PageResult
import ru.miet.kvosk.notes.db.TagRecord
import ru.miet.kvosk.notes.db.TagSearchFilter
import ru.miet.kvosk.notes.db.TagSort
import ru.miet.kvosk.notes.service.ServiceError
import ru.miet.kvosk.notes.service.ServiceResult
import ru.miet.kvosk.notes.service.TagServiceContract
import java.util.UUID

class TagRoutesTest {
    private val tagId = UUID.fromString("22222222-2222-2222-2222-222222222222")
    private val tag = TagRecord(tagId, "home")

    @Test
    fun `create tag returns 201`() {
        testApplication {
            application {
                configureSerialization()
                configureRouting(FakeNoteService(), FakeTagService())
            }
            val response =
                client.post("/api/v1/tags") {
                    contentType(ContentType.Application.Json)
                    setBody("{\"name\":\"home\"}")
                }
            assertEquals(HttpStatusCode.Created, response.status)
        }
    }

    @Test
    fun `get tag returns 200`() {
        testApplication {
            application {
                configureSerialization()
                configureRouting(FakeNoteService(), FakeTagService())
            }
            val response = client.get("/api/v1/tags/$tagId")
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    @Test
    fun `search tags returns 200`() {
        testApplication {
            application {
                configureSerialization()
                configureRouting(FakeNoteService(), FakeTagService())
            }
            val response =
                client.post("/api/v1/tags:search") {
                    contentType(ContentType.Application.Json)
                    setBody("{\"filter\":{\"query\":\"ho\"},\"page\":{\"number\":1,\"size\":50}}")
                }
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    private class FakeNoteService : ru.miet.kvosk.notes.service.NoteServiceContract {
        override fun create(
            title: String,
            content: String,
            tagIds: List<UUID>,
        ): ServiceResult<ru.miet.kvosk.notes.service.NoteDetails> {
            return ServiceResult.Error(ServiceError.Validation("title", "stub"))
        }

        override fun getById(
            id: UUID,
            includeTags: Boolean,
        ): ServiceResult<ru.miet.kvosk.notes.service.NoteDetails> {
            return ServiceResult.Error(ServiceError.NotFound("note", id.toString()))
        }

        override fun update(
            id: UUID,
            title: String,
            content: String,
            isArchived: Boolean,
            tagIds: List<UUID>,
        ): ServiceResult<ru.miet.kvosk.notes.service.NoteDetails> {
            return ServiceResult.Error(ServiceError.NotFound("note", id.toString()))
        }

        override fun updatePartial(
            id: UUID,
            title: String?,
            content: String?,
            isArchived: Boolean?,
            tagIds: List<UUID>?,
        ): ServiceResult<ru.miet.kvosk.notes.service.NoteDetails> {
            return ServiceResult.Error(ServiceError.NotFound("note", id.toString()))
        }

        override fun delete(id: UUID): ServiceResult<Boolean> {
            return ServiceResult.Success(true)
        }

        override fun search(
            filter: ru.miet.kvosk.notes.db.NoteSearchFilter,
            sort: List<ru.miet.kvosk.notes.db.NoteSort>,
            page: PageRequest,
            includeTags: Boolean,
        ): ServiceResult<PageResult<ru.miet.kvosk.notes.service.NoteDetails>> {
            return ServiceResult.Success(PageResult(emptyList(), 0, page.number, page.size))
        }
    }

    private inner class FakeTagService : TagServiceContract {
        override fun create(name: String): ServiceResult<TagRecord> {
            return ServiceResult.Success(tag)
        }

        override fun getById(id: UUID): ServiceResult<TagRecord> {
            return ServiceResult.Success(tag)
        }

        override fun search(
            filter: TagSearchFilter,
            sort: List<TagSort>,
            page: PageRequest,
        ): ServiceResult<PageResult<TagRecord>> {
            return ServiceResult.Success(PageResult(listOf(tag), 1, page.number, page.size))
        }
    }
}
