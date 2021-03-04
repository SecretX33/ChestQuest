package io.github.secretx33.chestquest.commands

import io.github.secretx33.chestquest.config.Config
import io.github.secretx33.chestquest.config.Const.CONFIGS_RELOADED
import io.github.secretx33.chestquest.config.Const.DEBUG_MODE_STATE_CHANGED
import io.github.secretx33.chestquest.config.Const.PLUGIN_COMMAND_PREFIX
import io.github.secretx33.chestquest.repository.ChestRepo
import io.github.secretx33.chestquest.utils.*
import io.github.secretx33.chestquest.utils.Utils.consoleMessage
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.koin.core.component.KoinApiExtension
import java.util.*

@KoinApiExtension
class Commands(private val plugin: JavaPlugin, private val chestRepo: ChestRepo) : CommandExecutor {

    init { plugin.getCommand(PLUGIN_COMMAND_PREFIX)?.executor = this }

    override fun onCommand(sender: CommandSender, command: Command, s: String, strings: Array<String>): Boolean {
        if (strings.isEmpty()) return true
        for (i in strings.indices) {
            strings[i] = strings[i].toLowerCase(Locale.US)
        }
        when (strings[0]) {
            "mark" -> if (sender.canEditQC()) {
                if(sender !is Player) sender.sendMessage("You may only use this command in-game")
                else {
                    if(strings.size == 1) {
                        sender.sendMessage("Please type a number after ${ChatColor.GOLD}mark${ChatColor.WHITE}. Command usage: /cq mark [number]")
                        return true
                    }
                    try {
                        val order = strings[1].toInt()
                        if(order < 1) {
                            sender.sendMessage("Please type a number greater than 0. Command usage: /cq mark [number]")
                            return true
                        }
                        sender.getTargetBlock(null, 5)?.takeIf { it.isChest() }?.let { chest ->
                            if(chestRepo.isQuestChest(chest.location)) {
                                sender.message("This chest is already a Quest Chest")
                            } else {
                                chestRepo.addQuestChest(chest.location, order)
                                sender.message("Marked chest at ${chest.coordinates()} as a Quest Chest ${ChatColor.BLUE}$order")
                                consoleMessage("Marked chest at ${chest.coordinates()} as a Quest Chest ${ChatColor.BLUE}$order")
                            }
                        }
                    } catch (e: NumberFormatException) {
                        sender.sendMessage("Please type only numbers after ${ChatColor.GOLD}mark${ChatColor.WHITE}. Command usage: /cq mark [number]")
                        return true
                    }
                }
            }
            "unmark" -> if (sender.canEditQC()) {
                if(sender !is Player) sender.sendMessage("You may only use this command ingame")
                else {
                    sender.getTargetBlock(null, 5)?.takeIf { it.isChest() }?.let {
                        if(chestRepo.isQuestChest(it.location)) {
                            chestRepo.removeQuestChest(it.location)
                            sender.message("Converted chest at ${it.coordinates()}} back to a normal chest")
                            consoleMessage("Converted chest at ${it.coordinates()}} back to a normal chest")
                        } else {
                            sender.message("This chest is NOT a Quest Chest")
                        }
                    }
                }
            }
            "setorder" -> if (sender.canEditQC()) {
                if(sender !is Player) sender.sendMessage("You may only use this command in-game")
                else {
                    if(strings.size == 1) {
                        sender.sendMessage("Please type a number after ${ChatColor.GOLD}setorder${ChatColor.WHITE}. Command usage: /cq setorder [number]")
                        return true
                    }
                    try {
                        val order = strings[1].toInt()
                        if(order < 1) {
                            sender.sendMessage("Please type a number greater than 0. Command usage: /cq setorder [number]")
                            return true
                        }
                        sender.getTargetBlock(null, 5)?.takeIf { it.isChest() }?.let { chest ->
                            if(!chestRepo.isQuestChest(chest.location)) {
                                sender.message("This chest is not a Quest Chest, you can't change its order")
                            } else {
                                val oldOrder = chestRepo.changeOrderQuestChest(chest.location, order)
                                sender.message("Chest order is now ${ChatColor.RED}$order${ChatColor.WHITE} (previously $oldOrder)")
                                consoleMessage("Chest order is now ${ChatColor.RED}$order${ChatColor.WHITE} (previously $oldOrder)")
                            }
                        }
                    } catch (e: NumberFormatException) {
                        sender.sendMessage("Please type only numbers after ${ChatColor.GOLD}setorder${ChatColor.WHITE}. Command usage: /cq setorder [number]")
                        return true
                    }
                }
            }
            "order" -> if (sender.canEditQC()) {
                if(sender !is Player) sender.sendMessage("You may only use this command in-game")
                else {
                    sender.getTargetBlock(null, 5)?.takeIf { it.isChest() }?.let { chest ->
                        if(!chestRepo.isQuestChest(chest.location)) {
                            sender.message("This chest is not a Quest Chest, and so it doesn't have order")
                        } else {
                            val order = chestRepo.getQuestChestOrder(chest.location)
                            sender.message("This chest order is $order")
                        }
                    }
                }
            }
            "reset" -> if (sender.canResetProgress()) {
                sender.getTargetBlock(null, 5)?.takeIf { it.isChest() }?.let { chest ->
                    if(!chestRepo.isQuestChest(chest.location)) {
                        sender.message("This chest is not a Quest Chest, and so it doesn't have order")
                    } else {
                        val order = chestRepo.getQuestChestOrder(chest.location)
                        sender.message("This chest order is $order")
                    }
                }
            }
            "reload" -> if (sender.canReload()) {
                plugin.saveDefaultConfig()
                plugin.reloadConfig()
                Config.reloadConfig()
                sender.sendMessage(CONFIGS_RELOADED)
                if(sender !is Player) consoleMessage(CONFIGS_RELOADED)
            }
            "debug" -> if (sender.canToggleDebug()) {
                val config = plugin.config
                Config.debug = !Config.debug
                config["general.debug"] = Config.debug
                plugin.saveConfig()
                sender.sendMessage(DEBUG_MODE_STATE_CHANGED.format(if(Config.debug) "ON" else "OFF"))
                if(sender !is Player) consoleMessage(DEBUG_MODE_STATE_CHANGED.format(if(Config.debug) "ON" else "OFF"))
            }
        }
        return true
    }
}

