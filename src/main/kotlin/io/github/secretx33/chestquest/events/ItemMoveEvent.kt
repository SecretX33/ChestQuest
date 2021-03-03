package io.github.secretx33.chestquest.events

import io.github.secretx33.chestquest.repository.ChestRepo
import io.github.secretx33.chestquest.utils.Utils.debugMessage
import io.github.secretx33.chestquest.utils.isChest
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.*
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class ItemMoveEvent(plugin: Plugin, private val chestRepo: ChestRepo) : Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun onItemMove(event: InventoryDragEvent){
        debugMessage("0. Who clicked is ${event.whoClicked.name}, inventory type is ${event.inventory.type}")

        val inv = event.inventory
        if(inv.isChest() && chestRepo.isVirtualInventory(inv)){
            event.isCancelled = true
            debugMessage("0. Canceled DragEvent")
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun onItemMove(event: InventoryClickEvent) {
        debugMessage("2. ${event.whoClicked.name} clicked on [${event.action?.name}] CI ${event.clickedInventory?.type?.name} (I ${event.inventory?.type?.name}) at ${event.clickedInventory?.location != null}")
        if (event.isPickAction()) debugMessage("3. Is pickup")
        if (event.isPutAction()) debugMessage("3. Is put action")

        val inv = event.inventory
        if (event.isPutAction() && inv.isChest() && chestRepo.isVirtualInventory(inv)){
            event.isCancelled = true
            debugMessage("4. Canceling put action")
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun onItemClickMonitor(event: InventoryClickEvent) {
        val inv = event.inventory
        val player = event.whoClicked as Player

        if(event.isPickAction() && inv.isChest() && chestRepo.isChestInventory(inv)){
            debugMessage("5. Registered pickup actions, saving to prevent exploits")
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

    private fun InventoryClickEvent.isPutAction(): Boolean {
        // shift click on item in player inventory
        if(action == InventoryAction.MOVE_TO_OTHER_INVENTORY && clickedInventory.type == InventoryType.PLAYER && clickedInventory.type != inventory.type)
            return true

        if(action.isPlace() && clickedInventory.type != InventoryType.PLAYER && inventory.type != InventoryType.PLAYER)
            return true

        return false
    }

    private fun InventoryAction.isPickUp() = this == InventoryAction.PICKUP_ALL || this == InventoryAction.PICKUP_HALF || this == InventoryAction.PICKUP_SOME || this == InventoryAction.PICKUP_ONE

    private fun InventoryAction.isPlace() = this == InventoryAction.PLACE_ALL || this == InventoryAction.PLACE_SOME || this == InventoryAction.PLACE_ONE
}
