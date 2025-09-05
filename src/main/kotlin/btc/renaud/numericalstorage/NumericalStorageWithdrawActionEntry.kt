package btc.renaud.numericalstorage

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Tags
import com.typewritermc.engine.paper.entry.Criteria
import com.typewritermc.engine.paper.entry.Modifier
import com.typewritermc.engine.paper.entry.TriggerableEntry
import com.typewritermc.engine.paper.entry.entries.ActionEntry
import com.typewritermc.engine.paper.entry.entries.ActionTrigger
import com.typewritermc.engine.paper.utils.sendMiniWithResolvers
import com.typewritermc.engine.paper.extensions.placeholderapi.parsePlaceholders
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.math.BigDecimal

@Entry(
    "numericalstorage_withdraw",
    "Withdraw amount from the player's numericalstorage",
    Colors.RED,
    "fa6-solid:piggy-bank"
)
@Tags("numericalstorage", "action")
class NumericalStorageWithdrawActionEntry(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("Storage definition to withdraw from/Remove Xp.")
    val storage: Ref<NumericalStorageDefinitionEntry> = emptyRef(),
    @Help("Amount to withdraw.")
    val amount: Double = 0.0,
    @Help("(optional)Placeholder to calculate the amount.")
    val amountPlaceholder: String = "",
    @Help("How to interpret the amount (FIXED or PERCENTAGE).")
    val amountType: AmountType = AmountType.FIXED,
    @Help("(optional)Command to execute. Placeholders: {amount}")
    val command: String = ""
) : ActionEntry {
    override fun ActionTrigger.execute() {
        val player = player
        val definition = storage.get() ?: return
        val artifactEntry = definition.artifact.get() ?: return
        val levelNumber = artifactEntry.getLevel(player.uniqueId)
        val level = definition.levels.getOrNull(levelNumber - 1) ?: return
        val current = artifactEntry.getBalance(player.uniqueId)
        val withdrawAmount = calculateAmount(player, current)
        if (withdrawAmount > current) {
            player.sendMini(level.withdrawErrorMessage, definition, level, withdrawAmount, current)
            return
        }
        val newBalance = current - withdrawAmount
        artifactEntry.setBalance(player.uniqueId, newBalance)
        if (command.isNotBlank()) {
            Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                command.replace("{amount}", withdrawAmount.toPlainString())
            )
        }
        player.sendMini(level.withdrawMessage, definition, level, withdrawAmount, newBalance)
    }

    private fun calculateAmount(player: Player, current: BigDecimal): BigDecimal {
        amountPlaceholder.takeIf { it.isNotBlank() }?.let {
            val parsed = it.parsePlaceholders(player).toDoubleOrNull()
            if (parsed != null) return BigDecimal.valueOf(parsed)
        }
        return when (amountType) {
            AmountType.FIXED -> BigDecimal.valueOf(amount)
            AmountType.PERCENTAGE -> current.multiply(BigDecimal.valueOf(amount)).divide(BigDecimal.valueOf(100))
        }
    }

    private fun Player.sendMini(
        message: String,
        definition: NumericalStorageDefinitionEntry,
        level: BankLevel,
        amount: BigDecimal,
        newBalance: BigDecimal
    ) {
        sendMiniWithResolvers(
            message,
            parsed("limit", BigDecimal.valueOf(level.limit).toPlainString()),
            parsed("amount", amount.toPlainString()),
            parsed("new_balance", newBalance.toPlainString()),
            parsed("prefix", definition.prefix)
        )
    }
}
