/*
This file is part of GoldenLasso.

GoldenLasso is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

GoldenLasso is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GoldenLasso.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.secretx33.chestquest.utils

import io.github.secretx33.chestquest.config.Config
import io.github.secretx33.chestquest.config.Const.PLUGIN_CHAT_PREFIX
import io.github.secretx33.chestquest.config.Const.PLUGIN_PERMISSION_PREFIX
import io.github.secretx33.chestquest.repository.ChestRepo
import io.github.secretx33.chestquest.utils.Utils.chestRepo
import io.github.secretx33.chestquest.utils.Utils.reflections
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.Chest
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.koin.core.component.KoinApiExtension
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject

@KoinApiExtension
object Utils: KoinComponent {
    private val console: ConsoleCommandSender = get()
    val chestRepo: ChestRepo = get()
    val reflections: Reflections by inject()

    fun consoleMessage(msg: String) {
        console.sendMessage("$PLUGIN_CHAT_PREFIX $msg")
    }

    fun debugMessage(msg: String) {
        if(Config.debug) console.sendMessage("$PLUGIN_CHAT_PREFIX $msg")
    }
}

@KoinApiExtension
fun Inventory.clone(): Inventory {
    val cloned = Bukkit.createInventory(null, type)

    this.contents.forEachIndexed { slot, item: ItemStack? ->
        item?.let { cloned.setItem(slot, reflections.clone(item)) }
    }
    return cloned
}

@KoinApiExtension
fun Chest.clone(): Inventory = this.inventory.clone()

fun Player.message(msg: String) = this.sendMessage("$PLUGIN_CHAT_PREFIX $msg")

fun CommandSender.canOpenQC() = hasPermission("$PLUGIN_PERMISSION_PREFIX.open")

fun CommandSender.canEditQC() = hasPermission("$PLUGIN_PERMISSION_PREFIX.edit")

fun CommandSender.canReload() = hasPermission("$PLUGIN_PERMISSION_PREFIX.reload")

fun CommandSender.canToggleDebug() = hasPermission("$PLUGIN_PERMISSION_PREFIX.debug")

fun Block.isChest(): Boolean = type == Material.CHEST

fun Block.isQuestChest(): Boolean = isChest() && chestRepo.isQuestChest(location)
