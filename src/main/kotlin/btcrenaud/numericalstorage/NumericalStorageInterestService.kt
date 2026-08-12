package btcrenaud.numericalstorage

import com.typewritermc.core.entries.Query
import com.typewritermc.core.extension.Initializable
import com.typewritermc.core.extension.annotations.Singleton
import com.typewritermc.engine.paper.utils.sendMiniWithResolvers
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@Singleton
class NumericalStorageInterestService : Initializable, Listener {
    override suspend fun initialize() {
        val plugin = Bukkit.getPluginManager().getPlugin("Typewriter") ?: return
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    override suspend fun shutdown() {
        HandlerList.unregisterAll(this)
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        NumericalStorageCoroutines.launch {
            Query.find(NumericalStorageDefinitionEntry::class).forEach { def ->
                if (!def.interestEnabled) return@forEach
                val artifact = def.artifact.get() ?: return@forEach
                runCatching {
                    val uuid = player.uniqueId
                    artifact.preload()
                    val key = artifact.storageKey(uuid, def.profileMode)
                    val playerLevel = artifact.getLevelAsync(uuid, def.profileMode)
                    val bankLevel = def.levels.getOrNull(playerLevel - 1)
                    val applicableRate = NumericalStorageCoroutines.onPlayerThread(player) {
                        getApplicableInterestRate(player, def, bankLevel)
                    } ?: def.interestRate
                    val now = System.currentTimeMillis()
                    var message: InterestMessage? = null

                    artifact.update { balances, _, interestTimes ->
                        var lastInterestTime = interestTimes[key] ?: 0L
                        if (lastInterestTime == 0L) {
                            interestTimes[key] = now
                            return@update
                        }
                        val cron = def.interestCron
                        if (cron.expression.isBlank()) return@update

                        var nextTime = cron.nextTimeAfter(
                            ZonedDateTime.ofInstant(Instant.ofEpochMilli(lastInterestTime), ZoneId.systemDefault())
                        )
                        var totalInterest = BigDecimal.ZERO
                        var currentBalance = balances[key] ?: BigDecimal.ZERO
                        var iterations = 0
                        val capacityLimit = bankLevel?.limit?.let { BigDecimal.valueOf(it) }

                        while (nextTime.toInstant().toEpochMilli() <= now && iterations < MAX_CATCH_UP_CYCLES) {
                            iterations++
                            if (currentBalance > BigDecimal.ZERO && (capacityLimit == null || currentBalance < capacityLimit)) {
                                var interest = currentBalance
                                    .multiply(BigDecimal.valueOf(applicableRate).movePointLeft(2))
                                    .setScale(2, RoundingMode.HALF_UP)
                                val cycleCap = bankLevel?.interestCap ?: 0.0
                                if (cycleCap > 0.0) interest = interest.min(BigDecimal.valueOf(cycleCap).setScale(2, RoundingMode.HALF_UP))
                                if (capacityLimit != null) interest = interest.min(capacityLimit - currentBalance)
                                if (interest > BigDecimal.ZERO) {
                                    currentBalance += interest
                                    totalInterest += interest
                                }
                            }
                            lastInterestTime = nextTime.toInstant().toEpochMilli()
                            nextTime = cron.nextTimeAfter(nextTime)
                        }
                        if (totalInterest > BigDecimal.ZERO) {
                            balances[key] = currentBalance
                            message = InterestMessage(totalInterest, currentBalance, applicableRate)
                        }
                        interestTimes[key] = lastInterestTime
                    }

                    message?.let { result ->
                        NumericalStorageCoroutines.onPlayerThread(player) {
                            player.sendMiniWithResolvers(
                                def.interestMessage,
                                parsed("amount", result.amount.toPlainString()),
                                parsed("new_balance", result.balance.toPlainString()),
                                parsed("rate", result.rate.toString()),
                                parsed("prefix", def.prefix),
                            )
                        }
                    }
                }.onFailure { throwable ->
                    Bukkit.getLogger().warning("NumericalStorage interest failed for '${def.id}': ${throwable.message}")
                }
            }
        }
    }

    private data class InterestMessage(val amount: BigDecimal, val balance: BigDecimal, val rate: Double)

    companion object {
        private const val MAX_CATCH_UP_CYCLES = 100

        fun getApplicableInterestRate(
            player: org.bukkit.entity.Player,
            def: NumericalStorageDefinitionEntry,
            bankLevel: BankLevel? = null,
        ): Double {
            val permissionRate = def.interestRates.firstOrNull { player.hasPermission(it.permission) }?.rate
            return permissionRate ?: bankLevel?.interestRate ?: def.interestRate
        }
    }
}
