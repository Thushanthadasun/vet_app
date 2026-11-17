package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Database(
    entities = [
        MemeEntity::class,
        QuizQuestionEntity::class,
        ScrambleWordEntity::class,
        GameBestScoreEntity::class,
        GoalEntity::class,
        GiftItemEntity::class,
        OpenedGiftCardEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memeDao(): MemeDao
    abstract fun quizDao(): QuizDao
    abstract fun scrambleDao(): ScrambleDao
    abstract fun gameBestScoreDao(): GameBestScoreDao
    abstract fun goalDao(): GoalDao
    abstract fun giftDao(): GiftDao

    companion object {
        private val seedMutex = Mutex()

        fun build(context: Context): AppDatabase {
            lateinit var instance: AppDatabase
            instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "pastel_paws.db",
            )
                .fallbackToDestructiveMigration(true)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            seedIfEmpty(instance)
                        }
                    }
                })
                .build()
            return instance
        }

        suspend fun seedIfEmpty(db: AppDatabase) = seedMutex.withLock {
            if (db.memeDao().getMemeCount() == 0) {
                db.memeDao().insertMemes(SeedData.starterMemes())
            }
            if (db.quizDao().getQuestionCount() == 0) {
                db.quizDao().insertQuestions(SeedData.starterQuizQuestions())
            }
            if (db.scrambleDao().getWordCount() == 0) {
                db.scrambleDao().insertWords(SeedData.starterScrambleWords())
            }
            if (db.giftDao().getGiftPoolCount() == 0) {
                db.giftDao().insertGifts(SeedData.starterGifts())
            }
        }
    }
}
