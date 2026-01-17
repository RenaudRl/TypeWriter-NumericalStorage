package btc.renaud.numericalstorage

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.engine.paper.entry.AssetManager
import com.typewritermc.engine.paper.entry.entries.ArtifactEntry
import com.typewritermc.core.extension.annotations.Tags
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.koin.java.KoinJavaComponent.get
import java.math.BigDecimal
import java.util.UUID
import kotlinx.coroutines.runBlocking
import btc.renaud.profiles.api.ProfilesAPI

@Entry(
    "player_numericalstorage_artifact",
    "Stores the numericalstorage amount per player",
    Colors.BLUE,
    "fa6-solid:box-archive",
)
@Tags("numericalstorage", "artifact")
class PlayerNumericalStorageArtifactEntry(
    override val id: String = "",
    override val name: String = "",
    @Help("Unique artifact identifier.")
    override val artifactId: String = "player_numericalstorage",
) : ArtifactEntry {

    private fun getStorageKey(uuid: UUID): String {
        return try {
            if (ProfilesAPI.isPerProfileExtension("NumericalStorage")) {
                ProfilesAPI.getProfileStorageKeyByUuid(uuid, uuid.toString())
            } else {
                uuid.toString()
            }
        } catch (t: Throwable) {
            uuid.toString()
        }
    }

    fun getLevels(): Map<String, Int> {
        val (_, levels, _) = load()
        return levels
    }

    fun getLastInterestTime(uuid: UUID): Long {
        val (_, _, interestTimes) = load()
        return interestTimes[getStorageKey(uuid)] ?: 0L
    }

    fun setLastInterestTime(uuid: UUID, time: Long) {
        val (balances, levels, interestTimes) = load()
        interestTimes[getStorageKey(uuid)] = time
        save(balances, levels, interestTimes)
    }

    private fun load(): Triple<MutableMap<String, BigDecimal>, MutableMap<String, Int>, MutableMap<String, Long>> {
        val assetManager = get<AssetManager>(AssetManager::class.java)
        val content = runBlocking { assetManager.fetchStringAsset(this@PlayerNumericalStorageArtifactEntry) }
        if (content.isNullOrBlank()) return Triple(mutableMapOf(), mutableMapOf(), mutableMapOf())
        return runCatching {
            val obj = JsonParser.parseString(content).asJsonObject

            val balancesMap: MutableMap<String, BigDecimal> = mutableMapOf()
            obj.getAsJsonObject("balances")?.entrySet()?.forEach { (id, amt) ->
                val value = runCatching { amt.asString.toBigDecimal() }.getOrNull() ?: return@forEach
                balancesMap[id] = value
            }

            val levelsMap: MutableMap<String, Int> = mutableMapOf()
            obj.getAsJsonObject("levels")?.entrySet()?.forEach { (id, lvl) ->
                val value = runCatching { lvl.asInt }.getOrNull() ?: return@forEach
                levelsMap[id] = value
            }

            val interestMap: MutableMap<String, Long> = mutableMapOf()
            obj.getAsJsonObject("interest_times")?.entrySet()?.forEach { (id, time) ->
                val value = runCatching { time.asLong }.getOrNull() ?: return@forEach
                interestMap[id] = value
            }

            Triple(balancesMap, levelsMap, interestMap)
        }.getOrDefault(Triple(mutableMapOf(), mutableMapOf(), mutableMapOf()))
    }

    private fun save(balances: Map<String, BigDecimal>, levels: Map<String, Int>, interestTimes: Map<String, Long>) {
        val assetManager = get<AssetManager>(AssetManager::class.java)

        val balanceJson = JsonObject().apply {
            balances.forEach { (id, value) ->
                addProperty(id, value.toPlainString())
            }
        }

        val levelJson = JsonObject().apply {
            levels.forEach { (id, value) ->
                addProperty(id, value)
            }
        }

        val interestJson = JsonObject().apply {
            interestTimes.forEach { (id, value) ->
                addProperty(id, value)
            }
        }

        val root = JsonObject().apply {
            add("balances", balanceJson)
            add("levels", levelJson)
            add("interest_times", interestJson)
        }

        runBlocking {
            assetManager.storeStringAsset(
                this@PlayerNumericalStorageArtifactEntry,
                root.toString()
            )
        }
    }

    fun addBalance(uuid: UUID, amount: BigDecimal) {
        val current = getBalance(uuid)
        setBalance(uuid, current + amount)
    }

    fun removeBalance(uuid: UUID, amount: BigDecimal) {
        val current = getBalance(uuid)
        val newBalance = current - amount
        setBalance(uuid, if (newBalance < BigDecimal.ZERO) BigDecimal.ZERO else newBalance)
    }

    fun getBalance(uuid: UUID): BigDecimal {
        val (balances, _, _) = load()
        return balances[getStorageKey(uuid)] ?: BigDecimal.ZERO
    }

    fun getBalances(): Map<String, BigDecimal> {
        val (balances, _, _) = load()
        return balances
    }

    fun setBalance(uuid: UUID, amount: BigDecimal) {
        val (balances, levels, interestTimes) = load()
        balances[getStorageKey(uuid)] = amount
        save(balances, levels, interestTimes)
    }

    fun getLevel(uuid: UUID): Int {
        val (_, levels, _) = load()
        return levels[getStorageKey(uuid)] ?: 1
    }

    fun setLevel(uuid: UUID, level: Int) {
        val (balances, levels, interestTimes) = load()
        levels[getStorageKey(uuid)] = level.coerceAtLeast(1)
        save(balances, levels, interestTimes)
    }

    fun resetLevel(uuid: UUID) {
        setLevel(uuid, 1)
    }
}
