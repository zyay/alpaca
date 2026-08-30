package com.alpaca.app.data.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CourseUnit(
    val unitId: String,
    val title: String,
    val region: String,
    val languageName: String = "Spanish",
    val lessons: List<Lesson>
)

@Serializable
data class Lesson(
    val lessonId: String,
    val title: String,
    val exercises: List<Exercise>
)

/**
 * Exercise contract shared by the bundled JSON content and every exercise UI.
 * The discriminator field in JSON is "type".
 */
@Serializable
sealed class Exercise {
    abstract val explanation: String?
}

@Serializable
@SerialName("multiple_choice")
data class MultipleChoiceExercise(
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    override val explanation: String? = null
) : Exercise()

@Serializable
@SerialName("match_pairs")
data class MatchPairsExercise(
    // Each pair: [spanish, english]
    val pairs: List<List<String>>,
    override val explanation: String? = null
) : Exercise()

@Serializable
@SerialName("fill_blank")
data class FillBlankExercise(
    val sentence: String, // contains "___"
    val answer: String,
    val wordBank: List<String>,
    override val explanation: String? = null
) : Exercise()

@Serializable
@SerialName("listening")
data class ListeningExercise(
    val text: String, // spoken via TTS
    val options: List<String>, // translations to choose from
    val correctIndex: Int,
    override val explanation: String? = null
) : Exercise()

@Serializable
@SerialName("pronunciation")
data class PronunciationExercise(
    val expected: String,
    val translation: String,
    val tolerance: Float = 0.55f,
    override val explanation: String? = null
) : Exercise()
