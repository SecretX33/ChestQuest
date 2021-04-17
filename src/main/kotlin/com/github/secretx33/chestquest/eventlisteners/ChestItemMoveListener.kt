package com.github.secretx33.chestquest.eventlisteners

import com.github.secretx33.chestquest.repository.ChestRepo
import com.github.secretx33.chestquest.utils.isChest
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension
import java.util.logging.Logger

@KoinApiExtension
class ChestItemMoveListener(plugin: Plugin, private val chestRepo: ChestRepo, private val log: Logger) : Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun InventoryDragEvent.onItemDrag() {
        val topInvSize = view.topInventory.size
        val editingTopInv = rawSlots.any { it < topInvSize }

        if(editingTopInv && inventory.isChest() && chestRepo.isChestInventory(inventory)){
            isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun InventoryClickEvent.onItemPut() {
        if(clickedInventory == null) return

        // cancel any try of putting or swapping items inside a quest chest inventory
        if(isPutAction() && inventory.isChest() && chestRepo.isChestInventory(inventory)){
            isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun InventoryClickEvent.onItemPickUp() {
        if(clickedInventory == null) return
        val player = whoClicked as? Player ?: return

        // if an item is picked up from the chest, save new inventory on database
        if(isPickAction() && inventory.isChest() && chestRepo.isChestInventory(inventory)){
            chestRepo.updateInventory(player.uniqueId, inventory)
        }
    }

    private fun InventoryClickEvent.isPickAction(): Boolean {
        // shift click on item in chest inventory (or another container)
        if(action == InventoryAction.MOVE_TO_OTHER_INVENTORY && clickedInventory.type != InventoryType.PLAYER && inventory.type != InventoryType.PLAYER)
            return true

        if(action.isPickUp() && clickedInventory.type != InventoryType.PLAYER && inventory.type != InventoryType.PLAYER)
            return true

        return false
    }

    private fun InventoryAction.isPickUp() = this == InventoryAction.PICKUP_ALL || this == InventoryAction.PICKUP_HALF || this == InventoryAction.PICKUP_SOME || this == InventoryAction.PICKUP_ONE

//    private fun InventoryClickEvent.isSwapInside(): Boolean = action == InventoryAction.SWAP_WITH_CURSOR && clickedInventory.type != InventoryType.PLAYER && inventory.type != InventoryType.PLAYER

    private fun InventoryClickEvent.isPutAction(): Boolean {
        // shift click on item in player inventory
        if(action == InventoryAction.MOVE_TO_OTHER_INVENTORY && clickedInventory.type == InventoryType.PLAYER && clickedInventory.type != inventory.type)
            return true

        if(action.isPut() && clickedInventory.type != InventoryType.PLAYER && inventory.type != InventoryType.PLAYER)
            return true

        return false
    }

    private fun InventoryAction.isPut() = this == InventoryAction.PLACE_ALL || this == InventoryAction.PLACE_SOME || this == InventoryAction.PLACE_ONE || this == InventoryAction.SWAP_WITH_CURSOR
}
