package btcrenaud.numericalstorage

import kotlin.test.Test
import kotlin.test.assertEquals

class NumericalStorageOptionalButtonsTest {
    @Test
    fun keepsSlotsInsideTheMenuAndDropsTheOthers() {
        assertEquals(listOf(0, 8, 26), visibleSlotIndices(listOf(0, 8, 26, 27, -1), rows = 3))
    }

    @Test
    fun removesDuplicatedSlotsSoOneSlotHoldsOneItem() {
        assertEquals(listOf(4, 5), visibleSlotIndices(listOf(4, 4, 5), rows = 1))
    }

    @Test
    fun clampsTheRowCountLikeTheMenuDoes() {
        assertEquals(listOf(8, 9, 53), visibleSlotIndices(listOf(8, 9, 53, 54), rows = 9))
        assertEquals(listOf(8), visibleSlotIndices(listOf(8, 9), rows = 0))
    }
}
