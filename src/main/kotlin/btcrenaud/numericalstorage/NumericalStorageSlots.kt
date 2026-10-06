package btcrenaud.numericalstorage

const val INVENTORY_COLUMNS = 9
private const val MIN_ROWS = 1
private const val MAX_ROWS = 6

/**
 * Keeps the [requested] inventory indices that exist in a menu of [rows] rows, without duplicates.
 *
 * Out-of-range indices are dropped rather than failing the whole menu, because they come from the
 * web editor and a typo must not stop the storage from opening. [rows] is clamped like the menu does.
 */
fun visibleSlotIndices(requested: List<Int>, rows: Int): List<Int> {
    val capacity = rows.coerceIn(MIN_ROWS, MAX_ROWS) * INVENTORY_COLUMNS
    return requested.filter { it in 0 until capacity }.distinct()
}
