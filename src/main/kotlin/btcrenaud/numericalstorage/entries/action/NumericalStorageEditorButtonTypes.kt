package btcrenaud.numericalstorage.entries.action

import btcrenaud.gui.editor.api.ButtonTypeContribution
import btcrenaud.gui.editor.api.ButtonTypeInfo

/**
 * What NumericalStorage tells the OmniGUI web editor about its buttons, so that a slot of a storage menu offers them
 * in its `buttonType` list.
 *
 * Every type is derived from [NumericalStorageButtonType], the enum the resolver switches on: a new type cannot be
 * resolved without being described, because the `when` below has no fallback branch. The id and the prefix are
 * written into saved menus and never change once released.
 */
internal object NumericalStorageEditorButtonTypes {
    const val EXTENSION_ID = "NumericalStorage"

    /** The tag prefix of the buttons; the tag is `numericalstorage_button:TYPE[:param]`. */
    const val PREFIX = "numericalstorage_button:"

    private val MENU_ENTRY_TYPES = setOf("numericalstorage_menu", "numericalstorage_open_menu")

    fun contribution(): ButtonTypeContribution = ButtonTypeContribution(
        extensionId = EXTENSION_ID,
        displayName = "Numerical Storage",
        prefix = PREFIX,
        types = NumericalStorageButtonType.entries.map { describe(it) },
        appliesTo = { context -> context.entryType in MENU_ENTRY_TYPES },
    )

    private fun describe(type: NumericalStorageButtonType): ButtonTypeInfo = when (type) {
        NumericalStorageButtonType.DEPOSIT_FIXED ->
            amount(type, "Deposit (fixed)", "Deposits a fixed amount. The parameter is the amount (DEPOSIT_FIXED:100).")
        NumericalStorageButtonType.DEPOSIT_PERCENT ->
            amount(type, "Deposit (percent)", "Deposits a percentage of the balance. The parameter is the percentage (DEPOSIT_PERCENT:50).")
        NumericalStorageButtonType.DEPOSIT_CUSTOM ->
            action(type, "Deposit (custom)", "Asks the player for the amount to deposit.")
        NumericalStorageButtonType.DEPOSIT_ALL ->
            action(type, "Deposit (all)", "Deposits everything the player can.")
        NumericalStorageButtonType.DEPOSIT_SUBMENU ->
            amount(type, "Deposit sub-menu", "Opens a deposit sub-menu. The parameter is the sub-menu id.")
        NumericalStorageButtonType.WITHDRAW_FIXED ->
            amount(type, "Withdraw (fixed)", "Withdraws a fixed amount. The parameter is the amount (WITHDRAW_FIXED:100).")
        NumericalStorageButtonType.WITHDRAW_PERCENT ->
            amount(type, "Withdraw (percent)", "Withdraws a percentage of the storage. The parameter is the percentage (WITHDRAW_PERCENT:50).")
        NumericalStorageButtonType.WITHDRAW_CUSTOM ->
            action(type, "Withdraw (custom)", "Asks the player for the amount to withdraw.")
        NumericalStorageButtonType.WITHDRAW_ALL ->
            action(type, "Withdraw (all)", "Withdraws everything the storage holds.")
        NumericalStorageButtonType.WITHDRAW_SUBMENU ->
            amount(type, "Withdraw sub-menu", "Opens a withdraw sub-menu. The parameter is the sub-menu id.")
        NumericalStorageButtonType.TRANSFER_FIXED ->
            amount(type, "Transfer (fixed)", "Transfers a fixed amount. The parameter is the amount (TRANSFER_FIXED:100).")
        NumericalStorageButtonType.TRANSFER_PERCENT ->
            amount(type, "Transfer (percent)", "Transfers a percentage of the storage. The parameter is the percentage (TRANSFER_PERCENT:50).")
        NumericalStorageButtonType.TRANSFER_ALL ->
            action(type, "Transfer (all)", "Transfers everything the storage holds.")
        NumericalStorageButtonType.UPGRADE_LEVEL ->
            action(type, "Upgrade level", "Upgrades the storage to its next level.")
        NumericalStorageButtonType.NAV_CLOSE ->
            ButtonTypeInfo(type.name, "Close", "Closes the menu.", category = NAVIGATION)
        NumericalStorageButtonType.SUB_MENU ->
            amount(type, "Sub-menu", "Opens a sub-menu by its id. The parameter is the sub-menu id (SUB_MENU:id).")
    }

    private fun action(type: NumericalStorageButtonType, label: String, description: String) =
        ButtonTypeInfo(type.name, label, description, category = ACTION)

    /** A type that takes a `:param` and may therefore be placed on several slots, each with its own value. */
    private fun amount(type: NumericalStorageButtonType, label: String, description: String) =
        ButtonTypeInfo(type.name, label, description, repeatable = true, parameterized = true, category = ACTION)

    private const val ACTION = "action"
    private const val NAVIGATION = "navigation"
}
