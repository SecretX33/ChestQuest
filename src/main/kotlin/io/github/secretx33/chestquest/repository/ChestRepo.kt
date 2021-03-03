package io.github.secretx33.chestquest.repository

import io.github.secretx33.chestquest.database.SQLite
import io.github.secretx33.chestquest.utils.Utils.debugMessage
import io.github.secretx33.chestquest.utils.clone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.Location
import org.bukkit.block.Chest
import org.bukkit.block.Container
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.koin.core.component.KoinApiExtension
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@KoinApiExtension
class ChestRepo(private val db: SQLite) {

    private val chestContents: MutableMap<Pair<Location, UUID>, Inventory> = ConcurrentHashMap()
    private val questChests: MutableSet<Location> = ConcurrentHashMap.newKeySet()

    init {
        loadDataFromDB()
//        periodicCleanup()
    }

    private fun loadDataFromDB() = CoroutineScope(Dispatchers.IO).launch {
        questChests.addAll(db.getAllQuestChestsAsync().await())
    }

    /*private fun periodicCleanup() = CoroutineScope(Dispatchers.Default).launch {
        while(true){
            delay(1000 * 60 * 1)
            GlobalScope.launch {
                debugMessage("[ChestRepo] Cleaned up inventory of offline players")
                val onlinePlayers = Bukkit.getOnlinePlayers().toList().map { it.uniqueId }
                chestContents.toMap().filterKeys { !onlinePlayers.contains(it.second) }.forEach { (k, _) -> chestContents.remove(k) }
            }
        }
    }*/

    fun getChestContent(chest: Chest, player: Player): Inventory {
        val key = Pair(chest.location, player.uniqueId)
        return chestContents.getOrPut(key) {
//            db.getChestContent(chest.location, player.uniqueId) ?: chest.inventory.clone().also { db.addChestContent(chest.location, player.uniqueId, it) }

            val dbEntry = db.getChestContent(chest.location, player.uniqueId)
            if(dbEntry != null){
                debugMessage("Got inventory of ${player.name} from Database")
                dbEntry
            } else {
                debugMessage("Got inventory of ${player.name} from clone")
                chest.inventory.clone().also { db.addChestContent(chest.location, player.uniqueId, it) }
            }
        }
    }

    fun removeEntriesOf(playerUuid: UUID) = CoroutineScope(Dispatchers.Default).launch {
        chestContents.toMap().filterKeys { (_, uuid) -> uuid == playerUuid }.forEach { (key, _) -> chestContents.remove(key) }
    }

    fun updateInventory(playerUuid: UUID, inventory: Inventory) = CoroutineScope(Dispatchers.IO).launch {
        var location = (inventory.holder as? Container)?.location
        if(location == null) {
            location = chestContents.toMap().filter { (_, inv) -> inv === inventory }.map { (k,_) -> k.first }[0]
            debugMessage("Localization came from 'transformation on chestContents' and it is $location")
        }
        db.updateInventory(location, playerUuid, inventory)
    }

    fun isChestInventory(inv: Inventory): Boolean = chestContents.containsValue(inv)

    fun isQuestChest(location: Location): Boolean = questChests.contains(location)

    fun addQuestChest(location: Location) {
        questChests.add(location)
        db.addQuestChest(location)
    }

    fun removeQuestChest(location: Location) = CoroutineScope(Dispatchers.Default).launch {
        questChests.remove(location)
        chestContents.toMap().filterKeys { pair -> pair.first == location }.forEach { chestContents.remove(it.key) }
        db.removeQuestChest(location)
    }
}
