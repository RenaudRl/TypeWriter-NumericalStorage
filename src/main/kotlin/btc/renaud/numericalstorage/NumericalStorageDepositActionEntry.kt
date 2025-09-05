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
import com.typewritermc.engine.paper.entry.triggerFor
import com.typewritermc.core.interaction.context
import com.typewritermc.engine.paper.utils.sendMiniWithResolvers
import com.typewritermc.engine.paper.extensions.placeholderapi.parsePlaceholders
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.math.BigDecimal

@Entry(
    "numericalstorage_deposit",
    "Deposit amount into the player's numericalstorage",
    Colors.RED,
    "fa6-solid:piggy-bank"
)
@Tags("numericalstorage", "action")
class NumericalStorageDepositActionEntry(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("Storage definition to deposit into.")
    val storage: Ref<NumericalStorageDefinitionEntry> = emptyRef(),
    @Help("Amount to deposit/Add Xp.")
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

        if (definition.classicLeveling) {
            var levelNumber = artifactEntry.getLevel(player.uniqueId)
            var level = definition.levels.getOrNull(levelNumber - 1) ?: return
            val current = artifactEntry.getBalance(player.uniqueId)
            val depositAmount = calculateAmount(player, current)
            var remaining = current + depositAmount
            while (levelNumber < definition.levels.size && remaining >= BigDecimal.valueOf(level.limit)) {
                remaining -= BigDecimal.valueOf(level.limit)
                levelNumber++
                val nextLevel = definition.levels.getOrNull(levelNumber - 1) ?: break
                player.sendLevelUp(definition, nextLevel, levelNumber)
                level = nextLevel
            }
            artifactEntry.setBalance(player.uniqueId, remaining)
            artifactEntry.setLevel(player.uniqueId, levelNumber)
            if (command.isNotBlank()) {
                Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    command.replace("{amount}", depositAmount.toPlainString())
                )
            }
            player.sendMini(level.depositMessage, definition, level, depositAmount, remaining)
        } else {
            val levelNumber = artifactEntry.getLevel(player.uniqueId)
            val level = definition.levels.getOrNull(levelNumber - 1) ?: return
            val current = artifactEntry.getBalance(player.uniqueId)
            val depositAmount = calculateAmount(player, current)
            val newBalance = current + depositAmount
            if (newBalance > BigDecimal.valueOf(level.limit)) {
                player.sendMini(level.depositErrorMessage, definition, level, depositAmount, current)
                return
            }
            artifactEntry.setBalance(player.uniqueId, newBalance)
            if (command.isNotBlank()) {
                Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    command.replace("{amount}", depositAmount.toPlainString())
                )
            }
            player.sendMini(level.depositMessage, definition, level, depositAmount, newBalance)
        }
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

    private fun Player.sendLevelUp(
        definition: NumericalStorageDefinitionEntry,
        level: BankLevel,
        levelNumber: Int
    ) {
        sendMiniWithResolvers(
            level.levelUpMessage,
            parsed("limit", BigDecimal.valueOf(level.limit).toPlainString()),
            parsed("prefix", definition.prefix),
            parsed("level", levelNumber.toString())
        )
        level.levelUpTrigger.triggerFor(this, context())
    }
}
