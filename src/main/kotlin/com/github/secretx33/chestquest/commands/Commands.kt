package com.github.secretx33.chestquest.commands

import com.github.secretx33.chestquest.config.Config
import com.github.secretx33.chestquest.config.Const.CONFIGS_RELOADED
import com.github.secretx33.chestquest.config.Const.DEBUG_MODE_STATE_CHANGED
import com.github.secretx33.chestquest.config.Const.PLUGIN_COMMAND_PREFIX
import com.github.secretx33.chestquest.repository.ChestRepo
import com.github.secretx33.chestquest.repository.PlayerProgressRepo
import com.github.secretx33.chestquest.utils.*
import com.github.secretx33.chestquest.utils.Utils.consoleMessage
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.koin.core.component.KoinApiExtension
import java.util.*

@KoinApiExtension
class Commands(private val plugin: JavaPlugin, private val chestRepo: ChestRepo, private val progressRepo: PlayerProgressRepo) : CommandExecutor {

    init { plugin.getCommand(PLUGIN_COMMAND_PREFIX)?.executor = this }

    override fun onCommand(sender: CommandSender, command: Command, s: String, strings: Array<String>): Boolean {
        if (strings.isEmpty()) return true
        for (i in strings.indices) {
            strings[i] = strings[i].toLowerCase(Locale.US)
        }
        return when (strings[0]) {
            "mark" -> mark(sender, strings)
            "unmark" -> unmark(sender)
            "setorder" -> setorder(sender, strings)
            "order" -> order(sender)
            "resetprogress" -> resetProgress(sender, strings)
            "reload" -> reload(sender)
            "debug" -> debug(sender)
            else -> true
        }
    }

    private fun mark(sender: CommandSender, strings: Array<String>): Boolean {
        if(!sender.canEditQC()) return true

        if(sender !is Player) sender.sendMessage("${ChatColor.RED}You may only use this command in-game")
        else {
            if(strings.size == 1) {
                sender.message("${ChatColor.RED}Please type a number after ${ChatColor.GOLD}mark${ChatColor.RED}. Command usage: /cq mark [number]")
                return true
            }
            try {
                val order = strings[1].toInt()
                if(order < 1) {
                    sender.message("${ChatColor.RED}Please type a number greater than 0. Command usage: /cq mark [number]")
                    return true
                }
                sender.getTargetBlock(null, 5)?.takeIf { it.isChest() }?.let { chest ->
                    if(chestRepo.isQuestChest(chest.location)) {
                        sender.message("${ChatColor.RED}This chest is already a Quest Chest")
                    } else {
                        chestRepo.addQuestChest(chest.location, order)
                        sender.message("Marked chest at ${chest.coordinates()} as a Quest Chest ${ChatColor.BLUE}$order")
                        consoleMessage("Marked chest at ${chest.coordinates()} as a Quest Chest ${ChatColor.BLUE}$order")
                    }
                }
            } catch (e: NumberFormatException) {
                sender.message("${ChatColor.RED}Please type only numbers after ${ChatColor.GOLD}mark${ChatColor.WHITE}. Command usage: /cq mark [number]")
            }
        }

        return true
    }

    private fun unmark(sender: CommandSender): Boolean {
        if(!sender.canEditQC()) return true

        if(sender !is Player) sender.sendMessage("${ChatColor.RED}You may only use this command ingame")
        else {
            sender.getTargetBlock(null, 5)?.takeIf { it.isChest() }?.let {
                if(chestRepo.isQuestChest(it.location)) {
                    chestRepo.removeQuestChest(it.location)
                    sender.message("Converted chest at ${it.coordinates()}} back to a normal chest")
                    consoleMessage("Converted chest at ${it.coordinates()}} back to a normal chest")
                } else {
                    sender.message("${ChatColor.RED}This chest is NOT a Quest Chest")
                }
            }
        }
        return true
    }

    private fun setorder(sender: CommandSender, strings: Array<String>): Boolean {
        if(!sender.canEditQC()) return true

        if(sender !is Player) sender.sendMessage("${ChatColor.RED}You may only use this command in-game")
        else {
            if(strings.size == 1) {
                sender.message("${ChatColor.RED}Please type a number after ${ChatColor.GOLD}setorder${ChatColor.RED}. Command usage: /cq setorder [number]")
                return true
            }
            try {
                val order = strings[1].toInt()
                if(order < 1) {
                    sender.message("${ChatColor.RED}Please type a number greater than 0. Command usage: /cq setorder [number]")
                    return true
                }
                sender.getTargetBlock(null, 5)?.takeIf { it.isChest() }?.let { chest ->
                    if(!chestRepo.isQuestChest(chest.location)) {
                        sender.message("${ChatColor.RED}This chest is not a Quest Chest, you can't change its order")
                    } else {
                        val oldOrder = chestRepo.changeOrderQuestChest(chest.location, order)
                        sender.message("Chest order is now ${ChatColor.RED}$order${ChatColor.WHITE} (previously $oldOrder)")
                        consoleMessage("Chest order is now ${ChatColor.RED}$order${ChatColor.WHITE} (previously $oldOrder)")
                    }
                }
            } catch (e: NumberFormatException) {
                sender.message("${ChatColor.RED}Please type only numbers after ${ChatColor.GOLD}setorder${ChatColor.RED}. Command usage: /cq setorder [number]")
            }
        }
        return true
    }

    private fun order(sender: CommandSender): Boolean {
        if(!sender.canEditQC()) return true

        if(sender !is Player) sender.sendMessage("${ChatColor.RED}You may only use this command in-game")
        else {
            sender.getTargetBlock(null, 5)?.takeIf { it.isChest() }?.let { chest ->
                if(!chestRepo.isQuestChest(chest.location)) {
                    sender.message("${ChatColor.RED}This chest is not a Quest Chest, and so it doesn't have order")
                } else {
                    val order = chestRepo.getQuestChestOrder(chest.location)
                    sender.message("This chest order is ${ChatColor.RED}$order")
                }
            }
        }
        return true
    }

    private fun resetProgress(sender: CommandSender, strings: Array<String>): Boolean {
        if(!sender.canResetProgress()) return true

        if(strings.size == 1) {
            sender.message("${ChatColor.RED}Please type a name after ${ChatColor.GOLD}reset${ChatColor.RED}. Command usage: /cq resetprogress [playerName]")
            return true
        }
        val player = Bukkit.getPlayer(strings[1])
        if(player == null) {
            sender.message("${ChatColor.RED}Player ${ChatColor.BLUE}${strings[1]}${ChatColor.RED} was not found, please check if you spelled his name correctly.")
            return true
        }
        progressRepo.clearPlayerProgress(player)
        sender.message("Cleared ALL progress from player ${ChatColor.BLUE}${player.name}${ChatColor.WHITE}.")
        return true
    }

    private fun reload(sender: CommandSender): Boolean {
        if(!sender.canReload()) return true

        plugin.saveDefaultConfig()
        plugin.reloadConfig()
        Config.reloadConfig()
        sender.sendMessage(CONFIGS_RELOADED)
        if(sender is Player) consoleMessage(CONFIGS_RELOADED)
        return true
    }

    private fun debug(sender: CommandSender): Boolean {
        if(!sender.canToggleDebug()) return true

        val config = plugin.config
        Config.debug = !Config.debug
        config["general.debug"] = Config.debug
        plugin.saveConfig()
        sender.sendMessage(DEBUG_MODE_STATE_CHANGED.format(if(Config.debug) "ON" else "OFF"))
        if(sender is Player) consoleMessage(DEBUG_MODE_STATE_CHANGED.format(if(Config.debug) "ON" else "OFF"))
        return true
    }
}

