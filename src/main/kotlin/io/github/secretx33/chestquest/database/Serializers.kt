package io.github.secretx33.chestquest.database

import com.google.gson.*
import io.github.secretx33.chestquest.utils.Utils
import io.github.secretx33.chestquest.utils.Utils.reflections
import io.github.secretx33.chestquest.utils.clone
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.koin.core.component.KoinApiExtension
import java.lang.reflect.Type

class LocationSerializer : JsonSerializer<Location>, JsonDeserializer<Location> {

    override fun serialize(src: Location?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        if(src == null) return JsonPrimitive("")
        return JsonObject().apply {
            addProperty("world", src.world.uid.toString())
            addProperty("x", src.x.toInt())
            addProperty("y", src.y.toInt())
            addProperty("z", src.z.toInt())
        }
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Location {
        if(json == null) throw NullPointerException("null json object cannot be converted to Location")
        (json as JsonObject).run {
            return Location(
                Bukkit.getWorld(get("world").asString),
                get("x").asDouble,
                get("y").asDouble,
                get("z").asDouble,
            )
        }
    }
}

@KoinApiExtension
class InventorySerializer : JsonSerializer<Inventory>, JsonDeserializer<Inventory> {

    override fun serialize(src: Inventory?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        if(src == null) throw NullPointerException("null json object cannot be converted to Inventory")
        val obj = JsonObject()
        obj.addProperty("type", src.type.ordinal)
        src.contents.forEachIndexed { slot, item: ItemStack? ->
            item?.let { obj.addProperty(slot.toString(), reflections.serialize(item)) }
        }
        return obj
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Inventory {
        if(json == null) throw NullPointerException("null json object cannot be converted to Inventory")
        json as JsonObject
        json.run {
            val inv = Bukkit.createInventory(null, InventoryType.values()[get("type").asInt])
            repeat(inv.size) { index ->
                get(index.toString())?.let { inv.setItem(index, reflections.deserializeItem(it.asString)) }
            }
            return inv
        }
    }

}
