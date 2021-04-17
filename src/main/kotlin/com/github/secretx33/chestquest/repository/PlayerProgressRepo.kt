package com.github.secretx33.chestquest.repository

import com.github.secretx33.chestquest.database.SQLite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.koin.core.component.KoinApiExtension
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@KoinApiExtension
class PlayerProgressRepo(private val db: SQLite) {

    private val playerProgress: MutableMap<UUID, Int> = ConcurrentHashMap()

    fun removeEntryOf(player: Player) = playerProgress.remove(player.uniqueId)

    fun getPlayerProgress(player: Player): Int {
        return playerProgress.getOrPut(player.uniqueId) {
            db.getPlayerProgress(player.uniqueId) ?: 0.also { db.addPlayerProgress(player.uniqueId, it) }
        }
    }

    fun canOpenChest(player: Player, chestOrder: Int): Boolean {
        val progress = getPlayerProgress(player)

        if(chestOrder - 1 > progress)
            return false
        if(chestOrder == progress + 1) {
            playerProgress[player.uniqueId] = progress + 1
            db.updatePlayerProgress(player.uniqueId, progress + 1)
        }
        return true
    }

    fun clearPlayerProgress(player: Player) {
        playerProgress.remove(player.uniqueId)
        db.removePlayerProgress(player.uniqueId)
    }
}
