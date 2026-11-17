package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.GoalEntity
import com.example.notifications.NotificationHelper
import com.example.ui.components.FancyGlassCard
import com.example.ui.components.FancyPillBadge
import com.example.ui.components.fancyIridescentBorder
import com.example.ui.theme.LocalPastelColors
import com.example.util.DateHelpers
import java.util.Calendar
import androidx.compose.ui.graphics.Brush

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    viewModel: GoalsViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val pastel = LocalPastelColors.current
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val selectedDayFilter by viewModel.selectedEpochDayFilter.collectAsStateWithLifecycle()
    val starToast by viewModel.starToastMessage.collectAsStateWithLifecycle()

    val (todayYear, todayMonth, _) = remember { DateHelpers.todayYearMonthDay() }
    var displayYear by remember { mutableIntStateOf(todayYear) }
    var displayMonth by remember { mutableIntStateOf(todayMonth) }

    var showAddDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {},
    )

    val completedCount = goals.count { it.isCompleted }
    val completionPct = if (goals.isEmpty()) 0 else (completedCount * 100) / goals.size
    val dueDaysSet = remember(goals) {
        goals.mapNotNull { it.dueDateEpochDay }.toSet()
    }
    val visibleGoals = remember(goals, selectedDayFilter) {
        if (selectedDayFilter == null) goals else goals.filter { it.dueDateEpochDay == selectedDayFilter }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("goals_screen"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                ) {
                    FancyPillBadge(
                        text = "+15 ★ per completed goal",
                        containerColor = pastel.starGoldSoft,
                        contentColor = pastel.starGold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.goals_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = "Stay on top of lectures, clinics, and personal wins",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                AnimatedVisibility(visible = starToast != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = pastel.starGoldSoft,
                        border = fancyIridescentBorder(1.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = starToast.orEmpty(),
                                style = MaterialTheme.typography.labelLarge,
                                color = pastel.starGold,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = viewModel::dismissToast) { Text("Nice") }
                        }
                    }
                }
            }

            // Completion Percentage Card
            item {
                FancyGlassCard(cornerRadius = 24.dp) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Completed",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "$completionPct% ($completedCount/${goals.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("goals_percentage_text"),
                            )
                        }
                        val pctFraction = if (goals.isEmpty()) 0f else completedCount.toFloat() / goals.size
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(11.dp)
                                .clip(CircleShape)
                                .background(pastel.successContainer.copy(alpha = 0.6f)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(pctFraction.coerceIn(0f, 1f))
                                    .height(11.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                pastel.success,
                                                MaterialTheme.colorScheme.tertiary,
                                            )
                                        )
                                    ),
                            )
                        }
                    }
                }
            }

            // Simple Month Calendar showing due dates
            item {
                SimpleGoalMonthCalendar(
                    year = displayYear,
                    monthZeroBased = displayMonth,
                    dueEpochDays = dueDaysSet,
                    selectedEpochDay = selectedDayFilter,
                    onPrevMonth = {
                        if (displayMonth == 0) {
                            displayMonth = 11
                            displayYear--
                        } else {
                            displayMonth--
                        }
                    },
                    onNextMonth = {
                        if (displayMonth == 11) {
                            displayMonth = 0
                            displayYear++
                        } else {
                            displayMonth++
                        }
                    },
                    onSelectDay = viewModel::toggleDateFilter,
                )
            }

            if (selectedDayFilter != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Showing goals due ${DateHelpers.formatEpochDay(selectedDayFilter!!)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        TextButton(onClick = viewModel::clearDateFilter) {
                            Text("Show All")
                        }
                    }
                }
            }

            if (visibleGoals.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Checklist,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(40.dp),
                            )
                            Text(
                                text = if (selectedDayFilter != null) "No goals due on this day" else "No goals yet",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "Tap + to add a study task, exam prep, or personal goal.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(visibleGoals, key = { it.id }) { goal ->
                    GoalRowCard(
                        goal = goal,
                        onToggle = { viewModel.toggleGoal(goal) },
                        onDelete = { viewModel.deleteGoal(goal) },
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_goal_fab"),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.goals_add))
        }
    }

    if (showAddDialog) {
        AddGoalDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, dueDay, reminderMillis ->
                if (reminderMillis != null &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !NotificationHelper.hasNotificationPermission(context)
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                viewModel.addGoal(title, dueDay, reminderMillis)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun SimpleGoalMonthCalendar(
    year: Int,
    monthZeroBased: Int,
    dueEpochDays: Set<Long>,
    selectedEpochDay: Long?,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDay: (Long) -> Unit,
) {
    val (offset, totalDays) = remember(year, monthZeroBased) {
        DateHelpers.daysInMonth(year, monthZeroBased)
    }
    val todayDay = remember { DateHelpers.todayEpochDay() }
    val weekHeaders = listOf("S", "M", "T", "W", "T", "F", "S")

    FancyGlassCard(cornerRadius = 24.dp) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPrevMonth) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous month")
                }
                Text(
                    text = DateHelpers.monthTitle(year, monthZeroBased),
                    style = MaterialTheme.typography.titleMedium,
                )
                IconButton(onClick = onNextMonth) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next month")
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                weekHeaders.forEach { day ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val totalCells = offset + totalDays
            val rows = (totalCells + 6) / 7
            for (r in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (c in 0 until 7) {
                        val cellIndex = r * 7 + c
                        val dayNum = cellIndex - offset + 1
                        if (dayNum in 1..totalDays) {
                            val epochDay = DateHelpers.toEpochDay(year, monthZeroBased, dayNum)
                            val hasGoal = epochDay in dueEpochDays
                            val isSelected = epochDay == selectedEpochDay
                            val isToday = epochDay == todayDay

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.1f)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                                            else -> androidx.compose.ui.graphics.Color.Transparent
                                        }
                                    )
                                    .clickable { onSelectDay(epochDay) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = dayNum.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                            else -> MaterialTheme.colorScheme.onSurface
                                        },
                                    )
                                    if (hasGoal) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                    else MaterialTheme.colorScheme.primary
                                                )
                                        )
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalRowCard(
    goal: GoalEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val pastel = LocalPastelColors.current
    FancyGlassCard(
        cornerRadius = 22.dp,
        containerColor = if (goal.isCompleted) {
            pastel.glassSurface.copy(alpha = 0.65f)
        } else {
            pastel.glassSurface
        },
        modifier = Modifier.testTag("goal_card_${goal.id}"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = goal.isCompleted,
                onCheckedChange = { onToggle() },
                modifier = Modifier.testTag("goal_checkbox_${goal.id}"),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goal.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (goal.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (goal.isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (goal.dueDateEpochDay != null || goal.reminderEpochMillis != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (goal.dueDateEpochDay != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.secondary,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = DateHelpers.formatEpochDay(goal.dueDateEpochDay),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (goal.reminderEpochMillis != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Alarm,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = DateHelpers.formatMillisDateTime(goal.reminderEpochMillis),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("goal_delete_${goal.id}"),
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete goal",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGoalDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, dueDay: Long?, reminderMillis: Long?) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var useDueDate by remember { mutableStateOf(false) }
    var selectedDueDay by remember { mutableStateOf(DateHelpers.todayEpochDay()) }
    var useReminder by remember { mutableStateOf(false) }
    var reminderHour by remember { mutableIntStateOf(9) }
    var reminderMinute by remember { mutableIntStateOf(0) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Goal title") },
                    placeholder = { Text("e.g., Review cardiology notes") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_goal_title_input"),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Set due date", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = useDueDate,
                        onCheckedChange = {
                            useDueDate = it
                            if (!it) useReminder = false
                        },
                        modifier = Modifier.testTag("add_goal_due_switch"),
                    )
                }

                if (useDueDate) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Due: ${DateHelpers.formatEpochDay(selectedDueDay)}")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Local reminder notification", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = useReminder,
                            onCheckedChange = { useReminder = it },
                            modifier = Modifier.testTag("add_goal_reminder_switch"),
                        )
                    }

                    if (useReminder) {
                        OutlinedButton(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            val formattedTime = String.format("%02d:%02d", reminderHour, reminderMinute)
                            Text("Reminder time: $formattedTime")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val due = if (useDueDate) selectedDueDay else null
                    val rem = if (useDueDate && useReminder) {
                        DateHelpers.combineDateAndTime(selectedDueDay, reminderHour, reminderMinute)
                    } else null
                    onSave(title, due, rem)
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("save_goal_button"),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )

    if (showDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dateState.selectedDateMillis?.let { utcMillis ->
                            val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = utcMillis
                            }
                            selectedDueDay = DateHelpers.toEpochDay(
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH),
                            )
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Reminder Time") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        reminderHour = timeState.hour
                        reminderMinute = timeState.minute
                        showTimePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
        )
    }
}
