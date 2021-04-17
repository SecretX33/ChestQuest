package com.github.secretx33.chestquest

import com.github.secretx33.chestquest.commands.Commands
import com.github.secretx33.chestquest.config.Config
import com.github.secretx33.chestquest.database.SQLite
import com.github.secretx33.chestquest.eventlisteners.*
import com.github.secretx33.chestquest.repository.ChestRepo
import com.github.secretx33.chestquest.repository.PlayerProgressRepo
import com.github.secretx33.chestquest.utils.*
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
        single { get<Plugin>().logger }
        single { Config(get()) }
        single { Reflections() }
        single { SQLite(get(), get(), get()) }
        single { ChestRepo(get(), get()) }
        single { PlayerProgressRepo(get()) }
        single { Commands(get()) }
        single { BreakChestListener(get(), get()) }
        single { CloseChestListener(get(), get(), get()) }
        single { ChestItemMoveListener(get(), get(), get()) }
        single { OpenChestListener(get(), get(), get()) }
        single { PlayerLoginListener(get(), get()) }
        single { PlayerLogoutListener(get(), get(), get()) }
    }

    override fun onEnable() {
        startKoin {
            printLogger(Level.ERROR)
            loadKoinModules(mod)
        }
        get<BreakChestListener>()
        get<CloseChestListener>()
        get<ChestItemMoveListener>()
        get<OpenChestListener>()
        get<PlayerLoginListener>()
        get<PlayerLogoutListener>()
        get<Commands>()
        logger.info("loaded.")
    }

    override fun onDisable() {
        get<SQLite>().close()
        unloadKoinModules(mod)
        stopKoin()
    }
}
