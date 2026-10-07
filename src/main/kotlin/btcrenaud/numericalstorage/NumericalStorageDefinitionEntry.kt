package btcrenaud.numericalstorage

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Colored
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Placeholder
import com.typewritermc.core.extension.annotations.Tags
import com.typewritermc.engine.paper.entry.ManifestEntry
import com.typewritermc.engine.paper.utils.CronExpression

@Entry(
    "numericalstorage_definition",
    "Defines a configurable numerical storage",
    Colors.BLUE,
    "fa6-solid:piggy-bank"
)
@Tags("numericalstorage", "definition")
class NumericalStorageDefinitionEntry(
    override val id: String = "",
    override val name: String = "",
    @Help("Display name shown to players.")
    val displayName: String = "",
    @Help("Prefix used in messages.")
    val prefix: String = "",
    @Help("Artifact storing player data")
    val artifact: Ref<PlayerNumericalStorageArtifactEntry> = emptyRef(),
    @Help("Enable interest system based on real time.")
    val interestEnabled: Boolean = false,
    @Help("Interest rate in percentage (e.g. 2.0 for 2%).")
    val interestRate: Double = 0.0,
    @Help("Message when interest is applied. Placeholders: <amount> <new_balance> <rate> <prefix>")
    val interestMessage: String = "<green>You received <amount> interest! New balance: <new_balance>",
    @Help("List of permission based interest rates. Each entry defines a permission and a specific rate.")
    val interestRates: List<InterestRatePermission> = emptyList(),
    @Help("Configured levels. Messages support placeholders: {limit}, {amount}, {new_balance}, {prefix}, {level}")
    val levels: List<BankLevel> = emptyList(),
    @Help("Menu configuration for this storage.")
    val menu: NumericalStorageMenuConfig = NumericalStorageMenuConfig(),
    @Help("Cron expression for interest application (e.g. '0 0 12 * * *' for daily at 12:00).")
    val interestCron: CronExpression = CronExpression.default(),
    @Help("Transaction configuration for deposits/withdrawals via menu and commands.")
    val transaction: TransactionConfig = TransactionConfig(),
    @Help("When enabled, balances are stored per-profile instead of globally: one balance per MMOProfiles profile. Without MMOProfiles, or before a profile is selected, the balance is the player's.")
    val profileMode: Boolean = false,
    @Help("Replies shown to the sender of the /typewriter ns admin commands (add, remove, level, reset, open).")
    val adminMessages: AdminCommandMessages = AdminCommandMessages()
) : ManifestEntry

/**
 * Replies of the `/typewriter ns` admin commands. They go to the sender (the console included), so they carry
 * their own prefix: the defaults reproduce the engine's `Typewriter »` header, and an admin can restyle or drop it.
 */
data class AdminCommandMessages(
    @Help("Reply to /typewriter ns reset. Placeholders: <player>, <storage>, <prefix>")
    @Colored
    @Placeholder
    val resetMessage: String = "<red><bold>Typewriter »<reset><white> Numerical storage reset for <player>.",
    @Help("Reply to /typewriter ns level. Placeholders: <player>, <level>, <storage>, <prefix>")
    @Colored
    @Placeholder
    val levelSetMessage: String = "<red><bold>Typewriter »<reset><white> Set numerical storage level to <level> for <player>.",
    @Help("Reply to /typewriter ns add. Placeholders: <player>, <amount>, <storage>, <prefix>")
    @Colored
    @Placeholder
    val addMessage: String = "<red><bold>Typewriter »<reset><white> Added <amount> to <player>.",
    @Help("Reply to /typewriter ns remove. Placeholders: <player>, <amount>, <storage>, <prefix>")
    @Colored
    @Placeholder
    val removeMessage: String = "<red><bold>Typewriter »<reset><white> Removed <amount> from <player>.",
    @Help("Reply to /typewriter ns open when no Open Numerical Storage Menu entry is linked to the definition. Placeholders: <storage>, <prefix>")
    @Colored
    @Placeholder
    val noMenuMessage: String = "<red><bold>Typewriter »<reset> <red>No menu entry found for storage '<storage>'. Create a 'numericalstorage_open_menu' entry in a Typewriter page.",
    @Help("Reply to /typewriter ns add and remove when the amount is not a positive number. Placeholders: <amount> (the value typed), <player>, <storage>, <prefix>")
    @Colored
    @Placeholder
    val invalidAmountMessage: String = "<red><bold>Typewriter »<reset> <red>Invalid amount '<amount>'. Use a positive number.",
    @Help("Reply to /typewriter ns add, remove, level and reset when no artifact is linked to the definition. Placeholders: <player>, <storage>, <prefix>")
    @Colored
    @Placeholder
    val noArtifactMessage: String = "<red><bold>Typewriter »<reset> <red>Storage '<storage>' has no artifact linked. Link a 'player_numericalstorage_artifact' entry to it."
)

data class InterestRatePermission(
    @Help("Permission node required for this rate.")
    val permission: String = "",
    @Help("Interest rate in percentage for this permission.")
    val rate: Double = 0.0
)

enum class TransactionMode {
    @Help("Uses addCommand/removeCommand and amountPlaceholder for economy integration.")
    INTERNAL,
    @Help("Uses Vault Economy API directly. No commands or placeholder needed.")
    VAULT
}

data class TransactionConfig(
    @Help("How the external economy is integrated: INTERNAL (commands + placeholder) or VAULT (direct API).")
    val transactionMode: TransactionMode = TransactionMode.INTERNAL,
    @Help("(INTERNAL mode) Placeholder to read the source balance (e.g. %vault_eco_balance%). Ignored in VAULT mode.")
    val amountPlaceholder: String = "",
    @Help("Message when adding to storage succeeds. Placeholders: <amount>, <new_balance>, <limit>, <prefix>")
    val addSuccessMessage: String = "<green>Successfully added <amount>! New balance: <new_balance>",
    @Help("Message when adding exceeds limit. Placeholders: <amount>, <balance>, <limit>, <prefix>")
    val addErrorMessage: String = "<red>Cannot add <amount>. Would exceed limit of <limit>.",
    @Help("Message when removing from storage succeeds. Placeholders: <amount>, <new_balance>, <limit>, <prefix>")
    val removeSuccessMessage: String = "<green>Successfully removed <amount>! New balance: <new_balance>",
    @Help("Message when removing exceeds available balance. Placeholders: <amount>, <balance>, <limit>, <prefix>")
    val removeErrorMessage: String = "<red>Cannot remove <amount>. Insufficient balance (<balance>).",
    @Help("(INTERNAL mode) Command executed before the persistent DEPOSIT mutation. Placeholders: {amount}, {player}. Ignored in VAULT mode.")
    val addCommand: String = "",
    @Help("(INTERNAL mode) Command executed before the persistent WITHDRAW mutation. Placeholders: {amount}, {player}. Ignored in VAULT mode.")
    val removeCommand: String = "",
    @Help("Message when no funds available for transaction. Placeholders: <prefix>")
    val noFundsMessage: String = "<red>No funds available.",
    @Help("Message when amount is invalid. Placeholders: <prefix>")
    val invalidAmountMessage: String = "<red>Invalid amount.",
    @Help("(VAULT mode) Message when Vault deposit fails (e.g. insufficient economy funds). Placeholders: <amount>, <prefix>")
    val vaultInsufficientMessage: String = "<red>Insufficient funds in your economy balance.",
    @Help("(VAULT mode) Message when Vault integration is unavailable. Placeholders: <prefix>")
    val vaultUnavailableMessage: String = "<red>Economy system is not available."
)
