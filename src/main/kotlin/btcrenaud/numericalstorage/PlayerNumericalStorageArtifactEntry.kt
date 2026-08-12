package btcrenaud.numericalstorage

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Singleton
import com.typewritermc.core.extension.annotations.Tags
import com.typewritermc.engine.paper.entry.AssetStorage
import com.typewritermc.engine.paper.entry.entries.ArtifactEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.get
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Immutable view of one NumericalStorage artifact. */
data class NumericalStorageSnapshot(
    val balances: Map<String, BigDecimal> = emptyMap(),
    val levels: Map<String, Int> = emptyMap(),
    val interestTimes: Map<String, Long> = emptyMap(),
) {
    fun balance(key: String): BigDecimal = balances[key] ?: BigDecimal.ZERO
    fun level(key: String): Int = levels[key] ?: 1
    fun lastInterestTime(key: String): Long = interestTimes[key] ?: 0L
}

/**
 * Persistent player storage artifact.
 *
 * Reads and writes are deliberately suspend-only at the persistence boundary. Synchronous
 * getters are cache-only and therefore safe for placeholder/render paths; they never perform
 * file, database or network I/O. Mutations are serialized per artifact and persist one complete,
 * immutable snapshot, avoiding the old read-modify-write races.
 */
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
    @Help("Technical stable artifact identifier. Leave empty so Typewriter can generate it.")
    override val artifactId: String = "",
) : ArtifactEntry {

    /** Keep old configured semantic IDs readable while all new writes use a technical UUID path. */
    private val canonicalArtifactId: String
        get() = artifactId.takeIf { it.isUuid() } ?: CANONICAL_ARTIFACT_ID

    override val path: String
        get() = "artifacts/$canonicalArtifactId.json"

    private val legacyPath: String?
        get() = artifactId.takeIf { it.isNotBlank() && !it.isUuid() }
            ?.let { "artifacts/$it.json" }

    // Lock by physical path, not page id: multiple definitions can legitimately reference the
    // same artifact and must still share one in-process transaction lock.
    private val cacheKey: String
        get() = path

    private val mutex: Mutex
        get() = LOCKS.computeIfAbsent(cacheKey) { Mutex() }

    private data class Cache(var snapshot: NumericalStorageSnapshot? = null, var loadedAt: Long = 0L)

    private val cache: Cache
        get() = CACHES.computeIfAbsent(cacheKey) { Cache() }

    /** Preloads the artifact asynchronously. Safe to call at join/startup. */
    suspend fun preload(): NumericalStorageSnapshot = mutex.withLock { loadLocked() }

    suspend fun snapshot(): NumericalStorageSnapshot = mutex.withLock { loadLocked() }

    /** Applies one complete mutation atomically and persists it once. */
    suspend fun update(
        block: (balances: MutableMap<String, BigDecimal>, levels: MutableMap<String, Int>, interestTimes: MutableMap<String, Long>) -> Unit,
    ): NumericalStorageSnapshot = mutex.withLock {
        val current = loadLocked()
        val balances = current.balances.toMutableMap()
        val levels = current.levels.toMutableMap()
        val interestTimes = current.interestTimes.toMutableMap()
        block(balances, levels, interestTimes)
        persistLocked(NumericalStorageSnapshot(balances.toMap(), levels.toMap(), interestTimes.toMap()))
    }

    fun storageKey(uuid: UUID, profileMode: Boolean = false): String =
        if (profileMode) resolveProfileKey(uuid) else uuid.toString()

    /* Cache-only API used by placeholder and GUI render paths. */
    fun getBalance(uuid: UUID, profileMode: Boolean = false): BigDecimal =
        cache.snapshot?.balance(storageKey(uuid, profileMode)) ?: BigDecimal.ZERO

    fun getBalances(): Map<String, BigDecimal> = cache.snapshot?.balances ?: emptyMap()

    fun getLevel(uuid: UUID, profileMode: Boolean = false): Int =
        cache.snapshot?.level(storageKey(uuid, profileMode)) ?: 1

    fun getLevels(): Map<String, Int> = cache.snapshot?.levels ?: emptyMap()

    fun getLastInterestTime(uuid: UUID, profileMode: Boolean = false): Long =
        cache.snapshot?.lastInterestTime(storageKey(uuid, profileMode)) ?: 0L

    /* Async API used by commands, transactions and lifecycle work. */
    suspend fun getBalanceAsync(uuid: UUID, profileMode: Boolean = false): BigDecimal =
        snapshot().balance(storageKey(uuid, profileMode))

    suspend fun getLevelAsync(uuid: UUID, profileMode: Boolean = false): Int =
        snapshot().level(storageKey(uuid, profileMode))

    suspend fun setBalance(uuid: UUID, amount: BigDecimal, profileMode: Boolean = false): NumericalStorageSnapshot =
        update { balances, _, _ -> balances[storageKey(uuid, profileMode)] = amount.nonNegative() }

    suspend fun addBalance(uuid: UUID, amount: BigDecimal, profileMode: Boolean = false): NumericalStorageSnapshot =
        update { balances, _, _ ->
            require(amount >= BigDecimal.ZERO) { "Amount must be non-negative" }
            val key = storageKey(uuid, profileMode)
            balances[key] = (balances[key] ?: BigDecimal.ZERO).add(amount)
        }

    suspend fun removeBalance(uuid: UUID, amount: BigDecimal, profileMode: Boolean = false): NumericalStorageSnapshot =
        update { balances, _, _ ->
            require(amount >= BigDecimal.ZERO) { "Amount must be non-negative" }
            val key = storageKey(uuid, profileMode)
            balances[key] = ((balances[key] ?: BigDecimal.ZERO) - amount).max(BigDecimal.ZERO)
        }

    suspend fun setLevel(uuid: UUID, level: Int, profileMode: Boolean = false): NumericalStorageSnapshot =
        update { _, levels, _ -> levels[storageKey(uuid, profileMode)] = level.coerceAtLeast(1) }

    suspend fun resetLevel(uuid: UUID, profileMode: Boolean = false): NumericalStorageSnapshot =
        setLevel(uuid, 1, profileMode)

    suspend fun setLastInterestTime(uuid: UUID, time: Long, profileMode: Boolean = false): NumericalStorageSnapshot =
        update { _, _, interestTimes -> interestTimes[storageKey(uuid, profileMode)] = time.coerceAtLeast(0L) }

    /**
     * Transfers funds while holding both artifact locks in deterministic order. This is the
     * transaction primitive used by the public transfer action; two independent writes are not
     * sufficient because a crash between them can create or destroy funds.
     */
    suspend fun transferTo(
        target: PlayerNumericalStorageArtifactEntry,
        uuid: UUID,
        amount: BigDecimal,
        sourceProfileMode: Boolean = false,
        targetProfileMode: Boolean = false,
    ): Result<Pair<BigDecimal, BigDecimal>> {
        if (amount <= BigDecimal.ZERO) return Result.failure(IllegalArgumentException("Amount must be positive"))
        if (this === target || cacheKey == target.cacheKey) return Result.failure(IllegalArgumentException("Source and target must differ"))

        val first = if (cacheKey < target.cacheKey) this else target
        val second = if (first === this) target else this
        return first.mutex.withLock {
            second.mutex.withLock {
                val source = loadLocked()
                val destination = target.loadLocked()
                val sourceKey = storageKey(uuid, sourceProfileMode)
                val targetKey = target.storageKey(uuid, targetProfileMode)
                val sourceBalance = source.balance(sourceKey)
                if (sourceBalance < amount) return@withLock Result.failure(IllegalStateException("Insufficient balance"))

                val sourceBalances = source.balances.toMutableMap()
                val targetBalances = destination.balances.toMutableMap()
                sourceBalances[sourceKey] = sourceBalance - amount
                targetBalances[targetKey] = destination.balance(targetKey) + amount
                val newSource = source.copy(balances = sourceBalances.toMap())
                val newTarget = destination.copy(balances = targetBalances.toMap())

                // Persist both while both locks are held. If the second write fails, compensate
                // both assets before returning so a transient storage failure does not strand
                // funds in one side of the transfer.
                try {
                    persistLocked(newSource)
                    target.persistLocked(newTarget)
                    Result.success(newSource.balance(sourceKey) to newTarget.balance(targetKey))
                } catch (error: Throwable) {
                    runCatching { persistLocked(source) }
                    runCatching { target.persistLocked(destination) }
                    Result.failure(error)
                }
            }
        }
    }

    private suspend fun loadLocked(): NumericalStorageSnapshot {
        val now = System.currentTimeMillis()
        cache.snapshot?.takeIf { now - cache.loadedAt < CACHE_TTL_MS }?.let { return it }
        val storage = get<AssetStorage>(AssetStorage::class.java)
        val content = withContext(Dispatchers.IO) {
            val canonical = storage.fetchStringAsset(path).getOrNull()
            if (canonical != null) return@withContext canonical

            val legacy = legacyPath ?: return@withContext null
            val legacyContent = storage.fetchStringAsset(legacy).getOrNull() ?: return@withContext null

            // Idempotent migration: backup first, then canonical write, then remove the old path.
            val backupPath = "backups/numericalstorage/$canonicalArtifactId-${legacy.substringAfterLast('/')}.json"
            runCatching { storage.storeStringAsset(backupPath, legacyContent) }
            storage.storeStringAsset(path, legacyContent)
            runCatching { storage.deleteAsset(legacy) }
            legacyContent
        }
        val parsed = parse(content)
        cache.snapshot = parsed
        cache.loadedAt = now
        return parsed
    }

    private suspend fun persistLocked(snapshot: NumericalStorageSnapshot): NumericalStorageSnapshot {
        val root = JsonObject().apply {
            add("balances", JsonObject().apply { snapshot.balances.forEach { (key, value) -> addProperty(key, value.toPlainString()) } })
            add("levels", JsonObject().apply { snapshot.levels.forEach { (key, value) -> addProperty(key, value) } })
            add("interest_times", JsonObject().apply { snapshot.interestTimes.forEach { (key, value) -> addProperty(key, value) } })
            addProperty("schema_version", 2)
        }
        val storage = get<AssetStorage>(AssetStorage::class.java)
        withContext(Dispatchers.IO) { storage.storeStringAsset(path, root.toString()) }
        cache.snapshot = snapshot
        cache.loadedAt = System.currentTimeMillis()
        return snapshot
    }

    private fun parse(content: String?): NumericalStorageSnapshot {
        if (content.isNullOrBlank()) return NumericalStorageSnapshot()
        return runCatching {
            val root = JsonParser.parseString(content).asJsonObject
            val balances = root.getAsJsonObject("balances")?.entrySet()?.mapNotNull { (key, value) ->
                runCatching { key to value.asString.toBigDecimal() }
                    .getOrNull()
                    ?.takeIf { it.second >= BigDecimal.ZERO }
            }?.toMap() ?: emptyMap()
            val levels = root.getAsJsonObject("levels")?.entrySet()?.mapNotNull { (key, value) ->
                runCatching { key to value.asInt }
                    .getOrNull()
                    ?.takeIf { it.second > 0 }
            }?.toMap() ?: emptyMap()
            val interestTimes = root.getAsJsonObject("interest_times")?.entrySet()?.mapNotNull { (key, value) ->
                runCatching { key to value.asLong }
                    .getOrNull()
                    ?.takeIf { it.second >= 0L }
            }?.toMap() ?: emptyMap()
            NumericalStorageSnapshot(balances, levels, interestTimes)
        }.getOrDefault(NumericalStorageSnapshot())
    }

    private fun resolveProfileKey(uuid: UUID): String = runCatching {
        val api = Class.forName("btc.renaud.profiles.api.ProfilesAPI")
        val enabled = api.getMethod("isEnabled").invoke(null) as? Boolean ?: false
        if (!enabled) return@runCatching uuid.toString()
        api.getMethod("getProfileStorageKeyByUuid", UUID::class.java, String::class.java)
            .invoke(null, uuid, uuid.toString()) as? String ?: uuid.toString()
    }.getOrDefault(uuid.toString())

    private fun String.isUuid(): Boolean = runCatching { UUID.fromString(this); true }.getOrDefault(false)
    private fun BigDecimal.nonNegative(): BigDecimal = max(BigDecimal.ZERO)

    companion object {
        // Generated technical UUID, shared by custom/public so a profile can be switched safely.
        private const val CANONICAL_ARTIFACT_ID = "2a2b3d2d-16b7-4f29-8ae6-8f6f2d8b5c4e"
        private const val CACHE_TTL_MS = 1_000L
        private val CACHES = ConcurrentHashMap<String, Cache>()
        private val LOCKS = ConcurrentHashMap<String, Mutex>()
    }
}
