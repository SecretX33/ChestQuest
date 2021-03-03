package io.github.secretx33.chestquest.commands

import io.github.secretx33.chestquest.config.Config
import io.github.secretx33.chestquest.config.Const
import io.github.secretx33.chestquest.config.Const.DEBUG_MODE_STATE_CHANGED
import io.github.secretx33.chestquest.config.Const.PLUGIN_COMMAND_PREFIX
import io.github.secretx33.chestquest.repository.ChestRepo
import io.github.secretx33.chestquest.utils.*
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
        if (strings.isNotEmpty()) {
            for (i in strings.indices) {
                strings[i] = strings[i].toLowerCase(Locale.US)
            }
            when (strings[0]) {
                "mark" -> if (sender.canEditQC()) {
                    if(sender !is Player) sender.sendMessage("You may only use this command in-game")
                    else {
                        sender.getTargetBlock(null, 5)?.takeIf { it.isChest() }?.let {
                            if(chestRepo.isQuestChest(it.location)) {
                                sender.message("This chest is already a Quest Chest")
                            } else {
                                chestRepo.addQuestChest(it.location)
                                sender.message("Marked chest at ${it.coordinates()} as a Quest Chest")
                            }
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
                            } else {
                                sender.message("This chest is NOT a Quest Chest")
                            }
                        }
                    }
                }
                "reload" -> if (sender.canReload()) {
                    plugin.saveDefaultConfig()
                    plugin.reloadConfig()
                    Config.reloadConfig()
                    sender.sendMessage(Const.CONFIGS_RELOADED)
                }
                "debug" -> if (sender.canToggleDebug()) {
                    val config = plugin.config
                    Config.debug = !Config.debug
                    config["general.debug"] = Config.debug
                    plugin.saveConfig()
                    sender.sendMessage(String.format(DEBUG_MODE_STATE_CHANGED, if(Config.debug) "ON" else "OFF"))
                }
            }
        }
        return true
    }
}

