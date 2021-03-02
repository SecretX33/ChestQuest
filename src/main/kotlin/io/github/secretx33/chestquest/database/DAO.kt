package io.github.secretx33.chestquest.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.secretx33.chestquest.config.Config
import io.github.secretx33.chestquest.utils.Utils.consoleMessage
import kotlinx.coroutines.*
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.inventory.Inventory
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension
import java.nio.file.FileSystems
import java.sql.*
import java.util.*
import kotlin.collections.HashSet


@KoinApiExtension
class DAO(plugin: Plugin) {

    private val url = "jdbc:sqlite:${plugin.dataFolder.absolutePath}${folderSeparator}database.db"
    private val ds = HikariDataSource(hikariConfig)

    init {
        initialize()
    }

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

    fun addChestContent(inv: Inventory) = CoroutineScope(Dispatchers.IO).launch {
        try {
            ds.connection.use { conn: Connection ->
                val prep = conn.prepareStatement(REMOVE_CHEST_CONTENTS_OF_WORLD).apply {
                    setString(1, it)
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

    private fun removeQuestChests(list: Iterable<String>) = CoroutineScope(Dispatchers.IO).launch {
        try {
            ds.connection.use { conn: Connection ->
                list.forEach {
                    val prep = conn.prepareStatement(REMOVE_CHEST_CONTENTS_OF_WORLD).apply {
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

    fun getAllQuestChests(): Deferred<Set<Location>> = CoroutineScope(Dispatchers.IO).async {
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

    companion object {
        val folderSeparator: String = FileSystems.getDefault().separator
        val hikariConfig = HikariConfig().apply {
            dataSourceClassName = "org.sqlite.SQLiteDataSource"
            isAutoCommit = false
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "20")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        }

        // create tables
        const val CREATE_QUEST_CHESTS = "CREATE TABLE IF NOT EXISTS questChests(id INTEGER PRIMARY KEY, world VARCHAR(50) NOT NULL, x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL);"
        const val CREATE_CHEST_CONTENT = "CREATE TABLE IF NOT EXISTS chestContents(id INTEGER PRIMARY KEY, chest_id INTEGER NOT NULL, player_uuid VARCHAR(50), inventory VARCHAR(20000), FOREIGN KEY(chest_id) REFERENCES questChests(id));"
        const val CREATE_TRIGGER = "CREATE TRIGGER IF NOT EXISTS removeInventories BEFORE DELETE ON questChests FOR EACH ROW BEGIN DELETE FROM chestContents WHERE chestContents.chest_id = OLD.id; END"

        // queries
        const val SELECT_ALL_FROM_QUEST_CHEST = "SELECT * FROM questChests;"
        const val SELECT_ALL_FROM_CHEST_CONTENT = "SELECT * FROM chestContents;"

        const val INSERT_CHEST_CONTENTS = "DELETE FROM questChests WHERE world = ?;"

        const val REMOVE_CHEST_CONTENTS_OF_WORLD = "DELETE FROM questChests WHERE world = ?;"
    }
}
