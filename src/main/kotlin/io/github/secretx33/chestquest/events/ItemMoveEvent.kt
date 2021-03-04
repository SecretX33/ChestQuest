package io.github.secretx33.chestquest.events

import io.github.secretx33.chestquest.repository.ChestRepo
import io.github.secretx33.chestquest.utils.Utils.debugMessage
import io.github.secretx33.chestquest.utils.isChest
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

@KoinApiExtension
class ItemMoveEvent(plugin: Plugin, private val chestRepo: ChestRepo) : Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun onItemDrag(event: InventoryDragEvent){
        val inv = event.inventory
        val topInvSize = event.view.topInventory.size
        val editingTopInv = event.rawSlots.toSet().any { it < topInvSize }

        if(editingTopInv && inv.isChest() && chestRepo.isVirtualInventory(inv)){
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun onItemMove(event: InventoryClickEvent) {
        if(event.clickedInventory == null) return
        /*debugMessage("1. ${event.whoClicked.name} clicked on [${event.action?.name}] CI ${event.clickedInventory.type?.name} (I ${event.inventory?.type?.name}) at ${event.clickedInventory?.location != null}")
        if (event.isPickAction()) debugMessage("1. Is pickup")
        if (event.isPutAction()) debugMessage("1. Is put action")*/

        val inv = event.inventory
        if (event.isPutAction() && inv.isChest() && chestRepo.isVirtualInventory(inv)){
            event.isCancelled = true
            debugMessage("1. Canceling put action")
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun onItemClickMonitor(event: InventoryClickEvent) {
        if(event.clickedInventory == null) return
        val inv = event.inventory
        val player = event.whoClicked as Player

        if(event.isPickAction() && inv.isChest() && chestRepo.isChestInventory(inv)){
            debugMessage("2. Registered pickup actions, saving to prevent exploits")
            chestRepo.updateInventory(player.uniqueId, inv)
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

    private fun InventoryClickEvent.isSwapInside(): Boolean = action == InventoryAction.SWAP_WITH_CURSOR && clickedInventory.type != InventoryType.PLAYER && inventory.type != InventoryType.PLAYER


    private fun InventoryClickEvent.isPutAction(): Boolean {
        // shift click on item in player inventory
        if(action == InventoryAction.MOVE_TO_OTHER_INVENTORY && clickedInventory.type == InventoryType.PLAYER && clickedInventory.type != inventory.type)
            return true

        if(action.isPlace() && clickedInventory.type != InventoryType.PLAYER && inventory.type != InventoryType.PLAYER)
            return true

        return false
    }

    private fun InventoryAction.isPickUp() = this == InventoryAction.PICKUP_ALL || this == InventoryAction.PICKUP_HALF || this == InventoryAction.PICKUP_SOME || this == InventoryAction.PICKUP_ONE

    private fun InventoryAction.isPlace() = this == InventoryAction.PLACE_ALL || this == InventoryAction.PLACE_SOME || this == InventoryAction.PLACE_ONE || this == InventoryAction.SWAP_WITH_CURSOR
}
