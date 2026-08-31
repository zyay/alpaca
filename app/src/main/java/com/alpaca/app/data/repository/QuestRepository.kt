package com.alpaca.app.data.repository

import com.alpaca.app.data.db.AlpacaDatabase
import com.alpaca.app.data.db.QuestDao
import com.alpaca.app.data.db.entities.QuestEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import java.time.LocalDate

data class QuestSpec(
    val questId: String,
    val type: String,
    val title: String,
    val unit: String,
    val targets: List<Int>,
    val rewards: List<Int>
)

/**
 * Daily quests, deterministic per calendar day so every install sees the same set.
 */
class QuestRepository(private val db: AlpacaDatabase) {
    private val dao: QuestDao = db.questDao()

    companion object {
        val specs = listOf(
            QuestSpec(
                "q_earn_xp", QuestEntity.TYPE_EARN_XP,
                "Practice makes perfect", "XP",
                targets = listOf(20, 30, 40), rewards = listOf(10, 12, 15)
            ),
            QuestSpec(
                "q_complete_lessons", QuestEntity.TYPE_COMPLETE_LESSONS,
                "Warm-up routine", "lessons",
                targets = listOf(2, 3, 4), rewards = listOf(8, 12, 15)
            ),
            QuestSpec(
                "q_earn_coins", QuestEntity.TYPE_EARN_COINS,
                "Coin collector", "coins",
                targets = listOf(10, 15, 20), rewards = listOf(8, 10, 12)
            )
        )
    }

    fun observeQuests(): Flow<List<QuestEntity>> = flow {
        val today = LocalDate.now().toEpochDay()
        ensureToday(today)
        emitAll(dao.observeForDay(today))
    }

    suspend fun todayQuests(): List<QuestEntity> {
        val today = LocalDate.now().toEpochDay()
        ensureToday(today)
        return dao.getForDay(today)
    }

    private suspend fun ensureToday(day: Long) {
        val existing = dao.getForDay(day)
        if (existing.size < specs.size) {
            dao.deleteBefore(day)
            val fresh = specs.mapIndexed { i, spec ->
                val tier = ((day + i) % spec.targets.size).toInt()
                QuestEntity(
                    questId = spec.questId,
                    epochDay = day,
                    type = spec.type,
                    target = spec.targets[tier],
                    rewardGems = spec.rewards[tier]
                )
            }
            // Keep any progress already earned today for quests that exist.
            dao.upsertAll(fresh.map { q ->
                existing.firstOrNull { it.questId == q.questId } ?: q
            })
        }
    }

    /** Bump XP / lessons / coins quests after a completed lesson. */
    suspend fun recordLesson(xp: Int, coins: Int, lessons: Int = 1) {
        val today = LocalDate.now().toEpochDay()
        dao.addProgress("q_earn_xp", today, xp)
        dao.addProgress("q_complete_lessons", today, lessons)
        dao.addProgress("q_earn_coins", today, coins)
    }

    /** Marks the quest claimed; returns gems awarded, or 0 when not claimable. */
    suspend fun claim(questId: String): Int {
        val today = LocalDate.now().toEpochDay()
        val quest = dao.getForDay(today).firstOrNull { it.questId == questId } ?: return 0
        if (!quest.isComplete || quest.claimed) return 0
        dao.markClaimed(questId, today)
        return quest.rewardGems
    }

    fun titleOf(quest: QuestEntity): String =
        specs.firstOrNull { it.questId == quest.questId }?.title ?: quest.type
}
