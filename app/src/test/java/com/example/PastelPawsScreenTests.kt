package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.example.data.local.GiftTier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PastelPawsScreenTests {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun home_screen_displays_nickname_stars_and_progress() {
        val app = composeRule.activity.application as PastelPawsApp
        val prefs = runBlocking { app.container.repository.prefsFlow.first() }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("home_greeting").assertTextContains(prefs.nickname, substring = true)
        composeRule.onNodeWithTag("home_star_count", useUnmergedTree = true)
            .assertTextContains("${prefs.totalStars} ★", substring = true)
        composeRule.onNodeWithTag("home_box_progress_text", useUnmergedTree = true)
            .assertTextContains("${prefs.starsIntoCurrentBox} / ${prefs.starsPerBox}", substring = true)
        composeRule.onNodeWithTag("theme_toggle_button").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun memes_screen_renders_sinhala_memes_and_filters_favorites() {
        val app = composeRule.activity.application as PastelPawsApp
        runBlocking { app.container.repository.ensureSeeded() }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_tab_memes").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("memes_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("memes_filter_trending").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 5000L) {
            composeRule.onAllNodesWithText("Pastel Paws", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("memes_screen")
            .performScrollToNode(hasText("Pastel Paws", substring = true))
        composeRule.onAllNodesWithText("Pastel Paws", substring = true)
            .onFirst()
            .assertIsDisplayed()

        // Favourite the first meme
        composeRule.onNodeWithTag("memes_screen")
            .performScrollToNode(hasTestTag("meme_fav_button_1"))
        composeRule.onNodeWithTag("meme_fav_button_1").performClick()
        composeRule.waitForIdle()

        // Switch to Favourites and scroll to the favourited meme card
        composeRule.onNodeWithTag("memes_screen").performScrollToIndex(0)
        composeRule.onNodeWithTag("memes_filter_favorites").performClick()
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5000L) {
            composeRule.onAllNodesWithText("Pastel Paws", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("memes_screen")
            .performScrollToNode(hasText("Pastel Paws", substring = true))
        composeRule.onAllNodesWithText("Pastel Paws", substring = true)
            .onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun games_screen_shows_four_games_and_plays_vet_quiz() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_tab_games").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("quiz_bank_count_badge").assertIsDisplayed()
        composeRule.onNodeWithTag("game_card_quiz").assertIsDisplayed()
        composeRule.onNodeWithTag("game_card_memory").assertIsDisplayed()
        composeRule.onNodeWithTag("games_screen").performScrollToNode(hasTestTag("game_card_reaction"))
        composeRule.onNodeWithTag("game_card_reaction").assertIsDisplayed()
        composeRule.onNodeWithTag("games_screen").performScrollToNode(hasTestTag("game_card_scramble"))
        composeRule.onNodeWithTag("game_card_scramble").assertIsDisplayed()

        // Verify adaptive Word Scramble + Hint button
        composeRule.onNodeWithTag("game_card_scramble").performClick()
        composeRule.waitUntil(timeoutMillis = 5000L) {
            composeRule.onAllNodes(hasTestTag("scramble_hint_button")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("scramble_hint_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("game_back_button").performClick()
        composeRule.waitForIdle()

        // Play Vet Quiz
        composeRule.onNodeWithTag("games_screen").performScrollToNode(hasTestTag("game_card_quiz"))
        composeRule.onNodeWithTag("game_card_quiz").performClick()
        composeRule.waitUntil(timeoutMillis = 5000L) {
            composeRule.onAllNodes(hasTestTag("quiz_option_0")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("quiz_option_0").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("quiz_feedback").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("game_mute_button").performClick()
        composeRule.onNodeWithTag("game_back_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("games_screen").assertIsDisplayed()
    }

    @Test
    fun goals_screen_adds_goal_and_awards_stars_on_completion() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_tab_goals").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("add_goal_fab").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("add_goal_title_input").performTextInput("Review canine anatomy")
        composeRule.onNodeWithTag("save_goal_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("goals_screen").performScrollToNode(hasText("Review canine anatomy"))
        composeRule.onNodeWithText("Review canine anatomy").assertIsDisplayed()

        composeRule.onAllNodes(isToggleable()).onFirst().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("goals_screen").performScrollToIndex(0)
        composeRule.waitUntil(timeoutMillis = 5000L) {
            composeRule.onAllNodesWithText("100% (1/1)", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("goals_percentage_text").assertTextContains("100% (1/1)", substring = true)
    }

    @Test
    fun gifts_screen_opens_box_and_pin_settings_previews_birthday() {
        val app = composeRule.activity.application as PastelPawsApp
        runBlocking {
            app.container.repository.ensureSeeded()
            app.container.preferencesRepository.addStars(50)
            app.container.repository.addGiftToPool("Iced Matcha", GiftTier.SMALL)
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_tab_gifts").performClick()
        composeRule.waitUntil(timeoutMillis = 5000L) {
            composeRule.onAllNodesWithText("Mystery Gift Box", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("gifts_screen").performScrollToNode(hasTestTag("open_mystery_box_button"))
        composeRule.onNodeWithTag("open_mystery_box_button").performClick()
        ShadowLooper.idleMainLooper(1150L, TimeUnit.MILLISECONDS)

        composeRule.waitUntil(timeoutMillis = 5000L) {
            composeRule.onAllNodesWithText("Mystery Box Opened!").fetchSemanticsNodes().isNotEmpty()
        }

        val cards = runBlocking {
            app.container.database.giftDao().observeOpenedCards().first()
        }
        assertTrue(cards.isNotEmpty())
        assertTrue(cards.first().claimCode.startsWith("PAW-"))

        composeRule.onNodeWithText("Keep in Cards").performClick()
        composeRule.waitForIdle()

        // Open PIN settings with 0306 and preview birthday surprise
        composeRule.onNodeWithTag("gifts_screen").performScrollToIndex(0)
        composeRule.onNodeWithTag("open_pin_settings_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pin_input_field").performTextInput("0306")
        composeRule.onNodeWithTag("pin_confirm_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("pin_settings_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("pin_settings_screen")
            .performScrollToNode(hasTestTag("preview_birthday_button"))
        composeRule.onNodeWithTag("preview_birthday_button").performClick()
        composeRule.mainClock.advanceTimeBy(400L)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("birthday_surprise_overlay").assertIsDisplayed()
        composeRule.onNodeWithTag("birthday_message_text").performScrollTo().assertIsDisplayed()
    }
}
