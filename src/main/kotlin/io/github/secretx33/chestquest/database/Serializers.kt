package io.github.secretx33.chestquest.database

import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.Types
import io.github.secretx33.chestquest.utils.Utils.reflections
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.koin.core.component.KoinApiExtension
import java.util.*

class LocationSerializer {

    @ToJson fun serialize(src: Location): String {
        val map = mapOf(
            "world" to src.world.uid.toString(),
            "x" to src.x.toInt().toString(),
            "y" to src.y.toInt().toString(),
            "z" to src.z.toInt().toString(),
        )
        return mapAdapter.toJson(map)
    }

    @FromJson fun deserialize(json: String): Location {
        val map = mapAdapter.fromJson(json) ?: throw NullPointerException("adapter returned a null value")
        map.run {
            return Location(
                Bukkit.getWorld(UUID.fromString(get("world"))),
                get("x")!!.toDouble(),
                get("y")!!.toDouble(),
                get("z")!!.toDouble(),
            )
        }
    }

    private companion object {
        val mapType = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
        val mapAdapter = Moshi.Builder().build().adapter<Map<String, String>>(mapType)
    }
}

@KoinApiExtension
class InventorySerializer {

    @ToJson fun serialize(src: Inventory): String {
        val map = HashMap<String, String>()
        map["type"] = src.type.ordinal.toString()
        src.contents.forEachIndexed { slot, item: ItemStack? ->
            item?.let { map[slot.toString()] = reflections.serialize(item) }
        }
        return mapAdapter.toJson(map)
    }

    @FromJson fun deserialize(json: String): Inventory {
        val map: Map<String, String> = mapAdapter.fromJson(json) ?: throw NullPointerException("adapter returned a null value")
        map.run {
            val inv = Bukkit.createInventory(null, InventoryType.values()[get("type")!!.toInt()])
            repeat(inv.size) { index ->
                get(index.toString())?.let { inv.setItem(index, reflections.deserializeItem(it)) }
            }
            return inv
        }
    }

    private companion object {
        val mapType = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
        val mapAdapter = Moshi.Builder().build().adapter<Map<String, String>>(mapType)
    }
}
