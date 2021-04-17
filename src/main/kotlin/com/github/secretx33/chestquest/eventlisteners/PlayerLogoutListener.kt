package com.github.secretx33.chestquest.eventlisteners

import com.github.secretx33.chestquest.repository.ChestRepo
import com.github.secretx33.chestquest.repository.PlayerProgressRepo
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class PlayerLogoutListener (plugin: Plugin, private val chestRepo: ChestRepo, private val progressRepo: PlayerProgressRepo) : Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun PlayerQuitEvent.onPlayerQuit() {
        chestRepo.removeEntriesOf(player.uniqueId)
        progressRepo.removeEntriesOf(player.uniqueId)
        println("Player ${player.name} logged off, removing his entries from memory.")
    }
}
