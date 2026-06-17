package btcrenaud.numericalstorage.entries.action

enum class NumericalStorageButtonType {
    DEPOSIT_FIXED,
    DEPOSIT_PERCENT,
    DEPOSIT_CUSTOM,
    DEPOSIT_ALL,
    DEPOSIT_SUBMENU,
    WITHDRAW_FIXED,
    WITHDRAW_PERCENT,
    WITHDRAW_CUSTOM,
    WITHDRAW_ALL,
    WITHDRAW_SUBMENU,
    TRANSFER_FIXED,
    TRANSFER_PERCENT,
    TRANSFER_ALL,
    UPGRADE_LEVEL,
    NAV_CLOSE,
    /** Opens a sub-menu by its ID. Format: SUB_MENU:<subMenuId> */
    SUB_MENU,
}
