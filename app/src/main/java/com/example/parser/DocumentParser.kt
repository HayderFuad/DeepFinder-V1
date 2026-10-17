package com.example.parser

import android.content.Context
import android.util.Xml
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.zip.ZipInputStream

/**
 * محللات المستندات خفيفة الوزن على الجهاز (On-Device Lightweight Parsers)
 * مصممة لحماية الذاكرة (Memory Safe) وتجنب الـ OutOfMemoryError
 */
object DocumentParser {

    private var isPdfBoxInitialized = false
    private const val MAX_TEXT_FILE_BYTES = 512 * 1024 // 512 KB كحد أقصى لكل ملف نصي
    private const val MAX_PDF_PAGES = 30 // قراءة أول 30 صفحة لحماية الأداء

    fun init(context: Context) {
        if (!isPdfBoxInitialized) {
            try {
                PDFBoxResourceLoader.init(context.applicationContext)
                isPdfBoxInitialized = true
            } catch (e: Exception) {
                // تجاهل إذا تمت تهيئته مسبقاً
            }
        }
    }

    /**
     * استخراج النص الخام من أي نوع ملف مدعوم
     */
    fun extractText(file: File): String {
        if (!file.exists() || !file.canRead() || file.isDirectory) return ""

        return try {
            when (file.extension.lowercase(Locale.ROOT)) {
                "txt", "md", "json", "csv", "xml", "log", "ini", "conf",
                "kt", "java", "py", "js", "ts", "html", "css", "c", "cpp", "h", "sh", "sql" -> {
                    extractPlainText(file)
                }
                "pdf" -> {
                    extractPdfText(file)
                }
                "docx" -> {
                    extractDocxText(file)
                }
                "xlsx" -> {
                    extractXlsxText(file)
                }
                else -> ""
            }
        } catch (e: Throwable) {
            ""
        }
    }

    /**
     * استخراج النصوص العادية مع حماية استهلاك الذاكرة
     */
    private fun extractPlainText(file: File): String {
        val length = file.length().coerceAtMost(MAX_TEXT_FILE_BYTES.toLong()).toInt()
        val buffer = CharArray(length)
        FileInputStream(file).use { fis ->
            InputStreamReader(fis, StandardCharsets.UTF_8).use { isr ->
                BufferedReader(isr).use { reader ->
                    val read = reader.read(buffer, 0, length)
                    return if (read > 0) String(buffer, 0, read) else ""
                }
            }
        }
    }

    /**
     * استخراج نصوص PDF باستخدام PDFBox-Android مع حد أقصى للصفحات
     */
    private fun extractPdfText(file: File): String {
        try {
            PDDocument.load(file).use { document ->
                if (document.isEncrypted) return ""
                val stripper = PDFTextStripper()
                stripper.startPage = 1
                stripper.endPage = document.numberOfPages.coerceAtMost(MAX_PDF_PAGES)
                return stripper.getText(document) ?: ""
            }
        } catch (e: Throwable) {
            return fallbackStreamSearch(file)
        }
    }

    /**
     * استخراج نصوص DOCX مباشرة من word/document.xml عبر ZipInputStream + XmlPullParser
     */
    private fun extractDocxText(file: File): String {
        val stringBuilder = StringBuilder()
        FileInputStream(file).use { fis ->
            ZipInputStream(fis).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name == "word/document.xml") {
                        val parser = Xml.newPullParser()
                        parser.setInput(zis, "UTF-8")
                        var eventType = parser.eventType
                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            if (eventType == XmlPullParser.START_TAG && parser.name == "t") {
                                val text = parser.nextText()
                                if (!text.isNullOrBlank()) {
                                    stringBuilder.append(text).append(" ")
                                }
                            }
                            eventType = parser.next()
                        }
                        break
                    }
                    entry = zis.nextEntry
                }
            }
        }
        return stringBuilder.toString()
    }

    /**
     * استخراج نصوص XLSX من xl/sharedStrings.xml
     */
    private fun extractXlsxText(file: File): String {
        val stringBuilder = StringBuilder()
        FileInputStream(file).use { fis ->
            ZipInputStream(fis).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name == "xl/sharedStrings.xml") {
                        val parser = Xml.newPullParser()
                        parser.setInput(zis, "UTF-8")
                        var eventType = parser.eventType
                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            if (eventType == XmlPullParser.START_TAG && parser.name == "t") {
                                val text = parser.nextText()
                                if (!text.isNullOrBlank()) {
                                    stringBuilder.append(text).append(" ")
                                }
                            }
                            eventType = parser.next()
                        }
                        break
                    }
                    entry = zis.nextEntry
                }
            }
        }
        return stringBuilder.toString()
    }

    /**
     * استخراج احتياطي للنصوص القابلة للقراءة في حال فشل المكتبات
     */
    private fun fallbackStreamSearch(file: File): String {
        return try {
            val bytes = ByteArray(64 * 1024)
            FileInputStream(file).use { it.read(bytes) }
            String(bytes, StandardCharsets.UTF_8).filter { it.isLetterOrDigit() || it.isWhitespace() }
        } catch (e: Exception) {
            ""
        }
    }
}
