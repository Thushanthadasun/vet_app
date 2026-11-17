package com.example.ui.screens

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.MemeEntity
import com.example.data.repository.PastelPawsRepository
import com.example.data.repository.SyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MemeFilter { ALL, TRENDING, FAVORITES }

class MemesViewModel(
    private val repository: PastelPawsRepository,
) : ViewModel() {

    private val _filter = MutableStateFlow(MemeFilter.ALL)
    val filter: StateFlow<MemeFilter> = _filter.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val memes: StateFlow<List<MemeEntity>> = combine(
        repository.memesFlow,
        _filter,
    ) { all, filter ->
        when (filter) {
            MemeFilter.ALL -> all
            MemeFilter.TRENDING -> all.filter { it.imageUrlOrPath.startsWith("http") }
            MemeFilter.FAVORITES -> all.filter { it.isFavorite }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    fun setFilter(filter: MemeFilter) {
        _filter.value = filter
    }

    fun autoSyncIfNeeded() {
        viewModelScope.launch {
            val result = repository.refreshMemesFromFeed(autoBackground = true)
            if (result is SyncResult.Success && result.count > 0) {
                _statusMessage.value = result.message
            }
        }
    }

    fun refreshFromNetwork() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = repository.refreshMemesFromFeed(autoBackground = false)
            _statusMessage.value = when (result) {
                is SyncResult.Success -> result.message
                is SyncResult.Error -> result.message
            }
            _isRefreshing.value = false
        }
    }

    fun toggleFavorite(meme: MemeEntity) {
        viewModelScope.launch {
            repository.toggleMemeFavorite(meme.id, !meme.isFavorite)
        }
    }

    fun addMemeFromGallery(uri: Uri, sinhalaCaption: String) {
        viewModelScope.launch {
            val saved = repository.addMemeFromGallery(uri, sinhalaCaption)
            _statusMessage.value = if (saved) {
                "Added meme to your offline collection."
            } else {
                "Could not read selected photo."
            }
        }
    }

    fun deleteMeme(meme: MemeEntity) {
        viewModelScope.launch {
            repository.deleteUserMeme(meme)
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    class Factory(private val repository: PastelPawsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MemesViewModel(repository) as T
        }
    }
}
