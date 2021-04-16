package com.github.secretx33.chestquest.commands

import com.github.secretx33.chestquest.commands.subcommands.*
import com.github.secretx33.chestquest.config.Const.PLUGIN_COMMAND_PREFIX
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.koin.core.component.KoinApiExtension
import java.util.*

@KoinApiExtension
class Commands(val plugin: JavaPlugin) : CommandExecutor, TabCompleter {

    private val subcommands: List<SubCommand> = listOf(DebugCommand(),
        MarkCommand(),
        OrderCommand(),
        ReloadCommand(),
        ResetProgressCommand(),
        SetOrderCommand(),
        UnmarkCommand())

    init {
        plugin.getCommand(PLUGIN_COMMAND_PREFIX)?.let { cmd ->
            cmd.executor = this
            cmd.tabCompleter = this
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, alias: String, strings: Array<String>): Boolean {
        if (strings.isEmpty()) return true

        val sub = strings[0]
        subcommands.firstOrNull { it.hasPermission(sender) && (it.name == sub || it.aliases.contains(sub)) }?.let { cmd ->
            if(sender is Player) {
                cmd.onCommandByPlayer(sender, alias, strings)
            } else {
                cmd.onCommandByConsole(sender, alias, strings)
            }
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, strings: Array<String>): List<String> {
        // chestquest <subcommand> <args>
        if(strings.size == 1) {
            return subcommands.asSequence()
                .filter { cmd -> cmd.hasPermission(sender) && cmd.name.startsWith(strings[0], ignoreCase = true)}
                .map { it.name }
                .toList()
        }
        if(strings.size > 1) {
            return subcommands
                .firstOrNull { it.hasPermission(sender) && it.aliases.contains(strings[0].toLowerCase()) }
                ?.getCompletor(sender, strings.size, strings[strings.size - 1], strings)
                ?: emptyList()
        }
        return emptyList()
    }
}

