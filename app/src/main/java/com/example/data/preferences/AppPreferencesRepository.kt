package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pastel_paws_prefs")

data class AppPreferencesState(
    val nickname: String = "Doc",
    val pinCode: String = "0306",
    val totalStars: Int = 0,
    val starsPerBox: Int = 50,
    val boxesOpenedFromStars: Int = 0,
    val birthdayBonusBoxes: Int = 0,
    val lastBirthdayBoxYear: Int = 0,
    val birthdayMessage: String = DEFAULT_BIRTHDAY_MESSAGE,
    val memeFeedUrl: String = "",
    val customQuizUrl: String = "",
    val isMuted: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val lastMemeSyncMillis: Long = 0L,
    val lastQuizSyncMillis: Long = 0L,
    val openTdbToken: String = "",
) {
    val earnedBoxesFromStars: Int
        get() = if (starsPerBox > 0) totalStars / starsPerBox else 0

    val unopenedFromStars: Int
        get() = (earnedBoxesFromStars - boxesOpenedFromStars).coerceAtLeast(0)

    val totalUnopenedBoxes: Int
        get() = unopenedFromStars + birthdayBonusBoxes.coerceAtLeast(0)

    val starsIntoCurrentBox: Int
        get() = if (starsPerBox > 0) totalStars % starsPerBox else 0

    val progressFraction: Float
        get() = if (starsPerBox > 0) {
            (starsIntoCurrentBox.toFloat() / starsPerBox.toFloat()).coerceIn(0f, 1f)
        } else 0f

    companion object {
        const val DEFAULT_BIRTHDAY_MESSAGE =
            "Happy Birthday, Doc! Wishing you calm study nights, happy furry patients, warm coffee, and a wonderful year ahead in veterinary science."
    }
}

class AppPreferencesRepository(private val context: Context) {

    private object Keys {
        val NICKNAME = stringPreferencesKey("nickname")
        val PIN_CODE = stringPreferencesKey("pin_code")
        val TOTAL_STARS = intPreferencesKey("total_stars")
        val STARS_PER_BOX = intPreferencesKey("stars_per_box")
        val BOXES_OPENED_FROM_STARS = intPreferencesKey("boxes_opened_from_stars")
        val BIRTHDAY_BONUS_BOXES = intPreferencesKey("birthday_bonus_boxes")
        val LAST_BIRTHDAY_BOX_YEAR = intPreferencesKey("last_birthday_box_year")
        val BIRTHDAY_MESSAGE = stringPreferencesKey("birthday_message")
        val MEME_FEED_URL = stringPreferencesKey("meme_feed_url")
        val CUSTOM_QUIZ_URL = stringPreferencesKey("custom_quiz_url")
        val IS_MUTED = booleanPreferencesKey("is_muted")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LAST_MEME_SYNC = longPreferencesKey("last_meme_sync")
        val LAST_QUIZ_SYNC = longPreferencesKey("last_quiz_sync")
        val OPENTDB_TOKEN = stringPreferencesKey("opentdb_token")
    }

    val preferencesFlow: Flow<AppPreferencesState> = context.dataStore.data.map { prefs ->
        val themeStr = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val themeMode = runCatching { ThemeMode.valueOf(themeStr) }.getOrDefault(ThemeMode.SYSTEM)
        AppPreferencesState(
            nickname = prefs[Keys.NICKNAME]?.ifBlank { "Doc" } ?: "Doc",
            pinCode = prefs[Keys.PIN_CODE]?.ifBlank { "0306" } ?: "0306",
            totalStars = prefs[Keys.TOTAL_STARS] ?: 0,
            starsPerBox = (prefs[Keys.STARS_PER_BOX] ?: 50).coerceAtLeast(5),
            boxesOpenedFromStars = prefs[Keys.BOXES_OPENED_FROM_STARS] ?: 0,
            birthdayBonusBoxes = prefs[Keys.BIRTHDAY_BONUS_BOXES] ?: 0,
            lastBirthdayBoxYear = prefs[Keys.LAST_BIRTHDAY_BOX_YEAR] ?: 0,
            birthdayMessage = prefs[Keys.BIRTHDAY_MESSAGE]?.ifBlank {
                AppPreferencesState.DEFAULT_BIRTHDAY_MESSAGE
            } ?: AppPreferencesState.DEFAULT_BIRTHDAY_MESSAGE,
            memeFeedUrl = prefs[Keys.MEME_FEED_URL] ?: "",
            customQuizUrl = prefs[Keys.CUSTOM_QUIZ_URL] ?: "",
            isMuted = prefs[Keys.IS_MUTED] ?: false,
            themeMode = themeMode,
            lastMemeSyncMillis = prefs[Keys.LAST_MEME_SYNC] ?: 0L,
            lastQuizSyncMillis = prefs[Keys.LAST_QUIZ_SYNC] ?: 0L,
            openTdbToken = prefs[Keys.OPENTDB_TOKEN] ?: "",
        )
    }

    suspend fun addStars(delta: Int) {
        if (delta <= 0) return
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.TOTAL_STARS] ?: 0
            prefs[Keys.TOTAL_STARS] = current + delta
        }
    }

    suspend fun recordBoxOpened(useBirthdayBoxFirst: Boolean) {
        context.dataStore.edit { prefs ->
            val bonus = prefs[Keys.BIRTHDAY_BONUS_BOXES] ?: 0
            if (useBirthdayBoxFirst && bonus > 0) {
                prefs[Keys.BIRTHDAY_BONUS_BOXES] = bonus - 1
            } else {
                val opened = prefs[Keys.BOXES_OPENED_FROM_STARS] ?: 0
                prefs[Keys.BOXES_OPENED_FROM_STARS] = opened + 1
            }
        }
    }

    suspend fun grantBirthdayBoxForYearIfNeeded(year: Int): Boolean {
        var granted = false
        context.dataStore.edit { prefs ->
            val lastYear = prefs[Keys.LAST_BIRTHDAY_BOX_YEAR] ?: 0
            if (lastYear < year) {
                prefs[Keys.LAST_BIRTHDAY_BOX_YEAR] = year
                val bonus = prefs[Keys.BIRTHDAY_BONUS_BOXES] ?: 0
                prefs[Keys.BIRTHDAY_BONUS_BOXES] = bonus + 1
                granted = true
            }
        }
        return granted
    }

    suspend fun updateAdminSettings(
        nickname: String,
        pinCode: String,
        starsPerBox: Int,
        birthdayMessage: String,
        memeFeedUrl: String,
        customQuizUrl: String,
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NICKNAME] = nickname.trim().ifBlank { "Doc" }
            if (pinCode.trim().length >= 4) {
                prefs[Keys.PIN_CODE] = pinCode.trim()
            }
            prefs[Keys.STARS_PER_BOX] = starsPerBox.coerceIn(5, 1000)
            prefs[Keys.BIRTHDAY_MESSAGE] = birthdayMessage.trim().ifBlank {
                AppPreferencesState.DEFAULT_BIRTHDAY_MESSAGE
            }
            prefs[Keys.MEME_FEED_URL] = memeFeedUrl.trim()
            prefs[Keys.CUSTOM_QUIZ_URL] = customQuizUrl.trim()
        }
    }

    suspend fun setMuted(muted: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.IS_MUTED] = muted }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs -> prefs[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setLastMemeSyncMillis(timeMillis: Long) {
        context.dataStore.edit { prefs -> prefs[Keys.LAST_MEME_SYNC] = timeMillis }
    }

    suspend fun setLastQuizSyncMillis(timeMillis: Long) {
        context.dataStore.edit { prefs -> prefs[Keys.LAST_QUIZ_SYNC] = timeMillis }
    }

    suspend fun setOpenTdbToken(token: String) {
        context.dataStore.edit { prefs -> prefs[Keys.OPENTDB_TOKEN] = token }
    }
}
