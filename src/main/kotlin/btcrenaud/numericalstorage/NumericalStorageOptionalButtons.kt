package btcrenaud.numericalstorage

import btcrenaud.gui.api.GuiSlot
import org.bukkit.entity.Player

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
