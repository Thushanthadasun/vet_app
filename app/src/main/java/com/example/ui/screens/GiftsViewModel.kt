package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.GiftItemEntity
import com.example.data.local.GiftTier
import com.example.data.local.OpenedGiftCardEntity
import com.example.data.preferences.AppPreferencesState
import com.example.data.repository.PastelPawsRepository
import com.example.ui.components.SoundEffectPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GiftsViewModel(
    private val repository: PastelPawsRepository,
    private val soundPlayer: SoundEffectPlayer,
) : ViewModel() {

    val prefs: StateFlow<AppPreferencesState> = repository.prefsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPreferencesState())

    val giftPool: StateFlow<List<GiftItemEntity>> = repository.giftPoolFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val openedCards: StateFlow<List<OpenedGiftCardEntity>> = repository.openedCardsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isOpeningBox = MutableStateFlow(false)
    val isOpeningBox: StateFlow<Boolean> = _isOpeningBox.asStateFlow()

    private val _justRevealedCard = MutableStateFlow<OpenedGiftCardEntity?>(null)
    val justRevealedCard: StateFlow<OpenedGiftCardEntity?> = _justRevealedCard.asStateFlow()

    fun openMysteryBox() {
        if (_isOpeningBox.value || prefs.value.totalUnopenedBoxes <= 0) return
        viewModelScope.launch {
            _isOpeningBox.value = true
            delay(950L)
            val card = repository.openNextMysteryBox()
            _isOpeningBox.value = false
            if (card != null) {
                soundPlayer.playWin(prefs.value.isMuted)
                _justRevealedCard.value = card
            }
        }
    }

    fun dismissRevealedDialog() {
        _justRevealedCard.value = null
    }

    fun setCardDelivered(cardId: Long, delivered: Boolean) {
        viewModelScope.launch {
            repository.setCardDelivered(cardId, delivered)
        }
    }

    fun addGift(title: String, tier: GiftTier) {
        viewModelScope.launch {
            repository.addGiftToPool(title, tier)
        }
    }

    fun deleteGift(giftId: Long) {
        viewModelScope.launch {
            repository.deleteGiftFromPool(giftId)
        }
    }

    fun saveSettings(
        nickname: String,
        pinCode: String,
        starsPerBox: Int,
        birthdayMessage: String,
        memeFeedUrl: String,
        customQuizUrl: String,
    ) {
        viewModelScope.launch {
            repository.updateAdminSettings(
                nickname = nickname,
                pinCode = pinCode,
                starsPerBox = starsPerBox,
                birthdayMessage = birthdayMessage,
                memeFeedUrl = memeFeedUrl,
                customQuizUrl = customQuizUrl,
            )
        }
    }

    class Factory(
        private val repository: PastelPawsRepository,
        private val soundPlayer: SoundEffectPlayer,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GiftsViewModel(repository, soundPlayer) as T
        }
    }
}
