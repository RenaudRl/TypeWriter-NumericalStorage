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
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.Plugin
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@Singleton
class NumericalStorageInterestService : Initializable, Listener {
    private var selectionListener: Listener? = null

    override suspend fun initialize() {
        val plugin = Bukkit.getPluginManager().getPlugin("Typewriter") ?: return
        Bukkit.getPluginManager().registerEvents(this, plugin)
        selectionListener = registerProfileSelection(plugin)
    }

    override suspend fun shutdown() {
        HandlerList.unregisterAll(this)
        selectionListener?.let(HandlerList::unregisterAll)
        selectionListener = null
    }

    /** Listens to MMOProfiles when it is there; its event class is only touched here, so its absence is not an error. */
    private fun registerProfileSelection(plugin: Plugin): Listener? {
        if (!Bukkit.getPluginManager().isPluginEnabled("MMOProfiles")) return null
        return try {
            ProfileSelectionListener(this).also { Bukkit.getPluginManager().registerEvents(it, plugin) }
        } catch (_: LinkageError) {
            null // No MMOProfiles API: every storage is keyed by the player, settled at join.
        }
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        NumericalStorageCoroutines.launch {
            // Placeholders only read the cache: load every artifact at join, interest or not, so none starts empty.
            Query.find(NumericalStorageDefinitionEntry::class).forEach { def ->
                runCatching { def.artifact.get()?.preload() }
            }
            Query.find(NumericalStorageDefinitionEntry::class).forEach { def ->
                // With MMOProfiles the storage is the one of the profile the player will select, which a join cannot name.
                if (def.profileMode && ProfileKeyResolver.managesProfiles()) return@forEach
                val key = def.artifact.get()?.storageKeyOrNull(player.uniqueId, def.profileMode) ?: return@forEach
                applyInterest(player, def, key)
            }
        }
    }

    /** Settles the interest of the profile [profileKey] that [player] has just selected, for the definitions keyed by profile. */
    fun onProfileSelected(player: Player, profileKey: String) {
        NumericalStorageCoroutines.launch {
            Query.find(NumericalStorageDefinitionEntry::class).forEach { def ->
                if (def.profileMode) applyInterest(player, def, profileKey)
            }
        }
    }

    private suspend fun applyInterest(player: Player, def: NumericalStorageDefinitionEntry, key: String) {
        if (!def.interestEnabled) return
        val artifact = def.artifact.get() ?: return
        runCatching {
            artifact.preload()
            val playerLevel = artifact.snapshot().level(key)
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
