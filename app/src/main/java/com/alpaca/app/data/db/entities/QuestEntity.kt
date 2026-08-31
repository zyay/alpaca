package com.alpaca.app.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quests")
data class QuestEntity(
    @PrimaryKey val questId: String,
    val epochDay: Long,
    val type: String,
    val target: Int,
    val progress: Int = 0,
    val rewardGems: Int,
    val claimed: Boolean = false
) {
    val isComplete: Boolean get() = progress >= target

    companion object {
        const val TYPE_EARN_XP = "EARN_XP"
        const val TYPE_COMPLETE_LESSONS = "COMPLETE_LESSONS"
        const val TYPE_EARN_COINS = "EARN_COINS"
    }
}
