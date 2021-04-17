package com.github.secretx33.chestquest.commands.subcommands

import com.github.secretx33.chestquest.repository.ChestRepo
import com.github.secretx33.chestquest.utils.CustomKoinComponent
import com.github.secretx33.chestquest.utils.inject
import com.github.secretx33.chestquest.utils.isChest
import com.github.secretx33.chestquest.utils.message
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class OrderCommand : SubCommand(), CustomKoinComponent {

    override val name: String = "order"
    override val permission: String = "edit"
    override val aliases: List<String> = listOf(name, "ord", "o", "numero", "posicao")

    private val chestRepo by inject<ChestRepo>()

    override fun onCommandByPlayer(player: Player, alias: String, strings: Array<String>) {
        val chest = player.getTargetBlock(null, 5)?.takeIf { it.isChest() } ?: return

        // if chest is not quest chest, warn and return
        if(!chestRepo.isQuestChest(chest.location)) {
            player.sendMessage("${ChatColor.RED}This chest is not a Quest Chest, and so it doesn't have order")
            return
        }
        // inform chest order to player
        val order = chestRepo.getQuestChestOrder(chest.location)
        player.message("This chest order is ${ChatColor.RED}$order")
    }

    override fun onCommandByConsole(sender: CommandSender, alias: String, strings: Array<String>) {
        sender.sendMessage("${ChatColor.RED}You may only use this command in-game")
    }

    override fun getCompletor(sender: CommandSender, length: Int, hint: String, strings: Array<String>): List<String> {
        return emptyList()
    }
}
