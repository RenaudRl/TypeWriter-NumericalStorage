
package btc.renaud.numericalstorage

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Tags
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.engine.paper.entry.ManifestEntry

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
    @Help("Interest interval in hours (e.g. 24 for daily interest).")
    val interestIntervalHours: Double = 24.0,
    @Help("Interest rate in percentage (e.g. 2.0 for 2%).")
    val interestRate: Double = 0.0,
    @Help("Message when interest is applied. Placeholders: {amount} {new_balance} {rate} {prefix}")
    val interestMessage: String = "<green>You received {amount} interest! New balance: {new_balance}",
    @Help("List of permission based interest rates. Each entry defines a permission and a specific rate.")
    val interestRates: List<InterestRatePermission> = emptyList(),
    @Help("Configured levels. Messages support placeholders: {limit}, {amount}, {new_balance}, {prefix}, {level}")
    val levels: List<BankLevel> = emptyList(),
    @Help("Enable classic leveling system (deposit fills level, overflow goes to next).")
    val classicLeveling: Boolean = false
) : ManifestEntry

// Data class for permission based interest rates
data class InterestRatePermission(
    @Help("Permission node required for this rate.")
    val permission: String = "",
    @Help("Interest rate in percentage for this permission.")
    val rate: Double = 0.0
)
