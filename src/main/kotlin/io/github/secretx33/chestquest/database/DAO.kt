package io.github.secretx33.chestquest.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.secretx33.chestquest.utils.Utils.consoleMessage
import kotlinx.coroutines.*
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension
import java.nio.file.FileSystems
import java.sql.*
import java.util.*
import kotlin.collections.HashSet


@KoinApiExtension
class DAO(private val plugin: Plugin) {

    private val url = "jdbc:sqlite:${plugin.dataFolder.absolutePath}${folderSeparator}database.db"
    private val ds = HikariDataSource(hikariConfig)

    init {
        initializeTables()
    }

    private fun initializeTables() {
        try {
            ds.connection.use { conn: Connection ->
                consoleMessage("The driver name is " + conn.metaData.driverName)
                conn.prepareStatement(CREATE_QUEST_CHESTS).execute()
                conn.prepareStatement(CREATE_CHEST_CONTENT).execute()
                conn.prepareStatement(CREATE_TRIGGER).execute()
                consoleMessage("A new database has been created.")
            }
        } catch (e: SQLException) {
            consoleMessage("${ChatColor.RED}An exception occurred while trying to connect to the database")
            e.printStackTrace()
        }
    }

    fun getAllQuestChests(): MutableSet<Location> {
        val set = HashSet<Location>()
        try {
            ds.connection.use { conn: Connection ->
                val rs = conn.prepareStatement(SELECT_ALL_FROM_QUEST_CHEST).executeQuery()
                while(rs.next()){
                    val world = Bukkit.getWorld(UUID.fromString(rs.getString("world")))
                    if(world == null){
                        
                    } else {
                        val location = Location(
                            world,
                            rs.getInt("x").toDouble(),
                            rs.getInt("y").toDouble(),
                            rs.getInt("z").toDouble()
                        )
                        set.add(location)
                    }
                }
            }
        } catch (e: SQLException) {
            consoleMessage("${ChatColor.RED}An exception occurred while trying to connect to the database")
            e.printStackTrace()
        }
        return set
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
        const val CREATE_CHEST_CONTENT = "CREATE TABLE IF NOT EXISTS chestContents(id INTEGER PRIMARY KEY, chest_id INTEGER NOT NULL, player_uuid VARCHAR(50), inventory VARCHAR(10000), FOREIGN KEY(chest_id) REFERENCES questChests(id));"
        const val CREATE_TRIGGER = "CREATE TRIGGER IF NOT EXISTS removeInventories BEFORE DELETE ON questChests FOR EACH ROW BEGIN DELETE FROM chestContents WHERE chestContents.chest_id == OLD.id; END"

        // queries
        const val SELECT_ALL_FROM_QUEST_CHEST = "SELECT * FROM questChests;"
        const val SELECT_ALL_FROM_CHEST_CONTENT = "SELECT * FROM chestContents;"
    }
}
