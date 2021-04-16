package com.github.secretx33.chestquest

import com.github.secretx33.chestquest.commands.Commands
import com.github.secretx33.chestquest.config.Config
import com.github.secretx33.chestquest.database.SQLite
import com.github.secretx33.chestquest.events.*
import com.github.secretx33.chestquest.repository.ChestRepo
import com.github.secretx33.chestquest.repository.PlayerProgressRepo
import com.github.secretx33.chestquest.utils.*
import com.github.secretx33.chestquest.utils.Utils.consoleMessage
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import org.koin.core.component.KoinApiExtension
import org.koin.core.logger.Level
import org.koin.dsl.bind
import org.koin.dsl.module

@KoinApiExtension
class ChestQuest : JavaPlugin(), CustomKoinComponent {

    private val mod = module {
        single<Plugin> { this@ChestQuest } bind JavaPlugin::class
        single { get<Plugin>().server.consoleSender }
        single { logger }
        single { Reflections() }
        single { SQLite(get(), get()) }
        single { ChestRepo(get()) }
        single { PlayerProgressRepo(get()) }
        single { Commands(get()) }
        single { BreakChestEvent(get(), get()) }
        single { CloseInventoryEvent(get(), get()) }
        single { ItemMoveEvent(get(), get()) }
        single { OpenChestEvent(get(), get(), get()) }
        single { PlayerLogoutEvent(get(), get(), get()) }
    }

    override fun onEnable() {
        saveDefaultConfig()
        startKoin {
            printLogger(Level.ERROR)
            loadKoinModules(mod)
        }
        Config.reloadConfig()
        get<BreakChestEvent>()
        get<CloseInventoryEvent>()
        get<ItemMoveEvent>()
        get<OpenChestEvent>()
        get<PlayerLogoutEvent>()
        get<Commands>()
        logger.info("loaded")
    }

    override fun onDisable() {
        get<SQLite>().close()
        unloadKoinModules(mod)
        stopKoin()
    }
}
