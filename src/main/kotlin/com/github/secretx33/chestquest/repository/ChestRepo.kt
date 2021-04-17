package com.github.secretx33.chestquest.repository

import com.github.secretx33.chestquest.database.SQLite
import com.github.secretx33.chestquest.utils.clone
import com.github.secretx33.chestquest.utils.formattedString
import com.github.secretx33.chestquest.utils.locationByAllMeans
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Chest
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.koin.core.component.KoinApiExtension
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

@KoinApiExtension
class ChestRepo(private val db: SQLite, private val log: Logger) {

    private val questChests   = ConcurrentHashMap<Location, Int>()
    private val chestContents = ConcurrentHashMap<Pair<Location, UUID>, Inventory>()

    init { loadDataFromDB() }

    private fun loadDataFromDB() = CoroutineScope(Dispatchers.Default).launch {
        questChests.clear()
        questChests.putAll(db.getAllQuestChestsAsync().await())
    }

    fun getChestContent(chest: Chest, player: Player): Inventory {
        val key = Pair(chest.location, player.uniqueId)
        return chestContents.getOrPut(key) {
            db.getChestContent(chest.location, player.uniqueId).also { println("Got inventory of ${player.name} from Database") }
                ?: chest.inventory.clone().also { inv -> db.addChestContent(chest.location, player.uniqueId, inv)
                    println("Got inventory of ${player.name} from Clone")
                }
        }
    }

    fun removeEntriesOf(player: Player) {
        println("chestContents before removing: ${chestContents.size} items")
        chestContents.filterKeys { (_, uuid) -> uuid == player.uniqueId }.let { chestContents.keys.removeAll(it.keys) }
        println("chestContents after removing: ${chestContents.size} items")
    }

    fun updateInventory(playerUuid: UUID, inventory: Inventory) = CoroutineScope(Dispatchers.Default).launch {
        var location = inventory.locationByAllMeans()
        if(location == null) {
            val loc = chestContents.entries.firstOrNull { (_, inv) -> inv === inventory }?.key?.first
            if(loc != null){
                log.warning("WARNING: Localization of chest inventory for player ${Bukkit.getPlayer(playerUuid)?.name} came from fallback 'search inventory on chestcontent' and it is $location")
                location = loc
            } else {
                log.severe("ERROR: Could not infer location of inventory, not even from chestContents, ${Bukkit.getPlayer(playerUuid)?.name}'s inventory was NOT saved")
                return@launch
            }
        }
        db.updateInventory(location, playerUuid, inventory)
    }

    fun isChestInventory(inv: Inventory): Boolean = chestContents.containsValue(inv)

    fun isQuestChest(location: Location): Boolean = questChests.containsKey(location)

    fun addQuestChest(location: Location, order: Int) {
        questChests[location] = order
        db.addQuestChest(location, order)
    }

    /**
     * Used to change a quest chest order
     * @param location Location The location of the chest having its order changed
     * @param newOrder Int The new order that is going to replace the old one
     * @return Int The old order
     */
    fun changeOrderQuestChest(location: Location, newOrder: Int): Int {
        questChests[location]!!.let { order ->
            if(order == newOrder) return order
            questChests[location] = newOrder
            db.updateChestOrder(location, newOrder)
            return order
        }
    }

    fun getQuestChestOrder(location: Location): Int = questChests[location] ?: throw NoSuchElementException("Chest ${location.formattedString()} was not found")

    fun removeQuestChest(location: Location) = CoroutineScope(Dispatchers.Default).launch {
        questChests.remove(location)
        chestContents.filterKeys { it.first == location }.let { chestContents.keys.removeAll(it.keys) }
        db.removeQuestChest(location)
    }
}
