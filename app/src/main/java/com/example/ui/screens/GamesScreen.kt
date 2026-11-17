package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Spellcheck
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.GameBestScoreEntity
import com.example.ui.components.FancyGlassCard
import com.example.ui.components.FancyPillBadge
import com.example.ui.components.fancyIridescentBorder
import com.example.ui.theme.LocalPastelColors

@Composable
fun GamesScreen(
    viewModel: GamesViewModel,
    modifier: Modifier = Modifier,
) {
    val activeGame by viewModel.activeGame.collectAsStateWithLifecycle()
    val bestScores by viewModel.bestScoresMap.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val quizQuestions by viewModel.quizQuestions.collectAsStateWithLifecycle()
    val scrambleWords by viewModel.scrambleWords.collectAsStateWithLifecycle()
    val quizBankCount by viewModel.quizBankCount.collectAsStateWithLifecycle()
    val isSyncingQuiz by viewModel.isSyncingQuiz.collectAsStateWithLifecycle()
    val syncNotice by viewModel.syncNotice.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.autoSyncQuizIfNeeded()
    }

    if (activeGame != null) {
        BackHandler { viewModel.exitGame() }
    }

    AnimatedContent(
        targetState = activeGame,
        label = "games_content",
        modifier = modifier.fillMaxSize(),
    ) { selectedGame ->
        if (selectedGame == null) {
            GamesHub(
                bestScores = bestScores,
                quizBankCount = quizBankCount,
                isMuted = isMuted,
                isSyncingQuiz = isSyncingQuiz,
                syncNotice = syncNotice,
                onToggleMute = viewModel::toggleMute,
                onSyncQuiz = viewModel::syncNewQuizQuestions,
                onDismissSyncNotice = viewModel::clearSyncNotice,
                onSelectGame = viewModel::openGame,
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = viewModel::exitGame,
                        modifier = Modifier.testTag("game_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back to Games list",
                        )
                    }

                    Text(
                        text = selectedGame.title,
                        style = MaterialTheme.typography.titleLarge,
                    )

                    IconButton(
                        onClick = viewModel::toggleMute,
                        modifier = Modifier.testTag("game_mute_button"),
                    ) {
                        Icon(
                            imageVector = if (isMuted) {
                                Icons.AutoMirrored.Rounded.VolumeOff
                            } else {
                                Icons.AutoMirrored.Rounded.VolumeUp
                            },
                            contentDescription = if (isMuted) "Unmute sound" else "Mute sound",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .weight(1f),
                ) {
                    when (selectedGame) {
                        GameType.VET_QUIZ -> VetQuizPlayView(
                            questions = quizQuestions,
                            bestScore = bestScores[GameType.VET_QUIZ.id],
                            isMuted = isMuted,
                            soundPlayer = viewModel.soundPlayer,
                            onReplay = viewModel::loadQuizSession,
                            onSyncQuestions = viewModel::syncNewQuizQuestions,
                            isSyncing = isSyncingQuiz,
                            onRecordAnswer = viewModel::recordQuizAnswer,
                            onComplete = { score, detail, stars ->
                                viewModel.recordResult(GameType.VET_QUIZ, score, detail, stars)
                            },
                        )
                        GameType.MEMORY_MATCH -> MemoryMatchPlayView(
                            bestScore = bestScores[GameType.MEMORY_MATCH.id],
                            isMuted = isMuted,
                            soundPlayer = viewModel.soundPlayer,
                            onComplete = { score, detail, stars ->
                                viewModel.recordResult(GameType.MEMORY_MATCH, score, detail, stars)
                            },
                        )
                        GameType.REACTION_TEST -> ReactionSpeedPlayView(
                            bestScore = bestScores[GameType.REACTION_TEST.id],
                            isMuted = isMuted,
                            soundPlayer = viewModel.soundPlayer,
                            onComplete = { avgMs, detail, stars ->
                                // Store lowest reaction time (ms) as bestScore with higherIsBetter = false
                                viewModel.recordResult(
                                    game = GameType.REACTION_TEST,
                                    score = avgMs,
                                    detail = detail,
                                    starsEarned = stars,
                                    higherIsBetter = false,
                                )
                            },
                        )
                        GameType.WORD_SCRAMBLE -> WordScramblePlayView(
                            words = scrambleWords,
                            bestScore = bestScores[GameType.WORD_SCRAMBLE.id],
                            isMuted = isMuted,
                            soundPlayer = viewModel.soundPlayer,
                            onReplay = viewModel::loadScrambleSession,
                            onWordSolved = viewModel::markScrambleWordSolved,
                            onComplete = { score, detail, stars ->
                                viewModel.recordResult(GameType.WORD_SCRAMBLE, score, detail, stars)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GamesHub(
    bestScores: Map<String, GameBestScoreEntity>,
    quizBankCount: Int,
    isMuted: Boolean,
    isSyncingQuiz: Boolean,
    syncNotice: String?,
    onToggleMute: () -> Unit,
    onSyncQuiz: () -> Unit,
    onDismissSyncNotice: () -> Unit,
    onSelectGame: (GameType) -> Unit,
) {
    val pastel = LocalPastelColors.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("games_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    FancyPillBadge(
                        text = "Earn Stars • Have Fun",
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f),
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.games_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = stringResource(R.string.games_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = pastel.glassSurface,
                    border = fancyIridescentBorder(1.2.dp),
                ) {
                    IconButton(
                        onClick = onToggleMute,
                        modifier = Modifier.testTag("games_hub_mute_button"),
                    ) {
                        Icon(
                            imageVector = if (isMuted) {
                                Icons.AutoMirrored.Rounded.VolumeOff
                            } else {
                                Icons.AutoMirrored.Rounded.VolumeUp
                            },
                            contentDescription = if (isMuted) "Unmute game sounds" else "Mute game sounds",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onSyncQuiz,
                    enabled = !isSyncingQuiz,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("sync_quiz_button"),
                ) {
                    if (isSyncingQuiz) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download New Questions")
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    border = fancyIridescentBorder(0.9.dp),
                    modifier = Modifier.testTag("quiz_bank_count_badge"),
                ) {
                    Text(
                        text = "$quizBankCount+ in bank",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }

        if (syncNotice != null) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f),
                    border = fancyIridescentBorder(1.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = syncNotice,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onDismissSyncNotice) { Text("OK") }
                    }
                }
            }
        }

        items(GameType.entries) { game ->
            val icon: ImageVector = when (game) {
                GameType.VET_QUIZ -> Icons.Rounded.Quiz
                GameType.MEMORY_MATCH -> Icons.Rounded.GridView
                GameType.REACTION_TEST -> Icons.Rounded.Bolt
                GameType.WORD_SCRAMBLE -> Icons.Rounded.Spellcheck
            }
            val medallionColors = when (game) {
                GameType.VET_QUIZ -> listOf(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.secondaryContainer,
                )
                GameType.MEMORY_MATCH -> listOf(
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.colorScheme.tertiaryContainer,
                )
                GameType.REACTION_TEST -> listOf(
                    pastel.starGoldSoft,
                    MaterialTheme.colorScheme.primaryContainer,
                )
                GameType.WORD_SCRAMBLE -> listOf(
                    MaterialTheme.colorScheme.tertiaryContainer,
                    MaterialTheme.colorScheme.primaryContainer,
                )
            }
            val iconTint = when (game) {
                GameType.VET_QUIZ -> MaterialTheme.colorScheme.primary
                GameType.MEMORY_MATCH -> MaterialTheme.colorScheme.secondary
                GameType.REACTION_TEST -> pastel.starGold
                GameType.WORD_SCRAMBLE -> MaterialTheme.colorScheme.tertiary
            }
            val best = bestScores[game.id]

            FancyGlassCard(
                onClick = { onSelectGame(game) },
                cornerRadius = 26.dp,
                modifier = Modifier.testTag("game_card_${game.id}"),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Brush.linearGradient(medallionColors)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = game.title,
                            tint = iconTint,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = game.title,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = game.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (best != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Best: ${best.bestDetail}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary,
                                    )
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                    ) {
                        Text(
                            text = "Play",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}
