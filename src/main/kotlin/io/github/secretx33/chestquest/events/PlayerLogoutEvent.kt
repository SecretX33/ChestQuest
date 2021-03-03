package io.github.secretx33.chestquest.events

import io.github.secretx33.chestquest.repository.ChestRepo
import io.github.secretx33.chestquest.utils.Utils
import io.github.secretx33.chestquest.utils.canEditQC
import io.github.secretx33.chestquest.utils.canOpenQC
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class PlayerLogoutEvent (plugin: Plugin, private val chestRepo: ChestRepo) : Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun onInventoryClose(event: PlayerQuitEvent) {
        val player = event.player

        if(player.canOpenQC() || player.canEditQC()) {
            chestRepo.removeEntriesOf(player.uniqueId)
            Utils.debugMessage("Player ${player.name} logged off, removing his entries from memory.")
        }
    }
}
