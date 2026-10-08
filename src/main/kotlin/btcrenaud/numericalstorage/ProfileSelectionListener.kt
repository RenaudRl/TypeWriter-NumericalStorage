package btcrenaud.numericalstorage

import fr.phoenixdevt.profiles.event.ProfileSelectedEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

/**
 * Tells the interest service which profile a player has just selected. `ProfileSelectedEvent` is fired by MMOProfiles
 * once the profile is applied, and carries the profile itself: the key is read from the event, not from the player
 * data, so it does not depend on the order in which MMOProfiles updates its own state.
 *
 * Only registered when MMOProfiles is present: this class is the one that needs its API on the class path.
 */
internal class ProfileSelectionListener(private val interest: NumericalStorageInterestService) : Listener {
    @EventHandler
    fun onProfileSelected(event: ProfileSelectedEvent) {
        interest.onProfileSelected(event.player, event.profile.uniqueId.toString())
    }
}
