package com.github.secretx33.chestquest.config

import org.bukkit.ChatColor

object Const {
    const val PLUGIN_NAME = "ChestQuest"
    const val PLUGIN_PERMISSION_PREFIX = "cq"
    const val PLUGIN_COMMAND_PREFIX = "chestquest"
    private val PLUGIN_CHAT_COLOR_PREFIX = ChatColor.GOLD

    val PLUGIN_CHAT_PREFIX = "$PLUGIN_CHAT_COLOR_PREFIX[$PLUGIN_NAME]${ChatColor.WHITE}"
    val INVALID_ENTRY_VALUE = "entry '%s' in your config file was set as ${ChatColor.RED}%s${ChatColor.WHITE}, but that's an invalid value, please use a valid value and reload your configs."
    const val ENTRY_HAS_NO_VALUE = "entry '%s' in your config file has no value, please use a valid value and reload your configs."
    val ENTRY_NOT_FOUND = "entry '${ChatColor.DARK_AQUA}%s${ChatColor.WHITE}' was ${ChatColor.RED}not${ChatColor.WHITE} found in your config file, please fix this issue and reload your configs."
    const val SECTION_NOT_FOUND = "'%s' section could not be find in your YML config file, please fix the issue or delete the file."
    val CONFIGS_RELOADED = "$PLUGIN_CHAT_COLOR_PREFIX$PLUGIN_NAME${ChatColor.WHITE} configs reloaded and reapplied."
    val DEBUG_MODE_STATE_CHANGED = "$PLUGIN_CHAT_COLOR_PREFIX$PLUGIN_NAME${ChatColor.WHITE} debug mode turned %s."
}
