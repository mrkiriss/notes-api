package ru.miet.kvosk.notes.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.UUID

private const val NOTE_TITLE_MAX_LENGTH = 200
private const val TAG_NAME_MAX_LENGTH = 50

object NotesTable : Table("notes") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val title = varchar("title", NOTE_TITLE_MAX_LENGTH)
    val content = text("content")
    val isArchived = bool("is_archived").default(false)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object TagsTable : Table("tags") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val name = varchar("name", TAG_NAME_MAX_LENGTH).uniqueIndex()

    override val primaryKey = PrimaryKey(id)
}

object NoteTagsTable : Table("note_tags") {
    val noteId = uuid("note_id").references(NotesTable.id)
    val tagId = uuid("tag_id").references(TagsTable.id)

    override val primaryKey = PrimaryKey(noteId, tagId)
}
