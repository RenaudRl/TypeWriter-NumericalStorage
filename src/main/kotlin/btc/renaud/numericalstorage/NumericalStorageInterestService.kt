package btc.renaud.numericalstorage

import com.typewritermc.core.entries.Query
import com.typewritermc.core.extension.annotations.Singleton
import com.typewritermc.engine.paper.utils.sendMiniWithResolvers
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.Plugin
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@Singleton
class NumericalStorageInterestService(private val plugin: Plugin) : Listener {

    fun register() {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        val definitions = Query.find(NumericalStorageDefinitionEntry::class)

        definitions.forEach { def ->
            if (!def.interestEnabled) return@forEach
            
            val artifact = def.artifact.get() ?: return@forEach
            var lastInterestTime = artifact.getLastInterestTime(player.uniqueId)
            val currentTime = System.currentTimeMillis()

            // If never received interest, set current time as start
            if (lastInterestTime == 0L) {
                artifact.setLastInterestTime(player.uniqueId, currentTime)
                return@forEach
            }

            // Use CronExpression for interest calculation
            val cron = def.interestCron
            if (cron.expression.isBlank()) return@forEach
            
            try {
                var nextTime = cron.nextTimeAfter(ZonedDateTime.ofInstant(Instant.ofEpochMilli(lastInterestTime), ZoneId.systemDefault()))
                
                var totalInterest = BigDecimal.ZERO
                var currentBalance = artifact.getBalance(player.uniqueId)
                var iterations = 0
                
                // Get current level limit (capacity)
                val playerLevel = artifact.getLevel(player.uniqueId)
                val bankLevel = def.levels.getOrNull(playerLevel - 1)
                val capacityLimit = bankLevel?.limit?.let { BigDecimal.valueOf(it) }
                
                // Iterate through all missed intervals
                while (nextTime.toInstant().toEpochMilli() <= currentTime && iterations < 100) {
                    iterations++
                    // Only apply interest if balance is positive AND below capacity limit
                    if (currentBalance > BigDecimal.ZERO) {
                        // Skip if already at or over capacity
                        if (capacityLimit != null && currentBalance >= capacityLimit) {
                            lastInterestTime = nextTime.toInstant().toEpochMilli()
                            nextTime = cron.nextTimeAfter(nextTime)
                            continue
                        }
                        
                        val applicableRate = getApplicableInterestRate(player, def)
                        val rate = BigDecimal.valueOf(applicableRate / 100.0)
                        var interest = currentBalance.multiply(rate).setScale(2, RoundingMode.HALF_UP)
                        
                        // Cap interest so balance doesn't exceed capacity
                        if (capacityLimit != null) {
                            val remainingCapacity = capacityLimit.subtract(currentBalance)
                            if (interest > remainingCapacity) {
                                interest = remainingCapacity.setScale(2, RoundingMode.HALF_UP)
                            }
                        }
                        
                        if (interest > BigDecimal.ZERO) {
                            currentBalance = currentBalance.add(interest)
                            totalInterest = totalInterest.add(interest)
                        }
                    }
                    lastInterestTime = nextTime.toInstant().toEpochMilli()
                    nextTime = cron.nextTimeAfter(nextTime)
                }
                
                if (totalInterest > BigDecimal.ZERO) {
                    artifact.setBalance(player.uniqueId, currentBalance)
                    player.sendMiniWithResolvers(
                        def.interestMessage,
                        parsed("amount", totalInterest.toPlainString()),
                        parsed("new_balance", currentBalance.toPlainString()),
                        parsed("rate", def.interestRate.toString()),
                        parsed("prefix", def.prefix)
                    )
                }
                // Update time even if no interest was applied (e.g. 0 balance)
                artifact.setLastInterestTime(player.uniqueId, lastInterestTime)

            } catch (e: Exception) {
                plugin.logger.warning("Invalid cron expression for NumericalStorage ${def.id}: ${def.interestCron.expression}")
            }
        }
    }

    companion object {
        fun getApplicableInterestRate(player: org.bukkit.entity.Player, def: NumericalStorageDefinitionEntry): Double {
            return def.interestRates.firstOrNull { player.hasPermission(it.permission) }?.rate ?: def.interestRate
        }
    }
}
