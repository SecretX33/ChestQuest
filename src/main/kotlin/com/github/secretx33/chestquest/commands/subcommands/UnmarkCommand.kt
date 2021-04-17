package com.github.secretx33.chestquest.commands.subcommands

import com.github.secretx33.chestquest.repository.ChestRepo
import com.github.secretx33.chestquest.utils.*
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class UnmarkCommand : SubCommand(), CustomKoinComponent {

    override val name: String = "unmark"
    override val permission: String = "edit"
    override val aliases: List<String> = listOf(name, "um", "un")

    private val chestRepo by inject<ChestRepo>()

    override fun onCommandByPlayer(player: Player, alias: String, strings: Array<String>) {
        val chest = player.getTargetBlock(null, 5)?.takeIf { it.isChest() } ?: return

        // if chest is not a quest chest
        if(!chestRepo.isQuestChest(chest.location)) {
            player.message("${ChatColor.RED}This chest is NOT a Quest Chest")
            return
        }
        // remove quest chest
        chestRepo.removeQuestChest(chest.location)
        player.message("Converted chest at ${chest.coordinates()} back to a normal chest")
        Utils.consoleMessage("Converted chest at ${chest.coordinates()} back to a normal chest")

    }

    override fun onCommandByConsole(sender: CommandSender, alias: String, strings: Array<String>) {
        sender.sendMessage("${ChatColor.RED}You may only use this command in-game")
    }

    override fun getCompletor(sender: CommandSender, length: Int, hint: String, strings: Array<String>): List<String> {
        return emptyList()
    }
}
