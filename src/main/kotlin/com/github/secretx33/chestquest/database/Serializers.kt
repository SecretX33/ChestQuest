package com.github.secretx33.chestquest.database

import com.github.secretx33.chestquest.utils.Utils.reflections
import com.github.secretx33.chestquest.utils.locationByAllMeans
import com.squareup.moshi.*
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Container
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.koin.core.component.KoinApiExtension
import java.lang.reflect.ParameterizedType
import java.util.*

class LocationSerializer {

    @ToJson fun serialize(src: Location): String {
        val map = mapOf(
            "world" to src.world.uid.toString(),
            "x" to src.x.toLong(),
            "y" to src.y.toLong(),
            "z" to src.z.toLong(),
        )
        return mapAdapter.toJson(map)
    }

    @FromJson fun deserialize(json: String): Location {
        val map = mapAdapter.fromJson(json) ?: throw NullPointerException("adapter returned a null value")
        map.run {
            return Location(
                Bukkit.getWorld(UUID.fromString(getValue("world").toString())),
                getValue("x") as Double,
                getValue("y") as Double,
                getValue("z") as Double,
            )
        }
    }

    private companion object {
        val mapType: ParameterizedType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        val mapAdapter: JsonAdapter<Map<String, Any>> = Moshi.Builder().build().adapter(mapType)
    }
}

@KoinApiExtension
class InventorySerializer {

    @ToJson fun serialize(src: Inventory): String {
        val map = HashMap<String, String>()
        map["holder"] = locAdapter.toJson(src.locationByAllMeans())
        map["type"] = src.type.toString()
        src.contents.forEachIndexed { slot, item: ItemStack? ->
            item?.let { map[slot.toString()] = reflections.serialize(item) }
        }
        return mapAdapter.toJson(map)
    }

    @FromJson fun deserialize(json: String): Inventory {
        val map: Map<String, String> = mapAdapter.fromJson(json) ?: throw NullPointerException("adapter returned a null value")
        map.run {
            val location: Location = locAdapter.fromJson(getValue("holder"))!!
            val holder: Container? = Bukkit.getWorld(location.world.uid).getBlockAt(location).state as? Container

            val inv = Bukkit.createInventory(holder, InventoryType.valueOf(getValue("type")))
            repeat(inv.size) { index ->
                get(index.toString())?.let { inv.setItem(index, reflections.deserializeItem(it)) }
            }
            return inv
        }
    }

    private companion object {
        val moshi: Moshi = Moshi.Builder()
            .add(LocationSerializer())
            .build()
        val locAdapter: JsonAdapter<Location> = moshi.adapter(Location::class.java).nonNull()
        val mapType: ParameterizedType = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
        val mapAdapter: JsonAdapter<Map<String, String>> = moshi.adapter(mapType)
    }
}
