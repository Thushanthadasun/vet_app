package com.example

import com.example.data.local.SeedData
import com.example.data.local.VetQuestionGenerator
import com.example.data.preferences.AppPreferencesState
import com.example.util.DateHelpers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class PastelPawsLogicUnitTest {

    @Test
    fun preferences_box_progress_math_is_accurate() {
        val state = AppPreferencesState(
            totalStars = 120,
            starsPerBox = 50,
            boxesOpenedFromStars = 1,
            birthdayBonusBoxes = 1,
        )
        assertEquals(2, state.earnedBoxesFromStars)
        assertEquals(1, state.unopenedFromStars)
        assertEquals(2, state.totalUnopenedBoxes)
        assertEquals(20, state.starsIntoCurrentBox)
        assertEquals(0.4f, state.progressFraction, 0.001f)
    }

    @Test
    fun next_march_6_alarm_is_always_in_future_and_on_march_6() {
        val sampleNow = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 16, 19, 30, 0)
        }.timeInMillis
        val nextMillis = DateHelpers.nextMarch6MorningMillis(sampleNow)
        assertTrue(nextMillis > sampleNow)
        val cal = Calendar.getInstance().apply { timeInMillis = nextMillis }
        assertEquals(Calendar.MARCH, cal.get(Calendar.MONTH))
        assertEquals(6, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(2027, cal.get(Calendar.YEAR))
    }

    @Test
    fun seed_data_has_valid_quiz_scramble_and_sinhala_memes() {
        val memes = SeedData.starterMemes()
        assertEquals(14, memes.size)
        assertTrue(memes.all { it.sourceName.isNotBlank() && it.title.isNotBlank() })

        val quiz = SeedData.starterQuizQuestions()
        assertEquals(24, quiz.size)
        assertTrue(quiz.all { it.correctIndex in 0..3 })

        val words = SeedData.starterScrambleWords()
        assertTrue(words.size >= 45)
        assertTrue(words.any { it.tier == 1 } && words.any { it.tier == 2 } && words.any { it.tier == 3 })
        assertTrue(words.all { it.word.length >= 4 && it.clue.isNotBlank() })
    }

    @Test
    fun vet_question_generator_produces_valid_unique_questions() {
        val batch = VetQuestionGenerator.generateBatch(10)
        assertEquals(10, batch.size)
        assertTrue(
            batch.all { q ->
                val options = listOf(q.optionA, q.optionB, q.optionC, q.optionD)
                q.question.isNotBlank() &&
                    q.correctIndex in 0..3 &&
                    options.distinct().size == 4
            }
        )
    }
}
