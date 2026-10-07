package btcrenaud.numericalstorage

import com.typewritermc.core.extension.annotations.Singleton
import com.typewritermc.core.entries.Query
import com.typewritermc.engine.paper.extensions.placeholderapi.PlaceholderHandler
import com.typewritermc.engine.paper.extensions.placeholderapi.parsePlaceholders
import org.bukkit.entity.Player
import java.time.Duration
import java.time.ZonedDateTime

@Singleton
class NumericalStoragePlaceholders : PlaceholderHandler {
    override fun onPlaceholderRequest(player: Player?, params: String): String? {
        player ?: return null
        if (!params.startsWith("ns_")) return null
        
        val content = params.substringAfter("ns_")
        
        // Split type from defId. The type is either "interest_cooldown" (multi-word)
        // or a single word like "balance", "level", "capacity", "interest", "name", "prefix".
        // defId may contain underscores (e.g. "ns_def_001"), so we must NOT use lastIndexOf.
        val type: String
        val defId: String
        if (content.startsWith("interest_cooldown_")) {
            type = "interest_cooldown"
            defId = content.removePrefix("interest_cooldown_")
        } else {
            val firstUnderscore = content.indexOf('_')
            if (firstUnderscore == -1) return null
            type = content.substring(0, firstUnderscore)
            defId = content.substring(firstUnderscore + 1)
        }
        
        if (type.isBlank() || defId.isBlank()) return null
        
        val definition = Query.findById<NumericalStorageDefinitionEntry>(defId) ?: return null
        // Reads the storage the definition itself uses: in profile mode, the profile the player is playing.
        val reads = definition.reads()
        // Without an artifact, a player counts as being at the first configured level.
        fun bankLevel() = reads?.bankLevel(player.uniqueId) ?: definition.levels.firstOrNull()

        return when (type.lowercase()) {
            "balance" -> reads?.balance(player.uniqueId)?.toPlainString()
            "level" -> reads?.level(player.uniqueId)?.toString()
            "capacity" -> bankLevel()?.limit?.let { java.math.BigDecimal.valueOf(it).toPlainString() } ?: unlimitedCapacityText
            "interest" -> NumericalStorageInterestService.getApplicableInterestRate(player, definition, bankLevel()).toString()
            "interest_cooldown" -> {
                if (!definition.interestEnabled) {
                    interestDisabledText
                } else {
                    val cron = definition.interestCron
                    if (cron.expression.isNotBlank()) {
                        try {
                            val nextTime = cron.nextTimeAfter(ZonedDateTime.now())
                            val duration = Duration.between(ZonedDateTime.now(), nextTime)
                            duration.asReadable(durationFormat())
                        } catch (e: Exception) {
                            interestErrorText
                        }
                    } else {
                        interestDisabledText
                    }
                }
            }
            "name" -> definition.displayName.parsePlaceholders(player)
            "prefix" -> definition.prefix.parsePlaceholders(player)
            else -> null
        }
    }
}
