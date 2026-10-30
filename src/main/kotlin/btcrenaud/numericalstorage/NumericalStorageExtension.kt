package btcrenaud.numericalstorage

import btcrenaud.gui.editor.api.ButtonTypeRegistry
import btcrenaud.gui.services.MenuSessionService
import btcrenaud.numericalstorage.entries.action.NumericalStorageEditorButtonTypes
import btcrenaud.numericalstorage.entries.action.handleNumericalStorageGuiCommand
import com.typewritermc.core.extension.Initializable
import com.typewritermc.core.extension.annotations.Singleton
import org.bukkit.Bukkit

@Singleton
class NumericalStorageExtension : Initializable {

    private companion object {
        const val GUI_COMMAND_PREFIX = "numstorage "
    }

    override suspend fun initialize() {
        // GuiText only exists from OmniGUI 0.21; the dependency DSL cannot state a version. Do not stop the init.
        try {
            Class.forName("btcrenaud.gui.api.GuiText")
        } catch (_: ClassNotFoundException) {
            Bukkit.getLogger().severe("[NumericalStorage] requires OmniGUI 0.21 or newer: menus will not open until OmniGUI is updated.")
        }

        NumericalStorageCoroutines.initialize()
        // Make reload/reinitialisation idempotent and keep GUI actions on the
        // direct MenuSessionService path instead of console dispatch.
        MenuSessionService.unregisterCustomCommandHandler(GUI_COMMAND_PREFIX)
        MenuSessionService.registerCustomCommandHandler(GUI_COMMAND_PREFIX) { player, _, command, _, _ ->
            handleNumericalStorageGuiCommand(player, command)
        }
        ButtonTypeRegistry.register(NumericalStorageEditorButtonTypes.contribution())
    }

    override suspend fun shutdown() {
        ButtonTypeRegistry.unregister(NumericalStorageEditorButtonTypes.EXTENSION_ID)
        MenuSessionService.unregisterCustomCommandHandler(GUI_COMMAND_PREFIX)
        NumericalStorageCoroutines.shutdown()
    }
}
