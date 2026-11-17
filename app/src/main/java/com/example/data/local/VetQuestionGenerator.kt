package com.example.data.local

import kotlin.random.Random

/**
 * Generates endless, accurate Veterinary Science clinical reference questions offline
 * (normal vital signs, gestation lengths, species terminology, and toxicities) with randomized
 * distractors so quiz sessions never run dry even without an internet connection.
 */
object VetQuestionGenerator {

    private data class SpeciesVitalProfile(
        val species: String,
        val tempRangeC: String,
        val heartRateBpm: String,
        val respRateBpm: String,
        val gestationDays: String,
    )

    private val profiles = listOf(
        SpeciesVitalProfile("Adult Dog", "38.0–39.2 °C", "60–140 bpm", "10–30 breaths/min", "63 days"),
        SpeciesVitalProfile("Adult Cat", "38.1–39.2 °C", "140–220 bpm", "20–40 breaths/min", "63–65 days"),
        SpeciesVitalProfile("Adult Horse", "37.2–38.3 °C", "28–44 bpm", "8–16 breaths/min", "335–342 days"),
        SpeciesVitalProfile("Dairy Cow", "38.0–39.3 °C", "48–84 bpm", "26–50 breaths/min", "280–285 days"),
        SpeciesVitalProfile("Adult Goat", "38.5–39.7 °C", "70–90 bpm", "12–20 breaths/min", "150 days"),
        SpeciesVitalProfile("Adult Rabbit", "38.5–40.0 °C", "130–325 bpm", "30–60 breaths/min", "31 days"),
    )

    private val toxicPairings = listOf(
        Triple("Grapes and raisins", "Dogs", "Acute renal failure"),
        Triple("Easter lilies (Lilium spp.)", "Cats", "Severe nephrotoxicity / acute kidney injury"),
        Triple("Xylitol (artificial sweetener)", "Dogs", "Profound hypoglycemia and hepatic necrosis"),
        Triple("Permethrin spot-on products", "Cats", "Severe tremors and seizures"),
        Triple("Onion and garlic (Allium spp.)", "Dogs and Cats", "Heinz body hemolytic anemia"),
        Triple("Toad venom (Bufo spp.)", "Dogs", "Cardiac arrhythmias and hypersalivation"),
    )

    fun generateBatch(count: Int): List<QuizQuestionEntity> {
        val generated = mutableListOf<QuizQuestionEntity>()
        val templates = listOf(0, 1, 2, 3, 4)

        repeat(count) { idx ->
            val template = templates[(idx + Random.nextInt(templates.size)) % templates.size]
            val q = when (template) {
                0 -> buildHeartRateQuestion()
                1 -> buildRespRateQuestion()
                2 -> buildGestationQuestion()
                3 -> buildTemperatureQuestion()
                else -> buildToxicologyQuestion()
            }
            generated += q
        }
        return generated
    }

    private fun buildHeartRateQuestion(): QuizQuestionEntity {
        val target = profiles.random()
        val distractors = profiles.filter { it.heartRateBpm != target.heartRateBpm }
            .shuffled()
            .take(3)
            .map { it.heartRateBpm }
        return assemble(
            question = "Clinical Vitals: What is the normal resting heart rate range for a healthy ${target.species}?",
            correct = target.heartRateBpm,
            distractors = distractors,
            source = "Smart Vet Generator",
            difficulty = "medium",
        )
    }

    private fun buildRespRateQuestion(): QuizQuestionEntity {
        val target = profiles.random()
        val distractors = profiles.filter { it.respRateBpm != target.respRateBpm }
            .shuffled()
            .take(3)
            .map { it.respRateBpm }
        return assemble(
            question = "Clinical Vitals: What is the normal resting respiratory rate for a ${target.species}?",
            correct = target.respRateBpm,
            distractors = distractors,
            source = "Smart Vet Generator",
            difficulty = "medium",
        )
    }

    private fun buildGestationQuestion(): QuizQuestionEntity {
        val target = profiles.random()
        val distractors = profiles.filter { it.gestationDays != target.gestationDays }
            .shuffled()
            .take(3)
            .map { it.gestationDays }
        return assemble(
            question = "Theriogenology: Approximately how long is the normal gestation period of a ${target.species}?",
            correct = target.gestationDays,
            distractors = distractors,
            source = "Smart Vet Generator",
            difficulty = "easy",
        )
    }

    private fun buildTemperatureQuestion(): QuizQuestionEntity {
        val target = profiles.random()
        val pool = listOf("36.2–37.0 °C", "37.2–38.3 °C", "38.0–39.2 °C", "38.5–40.0 °C", "40.8–42.0 °C")
        val distractors = pool.filter { it != target.tempRangeC }.shuffled().take(3)
        return assemble(
            question = "Clinical Exam: Which normal rectal temperature range corresponds to a healthy ${target.species}?",
            correct = target.tempRangeC,
            distractors = distractors,
            source = "Smart Vet Generator",
            difficulty = "medium",
        )
    }

    private fun buildToxicologyQuestion(): QuizQuestionEntity {
        val target = toxicPairings.random()
        val distractors = toxicPairings.filter { it.third != target.third }
            .shuffled()
            .take(3)
            .map { it.third }
        return assemble(
            question = "Vet Toxicology: Ingestion of ${target.first} in ${target.second} is classically associated with:",
            correct = target.third,
            distractors = distractors,
            source = "Smart Vet Generator",
            difficulty = "hard",
        )
    }

    private fun assemble(
        question: String,
        correct: String,
        distractors: List<String>,
        source: String,
        difficulty: String,
    ): QuizQuestionEntity {
        val options = (distractors + correct).shuffled()
        val idx = options.indexOf(correct).coerceAtLeast(0)
        return QuizQuestionEntity(
            id = -Random.nextLong(100_000L, 999_999L),
            question = question,
            optionA = options[0],
            optionB = options[1],
            optionC = options[2],
            optionD = options[3],
            correctIndex = idx,
            sourceLabel = source,
            isBuiltIn = true,
            difficulty = difficulty,
        )
    }
}
