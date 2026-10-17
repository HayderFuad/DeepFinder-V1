package com.example.service

import android.content.Context
import android.os.Environment
import com.example.data.AppDatabase
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
     * إنشاء ملفات تجريبية غنية بالمحتوى والأسماء لاختبار البحث الفوري والمطابقة المباشرة
     */
    fun createSampleFilesIfEmpty(context: Context) {
        val samplesDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "مستندات_DeepFinder")
        if (!samplesDir.exists()) {
            samplesDir.mkdirs()
        }

        val bookFile = File(samplesDir, "كتاب_البرمجة_بلغة_كوتلن.txt")
        if (!bookFile.exists()) {
            bookFile.writeText(
                "كتاب البرمجة الشامل للمطورين:\n" +
                        "تعلم تطوير تطبيقات أندرويد الحديثة باستخدام لغة كوتلن (Kotlin) مع واجهات Jetpack Compose " +
                        "ومحركات البحث الفوري ومكتبة Room وتقنيات Clean Architecture و Material Design 3.\n" +
                        "يحتوي الكتاب على أمثلة عملية لبناء تطبيقات احترافية وسريعة الأداء."
            )
        }

        val reportFile = File(samplesDir, "تقرير_المشروع_الهندسي.txt")
        if (!reportFile.exists()) {
            reportFile.writeText(
                "الجمهورية اليمنية - مشروع التخرج الهندسي والتقني 2026\n" +
                        "عنوان المشروع: محرك البحث الذكي الشامل للهاتف (DeepFinder Android).\n" +
                        "أعد هذا المشروع المهندس ابراهيم احمد محمد حاتم بالتعاون مع المهندس حيدر فؤاد.\n" +
                        "يقوم التطبيق بالبحث في أسماء الملفات ومحتواها الداخلي بسرعة فائقة مع الحفاظ على الخصوصية الكاملة وبدون إنترنت."
            )
        }

        val notesFile = File(samplesDir, "قائمة_المهام_والملاحظات.md")
        if (!notesFile.exists()) {
            notesFile.writeText(
                "# خطة عمل وتطوير تطبيق DeepFinder\n" +
                        "- [x] بناء محرك تطبيع الحروف العربية Arabic Normalizer لدعم همزات إبراهيم أحمد وحيدر فؤاد.\n" +
                        "- [x] دعم استخراج النصوص من ملفات PDF و Word و Excel والنصوص البرمجية.\n" +
                        "- [x] إضافة القفل ببصمة الإصبع Biometric Lock لحماية خصوصية المستخدم.\n" +
                        "- [ ] تحديث فهرس الملفات تلقائياً عند إضافة أي مستند جديد."
            )
        }

        val teamFile = File(samplesDir, "بيانات_فريق_العمل.json")
        if (!teamFile.exists()) {
            teamFile.writeText(
                "{\n" +
                        "  \"project\": \"DeepFinder Android\",\n" +
                        "  \"engineers\": [\n" +
                        "    {\"name\": \"ابراهيم احمد محمد حاتم\", \"role\": \"Architect & Security\", \"phone\": \"+967734414267\"},\n" +
                        "    {\"name\": \"حيدر فؤاد\", \"role\": \"Lead Android Architect\", \"phone\": \"+967776797221\"}\n" +
                        "  ]\n" +
                        "}"
            )
        }

        val budgetFile = File(samplesDir, "ميزانية_المشروع_2026.csv")
        if (!budgetFile.exists()) {
            budgetFile.writeText(
                "البند,المبلغ,المسؤول,الحالة\n" +
                        "تراخيص الفهرسة,0,ابراهيم احمد محمد حاتم,مكتمل (مفتوح المصدر)\n" +
                        "سيرفرات سحابية,0,حيدر فؤاد,ملغي (أوفلاين 100% بدون إنترنت)\n" +
                        "أدوات الاختبار,200,فريق التطوير,معتمد"
            )
        }

        val logFile = File(samplesDir, "سجل_العمليات_التقنية.log")
        if (!logFile.exists()) {
            logFile.writeText(
                "[INFO] 2026-10-16 08:30:12 - تم تشغيل محرك الفهرسة بنجاح.\n" +
                        "[INFO] 2026-10-16 08:30:15 - تمت معالجة كتاب البرمجة وتطبيع النصوص العربية.\n" +
                        "[SUCCESS] جاهز للبحث والاستعلام الفوري."
            )
        }
    }

    /**
     * بدء عملية الفهرسة الشاملة للهاتف
     */
    suspend fun startIndexing(context: Context, clearExisting: Boolean = false) = withContext(Dispatchers.IO) {
        if (_indexingState.value.isIndexing) return@withContext

        val db = AppDatabase.getDatabase(context)
        val dao = db.fileDao()

        // تهيئة محرك PDFBox
        DocumentParser.init(context)

        // إنشاء ملفات توضيحية تجريبية للبحث السريع
        createSampleFilesIfEmpty(context)

        if (clearExisting) {
            dao.clearAll()
        }

        _indexingState.value = IndexingState(
            isIndexing = true,
            totalScanned = 0,
            totalIndexed = dao.getTotalCount(),
            currentFileName = "جاري تحضير الملفات...",
            progress = 0.05f
        )

        // تحديد مسارات المسح
        val searchRoots = mutableListOf<File>()

        // 1. مجلد المستندات التجريبي للتطبيق
        val sampleDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "مستندات_DeepFinder")
        if (sampleDir.exists()) searchRoots.add(sampleDir)

        // 2. مجلدات التخزين الخارجي
        val externalStorage = Environment.getExternalStorageDirectory()
        if (externalStorage != null && externalStorage.exists() && externalStorage.canRead()) {
            searchRoots.add(externalStorage)
        }

        val allFilesToProcess = mutableListOf<File>()

        // جمع الملفات أولاً
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
            } catch (e: Exception) {
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
            // تجاهل المجلدات الخفية والنظامية
            if (!dir.name.contains("مستندات_DeepFinder")) return
        }

        val children = dir.listFiles() ?: return
        for (child in children) {
            val childLower = child.name.lowercase(Locale.ROOT)
            if (IGNORED_DIR_NAMES.contains(childLower) || (child.name.startsWith(".") && !child.name.contains("مستندات_DeepFinder"))) {
                continue
            }

            result.add(child)
            if (child.isDirectory) {
                collectFiles(child, result, depth + 1)
            }
        }
    }
}
