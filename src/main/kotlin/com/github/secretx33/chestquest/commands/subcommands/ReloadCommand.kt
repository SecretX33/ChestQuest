package com.github.secretx33.chestquest.commands.subcommands

import com.github.secretx33.chestquest.config.Config
import com.github.secretx33.chestquest.config.Const
import com.github.secretx33.chestquest.utils.CustomKoinComponent
import com.github.secretx33.chestquest.utils.inject
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class ReloadCommand : SubCommand(), CustomKoinComponent {

    override val name: String = "reload"
    override val permission: String = "reload"
    override val aliases: List<String> = listOf(name, "rel", "r")

    private val config by inject<Config>()

    override fun onCommandByPlayer(player: Player, alias: String, strings: Array<String>) {
        onCommandByConsole(player, alias, strings)
    }

    override fun onCommandByConsole(sender: CommandSender, alias: String, strings: Array<String>) {
        config.reload()
        sender.sendMessage(Const.CONFIGS_RELOADED)
    }

    override fun getCompletor(sender: CommandSender, length: Int, hint: String, strings: Array<String>): List<String> {
        return emptyList()
    }
}
