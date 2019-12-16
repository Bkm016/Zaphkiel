package ink.ptms.zaphkiel.module

import ink.ptms.zaphkiel.Zaphkiel
import ink.ptms.zaphkiel.ZaphkielAPI
import io.izzel.taboolib.module.inject.TListener
import io.izzel.taboolib.util.item.Items
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.*

/**
 * @Author sky
 * @Since 2019-12-15 22:22
 */
@TListener
class ItemEvents : Listener {

    @EventHandler(ignoreCancelled = true)
    fun e(e: PlayerDropItemEvent) {
        val item = ZaphkielAPI.read(e.itemDrop.itemStack)
        if (item.isExtension()) {
            item.getZaphkielItem().eval("onDrop", e)
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun e(e: PlayerItemBreakEvent) {
        val item = ZaphkielAPI.read(e.brokenItem)
        if (item.isExtension()) {
            item.getZaphkielItem().eval("onBreak", e)
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun e(e: PlayerItemConsumeEvent) {
        val item = ZaphkielAPI.read(e.item)
        if (item.isExtension()) {
            item.getZaphkielItem().eval("onConsume", e)
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun e(e: PlayerItemDamageEvent) {
        val item = ZaphkielAPI.read(e.item)
        if (item.isExtension()) {
            item.getZaphkielItem().eval("onItemDamage", e)
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun e(e: PlayerSwapHandItemsEvent) {
        if (Items.nonNull(e.mainHandItem)) {
            val item = ZaphkielAPI.read(e.mainHandItem!!)
            if (item.isExtension()) {
                item.getZaphkielItem().eval("onSwapHand", e)
            }
        }
        if (Items.nonNull(e.offHandItem)) {
            val item = ZaphkielAPI.read(e.offHandItem!!)
            if (item.isExtension()) {
                item.getZaphkielItem().eval("onSwapHand", e)
            }
        }
    }

    @EventHandler
    fun e(e: PlayerInteractEvent) {
        if (Items.nonNull(e.item)) {
            val item = ZaphkielAPI.read(e.item!!)
            if (item.isVanilla()) {
                return
            }
            when (e.action) {
                Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK -> {
                    item.getZaphkielItem().eval("onLeftCLick", e)
                }
                Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK -> {
                    item.getZaphkielItem().eval("onRightClick", e)
                }
                else -> {
                }
            }
        }
    }
}