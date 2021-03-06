package ink.ptms.zaphkiel

import com.google.common.collect.Maps
import ink.ptms.zaphkiel.api.*
import ink.ptms.zaphkiel.api.event.PluginReloadEvent
import ink.ptms.zaphkiel.api.event.single.Events
import ink.ptms.zaphkiel.api.event.single.ItemBuildEvent
import ink.ptms.zaphkiel.module.meta.Meta
import ink.ptms.zaphkiel.module.meta.MetaKey
import io.izzel.taboolib.TabooLibLoader
import io.izzel.taboolib.kotlin.Mirror
import io.izzel.taboolib.kotlin.Reflex.Companion.static
import io.izzel.taboolib.module.config.TConfigWatcher
import io.izzel.taboolib.module.db.local.SecuredFile
import io.izzel.taboolib.module.nms.nbt.NBTCompound
import io.izzel.taboolib.util.Files
import io.izzel.taboolib.util.Reflection
import io.izzel.taboolib.util.item.Equipments
import io.izzel.taboolib.util.item.Items
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * @Author sky
 * @Since 2019-12-15 20:14
 */
@Suppress("UNCHECKED_CAST")
object ZaphkielAPI {

    val mirror = Mirror()
    val events = Events
    val loaded = ArrayList<File>()
    val folderItem = File(Zaphkiel.plugin.dataFolder, "item")
    val folderDisplay = File(Zaphkiel.plugin.dataFolder, "display")
    val registeredItem = Maps.newHashMap<String, Item>()!!
    val registeredModel = Maps.newHashMap<String, Model>()!!
    val registeredDisplay = Maps.newHashMap<String, Display>()!!
    val registeredGroup = Maps.newHashMap<String, Group>()!!
    val registeredMeta = TabooLibLoader.getPluginClassSafely(Zaphkiel.plugin)
        .filter { it.isAnnotationPresent(MetaKey::class.java) }
        .map { it.getAnnotation(MetaKey::class.java).value to it }
        .toMap(HashMap())

    fun mirrorFuture(id: String, func: Mirror.MirrorFuture.() -> Unit) {
        mirror.mirrorFuture(id, func)
    }

    fun getItem(id: String): ItemStream? {
        return registeredItem[id]?.build(null)
    }

    fun getItem(id: String, player: Player?): ItemStream? {
        return registeredItem[id]?.build(player)
    }

    fun getItemStack(id: String): ItemStack? {
        return registeredItem[id]?.build(null)?.save()
    }

    fun getItemStack(id: String, player: Player?): ItemStack? {
        return registeredItem[id]?.build(player)?.save()
    }

    fun getName(item: ItemStack): String? {
        val read = read(item)
        return if (read.isExtension()) {
            read.getZaphkielName()
        } else {
            null
        }
    }

    fun getData(item: ItemStack): NBTCompound? {
        val read = read(item)
        return if (read.isExtension()) {
            read.getZaphkielData()
        } else {
            null
        }
    }

    fun getUnique(item: ItemStack): NBTCompound? {
        val read = read(item)
        return if (read.isExtension()) {
            read.getZaphkielUniqueData()
        } else {
            null
        }
    }

    fun getItem(item: ItemStack): Item? {
        val read = read(item)
        return if (read.isExtension()) {
            read.getZaphkielItem()
        } else {
            null
        }
    }

    fun read(item: ItemStack): ItemStream {
        if (Items.isNull(item)) {
            throw RuntimeException("Could not read empty item.")
        }
        return ItemStream(item)
    }

    fun rebuild(player: Player?, inventory: Inventory) {
        (0 until inventory.size).forEach { i ->
            val item = inventory.getItem(i)
            if (Items.isNull(item)) {
                return@forEach
            }
            val rebuild = rebuild(player, item!!)
            if (rebuild.rebuild) {
                rebuild.save()
            }
        }
    }

    fun rebuild(player: Player?, item: ItemStack): ItemStream {
        if (Items.isNull(item)) {
            throw RuntimeException("Could not read empty item.")
        }
        val itemStream = ItemStream(item)
        if (itemStream.isVanilla()) {
            return itemStream
        }
        val pre = Events.call(ItemBuildEvent.Rebuild(player, itemStream, itemStream.shouldRefresh()))
        if (pre.isCancelled) {
            return itemStream
        }
        itemStream.rebuild = true
        return itemStream.getZaphkielItem().build(player, itemStream)
    }

    fun reloadItem() {
        loaded.forEach { TConfigWatcher.getInst().removeListener(it) }
        registeredItem.clear()
        registeredModel.clear()
        reloadModel(folderItem)
        reloadItem(folderItem)
        PluginReloadEvent.Item().call()
        Zaphkiel.logger.info("Loaded ${registeredItem.size} item(s) and ${registeredModel.size} model(s).")
    }

    fun reloadItem(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { reloadItem(it) }
        } else {
            val keys = ArrayList<String>()
            val task = Runnable {
                keys.forEach { registeredItem.remove(it) }
                var group: Group? = null
                val conf = Files.load(file)
                if (conf.contains("__group__")) {
                    val name = file.name.substring(0, file.name.indexOf("."))
                    group = Group(name, file, conf.getConfigurationSection("__group__")!!, priority = conf.getInt("__group__.priority"))
                    registeredGroup[name] = group
                }
                conf.getKeys(false).filter { !it.endsWith("$") && it != "__group__" }.forEach { key ->
                    try {
                        registeredItem[key] = Item(conf.getConfigurationSection(key)!!, group = group)
                    } catch (t: Throwable) {
                        t.printStackTrace()
                    }
                    keys.add(key)
                }
                Bukkit.getOnlinePlayers().forEach { player ->
                    rebuild(player, player.inventory)
                }
            }
            task.run()
            loaded.add(file)
            TConfigWatcher.getInst().addSimpleListener(file) {
                task.run()
            }
        }
    }

    fun reloadModel(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { reloadModel(it) }
        } else {
            val conf = Files.load(file)
            conf.getKeys(false).filter { it.endsWith("$") }.forEach { key ->
                registeredModel[key.substring(0, key.length - 1)] = Model(conf.getConfigurationSection(key)!!)
            }
        }
    }

    fun reloadDisplay() {
        registeredDisplay.clear()
        reloadDisplay(folderDisplay)
        PluginReloadEvent.Display().call()
        Zaphkiel.logger.info("Loaded ${registeredDisplay.size} display plan(s).")
    }

    fun reloadDisplay(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { reloadDisplay(it) }
        } else {
            val conf = Files.load(file)
            conf.getKeys(false).forEach { key ->
                registeredDisplay[key] = Display(conf.getConfigurationSection(key)!!)
            }
        }
    }

    fun asEnchantment(name: String): Enchantment? {
        try {
            return Enchantment::class.java.static(name.toUpperCase())
        } catch (t: Throwable) {
        }
        return null
    }

    fun asPotionEffect(name: String): PotionEffectType? {
        try {
            return PotionEffectType::class.java.static(name.toUpperCase())
        } catch (t: Throwable) {
        }
        return null
    }

    fun asEquipmentSlot(id: String) = when (id.toLowerCase()) {
        "0", "mainhand", "hand" -> Equipments.HAND
        "1", "head", "helmet" -> Equipments.HEAD
        "2", "chest", "chestplate" -> Equipments.CHEST
        "3", "legs", "leggings" -> Equipments.LEGS
        "4", "feet", "boots" -> Equipments.FEET
        "-1", "offhand" -> Equipments.OFF_HAND
        else -> null
    }

    fun toItemStack(data: ByteArray): ItemStack {
        ByteArrayInputStream(fromZip(data)).use { byteArrayInputStream ->
            BukkitObjectInputStream(byteArrayInputStream).use { bukkitObjectInputStream ->
                return bukkitObjectInputStream.readObject() as ItemStack
            }
        }
    }

    fun fromItemStack(itemStack: ItemStack): ByteArray {
        ByteArrayOutputStream().use { byteArrayOutputStream ->
            BukkitObjectOutputStream(byteArrayOutputStream).use { bukkitObjectOutputStream ->
                bukkitObjectOutputStream.writeObject(itemStack)
                return toZip(byteArrayOutputStream.toByteArray())
            }
        }
    }

    fun toInventory(inventory: Inventory, data: ByteArray) {
        ByteArrayInputStream(fromZip(data)).use { byteArrayInputStream ->
            BukkitObjectInputStream(byteArrayInputStream).use { bukkitObjectInputStream ->
                val index = bukkitObjectInputStream.readObject() as Array<Int>
                index.indices.forEach {
                    inventory.setItem(index[it], bukkitObjectInputStream.readObject() as ItemStack)
                }
            }
        }
    }

    fun fromInventory(inventory: Inventory, size: Int): ByteArray {
        ByteArrayOutputStream().use { byteArrayOutputStream ->
            BukkitObjectOutputStream(byteArrayOutputStream).use { bukkitObjectOutputStream ->
                (0..size).map { it to inventory.getItem(it) }.filter { Items.nonNull(it.second) }.toMap().run {
                    bukkitObjectOutputStream.writeObject(this.keys.toTypedArray())
                    this.forEach { (_, v) ->
                        bukkitObjectOutputStream.writeObject(v)
                    }
                }
            }
            return toZip(byteArrayOutputStream.toByteArray())
        }
    }

    fun toZip(byteArray: ByteArray): ByteArray {
        ByteArrayOutputStream().use { byteArrayOutputStream ->
            GZIPOutputStream(byteArrayOutputStream).use { gzipOutputStream ->
                gzipOutputStream.write(byteArray)
                gzipOutputStream.flush()
            }
            return byteArrayOutputStream.toByteArray()
        }
    }

    fun fromZip(byteArray: ByteArray): ByteArray {
        ByteArrayInputStream(byteArray).use { byteArrayOutputStream ->
            GZIPInputStream(byteArrayOutputStream).use { gzipInputStream ->
                return gzipInputStream.readBytes()
            }
        }
    }

    fun getMeta(root: ConfigurationSection): MutableList<Meta> {
        val copy = SecuredFile()
        return root.getConfigurationSection("meta")?.getKeys(false)?.mapNotNull { id ->
            if (id.endsWith("!!")) {
                copy.set("meta.${id.substring(0, id.length - 2)}", root.get("meta.$id"))
            } else {
                copy.set("meta.$id", root.get("meta.$id"))
            }
            val locked: Boolean
            val meta = Reflection.instantiateObject(
                if (id.endsWith("!!")) {
                    locked = true
                    registeredMeta[id.substring(0, id.length - 2)]
                } else {
                    locked = false
                    registeredMeta[id]
                } ?: return@mapNotNull null, copy
            ) as Meta
            meta.locked = locked
            meta
        }?.toMutableList() ?: ArrayList()
    }
}