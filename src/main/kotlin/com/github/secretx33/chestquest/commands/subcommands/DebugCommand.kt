package com.github.secretx33.chestquest.commands.subcommands

import com.github.secretx33.chestquest.config.Config
import com.github.secretx33.chestquest.config.Const
import com.github.secretx33.chestquest.utils.CustomKoinComponent
import com.github.secretx33.chestquest.utils.Utils.consoleMessage
import com.github.secretx33.chestquest.utils.inject
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class DebugCommand : SubCommand(), CustomKoinComponent {

    override val name: String = "debug"
    override val permission: String = "debug"
    override val aliases: List<String> = listOf(name, "m")

    private val plugin by inject<Plugin>()

    override fun onCommandByPlayer(player: Player, strings: Array<String>) {
       onCommandByConsole(player, strings)
    }

    override fun onCommandByConsole(sender: CommandSender, strings: Array<String>) {
        val config = plugin.config
        Config.debug = !Config.debug
        config["general.debug"] = Config.debug
        plugin.saveConfig()
        val msg = Const.DEBUG_MODE_STATE_CHANGED.format(if(Config.debug) "ON" else "OFF")
        sender.sendMessage(msg)
        if(sender is Player) consoleMessage(msg)
    }

    override fun getCompletor(sender: CommandSender, length: Int, hint: String, strings: Array<String>): List<String> {
        return emptyList()
    }
}