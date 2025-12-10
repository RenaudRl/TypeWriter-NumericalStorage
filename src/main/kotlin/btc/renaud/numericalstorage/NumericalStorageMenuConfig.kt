package btc.renaud.numericalstorage

import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.engine.paper.content.modes.custom.HoldingItemContentMode
import com.typewritermc.core.extension.annotations.ContentEditor
import com.typewritermc.core.extension.annotations.Colored
import com.typewritermc.core.extension.annotations.MultiLine
import com.typewritermc.core.extension.annotations.Placeholder
import com.typewritermc.engine.paper.utils.item.Item
import com.typewritermc.engine.paper.utils.item.toItem
import com.typewritermc.engine.paper.extensions.placeholderapi.parsePlaceholders
import com.typewritermc.engine.paper.utils.asMini
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * Template describing how an item should look inside the menus.
 */
data class ItemTemplate(
    @Help("Item used when rendering the item")
    @ContentEditor(HoldingItemContentMode::class)
    val item: Item = Item.Empty,
    @Help("Display name of the item")
    @Placeholder
    @Colored
    val name: String = "",
    @Help("Lore displayed on the item")
    @Placeholder
    @Colored
    @MultiLine
    val lore: List<String> = emptyList(),
)

data class NumericalStorageMenuConfig(
    @Help("Title of the menu.")
    val title: String = "Storage",
    @Help("Number of rows in the menu.")
    val rows: Int = 3,
    @Help("Background item to fill empty slots.")
    val fillEnabled: Boolean = true,
    @Help("Item used to fill empty slots.")
    val fill: ItemTemplate = ItemTemplate(item = ItemStack(Material.GRAY_STAINED_GLASS_PANE).toItem()),
    @Help("List of transaction buttons (add/remove).")
    val transactionButtons: List<TransactionButtonConfig> = emptyList(),
    @Help("List of optional buttons (display only).")
    val optionalButtons: List<OptionalButtonConfig> = emptyList(),
    @Help("Configuration for the custom amount dialog.")
    val customAmountDialog: DialogConfig = DialogConfig(),
    @Help("Configuration for the level up button (only used if levelingMode is MANUAL).")
    val levelButton: LevelButtonConfig = LevelButtonConfig(),
    @Help("Message when already at max level. Placeholders: <prefix>")
    val maxLevelMessage: String = "<red>Already at max level.",
    @Help("Message when criteria not met for upgrade. Placeholders: <prefix>")
    val criteriaNotMetMessage: String = "<red>You don't meet the requirements to level up.",
    @Help("Message when not enough funds to upgrade. Placeholders: <cost>, <prefix>")
    val notEnoughFundsMessage: String = "<red>Not enough funds to upgrade. Cost: <cost>"
)

data class LevelButtonConfig(
    @Help("Item to display when level up is available. Placeholders: {cost}, {level}")
    val availableTemplate: ItemTemplate = ItemTemplate(name = "<green>Level Up Available", lore = listOf("<gray>Cost: {cost}", "<yellow>Click to upgrade!")),
    @Help("Item to display when max level is reached.")
    val maxLevelTemplate: ItemTemplate = ItemTemplate(name = "<red>Max Level Reached"),
    @Help("Slot in the inventory.")
    val slot: Int = 4
)

data class TransactionButtonConfig(
    @Help("Type of transaction: DEPOSIT or WITHDRAW.")
    val type: TransactionType = TransactionType.DEPOSIT,
    @Help("Type of amount: FIXED, PERCENTAGE, or CUSTOM.")
    val amountType: AmountType = AmountType.FIXED,
    @Help("Amount for FIXED (value) or PERCENTAGE (0-100). Ignored for CUSTOM.")
    val amount: Double = 0.0,
    @Help("Item to display.")
    val template: ItemTemplate = ItemTemplate(name = "Transaction"),
    @Help("Slot in the inventory.")
    val slot: Int = 0
)

enum class TransactionType {
    DEPOSIT, WITHDRAW
}

enum class AmountType {
    FIXED, PERCENTAGE, CUSTOM
}

data class OptionalButtonConfig(
    @Help("Item to display.")
    val template: ItemTemplate = ItemTemplate(name = "Info"),
    @Help("Slot in the inventory.")
    val slot: Int = 0
)

data class DialogConfig(
    @Help("Title of the dialog for custom amount input.")
    val title: String = "Enter Amount",
    @Help("Label for the input field.")
    val inputLabel: String = "Amount",
    @Help("Placeholder text for the input field.")
    val inputPlaceholder: String = "100",
    @Help("Width of the input field.")
    val inputWidth: Int = 200,
    @Help("Maximum length of the input field.")
    val maxLength: Int = 16,
    @Help("Text for the submit button.")
    val submitButton: String = "Submit",
    @Help("Message shown on invalid input.")
    val invalidInputMessage: String = "<red>Invalid amount!</red>"
)

fun ItemTemplate.buildItem(
    player: org.bukkit.entity.Player,
    fallbackMaterial: Material = Material.STONE,
    nameOverride: net.kyori.adventure.text.Component? = null,
    loreOverride: List<net.kyori.adventure.text.Component>? = null,
): ItemStack {
    val base = if (item != Item.Empty) item.build(player) else ItemStack(fallbackMaterial)
    val displayNameComponent = nameOverride ?: name.parsePlaceholders(player).asMini().decoration(TextDecoration.ITALIC, false)
    val loreComponents = loreOverride ?: lore.flatMap { it.split("\n") }
        .map { it.parsePlaceholders(player).asMini().decoration(TextDecoration.ITALIC, false) }
    
    val resolvedMeta = base.itemMeta ?: org.bukkit.Bukkit.getItemFactory().getItemMeta(base.type)
    if (resolvedMeta != null) {
        resolvedMeta.displayName(displayNameComponent)
        resolvedMeta.lore(loreComponents)
        base.itemMeta = resolvedMeta
    }
    return base
}
