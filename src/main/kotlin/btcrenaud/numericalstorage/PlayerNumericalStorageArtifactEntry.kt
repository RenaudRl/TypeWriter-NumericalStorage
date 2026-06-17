package btcrenaud.numericalstorage

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Singleton
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
import java.util.concurrent.ConcurrentHashMap

@Singleton
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

    private class Cache {
        var cachedData: Triple<MutableMap<String, BigDecimal>, MutableMap<String, Int>, MutableMap<String, Long>>? = null
        var lastCacheTime: Long = 0
        val cacheDurationMs = 500 // Short cache for batch operations
    }

    companion object {
        private val CACHES = ConcurrentHashMap<String, Cache>()
    }

    private val dataCache: Cache
        get() = CACHES.computeIfAbsent(if (id.isEmpty()) artifactId else id) { Cache() }

    private fun getStorageKey(uuid: UUID): String {
        return try {
            val profileEnabled = try {
                com.typewritermc.core.entries.Query.find<NumericalStorageDefinitionEntry>()
                    .firstOrNull()?.profileMode == true
            } catch (_: Exception) { false }

            if (profileEnabled) {
                resolveProfileKeyByUuid(uuid, uuid.toString())
            } else {
                uuid.toString()
            }
        } catch (t: Throwable) {
            uuid.toString()
        }
    }

    /**
     * Resolve profile storage key via reflection (avoids KSP cascade from direct ProfilesAPI import).
     */
    private fun resolveProfileKeyByUuid(uuid: UUID, fallback: String): String {
        return try {
            val apiClass = Class.forName("btc.renaud.profiles.api.ProfilesAPI")
            val isEnabledMethod = apiClass.getMethod("isEnabled")
            val getKeyMethod = apiClass.getMethod("getProfileStorageKeyByUuid", UUID::class.java, String::class.java)
            val isEnabled = isEnabledMethod.invoke(null) as? Boolean ?: false
            if (isEnabled) {
                getKeyMethod.invoke(null, uuid, fallback) as? String ?: fallback
            } else {
                fallback
            }
        } catch (_: Throwable) {
            fallback
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
        update { _, _, interestTimes ->
            interestTimes[getStorageKey(uuid)] = time
        }
    }

    /**
     * Optimized batch update.
     * Loads the data once, applies the block, and saves once.
     */
    fun update(block: (MutableMap<String, BigDecimal>, MutableMap<String, Int>, MutableMap<String, Long>) -> Unit) {
        val (balances, levels, interestTimes) = load()
        block(balances, levels, interestTimes)
        save(balances, levels, interestTimes)
    }


    private fun load(): Triple<MutableMap<String, BigDecimal>, MutableMap<String, Int>, MutableMap<String, Long>> {
        val cache = dataCache
        val now = System.currentTimeMillis()
        if (cache.cachedData != null && now - cache.lastCacheTime < cache.cacheDurationMs) {
            return cache.cachedData!!
        }

        val assetManager = get<AssetManager>(AssetManager::class.java)
        
        // Check if asset exists prevents "Asset not found" error log
        val exists = runBlocking { 
            runCatching { assetManager.containsAsset(this@PlayerNumericalStorageArtifactEntry) }.getOrDefault(false) 
        }

        val content = if (exists) {
            runCatching {
                runBlocking { assetManager.fetchStringAsset(this@PlayerNumericalStorageArtifactEntry) }
            }.getOrNull()
        } else {
            null
        }
        
        val data = if (content.isNullOrBlank()) {
            Triple(mutableMapOf<String, BigDecimal>(), mutableMapOf<String, Int>(), mutableMapOf<String, Long>())
        } else {
            runCatching {
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
        
        cache.cachedData = data
        cache.lastCacheTime = now
        return data
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
        
        // Update cache time to prevent immediate reload of potentially stale data
        val cache = dataCache
        cache.cachedData = Triple(balances.toMutableMap(), levels.toMutableMap(), interestTimes.toMutableMap())
        cache.lastCacheTime = System.currentTimeMillis()
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
        update { balances, _, _ ->
            balances[getStorageKey(uuid)] = amount
        }
    }


    fun getLevel(uuid: UUID): Int {
        val (_, levels, _) = load()
        return levels[getStorageKey(uuid)] ?: 1
    }

    fun setLevel(uuid: UUID, level: Int) {
        update { _, levels, _ ->
            levels[getStorageKey(uuid)] = level.coerceAtLeast(1)
        }
    }


    fun resetLevel(uuid: UUID) {
        setLevel(uuid, 1)
    }
}
