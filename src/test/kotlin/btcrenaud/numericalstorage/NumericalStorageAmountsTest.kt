package btcrenaud.numericalstorage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NumericalStorageAmountsTest {
    @Test
    fun acceptsDecimalAndRejectsInvalidValues() {
        assertEquals("12.50", "12.50".toFinitePositiveAmountOrNull()?.toPlainString())
        assertNull("0".toFinitePositiveAmountOrNull())
        assertNull("-1".toFinitePositiveAmountOrNull())
        assertNull("NaN".toFinitePositiveDoubleOrNull())
        assertNull("Infinity".toFinitePositiveDoubleOrNull())
    }
}
