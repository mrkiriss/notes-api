package ru.miet.kvosk.notes.db

import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inSubQuery
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.slice
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

interface NoteRepository {
    fun create(title: String, content: String, isArchived: Boolean, now: Instant = Instant.now()): NoteRecord
    fun findById(id: UUID): NoteRecord?
    fun update(id: UUID, title: String, content: String, isArchived: Boolean, now: Instant = Instant.now()): NoteRecord?
    fun updatePartial(
        id: UUID,
        title: String? = null,
        content: String? = null,
        isArchived: Boolean? = null,
        now: Instant = Instant.now(),
    ): NoteRecord?
    fun delete(id: UUID): Boolean
    fun setTags(noteId: UUID, tagIds: List<UUID>)
    fun getTags(noteId: UUID): List<TagRecord>
    fun search(filter: NoteSearchFilter, sort: List<NoteSort>, page: PageRequest): PageResult<NoteRecord>
}

class ExposedNoteRepository : NoteRepository {
    override fun create(title: String, content: String, isArchived: Boolean, now: Instant): NoteRecord = dbQuery {
        val insert = NotesTable.insert {
            it[NotesTable.title] = title
            it[NotesTable.content] = content
            it[NotesTable.isArchived] = isArchived
            it[NotesTable.createdAt] = now
            it[NotesTable.updatedAt] = now
        }
        insert.resultedValues?.singleOrNull()?.toNoteRecord()
            ?: error("Failed to insert note")
    }

    override fun findById(id: UUID): NoteRecord? = dbQuery {
        NotesTable.selectAll()
            .where { NotesTable.id eq id }
            .singleOrNull()
            ?.toNoteRecord()
    }

    override fun update(id: UUID, title: String, content: String, isArchived: Boolean, now: Instant): NoteRecord? {
        return updatePartial(id, title, content, isArchived, now)
    }

    override fun updatePartial(
        id: UUID,
        title: String?,
        content: String?,
        isArchived: Boolean?,
        now: Instant,
    ): NoteRecord? = dbQuery {
        val updated = NotesTable.update({ NotesTable.id eq id }) {
            if (title != null) it[NotesTable.title] = title
            if (content != null) it[NotesTable.content] = content
            if (isArchived != null) it[NotesTable.isArchived] = isArchived
            it[NotesTable.updatedAt] = now
        }
        if (updated == 0) {
            null
        } else {
            findById(id)
        }
    }

    override fun delete(id: UUID): Boolean = dbQuery {
        NoteTagsTable.deleteWhere { NoteTagsTable.noteId eq id }
        NotesTable.deleteWhere { NotesTable.id eq id } > 0
    }

    override fun setTags(noteId: UUID, tagIds: List<UUID>): Unit = dbQuery {
        NoteTagsTable.deleteWhere { NoteTagsTable.noteId eq noteId }
        if (tagIds.isNotEmpty()) {
            NoteTagsTable.batchInsert(tagIds) { tagId ->
                this[NoteTagsTable.noteId] = noteId
                this[NoteTagsTable.tagId] = tagId
            }
        }
    }

    override fun getTags(noteId: UUID): List<TagRecord> = dbQuery {
        TagsTable.join(NoteTagsTable, JoinType.INNER, TagsTable.id, NoteTagsTable.tagId)
            .selectAll()
            .where { NoteTagsTable.noteId eq noteId }
            .map { it.toTagRecord() }
    }

    override fun search(filter: NoteSearchFilter, sort: List<NoteSort>, page: PageRequest): PageResult<NoteRecord> =
        dbQuery {
            val baseQuery = NotesTable.selectAll()
            val whereConditions = mutableListOf<Op<Boolean>>()

            filter.query?.let { query ->
                val pattern = "%${query.lowercase()}%"
                whereConditions += (NotesTable.title.lowerCase() like pattern) or
                    (NotesTable.content.lowerCase() like pattern)
            }

            filter.isArchived?.let { value ->
                whereConditions += NotesTable.isArchived eq value
            }

            filter.tagIds?.takeIf { it.isNotEmpty() }?.let { tagIds ->
                val noteIds = NoteTagsTable
                    .selectAll()
                    .where { NoteTagsTable.tagId inList tagIds }
                    .map { it[NoteTagsTable.noteId] }
                    .distinct()
                if (noteIds.isEmpty()) {
                    return@dbQuery PageResult(emptyList(), 0, page.number, page.size)
                }
                whereConditions += NotesTable.id inList noteIds
            }

            val filteredQuery = if (whereConditions.isEmpty()) {
                baseQuery
            } else {
                baseQuery.where { whereConditions.reduce { acc, op -> acc and op } }
            }

            val total = filteredQuery
                .copy()
                .count()

            val orderBy = sort.map {
                val column = when (it.field) {
                    NoteSortField.CREATED_AT -> NotesTable.createdAt
                    NoteSortField.UPDATED_AT -> NotesTable.updatedAt
                    NoteSortField.TITLE -> NotesTable.title
                }
                column to if (it.direction == SortDirection.ASC) SortOrder.ASC else SortOrder.DESC
            }

            val query = if (orderBy.isEmpty()) {
                filteredQuery
            } else {
                filteredQuery.orderBy(*orderBy.toTypedArray())
            }

            val offset = ((page.number - 1).coerceAtLeast(0)) * page.size
            val items = query.limit(page.size, offset.toLong()).map { it.toNoteRecord() }

            PageResult(items, total, page.number, page.size)
        }
}

private fun ResultRow.toNoteRecord(): NoteRecord {
    return NoteRecord(
        id = this[NotesTable.id],
        title = this[NotesTable.title],
        content = this[NotesTable.content],
        isArchived = this[NotesTable.isArchived],
        createdAt = this[NotesTable.createdAt],
        updatedAt = this[NotesTable.updatedAt],
    )
}

private fun ResultRow.toTagRecord(): TagRecord {
    return TagRecord(
        id = this[TagsTable.id],
        name = this[TagsTable.name],
    )
}
