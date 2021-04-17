package com.github.secretx33.chestquest.commands.subcommands

import com.github.secretx33.chestquest.repository.ChestRepo
import com.github.secretx33.chestquest.utils.CustomKoinComponent
import com.github.secretx33.chestquest.utils.inject
import com.github.secretx33.chestquest.utils.isChest
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
class SetOrderCommand : SubCommand(), CustomKoinComponent {

    override val name: String = "setorder"
    override val permission: String = "edit"
    override val aliases: List<String> = listOf(name, "seto", "so")

    private val chestRepo by inject<ChestRepo>()

    override fun onCommandByPlayer(player: Player, alias: String, strings: Array<String>) {
        if(strings.size < 2 || strings[1].toIntOrNull().let { it == null || it < 1 }) {
            player.sendMessage("${ChatColor.RED}Please type a number greater than 0 after ${ChatColor.GOLD}setorder${ChatColor.RED}. Command usage: /$alias $name <number>")
            return
        }
        val chest = player.getTargetBlock(null, 5)?.takeIf { it.isChest() } ?: return
        val order = strings[1].toInt()

        // if chest is not quest chest, warn and return
        if(!chestRepo.isQuestChest(chest.location)) {
            player.sendMessage("${ChatColor.RED}This chest is not a Quest Chest, you can't change its order")
            return
        }

        // modify chest order to typed order
        val oldOrder = chestRepo.changeOrderQuestChest(chest.location, order)
        player.sendMessage("Chest order is now ${ChatColor.RED}$order${ChatColor.WHITE} (previously $oldOrder)")
    }

    override fun onCommandByConsole(sender: CommandSender, alias: String, strings: Array<String>) {
        sender.sendMessage("${ChatColor.RED}You may only use this command in-game")
    }

    override fun getCompletor(sender: CommandSender, length: Int, hint: String, strings: Array<String>): List<String> {
        if(sender !is Player || length != 2 || hint.isNotBlank()) return emptyList()
        return listOf("<number>")
    }
}
