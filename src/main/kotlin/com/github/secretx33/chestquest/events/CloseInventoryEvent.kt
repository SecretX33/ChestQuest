package com.github.secretx33.chestquest.events

import com.comphenix.protocol.wrappers.BlockPosition
import com.github.secretx33.chestquest.packets.WrapperPlayServerBlockAction
import com.github.secretx33.chestquest.repository.ChestRepo
import com.github.secretx33.chestquest.utils.Utils.debugMessage
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

@KoinApiExtension
class CloseInventoryEvent(plugin: Plugin, private val chestRepo: ChestRepo) : Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun onInventoryClose(event: InventoryCloseEvent) {
        val inv = event.inventory
        val player = event.player as? Player ?: return

        if(!player.canEditQC() && chestRepo.isChestInventory(inv)) {
            chestRepo.updateInventory(player.uniqueId, inv)
            (inv.holder as? Chest)?.let { chest -> player.simulateChestClose(chest) }
            debugMessage("Player ${player.name} closed his custom inventory, saving it into DB.")
        }
    }

    private fun Player.simulateChestClose(chest: Chest){
        playSound(chest.location, Sound.BLOCK_CHEST_CLOSE, 0.5f, 1f)
        sendCloseChestPacket(chest)
    }

    private fun Player.sendCloseChestPacket(chest: Chest){
        if(!Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) return

        val wrapper = WrapperPlayServerBlockAction()
        wrapper.blockType = Material.CHEST
        wrapper.location = BlockPosition(chest.location.toVector())
        wrapper.byte1 = 1 // update number of people with chest open action ID
        wrapper.byte2 = 0 // closing (since no one has it open)
        wrapper.sendPacket(this)
    }
}
