package btcrenaud.numericalstorage

import fr.phoenixdevt.profiles.PlayerProfile
import fr.phoenixdevt.profiles.ProfileList
import fr.phoenixdevt.profiles.ProfileProvider
import java.lang.reflect.Proxy
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MmoProfilesKeysTest {

    private val player: UUID = UUID.fromString("00000000-0000-0000-0000-000000000042")
    private val profile: UUID = UUID.fromString("00000000-0000-0000-0000-0000000000a7")

    private fun keys(failures: MutableList<Exception> = mutableListOf(), detect: () -> ProfileIdSource?) =
        ProfileKeys(detect, failures::add)

    @Test
    fun `a player with a profile is keyed by the profile id`() {
        assertEquals(profile.toString(), keys { ProfileIdSource { profile } }.keyOf(player))
    }

    @Test
    fun `a player without a selected profile has no key, because the player UUID is the Main profile`() {
        assertNull(keys { ProfileIdSource { null } }.keyOf(player))
    }

    @Test
    fun `MMOProfiles manages the profiles once its service is registered, and not before`() {
        var registered: ProfileIdSource? = null
        val keys = keys { registered }

        assertFalse(keys.managesProfiles())
        registered = ProfileIdSource { null }

        assertTrue(keys.managesProfiles())
    }

    @Test
    fun `without the MMOProfiles API nothing manages profiles`() {
        assertFalse(keys { throw NoClassDefFoundError("fr/phoenixdevt/profiles/ProfileProvider") }.managesProfiles())
    }

    @Test
    fun `without MMOProfiles the key is the player UUID and the missing API is not an error`() {
        val failures = mutableListOf<Exception>()

        val key = keys(failures) { throw NoClassDefFoundError("fr/phoenixdevt/profiles/ProfileProvider") }.keyOf(player)

        assertEquals(player.toString(), key)
        assertTrue(failures.isEmpty(), "a missing API is the normal case, got $failures")
    }

    @Test
    fun `a missing API is looked for once`() {
        var lookups = 0
        val keys = keys { lookups++; throw NoClassDefFoundError("fr/phoenixdevt/profiles/ProfileProvider") }

        keys.keyOf(player)
        keys.keyOf(player)

        assertEquals(1, lookups)
    }

    @Test
    fun `a service that is not registered yet is looked for again and used once it is`() {
        var registered: ProfileIdSource? = null
        val keys = keys { registered }

        assertEquals(player.toString(), keys.keyOf(player))
        registered = ProfileIdSource { profile }

        assertEquals(profile.toString(), keys.keyOf(player))
    }

    @Test
    fun `an MMOProfiles API that fails gives the player UUID and reports the failure instead of throwing`() {
        val failures = mutableListOf<Exception>()

        val key = keys(failures) { ProfileIdSource { error("profile data not loaded") } }.keyOf(player)

        assertEquals(player.toString(), key)
        assertEquals(1, failures.size)
    }

    @Test
    fun `the MMOProfiles source reads the current profile of the player data`() {
        val current = stub<PlayerProfile>("getUniqueId" to profile)
        val data = stub<ProfileList>("getCurrent" to current)
        val provider = stub<ProfileProvider>("getPlayerData" to data)

        assertEquals(profile, MmoProfilesIdSource(provider).currentProfileId(player))
    }

    @Test
    fun `the MMOProfiles source has no profile for a player without data or without a selected profile`() {
        assertNull(MmoProfilesIdSource(stub<ProfileProvider>("getPlayerData" to null)).currentProfileId(player))

        val noSelection = stub<ProfileList>("getCurrent" to null)
        assertNull(MmoProfilesIdSource(stub<ProfileProvider>("getPlayerData" to noSelection)).currentProfileId(player))
    }

    /** An interface whose methods answer from [answers] by name (the real API has no constructor to call, only interfaces). */
    private inline fun <reified T : Any> stub(vararg answers: Pair<String, Any?>): T {
        val byName = answers.toMap()
        return Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
            check(method.name in byName) { "unexpected call to ${method.name}" }
            byName[method.name]
        } as T
    }
}
