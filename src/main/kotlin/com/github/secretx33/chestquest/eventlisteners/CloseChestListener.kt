package com.github.secretx33.chestquest.eventlisteners

import com.comphenix.protocol.wrappers.BlockPosition
import com.github.secretx33.chestquest.packets.WrapperPlayServerBlockAction
import com.github.secretx33.chestquest.repository.ChestRepo
import com.github.secretx33.chestquest.utils.canEditQC
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Chest
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension
import java.util.logging.Logger

@KoinApiExtension
class CloseChestListener(plugin: Plugin, private val chestRepo: ChestRepo, private val log: Logger) : Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun InventoryCloseEvent.onInventoryClose() {
        if(player !is Player || player.canEditQC() || !chestRepo.isChestInventory(inventory)) return

        // update inventory
        chestRepo.updateInventory(player.uniqueId, inventory)
        (inventory.holder as? Chest)?.let { (player as Player).simulateChestClose(it) }
        log.fine("Player ${player.name} closed his custom inventory, saving it into DB.")
    }

    private fun Player.simulateChestClose(chest: Chest){
        playSound(chest.location, Sound.BLOCK_CHEST_CLOSE, 0.5f, 1f)
        sendCloseChestPacket(chest)
    }

    private fun Player.sendCloseChestPacket(chest: Chest){
        if(!Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) return

        val wrapper = WrapperPlayServerBlockAction()
        wrapper.apply {
            blockType = Material.CHEST
            location = BlockPosition(chest.location.toVector())
            byte1 = 1   // update number of people with chest open action ID
            byte2 = 0   // closing (since no one has it open)
        }
        wrapper.sendPacket(this)
    }
}
