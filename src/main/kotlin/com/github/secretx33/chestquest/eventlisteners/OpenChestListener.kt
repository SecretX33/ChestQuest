package com.github.secretx33.chestquest.eventlisteners

import com.comphenix.protocol.wrappers.BlockPosition
import com.github.secretx33.chestquest.packets.WrapperPlayServerBlockAction
import com.github.secretx33.chestquest.repository.ChestRepo
import com.github.secretx33.chestquest.repository.PlayerProgressRepo
import com.github.secretx33.chestquest.utils.canEditQC
import com.github.secretx33.chestquest.utils.isChest
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Chest
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class OpenChestListener(plugin: Plugin, private val chestRepo: ChestRepo, private val progressRepo: PlayerProgressRepo) : Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private fun PlayerInteractEvent.onChestOpen() {
        if(!isChestQuest() || player.canEditQC()) return

        isCancelled = true
        val chest = clickedBlock?.state as Chest
        val chestOrder = chestRepo.getQuestChestOrder(chest.location)

        // if player cannot open chest
        if(!progressRepo.canOpenChest(player.uniqueId, chestOrder)) {
            println("Player ${player.name} progress still ${progressRepo.getPlayerProgress(player.uniqueId)}, he cannot open a chest that has a order of $chestOrder")
            return
        }

        // open chest inventory to the player
        player.openInventory(chestRepo.getChestContent(chest, player))
        player.simulateChestOpen(chest)
        println("Player ${player.name} can open chest of order $chestOrder because he has progress of ${progressRepo.getPlayerProgress(player.uniqueId)}")
    }

    private fun Player.simulateChestOpen(chest: Chest) {
        playSound(chest.location, Sound.BLOCK_CHEST_OPEN, 0.67f, 1f)
        sendOpenChestPacket(chest)
    }

    private fun Player.sendOpenChestPacket(chest: Chest){
        if(!Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) return

        val wrapper = WrapperPlayServerBlockAction()
        wrapper.apply {
            blockType = Material.CHEST
            location = BlockPosition(chest.location.toVector())
            byte1 = 1  // update number of people with chest open action ID
            byte2 = 1  // opening (since one has open it)
        }
        wrapper.sendPacket(this)
    }

    private fun PlayerInteractEvent.isChestQuest(): Boolean {
        return action == Action.RIGHT_CLICK_BLOCK && clickedBlock?.isChest() == true && chestRepo.isQuestChest(clickedBlock.location)
    }
}
