package btcrenaud.numericalstorage

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
    @Help("Configuration for the close button.")
    val closeButton: CloseButtonConfig = CloseButtonConfig(),
    @Help("Label for deposit buttons. Placeholders: {amount}, {balance}, {limit}")
    val depositLabel: String = "<green>Deposit",
    @Help("Label for deposit percentage buttons.")
    val depositPercentLabel: String = "<green>Deposit %",
    @Help("Label for deposit custom amount buttons.")
    val depositCustomLabel: String = "<green>Deposit Custom",
    @Help("Label for deposit ALL button.")
    val depositAllLabel: String = "<green>Deposit All",
    @Help("Label for deposit sub-menu opener button.")
    val depositSubmenuLabel: String = "<green>Deposit More...",
    @Help("Label for withdraw buttons. Placeholders: {amount}, {balance}, {limit}")
    val withdrawLabel: String = "<red>Withdraw",
    @Help("Label for withdraw percentage buttons.")
    val withdrawPercentLabel: String = "<red>Withdraw %",
    @Help("Label for withdraw custom amount buttons.")
    val withdrawCustomLabel: String = "<red>Withdraw Custom",
    @Help("Label for withdraw ALL button.")
    val withdrawAllLabel: String = "<red>Withdraw All",
    @Help("Label for withdraw sub-menu opener button.")
    val withdrawSubmenuLabel: String = "<red>Withdraw More...",
    @Help("Label for transfer buttons.")
    val transferLabel: String = "<aqua>Transfer",
    @Help("Label for transfer percentage buttons.")
    val transferPercentLabel: String = "<aqua>Transfer %",
    @Help("Label for transfer ALL button.")
    val transferAllLabel: String = "<aqua>Transfer All",
    @Help("Message when transfer source has insufficient balance. Placeholders: <amount>, <balance>, <prefix>")
    val transferInsufficientMessage: String = "<red>Insufficient balance in source for transfer.",
    @Help("Message when transfer target has insufficient capacity. Placeholders: <amount>, <limit>, <prefix>")
    val transferCapacityMessage: String = "<red>Target storage has insufficient capacity.",
    @Help("Message when transfer succeeds. Placeholders: <amount>, <new_balance>, <target_balance>, <prefix>")
    val transferSuccessMessage: String = "<aqua>Transferred <amount>! New source balance: <new_balance>, target: <target_balance>",
    @Help("Message when already at max level. Placeholders: <prefix>")
    val maxLevelMessage: String = "<red>Already at max level.",
    @Help("Message when criteria not met for upgrade. Placeholders: <prefix>")
    val criteriaNotMetMessage: String = "<red>You don't meet the requirements to level up.",
    @Help("Message when not enough funds to upgrade. Placeholders: <cost>, <prefix>")
    val notEnoughFundsMessage: String = "<red>Not enough funds to upgrade. Cost: <cost>",
    @Help("Sub-menus that can contain additional buttons (e.g. 'deposit_options' with 25/50/100% buttons). Each sub-menu has a unique ID referenced by SUB_MENU button type param.")
    val subMenus: Map<String, SubMenuConfig> = emptyMap(),
)

data class LevelButtonConfig(
    @Help("Item to display when level up is available. Placeholders: {cost}, {level}")
    val availableTemplate: ItemTemplate = ItemTemplate(name = "<green>Level Up Available", lore = listOf("<gray>Cost: {cost}", "<yellow>Click to upgrade!")),
    @Help("Item to display when max level is reached.")
    val maxLevelTemplate: ItemTemplate = ItemTemplate(name = "<red>Max Level Reached"),
    @Help("Slot in the inventory.")
    val slots: List<Int> = listOf(4)
)

data class TransactionButtonConfig(
    @Help("Type of transaction: DEPOSIT, WITHDRAW, or TRANSFER.")
    val type: TransactionType = TransactionType.DEPOSIT,
    @Help("Type of amount: FIXED, PERCENTAGE, CUSTOM, or ALL.")
    val amountType: AmountType = AmountType.FIXED,
    @Help("Amount for FIXED (value) or PERCENTAGE (0-100). Ignored for CUSTOM and ALL.")
    val amount: Double = 0.0,
    @Help("Item to display.")
    val template: ItemTemplate = ItemTemplate(name = "Transaction"),
    @Help("Slot in the inventory.")
    val slots: List<Int> = listOf(0),
    @Help("Optional label override for the button. If empty, the default label from the menu config is used.")
    val label: String = "",
    @Help("(TRANSFER only) Target definition ID. The balance is transferred from this storage to the target.")
    val transferTarget: String = ""
)

enum class TransactionType {
    DEPOSIT, WITHDRAW, TRANSFER
}

enum class AmountType {
    FIXED, PERCENTAGE, CUSTOM, ALL
}

data class OptionalButtonConfig(
    @Help("Item to display.")
    val template: ItemTemplate = ItemTemplate(name = "Info"),
    @Help("Slot in the inventory.")
    val slots: List<Int> = listOf(0)
)

data class CloseButtonConfig(
    @Help("Item to display for the close button.")
    val template: ItemTemplate = ItemTemplate(name = "<red>Close"),
    @Help("Slot in the inventory.")
    val slots: List<Int> = listOf(8)
)

/**
 * Configuration for a sub-menu that opens when a SUB_MENU or DEPOSIT_SUBMENU/WITHDRAW_SUBMENU button is clicked.
 * Sub-menus contain their own set of transaction buttons and optional buttons.
 */
data class SubMenuConfig(
    @Help("Title of the sub-menu.")
    val title: String = "Options",
    @Help("Number of rows in the sub-menu.")
    val rows: Int = 3,
    @Help("List of transaction buttons in this sub-menu.")
    val transactionButtons: List<TransactionButtonConfig> = emptyList(),
    @Help("List of optional buttons in this sub-menu.")
    val optionalButtons: List<OptionalButtonConfig> = emptyList(),
    @Help("Configuration for the back/return button.")
    val backButton: ItemTemplate = ItemTemplate(name = "<red>Back"),
    @Help("Slots for the back button.")
    val backButtonSlots: List<Int> = listOf(0),
    @Help("Configuration for the close button in the sub-menu.")
    val closeButton: CloseButtonConfig = CloseButtonConfig(),
    @Help("Background item to fill empty slots.")
    val fill: ItemTemplate = ItemTemplate(item = ItemStack(Material.GRAY_STAINED_GLASS_PANE).toItem()),
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
