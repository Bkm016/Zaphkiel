package ink.ptms.zaphkiel.module.meta

import ink.ptms.zaphkiel.api.Item
import io.izzel.taboolib.module.nms.nbt.NBTCompound
import org.bukkit.inventory.meta.ItemMeta

abstract class Meta(val item: Item) {

    open fun build(itemMeta: ItemMeta) {

    }

    open fun build(compound: NBTCompound) {

    }
}