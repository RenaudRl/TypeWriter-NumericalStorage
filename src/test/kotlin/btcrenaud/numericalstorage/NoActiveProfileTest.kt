package btcrenaud.numericalstorage

import java.math.BigDecimal
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * While MMOProfiles has no profile for a player, the player's UUID (their Main profile) must not stand in for one:
 * reads show an empty storage, and a write has no key to land under.
 */
class NoActiveProfileTest {

    private val player: UUID = UUID.fromString("00000000-0000-0000-0000-000000000042")
    private val snapshot = NumericalStorageSnapshot(
        balances = mapOf("main" to BigDecimal("650")),
        levels = mapOf("main" to 2),
        interestTimes = mapOf("main" to 1_000L),
    )

    @Test
    fun `a missing key reads an empty storage`() {
        assertEquals(BigDecimal.ZERO, snapshot.balanceOf(null))
        assertEquals(1, snapshot.levelOf(null))
        assertEquals(0L, snapshot.lastInterestTimeOf(null))
    }

    @Test
    fun `a key reads what is stored under it`() {
        assertEquals(BigDecimal("650"), snapshot.balanceOf("main"))
        assertEquals(2, snapshot.levelOf("main"))
        assertEquals(1_000L, snapshot.lastInterestTimeOf("main"))
    }

    @Test
    fun `a profile without anything stored reads an empty storage`() {
        assertEquals(BigDecimal.ZERO, snapshot.balanceOf("second"))
        assertEquals(1, snapshot.levelOf("second"))
    }

    @Test
    fun `a refused write names the player`() {
        val refusal = NoActiveProfileException(player)

        assertIs<IllegalStateException>(refusal)
        assertEquals(player, refusal.playerId)
    }

    @Test
    fun `a definition that is not in profile mode keys by the player and never asks MMOProfiles`() {
        val key = storageKeyFor(player, profileMode = false) { error("MMOProfiles must not be asked") }

        assertEquals(player.toString(), key)
    }

    @Test
    fun `a definition in profile mode keys by the profile, and has no key without one`() {
        assertEquals("profile-a", storageKeyFor(player, profileMode = true) { "profile-a" })
        assertNull(storageKeyFor(player, profileMode = true) { null })
    }
}