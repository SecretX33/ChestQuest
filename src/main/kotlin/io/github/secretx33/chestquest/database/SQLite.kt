package io.github.secretx33.chestquest.database

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.secretx33.chestquest.config.Config
import io.github.secretx33.chestquest.utils.Utils.consoleMessage
import io.github.secretx33.chestquest.utils.Utils.debugMessage
import kotlinx.coroutines.*
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.inventory.Inventory
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension
import java.lang.reflect.Type
import java.sql.Connection
import java.sql.SQLData
import java.sql.SQLException
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet

@KoinApiExtension
class SQLite(plugin: Plugin) {

    private val url = "jdbc:sqlite:${plugin.dataFolder.absolutePath.replace("\\","/")}/database.db"
    private val ds = HikariDataSource(hikariConfig.apply { jdbcUrl = url })

    init {
        consoleMessage("URL from db is $url")
        initialize()
    }

    fun close() = ds.close()

    private fun initialize() {
        try {
            ds.connection.use { conn: Connection ->
                consoleMessage("The driver name is " + conn.metaData.driverName)
                conn.prepareStatement(CREATE_QUEST_CHESTS).execute()
                conn.prepareStatement(CREATE_CHEST_CONTENT).execute()
                conn.prepareStatement(CREATE_TRIGGER).execute()
                conn.commit()
                consoleMessage("A new database has been created.")
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
                val temp = gson.toJson(chestLoc)
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
                val prep = conn.prepareStatement(INSERT_CHEST_CONTENTS).apply {
                    setString(1, gson.toJson(chestLoc))
                    setString(2, playerUuid.toString())
                    setString(3, gson.toJson(inv))
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
                    setString(1, gson.toJson(chestLoc))
                }
                prep.execute()
                conn.commit()
            }
        } catch (e: SQLException) {
            consoleMessage("${ChatColor.RED}An exception occurred while trying to connect to the database")
            e.printStackTrace()
        }
    }

    private fun removeQuestChests(list: Iterable<String>) = CoroutineScope(Dispatchers.IO).launch {
        try {
            ds.connection.use { conn: Connection ->
                list.forEach {
                    val prep = conn.prepareStatement(REMOVE_CHEST_QUESTS_OF_WORLD).apply {
                        setString(1, it)
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
                    setString(1, gson.toJson(chestLoc))
                    setString(2, playerUuid.toString())
                }
                val rs = prep.executeQuery()
                if(rs.next()){
                    return gson.fromJson<Inventory>(rs.getString("inventory"), invTypeToken)
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
                    val world = Bukkit.getWorld(UUID.fromString(rs.getString("world")))
                    if(world != null){
                        val location = Location(
                            world,
                            rs.getInt("x").toDouble(),
                            rs.getInt("y").toDouble(),
                            rs.getInt("z").toDouble()
                        )
                        set.add(location)
                    } else if (Config.removeDBEntriesIfWorldIsMissing) {
                        removeSet.add(rs.getString("world"))
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
                    val key = Pair(gson.fromJson<Location>(rs.getString("chest_location"), locTypeToken), UUID.fromString(rs.getString("player_uuid")))
                    val value = gson.fromJson<Inventory>(rs.getString("inventory"), invTypeToken)
                    map[key] = value
                }
            }
        } catch (e: SQLException) {
            consoleMessage("${ChatColor.RED}An exception occurred while trying to connect to the database")
            e.printStackTrace()
        }
        map
    }

    fun updateInventory(chestLoc: Location, playerUuid: UUID, inv: Inventory){
        try {
            ds.connection.use { conn: Connection ->
                val prep = conn.prepareStatement(UPDATE_CHEST_CONTENTS).apply {
                    setString(1, gson.toJson(inv))
                    setString(2, gson.toJson(chestLoc))
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
        val gson = GsonBuilder()
            .registerTypeAdapter(Location::class.java, LocationSerializer())
            .registerTypeAdapter(Inventory::class.java, InventorySerializer())
            .create()
//        val folderSeparator: String = FileSystems.getDefault().separator
        val hikariConfig = HikariConfig().apply {
            dataSourceClassName = "org.sqlite.SQLiteDataSource"
            isAutoCommit = false
//            addDataSourceProperty("cachePrepStmts", "true")
//            addDataSourceProperty("prepStmtCacheSize", "100")
//            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        }

        // TypeTokens
        val locTypeToken: Type = object : TypeToken<Location>() {}.type
        val invTypeToken: Type = object : TypeToken<Inventory>() {}.type

        // create tables
        const val CREATE_QUEST_CHESTS = "CREATE TABLE IF NOT EXISTS questChests(location VARCHAR(150) PRIMARY KEY);"
        const val CREATE_CHEST_CONTENT = "CREATE TABLE IF NOT EXISTS chestContents(id INTEGER PRIMARY KEY, chest_location INTEGER NOT NULL, player_uuid VARCHAR(50) NOT NULL, inventory VARCHAR(20000) NOT NULL, FOREIGN KEY(chest_location) REFERENCES questChests(location));"
        const val CREATE_TRIGGER = "CREATE TRIGGER IF NOT EXISTS removeInventories BEFORE DELETE ON questChests FOR EACH ROW BEGIN DELETE FROM chestContents WHERE chestContents.chest_location = OLD.location; END"
        // selects
        const val SELECT_ALL_FROM_QUEST_CHEST = "SELECT * FROM questChests;"
        const val SELECT_ALL_FROM_CHEST_CONTENT = "SELECT chest_location, player_uuid, inventory FROM chestContents;"
        const val SELECT_CHEST_CONTENT = "SELECT inventory FROM chestContents WHERE chest_location = '?' AND player_uuid = '?' LIMIT 1;"
        // inserts
        const val INSERT_QUEST_CHEST = "INSERT INTO questChests(location) VALUES ('?');"
        const val INSERT_CHEST_CONTENTS = "INSERT INTO chestContents(chest_location, player_uuid, inventory) VALUES ('?', '?', '?');"
        // updates
        const val UPDATE_CHEST_CONTENTS = "UPDATE chestContents SET inventory = '?' WHERE chest_location = '?' AND player_uuid = '?';"
        // removes
        const val REMOVE_CHEST_QUESTS_OF_WORLD = """DELETE FROM questChests WHERE location LIKE '{"world":"?%';""" // change this later
        const val REMOVE_QUEST_CHEST = "DELETE FROM questChests WHERE location = '?';"
    }
}
