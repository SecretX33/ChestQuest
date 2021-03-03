package io.github.secretx33.chestquest.events

import io.github.secretx33.chestquest.repository.ChestRepo
import io.github.secretx33.chestquest.utils.Utils.debugMessage
import io.github.secretx33.chestquest.utils.canEditQC
import io.github.secretx33.chestquest.utils.canOpenQC
import io.github.secretx33.chestquest.utils.isChest
import org.bukkit.Bukkit
import org.bukkit.block.Chest
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class OpenChestEvent(plugin: Plugin, private val chestRepo: ChestRepo) : Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private fun onInteract(event: PlayerInteractEvent) {
        if(!event.isChestQuest()) return

        val chest = event.clickedBlock?.state as Chest
        val player = event.player

        if(!player.canOpenQC()) {
            event.isCancelled = true
            player.openInventory(chestRepo.getTempChestContent(chest, player))
            debugMessage("Player ${player.name} doesn't have permission to open any Quest Chest, displaying an empty chest to him")
        } else if(!player.canEditQC()) {
            event.isCancelled = true
            player.openInventory(chestRepo.getChestContent(chest, player))
            debugMessage("Altered interaction with chest")
        }
    }

    private fun PlayerInteractEvent.isChestQuest(): Boolean {
        return action == Action.RIGHT_CLICK_BLOCK && clickedBlock?.isChest() == true && chestRepo.isQuestChest(clickedBlock.location)
    }
}
