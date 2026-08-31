package com.alpaca.app.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey val id: Int = 1,
    val xp: Int = 0,
    val coins: Int = 0,
    val gems: Int = 0,
    val streakDays: Int = 0,
    val lastPracticeEpochDay: Long = -1L,
    val fleeceEnergy: Int = MAX_ENERGY,
    val energyAnchorEpochMs: Long = 0L,
    val streakFreezes: Int = 0
) {
    companion object {
        const val MAX_ENERGY = 5
        const val REGEN_MS = 30L * 60L * 1000L // one tuft per 30 minutes
        const val MAX_FREEZES = 2
        const val FREEZE_PRICE_GEMS = 200
        const val REFILL_PRICE_GEMS = 350
    }
}
