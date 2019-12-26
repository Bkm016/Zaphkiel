package ink.ptms.zaphkiel.api

import ink.ptms.zaphkiel.Zaphkiel
import ink.ptms.zaphkiel.ZaphkielAPI
import io.izzel.taboolib.module.nms.nbt.NBTBase
import io.izzel.taboolib.util.Commands
import io.izzel.taboolib.util.lite.Effects
import io.izzel.taboolib.util.lite.Numbers
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerItemBreakEvent
import org.bukkit.inventory.ItemStack

/**
 * @Author sky
 * @Since 2019-12-15 22:30
 */
open class ItemAPI(val item: Item, val itemStack: ItemStack, val player: Player) {

    val itemStream = ItemStream(itemStack)

    fun command(sender: CommandSender, command: String) {
        Commands.dispatchCommand(sender, command)
    }

    fun commandOP(sender: CommandSender, command: String) {
        val op = sender.isOp
        sender.isOp = true
        try {
            Commands.dispatchCommand(sender, command)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        sender.isOp = op
    }

    fun commandConsole(command: String) {
        Commands.dispatchCommand(Bukkit.getConsoleSender(), command)
    }

    fun toCooldown(player: Player, gameTick: Int) {
        ZaphkielAPI.database.getData(player).set("Zaphkiel.cooldown.${item.id}", System.currentTimeMillis() + (gameTick * 50L))
    }

    fun toCooldown(player: Player, index: String, gameTick: Int) {
        ZaphkielAPI.database.getData(player).set("Zaphkiel.cooldown.$index", System.currentTimeMillis() + (gameTick * 50L))
    }

    fun isCooldown(player: Player): Boolean {
        return ZaphkielAPI.database.getData(player).getLong("Zaphkiel.cooldown.${item.id}") > System.currentTimeMillis()
    }

    fun isCooldown(player: Player, index: String): Boolean {
        return ZaphkielAPI.database.getData(player).getLong("Zaphkiel.cooldown.$index") > System.currentTimeMillis()
    }

    fun toCooldown(gameTick: Int) {
        itemStream.getZaphkielData().putDeep("cooldown.${item.id}", System.currentTimeMillis() + (gameTick * 50L))
    }

    fun isCooldown(): Boolean {
        return itemStream.getZaphkielData().getDeep("cooldown.${item.id}")?.asLong() ?: 0 > System.currentTimeMillis()
    }

    fun toRepair(value: Int): Boolean {
        val max = itemStream.getZaphkielData()["durability"] ?: return true
        val current = itemStream.getZaphkielData()["durability_current"] ?: NBTBase(max.asInt())
        val currentLatest = Math.max(Math.min(current.asInt() + value, max.asInt()), 0)
        return if (currentLatest > 0) {
            itemStream.getZaphkielData()["durability_current"] = NBTBase(currentLatest)
            true
        } else {
            val itemStackFinal = itemStack.clone()
            Bukkit.getPluginManager().callEvent(PlayerItemBreakEvent(player, itemStack))
            Bukkit.getScheduler().runTaskAsynchronously(Zaphkiel.getPlugin(), Runnable {
                player.world.playSound(player.location, Sound.ENTITY_ITEM_BREAK, 1f, Numbers.getRandomDouble(0.5, 1.5).toFloat())
                Effects.create(Particle.ITEM_CRACK, player.location.add(0.0, 1.0, 0.0)).speed(0.1).data(itemStackFinal).count(15).range(50.0).play()
            })
            itemStack.amount = 0
            false
        }
    }

    fun save() {
        itemStream.rebuild(player)
    }

    interface Injector {

        fun inject(itemAPI: ItemAPI): ItemAPI
    }

    companion object {

        val injectors = arrayListOf<Injector>()

        fun inject(injector: Injector) {
            injectors.add(injector)
        }

        fun get(itemAPI: ItemAPI): ItemAPI {
            var api = itemAPI
            injectors.forEach { injector ->
                api = injector.inject(api)
            }
            return api
        }
    }
}