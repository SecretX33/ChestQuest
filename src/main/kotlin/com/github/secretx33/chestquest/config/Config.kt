package com.github.secretx33.chestquest.config

import com.github.secretx33.chestquest.utils.CustomKoinComponent
import com.github.secretx33.chestquest.utils.Utils.consoleMessage
import com.github.secretx33.chestquest.utils.inject
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension

@KoinApiExtension
object Config : CustomKoinComponent {
    private val plugin: Plugin by inject()
    var removeDBEntriesIfWorldIsMissing = false
    var debug: Boolean = false

    fun reloadConfig() {
        val config = plugin.config
        val section = "general"
        var general: ConfigurationSection? = null
        if(config.isSet(section)) {
            general = config.getConfigurationSection(section)
        }
        if(general == null) {
            consoleMessage(String.format(Const.SECTION_NOT_FOUND, section))
            return
        }
        if(config.isSet("general.debug")){
            debug = config.getBoolean("general.debug")
        } else {
            consoleMessage(String.format(Const.ENTRY_NOT_FOUND, "debug"))
        }
        val field = "automatically-remove-db-entries-from-missing-world"
        if(config.isSet("general.$field")){
            removeDBEntriesIfWorldIsMissing = config.getBoolean("general.$field")
        } else {
            consoleMessage(String.format(Const.ENTRY_NOT_FOUND, field))
        }
    }
}
