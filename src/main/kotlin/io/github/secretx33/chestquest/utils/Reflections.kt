package io.github.secretx33.chestquest.utils

import com.google.common.base.Objects
import com.google.common.base.Preconditions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack
import java.io.*
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.math.BigInteger
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

class Reflections {
    private val gson = Gson()
    private val version: String = Bukkit.getServer().javaClass.getPackage().name.replace(".", ",").split(",").toTypedArray()[3] + "."
    private val CraftItemStack: Class<*> = getBukkitClass("inventory.CraftItemStack")
    private val NMS_Entity: Class<*> = getNMSClass("Entity")
    private val NMS_ItemStack: Class<*> = getNMSClass("ItemStack")
    private val NBTTagCompound: Class<*> = getNMSClass("NBTTagCompound")
    private val NBTCompressedStreamTools: Class<*> = getNMSClass("NBTCompressedStreamTools")

    private fun getBukkitClass(bukkitClassString: String): Class<*> {
        val name = "org.bukkit.craftbukkit.$version$bukkitClassString"
        return Class.forName(name)
    }

    private fun getNMSClass(nmsClassString: String): Class<*> {
        val name = "net.minecraft.server.$version$nmsClassString"
        return Class.forName(name)
    }

    private val fields       = ConcurrentHashMap<Pair<String, String>, Field>()
    private val methods      = ConcurrentHashMap<Triple<String, String, Array<out Class<*>?>>, Method>()
    private val constructors = ConcurrentHashMap<Pair<String, Array<out Class<*>?>>, Constructor<*>>()

    private fun Class<*>.field(fieldName: String): Field {
        val className = this::class.java.name.toString()

        return this@Reflections.fields.getOrPut(Pair(className, fieldName)) {
            this.getDeclaredField(fieldName).apply {
                isAccessible = true
            }
        }
    }

    private fun Class<*>.method(methodName: String, vararg parameterTypes: Class<*>?): Method {
        val className = this::class.java.name.toString()

        return this@Reflections.methods.getOrPut(Triple(className, methodName, parameterTypes)) {
            this.getDeclaredMethod(methodName, *parameterTypes).apply {
                isAccessible = true
            }
        }
    }

    private fun Class<*>.constructor(vararg parameterTypes: Class<*>?): Constructor<*> {
        val className = this::class.java.name.toString()

        return this@Reflections.constructors.getOrPut(Pair(className, parameterTypes)) {
            this.getDeclaredConstructor(*parameterTypes).apply {
                isAccessible = true
            }
        }
    }

    fun getCraftItemStack(stack: ItemStack): Any? {
        if (CraftItemStack.isInstance(stack)) return CraftItemStack.cast(stack)
        try {
            return CraftItemStack.method("asCraftCopy", ItemStack::class.java).invoke(CraftItemStack, stack)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun clone(stack: ItemStack): ItemStack {
        return CraftItemStack.method("asCraftCopy", ItemStack::class.java).invoke(CraftItemStack, stack) as ItemStack
    }

    fun getNMSItemStack(item: Any): Any? {
        if (NMS_ItemStack.isInstance(item)) return NMS_ItemStack.cast(item)
        val itemStack = item as ItemStack
        try {
            return CraftItemStack.method("asNMSCopy", ItemStack::class.java).invoke(CraftItemStack, itemStack)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun setValueOnFinalField(field: Field, `object`: Any, newValue: Any) {
        field.isAccessible = true
        try {
            Field::class.java.field("modifiers").setInt(field, field.modifiers and Modifier.FINAL.inv())
            field[`object`] = newValue
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getNBTTagFromInputStream(inputStream: InputStream): Any? {
        try {
            val readTag_NBTCompStreamTools = NBTCompressedStreamTools.method("a", InputStream::class.java)
            return readTag_NBTCompStreamTools.invoke(NBTCompressedStreamTools, inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun writeNBTTagToOutputStream(NBTTag: Any, outputStream: OutputStream) {
        try {
            val readTag_NBTCompStreamTools = NBTCompressedStreamTools.method("a", NBTTagCompound, OutputStream::class.java)
            readTag_NBTCompStreamTools.invoke(NBTCompressedStreamTools, NBTTag, outputStream)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getNBTTag(stack: ItemStack): Any {
        try {
            val NBTTag = NBTTagCompound.newInstance()
            val nmsItem = CraftItemStack.method("asNMSCopy", ItemStack::class.java).invoke(getCraftItemStack(stack), stack)
            NMS_ItemStack.getDeclaredMethod("save", NBTTagCompound).invoke(nmsItem, NBTTag)
            return NBTTag
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Any()
    }

    fun deserializeNBTTag(stringNBTTag: String): Any {
        val inputStream = ByteArrayInputStream(gson.fromJson(stringNBTTag, object : TypeToken<ByteArray>() {}.type))
        try {
            val NBTTag = NBTCompressedStreamTools.getDeclaredMethod("a", InputStream::class.java).invoke(NBTCompressedStreamTools, inputStream)
            if (NBTTag != null) return NBTTag
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Any()
    }

    /**
     * Serialize a ItemStack into a String.
     *
     * @param stack The ItemStack to serialize
     * @return The serialization string
     */
    fun serialize(stack: ItemStack): String {
        val byteOutputStream = ByteArrayOutputStream()
        val dataOutputStream = DataOutputStream(byteOutputStream)
        val NBTTag = getNBTTag(stack)
        writeNBTTagToOutputStream(NBTTag, dataOutputStream)
        return BigInteger(1, byteOutputStream.toByteArray()).toString(32)
    }

    /**
     * Deserialize a String back to an ItemStack.
     *
     * @param serializedItem The serialized ItemStack
     * @return The deserialized ItemStack object
     */
    fun deserializeItem(serializedItem: String): ItemStack? {
        val inputStream = DataInputStream(ByteArrayInputStream(BigInteger(serializedItem, 32).toByteArray()))
        try {
            val NBTTag = getNBTTagFromInputStream(inputStream)

            // Creating a new NMS_ItemStack from the NBTTag
            val nmsItem: Any = when {
                VersionUtil.serverVersion.isLowerThanOrEqualTo(VersionUtil.v1_10_2_R01) -> {
                    NMS_ItemStack.method("createStack", NBTTagCompound).invoke(NMS_ItemStack, NBTTag)
                }
                VersionUtil.serverVersion.isLowerThanOrEqualTo(VersionUtil.v1_12_2_R01) -> {
                    NMS_ItemStack.constructor(NBTTagCompound).newInstance(NBTTag)
                }
                else -> {
                    NMS_ItemStack.method("a", NBTTagCompound).invoke(NMS_ItemStack, NBTTag)
                }
            }

            // And returning its mirrored CraftItemStack
            val craftMirror = CraftItemStack.method("asCraftMirror", NMS_ItemStack).invoke(CraftItemStack, nmsItem)
            return craftMirror as ItemStack
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    // Version Util (from EssentialsX)
    private object VersionUtil {
        val v1_8_8_R01  = BukkitVersion.fromString("1.8.8-R0.1-SNAPSHOT")
        val v1_9_R01    = BukkitVersion.fromString("1.9-R0.1-SNAPSHOT")
        val v1_9_4_R01  = BukkitVersion.fromString("1.9.4-R0.1-SNAPSHOT")
        val v1_10_R01   = BukkitVersion.fromString("1.10-R0.1-SNAPSHOT")
        val v1_10_2_R01 = BukkitVersion.fromString("1.10.2-R0.1-SNAPSHOT")
        val v1_11_R01   = BukkitVersion.fromString("1.11-R0.1-SNAPSHOT")
        val v1_11_2_R01 = BukkitVersion.fromString("1.11.2-R0.1-SNAPSHOT")
        val v1_12_2_R01 = BukkitVersion.fromString("1.12.2-R0.1-SNAPSHOT")
        val v1_13_0_R01 = BukkitVersion.fromString("1.13.0-R0.1-SNAPSHOT")
        val v1_13_2_R01 = BukkitVersion.fromString("1.13.2-R0.1-SNAPSHOT")
        val v1_14_R01   = BukkitVersion.fromString("1.14-R0.1-SNAPSHOT")
        val v1_14_4_R01 = BukkitVersion.fromString("1.14.4-R0.1-SNAPSHOT")
        val v1_15_R01   = BukkitVersion.fromString("1.15-R0.1-SNAPSHOT")
        val v1_15_2_R01 = BukkitVersion.fromString("1.15.2-R0.1-SNAPSHOT")
        val v1_16_1_R01 = BukkitVersion.fromString("1.16.1-R0.1-SNAPSHOT")
        val v1_16_5_R01 = BukkitVersion.fromString("1.16.5-R0.1-SNAPSHOT")

        val nmsVersion: String by lazy {
            val name = Bukkit.getServer().javaClass.name
            val parts = name.split("\\.").toTypedArray()
            if (parts.size > 3) {
                parts[3]
            } else ""
        }
        val serverVersion by lazy { BukkitVersion.fromString(Bukkit.getServer().bukkitVersion) }

        class BukkitVersion private constructor(
            val major: Int,
            val minor: Int,
            val patch: Int,
            val revision: Double,
            val prerelease: Int
        ) : Comparable<BukkitVersion> {
            fun isHigherThan(o: BukkitVersion): Boolean = compareTo(o) > 0

            fun isHigherThanOrEqualTo(o: BukkitVersion): Boolean = compareTo(o) >= 0

            fun isLowerThan(o: BukkitVersion): Boolean = compareTo(o) < 0

            fun isLowerThanOrEqualTo(o: BukkitVersion): Boolean = compareTo(o) <= 0

            override fun equals(other: Any?): Boolean {
                if (this === other) {
                    return true
                }
                if (other == null || javaClass != other.javaClass) {
                    return false
                }
                val that = other as BukkitVersion
                return major == that.major && minor == that.minor && patch == that.patch && revision == that.revision && prerelease == that.prerelease
            }

            override fun hashCode(): Int {
                return Objects.hashCode(major, minor, patch, revision, prerelease)
            }

            override fun toString(): String {
                val sb = StringBuilder("$major.$minor")
                if (patch != 0) {
                    sb.append(".").append(patch)
                }
                if (prerelease != -1) {
                    sb.append("-pre").append(prerelease)
                }
                return sb.append("-R").append(revision).toString()
            }

            override fun compareTo(other: BukkitVersion): Int {
                return if (major < other.major) {
                    -1
                } else if (major > other.major) {
                    1
                } else { // equal major
                    if (minor < other.minor) {
                        -1
                    } else if (minor > other.minor) {
                        1
                    } else { // equal minor
                        if (patch < other.patch) {
                            -1
                        } else if (patch > other.patch) {
                            1
                        } else { // equal patch
                            if (prerelease < other.prerelease) {
                                -1
                            } else if (prerelease > other.prerelease) {
                                1
                            } else { // equal prerelease
                                revision.compareTo(other.revision)
                            }
                        }
                    }
                }
            }

            companion object {
                private val VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)\\.?([0-9]*)?(?:-pre(\\d))?(?:-?R?([\\d.]+))?(?:-SNAPSHOT)?")

                fun fromString(string: String): BukkitVersion {
                    Preconditions.checkNotNull(string, "string cannot be null.")
                    var matcher = VERSION_PATTERN.matcher(string)
                    if (!matcher.matches()) {
                        require(Bukkit.getName() == "Essentials Fake Server") { "$string is not in valid version format. e.g. 1.8.8-R0.1" }
                        matcher = VERSION_PATTERN.matcher(v1_14_R01.toString())
                        Preconditions.checkArgument(matcher.matches(),"$string is not in valid version format. e.g. 1.8.8-R0.1")
                    }
                    return from(
                        matcher.group(1),
                        matcher.group(2),
                        matcher.group(3),
                        if (matcher.groupCount() < 5) "" else matcher.group(5),
                        matcher.group(4)
                    )
                }

                private fun from(
                    major: String,
                    minor: String,
                    patch: String?,
                    revision: String?,
                    prerelease: String?
                ): BukkitVersion {
                    var patch = patch
                    var revision = revision
                    var prerelease = prerelease
                    if (patch == null || patch.isEmpty()) patch = "0"
                    if (revision == null || revision.isEmpty()) revision = "0"
                    if (prerelease == null || prerelease.isEmpty()) prerelease = "-1"
                    return BukkitVersion(
                        major.toInt(),
                        minor.toInt(),
                        patch.toInt(),
                        revision.toDouble(),
                        prerelease.toInt()
                    )
                }
            }
        }
    }
}
