package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FileType {
    FOLDER,
    PDF,
    WORD,
    EXCEL,
    TEXT,
    CODE,
    ARCHIVE,
    IMAGE,
    OTHER
}

object FileHelper {

    fun determineFileType(file: File): FileType {
        if (file.isDirectory) return FileType.FOLDER

        return when (file.extension.lowercase(Locale.ROOT)) {
            "pdf" -> FileType.PDF
            "docx", "doc" -> FileType.WORD
            "xlsx", "xls", "csv" -> FileType.EXCEL
            "txt", "md", "log", "json", "xml", "ini", "conf" -> FileType.TEXT
            "kt", "java", "py", "js", "ts", "html", "css", "c", "cpp", "h", "sh", "sql" -> FileType.CODE
            "zip", "rar", "7z", "tar", "gz" -> FileType.ARCHIVE
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "svg" -> FileType.IMAGE
            else -> FileType.OTHER
        }
    }

    fun isIndexableContent(file: File): Boolean {
        if (file.isDirectory) return false
        val ext = file.extension.lowercase(Locale.ROOT)
        return when (ext) {
            "txt", "md", "json", "csv", "xml", "log", "pdf", "docx", "xlsx",
            "html", "kt", "java", "py", "js", "ts", "sh", "sql", "conf", "ini" -> true
            else -> false
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 بايت"
        val units = arrayOf("بايت", "ك.ب", "م.ب", "ج.ب", "ت.ب")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
        return String.format(Locale.US, "%.1f %s", value, units[digitGroups])
    }

    fun formatLastModified(timestamp: Long): String {
        if (timestamp <= 0) return "-"
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun openFile(context: Context, file: File) {
        try {
            if (!file.exists()) {
                Toast.makeText(context, "الملف غير موجود!", Toast.LENGTH_SHORT).show()
                return
            }

            if (file.isDirectory) {
                // فتح مجلد إن أمكن
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.fromFile(file), "resource/folder")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(Intent.createChooser(intent, "فتح المجلد عبر"))
                return
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val mimeType = context.contentResolver.getType(uri) ?: getMimeType(file)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(Intent.createChooser(intent, "فتح الملف باستخدام"))
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح الملف: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareFile(context: Context, file: File) {
        try {
            if (!file.exists()) {
                Toast.makeText(context, "الملف غير موجود!", Toast.LENGTH_SHORT).show()
                return
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val mimeType = getMimeType(file)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(Intent.createChooser(intent, "مشاركة الملف عبر"))
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر مشاركة الملف: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyToClipboard(context: Context, text: String, label: String = "DeepFinder") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "تم النسخ إلى الحافظة بنجاح", Toast.LENGTH_SHORT).show()
    }

    private fun getMimeType(file: File): String {
        return when (file.extension.lowercase(Locale.ROOT)) {
            "pdf" -> "application/pdf"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "doc" -> "application/msword"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "xls" -> "application/vnd.ms-excel"
            "txt", "log", "ini", "conf" -> "text/plain"
            "json" -> "application/json"
            "xml" -> "text/xml"
            "csv" -> "text/csv"
            "zip" -> "application/zip"
            "apk" -> "application/vnd.android.package-archive"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            else -> "*/*"
        }
    }

    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true // تم منحها في المانيفست أو عند التثبيت
        }
    }

    fun requestStoragePermissionIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
    }
}
