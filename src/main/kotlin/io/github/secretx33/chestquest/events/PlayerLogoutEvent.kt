package io.github.secretx33.chestquest.events

import io.github.secretx33.chestquest.repository.ChestRepo
import io.github.secretx33.chestquest.repository.PlayerProgressRepo
import io.github.secretx33.chestquest.utils.Utils.debugMessage
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class PlayerLogoutEvent (plugin: Plugin, private val chestRepo: ChestRepo, private val progressRepo: PlayerProgressRepo) : Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun onInventoryClose(event: PlayerQuitEvent) {
        val player = event.player
        chestRepo.removeEntriesOf(player.uniqueId)
        progressRepo.removeEntriesOf(player.uniqueId)
        debugMessage("Player ${player.name} logged off, removing his entries from memory.")
    }
}
