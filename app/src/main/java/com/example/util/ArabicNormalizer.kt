package com.example.util

/**
 * محرك تطبيع ومعالجة اللغة العربية (Arabic Normalization Engine)
 * يوحد الهمزات، التاء المربوطة، الألف المقصورة، ويزيل التشكيل والمد (Tatweel)
 * لضمان دقة البحث بنسبة 100% سواء في أسماء الملفات أو محتواها الداخلي.
 */
object ArabicNormalizer {

    private val TASHKEEL_REGEX = Regex("[\u064B-\u0652\u0653-\u0655\u0670\u06D6-\u06ED]")
    private val TATWEEL_CHAR = '\u0640'

    /**
     * تطبيع شامل للنص العربي:
     * 1. إزالة التشكيل (الحركات) والمد (ـ)
     * 2. توحيد الهمزات (أ، إ، آ، ٱ -> ا)
     * 3. توحيد التاء المربوطة (ة -> ه)
     * 4. توحيد الألف المقصورة (ى -> ي)
     * 5. تحويل الحروف الإنجليزية إلى أحرف صغيرة (lowercase)
     */
    fun normalize(text: String?): String {
        if (text.isNullOrEmpty()) return ""

        var cleaned = text.replace(TATWEEL_CHAR.toString(), "")
        cleaned = TASHKEEL_REGEX.replace(cleaned, "")

        val builder = StringBuilder(cleaned.length)
        for (ch in cleaned) {
            when (ch) {
                'أ', 'إ', 'آ', 'ٱ' -> builder.append('ا')
                'ة' -> builder.append('ه')
                'ى' -> builder.append('ي')
                'ؤ' -> builder.append('و')
                'ئ' -> builder.append('ي')
                // الأرقام العربية إلى إنجليزية
                '٠' -> builder.append('0')
                '١' -> builder.append('1')
                '٢' -> builder.append('2')
                '٣' -> builder.append('3')
                '٤' -> builder.append('4')
                '٥' -> builder.append('5')
                '٦' -> builder.append('6')
                '٧' -> builder.append('7')
                '٨' -> builder.append('8')
                '٩' -> builder.append('9')
                else -> builder.append(ch.lowercaseChar())
            }
        }

        return builder.toString().trim()
    }

    /**
     * التحقق مما إذا كان النص يحتوي على النص المبحوث عنه بتطبيع عربي كامل
     */
    fun containsNormalized(source: String?, query: String?): Boolean {
        if (source.isNullOrEmpty() || query.isNullOrEmpty()) return false
        val normSource = normalize(source)
        val normQuery = normalize(query)
        return normSource.contains(normQuery)
    }

    /**
     * استخراج مقتطف (Snippet) ذكي يحيط بالكلمة المطابقة داخل النص
     * مع تحديد موقع الكلمة لعرضها مضللة
     */
    fun extractSnippet(content: String?, query: String?, windowSize: Int = 120): SnippetResult? {
        if (content.isNullOrEmpty() || query.isNullOrEmpty()) return null

        val normContent = normalize(content)
        val normQuery = normalize(query)

        val matchIndex = normContent.indexOf(normQuery)
        if (matchIndex == -1) return null

        // حساب نافذة المقتطف التقريبية
        val start = (matchIndex - windowSize / 2).coerceAtLeast(0)
        val end = (matchIndex + normQuery.length + windowSize / 2).coerceAtMost(content.length)

        val prefix = if (start > 0) "..." else ""
        val suffix = if (end < content.length) "..." else ""

        val rawSnippet = content.substring(start, end).replace("\n", " ").replace("\r", " ")
        val snippetText = "$prefix$rawSnippet$suffix"

        return SnippetResult(
            snippetText = snippetText,
            matchTerm = query
        )
    }
}

data class SnippetResult(
    val snippetText: String,
    val matchTerm: String
)
