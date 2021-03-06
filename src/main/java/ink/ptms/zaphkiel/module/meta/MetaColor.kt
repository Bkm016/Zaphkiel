package ink.ptms.zaphkiel.module.meta

import io.izzel.taboolib.util.Coerce
import org.bukkit.Color
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.inventory.meta.PotionMeta

@MetaKey("color")
class MetaColor(root: ConfigurationSection) : Meta(root) {

    val color = root.getString("meta.color")?.split("-")?.run {
        Color.fromRGB(Coerce.toInteger(getOrElse(0) { 0 }), Coerce.toInteger(getOrElse(1) { 0 }), Coerce.toInteger(getOrElse(2) { 0 }))
    }

    override fun build(itemMeta: ItemMeta) {
        if (itemMeta is PotionMeta) {
            itemMeta.color = color ?: return
        } else if (itemMeta is LeatherArmorMeta) {
            itemMeta.setColor(color ?: return)
        }
    }

    override fun drop(itemMeta: ItemMeta) {
        if (itemMeta is PotionMeta) {
            itemMeta.color = null
        } else if (itemMeta is LeatherArmorMeta) {
            itemMeta.setColor(null)
        }
    }

    override fun toString(): String {
        return "MetaColor(color=$color)"
    }
}