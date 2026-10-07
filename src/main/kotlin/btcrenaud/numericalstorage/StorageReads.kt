package btcrenaud.numericalstorage

import com.typewritermc.engine.paper.entry.entries.get
import java.math.BigDecimal
import java.util.UUID

/**
 * The cache reads of one player's storage, for placeholders and menu rendering. Every call states whether the
 * definition is in profile mode, so that the key it reads is the one the commands, the interest and the transactions
 * write.
 */
internal interface StorageReader {
    fun balance(playerId: UUID, profileMode: Boolean): BigDecimal
    fun level(playerId: UUID, profileMode: Boolean): Int
}

/**
 * What a definition shows of a player's storage. The profile mode is fixed when the reads are built from the
 * definition, so a read cannot target a different key than the writes: a call site has no flag to forget.
 */
internal class DefinitionReads(
    private val reader: StorageReader,
    private val profileMode: Boolean,
    private val levels: List<BankLevel>,
) {
    fun balance(playerId: UUID): BigDecimal = reader.balance(playerId, profileMode)

    fun level(playerId: UUID): Int = reader.level(playerId, profileMode)

    /** The configured level the player is at, or `null` when none is configured for it (no limit, no level rate). */
    fun bankLevel(playerId: UUID): BankLevel? = levels.getOrNull(level(playerId) - 1)
}

internal class ArtifactReader(private val artifact: PlayerNumericalStorageArtifactEntry) : StorageReader {
    override fun balance(playerId: UUID, profileMode: Boolean): BigDecimal = artifact.getBalance(playerId, profileMode)

    override fun level(playerId: UUID, profileMode: Boolean): Int = artifact.getLevel(playerId, profileMode)
}

/** The reads of this definition, or `null` when it has no artifact linked. */
internal fun NumericalStorageDefinitionEntry.reads(): DefinitionReads? =
    artifact.get()?.let { DefinitionReads(ArtifactReader(it), profileMode, levels) }
