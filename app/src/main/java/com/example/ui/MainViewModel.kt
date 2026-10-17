package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.SearchFilter
import com.example.data.SearchRepository
import com.example.data.SearchResult
import com.example.service.FileIndexer
import com.example.service.IndexingState
import com.example.util.FileHelper
import com.example.util.PreferencesManager
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = SearchRepository(db.fileDao())
    private val prefs = PreferencesManager(application)

    // حالة البحث والمدخلات
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(SearchFilter.ALL)
    val selectedFilter: StateFlow<SearchFilter> = _selectedFilter.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // حالة الفهرسة
    val indexingState: StateFlow<IndexingState> = FileIndexer.indexingState

    // إجمالي الملفات في الفهرس
    val totalIndexedCount: StateFlow<Int> = repository.getTotalCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // حالة الصلاحيات
    private val _hasStoragePermission = MutableStateFlow(FileHelper.hasStoragePermission(application))
    val hasStoragePermission: StateFlow<Boolean> = _hasStoragePermission.asStateFlow()

    // حالة قفل البصمة
    private val _isLockedByBiometrics = MutableStateFlow(prefs.isBiometricEnabled)
    val isLockedByBiometrics: StateFlow<Boolean> = _isLockedByBiometrics.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.themeMode)
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(prefs.isBiometricEnabled)
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    init {
        viewModelScope.launch {
            // إزالة أي ملفات تجريبية افتراضية قديمة فور التشغيل
            FileIndexer.cleanupLegacyDemoFiles(getApplication())

            val count = repository.getTotalCount()
            if (count == 0 && FileHelper.hasStoragePermission(getApplication())) {
                startIndexing()
            } else {
                performSearch(_searchQuery.value, _selectedFilter.value)
            }
        }

        // مراقبة حقل البحث مع Debounce 250ms
        viewModelScope.launch {
            combine(
                _searchQuery.debounce(250).distinctUntilChanged(),
                _selectedFilter
            ) { query, filter ->
                query to filter
            }.collect { (query, filter) ->
                performSearch(query, filter)
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun clearQuery() {
        _searchQuery.value = ""
    }

    fun onFilterChange(filter: SearchFilter) {
        _selectedFilter.value = filter
    }

    private suspend fun performSearch(query: String, filter: SearchFilter) {
        _isSearching.value = true
        try {
            val results = repository.search(query, filter)
            _searchResults.value = results
        } catch (e: Exception) {
            _searchResults.value = emptyList()
        } finally {
            _isSearching.value = false
        }
    }

    fun refreshSearch() {
        viewModelScope.launch {
            performSearch(_searchQuery.value, _selectedFilter.value)
        }
    }

    fun startIndexing(clearExisting: Boolean = false) {
        viewModelScope.launch {
            FileIndexer.startIndexing(getApplication(), clearExisting)
            performSearch(_searchQuery.value, _selectedFilter.value)
        }
    }

    fun clearIndex() {
        viewModelScope.launch {
            repository.clearIndex()
            _searchResults.value = emptyList()
        }
    }

    fun updateStoragePermissionStatus() {
        val hadPermission = _hasStoragePermission.value
        val nowHasPermission = FileHelper.hasStoragePermission(getApplication())
        _hasStoragePermission.value = nowHasPermission
        if (!hadPermission && nowHasPermission) {
            startIndexing(clearExisting = false)
        }
    }

    fun onBiometricUnlocked() {
        _isLockedByBiometrics.value = false
    }

    fun setThemeMode(mode: String) {
        prefs.themeMode = mode
        _themeMode.value = mode
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.isBiometricEnabled = enabled
        _isBiometricEnabled.value = enabled
        if (!enabled) {
            _isLockedByBiometrics.value = false
        }
    }
}
