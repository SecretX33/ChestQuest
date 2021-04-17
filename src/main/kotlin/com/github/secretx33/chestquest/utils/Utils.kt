package com.github.secretx33.chestquest.utils

import com.github.secretx33.chestquest.config.Const.PLUGIN_CHAT_PREFIX
import com.github.secretx33.chestquest.config.Const.PLUGIN_PERMISSION_PREFIX
import com.github.secretx33.chestquest.repository.ChestRepo
import com.github.secretx33.chestquest.utils.Utils.chestRepo
import com.github.secretx33.chestquest.utils.Utils.reflections
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.Container
import org.bukkit.command.CommandSender
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
object Utils: CustomKoinComponent {
    val chestRepo: ChestRepo by inject()
    val reflections: Reflections by inject()
}

@KoinApiExtension
fun Inventory.clone(): Inventory {
    val cloned = Bukkit.createInventory(holder, type)

    this.contents.forEachIndexed { slot, item: ItemStack? ->
        item?.let { cloned.setItem(slot, reflections.clone(item)) }
    }
    return cloned
}

fun CommandSender.message(msg: String) = this.sendMessage("$PLUGIN_CHAT_PREFIX $msg")

fun CommandSender.canEditQC() = hasPermission("$PLUGIN_PERMISSION_PREFIX.edit")

fun CommandSender.canResetProgress() = hasPermission("$PLUGIN_PERMISSION_PREFIX.resetprogress")

fun CommandSender.canReload() = hasPermission("$PLUGIN_PERMISSION_PREFIX.reload")

fun CommandSender.canToggleDebug() = hasPermission("$PLUGIN_PERMISSION_PREFIX.debug")

fun Inventory.isChest(): Boolean = type == InventoryType.CHEST

fun Inventory.locationByAllMeans(): Location? = location ?: holder.inventory.location ?: (holder as? Container)?.location

fun Block.isChest(): Boolean = type == Material.CHEST

@KoinApiExtension
fun Block.isQuestChest(): Boolean = isChest() && chestRepo.isQuestChest(location)

fun Block.coordinates(): String = "${location.x.toLong()} ${location.y.toLong()} ${location.z.toLong()}"

fun Location.prettyString(): String = "World: ${world?.name ?: "Unknown"}, ${x.toLong()}, ${y.toLong()}, ${z.toLong()}"
