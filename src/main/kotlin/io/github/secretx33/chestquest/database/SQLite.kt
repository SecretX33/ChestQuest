package io.github.secretx33.chestquest.database

import com.squareup.moshi.Moshi
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.secretx33.chestquest.config.Config
import io.github.secretx33.chestquest.utils.Utils.consoleMessage
import io.github.secretx33.chestquest.utils.Utils.debugMessage
import kotlinx.coroutines.*
import org.bukkit.ChatColor
import org.bukkit.Location
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
                conn.prepareStatement(CREATE_TRIGGER).execute()
                conn.commit()
                debugMessage("Initiated DB")
            }
        } catch (e: SQLException) {
            consoleMessage("${ChatColor.RED}An exception occurred while trying to connect to the database")
            e.printStackTrace()
        }
    }

    // ADD

    fun addQuestChest(chestLoc: Location) = CoroutineScope(Dispatchers.IO).launch {
        try {
            ds.connection.use { conn: Connection ->
                val temp = jsonLoc.toJson(chestLoc)
                debugMessage("My Gson is $temp")
                val prep = conn.prepareStatement(INSERT_QUEST_CHEST).apply {
                    setString(1, temp)
                }
                prep.execute()
                conn.commit()
            }
        } catch (e: SQLException) {
            consoleMessage("${ChatColor.RED}An exception occurred while trying to connect to the database")
            e.printStackTrace()
        }
    }

    fun addChestContent(chestLoc: Location, playerUuid: UUID, inv: Inventory) = CoroutineScope(Dispatchers.IO).launch {
        try {
            ds.connection.use { conn: Connection ->
                val serializedInv = jsonInv.toJson(inv)!!
                debugMessage("Inventory is: $serializedInv")
                val prep = conn.prepareStatement(INSERT_CHEST_CONTENTS).apply {
                    setString(1, jsonLoc.toJson(chestLoc)!!)
                    setString(2, playerUuid.toString())
                    setString(3, serializedInv)
                }
                prep.execute()
                conn.commit()
            }
        } catch (e: SQLException) {
            consoleMessage("${ChatColor.RED}An exception occurred while trying to connect to the database")
            e.printStackTrace()
        }
    }

    // REMOVE

    fun removeQuestChest(chestLoc: Location) = CoroutineScope(Dispatchers.IO).launch {
        try {
            ds.connection.use { conn: Connection ->
                val prep = conn.prepareStatement(REMOVE_QUEST_CHEST).apply {
                    setString(1, jsonLoc.toJson(chestLoc)!!)
                }
                prep.execute()
                conn.commit()
            }
        } catch (e: SQLException) {
            consoleMessage("${ChatColor.RED}An exception occurred while trying to connect to the database")
            e.printStackTrace()
        }
    }

    private fun removeQuestChests(worldUuids: Iterable<String>) = CoroutineScope(Dispatchers.IO).launch {
        try {
            ds.connection.use { conn: Connection ->
                worldUuids.forEach {
                    val prep = conn.prepareStatement(REMOVE_CHEST_QUESTS_OF_WORLD).apply {
                        setString(1, "%$it%")
                    }
                    prep.execute()
                }
                conn.commit()
            }
        } catch (e: SQLException) {
            consoleMessage("${ChatColor.RED}An exception occurred while trying to connect to the database")
            e.printStackTrace()
        }
    }

    // GET

    fun getChestContent(chestLoc: Location, playerUuid: UUID): Inventory? {
        try {
            ds.connection.use { conn: Connection ->
                val prep = conn.prepareStatement(SELECT_CHEST_CONTENT).apply {
                    setString(1, jsonLoc.toJson(chestLoc)!!)
                    setString(2, playerUuid.toString())
                }
                val rs = prep.executeQuery()
                if(rs.next()){
                    val inv = rs.getString("inventory")
                    debugMessage("Inventory from DB is: $inv")
                    return jsonInv.fromJson(inv)
                }
            }
        } catch (e: SQLException) {
            consoleMessage("${ChatColor.RED}An exception occurred while trying to connect to the database")
            e.printStackTrace()
        }
        return null
    }

    fun getAllQuestChestsAsync(): Deferred<Set<Location>> = CoroutineScope(Dispatchers.IO).async {
        val set = HashSet<Location>()
        val removeSet = HashSet<String>()
        try {
            ds.connection.use { conn: Connection ->
                val rs = conn.prepareStatement(SELECT_ALL_FROM_QUEST_CHEST).executeQuery()
                while(rs.next()){
                    val chestLoc = jsonLoc.fromJson(rs.getString("location"))!!
                    debugMessage("Loaded chest at $chestLoc")
                    if(chestLoc.world != null){
                        set.add(chestLoc)
                    } else if (Config.removeDBEntriesIfWorldIsMissing) {
                        debugMessage("Null world detected")
                        debugMessage(rs.getString("location"))
                        UUID_WORLD_PATTERN.matcher(rs.getString("location")).replaceFirst("$1")?.let {
                            removeSet.add(it)
                            debugMessage("Added UUID is $it")
                        }
                    }
                }
                if(removeSet.isNotEmpty()){
                    removeSet.forEach { consoleMessage("${ChatColor.RED}WARNING: The world with UUID '$it' was not found, removing ALL chests and inventories linked to it") }
                    removeQuestChests(removeSet)
                }
            }
        } catch (e: SQLException) {
            consoleMessage("${ChatColor.RED}An exception occurred while trying to connect to the database")
            e.printStackTrace()
        }
        set
    }

    fun getAllChestContentsAsync(): Deferred<Map<Pair<Location, UUID>, Inventory>> = CoroutineScope(Dispatchers.IO).async {
        val map = HashMap<Pair<Location, UUID>, Inventory>()
        try {
            ds.connection.use { conn: Connection ->
                val rs = conn.prepareStatement(SELECT_ALL_FROM_CHEST_CONTENT).executeQuery()
                while(rs.next()){
                    val key = Pair(jsonLoc.fromJson(rs.getString("chest_location"))!!, UUID.fromString(rs.getString("player_uuid")))
                    val value = jsonInv.fromJson(rs.getString("inventory"))!!
                    map[key] = value
                }
            }
        } catch (e: SQLException) {
            consoleMessage("${ChatColor.RED}An exception occurred while trying to connect to the database")
            e.printStackTrace()
        }
        map
    }

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
            consoleMessage("${ChatColor.RED}An exception occurred while trying to connect to the database")
            e.printStackTrace()
        }
    }

    private companion object {
        val moshi = Moshi.Builder()
            .add(LocationSerializer())
            .add(InventorySerializer())
            .build()
        val jsonLoc = moshi.adapter(Location::class.java)
        val jsonInv = moshi.adapter(Inventory::class.java)

        val folderSeparator: String = FileSystems.getDefault().separator
        val hikariConfig = HikariConfig().apply { isAutoCommit = false }

        // create tables
        const val CREATE_QUEST_CHESTS = "CREATE TABLE IF NOT EXISTS questChests(location VARCHAR(150) PRIMARY KEY);"
        const val CREATE_CHEST_CONTENT = "CREATE TABLE IF NOT EXISTS chestContents(id INTEGER PRIMARY KEY, chest_location INTEGER NOT NULL, player_uuid VARCHAR(50) NOT NULL, inventory VARCHAR(20000) NOT NULL, FOREIGN KEY(chest_location) REFERENCES questChests(location));"
        const val CREATE_TRIGGER = "CREATE TRIGGER IF NOT EXISTS removeInventories BEFORE DELETE ON questChests FOR EACH ROW BEGIN DELETE FROM chestContents WHERE chestContents.chest_location = OLD.location; END"
        // selects
        const val SELECT_ALL_FROM_QUEST_CHEST = "SELECT * FROM questChests;"
        const val SELECT_ALL_FROM_CHEST_CONTENT = "SELECT chest_location, player_uuid, inventory FROM chestContents;"
        const val SELECT_CHEST_CONTENT = "SELECT inventory FROM chestContents WHERE chest_location = ? AND player_uuid = ? LIMIT 1;"
        // inserts
        const val INSERT_QUEST_CHEST = "INSERT INTO questChests(location) VALUES (?);"
        const val INSERT_CHEST_CONTENTS = "INSERT INTO chestContents(chest_location, player_uuid, inventory) VALUES (?, ?, ?);"
        // updates
        const val UPDATE_CHEST_CONTENTS = "UPDATE chestContents SET inventory = ? WHERE chest_location = ? AND player_uuid = ?;"
        // removes
        const val REMOVE_CHEST_QUESTS_OF_WORLD = """DELETE FROM questChests WHERE location LIKE ?;""" // change this later
        const val REMOVE_QUEST_CHEST = "DELETE FROM questChests WHERE location = ?;"

        val UUID_WORLD_PATTERN = Pattern.compile("""^"\{\\"world\\":\\"([0-9a-zA-Z-]+).*""")
    }
}
