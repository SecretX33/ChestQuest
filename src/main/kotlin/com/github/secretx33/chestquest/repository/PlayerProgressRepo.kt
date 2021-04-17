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

    init { loadDataFromDB() }

    private fun loadDataFromDB() = CoroutineScope(Dispatchers.Default).launch {
        playerProgress.clear()
        playerProgress.putAll(db.getAllPlayerProgressAsync().await())
    }

    fun removeEntryOf(player: Player) = playerProgress.remove(player.uniqueId)

    fun getPlayerProgress(playerUuid: UUID): Int {
        return playerProgress.getOrPut(playerUuid) {
//            db.getPlayerProgress(playerUuid) ?: 0.also { db.addPlayerProgress(playerUuid, $it) }
            val dbEntry = db.getPlayerProgress(playerUuid)
            if(dbEntry != null){
                println("Got progress of ${Bukkit.getPlayer(playerUuid).name ?: playerUuid.toString()} from Database")
                dbEntry
            } else {
                println("Player ${Bukkit.getPlayer(playerUuid).name ?: playerUuid.toString()} had no progress before, setting it to zero")
                db.addPlayerProgress(playerUuid, 0)
                0
            }
        }
    }

    fun canOpenChest(playerUuid: UUID, chestOrder: Int): Boolean {
        val progress = getPlayerProgress(playerUuid)

        if(chestOrder - 1 > progress)
            return false
        if(chestOrder == progress + 1) {
            playerProgress[playerUuid] = progress + 1
            db.updatePlayerProgress(playerUuid, progress + 1)
        }
        return true
    }

    fun clearPlayerProgress(player: Player) {
        playerProgress.remove(player.uniqueId)
        db.removePlayerProgress(player.uniqueId)
    }
}
