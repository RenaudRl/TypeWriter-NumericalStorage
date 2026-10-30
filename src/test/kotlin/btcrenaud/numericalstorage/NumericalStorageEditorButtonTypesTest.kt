package btcrenaud.numericalstorage

import btcrenaud.gui.editor.api.ButtonTypeRegistry
import btcrenaud.gui.editor.api.MenuEditContext
import btcrenaud.numericalstorage.entries.action.NumericalStorageButtonType
import btcrenaud.numericalstorage.entries.action.NumericalStorageEditorButtonTypes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NumericalStorageEditorButtonTypesTest {

    private val contribution = NumericalStorageEditorButtonTypes.contribution()

    private fun menu(entryType: String) = object : MenuEditContext {
        override val entryId = "test"
        override val entryType = entryType
        override fun stringField(name: String): String? = null
    }

    @Test
    fun everyButtonTypeTheResolverHandlesIsDeclared() {
        val resolved = NumericalStorageButtonType.entries.map { it.name }
        val declared = contribution.types.map { it.id }

        assertEquals(resolved.sorted(), declared.sorted())
    }

    @Test
    fun theTypesThatTakeAParameterAreOfferedWithOne() {
        val parameterized = contribution.types.filter { it.parameterized }.map { it.id }.toSet()

        assertEquals(
            setOf(
                "DEPOSIT_FIXED", "DEPOSIT_PERCENT", "DEPOSIT_SUBMENU",
                "WITHDRAW_FIXED", "WITHDRAW_PERCENT", "WITHDRAW_SUBMENU",
                "TRANSFER_FIXED", "TRANSFER_PERCENT", "SUB_MENU",
            ),
            parameterized,
        )
    }

    @Test
    fun theTypesAreOfferedToTheTwoStorageMenusOnly() {
        assertTrue(contribution.appliesTo(menu("numericalstorage_menu")))
        assertTrue(contribution.appliesTo(menu("numericalstorage_open_menu")))
        assertFalse(contribution.appliesTo(menu("open_gui")))
    }

    @Test
    fun onceRegisteredTheEditorKnowsEveryTypeAndAParameterisedOneByItsBaseId() {
        ButtonTypeRegistry.register(contribution)
        try {
            val context = menu("numericalstorage_open_menu")
            contribution.types.forEach {
                assertEquals(true, ButtonTypeRegistry.isKnownButtonType(it.id, contribution.prefix, context), it.id)
            }
            assertEquals(true, ButtonTypeRegistry.isKnownButtonType("DEPOSIT_FIXED:100", contribution.prefix, context))
            assertEquals(false, ButtonTypeRegistry.isKnownButtonType("STORAGE_STATUS", contribution.prefix, context))
        } finally {
            ButtonTypeRegistry.unregister(NumericalStorageEditorButtonTypes.EXTENSION_ID)
        }
        assertTrue(ButtonTypeRegistry.all().none { it.extensionId == NumericalStorageEditorButtonTypes.EXTENSION_ID })
    }
}
