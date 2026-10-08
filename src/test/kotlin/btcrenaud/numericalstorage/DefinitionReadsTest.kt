package btcrenaud.numericalstorage

import java.math.BigDecimal
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The placeholders and the upgrade button used to read the player's own key whatever the definition said, so a
 * definition in profile mode showed a balance and a level that no write ever touched.
 */
class DefinitionReadsTest {

    private val player: UUID = UUID.fromString("00000000-0000-0000-0000-000000000042")

    /** Holds one balance and one level per storage key kind, the way the artifact keys profile and global entries apart. */
    private class TwoKeyReader(
        private val globalBalance: String,
        private val profileBalance: String,
        private val globalLevel: Int,
        private val profileLevel: Int,
    ) : StorageReader {
        override fun balance(playerId: UUID, profileMode: Boolean): BigDecimal =
            BigDecimal(if (profileMode) profileBalance else globalBalance)

        override fun level(playerId: UUID, profileMode: Boolean): Int = if (profileMode) profileLevel else globalLevel
    }

    private val reader = TwoKeyReader(globalBalance = "10", profileBalance = "250", globalLevel = 1, profileLevel = 3)
    private val levels = listOf(BankLevel(limit = 100.0), BankLevel(limit = 500.0), BankLevel(limit = 2000.0))

    @Test
    fun `a definition in profile mode reads the balance and the level of the profile`() {
        val reads = DefinitionReads(reader, profileMode = true, levels = levels)

        assertEquals(BigDecimal("250"), reads.balance(player))
        assertEquals(3, reads.level(player))
    }

    @Test
    fun `a global definition reads the balance and the level of the player`() {
        val reads = DefinitionReads(reader, profileMode = false, levels = levels)

        assertEquals(BigDecimal("10"), reads.balance(player))
        assertEquals(1, reads.level(player))
    }

    @Test
    fun `the capacity level follows the level read with the definition profile mode`() {
        assertEquals(2000.0, DefinitionReads(reader, profileMode = true, levels = levels).bankLevel(player)?.limit)
        assertEquals(100.0, DefinitionReads(reader, profileMode = false, levels = levels).bankLevel(player)?.limit)
    }

    @Test
    fun `reading a balance or a level warms a cold storage up`() {
        var warmUps = 0
        val coldReader = object : StorageReader {
            override fun balance(playerId: UUID, profileMode: Boolean): BigDecimal = BigDecimal.ZERO
            override fun level(playerId: UUID, profileMode: Boolean): Int = 1
            override fun warmUp() { warmUps++ }
        }
        val reads = DefinitionReads(coldReader, profileMode = true, levels = levels)

        reads.balance(player)
        reads.level(player)

        assertEquals(2, warmUps)
    }

    @Test
    fun `a level beyond the configured ones has no bank level`() {
        val reads = DefinitionReads(reader, profileMode = true, levels = levels.take(2))

        assertNull(reads.bankLevel(player))
    }
}
