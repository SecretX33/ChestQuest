package com.github.secretx33.chestquest.commands.subcommands

import com.github.secretx33.chestquest.repository.ChestRepo
import com.github.secretx33.chestquest.utils.*
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class MarkCommand : SubCommand(), CustomKoinComponent {

    override val name: String = "mark"
    override val permission: String = "edit"
    override val aliases: List<String> = listOf(name, "m")

    private val chestRepo by inject<ChestRepo>()

    override fun onCommandByPlayer(player: Player, alias: String, strings: Array<String>) {
        if(strings.size < 2 || strings[1].toIntOrNull().let { it == null || it < 1 }) {
            player.sendMessage("${ChatColor.RED}Please type a number greater than 0 after ${ChatColor.GOLD}mark${ChatColor.RED}. Command usage: /$alias $name <number>")
            return
        }
        val chest = player.getTargetBlock(null, 5)?.takeIf { it.isChest() } ?: return
        val order = strings[1].toInt()

        // if chest is already a quest chest, warn and return
        if(chestRepo.isQuestChest(chest.location)) {
            player.sendMessage("${ChatColor.RED}This chest is already a Quest Chest")
            return
        }
        // mark chest as quest chest
        chestRepo.addQuestChest(chest.location, order)
        player.sendMessage("Marked chest at ${chest.coordinates()} as a Quest Chest ${ChatColor.BLUE}$order")
    }

    override fun onCommandByConsole(sender: CommandSender, alias: String, strings: Array<String>) {
        sender.sendMessage("${ChatColor.RED}You may only use this command in-game")
    }

    override fun getCompletor(sender: CommandSender, length: Int, hint: String, strings: Array<String>): List<String> {
        if(sender !is Player || length != 2 || hint.isNotBlank()) return emptyList()
        return listOf("<number>")
    }
}
