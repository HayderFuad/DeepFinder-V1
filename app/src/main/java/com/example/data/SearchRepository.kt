package com.example.data

import com.example.util.ArabicNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

enum class SearchFilter(val labelAr: String) {
    ALL("الكل"),
    NAME_ONLY("تطابق الاسم"),
    CONTENT_ONLY("تطابق المحتوى"),
    PDF("مستندات PDF"),
    OFFICE("Word و Excel"),
    TEXT("ملفات نصية"),
    FOLDERS("مجلدات")
}

class SearchRepository(private val fileDao: FileDao) {

    fun getTotalCountFlow(): Flow<Int> = fileDao.getTotalCountFlow()

    suspend fun getTotalCount(): Int = fileDao.getTotalCount()

    suspend fun getContentIndexedCount(): Int = fileDao.getContentIndexedCount()

    suspend fun clearIndex() = withContext(Dispatchers.IO) {
        fileDao.clearAll()
    }

    suspend fun search(rawQuery: String, filter: SearchFilter): List<SearchResult> = withContext(Dispatchers.IO) {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) {
            val recents = fileDao.getRecentFiles()
            return@withContext recents.map { file ->
                SearchResult(
                    file = file,
                    matchType = MatchType.NAME_MATCH,
                    snippet = if (file.contentPreview.isNotBlank()) file.contentPreview else null,
                    matchTerm = ""
                )
            }
        }

        val normQuery = ArabicNormalizer.normalize(trimmed)

        val rawResults: List<IndexedFile> = when (filter) {
            SearchFilter.ALL -> fileDao.searchAll(normQuery)
            SearchFilter.NAME_ONLY -> fileDao.searchByNameOnly(normQuery)
            SearchFilter.CONTENT_ONLY -> fileDao.searchByContentOnly(normQuery)
            SearchFilter.PDF -> fileDao.searchByExtensions(normQuery, listOf("pdf"))
            SearchFilter.OFFICE -> fileDao.searchByExtensions(normQuery, listOf("docx", "doc", "xlsx", "xls"))
            SearchFilter.TEXT -> fileDao.searchByExtensions(normQuery, listOf("txt", "md", "json", "csv", "xml", "log", "ini", "conf", "kt", "java", "py", "js"))
            SearchFilter.FOLDERS -> fileDao.searchDirectories(normQuery)
        }

        return@withContext rawResults.map { file ->
            val nameMatches = file.normalizedFileName.contains(normQuery)
            val contentMatches = file.normalizedContent.contains(normQuery)

            val matchType = when {
                nameMatches && contentMatches -> MatchType.BOTH
                nameMatches -> MatchType.NAME_MATCH
                else -> MatchType.CONTENT_MATCH
            }

            var snippet: String? = null
            if (contentMatches) {
                val snippetRes = ArabicNormalizer.extractSnippet(file.normalizedContent, normQuery)
                snippet = snippetRes?.snippetText ?: file.contentPreview
            } else if (file.contentPreview.isNotBlank()) {
                snippet = file.contentPreview
            }

            SearchResult(
                file = file,
                matchType = matchType,
                snippet = snippet,
                matchTerm = trimmed
            )
        }.sortedWith(
            compareByDescending<SearchResult> { it.matchType == MatchType.BOTH }
                .thenByDescending { it.matchType == MatchType.NAME_MATCH }
                .thenByDescending { it.file.lastModified }
        )
    }
}
