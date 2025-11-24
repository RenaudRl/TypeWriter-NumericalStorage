package btc.renaud.numericalstorage

import com.typewritermc.core.entries.Query
import com.typewritermc.core.extension.annotations.Singleton
import com.typewritermc.engine.paper.utils.msg
import com.typewritermc.engine.paper.utils.sendMiniWithResolvers
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.Plugin
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

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
            if (!def.interestEnabled || def.interestIntervalHours <= 0.0 || def.interestRate <= 0.0) return@forEach

            val artifact = def.artifact.get() ?: return@forEach
            val lastInterestTime = artifact.getLastInterestTime(player.uniqueId)
            val currentTime = System.currentTimeMillis()
            val intervalMillis = (def.interestIntervalHours * 3600 * 1000).toLong()

            // If never received interest, set current time as start
            if (lastInterestTime == 0L) {
                artifact.setLastInterestTime(player.uniqueId, currentTime)
                return@forEach
            }

            val timeDiff = currentTime - lastInterestTime
            if (timeDiff >= intervalMillis) {
                val intervalsPassed = timeDiff / intervalMillis
                if (intervalsPassed > 0) {
                    val balance = artifact.getBalance(player.uniqueId)
                    if (balance > BigDecimal.ZERO) {
                        // Determine applicable interest rate based on permissions
                        val applicableRate = getApplicableInterestRate(player, def)
                        val rate = BigDecimal.valueOf(applicableRate / 100.0)
                        // Compound interest: Balance * (1 + rate)^intervals
                        // Or simple interest for each interval? Usually compound.
                        // Let's do simple calculation loop for now to be safe or just one big jump.
                        // Ideally: NewBalance = Balance * (1 + rate)^intervals
                        
                        val multiplier = (BigDecimal.ONE + rate).pow(intervalsPassed.toInt())
                        val newBalance = balance.multiply(multiplier).setScale(2, RoundingMode.HALF_UP)
                        val interestAmount = newBalance.subtract(balance)

                        if (interestAmount > BigDecimal.ZERO) {
                            artifact.setBalance(player.uniqueId, newBalance)
                            artifact.setLastInterestTime(player.uniqueId, currentTime) // Reset to now, or add intervals?
                            // Better to add exact intervals to keep schedule? 
                            // lastInterestTime + (intervals * intervalMillis)
                            // But if they are offline for a month, they get interest for the month.
                            // Let's set to currentTime to avoid "banking" time if we change config.
                            // Actually, keeping schedule is better for "every 24h".
                            
                            val newLastTime = lastInterestTime + (intervalsPassed * intervalMillis)
                            artifact.setLastInterestTime(player.uniqueId, newLastTime)

                            player.sendMiniWithResolvers(
                                def.interestMessage,
                                parsed("amount", interestAmount.toPlainString()),
                                parsed("new_balance", newBalance.toPlainString()),
                                parsed("rate", def.interestRate.toString()),
                                parsed("prefix", def.prefix)
                            )
                        }
                    } else {
                         // Just update time if balance is zero
                         val newLastTime = lastInterestTime + (intervalsPassed * intervalMillis)
                         artifact.setLastInterestTime(player.uniqueId, newLastTime)
                    }
                }
            }
        }
    }

    companion object {
        fun getApplicableInterestRate(player: org.bukkit.entity.Player, def: NumericalStorageDefinitionEntry): Double {
            return def.interestRates.firstOrNull { player.hasPermission(it.permission) }?.rate ?: def.interestRate
        }
    }
}
