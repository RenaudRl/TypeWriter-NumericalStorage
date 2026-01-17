package btc.renaud.numericalstorage

import com.typewritermc.engine.paper.utils.asMini
import com.typewritermc.engine.paper.utils.server
import com.typewritermc.engine.paper.extensions.placeholderapi.parsePlaceholders
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

class NumericalStorageMenu(
    val player: Player,
    val definition: NumericalStorageDefinitionEntry
) : InventoryHolder { // Rebuild trigger

    private val config = definition.menu
    private val inventory: Inventory = server.createInventory(
        this,
        config.rows * 9,
        config.title.parsePlaceholders(player).asMini()
    )

    init {
        render()
    }

    fun render() {
        inventory.clear()

        // Fill
        if (config.fillEnabled) {
            val fillItem = config.fill.buildItem(player, Material.GRAY_STAINED_GLASS_PANE)
            for (i in 0 until inventory.size) {
                inventory.setItem(i, fillItem)
            }
        }

        // Transaction Buttons
        config.transactionButtons.forEach { btn ->
            if (btn.slot in 0 until inventory.size) {
                val item = btn.template.buildItem(player, Material.STONE)
                inventory.setItem(btn.slot, item)
            }
        }

        // Optional Buttons
        config.optionalButtons.forEach { btn ->
            if (btn.slot in 0 until inventory.size) {
                val item = btn.template.buildItem(player, Material.PAPER)
                inventory.setItem(btn.slot, item)
            }
        }

        // Level Button - always show if a slot is configured
        val artifact = definition.artifact.get()
        if (artifact != null) {
            val level = artifact.getLevel(player.uniqueId)
            val maxLevel = definition.levels.size
            
            val btnConfig = config.levelButton
            if (btnConfig.slot in 0 until inventory.size) {
                if (level < maxLevel) {
                    val cost = definition.levels[level - 1].limit
                    
                    val costString = java.math.BigDecimal.valueOf(cost).toPlainString()
                    val nextLevelString = (level + 1).toString()
                    
                    val name = btnConfig.availableTemplate.name
                        .replace("{cost}", costString)
                        .replace("{level}", nextLevelString)
                        .parsePlaceholders(player)
                        .asMini()
                        .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)
                        
                    val lore = btnConfig.availableTemplate.lore.map {
                        it.replace("{cost}", costString)
                          .replace("{level}", nextLevelString)
                          .parsePlaceholders(player)
                          .asMini()
                          .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)
                    }
                    
                    val base = btnConfig.availableTemplate.item.build(player)
                    val meta = base.itemMeta ?: org.bukkit.Bukkit.getItemFactory().getItemMeta(base.type)
                    meta.displayName(name)
                    meta.lore(lore)
                    base.itemMeta = meta
                    
                    inventory.setItem(btnConfig.slot, base)
                } else {
                    val item = btnConfig.maxLevelTemplate.buildItem(player, Material.BARRIER)
                    inventory.setItem(btnConfig.slot, item)
                }
            }
        }
    }

    override fun getInventory(): Inventory = inventory
}
