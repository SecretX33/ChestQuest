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
class ReloadCommand : SubCommand(), CustomKoinComponent {

    override val name: String = "reload"
    override val permission: String = "reload"
    override val aliases: List<String> = listOf(name, "rel", "r")

    private val plugin by inject<Plugin>()

    override fun onCommandByPlayer(player: Player, strings: Array<String>) {
        onCommandByConsole(player, strings)
    }

    override fun onCommandByConsole(sender: CommandSender, strings: Array<String>) {
        plugin.saveDefaultConfig()
        plugin.reloadConfig()
        Config.reloadConfig()
        sender.sendMessage(Const.CONFIGS_RELOADED)
        if(sender is Player) consoleMessage(Const.CONFIGS_RELOADED)
    }

    override fun getCompletor(sender: CommandSender, length: Int, hint: String, strings: Array<String>): List<String> {
        return emptyList()
    }
}
