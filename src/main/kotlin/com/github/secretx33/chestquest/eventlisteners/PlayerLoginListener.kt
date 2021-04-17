package com.github.secretx33.chestquest.eventlisteners

import com.github.secretx33.chestquest.repository.ChestRepo
import com.github.secretx33.chestquest.repository.PlayerProgressRepo
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class PlayerLoginListener(plugin: Plugin, private val progressRepo: PlayerProgressRepo) : Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun PlayerJoinEvent.onPlayerJoin() {
        // cache player progress
        progressRepo.getPlayerProgress(player)
    }
}
