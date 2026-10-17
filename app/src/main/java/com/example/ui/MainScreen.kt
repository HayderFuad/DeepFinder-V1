package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContactSupport
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ContactDialog
import com.example.ui.components.FileResultCard
import com.example.ui.components.FilterChipsRow
import com.example.ui.components.IndexingBanner
import com.example.ui.components.PermissionBanner
import com.example.ui.components.SettingsDialog
import com.example.ui.components.VoiceSearchDialog
import com.example.util.BiometricHelper
import com.example.util.FileHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val indexingState by viewModel.indexingState.collectAsStateWithLifecycle()
    val totalIndexed by viewModel.totalIndexedCount.collectAsStateWithLifecycle()
    val hasStoragePermission by viewModel.hasStoragePermission.collectAsStateWithLifecycle()
    val isLocked by viewModel.isLockedByBiometrics.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()

    var showContactDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }

    // مشغل نشاط التعرف الصوتي للنظام (Fallback)
    val speechIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = matches?.firstOrNull()?.trim().orEmpty()
            if (spokenText.isNotEmpty()) {
                viewModel.onQueryChange(spokenText)
            }
        }
    }

    // مشغل طلب صلاحية الميكروفون (RECORD_AUDIO)
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showVoiceDialog = true
        } else {
            Toast.makeText(
                context,
                "يلزم منح صلاحية الميكروفون لاستخدام البحث الصوتي",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val onVoiceSearchClick = {
        val hasAudioPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasAudioPermission) {
            showVoiceDialog = true
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val quickSearchSuggestions = remember {
        listOf(
            "ابراهيم احمد",
            "حيدر فؤاد",
            "كتاب البرمجة",
            "ميزانية",
            "تقرير المشروع"
        )
    }

    // دعم كامل للاتجاه من اليمين لليسار (RTL) للغة العربية
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        if (isLocked) {
            BiometricLockScreen(
                onUnlockClicked = {
                    if (activity != null) {
                        BiometricHelper.authenticate(
                            activity = activity,
                            onSuccess = { viewModel.onBiometricUnlocked() },
                            onError = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                        viewModel.onBiometricUnlocked()
                    }
                }
            )
        } else {
            Scaffold(
                modifier = modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "DeepFinder",
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "v1.0",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "محرك البحث الذكي الشامل للهاتف",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        actions = {
                            // زر تواصل معنا البارز في الشريط العلوي
                            FilledTonalButton(
                                onClick = { showContactDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("contact_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContactSupport,
                                    contentDescription = "تواصل معنا",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "تواصل معنا",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            // زر إعادة الفهرسة
                            IconButton(
                                onClick = { viewModel.startIndexing(clearExisting = false) },
                                modifier = Modifier.testTag("reindex_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "تحديث الفهرس",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            // زر الإعدادات
                            IconButton(
                                onClick = { showSettingsDialog = true },
                                modifier = Modifier.testTag("settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "الإعدادات",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // قسم رأس البحث الثابت (Persistent Sticky Search Header)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // شريط البحث الدائم بالأعلى مع أيقونة الميكروفون للبحث الصوتي
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                OutlinedTextField(
                                    value = query,
                                    onValueChange = { viewModel.onQueryChange(it) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("search_text_input"),
                                    placeholder = {
                                        Text(
                                            text = "ابحث نصياً أو صوتياً في أسماء ومحتوى الملفات...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    },
                                    leadingIcon = {
                                        if (isSearching) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = "بحث",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    trailingIcon = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(end = 4.dp)
                                        ) {
                                            if (query.isNotEmpty()) {
                                                IconButton(
                                                    onClick = { viewModel.clearQuery() },
                                                    modifier = Modifier.testTag("clear_search_button")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Clear,
                                                        contentDescription = "مسح النص"
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = onVoiceSearchClick,
                                                modifier = Modifier.testTag("voice_search_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Mic,
                                                    contentDescription = "بحث صوتي (Speech-to-Text)",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(24.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                    )
                                )
                            }

                            // اقتراحات بحث سريعة عند خلو حقل البحث
                            if (query.isBlank()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "بحث سريع:",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    quickSearchSuggestions.forEach { suggestion ->
                                        AssistChip(
                                            onClick = { viewModel.onQueryChange(suggestion) },
                                            label = {
                                                Text(
                                                    text = suggestion,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                            )
                                        )
                                    }
                                }
                            }

                            // شريط الفلاتر السريعة
                            FilterChipsRow(
                                selectedFilter = filter,
                                onFilterSelected = { viewModel.onFilterChange(it) }
                            )

                            // شريط الإحصائيات اللحظية الثابت
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (query.isNotBlank())
                                        "النتائج المطابقة لـ \"$query\": ${searchResults.size} نتيجة"
                                    else
                                        "أحدث الملفات المفهرسة (${searchResults.size})",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "إجمالي الفهرس: $totalIndexed عنصر",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }

                    // بطاقة حالة الفهرسة الذكية
                    IndexingBanner(state = indexingState)

                    // تنبيه الصلاحية في حال عدم التفعيل
                    if (!hasStoragePermission) {
                        PermissionBanner(
                            onRequestPermission = {
                                val intent = FileHelper.requestStoragePermissionIntent(context)
                                context.startActivity(intent)
                            }
                        )
                    }

                    // القائمة الكسولة (LazyColumn) لعرض نتائج البحث اللحظية
                    if (searchResults.isEmpty()) {
                        EmptyResultsView(
                            query = query,
                            onStartIndexing = { viewModel.startIndexing(clearExisting = true) }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("search_results_list"),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                items = searchResults,
                                key = { it.file.id }
                            ) { item ->
                                FileResultCard(
                                    result = item,
                                    onOpen = { file -> FileHelper.openFile(context, file) },
                                    onShare = { file -> FileHelper.shareFile(context, file) },
                                    onCopyText = { text, label ->
                                        FileHelper.copyToClipboard(context, text, label)
                                    }
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(30.dp))
                            }
                        }
                    }
                }
            }

            // نافذة البحث الصوتي (Speech-to-Text)
            if (showVoiceDialog) {
                VoiceSearchDialog(
                    onResult = { recognizedText ->
                        showVoiceDialog = false
                        viewModel.onQueryChange(recognizedText)
                    },
                    onLaunchSystemRecognizer = { lang ->
                        showVoiceDialog = false
                        try {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                )
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث الآن للبحث في ملفاتك...")
                            }
                            speechIntentLauncher.launch(intent)
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "خدمة التعرف الصوتي غير مدعومة على هذا الجهاز",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onDismiss = { showVoiceDialog = false }
                )
            }

            // نافذة تواصل معنا المنبثقة
            if (showContactDialog) {
                ContactDialog(onDismiss = { showContactDialog = false })
            }

            // نافذة الإعدادات
            if (showSettingsDialog) {
                SettingsDialog(
                    themeMode = themeMode,
                    onThemeModeChanged = { viewModel.setThemeMode(it) },
                    isBiometricEnabled = isBiometricEnabled,
                    onBiometricToggled = { enabled ->
                        if (enabled && activity != null) {
                            BiometricHelper.authenticate(
                                activity = activity,
                                onSuccess = { viewModel.setBiometricEnabled(true) },
                                onError = {
                                    Toast.makeText(context, "فشل تفعيل البصمة: $it", Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            viewModel.setBiometricEnabled(enabled)
                        }
                    },
                    onReindex = { viewModel.startIndexing(clearExisting = true) },
                    onClearIndex = { viewModel.clearIndex() },
                    onDismiss = { showSettingsDialog = false }
                )
            }
        }
    }
}

@Composable
fun EmptyResultsView(
    query: String,
    onStartIndexing: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (query.isNotBlank()) Icons.Default.SearchOff else Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (query.isNotBlank()) "لم يتم العثور على نتائج" else "ابدأ البحث الفوري الآن",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (query.isNotBlank())
                    "لا يوجد ملف يحتوي على \"$query\" في اسمه أو نصه الداخلي. تأكد من تفعيل الصلاحية وفهرسة الملفات."
                else
                    "اكتب أي كلمة أو اضغط على أيقونة الميكروفون للبحث الصوتي الفوري في مستندات PDF و Word و Excel والنصوص في هاتفك.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onStartIndexing,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("إعادة مسح وفهرسة الملفات")
            }
        }
    }
}

@Composable
fun BiometricLockScreen(
    onUnlockClicked: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "قفل",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "DeepFinder مقفل",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "تم تفعيل حماية البصمة لضمان خصوصية نتائج البحث وملفاتك الحساسة.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onUnlockClicked,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "فتح القفل بالبصمة أو الرمز",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
