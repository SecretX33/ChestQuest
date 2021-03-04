package io.github.secretx33.chestquest

import io.github.secretx33.chestquest.commands.Commands
import io.github.secretx33.chestquest.database.SQLite
import io.github.secretx33.chestquest.events.*
import io.github.secretx33.chestquest.repository.ChestRepo
import io.github.secretx33.chestquest.repository.PlayerProgressRepo
import io.github.secretx33.chestquest.utils.Reflections
import io.github.secretx33.chestquest.utils.Utils.consoleMessage
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import org.koin.core.component.KoinApiExtension
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.dsl.bind
import org.koin.dsl.module

@KoinApiExtension
class Main : JavaPlugin(), KoinComponent {

    private val db by inject<SQLite>()

    override fun onEnable() {
        saveDefaultConfig()
        startKoin {
            printLogger()
            modules(module {
                single<Plugin> { this@Main } bind JavaPlugin::class
                single { get<Plugin>().server.consoleSender }
                single { Reflections() }
                single { SQLite(get()) }
                single { ChestRepo(get()) }
                single { PlayerProgressRepo(get()) }
            })
        }
        val commands = Commands(get(), get())
        val breakChestEvent = BreakChestEvent(get(), get())
        val closeInvEvent = CloseInventoryEvent(get(), get())
        val itemMoveEvent = ItemMoveEvent(get(), get())
        val openChestEvent = OpenChestEvent(get(), get(), get())
        val playerLogoutEvent = PlayerLogoutEvent(get(), get())
        consoleMessage("loaded")
    }

    override fun onDisable() {
        db.close()
    }
}
