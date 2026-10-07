package btcrenaud.numericalstorage

import com.typewritermc.engine.paper.logger
import fr.phoenixdevt.profiles.ProfileProvider
import org.bukkit.Bukkit
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level

/** Names the profile a player is playing, when profiles exist. A seam over MMOProfiles so the key logic is testable. */
internal fun interface ProfileIdSource {
    /** The id of the profile [playerId] has selected, or `null` when they have none (not selected yet, or not online). */
    fun currentProfileId(playerId: UUID): UUID?
}

/**
 * The key under which the balance of a player in profile mode is stored: the id of the profile they play when
 * MMOProfiles manages profiles, the player's UUID otherwise.
 *
 * MMOProfiles is an optional plugin, detected at run time. Without it, or while the player has no profile, the key is
 * the UUID; a failure of the MMOProfiles API is reported to [onFailure] and also gives the UUID, because a balance
 * is never worth an exception in a menu. In MMOProfiles' proxy mode the UUID Bukkit sees is already the profile's,
 * so both keys coincide.
 *
 * A source that is not there yet (MMOProfiles has not registered its service) is looked for again on the next call;
 * an API that cannot be loaded at all is given up on for good.
 */
internal class ProfileKeys(
    private val detect: () -> ProfileIdSource?,
    private val onFailure: (Exception) -> Unit,
) {
    @Volatile private var source: ProfileIdSource? = null
    @Volatile private var apiMissing = false

    fun keyOf(playerId: UUID): String {
        val uuidKey = playerId.toString()
        val profiles = source ?: detectSource() ?: return uuidKey
        return try {
            profiles.currentProfileId(playerId)?.toString() ?: uuidKey
        } catch (error: Exception) {
            onFailure(error)
            uuidKey
        }
    }

    private fun detectSource(): ProfileIdSource? {
        if (apiMissing) return null
        return try {
            detect()?.also { source = it }
        } catch (_: LinkageError) {
            apiMissing = true // No MMOProfiles API on this server.
            null
        } catch (error: Exception) {
            onFailure(error)
            null
        }
    }
}

/** The [ProfileKeys] of this extension, logging the first failure of the MMOProfiles API. */
internal object ProfileKeyResolver {
    private val failureLogged = AtomicBoolean(false)
    private val keys = ProfileKeys(MmoProfilesDetection::source, ::logFailureOnce)

    fun keyOf(playerId: UUID): String = keys.keyOf(playerId)

    private fun logFailureOnce(error: Exception) {
        if (!failureLogged.compareAndSet(false, true)) return
        logger.log(Level.WARNING, "[NumericalStorage] The MMOProfiles API failed: balances are keyed by player UUID (logged once)", error)
    }
}

/** The [ProfileIdSource] of the MMOProfiles API. Its class is only loaded through [MmoProfilesDetection]. */
internal class MmoProfilesIdSource(private val provider: ProfileProvider) : ProfileIdSource {

    override fun currentProfileId(playerId: UUID): UUID? = provider.getPlayerData(playerId)?.current?.uniqueId

    companion object {
        /** The source backed by the `ProfileProvider` MMOProfiles registers as a Bukkit service; `null` until it has. */
        fun ofRegisteredService(): MmoProfilesIdSource? =
            Bukkit.getServicesManager().getRegistration(ProfileProvider::class.java)?.provider?.let(::MmoProfilesIdSource)
    }
}

/** Kept apart from [MmoProfilesIdSource] so that a missing MMOProfiles API fails here, where [ProfileKeys] catches it. */
internal object MmoProfilesDetection {
    fun source(): ProfileIdSource? = MmoProfilesIdSource.ofRegisteredService()
}
