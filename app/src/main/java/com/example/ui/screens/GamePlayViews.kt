package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiNature
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Vaccines
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.GameBestScoreEntity
import com.example.data.local.QuizQuestionEntity
import com.example.data.local.ScrambleWordEntity
import com.example.ui.components.SoundEffectPlayer
import com.example.ui.theme.LocalPastelColors
import kotlinx.coroutines.delay
import kotlin.random.Random

// ---------------------------------------------------------------------------
// 1. VET QUIZ
// ---------------------------------------------------------------------------
@Composable
fun VetQuizPlayView(
    questions: List<QuizQuestionEntity>,
    bestScore: GameBestScoreEntity?,
    isMuted: Boolean,
    soundPlayer: SoundEffectPlayer,
    onReplay: () -> Unit,
    onSyncQuestions: () -> Unit,
    isSyncing: Boolean,
    onRecordAnswer: (questionId: Long, wasCorrect: Boolean) -> Unit,
    onComplete: (score: Int, detail: String, stars: Int) -> Unit,
) {
    val pastel = LocalPastelColors.current
    var currentIndex by remember(questions) { mutableIntStateOf(0) }
    var selectedOption by remember(questions) { mutableStateOf<Int?>(null) }
    var score by remember(questions) { mutableIntStateOf(0) }
    var finished by remember(questions) { mutableStateOf(false) }

    if (questions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading questions...", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    if (finished) {
        val stars = (3 + score / 20).coerceIn(3, 8)
        GameSummaryCard(
            title = "Quiz Complete!",
            scoreText = "$score pts (${score / 10} / ${questions.size} correct)",
            bestText = bestScore?.bestDetail ?: "$score pts",
            starsEarned = stars,
            onReplay = onReplay,
        )
        return
    }

    val current = questions[currentIndex]
    val options = listOf(current.optionA, current.optionB, current.optionC, current.optionD)
    val diffLabel = current.difficulty.replaceFirstChar { it.uppercase() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Question ${currentIndex + 1} / ${questions.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = diffLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = "Score: $score",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = current.question,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Source: ${current.sourceLabel}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        options.forEachIndexed { idx, optionText ->
            val isChosen = selectedOption == idx
            val isCorrect = idx == current.correctIndex
            val bgColor = when {
                selectedOption == null -> MaterialTheme.colorScheme.surface
                isCorrect -> pastel.successContainer
                isChosen -> pastel.wrongContainer
                else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            }
            val borderColor = when {
                selectedOption == null -> MaterialTheme.colorScheme.outline
                isCorrect -> pastel.success
                isChosen -> pastel.wrong
                else -> MaterialTheme.colorScheme.outlineVariant
            }

            Card(
                onClick = {
                    if (selectedOption == null) {
                        selectedOption = idx
                        val wasRight = idx == current.correctIndex
                        onRecordAnswer(current.id, wasRight)
                        if (wasRight) {
                            score += 10
                            soundPlayer.playCorrect(isMuted)
                        } else {
                            soundPlayer.playWrong(isMuted)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quiz_option_$idx"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = bgColor),
                border = BorderStroke(1.5.dp, borderColor),
            ) {
                Text(
                    text = optionText,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        if (selectedOption != null) {
            val wasRight = selectedOption == current.correctIndex
            Text(
                text = if (wasRight) "Correct!" else "Correct answer: ${options[current.correctIndex]}",
                style = MaterialTheme.typography.titleMedium,
                color = if (wasRight) pastel.success else pastel.wrong,
                modifier = Modifier.testTag("quiz_feedback"),
            )
            Button(
                onClick = {
                    if (currentIndex + 1 < questions.size) {
                        currentIndex++
                        selectedOption = null
                    } else {
                        finished = true
                        val stars = (3 + score / 20).coerceIn(3, 8)
                        soundPlayer.playWin(isMuted)
                        onComplete(score, "$score pts", stars)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quiz_next_button"),
            ) {
                Text(if (currentIndex + 1 < questions.size) "Next Question" else "Finish Quiz")
            }
        }

        OutlinedButton(
            onClick = onSyncQuestions,
            enabled = !isSyncing,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (isSyncing) "Syncing..." else "Fetch Fresh Online Questions")
        }
    }
}

// ---------------------------------------------------------------------------
// 2. MEMORY MATCH (4x4 / 4x5 / 6x4)
// ---------------------------------------------------------------------------
private enum class MemoryBoardSize(val label: String, val rows: Int, val cols: Int, val bonusStars: Int) {
    GRID_4X4("4×4", 4, 4, 4),
    GRID_4X5("4×5", 5, 4, 5),
    GRID_6X4("6×4", 6, 4, 6),
}

private data class MemoryCardModel(
    val uniqueId: Int,
    val pairId: Int,
    val icon: ImageVector,
    val label: String,
    val isFaceUp: Boolean = false,
    val isMatched: Boolean = false,
)

@Composable
fun MemoryMatchPlayView(
    bestScore: GameBestScoreEntity?,
    isMuted: Boolean,
    soundPlayer: SoundEffectPlayer,
    onComplete: (score: Int, detail: String, stars: Int) -> Unit,
) {
    val pastel = LocalPastelColors.current
    val allIcons = remember {
        listOf(
            Icons.Rounded.Pets to "Paw",
            Icons.Rounded.LocalHospital to "Clinic",
            Icons.Rounded.Healing to "Care",
            Icons.Rounded.Favorite to "Heart",
            Icons.Rounded.Star to "Star",
            Icons.Rounded.Spa to "Calm",
            Icons.Rounded.WaterDrop to "Hydrate",
            Icons.Rounded.EmojiNature to "Nature",
            Icons.Rounded.Vaccines to "Vaccine",
            Icons.Rounded.Science to "Lab",
            Icons.Rounded.MedicalServices to "Kit",
            Icons.Rounded.WbSunny to "Warmth",
        )
    }

    var boardSize by remember { mutableStateOf(MemoryBoardSize.GRID_4X4) }
    var sessionSeed by remember { mutableIntStateOf(0) }
    var cards by remember(sessionSeed, boardSize) {
        val pairCount = (boardSize.rows * boardSize.cols) / 2
        val chosenIcons = allIcons.shuffled().take(pairCount)
        mutableStateOf(
            (chosenIcons + chosenIcons).mapIndexed { index, (icon, label) ->
                MemoryCardModel(
                    uniqueId = index,
                    pairId = index % chosenIcons.size,
                    icon = icon,
                    label = label,
                )
            }.shuffled()
        )
    }
    var firstFlippedIndex by remember(sessionSeed, boardSize) { mutableStateOf<Int?>(null) }
    var secondFlippedIndex by remember(sessionSeed, boardSize) { mutableStateOf<Int?>(null) }
    var moves by remember(sessionSeed, boardSize) { mutableIntStateOf(0) }
    var feedback by remember(sessionSeed, boardSize) { mutableStateOf("Tap two cards to find a pair") }
    var finished by remember(sessionSeed, boardSize) { mutableStateOf(false) }

    LaunchedEffect(firstFlippedIndex, secondFlippedIndex) {
        val first = firstFlippedIndex
        val second = secondFlippedIndex
        if (first != null && second != null) {
            if (cards[first].pairId == cards[second].pairId) {
                soundPlayer.playCorrect(isMuted)
                feedback = "Matched ${cards[first].label}!"
                cards = cards.mapIndexed { idx, c ->
                    if (idx == first || idx == second) c.copy(isMatched = true) else c
                }
                firstFlippedIndex = null
                secondFlippedIndex = null
                if (cards.all { it.isMatched }) {
                    finished = true
                    val baseScore = boardSize.rows * boardSize.cols * 10
                    val score = (baseScore - moves * 4).coerceAtLeast(30)
                    soundPlayer.playWin(isMuted)
                    onComplete(score, "$score pts (${boardSize.label})", boardSize.bonusStars)
                }
            } else {
                soundPlayer.playWrong(isMuted)
                feedback = "Not a match"
                delay(650L)
                cards = cards.mapIndexed { idx, c ->
                    if (idx == first || idx == second) c.copy(isFaceUp = false) else c
                }
                firstFlippedIndex = null
                secondFlippedIndex = null
            }
        }
    }

    if (finished) {
        val baseScore = boardSize.rows * boardSize.cols * 10
        val score = (baseScore - moves * 4).coerceAtLeast(30)
        GameSummaryCard(
            title = "All Pairs Found!",
            scoreText = "$score pts in $moves moves (${boardSize.label})",
            bestText = bestScore?.bestDetail ?: "$score pts",
            starsEarned = boardSize.bonusStars,
            onReplay = { sessionSeed++ },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MemoryBoardSize.entries.forEach { sizeOption ->
                FilterChip(
                    selected = boardSize == sizeOption,
                    onClick = { boardSize = sizeOption },
                    label = { Text(sizeOption.label) },
                    modifier = Modifier.testTag("memory_size_${sizeOption.label}"),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Moves: $moves",
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Text(
            text = feedback,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (row in 0 until boardSize.rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (col in 0 until boardSize.cols) {
                        val idx = row * boardSize.cols + col
                        val card = cards[idx]
                        val revealed = card.isFaceUp || card.isMatched
                        val tileColor by animateColorAsState(
                            targetValue = when {
                                card.isMatched -> pastel.successContainer
                                revealed -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surface
                            },
                            label = "memory_tile",
                        )
                        Card(
                            onClick = {
                                if (!revealed && secondFlippedIndex == null) {
                                    soundPlayer.playTap(isMuted)
                                    cards = cards.mapIndexed { i, c ->
                                        if (i == idx) c.copy(isFaceUp = true) else c
                                    }
                                    if (firstFlippedIndex == null) {
                                        firstFlippedIndex = idx
                                    } else {
                                        moves++
                                        secondFlippedIndex = idx
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(if (boardSize == MemoryBoardSize.GRID_6X4) 1.15f else 1f)
                                .testTag("memory_card_$idx"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = tileColor),
                            border = BorderStroke(
                                1.dp,
                                if (card.isMatched) pastel.success else MaterialTheme.colorScheme.outline,
                            ),
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (revealed) {
                                    Icon(
                                        imageVector = card.icon,
                                        contentDescription = card.label,
                                        tint = if (card.isMatched) {
                                            pastel.success
                                        } else {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        },
                                        modifier = Modifier.size(28.dp),
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = "Hidden card",
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = { sessionSeed++ },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Restart")
        }
    }
}

// ---------------------------------------------------------------------------
// 3. REACTION SPEED TEST
// ---------------------------------------------------------------------------
private enum class ReactionPhase { IDLE, WAITING, GO, RESULT_ROUND, FINISHED }

@Composable
fun ReactionSpeedPlayView(
    bestScore: GameBestScoreEntity?,
    isMuted: Boolean,
    soundPlayer: SoundEffectPlayer,
    onComplete: (avgMs: Int, detail: String, stars: Int) -> Unit,
) {
    val pastel = LocalPastelColors.current
    var sessionSeed by remember { mutableIntStateOf(0) }
    var phase by remember(sessionSeed) { mutableStateOf(ReactionPhase.IDLE) }
    var round by remember(sessionSeed) { mutableIntStateOf(1) }
    var goStartMs by remember(sessionSeed) { mutableLongStateOf(0L) }
    var roundTimes by remember(sessionSeed) { mutableStateOf<List<Int>>(emptyList()) }
    var statusText by remember(sessionSeed) { mutableStateOf("Tap the pad below to start round 1 of 5") }

    LaunchedEffect(phase, round, sessionSeed) {
        if (phase == ReactionPhase.WAITING) {
            val waitMs = Random.nextLong(1100L, 3000L)
            delay(waitMs)
            goStartMs = System.currentTimeMillis()
            phase = ReactionPhase.GO
            statusText = "TAP NOW!"
        }
    }

    if (phase == ReactionPhase.FINISHED) {
        val avg = roundTimes.average().toInt().coerceAtLeast(1)
        GameSummaryCard(
            title = "Reflexes Tested!",
            scoreText = "Average: $avg ms",
            bestText = bestScore?.bestDetail ?: "$avg ms",
            starsEarned = 4,
            onReplay = { sessionSeed++ },
        )
        return
    }

    val padColor by animateColorAsState(
        targetValue = when (phase) {
            ReactionPhase.GO -> pastel.successContainer
            ReactionPhase.WAITING -> MaterialTheme.colorScheme.primaryContainer
            ReactionPhase.RESULT_ROUND -> MaterialTheme.colorScheme.tertiaryContainer
            else -> MaterialTheme.colorScheme.surface
        },
        label = "reaction_pad",
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Round $round / 5", style = MaterialTheme.typography.labelLarge)
            if (bestScore != null) {
                Text(
                    "Best: ${bestScore.bestDetail}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Card(
            onClick = {
                when (phase) {
                    ReactionPhase.IDLE, ReactionPhase.RESULT_ROUND -> {
                        statusText = "Wait for mint green..."
                        phase = ReactionPhase.WAITING
                    }
                    ReactionPhase.WAITING -> {
                        soundPlayer.playWrong(isMuted)
                        statusText = "Too early! Tap to retry round $round"
                        phase = ReactionPhase.IDLE
                    }
                    ReactionPhase.GO -> {
                        val reactionMs = (System.currentTimeMillis() - goStartMs).toInt().coerceAtLeast(40)
                        soundPlayer.playCorrect(isMuted)
                        val updated = roundTimes + reactionMs
                        roundTimes = updated
                        if (updated.size >= 5) {
                            val avg = updated.average().toInt().coerceAtLeast(1)
                            phase = ReactionPhase.FINISHED
                            soundPlayer.playWin(isMuted)
                            onComplete(avg, "$avg ms avg", 4)
                        } else {
                            round = updated.size + 1
                            statusText = "$reactionMs ms! Tap for round $round"
                            phase = ReactionPhase.RESULT_ROUND
                        }
                    }
                    ReactionPhase.FINISHED -> Unit
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("reaction_tap_pad"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = padColor),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 4. VET WORD SCRAMBLE (Adaptive Tiers + Hint Letter)
// ---------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordScramblePlayView(
    words: List<ScrambleWordEntity>,
    bestScore: GameBestScoreEntity?,
    isMuted: Boolean,
    soundPlayer: SoundEffectPlayer,
    onReplay: () -> Unit,
    onWordSolved: (String) -> Unit,
    onComplete: (score: Int, detail: String, stars: Int) -> Unit,
) {
    val pastel = LocalPastelColors.current
    var wordIndex by remember(words) { mutableIntStateOf(0) }
    var score by remember(words) { mutableIntStateOf(0) }
    var finished by remember(words) { mutableStateOf(false) }

    if (words.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading veterinary words...", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    if (finished) {
        val stars = (3 + score / 25).coerceIn(3, 8)
        GameSummaryCard(
            title = "Terminology Mastered!",
            scoreText = "$score pts",
            bestText = bestScore?.bestDetail ?: "$score pts",
            starsEarned = stars,
            onReplay = onReplay,
        )
        return
    }

    val currentWord = words[wordIndex]
    val targetWord = currentWord.word
    val tierLabel = when (currentWord.tier) {
        1 -> "Foundation"
        2 -> "Clinical"
        else -> "Master Vet"
    }

    val scrambledLetters = remember(wordIndex, words) {
        var letters = targetWord.toList().shuffled()
        if (letters.joinToString("") == targetWord && targetWord.length > 1) {
            letters = letters.reversed()
        }
        letters
    }
    var usedIndices by remember(wordIndex, words) { mutableStateOf<List<Int>>(emptyList()) }
    var hintsUsedForWord by remember(wordIndex, words) { mutableIntStateOf(0) }
    var feedback by remember(wordIndex, words) { mutableStateOf<Pair<Boolean, String>?>(null) }

    val builtWord = usedIndices.map { scrambledLetters[it] }.joinToString("")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Word ${wordIndex + 1} / ${words.size}", style = MaterialTheme.typography.labelLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = tierLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                Text("Score: $score", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }

        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Clue:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                Text(currentWord.clue, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = builtWord.ifEmpty { "Tap letters below (${targetWord.length} letters)..." },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(vertical = 14.dp, horizontal = 12.dp)
                            .testTag("scramble_built_word"),
                    )
                }
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            scrambledLetters.forEachIndexed { idx, ch ->
                val isUsed = idx in usedIndices
                OutlinedButton(
                    onClick = {
                        if (!isUsed && feedback == null) {
                            soundPlayer.playTap(isMuted)
                            usedIndices = usedIndices + idx
                        }
                    },
                    enabled = !isUsed && feedback == null,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("scramble_letter_$idx"),
                ) {
                    Text(ch.toString(), style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = {
                    if (feedback == null) usedIndices = emptyList()
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("Clear")
            }
            OutlinedButton(
                onClick = {
                    if (feedback == null && builtWord.length < targetWord.length) {
                        // Match existing correct prefix or reset to longest valid prefix, then reveal next letter
                        val validPrefixLen = builtWord.zip(targetWord).takeWhile { (a, b) -> a == b }.size
                        var keptIndices = usedIndices.take(validPrefixLen)
                        val neededChar = targetWord[validPrefixLen]
                        val nextIdx = scrambledLetters.indices.firstOrNull { i ->
                            i !in keptIndices && scrambledLetters[i] == neededChar
                        }
                        if (nextIdx != null) {
                            soundPlayer.playTap(isMuted)
                            keptIndices = keptIndices + nextIdx
                            usedIndices = keptIndices
                            hintsUsedForWord++
                        }
                    }
                },
                enabled = feedback == null && builtWord.length < targetWord.length,
                modifier = Modifier
                    .weight(1f)
                    .testTag("scramble_hint_button"),
            ) {
                Icon(Icons.Rounded.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Hint")
            }
            Button(
                onClick = {
                    if (builtWord.length == targetWord.length && feedback == null) {
                        if (builtWord == targetWord) {
                            val earned = (20 - hintsUsedForWord * 3).coerceAtLeast(10)
                            score += earned
                            soundPlayer.playCorrect(isMuted)
                            onWordSolved(targetWord)
                            feedback = true to "Spot on! $targetWord (+$earned pts)"
                        } else {
                            soundPlayer.playWrong(isMuted)
                            feedback = false to "Answer: $targetWord"
                        }
                    }
                },
                enabled = builtWord.length == targetWord.length && feedback == null,
                modifier = Modifier
                    .weight(1f)
                    .testTag("scramble_check_button"),
            ) {
                Text("Check")
            }
        }

        if (feedback != null) {
            val (ok, msg) = feedback!!
            Text(
                text = msg,
                style = MaterialTheme.typography.titleMedium,
                color = if (ok) pastel.success else pastel.wrong,
            )
            Button(
                onClick = {
                    if (wordIndex + 1 < words.size) {
                        wordIndex++
                    } else {
                        finished = true
                        val stars = (3 + score / 25).coerceIn(3, 8)
                        soundPlayer.playWin(isMuted)
                        onComplete(score, "$score pts", stars)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (wordIndex + 1 < words.size) "Next Word" else "Finish")
            }
        }
    }
}

@Composable
private fun GameSummaryCard(
    title: String,
    scoreText: String,
    bestText: String,
    starsEarned: Int,
    onReplay: () -> Unit,
) {
    val pastel = LocalPastelColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("game_summary_card"),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = pastel.success,
                modifier = Modifier.size(48.dp),
            )
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(scoreText, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text("Best: $bestText", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = pastel.starGoldSoft,
            ) {
                Text(
                    text = "+$starsEarned ★ earned!",
                    style = MaterialTheme.typography.titleMedium,
                    color = pastel.starGold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onReplay,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("game_replay_button"),
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Play Again")
            }
        }
    }
}
