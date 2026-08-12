package btcrenaud.numericalstorage

import btcrenaud.gui.services.MenuSessionService
import btcrenaud.numericalstorage.entries.action.handleNumericalStorageGuiCommand
import com.typewritermc.core.extension.Initializable
import com.typewritermc.core.extension.annotations.Singleton

@Singleton
class NumericalStorageExtension : Initializable {

    private companion object {
        const val GUI_COMMAND_PREFIX = "numstorage "
    }

    override suspend fun initialize() {
        NumericalStorageCoroutines.initialize()
        // Make reload/reinitialisation idempotent and keep GUI actions on the
        // direct MenuSessionService path instead of console dispatch.
        MenuSessionService.unregisterCustomCommandHandler(GUI_COMMAND_PREFIX)
        MenuSessionService.registerCustomCommandHandler(GUI_COMMAND_PREFIX) { player, _, command, _, _ ->
            handleNumericalStorageGuiCommand(player, command)
        }
    }

    override suspend fun shutdown() {
        MenuSessionService.unregisterCustomCommandHandler(GUI_COMMAND_PREFIX)
        NumericalStorageCoroutines.shutdown()
    }
}
