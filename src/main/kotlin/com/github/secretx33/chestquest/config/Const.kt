package com.github.secretx33.chestquest.config

import org.bukkit.ChatColor

object Const {
    const val PLUGIN_NAME = "ChestQuest"
    const val PLUGIN_PERMISSION_PREFIX = "cq"
    const val PLUGIN_COMMAND_PREFIX = "chestquest"
    private val PLUGIN_CHAT_COLOR_PREFIX = ChatColor.GOLD

    val ENTRY_NOT_FOUND = "entry '${ChatColor.DARK_AQUA}%s${ChatColor.WHITE}' was ${ChatColor.RED}not${ChatColor.WHITE} found in your config file, please fix this issue and reload your configs."
    const val SECTION_NOT_FOUND = "'%s' section could not be find in your YML config file, please fix the issue or delete the file."
    val CONFIGS_RELOADED = "$PLUGIN_CHAT_COLOR_PREFIX$PLUGIN_NAME${ChatColor.WHITE} configs reloaded and reapplied."
}
