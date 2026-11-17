package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class GiftTier(val label: String, val weight: Int) {
    SMALL("Small", 65),
    MEDIUM("Medium", 27),
    BIG("Big", 8),
}

@Entity(tableName = "memes")
data class MemeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val imageUrlOrPath: String = "",
    val drawableResName: String = "",
    val sourceName: String,
    val sourceUrl: String = "",
    val isFavorite: Boolean = false,
    val isUserAdded: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "quiz_questions")
data class QuizQuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val question: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctIndex: Int,
    val sourceLabel: String,
    val isBuiltIn: Boolean = false,
    val timesSeen: Int = 0,
    val timesWrong: Int = 0,
    val difficulty: String = "medium",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "scramble_words")
data class ScrambleWordEntity(
    @PrimaryKey val word: String,
    val clue: String,
    val tier: Int = 1,
    val timesSolved: Int = 0,
    val isBuiltIn: Boolean = true,
)

@Entity(tableName = "game_best_scores")
data class GameBestScoreEntity(
    @PrimaryKey val gameId: String,
    val bestScore: Int,
    val bestDetail: String,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dueDateEpochDay: Long? = null,
    val reminderEpochMillis: Long? = null,
    val isCompleted: Boolean = false,
    val starsAwarded: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "gift_items")
data class GiftItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val tier: String = GiftTier.SMALL.name,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val giftTier: GiftTier
        get() = runCatching { GiftTier.valueOf(tier) }.getOrDefault(GiftTier.SMALL)
}

@Entity(tableName = "opened_gift_cards")
data class OpenedGiftCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val giftTitle: String,
    val tier: String,
    val claimCode: String,
    val openedAt: Long = System.currentTimeMillis(),
    val isDelivered: Boolean = false,
) {
    val giftTier: GiftTier
        get() = runCatching { GiftTier.valueOf(tier) }.getOrDefault(GiftTier.SMALL)
}
