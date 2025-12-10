package btc.renaud.numericalstorage

import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.engine.paper.entry.TriggerableEntry

data class BankLevel(
    @Help("Maximum balance/xp allowed.")
    val limit: Double = 0.0,
    @Help("Message when deposit/xp exceeds limit. Placeholders: <limit> <amount> <prefix>")
    val depositErrorMessage: String = "",
    @Help("Message when deposit/xp add succeeds. Placeholders: <limit> <amount> <new_balance> <prefix>")
    val depositMessage: String = "",
    @Help("Message when this level is reached. Placeholders: <limit> <prefix> <level>")
    val levelUpMessage: String = "",
    @Help("Criteria required to unlock this level (all must be met).")
    val criteria: List<LevelCriteria> = emptyList(),
    @Help("Triggers to execute when this level is reached.")
    val levelUpTriggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("Message when withdraw/remove xp succeeds. Placeholders: <limit> <amount> <new_balance> <prefix>")
    val withdrawMessage: String = "",
    @Help("Message when withdraw/remove xp fails. Placeholders: <limit> <amount> <prefix>")
    val withdrawErrorMessage: String = ""
)

