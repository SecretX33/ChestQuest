package com.github.secretx33.chestquest.events

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.wrappers.EnumWrappers
import com.github.secretx33.chestquest.packets.WrapperPlayServerWorldParticles
import com.github.secretx33.chestquest.repository.ChestRepo
import com.github.secretx33.chestquest.repository.PlayerProgressRepo
import com.github.secretx33.chestquest.utils.Utils.debugMessage
import com.github.secretx33.chestquest.utils.canEditQC
import com.github.secretx33.chestquest.utils.isChest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.block.Chest
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinApiExtension
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

@KoinApiExtension
class OpenChestEvent(plugin: Plugin, private val chestRepo: ChestRepo, private val progressRepo: PlayerProgressRepo) : Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private fun onInteract(event: PlayerInteractEvent) {
        if(!event.isChestQuest()) return

        val player = event.player
        if(player.canEditQC()) return

        val chest = event.clickedBlock?.state as Chest
        val chestOrder = chestRepo.getQuestChestOrder(chest.location)

        if(!progressRepo.canOpenChest(player.uniqueId, chestOrder)) {
            event.isCancelled = true
            debugMessage("Player ${player.name} progress still ${progressRepo.getPlayerProgress(player.uniqueId)}, he cannot open a chest that has a order of $chestOrder")
        } else {
            event.isCancelled = true
            player.openInventory(chestRepo.getChestContent(chest, player))
            spawnParticles(chest, player)
            debugMessage("Player ${player.name} can open chest of order $chestOrder because he has progress of ${progressRepo.getPlayerProgress(player.uniqueId)}")
        }
    }

    private fun spawnParticles(chest: Chest, player: Player) {
        if(!Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) return

        CoroutineScope(Dispatchers.Default).launch {
            val manager = ProtocolLibrary.getProtocolManager()
            val x = chest.location.x.toFloat() + 0.5f
            val y = chest.location.y.toFloat() + 1f
            val z = chest.location.z.toFloat() + 0.5f
            val firstParticle = WrapperPlayServerWorldParticles(manager.createPacket(PacketType.Play.Server.WORLD_PARTICLES))
            val secondParticle = WrapperPlayServerWorldParticles(manager.createPacket(PacketType.Play.Server.WORLD_PARTICLES))
            firstParticle.particleType = EnumWrappers.Particle.TOTEM
            secondParticle.particleType = EnumWrappers.Particle.TOTEM
            debugMessage("Location of chest is ${chest.location}")
            for (i in 0 until firstParticle.handle.float.fields.size) {
                debugMessage("Float $i is ${firstParticle.handle.float.read(i)}")
            }
            repeat(500) {
                val double = it.toDouble() * 0.2
                firstParticle.x = x + cos(double).toFloat()
                firstParticle.y = y + (sin(double).toFloat() * 0.75f)
                firstParticle.z = z + sin(double).toFloat()
//                firstParticle.x = x + (1.1 * sin(double)).toFloat()
//                firstParticle.y = y + (0.3 * tan(double)).toFloat()
//                firstParticle.z = z + (1.1 * cos(double)).toFloat()
                secondParticle.x = x + cos(double + PI).toFloat()
                secondParticle.y = y + (sin(double).toFloat() * 0.75f)
                secondParticle.z = z + sin(double + PI).toFloat()
                firstParticle.sendPacket(player)
                secondParticle.sendPacket(player)
                delay(50)
            }
        }

    }

    private fun PlayerInteractEvent.isChestQuest(): Boolean {
        return action == Action.RIGHT_CLICK_BLOCK && clickedBlock?.isChest() == true && chestRepo.isQuestChest(clickedBlock.location)
    }
}
