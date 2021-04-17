package com.github.secretx33.chestquest.repository

import com.github.secretx33.chestquest.database.SQLite
import com.github.secretx33.chestquest.utils.clone
import com.github.secretx33.chestquest.utils.formattedString
import com.github.secretx33.chestquest.utils.locationByAllMeans
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.Location
import org.bukkit.block.Chest
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.koin.core.component.KoinApiExtension
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@KoinApiExtension
class ChestRepo(private val db: SQLite) {

    private val chestContents: MutableMap<Pair<Location, UUID>, Inventory> = ConcurrentHashMap()
    private val questChests: MutableMap<Location, Int> = ConcurrentHashMap()

    init { loadDataFromDB() }

    private fun loadDataFromDB() = CoroutineScope(Dispatchers.Default).launch {
        questChests.clear()
        questChests.putAll(db.getAllQuestChestsAsync().await())
    }

    fun getChestContent(chest: Chest, player: Player): Inventory {
        val key = Pair(chest.location, player.uniqueId)
        return chestContents.getOrPut(key) {
//            db.getChestContent(chest.location, player.uniqueId) ?: chest.inventory.clone().also { inv -> db.addChestContent(chest.location, player.uniqueId, inv) }
            val dbEntry = db.getChestContent(chest.location, player.uniqueId)
            if(dbEntry != null){
                println("Got inventory of ${player.name} from Database")
                dbEntry
            } else {
                println("Got inventory of ${player.name} from Clone")
                chest.inventory.clone().also { inv -> db.addChestContent(chest.location, player.uniqueId, inv) }
            }
        }
    }

    fun removeEntriesOf(playerUuid: UUID) = CoroutineScope(Dispatchers.Default).launch {
        chestContents.filterKeys { (_, uuid) -> uuid == playerUuid }.let { chestContents.keys.removeAll(it.keys) }
    }

    fun updateInventory(playerUuid: UUID, inventory: Inventory) = CoroutineScope(Dispatchers.Default).launch {
        var location = inventory.locationByAllMeans()
        if(location == null) {
            val loc = chestContents.entries.firstOrNull { (_, inv) -> inv === inventory }?.key?.first
            if(loc != null){
                println("INFO: Localization came from 'transformation on chestContents' and it is $location")
                location = loc
            } else {
                println("WARNING: Could not infer location of inventory not even from chestContents, inventory was NOT saved")
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
