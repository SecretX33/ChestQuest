package com.github.secretx33.chestquest.commands.subcommands

import com.github.secretx33.chestquest.repository.ChestRepo
import com.github.secretx33.chestquest.utils.*
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.koin.core.component.KoinApiExtension
import java.util.*

@KoinApiExtension
class MarkCommand : SubCommand(), CustomKoinComponent {

    override val name: String = "mark"
    override val permission: String = "edit"
    override val aliases: List<String> = listOf(name, "m")

    private val chestRepo by inject<ChestRepo>()

    override fun onCommandByPlayer(player: Player, strings: Array<String>) {
        if(strings.size == 1) {
            player.sendMessage("${ChatColor.RED}Please type a number after ${ChatColor.GOLD}mark${ChatColor.RED}. Command usage: /cq mark <number>")
            return
        }
        try {
            val order = strings[1].toInt()
            if(order < 1) {
                player.sendMessage("${ChatColor.RED}Please type a number greater than 0. Command usage: /cq mark <number>")
                return
            }
            player.getTargetBlock(null, 5)?.takeIf { it.isChest() }?.let { chest ->
                if(chestRepo.isQuestChest(chest.location)) {
                    player.message("${ChatColor.RED}This chest is already a Quest Chest")
                } else {
                    chestRepo.addQuestChest(chest.location, order)
                    player.message("Marked chest at ${chest.coordinates()} as a Quest Chest ${ChatColor.BLUE}$order")
                    Utils.consoleMessage("Marked chest at ${chest.coordinates()} as a Quest Chest ${ChatColor.BLUE}$order")
                }
            }
        } catch (e: NumberFormatException) {
            player.sendMessage("${ChatColor.RED}Please type only numbers after ${ChatColor.GOLD}mark${ChatColor.WHITE}. Command usage: /cq mark <number>")
        }
    }

    override fun onCommandByConsole(sender: CommandSender, strings: Array<String>) {
        sender.sendMessage("${ChatColor.RED}You may only use this command in-game")
    }

    override fun getCompletor(sender: CommandSender, length: Int, hint: String, strings: Array<String>): List<String> {
        val options = ArrayList<String>()
        if (length == 1) {
            return options
        }
        if (length == 2 && sender is Player) {
            options.add("<number>")
        }
        return options.filter { it.startsWith(hint, ignoreCase = true) }
    }
}
