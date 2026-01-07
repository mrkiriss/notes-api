package ru.miet.kvosk.notes.db

import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.util.UUID

interface TagRepository {
    fun create(name: String): TagRecord
    fun findById(id: UUID): TagRecord?
    fun findByName(name: String): TagRecord?
    fun update(id: UUID, name: String): TagRecord?
    fun delete(id: UUID): Boolean
    fun search(filter: TagSearchFilter, sort: List<TagSort>, page: PageRequest): PageResult<TagRecord>
}

class ExposedTagRepository : TagRepository {
    override fun create(name: String): TagRecord = dbQuery {
        val insert = TagsTable.insert {
            it[TagsTable.name] = name
        }
        insert.resultedValues?.singleOrNull()?.toTagRecord()
            ?: error("Failed to insert tag")
    }

    override fun findById(id: UUID): TagRecord? = dbQuery {
        TagsTable.selectAll()
            .where { TagsTable.id eq id }
            .singleOrNull()
            ?.toTagRecord()
    }

    override fun findByName(name: String): TagRecord? = dbQuery {
        TagsTable.selectAll()
            .where { TagsTable.name eq name }
            .singleOrNull()
            ?.toTagRecord()
    }

    override fun update(id: UUID, name: String): TagRecord? = dbQuery {
        val updated = TagsTable.update({ TagsTable.id eq id }) {
            it[TagsTable.name] = name
        }
        if (updated == 0) {
            null
        } else {
            findById(id)
        }
    }

    override fun delete(id: UUID): Boolean = dbQuery {
        NoteTagsTable.deleteWhere { NoteTagsTable.tagId eq id }
        TagsTable.deleteWhere { TagsTable.id eq id } > 0
    }

    override fun search(filter: TagSearchFilter, sort: List<TagSort>, page: PageRequest): PageResult<TagRecord> =
        dbQuery {
            val baseQuery = TagsTable.selectAll()
            val whereConditions = mutableListOf<Op<Boolean>>()

            filter.query?.let { query ->
                val pattern = "%${query.lowercase()}%"
                whereConditions += TagsTable.name.lowerCase() like pattern
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
                    TagSortField.NAME -> TagsTable.name
                }
                column to if (it.direction == SortDirection.ASC) SortOrder.ASC else SortOrder.DESC
            }

            val query = if (orderBy.isEmpty()) {
                filteredQuery
            } else {
                filteredQuery.orderBy(*orderBy.toTypedArray())
            }

            val offset = ((page.number - 1).coerceAtLeast(0)) * page.size
            val items = query.limit(page.size, offset.toLong()).map { it.toTagRecord() }

            PageResult(items, total, page.number, page.size)
        }
}

private fun ResultRow.toTagRecord(): TagRecord {
    return TagRecord(
        id = this[TagsTable.id],
        name = this[TagsTable.name],
    )
}
