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
        var freezes = user.streakFreezes
        val gap = if (user.lastPracticeEpochDay < 0) 0L else today - user.lastPracticeEpochDay
        val newStreak: Int = when {
            gap <= 0L -> user.streakDays
            gap == 1L -> user.streakDays + 1
            else -> {
                val missed = (gap - 1).toInt()
                if (user.streakDays > 0 && freezes >= missed) {
                    freezes -= missed
                    user.streakDays + 1
                } else {
                    if (user.streakDays > 0) freezes = 0
                    1
                }
            }
        }
        userDao.upsert(
            user.copy(
                xp = user.xp + xp,
                coins = user.coins + coins,
                streakDays = newStreak.coerceAtLeast(1),
                lastPracticeEpochDay = today,
                streakFreezes = freezes
            )
        )
        return LessonReward(xp, coins, newStreak > user.streakDays, newStreak)
    }

    suspend fun addGems(amount: Int) {
        if (amount <= 0) return
        val user = currentUser()
        userDao.upsert(user.copy(gems = user.gems + amount))
    }

    /** Spends gems; returns false (and spends nothing) when the balance is short. */
    suspend fun trySpendGems(amount: Int): Boolean {
        val user = currentUser()
        if (user.gems < amount) return false
        userDao.upsert(user.copy(gems = user.gems - amount))
        return true
    }

    suspend fun refillEnergy(): Boolean {
        val user = currentUser()
        if (user.fleeceEnergy >= UserEntity.MAX_ENERGY) return false
        userDao.upsert(user.copy(fleeceEnergy = UserEntity.MAX_ENERGY, energyAnchorEpochMs = 0L))
        return true
    }

    suspend fun buyStreakFreeze(): Boolean {
        val user = currentUser()
        if (user.streakFreezes >= UserEntity.MAX_FREEZES) return false
        if (!trySpendGems(UserEntity.FREEZE_PRICE_GEMS)) return false
        userDao.upsert(currentUser().copy(streakFreezes = user.streakFreezes + 1))
        return true
    }

    suspend fun buyEnergyRefill(): Boolean {
        if (!trySpendGems(UserEntity.REFILL_PRICE_GEMS)) return false
        return refillEnergy()
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
