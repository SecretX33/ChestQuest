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
import java.util.*

@KoinApiExtension
class SetOrderCommand : SubCommand(), CustomKoinComponent {

    override val name: String = "setorder"
    override val permission: String = "edit"
    override val aliases: List<String> = listOf(name, "seto", "so")

    private val chestRepo by inject<ChestRepo>()

    override fun onCommandByPlayer(player: Player, strings: Array<String>) {
        if(strings.size == 1) {
            player.sendMessage("${ChatColor.RED}Please type a number after ${ChatColor.GOLD}setorder${ChatColor.RED}. Command usage: /cq setorder <number>")
            return
        }
        try {
            val order = strings[1].toInt()
            if(order < 1) {
                player.sendMessage("${ChatColor.RED}Please type a number greater than 0. Command usage: /cq setorder <number>")
                return
            }
            player.getTargetBlock(null, 5)?.takeIf { it.isChest() }?.let { chest ->
                if(!chestRepo.isQuestChest(chest.location)) {
                    player.sendMessage("${ChatColor.RED}This chest is not a Quest Chest, you can't change its order")
                } else {
                    val oldOrder = chestRepo.changeOrderQuestChest(chest.location, order)
                    val msg = "Chest order is now ${ChatColor.RED}$order${ChatColor.WHITE} (previously $oldOrder)"
                    player.message(msg)
                }
            }
        } catch (e: NumberFormatException) {
            player.sendMessage("${ChatColor.RED}Please type only numbers after ${ChatColor.GOLD}setorder${ChatColor.RED}. Command usage: /cq setorder <number>")
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
        if (length == 2) {
            options.add("<number>")
        }
        return options.filter { it.startsWith(hint, ignoreCase = true) }
    }
}
