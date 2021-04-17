package com.github.secretx33.chestquest.eventlisteners

import com.github.secretx33.chestquest.repository.ChestRepo
import com.github.secretx33.chestquest.utils.isChest
import net.md_5.bungee.api.ChatColor
import org.bukkit.Bukkit
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class BreakChestListener(plugin: Plugin, private val chestRepo: ChestRepo) : Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun BlockBreakEvent.onQuestChestBreak() {
        if(!block.isQuestChest()) return

        chestRepo.removeQuestChest(block.location)
        player.sendMessage("${ChatColor.RED}You broke a Quest Chest!")
    }

    private fun Block.isQuestChest(): Boolean = isChest() && chestRepo.isQuestChest(location)
}
