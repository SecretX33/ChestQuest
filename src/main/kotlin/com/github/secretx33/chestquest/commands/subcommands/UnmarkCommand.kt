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

    override fun onCommandByPlayer(player: Player, strings: Array<String>) {
        player.getTargetBlock(null, 5)?.takeIf { it.isChest() }?.let {
            if(chestRepo.isQuestChest(it.location)) {
                chestRepo.removeQuestChest(it.location)
                player.message("Converted chest at ${it.coordinates()} back to a normal chest")
                Utils.consoleMessage("Converted chest at ${it.coordinates()} back to a normal chest")
            } else {
                player.message("${ChatColor.RED}This chest is NOT a Quest Chest")
            }
        }
    }

    override fun onCommandByConsole(sender: CommandSender, strings: Array<String>) {
        sender.sendMessage("${ChatColor.RED}You may only use this command in-game")
    }

    override fun getCompletor(sender: CommandSender, length: Int, hint: String, strings: Array<String>): List<String> {
        return emptyList()
    }
}
