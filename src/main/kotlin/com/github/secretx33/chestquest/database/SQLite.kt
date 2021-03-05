package com.github.secretx33.chestquest.database

import com.github.secretx33.chestquest.config.Config
import com.github.secretx33.chestquest.utils.Utils.consoleMessage
import com.github.secretx33.chestquest.utils.Utils.debugMessage
import com.github.secretx33.chestquest.utils.prettyString
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.*
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.block.Container
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension
import java.nio.file.FileSystems
import java.sql.Connection
import java.sql.SQLException
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.HashMap
import kotlin.collections.HashSet

@KoinApiExtension
class SQLite(plugin: Plugin) {

    private val url = "jdbc:sqlite:${plugin.dataFolder.absolutePath}${folderSeparator}database.db"
    private val ds = HikariDataSource(hikariConfig.apply { jdbcUrl = url })

    init { initialize() }

    fun close() = ds.close()

    private fun initialize() {
        try {
            ds.connection.use { conn: Connection ->
                conn.prepareStatement(CREATE_QUEST_CHESTS).execute()
                conn.prepareStatement(CREATE_CHEST_CONTENT).execute()
                conn.prepareStatement(CREATE_PLAYER_PROGRESS).execute()
                conn.prepareStatement(CREATE_TRIGGER).execute()
                conn.commit()
                debugMessage("Initiated DB")
            }
        } catch (e: SQLException) {
            consoleMessage("${ChatColor.RED}ERROR: An exception occurred while trying to connect to the database and create the tables")
            e.printStackTrace()
        }
    }

    // ADD

    fun addQuestChest(chestLoc: Location, chestOrder: Int) = CoroutineScope(Dispatchers.IO).launch {
        require(chestOrder >= 1) { "Chest order cannot be less than 1, actual value is $chestOrder" }
        try {
            ds.connection.use { conn: Connection ->
                val temp = jsonLoc.toJson(chestLoc)
                val prep = conn.prepareStatement(INSERT_QUEST_CHEST).apply {
                    setString(1, temp)
                }
                prep.execute()
                conn.commit()
            }
        } catch (e: SQLException) {
            consoleMessage("${ChatColor.RED}ERROR: An exception occurred while adding a specific Quest Chest (${chestLoc.prettyString()})")
            e.printStackTrace()
        }
    }

    fun addChestContent(chestLoc: Location, playerUuid: UUID, inv: Inventory) = CoroutineScope(Dispatchers.IO).launch {
        try {
            ds.connection.use { conn: Connection ->
                val serializedInv = jsonInv.toJson(inv)
                debugMessage("Inventory is: $serializedInv")
                val prep = conn.prepareStatement(INSERT_CHEST_CONTENTS).apply {
                    setString(1, jsonLoc.toJson(chestLoc))
                    setString(2, playerUuid.toString())
                    setString(3, serializedInv)
                }
                prep.execute()
                conn.commit()
            }
        } catch (e: SQLException) {
            consoleMessage("${ChatColor.RED}ERROR: An exception occurred while trying to add a content of the chest ${chestLoc.prettyString()} to the database")
            e.printStackTrace()
        }
    }

    fun addPlayerProgress(playerUuid: UUID, progress: Int) = CoroutineScope(Dispatchers.IO).launch {
        require(progress >= 0) { "Progress has to be at least 0. Actual value if $progress" }
        try {
            ds.connection.use { conn: Connection ->
                val prep = conn.prepareStatement(INSERT_PLAYER_PROGRESS).apply {
                    setString(1, playerUuid.toString())
                    setInt(2, progress)
                }
                prep.execute()
                conn.commit()
            }
        } catch (e: SQLException) {
            consoleMessage("${ChatColor.RED}ERROR: An exception occurred while trying to add player ${Bukkit.getPlayer(playerUuid)?.name ?: "Unknown"} ($playerUuid) progress of $progress to the database")
            e.printStackTrace()
        }
    }

    // REMOVE

    fun removeQuestChest(chestLoc: Location) = CoroutineScope(Dispatchers.IO).launch {
        try {
            ds.connection.use { conn: Connection ->
                val prep = conn.prepareStatement(REMOVE_QUEST_CHEST).apply {
                    setString(1, jsonLoc.toJson(chestLoc))
                }
                prep.execute()
                conn.commit()
            }
        } catch (e: SQLException) {
            consoleMessage("${ChatColor.RED}ERROR: An exception occurred while trying to remove a Quest Chest from the database")
            e.printStackTrace()
        }
    }

    private fun removeQuestChestsByWorldUuid(worldUuids: Iterable<String>) = CoroutineScope(Dispatchers.IO).launch {
        try {
            ds.connection.use { conn: Connection ->
                val prep = conn.prepareStatement(REMOVE_QUEST_CHESTS_OF_WORLD)
                worldUuids.forEach {
                    prep.apply {
                        setString(1, "%$it%")
                        addBatch()
                    }
                }
                prep.executeBatch()
                conn.commit()
            }
        } catch (e: SQLException) {
            consoleMessage("${ChatColor.RED}ERROR: An exception occurred while trying to buck remove all quest chests from worlds")
            e.printStackTrace()
        }
    }

    private fun removeQuestChestsByLocation(locations: Iterable<Location>) = CoroutineScope(Dispatchers.IO).launch {
        try {
            ds.connection.use { conn: Connection ->
                val prep = conn.prepareStatement(REMOVE_QUEST_CHEST)
                locations.forEach { loc ->
                    prep.apply {
                        setString(1, jsonLoc.toJson(loc))
                        addBatch()
                    }
                }
                prep.executeBatch()
                conn.commit()
            }
        } catch (e: SQLException) {
            consoleMessage("${ChatColor.RED}ERROR: An exception occurred while trying to remove a list of quest chests")
            e.printStackTrace()
        }
    }

    fun removePlayerProgress(playerUuid: UUID) = CoroutineScope(Dispatchers.IO).launch {
        try {
            ds.connection.use { conn: Connection ->
                val prep = conn.prepareStatement(REMOVE_PLAYER_PROGRESS).apply {
                    setString(1, playerUuid.toString())
                }
                prep.execute()
                conn.commit()
            }
        } catch (e: SQLException) {
            consoleMessage("${ChatColor.RED}ERROR: An exception occurred while trying to remove a Quest Chest from the database")
            e.printStackTrace()
        }
    }

    // GET

    fun getChestContent(chestLoc: Location, playerUuid: UUID): Inventory? {
        try {
            ds.connection.use { conn: Connection ->
                val prep = conn.prepareStatement(SELECT_CHEST_CONTENT).apply {
                    setString(1, jsonLoc.toJson(chestLoc))
                    setString(2, playerUuid.toString())
                }
                val rs = prep.executeQuery()
                if(rs.next()){
                    val inv = jsonInv.fromJson(rs.getString("inventory"))
                    when {
                        inv == null -> {
                            consoleMessage("${ChatColor.RED}While trying to get the chestContent of Player ${Bukkit.getPlayer(playerUuid)?.name ?: "Unknown"} ($playerUuid) in ${chestLoc.prettyString()}, inventory came null, report this to SecretX!")
                        }
                        inv.holder == null -> {
                            consoleMessage("${ChatColor.RED}While trying to get the chestContent of Player ${Bukkit.getPlayer(playerUuid)?.name ?: "Unknown"} ($playerUuid) in ${chestLoc.prettyString()}, holder came null, report this to SecretX!")
                        }
                        else -> return inv
                    }
                } else {
                    return null
                }
            }
        } catch (e: SQLException) {
            consoleMessage("${ChatColor.RED}ERROR: An exception occurred while trying to get inventory of chest at ${chestLoc.prettyString()} from database")
            e.printStackTrace()
        }
        return newEmptyInventory()
    }

    fun getAllQuestChestsAsync(): Deferred<Map<Location, Int>> = CoroutineScope(Dispatchers.IO).async {
        val chestMap = HashMap<Location, Int>()
        val worldRemoveSet = HashSet<String>()
        val chestRemoveSet = HashSet<Location>()
        try {
            ds.connection.use { conn: Connection ->
                val rs = conn.prepareStatement(SELECT_ALL_FROM_QUEST_CHEST).executeQuery()
                while(rs.next()){
                    val chestLoc = jsonLoc.fromJson(rs.getString("location"))!!
                    if(chestLoc.world == null && Config.removeDBEntriesIfWorldIsMissing){
                        UUID_WORLD_PATTERN.matcher(rs.getString("location")).replaceFirst("$1")?.let {
                            worldRemoveSet.add(it)
                        }
                    } else if(chestLoc.world != null) {
                        if (chestLoc.world.getBlockAt(chestLoc).state !is Container) {
                            consoleMessage("${ChatColor.RED}WARNING: The chest located at '${chestLoc.prettyString()}' was not found, queuing its removal to preserve DB integrity.${ChatColor.WHITE} Usually this happens when a Quest Chest is broken with this plugin being disabled or missing.")
                            chestRemoveSet.add(chestLoc)
                        } else {
                            chestMap[chestLoc] = rs.getInt("chest_order")
                        }
                    }
                }
                if(worldRemoveSet.isNotEmpty()){
                    worldRemoveSet.forEach { consoleMessage("${ChatColor.RED}WARNING: The world with UUID '$it' was not found, removing ALL chests and inventories linked to it") }
                    removeQuestChestsByWorldUuid(worldRemoveSet)
                }
                if(chestRemoveSet.isNotEmpty())
                    removeQuestChestsByLocation(chestRemoveSet)
            }
        } catch (e: SQLException) {
            consoleMessage("${ChatColor.RED}ERROR: An exception occurred while trying to connect to the database")
            e.printStackTrace()
        }
        chestMap
    }

    /**
     * Get all entries of database for Player Progress table, which stores player progress in the quest chain
     * @return Deferred<Map<UUID, Int>> UUID is the player UUID, and Int is his progress in the quest chain, used to prevent him of opening chests that have higher number than the player progress + 1
     */
    fun getAllPlayerProgressAsync(): Deferred<Map<UUID, Int>> = CoroutineScope(Dispatchers.IO).async {
        val map = HashMap<UUID, Int>()
        try {
            ds.connection.use { conn: Connection ->
                val rs = conn.prepareStatement(SELECT_ALL_FROM_PLAYER_PROGRESS).executeQuery()
                while(rs.next()){
                    val key = UUID.fromString(rs.getString("player_uuid"))
                    val value = rs.getInt("progress")
                    map[key] = value
                }
            }
        } catch (e: SQLException) {
            consoleMessage("${ChatColor.RED}ERROR: An exception occurred while trying to get all player progress from database async")
            e.printStackTrace()
        }
        map
    }

    fun getPlayerProgress(playerUuid: UUID): Int? {
        try {
            ds.connection.use { conn: Connection ->
                val prep = conn.prepareStatement(SELECT_PLAYER_PROCESS).apply {
                    setString(1, playerUuid.toString())
                }
                val rs = prep.executeQuery()
                if(rs.next()){
                    return rs.getInt("progress")
                }
            }
        } catch (e: SQLException) {
            consoleMessage("${ChatColor.RED}ERROR: An exception occurred while trying to get progress of player ${Bukkit.getPlayer(playerUuid)?.name} ($playerUuid) from database")
            e.printStackTrace()
        }
        return null
    }

    // UPDATE

    fun updateInventory(chestLoc: Location, playerUuid: UUID, inv: Inventory) = CoroutineScope(Dispatchers.IO).launch {
        try {
            ds.connection.use { conn: Connection ->
                val prep = conn.prepareStatement(UPDATE_CHEST_CONTENTS).apply {
                    setString(1, jsonInv.toJson(inv))
                    setString(2, jsonLoc.toJson(chestLoc))
                    setString(3, playerUuid.toString())
                }
                prep.execute()
                conn.commit()
            }
        } catch (e: SQLException) {
            consoleMessage("${ChatColor.RED}ERROR: An exception occurred while updating an inventory to the database")
            e.printStackTrace()
        }
    }

    fun updateChestOrder(location: Location, newOrder: Int) = CoroutineScope(Dispatchers.IO).launch {
        try {
            ds.connection.use { conn: Connection ->
                val prep = conn.prepareStatement(UPDATE_QUEST_CHEST_ORDER).apply {
                    setInt(1, newOrder)
                    setString(2, jsonLoc.toJson(location))
                }
                prep.execute()
                conn.commit()
            }
        } catch (e: SQLException) {
            consoleMessage("${ChatColor.RED}ERROR: An exception occurred while updating chest order of ${location.prettyString()} to the database")
            e.printStackTrace()
        }
    }

    fun updatePlayerProgress(playerUuid: UUID, progress: Int) = CoroutineScope(Dispatchers.IO).launch {
        try {
            ds.connection.use { conn: Connection ->
                val prep = conn.prepareStatement(UPDATE_PLAYER_PROGRESS).apply {
                    setInt(1, progress)
                    setString(2, playerUuid.toString())
                }
                prep.execute()
                conn.commit()
            }
        } catch (e: SQLException) {
            consoleMessage("${ChatColor.RED}ERROR: An exception occurred while trying to add player ${Bukkit.getPlayer(playerUuid)?.name ?: "Unknown"} ($playerUuid) progress of $progress to the database")
            e.printStackTrace()
        }
    }

    private fun newEmptyInventory(): Inventory = Bukkit.createInventory(null, InventoryType.CHEST)

    private companion object {
        val moshi: Moshi = Moshi.Builder()
            .add(LocationSerializer())
            .add(InventorySerializer())
            .build()
        val jsonLoc: JsonAdapter<Location> = moshi.adapter(Location::class.java)
        val jsonInv: JsonAdapter<Inventory> = moshi.adapter(Inventory::class.java)

        val folderSeparator: String = FileSystems.getDefault().separator
        val hikariConfig = HikariConfig().apply { isAutoCommit = false }

        // create tables
        const val CREATE_QUEST_CHESTS = "CREATE TABLE IF NOT EXISTS questChests(location VARCHAR(150), chest_order INTEGER NOT NULL PRIMARY KEY);"
        const val CREATE_CHEST_CONTENT = "CREATE TABLE IF NOT EXISTS chestContents(id INTEGER PRIMARY KEY, chest_location INTEGER NOT NULL, player_uuid VARCHAR(60) NOT NULL, inventory VARCHAR(500000) NOT NULL, FOREIGN KEY(chest_location) REFERENCES questChests(location));"
        const val CREATE_PLAYER_PROGRESS = "CREATE TABLE IF NOT EXISTS playerProgress(player_uuid VARCHAR(60) NOT NULL PRIMARY KEY, progress INTEGER NOT NULL);"
        const val CREATE_TRIGGER = "CREATE TRIGGER IF NOT EXISTS removeInventories BEFORE DELETE ON questChests FOR EACH ROW BEGIN DELETE FROM chestContents WHERE chestContents.chest_location = OLD.location; END"
        // selects
        const val SELECT_ALL_FROM_QUEST_CHEST = "SELECT * FROM questChests;"
        const val SELECT_ALL_FROM_PLAYER_PROGRESS = "SELECT * FROM playerProgress;"
        const val SELECT_CHEST_CONTENT = "SELECT inventory FROM chestContents WHERE chest_location = ? AND player_uuid = ? LIMIT 1;"
        const val SELECT_PLAYER_PROCESS = "SELECT progress FROM playerProgress WHERE player_uuid = ? LIMIT 1;"
        // inserts
        const val INSERT_QUEST_CHEST = "INSERT INTO questChests(location, chest_order) VALUES (?, ?);"
        const val INSERT_CHEST_CONTENTS = "INSERT INTO chestContents(chest_location, player_uuid, inventory) VALUES (?, ?, ?);"
        const val INSERT_PLAYER_PROGRESS = "INSERT INTO playerProgress(player_uuid, progress) VALUES (?, ?);"
        // updates
        const val UPDATE_QUEST_CHEST_ORDER = "UPDATE questChests SET chest_order = ? WHERE location = ?;"
        const val UPDATE_CHEST_CONTENTS = "UPDATE chestContents SET inventory = ? WHERE chest_location = ? AND player_uuid = ?;"
        const val UPDATE_PLAYER_PROGRESS = "UPDATE playerProgress SET progress = ? WHERE player_uuid = ?;"
        // removes
        const val REMOVE_QUEST_CHESTS_OF_WORLD = "DELETE FROM questChests WHERE location LIKE ?;"
        const val REMOVE_QUEST_CHEST = "DELETE FROM questChests WHERE location = ?;"
        const val REMOVE_PLAYER_PROGRESS = "DELETE FROM playerProgress WHERE player_uuid = ?;"

        val UUID_WORLD_PATTERN: Pattern = Pattern.compile("""^"\{\\"world\\":\\"([0-9a-zA-Z-]+).*""")
    }
}
