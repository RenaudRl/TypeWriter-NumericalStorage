package btc.renaud.numericalstorage

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
        val parts = params.substringAfter("ns_").split("_", limit = 2)
        val type = parts.firstOrNull() ?: return null
        val defId = parts.getOrNull(1) ?: return null
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
            "interest" -> NumericalStorageInterestService.getApplicableInterestRate(player, definition).toString()
            "interest_cooldown" -> {
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
        return buildString {
            if (days > 0) append("$days d ")
            if (hours > 0) append("$hours h ")
            if (minutes > 0) append("$minutes m ")
            if (seconds > 0) append("$seconds s")
        }.trim()
    }
}

