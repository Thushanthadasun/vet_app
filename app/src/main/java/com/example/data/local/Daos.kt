package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MemeDao {
    @Query("SELECT * FROM memes ORDER BY createdAt DESC, id DESC")
    fun observeAllMemes(): Flow<List<MemeEntity>>

    @Query("SELECT COUNT(*) FROM memes")
    suspend fun getMemeCount(): Int

    @Query("SELECT imageUrlOrPath FROM memes WHERE imageUrlOrPath != ''")
    suspend fun getAllImageUrls(): List<String>

    @Query("SELECT title FROM memes")
    suspend fun getAllTitles(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemes(memes: List<MemeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeme(meme: MemeEntity): Long

    @Query("UPDATE memes SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("DELETE FROM memes WHERE isUserAdded = 0 AND drawableResName = '' AND isFavorite = 0 AND id NOT IN (SELECT id FROM memes WHERE isUserAdded = 0 AND drawableResName = '' ORDER BY createdAt DESC LIMIT 80)")
    suspend fun trimOldRemoteMemes()

    @Query("DELETE FROM memes WHERE id = :id")
    suspend fun deleteMemeById(id: Long)
}

@Dao
interface QuizDao {
    @Query("SELECT * FROM quiz_questions ORDER BY isBuiltIn ASC, id DESC")
    fun observeAllQuestions(): Flow<List<QuizQuestionEntity>>

    @Query("SELECT COUNT(*) FROM quiz_questions")
    fun observeQuestionCount(): Flow<Int>

    @Query("SELECT * FROM quiz_questions")
    suspend fun getAllQuestionsOnce(): List<QuizQuestionEntity>

    @Query("SELECT question FROM quiz_questions")
    suspend fun getAllQuestionTexts(): List<String>

    @Query("SELECT COUNT(*) FROM quiz_questions")
    suspend fun getQuestionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuizQuestionEntity>)

    @Query("UPDATE quiz_questions SET timesSeen = timesSeen + 1, timesWrong = timesWrong + :wrongDelta WHERE id = :id")
    suspend fun recordAnswer(id: Long, wrongDelta: Int)
}

@Dao
interface ScrambleDao {
    @Query("SELECT * FROM scramble_words ORDER BY timesSolved ASC, tier ASC")
    suspend fun getAllWordsOnce(): List<ScrambleWordEntity>

    @Query("SELECT COUNT(*) FROM scramble_words")
    suspend fun getWordCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWords(words: List<ScrambleWordEntity>)

    @Query("UPDATE scramble_words SET timesSolved = timesSolved + 1 WHERE word = :word")
    suspend fun markSolved(word: String)
}

@Dao
interface GameBestScoreDao {
    @Query("SELECT * FROM game_best_scores")
    fun observeBestScores(): Flow<List<GameBestScoreEntity>>

    @Query("SELECT * FROM game_best_scores WHERE gameId = :gameId LIMIT 1")
    suspend fun getBestScore(gameId: String): GameBestScoreEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBestScore(score: GameBestScoreEntity)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY isCompleted ASC, dueDateEpochDay ASC, createdAt DESC")
    fun observeAllGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE isCompleted = 0 AND reminderEpochMillis IS NOT NULL")
    suspend fun getPendingReminderGoals(): List<GoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoalById(id: Long)
}

@Dao
interface GiftDao {
    @Query("SELECT * FROM gift_items ORDER BY tier ASC, id ASC")
    fun observeGiftPool(): Flow<List<GiftItemEntity>>

    @Query("SELECT * FROM gift_items")
    suspend fun getGiftPoolOnce(): List<GiftItemEntity>

    @Query("SELECT COUNT(*) FROM gift_items")
    suspend fun getGiftPoolCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGifts(gifts: List<GiftItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGift(gift: GiftItemEntity): Long

    @Query("DELETE FROM gift_items WHERE id = :id")
    suspend fun deleteGiftById(id: Long)

    @Query("SELECT * FROM opened_gift_cards ORDER BY openedAt DESC, id DESC")
    fun observeOpenedCards(): Flow<List<OpenedGiftCardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOpenedCard(card: OpenedGiftCardEntity): Long

    @Query("UPDATE opened_gift_cards SET isDelivered = :delivered WHERE id = :id")
    suspend fun setCardDelivered(id: Long, delivered: Boolean)
}
