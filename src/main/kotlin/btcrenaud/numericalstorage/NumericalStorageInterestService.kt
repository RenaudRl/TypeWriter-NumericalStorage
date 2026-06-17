package btcrenaud.numericalstorage

import com.typewritermc.core.entries.Query
import com.typewritermc.core.extension.Initializable
import com.typewritermc.core.extension.annotations.Singleton
import com.typewritermc.engine.paper.utils.sendMiniWithResolvers
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
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
        Bukkit.getPluginManager().registerEvents(
            this,
            Bukkit.getPluginManager().getPlugin("Typewriter") ?: return
        )
    }

    override suspend fun shutdown() = Unit

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        val definitions = Query.find(NumericalStorageDefinitionEntry::class)

        definitions.forEach { def ->
            if (!def.interestEnabled) return@forEach

            val artifact = def.artifact.get() ?: return@forEach
            val uuid = player.uniqueId

            artifact.update { balances, _, interestTimes ->
                val key = resolveStorageKey(uuid, def)
                val currentTime = System.currentTimeMillis()

                var lastInterestTime = interestTimes[key] ?: 0L

                if (lastInterestTime == 0L) {
                    interestTimes[key] = currentTime
                    return@update
                }

                val cron = def.interestCron
                if (cron.expression.isBlank()) return@update

                try {
                    var nextTime = cron.nextTimeAfter(
                        ZonedDateTime.ofInstant(Instant.ofEpochMilli(lastInterestTime), ZoneId.systemDefault())
                    )

                    var totalInterest = BigDecimal.ZERO
                    var currentBalance = balances[key] ?: BigDecimal.ZERO
                    var iterations = 0

                    // Read level once outside the loop
                    val playerLevel = try {
                        // Read from the levels map directly to avoid nested load()
                        artifact.getLevel(uuid)
                    } catch (_: Exception) { 1 }
                    val bankLevel = def.levels.getOrNull(playerLevel - 1)
                    val capacityLimit = bankLevel?.limit?.let { BigDecimal.valueOf(it) }

                    while (nextTime.toInstant().toEpochMilli() <= currentTime && iterations < 100) {
                        iterations++
                        if (currentBalance > BigDecimal.ZERO) {
                            if (capacityLimit != null && currentBalance >= capacityLimit) {
                                lastInterestTime = nextTime.toInstant().toEpochMilli()
                                nextTime = cron.nextTimeAfter(nextTime)
                                continue
                            }

                            val applicableRate = getApplicableInterestRate(player, def, bankLevel)
                            val rate = BigDecimal.valueOf(applicableRate / 100.0)
                            var interest = currentBalance.multiply(rate).setScale(2, RoundingMode.HALF_UP)

                            val cycleCap = bankLevel?.interestCap ?: 0.0
                            if (cycleCap > 0.0) {
                                val capBD = BigDecimal.valueOf(cycleCap).setScale(2, RoundingMode.HALF_UP)
                                if (interest > capBD) {
                                    interest = capBD
                                }
                            }

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
                        balances[key] = currentBalance
                        val finalRate = getApplicableInterestRate(player, def, bankLevel)
                        player.sendMiniWithResolvers(
                            def.interestMessage,
                            parsed("amount", totalInterest.toPlainString()),
                            parsed("new_balance", currentBalance.toPlainString()),
                            parsed("rate", finalRate.toString()),
                            parsed("prefix", def.prefix)
                        )
                    }
                    interestTimes[key] = lastInterestTime
                } catch (e: Exception) {
                    // Silent fail for invalid cron expressions
                }
            }
        }
    }

    private fun resolveStorageKey(uuid: java.util.UUID, def: NumericalStorageDefinitionEntry): String {
        return try {
            if (def.profileMode && isProfileApiEnabled()) {
                getProfileKeyByUuid(uuid, uuid.toString())
            } else {
                uuid.toString()
            }
        } catch (_: Throwable) {
            uuid.toString()
        }
    }

    private fun isProfileApiEnabled(): Boolean {
        return try {
            val apiClass = Class.forName("btc.renaud.profiles.api.ProfilesAPI")
            val method = apiClass.getMethod("isEnabled")
            method.invoke(null) as? Boolean ?: false
        } catch (_: Throwable) { false }
    }

    private fun getProfileKeyByUuid(uuid: java.util.UUID, fallback: String): String {
        return try {
            val apiClass = Class.forName("btc.renaud.profiles.api.ProfilesAPI")
            val method = apiClass.getMethod("getProfileStorageKeyByUuid", java.util.UUID::class.java, String::class.java)
            method.invoke(null, uuid, fallback) as? String ?: fallback
        } catch (_: Throwable) { fallback }
    }

    companion object {
        fun getApplicableInterestRate(
            player: org.bukkit.entity.Player,
            def: NumericalStorageDefinitionEntry,
            bankLevel: BankLevel? = null
        ): Double {
            val permissionRate = def.interestRates.firstOrNull { player.hasPermission(it.permission) }?.rate
            if (permissionRate != null) return permissionRate
            return bankLevel?.interestRate ?: def.interestRate
        }
    }
}
