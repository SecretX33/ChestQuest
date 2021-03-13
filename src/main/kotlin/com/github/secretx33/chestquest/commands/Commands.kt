package com.github.secretx33.chestquest.commands

import com.github.secretx33.chestquest.commands.subcommands.*
import com.github.secretx33.chestquest.config.Const.PLUGIN_COMMAND_PREFIX
import com.github.secretx33.chestquest.repository.ChestRepo
import com.github.secretx33.chestquest.repository.PlayerProgressRepo
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.koin.core.component.KoinApiExtension
import java.util.*

@KoinApiExtension
class Commands(private val plugin: JavaPlugin, private val chestRepo: ChestRepo, private val progressRepo: PlayerProgressRepo) : CommandExecutor, TabCompleter {

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

    override fun onCommand(sender: CommandSender, command: Command, s: String, strings: Array<String>): Boolean {
        if (strings.isEmpty()) return true
        for (i in strings.indices) {
            strings[i] = strings[i].toLowerCase(Locale.US)
        }
        val sub = strings[0]
        for(cmd in subcommands) {
            if(sub == cmd.name || cmd.aliases.contains(sub)) {
                if(cmd.hasPermission(sender)) {
                    if(sender is Player) {
                        cmd.onCommandByPlayer(sender, strings)
                    } else {
                        cmd.onCommandByConsole(sender, strings)
                    }
                }
                break
            }
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, strings: Array<String>): List<String> {
        // chestquest <subcommand> <args>
        if(strings.size == 1){
            val subs = ArrayList<String>()
            for (cmd in subcommands) {
                if(cmd.hasPermission(sender)) {
                    subs.add(cmd.name)
                }
            }
            return subs.filter { it.startsWith(strings[0], ignoreCase = true) }
        }
        if(strings.size > 1) {
            for(cmd in subcommands) {
                if(cmd.aliases.contains(strings[0].toLowerCase())) {
                    return if(cmd.hasPermission(sender)) {
                        cmd.getCompletor(sender, strings.size, strings[strings.size - 1], strings)
                    } else emptyList()
                }
            }
        }
        return emptyList()
    }
}

