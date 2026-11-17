package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.GameBestScoreEntity
import com.example.data.local.QuizQuestionEntity
import com.example.data.local.ScrambleWordEntity
import com.example.data.repository.PastelPawsRepository
import com.example.data.repository.SyncResult
import com.example.ui.components.SoundEffectPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class GameType(
    val id: String,
    val title: String,
    val subtitle: String,
) {
    VET_QUIZ("quiz", "Vet Quiz", "Adaptive & expanding vet trivia"),
    MEMORY_MATCH("memory", "Memory Match", "4×4, 4×5, or 6×4 calm pairs"),
    REACTION_TEST("reaction", "Reaction Speed", "5-round reflex timer"),
    WORD_SCRAMBLE("scramble", "Word Scramble", "Tiered veterinary terminology"),
}

class GamesViewModel(
    private val repository: PastelPawsRepository,
    val soundPlayer: SoundEffectPlayer,
) : ViewModel() {

    private val _activeGame = MutableStateFlow<GameType?>(null)
    val activeGame: StateFlow<GameType?> = _activeGame.asStateFlow()

    private val _quizQuestions = MutableStateFlow<List<QuizQuestionEntity>>(emptyList())
    val quizQuestions: StateFlow<List<QuizQuestionEntity>> = _quizQuestions.asStateFlow()

    private val _scrambleWords = MutableStateFlow<List<ScrambleWordEntity>>(emptyList())
    val scrambleWords: StateFlow<List<ScrambleWordEntity>> = _scrambleWords.asStateFlow()

    private val _isSyncingQuiz = MutableStateFlow(false)
    val isSyncingQuiz: StateFlow<Boolean> = _isSyncingQuiz.asStateFlow()

    private val _syncNotice = MutableStateFlow<String?>(null)
    val syncNotice: StateFlow<String?> = _syncNotice.asStateFlow()

    val quizBankCount: StateFlow<Int> = repository.quizCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 24)

    val isMuted: StateFlow<Boolean> = repository.prefsFlow
        .map { it.isMuted }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val bestScoresMap: StateFlow<Map<String, GameBestScoreEntity>> = repository.bestScoresFlow
        .map { list -> list.associateBy { it.gameId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun openGame(game: GameType) {
        _activeGame.value = game
        when (game) {
            GameType.VET_QUIZ -> loadQuizSession()
            GameType.WORD_SCRAMBLE -> loadScrambleSession()
            else -> Unit
        }
    }

    fun exitGame() {
        _activeGame.value = null
    }

    fun toggleMute() {
        viewModelScope.launch {
            repository.setMuted(!isMuted.value)
        }
    }

    fun autoSyncQuizIfNeeded() {
        viewModelScope.launch {
            val result = repository.refreshQuizQuestions(autoBackground = true)
            if (result is SyncResult.Success && result.count > 0) {
                _syncNotice.value = result.message
            }
        }
    }

    fun loadQuizSession() {
        viewModelScope.launch {
            _quizQuestions.value = repository.getQuizSessionQuestions(limit = 8)
        }
    }

    fun loadScrambleSession() {
        viewModelScope.launch {
            _scrambleWords.value = repository.getAdaptiveScrambleWords(count = 6)
        }
    }

    fun recordQuizAnswer(questionId: Long, wasCorrect: Boolean) {
        viewModelScope.launch {
            repository.recordQuizAnswer(questionId, wasCorrect)
        }
    }

    fun markScrambleWordSolved(word: String) {
        viewModelScope.launch {
            repository.markScrambleWordSolved(word)
        }
    }

    fun syncNewQuizQuestions() {
        if (_isSyncingQuiz.value) return
        viewModelScope.launch {
            _isSyncingQuiz.value = true
            val result = repository.refreshQuizQuestions(autoBackground = false)
            _syncNotice.value = when (result) {
                is SyncResult.Success -> result.message
                is SyncResult.Error -> result.message
            }
            _quizQuestions.value = repository.getQuizSessionQuestions(limit = 8)
            _scrambleWords.value = repository.getAdaptiveScrambleWords(count = 6)
            _isSyncingQuiz.value = false
        }
    }

    fun clearSyncNotice() {
        _syncNotice.value = null
    }

    fun recordResult(
        game: GameType,
        score: Int,
        detail: String,
        starsEarned: Int,
        higherIsBetter: Boolean = true,
    ) {
        viewModelScope.launch {
            repository.recordGameCompletion(
                gameId = game.id,
                score = score,
                detail = detail,
                starsEarned = starsEarned,
                higherIsBetter = higherIsBetter,
            )
        }
    }

    class Factory(
        private val repository: PastelPawsRepository,
        private val soundPlayer: SoundEffectPlayer,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GamesViewModel(repository, soundPlayer) as T
        }
    }
}
