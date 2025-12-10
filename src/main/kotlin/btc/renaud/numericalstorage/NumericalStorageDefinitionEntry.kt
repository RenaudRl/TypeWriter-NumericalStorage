
package btc.renaud.numericalstorage

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Tags
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
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
    @Help("Message when interest is applied. Placeholders: {amount} {new_balance} {rate} {prefix}")
    val interestMessage: String = "<green>You received {amount} interest! New balance: {new_balance}",
    @Help("List of permission based interest rates. Each entry defines a permission and a specific rate.")
    val interestRates: List<InterestRatePermission> = emptyList(),
    @Help("Configured levels. Messages support placeholders: {limit}, {amount}, {new_balance}, {prefix}, {level}")
    val levels: List<BankLevel> = emptyList(),
    @Help("Menu configuration for this storage.")
    val menu: NumericalStorageMenuConfig = NumericalStorageMenuConfig(),
    @Help("Cron expression for interest application (e.g. '0 0 12 * * *' for daily at 12:00).")
    val interestCron: CronExpression = CronExpression.default(),
    @Help("Transaction configuration for deposits/withdrawals via menu and commands.")
    val transaction: TransactionConfig = TransactionConfig()
) : ManifestEntry

// Data class for permission based interest rates
data class InterestRatePermission(
    @Help("Permission node required for this rate.")
    val permission: String = "",
    @Help("Interest rate in percentage for this permission.")
    val rate: Double = 0.0
)

/**
 * Configuration for transactions (add/remove) via menu and commands.
 * Supports dynamic amount calculation via placeholders.
 */
data class TransactionConfig(
    @Help("Placeholder to calculate the transaction amount dynamically (e.g. %vault_eco_balance%). This placeholder will be used for all transaction calculations.")
    val amountPlaceholder: String = "",
    @Help("Message when adding to storage succeeds. Placeholders: <amount>, <new_balance>, <limit>, <prefix>")
    val addSuccessMessage: String = "<green>Successfully added <amount>! New balance: <new_balance>",
    @Help("Message when adding exceeds limit. Placeholders: <amount>, <balance>, <limit>, <prefix>")
    val addErrorMessage: String = "<red>Cannot add <amount>. Would exceed limit of <limit>.",
    @Help("Message when removing from storage succeeds. Placeholders: <amount>, <new_balance>, <limit>, <prefix>")
    val removeSuccessMessage: String = "<green>Successfully removed <amount>! New balance: <new_balance>",
    @Help("Message when removing exceeds available balance. Placeholders: <amount>, <balance>, <limit>, <prefix>")
    val removeErrorMessage: String = "<red>Cannot remove <amount>. Insufficient balance (<balance>).",
    @Help("(optional) Command to execute after successful ADD. Placeholder: {amount}, {player}")
    val addCommand: String = "",
    @Help("(optional) Command to execute after successful REMOVE. Placeholder: {amount}, {player}")
    val removeCommand: String = "",
    @Help("Message when no funds available for percentage transaction. Placeholders: <prefix>")
    val noFundsMessage: String = "<red>No funds available.",
    @Help("Message when amount is invalid. Placeholders: <prefix>")
    val invalidAmountMessage: String = "<red>Invalid amount."
)

