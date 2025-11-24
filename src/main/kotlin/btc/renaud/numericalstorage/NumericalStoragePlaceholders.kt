package btc.renaud.numericalstorage

import com.typewritermc.core.extension.annotations.Singleton
import com.typewritermc.core.entries.Query
import com.typewritermc.engine.paper.extensions.placeholderapi.PlaceholderHandler
import com.typewritermc.engine.paper.extensions.placeholderapi.parsePlaceholders
import org.bukkit.entity.Player

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
            "interest" -> NumericalStorageInterestService.getApplicableInterestRate(player, definition).toString()
            "name" -> definition.displayName.parsePlaceholders(player)
            "prefix" -> definition.prefix.parsePlaceholders(player)
            else -> null
        }
    }
}

