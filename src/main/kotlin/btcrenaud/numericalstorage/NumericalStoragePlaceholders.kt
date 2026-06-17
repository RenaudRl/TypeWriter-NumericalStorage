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
        val artifact = definition.artifact.get()
        
        return when (type.lowercase()) {
            "balance" -> artifact?.getBalance(player.uniqueId)?.toPlainString()
            "level" -> artifact?.getLevel(player.uniqueId)?.toString()
            "capacity" -> {
                val level = artifact?.getLevel(player.uniqueId) ?: 1
                val bankLevel = definition.levels.getOrNull(level - 1)
                bankLevel?.limit?.let { java.math.BigDecimal.valueOf(it).toPlainString() } ?: "∞"
            }
            "interest" -> {
                val level = artifact?.getLevel(player.uniqueId) ?: 1
                val bankLevel = definition.levels.getOrNull(level - 1)
                NumericalStorageInterestService.getApplicableInterestRate(player, definition, bankLevel).toString()
            }
            "interest_cooldown" -> {
                if (!definition.interestEnabled) {
                    "Disabled"
                } else {
                    val cron = definition.interestCron
                    if (cron.expression.isNotBlank()) {
                        try {
                            val nextTime = cron.nextTimeAfter(ZonedDateTime.now())
                            val duration = Duration.between(ZonedDateTime.now(), nextTime)
                            duration.asReadable()
                        } catch (e: Exception) {
                            "Error"
                        }
                    } else {
                        "Disabled"
                    }
                }
            }
            "name" -> definition.displayName.parsePlaceholders(player)
            "prefix" -> definition.prefix.parsePlaceholders(player)
            else -> null
        }
    }


    private fun java.time.Duration.asReadable(): String {
        val days = toDays()
        val hours = toHoursPart()
        val minutes = toMinutesPart()
        val seconds = toSecondsPart()
        val result = buildString {
            if (days > 0) append("${days}j ")
            if (hours > 0) append("${hours}h ")
            if (minutes > 0) append("${minutes}m ")
            if (seconds > 0 || isEmpty()) append("${seconds}s")
        }.trim()
        return result.ifEmpty { "0s" }
    }
}
