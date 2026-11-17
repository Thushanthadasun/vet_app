package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.AppDatabase
import com.example.data.local.GameBestScoreEntity
import com.example.data.local.GiftItemEntity
import com.example.data.local.GiftTier
import com.example.data.local.GoalEntity
import com.example.data.local.MemeEntity
import com.example.data.local.OpenedGiftCardEntity
import com.example.data.local.QuizQuestionEntity
import com.example.data.local.ScrambleWordEntity
import com.example.data.local.SeedData
import com.example.data.local.VetQuestionGenerator
import com.example.data.network.FeedNetworkClient
import com.example.data.preferences.AppPreferencesRepository
import com.example.data.preferences.AppPreferencesState
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.random.Random

sealed class SyncResult {
    data class Success(val count: Int, val message: String) : SyncResult()
    data class Error(val message: String) : SyncResult()
}

class PastelPawsRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val preferences: AppPreferencesRepository,
    private val network: FeedNetworkClient,
) {
    private val memeDao = database.memeDao()
    private val quizDao = database.quizDao()
    private val scrambleDao = database.scrambleDao()
    private val gameDao = database.gameBestScoreDao()
    private val goalDao = database.goalDao()
    private val giftDao = database.giftDao()

    val prefsFlow: Flow<AppPreferencesState> = preferences.preferencesFlow
    val memesFlow: Flow<List<MemeEntity>> = memeDao.observeAllMemes()
    val quizCountFlow: Flow<Int> = quizDao.observeQuestionCount()
    val bestScoresFlow: Flow<List<GameBestScoreEntity>> = gameDao.observeBestScores()
    val goalsFlow: Flow<List<GoalEntity>> = goalDao.observeAllGoals()
    val giftPoolFlow: Flow<List<GiftItemEntity>> = giftDao.observeGiftPool()
    val openedCardsFlow: Flow<List<OpenedGiftCardEntity>> = giftDao.observeOpenedCards()

    suspend fun ensureSeeded() {
        AppDatabase.seedIfEmpty(database)
    }

    // --- Memes (Accumulative + Offline Cache + Custom JSON/RSS + Live Social Trending) ---
    suspend fun refreshMemesFromFeed(autoBackground: Boolean = false): SyncResult {
        ensureSeeded()
        val prefs = prefsFlow.first()
        if (autoBackground) {
            val twoHoursMs = 2 * 60 * 60 * 1000L
            if (System.currentTimeMillis() - prefs.lastMemeSyncMillis < twoHoursMs) {
                return SyncResult.Success(0, "")
            }
        }

        val customUrl = prefs.memeFeedUrl.trim()
        val fetched = mutableListOf<MemeEntity>()
        var customError: String? = null

        if (customUrl.isNotBlank()) {
            try {
                fetched += network.fetchCustomMemeFeed(customUrl)
            } catch (e: Exception) {
                customError = e.message
            }
        }

        val socialOutcome = network.fetchPublicSocialMemes()
        fetched += socialOutcome.memes

        val existingUrls = memeDao.getAllImageUrls().toSet()
        val existingTitles = memeDao.getAllTitles().map { it.lowercase() }.toSet()

        val newMemes = fetched.distinctBy { it.imageUrlOrPath.ifBlank { it.title } }.filter { meme ->
            val urlNew = meme.imageUrlOrPath.isBlank() || meme.imageUrlOrPath !in existingUrls
            val titleNew = meme.title.lowercase() !in existingTitles
            urlNew && titleNew
        }

        return if (newMemes.isNotEmpty()) {
            memeDao.insertMemes(newMemes)
            memeDao.trimOldRemoteMemes()
            preferences.setLastMemeSyncMillis(System.currentTimeMillis())
            SyncResult.Success(
                count = newMemes.size,
                message = "Loaded ${newMemes.size} trending social memes & cached them offline.",
            )
        } else if (customError != null) {
            SyncResult.Error("Could not reach custom feed ($customError). Showing cached memes.")
        } else if (socialOutcome.sourcesReached == 0) {
            if (autoBackground) {
                SyncResult.Success(0, "")
            } else {
                SyncResult.Error(
                    "Could not reach social meme sources (${socialOutcome.lastError ?: "offline"}). Showing cached memes."
                )
            }
        } else {
            preferences.setLastMemeSyncMillis(System.currentTimeMillis())
            val msg = if (customUrl.isBlank()) {
                "Trending feeds synced. Paste a Sinhala JSON or RSS link in PIN Settings for your own FB-style page."
            } else {
                "All trending memes are already cached and up to date."
            }
            SyncResult.Success(0, msg)
        }
    }

    suspend fun toggleMemeFavorite(id: Long, favorite: Boolean) {
        memeDao.setFavorite(id, favorite)
    }

    suspend fun addMemeFromGallery(uri: Uri, sinhalaCaption: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "memes").apply { mkdirs() }
            val dest = File(dir, "gallery_${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext false

            val caption = sinhalaCaption.trim().ifBlank { "මගේ ගැලරියෙන් එකතු කළ මීම් එකක්" }
            memeDao.insertMeme(
                MemeEntity(
                    title = caption,
                    imageUrlOrPath = dest.absolutePath,
                    drawableResName = "",
                    sourceName = "Phone Gallery",
                    sourceUrl = "",
                    isFavorite = false,
                    isUserAdded = true,
                )
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun deleteUserMeme(meme: MemeEntity) {
        if (!meme.isUserAdded) return
        if (meme.imageUrlOrPath.isNotBlank()) {
            runCatching { File(meme.imageUrlOrPath).delete() }
        }
        memeDao.deleteMemeById(meme.id)
    }

    // --- Infinite & Intelligent Games ---
    suspend fun refreshQuizQuestions(autoBackground: Boolean = false): SyncResult {
        ensureSeeded()
        val prefs = prefsFlow.first()
        if (autoBackground) {
            val fourHoursMs = 4 * 60 * 60 * 1000L
            if (System.currentTimeMillis() - prefs.lastQuizSyncMillis < fourHoursMs) {
                return SyncResult.Success(0, "")
            }
        }

        val customUrl = prefs.customQuizUrl.trim()
        val fetchedQuestions = mutableListOf<QuizQuestionEntity>()
        val fetchedWords = mutableListOf<ScrambleWordEntity>()
        var errorNote: String? = null

        // 1. Open Trivia DB with session token so questions never repeat across refreshes
        try {
            var token = prefs.openTdbToken
            if (token.isBlank()) {
                token = network.requestOpenTdbToken().orEmpty()
                if (token.isNotBlank()) preferences.setOpenTdbToken(token)
            }
            var tdbResult = network.fetchOpenTriviaAnimalQuestions(token)
            if (tdbResult.tokenExpired) {
                val freshToken = network.requestOpenTdbToken().orEmpty()
                if (freshToken.isNotBlank()) {
                    preferences.setOpenTdbToken(freshToken)
                    tdbResult = network.fetchOpenTriviaAnimalQuestions(freshToken)
                }
            }
            fetchedQuestions += tdbResult.questions
        } catch (e: Exception) {
            errorNote = e.message
        }

        // 2. The Trivia API (science/biology/animals, free & continuously updated)
        runCatching {
            fetchedQuestions += network.fetchTheTriviaApiQuestions()
        }

        // 3. User's Custom Vet Quiz + Word Scramble JSON URL
        if (customUrl.isNotBlank()) {
            try {
                val custom = network.fetchCustomQuizAndWords(customUrl)
                fetchedQuestions += custom.questions
                fetchedWords += custom.words
            } catch (e: Exception) {
                errorNote = e.message
            }
        }

        val existingTexts = quizDao.getAllQuestionTexts().map { it.trim().lowercase() }.toSet()
        val uniqueNewQuestions = fetchedQuestions
            .distinctBy { it.question.trim().lowercase() }
            .filter { it.question.trim().lowercase() !in existingTexts }

        if (uniqueNewQuestions.isNotEmpty()) {
            quizDao.insertQuestions(uniqueNewQuestions)
        }
        if (fetchedWords.isNotEmpty()) {
            scrambleDao.insertWords(fetchedWords)
        }

        return if (uniqueNewQuestions.isNotEmpty() || fetchedWords.isNotEmpty()) {
            preferences.setLastQuizSyncMillis(System.currentTimeMillis())
            SyncResult.Success(
                count = uniqueNewQuestions.size,
                message = "Added ${uniqueNewQuestions.size} new questions to your growing Vet Quiz bank!",
            )
        } else if (fetchedQuestions.isNotEmpty()) {
            preferences.setLastQuizSyncMillis(System.currentTimeMillis())
            SyncResult.Success(0, "Question bank is synced and ready.")
        } else {
            SyncResult.Error("Offline mode (${errorNote ?: "no connection"}). Using built-in bank + smart generator.")
        }
    }

    /**
     * Intelligent adaptive session builder:
     * Prioritizes unseen questions, resurfaces previously missed questions (spaced repetition),
     * then adds procedural clinical vet questions so quizzes remain fresh and infinite offline too.
     */
    suspend fun getQuizSessionQuestions(limit: Int = 8): List<QuizQuestionEntity> {
        ensureSeeded()
        val bank = quizDao.getAllQuestionsOnce().ifEmpty { SeedData.starterQuizQuestions() }

        val unseen = bank.filter { it.timesSeen == 0 }.shuffled()
        val needsReview = bank.filter { it.timesSeen > 0 && it.timesWrong > 0 }
            .sortedByDescending { it.timesWrong }
            .shuffled()
        val familiar = bank.filter { it.timesSeen > 0 && it.timesWrong == 0 }
            .sortedBy { it.timesSeen }
            .shuffled()

        val procedural = VetQuestionGenerator.generateBatch(2)
        val ordered = (unseen + needsReview + familiar).distinctBy { it.question }
        val dbQuota = (limit - procedural.size).coerceAtLeast(1)
        return (ordered.take(dbQuota) + procedural).shuffled().take(limit)
    }

    suspend fun recordQuizAnswer(questionId: Long, wasCorrect: Boolean) {
        if (questionId <= 0L) return
        quizDao.recordAnswer(questionId, wrongDelta = if (wasCorrect) 0 else 1)
    }

    suspend fun getAdaptiveScrambleWords(count: Int = 6): List<ScrambleWordEntity> {
        ensureSeeded()
        val all = scrambleDao.getAllWordsOnce().ifEmpty { SeedData.starterScrambleWords() }
        // Serve least-solved words first, progressing gently from Tier 1 -> Tier 2 -> Tier 3
        val leastSolved = all.groupBy { it.timesSolved }.toSortedMap()
        val picked = mutableListOf<ScrambleWordEntity>()
        for ((_, bucket) in leastSolved) {
            val tier1 = bucket.filter { it.tier == 1 }.shuffled()
            val tier2 = bucket.filter { it.tier == 2 }.shuffled()
            val tier3 = bucket.filter { it.tier >= 3 }.shuffled()
            picked += tier1.take(2) + tier2.take(2) + tier3.take(2) + bucket.shuffled()
            if (picked.distinctBy { it.word }.size >= count) break
        }
        return picked.distinctBy { it.word }.take(count)
    }

    suspend fun markScrambleWordSolved(word: String) {
        scrambleDao.markSolved(word)
    }

    suspend fun recordGameCompletion(
        gameId: String,
        score: Int,
        detail: String,
        starsEarned: Int,
        higherIsBetter: Boolean = true,
    ) {
        val existing = gameDao.getBestScore(gameId)
        val isNewBest = when {
            existing == null -> true
            higherIsBetter -> score > existing.bestScore
            else -> score < existing.bestScore
        }
        if (isNewBest) {
            gameDao.upsertBestScore(
                GameBestScoreEntity(
                    gameId = gameId,
                    bestScore = score,
                    bestDetail = detail,
                )
            )
        }
        if (starsEarned > 0) {
            preferences.addStars(starsEarned)
        }
    }

    // --- Goals ---
    suspend fun addGoal(title: String, dueDateEpochDay: Long?, reminderEpochMillis: Long?): GoalEntity {
        val entity = GoalEntity(
            title = title.trim(),
            dueDateEpochDay = dueDateEpochDay,
            reminderEpochMillis = reminderEpochMillis,
        )
        val id = goalDao.insertGoal(entity)
        return entity.copy(id = id)
    }

    suspend fun toggleGoalCompleted(goal: GoalEntity): Int {
        val nowCompleted = !goal.isCompleted
        var starsGranted = 0
        val updated = if (nowCompleted && !goal.starsAwarded) {
            starsGranted = 15
            preferences.addStars(starsGranted)
            goal.copy(isCompleted = true, starsAwarded = true)
        } else {
            goal.copy(isCompleted = nowCompleted)
        }
        goalDao.updateGoal(updated)
        return starsGranted
    }

    suspend fun deleteGoal(goalId: Long) {
        goalDao.deleteGoalById(goalId)
    }

    suspend fun getPendingReminderGoals(): List<GoalEntity> = goalDao.getPendingReminderGoals()

    // --- Mystery Gift Boxes ---
    suspend fun openNextMysteryBox(): OpenedGiftCardEntity? {
        val prefs = prefsFlow.first()
        if (prefs.totalUnopenedBoxes <= 0) return null
        ensureSeeded()
        val pool = giftDao.getGiftPoolOnce().ifEmpty { SeedData.starterGifts() }
        val chosen = drawWeightedGift(pool)
        val claimCode = generateClaimCode()
        val card = OpenedGiftCardEntity(
            giftTitle = chosen.title,
            tier = chosen.giftTier.name,
            claimCode = claimCode,
            openedAt = System.currentTimeMillis(),
            isDelivered = false,
        )
        val id = giftDao.insertOpenedCard(card)
        preferences.recordBoxOpened(useBirthdayBoxFirst = prefs.birthdayBonusBoxes > 0)
        return card.copy(id = id)
    }

    private fun drawWeightedGift(pool: List<GiftItemEntity>): GiftItemEntity {
        val byTier = pool.groupBy { it.giftTier }
        val availableTiers = byTier.keys.toList()
        val totalWeight = availableTiers.sumOf { it.weight }
        var roll = Random.nextInt(totalWeight.coerceAtLeast(1))
        var selectedTier = availableTiers.first()
        for (tier in availableTiers) {
            if (roll < tier.weight) {
                selectedTier = tier
                break
            }
            roll -= tier.weight
        }
        val candidates = byTier[selectedTier].orEmpty().ifEmpty { pool }
        return candidates[Random.nextInt(candidates.size)]
    }

    private fun generateClaimCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val suffix = (1..4).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        return "PAW-$suffix"
    }

    suspend fun setCardDelivered(cardId: Long, delivered: Boolean) {
        giftDao.setCardDelivered(cardId, delivered)
    }

    suspend fun addGiftToPool(title: String, tier: GiftTier) {
        if (title.isBlank()) return
        giftDao.insertGift(GiftItemEntity(title = title.trim(), tier = tier.name))
    }

    suspend fun deleteGiftFromPool(giftId: Long) {
        giftDao.deleteGiftById(giftId)
    }

    // --- Settings & Birthday ---
    suspend fun grantBirthdayBoxIfNeeded(year: Int): Boolean =
        preferences.grantBirthdayBoxForYearIfNeeded(year)

    suspend fun updateAdminSettings(
        nickname: String,
        pinCode: String,
        starsPerBox: Int,
        birthdayMessage: String,
        memeFeedUrl: String,
        customQuizUrl: String,
    ) = preferences.updateAdminSettings(
        nickname = nickname,
        pinCode = pinCode,
        starsPerBox = starsPerBox,
        birthdayMessage = birthdayMessage,
        memeFeedUrl = memeFeedUrl,
        customQuizUrl = customQuizUrl,
    )

    suspend fun setMuted(muted: Boolean) = preferences.setMuted(muted)
    suspend fun setThemeMode(mode: ThemeMode) = preferences.setThemeMode(mode)
}
