package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "indexed_files",
    indices = [
        Index(value = ["filePath"], unique = true),
        Index(value = ["normalizedFileName"]),
        Index(value = ["extension"]),
        Index(value = ["isDirectory"])
    ]
)
data class IndexedFile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val fileName: String,
    val normalizedFileName: String,
    val extension: String,
    val isDirectory: Boolean,
    val fileSizeBytes: Long,
    val lastModified: Long,
    val contentPreview: String = "",
    val normalizedContent: String = "",
    val isContentIndexed: Boolean = false
)

enum class MatchType {
    NAME_MATCH,
    CONTENT_MATCH,
    BOTH
}

data class SearchResult(
    val file: IndexedFile,
    val matchType: MatchType,
    val snippet: String? = null,
    val rawContent: String? = null,
    val matchTerm: String = ""
)
