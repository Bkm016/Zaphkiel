package ink.ptms.zaphkiel.module.meta

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.meta.ItemMeta

@MetaKey("unbreakable")
class MetaUnbreakable(root: ConfigurationSection) : Meta(root) {

    val unbreakable = root.getBoolean("meta.unbreakable")

    override fun build(itemMeta: ItemMeta) {
        itemMeta.isUnbreakable = unbreakable
    }

    override fun drop(itemMeta: ItemMeta) {
        itemMeta.isUnbreakable = false
    }

    override fun toString(): String {
        return "MetaUnbreakable(unbreakable=$unbreakable)"
    }
}