package com.github.secretx33.chestquest.events

import com.github.secretx33.chestquest.repository.ChestRepo
import com.github.secretx33.chestquest.utils.Utils.consoleMessage
import com.github.secretx33.chestquest.utils.coordinates
import com.github.secretx33.chestquest.utils.isQuestChest
import com.github.secretx33.chestquest.utils.message
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class BreakChestEvent(plugin: Plugin, private val chestRepo: ChestRepo) : Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun onInteract(event: BlockBreakEvent) {
        val player = event.player
        val block = event.block

        if(block.isQuestChest()){
            chestRepo.removeQuestChest(block.location)
            player.message("You broke a Quest Chest!")
            consoleMessage("Player ${player.name} broke a Quest Chest at ${block.coordinates()}")
        }
    }
}
