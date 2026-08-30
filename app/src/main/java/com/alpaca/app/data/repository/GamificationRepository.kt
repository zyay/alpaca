package com.alpaca.app.data.repository

import com.alpaca.app.data.db.AlpacaDatabase
import com.alpaca.app.data.db.entities.UserEntity
import com.alpaca.app.data.db.entities.UserEntity.Companion.MAX_ENERGY
import com.alpaca.app.data.db.entities.UserEntity.Companion.REGEN_MS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

data class LessonReward(
    val xpGained: Int,
    val coinsGained: Int,
    val streakIncreased: Boolean,
    val newStreak: Int
)

class GamificationRepository(private val db: AlpacaDatabase) {
    private val userDao = db.userDao()

    fun observeUser(): Flow<UserEntity> =
        userDao.observeUser().map { row ->
            (row ?: UserEntity()).withRegen(System.currentTimeMillis())
        }

    suspend fun currentUser(): UserEntity {
        val row = userDao.getUser() ?: UserEntity().also { userDao.upsert(it) }
        val regen = row.withRegen(System.currentTimeMillis())
        if (regen != row) userDao.upsert(regen)
        return regen
    }

    /** Returns remaining energy, or -1 when there is none left. */
    suspend fun consumeEnergy(): Int {
        val user = currentUser()
        if (user.fleeceEnergy <= 0) return -1
        val now = System.currentTimeMillis()
        val updated = user.copy(
            fleeceEnergy = user.fleeceEnergy - 1,
            energyAnchorEpochMs = if (user.energyAnchorEpochMs == 0L) now else user.energyAnchorEpochMs
        )
        userDao.upsert(updated)
        return updated.fleeceEnergy
    }

    suspend fun awardLesson(xp: Int, coins: Int): LessonReward {
        val user = currentUser()
        val today = LocalDate.now().toEpochDay()
        val newStreak = when {
            user.lastPracticeEpochDay == today -> user.streakDays
            user.lastPracticeEpochDay == today - 1 -> user.streakDays + 1
            else -> 1
        }
        userDao.upsert(
            user.copy(
                xp = user.xp + xp,
                coins = user.coins + coins,
                streakDays = newStreak,
                lastPracticeEpochDay = today
            )
        )
        return LessonReward(xp, coins, newStreak > user.streakDays, newStreak)
    }

    private fun UserEntity.withRegen(now: Long): UserEntity {
        if (fleeceEnergy >= MAX_ENERGY || energyAnchorEpochMs == 0L) return this
        val gained = ((now - energyAnchorEpochMs) / REGEN_MS).toInt()
        if (gained <= 0) return this
        val newEnergy = minOf(MAX_ENERGY, fleeceEnergy + gained)
        val newAnchor = if (newEnergy >= MAX_ENERGY) 0L else energyAnchorEpochMs + gained * REGEN_MS
        return copy(fleeceEnergy = newEnergy, energyAnchorEpochMs = newAnchor)
    }
}
