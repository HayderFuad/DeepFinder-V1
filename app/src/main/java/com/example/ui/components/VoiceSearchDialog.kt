package com.example.ui.components

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp

@Composable
fun VoiceSearchDialog(
    onResult: (String) -> Unit,
    onLaunchSystemRecognizer: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedLang by remember { mutableStateOf("ar-SA") }
    var isListening by remember { mutableStateOf(false) }
    var partialText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("تحدث الآن للبحث الصوتي...") }
    var hasError by remember { mutableStateOf(false) }

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }

    fun startListening(lang: String) {
        if (speechRecognizer == null) {
            onLaunchSystemRecognizer(lang)
            return
        }
        hasError = false
        partialText = ""
        statusMessage = "جاري الاستماع... تحدث بوضوح"
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        try {
            speechRecognizer.startListening(intent)
            isListening = true
        } catch (e: Exception) {
            isListening = false
            hasError = true
            statusMessage = "تعذر بدء التعرف الصوتي المباشر"
        }
    }

    DisposableEffect(speechRecognizer, selectedLang) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                hasError = false
                statusMessage = "تحدث الآن... جاري التقاط الصوت"
            }

            override fun onBeginningOfSpeech() {
                isListening = true
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
                statusMessage = "جاري معالجة النص المنطوق..."
            }

            override fun onError(error: Int) {
                isListening = false
                hasError = true
                statusMessage = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "لم يتم التعرف على الكلام، حاول مرة أخرى"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "لم يتم سماع أي صوت، حاول مجدداً"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "يرجى منح صلاحية الميكروفون"
                    else -> "تعذر التقاط الصوت على هذا الجهاز، يمكنك المحاولة مجدداً"
                }
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognized = matches?.firstOrNull()?.trim().orEmpty()
                if (recognized.isNotEmpty()) {
                    partialText = recognized
                    onResult(recognized)
                } else if (partialText.isNotBlank()) {
                    onResult(partialText.trim())
                } else {
                    hasError = true
                    statusMessage = "لم يتم التعرف على نص واضح، حاول مجدداً"
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (partial.isNotBlank()) {
                    partialText = partial
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        speechRecognizer?.setRecognitionListener(listener)
        onDispose {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(selectedLang) {
        startListening(selectedLang)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.22f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("voice_search_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "البحث الصوتي الذكي",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
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

                Spacer(modifier = Modifier.height(20.dp))

                // أيقونة الميكروفون النابضة
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(96.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .scale(if (isListening) pulseScale else 1f)
                            .clip(CircleShape)
                            .background(
                                if (isListening)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                    Surface(
                        shape = CircleShape,
                        color = if (isListening)
                            MaterialTheme.colorScheme.primary
                        else if (hasError)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(64.dp),
                        onClick = { startListening(selectedLang) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (hasError) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "ميكروفون",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                if (partialText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "\"$partialText\"",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hasError || !isListening) {
                    OutlinedButton(
                        onClick = { startListening(selectedLang) },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إعادة المحاولة")
                    }
                }
                if (partialText.isNotBlank()) {
                    Button(
                        onClick = { onResult(partialText.trim()) },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("بحث الآن")
                    }
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("إلغاء")
            }
        }
    )
}
