package com.example.service

import android.content.Context
import android.os.Environment
import com.example.data.AppDatabase
import com.example.data.FileDao
import com.example.data.IndexedFile
import com.example.parser.DocumentParser
import com.example.util.ArabicNormalizer
import com.example.util.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.coroutines.coroutineContext

data class IndexingState(
    val isIndexing: Boolean = false,
    val totalScanned: Int = 0,
    val totalIndexed: Int = 0,
    val currentFileName: String = "",
    val progress: Float = 0f,
    val isCompleted: Boolean = false
)

object FileIndexer {

    private val _indexingState = MutableStateFlow(IndexingState())
    val indexingState: StateFlow<IndexingState> = _indexingState.asStateFlow()

    // المجلدات غير المهمة أو المحمية التي يجب تجاهلها لتسريع الفهرسة
    private val IGNORED_DIR_NAMES = setOf(
        "android",
        ".thumbnails",
        ".git",
        ".gradle",
        ".idea",
        "node_modules",
        "cache",
        ".cache",
        "lost.dir",
        "system volume information",
        ".trash"
    )

    /**
     * إزالة أي ملفات تجريبية قديمة إن وجدت من قاعدة البيانات أو التخزين
     */
    suspend fun cleanupLegacyDemoFiles(context: Context) = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            db.fileDao().deleteDemoFiles()

            val extSampleDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "مستندات_DeepFinder")
            if (extSampleDir.exists()) {
                extSampleDir.deleteRecursively()
            }
            val intSampleDir = File(context.filesDir, "مستندات_DeepFinder")
            if (intSampleDir.exists()) {
                intSampleDir.deleteRecursively()
            }
        } catch (_: Exception) {
        }
    }

    /**
     * بدء عملية الفهرسة الشاملة لملفات الهاتف الحقيقية فقط
     */
    suspend fun startIndexing(context: Context, clearExisting: Boolean = false) = withContext(Dispatchers.IO) {
        if (_indexingState.value.isIndexing) return@withContext

        val db = AppDatabase.getDatabase(context)
        val dao = db.fileDao()

        // تهيئة محرك PDFBox
        DocumentParser.init(context)

        // تنظيف أي ملفات تجريبية قديمة
        cleanupLegacyDemoFiles(context)

        if (clearExisting) {
            dao.clearAll()
        }

        _indexingState.value = IndexingState(
            isIndexing = true,
            totalScanned = 0,
            totalIndexed = dao.getTotalCount(),
            currentFileName = "جاري فحص ملفات الهاتف...",
            progress = 0.05f
        )

        // تحديد مسارات المسح الحقيقية على الهاتف
        val searchRoots = mutableListOf<File>()
        val externalStorage = Environment.getExternalStorageDirectory()
        if (externalStorage != null && externalStorage.exists() && externalStorage.canRead()) {
            searchRoots.add(externalStorage)
        }

        val allFilesToProcess = mutableListOf<File>()

        // جمع الملفات من مسارات التخزين
        for (root in searchRoots) {
            collectFiles(root, allFilesToProcess)
            if (!coroutineContext.isActive) return@withContext
        }

        val totalToProcess = allFilesToProcess.size.coerceAtLeast(1)
        val batch = mutableListOf<IndexedFile>()
        var processedCount = 0

        for (file in allFilesToProcess) {
            if (!coroutineContext.isActive) break

            processedCount++
            _indexingState.value = _indexingState.value.copy(
                totalScanned = processedCount,
                currentFileName = file.name,
                progress = (processedCount.toFloat() / totalToProcess.toFloat()).coerceIn(0f, 1f)
            )

            try {
                val isDir = file.isDirectory
                val normName = ArabicNormalizer.normalize(file.name)
                var normContent = ""
                var preview = ""
                var isContentIndexed = false

                if (!isDir && FileHelper.isIndexableContent(file)) {
                    val rawText = DocumentParser.extractText(file)
                    if (rawText.isNotBlank()) {
                        normContent = ArabicNormalizer.normalize(rawText)
                        preview = rawText.take(200).replace("\n", " ").trim()
                        isContentIndexed = true
                    }
                }

                batch.add(
                    IndexedFile(
                        filePath = file.absolutePath,
                        fileName = file.name,
                        normalizedFileName = normName,
                        extension = if (isDir) "" else file.extension.lowercase(Locale.ROOT),
                        isDirectory = isDir,
                        fileSizeBytes = if (isDir) 0L else file.length(),
                        lastModified = file.lastModified(),
                        contentPreview = preview,
                        normalizedContent = normContent,
                        isContentIndexed = isContentIndexed
                    )
                )

                if (batch.size >= 50) {
                    dao.insertAll(batch)
                    val newCount = dao.getTotalCount()
                    _indexingState.value = _indexingState.value.copy(totalIndexed = newCount)
                    batch.clear()
                }
            } catch (_: Exception) {
                // تجاوز الملفات غير القابلة للقراءة
            }
        }

        if (batch.isNotEmpty()) {
            dao.insertAll(batch)
            batch.clear()
        }

        val finalCount = dao.getTotalCount()
        _indexingState.value = IndexingState(
            isIndexing = false,
            totalScanned = processedCount,
            totalIndexed = finalCount,
            currentFileName = "اكتملت الفهرسة بنجاح",
            progress = 1f,
            isCompleted = true
        )
    }

    private fun collectFiles(dir: File, result: MutableList<File>, depth: Int = 0) {
        if (depth > 12) return // حماية ضد الدوران اللانهائي
        if (!dir.exists() || !dir.canRead()) return

        val nameLower = dir.name.lowercase(Locale.ROOT)
        if (IGNORED_DIR_NAMES.contains(nameLower) || dir.name.startsWith(".")) {
            return
        }

        val children = dir.listFiles() ?: return
        for (child in children) {
            val childLower = child.name.lowercase(Locale.ROOT)
            if (IGNORED_DIR_NAMES.contains(childLower) || child.name.startsWith(".")) {
                continue
            }

            result.add(child)
            if (child.isDirectory) {
                collectFiles(child, result, depth + 1)
            }
        }
    }
}
