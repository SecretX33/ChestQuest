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
import java.util.*

@KoinApiExtension
class ResetProgressCommand : SubCommand(), CustomKoinComponent {

    override val name: String = "resetprogress"
    override val permission: String = "resetprogress"
    override val aliases: List<String> = listOf(name, "resetp", "reset", "rp", "clearprogress", "clearp")

    private val progressRepo by inject<PlayerProgressRepo>()

    override fun onCommandByPlayer(player: Player, strings: Array<String>) {
        onCommandByConsole(player, strings)
    }

    override fun onCommandByConsole(sender: CommandSender, strings: Array<String>) {
        if(strings.size == 1) {
            sender.message("${ChatColor.RED}Please type a name after ${ChatColor.GOLD}reset${ChatColor.RED}. Command usage: /cq resetprogress [playerName]")
            return
        }
        val player = Bukkit.getPlayer(strings[1])
        if(player == null) {
            sender.message("${ChatColor.RED}Player ${ChatColor.BLUE}${strings[1]}${ChatColor.RED} was not found, please check if you spelled his name correctly.")
            return
        }
        progressRepo.clearPlayerProgress(player)
        sender.message("Cleared ALL progress from player ${ChatColor.BLUE}${player.name}${ChatColor.WHITE}.")
    }

    override fun getCompletor(sender: CommandSender, length: Int, hint: String, strings: Array<String>): List<String> {
        val options = ArrayList<String>()
        if (length == 1) {
            return options
        }
        if (length == 2) {
            options.addAll(Bukkit.getServer().onlinePlayers.map { it.name })
        }
        return options.filter { it.startsWith(hint, ignoreCase = true) }
    }
}
