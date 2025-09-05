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

    private fun load(): Pair<MutableMap<UUID, BigDecimal>, MutableMap<UUID, Int>> {
        val assetManager = get<AssetManager>(AssetManager::class.java)
        val content = runBlocking { assetManager.fetchStringAsset(this@PlayerNumericalStorageArtifactEntry) }
        if (content.isNullOrBlank()) return mutableMapOf<UUID, BigDecimal>() to mutableMapOf()
        return runCatching {
            val obj = JsonParser.parseString(content).asJsonObject

            val balancesMap: MutableMap<UUID, BigDecimal> = mutableMapOf()
            obj.getAsJsonObject("balances")?.entrySet()?.forEach { (id, amt) ->
                val uuid = runCatching { UUID.fromString(id) }.getOrNull() ?: return@forEach
                val value = runCatching { amt.asString.toBigDecimal() }.getOrNull() ?: return@forEach
                balancesMap[uuid] = value
            }

            val levelsMap: MutableMap<UUID, Int> = mutableMapOf()
            obj.getAsJsonObject("levels")?.entrySet()?.forEach { (id, lvl) ->
                val uuid = runCatching { UUID.fromString(id) }.getOrNull() ?: return@forEach
                val value = runCatching { lvl.asInt }.getOrNull() ?: return@forEach
                levelsMap[uuid] = value
            }

            balancesMap to levelsMap
        }.getOrDefault(mutableMapOf<UUID, BigDecimal>() to mutableMapOf())
    }

    private fun save(balances: Map<UUID, BigDecimal>, levels: Map<UUID, Int>) {
        val assetManager = get<AssetManager>(AssetManager::class.java)

        val balanceJson = JsonObject().apply {
            balances.forEach { (id, value) ->
                addProperty(id.toString(), value.toPlainString())
            }
        }

        val levelJson = JsonObject().apply {
            levels.forEach { (id, value) ->
                addProperty(id.toString(), value)
            }
        }

        val root = JsonObject().apply {
            add("balances", balanceJson)
            add("levels", levelJson)
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
        val (balances, _) = load()
        return balances[uuid] ?: BigDecimal.ZERO
    }

    fun getBalances(): Map<UUID, BigDecimal> {
        val (balances, _) = load()
        return balances
    }

    fun setBalance(uuid: UUID, amount: BigDecimal) {
        val (balances, levels) = load()
        balances[uuid] = amount
        save(balances, levels)
    }

    fun getLevel(uuid: UUID): Int {
        val (_, levels) = load()
        return levels[uuid] ?: 1
    }

    fun setLevel(uuid: UUID, level: Int) {
        val (balances, levels) = load()
        levels[uuid] = level.coerceAtLeast(1)
        save(balances, levels)
    }

    fun resetLevel(uuid: UUID) {
        setLevel(uuid, 1)
    }

    fun getLevels(): Map<UUID, Int> {
        val (_, levels) = load()
        return levels
    }
}
