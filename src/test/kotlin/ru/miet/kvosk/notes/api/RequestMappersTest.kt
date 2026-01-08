package ru.miet.kvosk.notes.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class RequestMappersTest {
    @Test
    fun `note search defaults page size`() {
        val request = NoteSearchRequest()
        val page = request.toPage()
        assertEquals(1, page.number)
        assertEquals(20, page.size)
    }

    @Test
    fun `tag search defaults page size`() {
        val request = TagSearchRequest()
        val page = request.toPage()
        assertEquals(1, page.number)
        assertEquals(50, page.size)
    }

    @Test
    fun `note sort mapping`() {
        val request =
            NoteSearchRequest(
                sort =
                    listOf(
                        NoteSortDto(field = NoteSortFieldDto.CREATED_AT, direction = SortDirectionDto.DESC),
                    ),
            )
        val sort = request.toSort()
        assertEquals(1, sort.size)
        assertEquals(ru.miet.kvosk.notes.db.NoteSortField.CREATED_AT, sort.first().field)
        assertEquals(ru.miet.kvosk.notes.db.SortDirection.DESC, sort.first().direction)
    }

    @Test
    fun `invalid tag ids throw`() {
        val request =
            NoteSearchRequest(
                filter = NoteSearchFilterDto(tagIds = listOf("not-a-uuid")),
            )
        assertThrows(IllegalArgumentException::class.java) {
            request.toFilter()
        }
    }
}
