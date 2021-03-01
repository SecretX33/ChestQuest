package io.github.secretx33.chestquest.repository

import io.github.secretx33.chestquest.utils.Utils.debugMessage
import io.github.secretx33.chestquest.utils.clone
import org.bukkit.Location
import org.bukkit.block.Chest
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.koin.core.component.KoinApiExtension
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore
import kotlin.collections.HashSet

@KoinApiExtension
class ChestRepo {

    private val lock = Semaphore(1, true)
    private val chestContents: MutableMap<Pair<Location, UUID>, Inventory> = ConcurrentHashMap()
    private val questChests: MutableSet<Location> = HashSet()

    fun getChestContent(chest: Chest, player: Player): Inventory {
        val key = Pair(chest.location, player.uniqueId)
        return chestContents.getOrPut(key) { chest.inventory.clone() }
    }

    fun isQuestChest(location: Location): Boolean = runSync { questChests.contains(location) }

    fun addQuestChest(location: Location)  = runSync { questChests.add(location) }

    fun replaceQuestChests(locations: Collection<Location>) = runSync { questChests.addAll(locations) }

    fun removeQuestChest(location: Location) {
        runSync { questChests.remove(location) }
        chestContents.toMap().filterKeys { pair -> pair.first == location }.forEach { chestContents.remove(it.key) }
    }

    private fun <T> runSync(func: () -> T): T {
        try {
            lock.acquire()
            return func.invoke()
        } finally {
            lock.release()
        }
    }
}
