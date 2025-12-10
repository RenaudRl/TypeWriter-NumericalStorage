package btc.renaud.numericalstorage

import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.engine.paper.entry.entries.ReadableFactEntry

/**
 * Mode for criteria evaluation.
 */
enum class CriteriaMode {
    @Help("Use a placeholder to check the criteria value")
    PLACEHOLDER,
    @Help("Use a Fact entry to check the criteria value")
    FACT
}

/**
 * Defines a criteria that must be met for leveling up.
 */
data class LevelCriteria(
    @Help("Mode of criteria: PLACEHOLDER or FACT")
    val mode: CriteriaMode = CriteriaMode.PLACEHOLDER,
    @Help("Placeholder that returns a numeric value (e.g. %player_level%, %vault_eco_balance%). Used when mode is PLACEHOLDER.")
    val placeholder: String = "",
    @Help("Fact entry to read the criteria value from. Used when mode is FACT.")
    val fact: Ref<ReadableFactEntry> = emptyRef(),
    @Help("Required value to meet the criteria.")
    val requiredValue: Double = 0.0,
    @Help("Comparison type for the criteria check.")
    val comparison: ComparisonType = ComparisonType.GREATER_THAN_OR_EQUALS,
    @Help("If true and mode is PLACEHOLDER, deduct the required value from the source when criteria is met (for monetary systems).")
    val deductOnMet: Boolean = false
) {
    /**
     * Check if the criteria is met given a player's current value.
     */
    fun isMet(currentValue: Double): Boolean {
        return when (comparison) {
            ComparisonType.EQUALS -> currentValue == requiredValue
            ComparisonType.NOT_EQUALS -> currentValue != requiredValue
            ComparisonType.GREATER_THAN -> currentValue > requiredValue
            ComparisonType.GREATER_THAN_OR_EQUALS -> currentValue >= requiredValue
            ComparisonType.LESS_THAN -> currentValue < requiredValue
            ComparisonType.LESS_THAN_OR_EQUALS -> currentValue <= requiredValue
        }
    }
}

enum class ComparisonType {
    EQUALS,
    NOT_EQUALS,
    GREATER_THAN,
    GREATER_THAN_OR_EQUALS,
    LESS_THAN,
    LESS_THAN_OR_EQUALS
}
