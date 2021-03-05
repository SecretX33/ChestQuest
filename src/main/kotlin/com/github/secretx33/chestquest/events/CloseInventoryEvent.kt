package com.github.secretx33.chestquest.events

import com.github.secretx33.chestquest.repository.ChestRepo
import com.github.secretx33.chestquest.utils.Utils.debugMessage
import com.github.secretx33.chestquest.utils.canEditQC
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class CloseInventoryEvent(plugin: Plugin, private val chestRepo: ChestRepo) : Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun onInventoryClose(event: InventoryCloseEvent) {
        val inv = event.inventory
        val player = event.player

        if(!player.canEditQC() && chestRepo.isChestInventory(inv)) {
            chestRepo.updateInventory(player.uniqueId, inv)
            debugMessage("Player ${player.name} closed his custom inventory, saving it into DB...")
        }
    }
}
