package com.github.secretx33.chestquest.packets

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.PacketContainer
import com.comphenix.protocol.wrappers.BlockPosition
import org.bukkit.Material

class WrapperPlayServerBlockAction : AbstractPacket {

    constructor() : super(PacketContainer(TYPE), TYPE) {
        handle.modifier.writeDefaults()
    }

    constructor(packet: PacketContainer) : super(packet, TYPE)

    /**
     * Retrieve or Set Location.
     *
     * @param value - new value.
     */
    var location: BlockPosition?
        get() = handle.blockPositionModifier.read(0)
        set(value) {
            handle.blockPositionModifier.write(0, value)
        }

    /**
     * Retrieve or Set Byte 1.
     *
     * @param value - new value.
     */
    var byte1: Int
        get() = handle.integers.read(0)
        set(value) {
            handle.integers.write(0, value)
        }
    /**
     * Retrieve Byte 2.
     *
     *
     * Notes: varies depending on block - see Block_Actions
     *
     * @return The current Byte 2
     */
    /**
     * Set Byte 2.
     *
     * @param value - new value.
     */
    var byte2: Int
        get() = handle.integers.read(1)
        set(value) {
            handle.integers.write(1, value)
        }

    /**
     * Retrieve or Set Block Type.
     *
     * @param value - new value.
     */
    var blockType: Material?
        get() = handle.blocks.read(0)
        set(value) {
            handle.blocks.write(0, value)
        }

    companion object {
        val TYPE: PacketType = PacketType.Play.Server.BLOCK_ACTION
    }
}
