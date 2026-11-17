package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.AppPreferencesState
import com.example.data.repository.PastelPawsRepository
import com.example.ui.theme.ThemeMode
import com.example.util.DateHelpers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class BottomTab(val route: String, val label: String) {
    HOME("home", "Home"),
    MEMES("memes", "Memes"),
    GAMES("games", "Games"),
    GOALS("goals", "Goals"),
    GIFTS("gifts", "Gifts"),
}

class MainViewModel(
    private val repository: PastelPawsRepository,
) : ViewModel() {

    val prefsState: StateFlow<AppPreferencesState> = repository.prefsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppPreferencesState(),
        )

    private val _currentTab = MutableStateFlow(BottomTab.HOME)
    val currentTab: StateFlow<BottomTab> = _currentTab.asStateFlow()

    private val _showBirthdayOverlay = MutableStateFlow(false)
    val showBirthdayOverlay: StateFlow<Boolean> = _showBirthdayOverlay.asStateFlow()

    private val _isBirthdayPreview = MutableStateFlow(false)
    val isBirthdayPreview: StateFlow<Boolean> = _isBirthdayPreview.asStateFlow()

    private val _birthdayJustGrantedBox = MutableStateFlow(false)
    val birthdayJustGrantedBox: StateFlow<Boolean> = _birthdayJustGrantedBox.asStateFlow()

    init {
        checkTodayBirthday()
    }

    fun selectTab(tab: BottomTab) {
        _currentTab.value = tab
    }

    fun cycleThemeMode() {
        val current = prefsState.value.themeMode
        val next = when (current) {
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
        }
        viewModelScope.launch {
            repository.setThemeMode(next)
        }
    }

    fun checkTodayBirthday(forceOpen: Boolean = false) {
        if (DateHelpers.isMarch6Today() || forceOpen) {
            viewModelScope.launch {
                val granted = if (DateHelpers.isMarch6Today()) {
                    repository.grantBirthdayBoxIfNeeded(DateHelpers.currentYear())
                } else {
                    false
                }
                _birthdayJustGrantedBox.value = granted
                if (granted || forceOpen) {
                    _isBirthdayPreview.value = false
                    _showBirthdayOverlay.value = true
                }
            }
        }
    }

    fun previewBirthdaySurprise() {
        _isBirthdayPreview.value = true
        _birthdayJustGrantedBox.value = false
        _showBirthdayOverlay.value = true
    }

    fun dismissBirthdayOverlay() {
        _showBirthdayOverlay.value = false
    }

    class Factory(private val repository: PastelPawsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
    }
}
