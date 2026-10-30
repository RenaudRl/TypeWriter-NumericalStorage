package btcrenaud.numericalstorage

import btcrenaud.numericalstorage.entries.action.NumericalStorageMenuTitle
import kotlin.test.Test
import kotlin.test.assertEquals

class NumericalStorageMenuTitleTest {

    @Test
    fun aTitleWrittenByTheEditorWinsOverTheDefinitionTitle() {
        assertEquals("<shift:-8>Bank", NumericalStorageMenuTitle.template("<shift:-8>Bank", "Storage"))
    }

    @Test
    fun anEmptyTitleKeepsTheDefinitionTitleSoAPageSavedBeforeTheFieldExistedIsUnchanged() {
        assertEquals("Storage", NumericalStorageMenuTitle.template("", "Storage"))
    }
}
