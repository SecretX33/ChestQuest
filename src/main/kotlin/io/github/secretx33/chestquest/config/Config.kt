package io.github.secretx33.chestquest.config

import io.github.secretx33.chestquest.utils.Utils.consoleMessage
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

@KoinApiExtension
object Config : KoinComponent {
    private val plugin: Plugin = get()
    var removeDBEntriesIfWorldIsMissing = true
    var debug: Boolean = false

    init { reloadConfig() }

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
        var field = "automatically-remove-db-entries-from-missing-world"
        if(config.isSet("$section.field")){
            removeDBEntriesIfWorldIsMissing = config.getBoolean(field)
        }
        if(config.isSet("general.debug")) debug = config.getBoolean("general.debug")
    }
}
