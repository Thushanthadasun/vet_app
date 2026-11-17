package com.example.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.GoalEntity
import com.example.data.repository.PastelPawsRepository
import com.example.notifications.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GoalsViewModel(
    private val appContext: Context,
    private val repository: PastelPawsRepository,
) : ViewModel() {

    val goals: StateFlow<List<GoalEntity>> = repository.goalsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedEpochDayFilter = MutableStateFlow<Long?>(null)
    val selectedEpochDayFilter: StateFlow<Long?> = _selectedEpochDayFilter.asStateFlow()

    private val _starToastMessage = MutableStateFlow<String?>(null)
    val starToastMessage: StateFlow<String?> = _starToastMessage.asStateFlow()

    fun toggleDateFilter(epochDay: Long) {
        _selectedEpochDayFilter.value = if (_selectedEpochDayFilter.value == epochDay) null else epochDay
    }

    fun clearDateFilter() {
        _selectedEpochDayFilter.value = null
    }

    fun addGoal(title: String, dueDateEpochDay: Long?, reminderEpochMillis: Long?) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val saved = repository.addGoal(
                title = title,
                dueDateEpochDay = dueDateEpochDay,
                reminderEpochMillis = reminderEpochMillis,
            )
            if (reminderEpochMillis != null && reminderEpochMillis > System.currentTimeMillis()) {
                NotificationHelper.scheduleGoalReminder(
                    context = appContext,
                    goalId = saved.id,
                    title = saved.title,
                    triggerAtMillis = reminderEpochMillis,
                )
            }
        }
    }

    fun toggleGoal(goal: GoalEntity) {
        viewModelScope.launch {
            val starsEarned = repository.toggleGoalCompleted(goal)
            if (!goal.isCompleted) {
                NotificationHelper.cancelGoalReminder(appContext, goal.id)
                if (starsEarned > 0) {
                    _starToastMessage.value = "+$starsEarned ★ earned for completing \"${goal.title}\"!"
                }
            } else if (goal.reminderEpochMillis != null &&
                goal.reminderEpochMillis > System.currentTimeMillis()
            ) {
                NotificationHelper.scheduleGoalReminder(
                    context = appContext,
                    goalId = goal.id,
                    title = goal.title,
                    triggerAtMillis = goal.reminderEpochMillis,
                )
            }
        }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch {
            NotificationHelper.cancelGoalReminder(appContext, goal.id)
            repository.deleteGoal(goal.id)
        }
    }

    fun dismissToast() {
        _starToastMessage.value = null
    }

    class Factory(
        private val appContext: Context,
        private val repository: PastelPawsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GoalsViewModel(appContext, repository) as T
        }
    }
}
