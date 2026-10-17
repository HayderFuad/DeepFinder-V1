package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.speech.RecognizerIntent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

/**
 * نافذة البحث الصوتي الآمنة تماماً (Crash-Proof Voice Search Dialog)
 * تعمل عبر خيط خلفي معزول (Dispatchers.IO) لقراءة مستوى الصوت من الميكروفون
 * دون التسبب في أي انهيار (Force Close) على الأجهزة أو المحاكيات التي تفتقر لخدمات Google الصوتية.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceSearchDialog(
    indexedFileNames: List<String> = emptyList(),
    onResult: (String) -> Unit,
    onLaunchSystemRecognizer: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedLang by remember { mutableStateOf("ar-SA") }
    var isRecording by remember { mutableStateOf(true) }
    var audioLevel by remember { mutableFloatStateOf(0f) }
    var detectedSpeech by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("تحدث الآن... جاري التقاط الصوت من الميكروفون") }
    var recordingSession by remember { mutableStateOf(0) }

    val hasSystemSpeechActivity = remember(context) {
        canResolveSpeechIntent(context)
    }

    // قراءة مستوى الصوت الحقيقي من الميكروفون في خيط خلفي معزول تماماً لمنع أي خروج مفاجئ
    LaunchedEffect(isRecording, recordingSession) {
        if (!isRecording) {
            audioLevel = 0f
            return@LaunchedEffect
        }

        statusMessage = "تحدث الآن... جاري الاستماع عبر الميكروفون"
        detectedSpeech = false

        withContext(Dispatchers.IO) {
            var recorder: AudioRecord? = null
            try {
                val sampleRate = 16000
                val channelConfig = AudioFormat.CHANNEL_IN_MONO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

                if (minBufSize <= 0) {
                    withContext(Dispatchers.Main) {
                        isRecording = false
                        statusMessage = "الميكروفون قيد الاستخدام أو غير متاح حالياً"
                    }
                    return@withContext
                }

                @SuppressLint("MissingPermission")
                val audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    minBufSize * 2
                )
                recorder = audioRecord

                if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                    withContext(Dispatchers.Main) {
                        isRecording = false
                        statusMessage = "تعذر تهيئة الميكروفون على هذا الجهاز"
                    }
                    return@withContext
                }

                audioRecord.startRecording()
                val buffer = ShortArray(minBufSize)
                var speechTicks = 0
                var silenceAfterSpeechTicks = 0

                while (isActive && isRecording) {
                    val readCount = audioRecord.read(buffer, 0, buffer.size)
                    if (readCount > 0) {
                        var sum = 0.0
                        for (i in 0 until readCount) {
                            val sample = buffer[i].toDouble()
                            sum += sample * sample
                        }
                        val rms = sqrt(sum / readCount)
                        val normalized = (rms / 4000.0).toFloat().coerceIn(0f, 1f)

                        withContext(Dispatchers.Main) {
                            audioLevel = normalized
                            if (normalized > 0.08f) {
                                speechTicks++
                                silenceAfterSpeechTicks = 0
                                if (speechTicks > 2) {
                                    detectedSpeech = true
                                    statusMessage = "جاري التقاط صوتك بوضوح..."
                                }
                            } else if (detectedSpeech) {
                                silenceAfterSpeechTicks++
                                if (silenceAfterSpeechTicks > 18) {
                                    // توقف المستخدم عن الكلام بعد التحدث
                                    isRecording = false
                                    if (hasSystemSpeechActivity) {
                                        statusMessage = "تم التقاط الصوت، جاري فتح محول الكلام إلى نص..."
                                    } else {
                                        statusMessage = "تم التقاط الصوت بنجاح! اختر تطابقاً أو اضغط تحويل النص"
                                    }
                                }
                            }
                        }
                    }
                    delay(60)
                }
            } catch (_: Throwable) {
                withContext(Dispatchers.Main) {
                    isRecording = false
                    audioLevel = 0f
                    statusMessage = "تم إيقاف الميكروفون بأمان"
                }
            } finally {
                try {
                    recorder?.stop()
                } catch (_: Throwable) {
                }
                try {
                    recorder?.release()
                } catch (_: Throwable) {
                }
            }
        }
    }

    val animatedScale by animateFloatAsState(
        targetValue = if (isRecording) (1f + audioLevel * 0.35f) else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "mic_scale"
    )

    AlertDialog(
        onDismissRequest = {
            isRecording = false
            onDismiss()
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("voice_search_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "البحث الصوتي الذكي",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = {
                        isRecording = false
                        onDismiss()
                    }
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // اختيار لغة التحدث (العربية أو الإنجليزية)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedLang == "ar-SA",
                        onClick = { selectedLang = "ar-SA" },
                        label = { Text("العربية (AR)") }
                    )
                    FilterChip(
                        selected = selectedLang == "en-US",
                        onClick = { selectedLang = "en-US" },
                        label = { Text("English (EN)") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // أيقونة الميكروفون المتفاعلة مع مستوى الصوت الحقيقي
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(92.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .scale(animatedScale)
                            .clip(CircleShape)
                            .background(
                                if (isRecording)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                    Surface(
                        shape = CircleShape,
                        color = if (isRecording)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(62.dp),
                        onClick = {
                            if (isRecording) {
                                isRecording = false
                                statusMessage = "تم إيقاف الاستماع مؤقتاً"
                            } else {
                                isRecording = true
                                recordingSession++
                            }
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = "ميكروفون",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // مؤشر مستوى الصوت المباشر
                if (isRecording) {
                    LinearProgressIndicator(
                        progress = { audioLevel.coerceIn(0.05f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // زر الإملاء الصوتي عبر النظام (في حال توفره على الهاتف)
                if (hasSystemSpeechActivity) {
                    Button(
                        onClick = {
                            isRecording = false
                            onLaunchSystemRecognizer(selectedLang)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تحويل الكلام إلى نص عبر النظام")
                    }
                }

                // عرض أسماء ملفات حقيقية من الفهرس للاختيار الصوتي السريع عند التحدث
                if (indexedFileNames.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "أو اختر مباشرة من ملفاتك المفهرسة:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        indexedFileNames.take(6).forEach { fileName ->
                            val cleanName = fileName.substringBeforeLast(".")
                            if (cleanName.isNotBlank()) {
                                AssistChip(
                                    onClick = {
                                        isRecording = false
                                        onResult(cleanName)
                                    },
                                    label = {
                                        Text(
                                            text = cleanName,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = {
                    isRecording = true
                    recordingSession++
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("إعادة الاستماع")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    isRecording = false
                    onDismiss()
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("إغلاق")
            }
        }
    )
}

/**
 * فحص آمن لتوفر خدمة تحويل الكلام إلى نص في النظام قبل استدعائها لمنع أي انهيار
 */
fun canResolveSpeechIntent(context: Context): Boolean {
    return try {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        val activities = context.packageManager.queryIntentActivities(intent, 0)
        !activities.isNullOrEmpty()
    } catch (_: Throwable) {
        false
    }
}
