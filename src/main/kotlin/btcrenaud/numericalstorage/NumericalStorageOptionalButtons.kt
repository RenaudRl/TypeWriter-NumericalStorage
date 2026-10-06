package btcrenaud.numericalstorage

import btcrenaud.gui.api.GuiSlot
import org.bukkit.entity.Player

/** [slots] with the ones sitting where an [optional] button is drawn removed, then the optional buttons added. */
fun withOptionalButtons(slots: List<GuiSlot>, optional: List<GuiSlot>): List<GuiSlot> {
    if (optional.isEmpty()) return slots
    val occupied = optional.mapTo(HashSet()) { it.x to it.y }
    return slots.filterNot { (it.x to it.y) in occupied } + optional
}

/**
 * Display-only slots for [buttons]: the item is shown, a click does nothing.
 *
 * Every index gets its own item instance so that no stack is shared between two inventory slots.
 */
fun renderOptionalButtons(player: Player, buttons: List<OptionalButtonConfig>, rows: Int): List<GuiSlot> =
    buttons.flatMap { button ->
        visibleSlotIndices(button.slots, rows).map { index ->
            GuiSlot(
                x = index % INVENTORY_COLUMNS,
                y = index / INVENTORY_COLUMNS,
                item = button.template.buildItem(player),
                isGhost = true,
            )
        }
    }
