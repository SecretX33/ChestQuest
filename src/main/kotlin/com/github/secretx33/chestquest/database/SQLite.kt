package com.github.secretx33.chestquest.database

import com.github.secretx33.chestquest.config.Config
import com.github.secretx33.chestquest.utils.formattedString
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.block.Container
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.plugin.Plugin
import org.intellij.lang.annotations.Language
import org.koin.core.component.KoinApiExtension
import java.nio.file.FileSystems
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*
import java.util.logging.Logger
import java.util.regex.Pattern

@KoinApiExtension
class SQLite(plugin: Plugin, private val log: Logger) {

    private val url = "jdbc:sqlite:${plugin.dataFolder.absolutePath}${folderSeparator}database.db"
    private val ds = HikariDataSource(hikariConfig.apply { jdbcUrl = url })
    private val chestQuestLock = Semaphore(1)
    private val chestContentLock = Semaphore(1)
    private val playerProgressLock = Semaphore(1)

    init { initialize() }

    fun close() = ds.safeClose()

    private fun initialize() {
        try {
            ds.connection.use { conn: Connection ->
                conn.prepareStatement(CREATE_QUEST_CHESTS).use { it.execute() }
                conn.prepareStatement(CREATE_CHEST_CONTENT).use { it.execute() }
                conn.prepareStatement(CREATE_PLAYER_PROGRESS).use { it.execute() }
                conn.prepareStatement(CREATE_TRIGGER).use { it.execute() }
                conn.commit()
                log.fine("Initiated DB")
            }
        } catch (e: SQLException) {
            log.severe("ERROR: An exception occurred while trying to connect to the database and create the tables\n${e.printStackTrace()}")
        }
    }

    // ADD

    fun addQuestChest(chestLoc: Location, chestOrder: Int) = CoroutineScope(Dispatchers.IO).launch {
        require(chestOrder >= 1) { "Chest order cannot be less than 1, actual value is $chestOrder" }
        try {
            withStatement(INSERT_QUEST_CHEST, chestQuestLock) {
                setString(1, chestLoc.toJson())
                setInt(2, chestOrder)
                execute()
            }
        } catch (e: SQLException) {
            log.severe("ERROR: An exception occurred while adding a specific Quest Chest (${chestLoc.formattedString()})\n${e.stackTraceToString()}")
        }
    }

    fun addChestContent(chestLoc: Location, playerUuid: UUID, inv: Inventory) = CoroutineScope(Dispatchers.IO).launch {
        try {
            withStatement(INSERT_CHEST_CONTENTS, chestContentLock) {
                setString(1, chestLoc.toJson())
                setString(2, playerUuid.toString())
                setString(3, inv.toJson())
                execute()
            }
        } catch (e: SQLException) {
            log.severe("ERROR: An exception occurred while trying to add a content of the chest ${chestLoc.formattedString()} to the database\n${e.stackTraceToString()}")
        }
    }

    fun addPlayerProgress(playerUuid: UUID, progress: Int) = CoroutineScope(Dispatchers.IO).launch {
        require(progress >= 0) { "Progress has to be at least 0. Actual value if $progress" }
        try {
            withStatement(INSERT_PLAYER_PROGRESS, playerProgressLock) {
                setString(1, playerUuid.toString())
                setInt(2, progress)
                execute()
            }
        } catch (e: SQLException) {
            log.severe("ERROR: An exception occurred while trying to add player ${Bukkit.getPlayer(playerUuid)?.name ?: "Unknown"} ($playerUuid) progress of $progress to the database\n${e.stackTraceToString()}")
        }
    }

    // REMOVE

    fun removeQuestChest(chestLoc: Location) = CoroutineScope(Dispatchers.IO).launch {
        try {
            withStatement(REMOVE_QUEST_CHEST, chestQuestLock) {
                setString(1, chestLoc.toJson())
                execute()
            }
        } catch (e: SQLException) {
            log.severe("ERROR: An exception occurred while trying to remove a Quest Chest from the database\n${e.stackTraceToString()}")
        }
    }

    private fun removeQuestChestsByWorldUuid(worldUuids: Iterable<String>) = CoroutineScope(Dispatchers.IO).launch {
        try {
            withStatement(REMOVE_QUEST_CHESTS_OF_WORLD, chestQuestLock) {
                worldUuids.forEach {
                    setString(1, "%$it%")
                    addBatch()
                }
                executeBatch()
            }
        } catch (e: SQLException) {
            log.severe("ERROR: An exception occurred while trying to buck remove all quest chests from worlds\n${e.stackTraceToString()}")
        }
    }

    private fun removeQuestChestsByLocation(locations: Iterable<Location>) = CoroutineScope(Dispatchers.IO).launch {
        try {
            withStatement(REMOVE_QUEST_CHEST, chestQuestLock) {
                locations.forEach { loc ->
                    setString(1, loc.toJson())
                    addBatch()
                }
                executeBatch()
            }
        } catch (e: SQLException) {
            log.severe("ERROR: An exception occurred while trying to remove a list of quest chests\n${e.stackTraceToString()}")
        }
    }

    fun removePlayerProgress(playerUuid: UUID) = CoroutineScope(Dispatchers.IO).launch {
        try {
            withStatement(REMOVE_PLAYER_PROGRESS, playerProgressLock) {
                setString(1, playerUuid.toString())
                execute()
            }
        } catch (e: SQLException) {
            log.severe("ERROR: An exception occurred while trying to remove a Quest Chest from the database\n${e.stackTraceToString()}")
        }
    }

    // GET

    fun getChestContent(chestLoc: Location, playerUuid: UUID): Inventory? {
        var conn: Connection? = null
        var prep: PreparedStatement? = null
        var rs: ResultSet? = null

        try {
            conn = ds.connection
            prep = conn.prepareStatement(SELECT_CHEST_CONTENT).apply {
                setString(1, chestLoc.toJson())
                setString(2, playerUuid.toString())
            }
            rs = prep.executeQuery()

            if(rs.next()){
                val inv = jsonInv.fromJson(rs.getString("inventory"))
                when {
                    inv == null -> log.severe("ERROR: While trying to get the chestContent of Player ${Bukkit.getPlayer(playerUuid)?.name ?: "Unknown"} ($playerUuid) in ${chestLoc.formattedString()}, inventory came null, report this to SecretX!")
                    inv.holder == null -> log.severe("ERROR: While trying to get the chestContent of Player ${Bukkit.getPlayer(playerUuid)?.name ?: "Unknown"} ($playerUuid) in ${chestLoc.formattedString()}, holder came null, report this to SecretX!")
                    else -> return inv
                }
            } else {
                return null
            }
        } catch (e: SQLException) {
            log.severe("ERROR: An exception occurred while trying to get inventory of chest at ${chestLoc.formattedString()} from database\n${e.stackTraceToString()}")
        } finally {
            rs?.safeClose()
            prep?.safeClose()
            conn?.safeClose()
        }
        return newEmptyInventory()
    }

    fun getAllQuestChestsAsync(): Deferred<Map<Location, Int>> = CoroutineScope(Dispatchers.IO).async {
        var conn: Connection? = null
        var prep: PreparedStatement? = null
        var rs: ResultSet? = null

        val chests = HashMap<Location, Int>()
        val worldRemoveSet = HashSet<String>()
        val chestRemoveSet = HashSet<Location>()

        try {
            conn = ds.connection
            prep = conn.prepareStatement(SELECT_ALL_FROM_QUEST_CHEST)
            rs = prep.executeQuery()
            while(rs.next()){
                val chestLoc = jsonLoc.fromJson(rs.getString("location"))!!
                if(chestLoc.world == null && Config.removeDBEntriesIfWorldIsMissing){
                    UUID_WORLD_PATTERN.matcher(rs.getString("location")).replaceFirst("$1")?.let {
                        worldRemoveSet.add(it)
                    }
                } else if(chestLoc.world != null) {
                    if (chestLoc.world.getBlockAt(chestLoc).state !is Container) {
                        log.warning("WARNING: The chest located at '${chestLoc.formattedString()}' was not found, queuing its removal to preserve DB integrity.${ChatColor.WHITE} Usually this happens when a Quest Chest is broken with this plugin being disabled or missing.")
                        chestRemoveSet.add(chestLoc)
                    } else {
                        chests[chestLoc] = rs.getInt("chest_order")
                    }
                }
            }
            if(worldRemoveSet.isNotEmpty()){
                worldRemoveSet.forEach { log.warning("WARNING: The world with UUID '$it' was not found, removing ALL chests and inventories linked to it") }
                removeQuestChestsByWorldUuid(worldRemoveSet)
            }
            if(chestRemoveSet.isNotEmpty())
                removeQuestChestsByLocation(chestRemoveSet)
        } catch (e: SQLException) {
            log.severe("ERROR: An exception occurred while trying to get all chest quests async\n${e.stackTraceToString()}")
        } finally {
            rs?.safeClose()
            prep?.safeClose()
            conn?.safeClose()
        }
        chests
    }

    /**
     * Get all entries of database for Player Progress table, which stores player progress in the quest chain
     * @return Deferred<Map<UUID, Int>> UUID is the player UUID, and Int is his progress in the quest chain, used to prevent him of opening chests that have higher number than the player progress + 1
     */
    fun getAllPlayerProgressAsync(): Deferred<Map<UUID, Int>> = CoroutineScope(Dispatchers.IO).async {
        val map = HashMap<UUID, Int>()
        try {
            withQueryStatement(SELECT_ALL_FROM_PLAYER_PROGRESS) { rs ->
                while(rs.next()){
                    val key = UUID.fromString(rs.getString("player_uuid"))
                    val value = rs.getInt("progress")
                    map[key] = value
                }
            }
        } catch (e: SQLException) {
            log.severe("ERROR: An exception occurred while trying to get all player progress from database async\n${e.stackTraceToString()}")
        }
        map
    }

    fun getPlayerProgress(playerUuid: UUID): Int? {
        try {
            withQueryStatement(SELECT_PLAYER_PROCESS, {
                setString(1, playerUuid.toString())
            }) { rs ->
                if(rs.next()) return rs.getInt("progress")
            }
        } catch (e: SQLException) {
            log.severe("ERROR: An exception occurred while trying to get progress of player ${Bukkit.getPlayer(playerUuid)?.name} ($playerUuid) from database\n${e.stackTraceToString()}")
        }
        return null
    }

    // UPDATE

    fun updateInventory(chestLoc: Location, playerUuid: UUID, inv: Inventory) = CoroutineScope(Dispatchers.IO).launch {
        try {
            withStatement(UPDATE_CHEST_CONTENTS, chestContentLock) {
                setString(1, inv.toJson())
                setString(2, chestLoc.toJson())
                setString(3, playerUuid.toString())
                execute()
            }
        } catch (e: SQLException) {
            log.severe("ERROR: An exception occurred while updating an inventory to the database\n${e.stackTraceToString()}")
        }
    }

    fun updateChestOrder(location: Location, newOrder: Int) = CoroutineScope(Dispatchers.IO).launch {
        try {
            withStatement(UPDATE_QUEST_CHEST_ORDER, chestQuestLock) {
                setInt(1, newOrder)
                setString(2, location.toJson())
                execute()
            }
        } catch (e: SQLException) {
            log.severe("ERROR: An exception occurred while updating chest order of ${location.formattedString()} to the database\n${e.stackTraceToString()}")
        }
    }

    fun updatePlayerProgress(playerUuid: UUID, progress: Int) = CoroutineScope(Dispatchers.IO).launch {
        try {
            withStatement(UPDATE_PLAYER_PROGRESS, playerProgressLock) {
                setInt(1, progress)
                setString(2, playerUuid.toString())
                execute()
            }
        } catch (e: SQLException) {
            log.severe("ERROR: An exception occurred while trying to add player ${Bukkit.getPlayer(playerUuid)?.name ?: "Unknown"} ($playerUuid) progress of $progress to the database\n${e.stackTraceToString()}")
        }
    }

    private fun String.toLocation() = jsonLoc.fromJson(this)

    private fun Location.toJson() = jsonLoc.toJson(this)

    private fun Inventory.toJson() = jsonInv.toJson(this)

    private fun AutoCloseable?.safeClose() { runCatching { this?.close() } }

    private fun newEmptyInventory(): Inventory = Bukkit.createInventory(null, InventoryType.CHEST)

    private suspend fun <T> withStatement(@Language("SQL") statement: String, semaphore: Semaphore, prepareBlock: PreparedStatement.() -> T): T {
        semaphore.withPermit {
            ds.connection.use { conn ->
                conn.prepareStatement(statement).use { prep ->
                    return prep.prepareBlock().also { conn.commit() }
                }
            }
        }
    }

    private inline fun <reified T> withQueryStatement(@Language("SQL") statement: String, noinline prepareBlock: PreparedStatement.() -> Unit = {}, resultBlock: (ResultSet) -> T): T {
        ds.connection.use { conn ->
            conn.prepareStatement(statement).use { prep ->
                prep.apply {
                    prepareBlock()
                    executeQuery().use { rs ->
                        return resultBlock(rs)
                    }
                }
            }
        }
    }

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
        const val CREATE_QUEST_CHESTS = "CREATE TABLE IF NOT EXISTS questChests(location VARCHAR(150) NOT NULL PRIMARY KEY, chest_order INTEGER NOT NULL);"
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
