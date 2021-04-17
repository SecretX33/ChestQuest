package com.github.secretx33.chestquest.commands.subcommands

import com.github.secretx33.chestquest.repository.PlayerProgressRepo
import com.github.secretx33.chestquest.utils.CustomKoinComponent
import com.github.secretx33.chestquest.utils.inject
import com.github.secretx33.chestquest.utils.message
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class ResetProgressCommand : SubCommand(), CustomKoinComponent {

    override val name: String = "resetprogress"
    override val permission: String = "resetprogress"
    override val aliases: List<String> = listOf(name, "resetp", "reset", "rp", "clearprogress", "clearp")

    private val progressRepo by inject<PlayerProgressRepo>()

    override fun onCommandByPlayer(player: Player, alias: String, strings: Array<String>) {
        onCommandByConsole(player, alias, strings)
    }

    override fun onCommandByConsole(sender: CommandSender, alias: String, strings: Array<String>) {
        if(strings.size < 2) {
            sender.message("${ChatColor.RED}Please type a name after ${ChatColor.GOLD}reset${ChatColor.RED}. Command usage: /$alias $name <player>")
            return
        }

        // if player doesn't exist, warn and return
        val player = Bukkit.getPlayerExact(strings[1]) ?: run {
            sender.message("${ChatColor.RED}Player ${ChatColor.BLUE}${strings[1]}${ChatColor.RED} was not found, please check if you spelled his name correctly.")
            return
        }

        // clear all player progress
        progressRepo.clearPlayerProgress(player)
        sender.message("Cleared ALL progress from player ${ChatColor.BLUE}${player.name}${ChatColor.WHITE}.")
    }

    override fun getCompletor(sender: CommandSender, length: Int, hint: String, strings: Array<String>): List<String> {
        if(length != 2) return emptyList()
        return Bukkit.getOnlinePlayers().asSequence()
            .filter { it.name.startsWith(hint, ignoreCase = true) }
            .map { it.name }
            .toList()
    }
}
