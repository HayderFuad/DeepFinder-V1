package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MatchType
import com.example.data.SearchResult
import com.example.ui.theme.ColorArchive
import com.example.ui.theme.ColorCode
import com.example.ui.theme.ColorExcel
import com.example.ui.theme.ColorFolder
import com.example.ui.theme.ColorOther
import com.example.ui.theme.ColorPdf
import com.example.ui.theme.ColorText
import com.example.ui.theme.ColorWord
import com.example.util.ArabicNormalizer
import com.example.util.FileHelper
import com.example.util.FileType
import java.io.File

@Composable
fun FileResultCard(
    result: SearchResult,
    onOpen: (File) -> Unit,
    onShare: (File) -> Unit,
    onCopyText: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val fileObj = File(result.file.filePath)
    val fileType = FileHelper.determineFileType(fileObj)
    val (typeColor, typeIcon) = getTypeColorAndIcon(fileType)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("file_result_card_${result.file.id}")
            .clickable { onOpen(fileObj) },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // الصف العلوي: الأيقونة، الاسم، وشارة نوع التطابق
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(typeColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = "نوع الملف",
                        tint = typeColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    val highlightedName = highlightText(
                        fullText = result.file.fileName,
                        query = result.matchTerm,
                        highlightColor = MaterialTheme.colorScheme.tertiary,
                        normalColor = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = highlightedName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = result.file.filePath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                MatchBadge(matchType = result.matchType)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // معلومات الحجم والتاريخ والامتداد
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (result.file.extension.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = typeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = result.file.extension.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = typeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                if (!result.file.isDirectory) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = FileHelper.formatFileSize(result.file.fileSizeBytes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = FileHelper.formatLastModified(result.file.lastModified),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            // صندوق المقتطف النصي (Content Snippet Box) مباشرة داخل البطاقة
            if (!result.snippet.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    )
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Article,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "مقتطف المحتوى الداخلي:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Text(
                                text = "نسخ المقتطف",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { onCopyText(result.snippet, "مقتطف الملف") }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))

                        val highlightedSnippet = highlightText(
                            fullText = result.snippet,
                            query = result.matchTerm,
                            highlightColor = MaterialTheme.colorScheme.tertiary,
                            normalColor = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = highlightedSnippet,
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // أزرار العمليات السريعة (فتح، مشاركة، نسخ المسار)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onCopyText(result.file.filePath, "مسار الملف") },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "نسخ المسار",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (!result.file.isDirectory) {
                    IconButton(
                        onClick = { onShare(fileObj) },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "مشاركة الملف",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                FilledTonalButton(
                    onClick = { onOpen(fileObj) },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(
                        horizontal = 14.dp,
                        vertical = 6.dp
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (result.file.isDirectory) "استعراض" else "فتح الملف",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
fun MatchBadge(matchType: MatchType) {
    val (badgeText, badgeBg, badgeTextColor) = when (matchType) {
        MatchType.NAME_MATCH -> Triple(
            "مطابقة الاسم",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        MatchType.CONTENT_MATCH -> Triple(
            "داخل المحتوى",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        MatchType.BOTH -> Triple(
            "اسم ومحتوى",
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.tertiary
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = badgeBg
    ) {
        Text(
            text = badgeText,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = badgeTextColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

fun getTypeColorAndIcon(type: FileType): Pair<Color, ImageVector> {
    return when (type) {
        FileType.PDF -> ColorPdf to Icons.Default.PictureAsPdf
        FileType.WORD -> ColorWord to Icons.Default.Description
        FileType.EXCEL -> ColorExcel to Icons.Default.TableChart
        FileType.TEXT -> ColorText to Icons.AutoMirrored.Filled.Article
        FileType.CODE -> ColorCode to Icons.Default.Code
        FileType.FOLDER -> ColorFolder to Icons.Default.Folder
        FileType.ARCHIVE -> ColorArchive to Icons.Default.Archive
        FileType.IMAGE -> ColorWord to Icons.Default.Description
        FileType.OTHER -> ColorOther to Icons.Default.InsertDriveFile
    }
}

/**
 * تظليل الكلمات المطابقة بذكاء مع دعم التطبيع العربي
 */
fun highlightText(
    fullText: String,
    query: String,
    highlightColor: Color,
    normalColor: Color
): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(fullText)

    val normFull = ArabicNormalizer.normalize(fullText)
    val normQuery = ArabicNormalizer.normalize(query)

    if (!normFull.contains(normQuery)) {
        return AnnotatedString(fullText)
    }

    return buildAnnotatedString {
        var currentIndex = 0
        while (currentIndex < fullText.length) {
            val remainingNorm = ArabicNormalizer.normalize(fullText.substring(currentIndex))
            val matchOffset = remainingNorm.indexOf(normQuery)
            if (matchOffset == -1) {
                append(fullText.substring(currentIndex))
                break
            }

            val matchStartInFull = (currentIndex + matchOffset).coerceAtMost(fullText.length)
            val matchEndInFull = (matchStartInFull + query.length).coerceAtMost(fullText.length)

            if (matchStartInFull > currentIndex) {
                append(fullText.substring(currentIndex, matchStartInFull))
            }

            pushStyle(
                SpanStyle(
                    color = highlightColor,
                    fontWeight = FontWeight.ExtraBold,
                    background = highlightColor.copy(alpha = 0.22f)
                )
            )
            append(fullText.substring(matchStartInFull, matchEndInFull))
            pop()

            currentIndex = matchEndInFull.coerceAtLeast(currentIndex + 1)
        }
    }
}
